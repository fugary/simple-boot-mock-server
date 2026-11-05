package com.fugary.simple.mock.push;

import com.fugary.simple.mock.entity.mock.MockData;
import com.fugary.simple.mock.entity.mock.MockRequest;
import com.fugary.simple.mock.utils.MockEventStreamUtils;
import com.fugary.simple.mock.utils.MockJsUtils;
import com.fugary.simple.mock.web.vo.http.HttpRequestVo;
import com.fugary.simple.mock.web.vo.query.MockParamsVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * SSE 代理处理器默认实现
 * 逐行读取并转发上游 SSE 事件
 *
 * Create date 2026/01/03<br>
 *
 * @author gary.fu
 */
@Slf4j
@Component
public class DefaultMockSsePushProcessorImpl implements MockSsePushProcessor {

    @Autowired
    private CloseableHttpClient mockHttpClient;

    @Autowired
    private DefaultMockPushProcessorImpl defaultPushProcessor;

    @Autowired
    @Qualifier("eventStreamThreadPool")
    private ExecutorService eventStreamThreadPool;

    @Autowired
    private MockPostScriptProcessor mockPostScriptProcessor;

    @Override
    public SseEmitter processSseProxy(MockParamsVo mockParams, MockRequest mockRequest, MockData mockData) {
        SseEmitter sseEmitter = new SseEmitter(0L); // 无限超时
        String requestUrl = defaultPushProcessor.getRequestUrl(mockParams.getTargetUrl(), mockParams);
        CompletableFuture<Void> disconnected = new CompletableFuture<>();
        MockEventStreamUtils.onDisconnect(sseEmitter, () -> disconnected.complete(null));
        HttpRequestVo requestVo = MockJsUtils.getHttpRequestVo();

        eventStreamThreadPool.execute(() -> {
            if (disconnected.isDone()) {
                return;
            }
            MockJsUtils.setCurrentRequestVo(requestVo);
            try (AutoCloseable scope = mockPostScriptProcessor.openSseScriptScope(mockRequest, mockData)) {
                HttpUriRequest upstream = createRequest(requestUrl, mockParams);
                // 即使客户端在请求创建前断开，注册时也会立即 abort，唤醒连接池等待或 socket 读取。
                disconnected.thenRun(upstream::abort);
                try (CloseableHttpResponse response = mockHttpClient.execute(upstream)) {
                    int status = response.getStatusLine().getStatusCode();
                    if (status >= 400) {
                        throw new HttpResponseException(status, response.getStatusLine().getReasonPhrase());
                    }
                    if (response.getEntity() != null) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(
                                response.getEntity().getContent(), StandardCharsets.UTF_8));
                        forwardEvents(reader, sseEmitter, mockRequest, mockData, disconnected);
                    }
                }
                if (!disconnected.isDone()) {
                    sseEmitter.complete();
                }
            } catch (Exception e) {
                if (!disconnected.isDone()) {
                    log.error("SSE 代理请求错误: {}", requestUrl, e);
                    try {
                        sseEmitter.send(SseEmitter.event().name("error").data("SSE Proxy Error: " + e.getMessage()));
                    } catch (Exception sendError) {
                        log.debug("SSE 连接已关闭", sendError);
                    }
                    sseEmitter.completeWithError(e);
                }
            } finally {
                MockJsUtils.removeCurrentRequestVo();
            }
        });
        return sseEmitter;
    }

    private HttpUriRequest createRequest(String url, MockParamsVo params) {
        HttpMethod method = Optional.ofNullable(HttpMethod.resolve(params.getMethod())).orElse(HttpMethod.GET);
        RequestBuilder builder = RequestBuilder.create(method.name()).setUri(url);
        HttpHeaders headers = defaultPushProcessor.getHeaders(params);
        headers.setAccept(Collections.singletonList(MediaType.TEXT_EVENT_STREAM));
        // 由 HttpClient 根据实际发送的实体生成传输长度。
        headers.remove(HttpHeaders.CONTENT_LENGTH);
        headers.remove(HttpHeaders.TRANSFER_ENCODING);
        headers.forEach((key, values) -> values.forEach(value -> builder.addHeader(key, value)));
        if (params.getRequestBody() != null && method != HttpMethod.GET && method != HttpMethod.HEAD) {
            builder.setEntity(new ByteArrayEntity(params.getRequestBody().getBytes(StandardCharsets.UTF_8)));
        }
        return builder.build();
    }

    private void forwardEvents(BufferedReader reader, SseEmitter emitter, MockRequest request, MockData data,
                               CompletableFuture<Void> disconnected) throws IOException {
        Map<String, Object> event = new LinkedHashMap<>();
        StringBuilder body = new StringBuilder();
        boolean hasData = false;
        String line;
        while (!disconnected.isDone() && (line = reader.readLine()) != null) {
            if (line.isEmpty()) {
                if (hasData) {
                    sendEvent(emitter, request, data, event, body);
                }
                event.clear();
                body.setLength(0);
                hasData = false;
            } else if (line.startsWith("data:")) {
                if (body.length() > 0) {
                    body.append('\n');
                }
                body.append(line, line.startsWith("data: ") ? 6 : 5, line.length());
                hasData = true;
            } else if (line.startsWith("event:")) {
                event.put("event", line.substring(6).trim());
            } else if (line.startsWith("id:")) {
                event.put("id", line.substring(3).trim());
            } else if (line.startsWith("retry:")) {
                try {
                    event.put("retry", Long.parseLong(line.substring(6).trim()));
                } catch (NumberFormatException ignored) {
                    // 忽略无效的 retry 值。
                }
            }
        }
        if (hasData && !disconnected.isDone()) {
            sendEvent(emitter, request, data, event, body);
        }
    }

    private void sendEvent(SseEmitter emitter, MockRequest request, MockData data,
                           Map<String, Object> event, StringBuilder body) throws IOException {
        if (body.length() > 0) {
            event.put("data", body.toString());
        }
        Object processed = mockPostScriptProcessor.processSse(request, data, event);
        if (processed instanceof Map) {
            Map<?, ?> fields = (Map<?, ?>) processed;
            SseEmitter.SseEventBuilder builder = SseEmitter.event();
            if (fields.containsKey("id")) {
                builder.id(String.valueOf(fields.get("id")));
            }
            if (fields.containsKey("event")) {
                builder.name(String.valueOf(fields.get("event")));
            }
            if (fields.containsKey("retry")) {
                try {
                    builder.reconnectTime(Long.parseLong(String.valueOf(fields.get("retry"))));
                } catch (NumberFormatException ignored) {
                    // 后置脚本也可能返回无效的 retry。
                }
            }
            if (fields.containsKey("data")) {
                builder.data(fields.get("data"));
            }
            emitter.send(builder);
        } else if (processed != null) {
            emitter.send(processed);
        }
    }
}
