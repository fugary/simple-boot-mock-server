package com.fugary.simple.mock.push;

import com.fugary.simple.mock.utils.MockJsUtils;
import com.fugary.simple.mock.web.vo.http.HttpRequestVo;
import com.fugary.simple.mock.web.vo.query.MockParamsVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import com.fugary.simple.mock.entity.mock.MockData;
import com.fugary.simple.mock.entity.mock.MockRequest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * SSE 代理处理器默认实现
 * 使用 RestTemplate 进行流式读取并转发 SSE 事件
 *
 * Create date 2026/01/03<br>
 *
 * @author gary.fu
 */
@Slf4j
@Component
public class DefaultMockSsePushProcessorImpl implements MockSsePushProcessor {

    @Autowired
    private RestTemplate restTemplate;

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
        AtomicBoolean cancelled = new AtomicBoolean(false);

        log.info("开始处理 SSE 代理请求: {}", requestUrl);

        // Capture request context from the main thread
        final HttpRequestVo requestVo = MockJsUtils.getHttpRequestVo();

        // 设置生命周期回调
        Runnable cleanupTask = () -> {
            cancelled.set(true);
            log.info("SSE 连接已关闭，清理资源: {}", requestUrl);
        };

        sseEmitter.onCompletion(cleanupTask);
        sseEmitter.onTimeout(cleanupTask);
        sseEmitter.onError(throwable -> {
            log.warn("SSE 连接发生错误: {}, 错误: {}", requestUrl, throwable.getMessage());
            cleanupTask.run();
        });

        CompletableFuture.runAsync(() -> {
            // Set request context in the new thread
            MockJsUtils.setCurrentRequestVo(requestVo);
            try {
                // 准备请求头
                HttpHeaders headers = defaultPushProcessor.getHeaders(mockParams);
                headers.setAccept(Collections.singletonList(MediaType.TEXT_EVENT_STREAM));

                // 使用 RestTemplate.execute 进行流式处理
                restTemplate.execute(
                        URI.create(requestUrl),
                        Optional.ofNullable(HttpMethod.resolve(mockParams.getMethod())).orElse(HttpMethod.GET),
                        clientHttpRequest -> {
                            // 设置请求头
                            headers.forEach((key, values) -> values
                                    .forEach(value -> clientHttpRequest.getHeaders().add(key, value)));
                            // 如果有请求体，写入请求体
                            if (mockParams.getRequestBody() != null
                                    && !HttpMethod.GET.matches(mockParams.getMethod())
                                    && !HttpMethod.HEAD.matches(mockParams.getMethod())) {
                                byte[] bodyBytes = mockParams.getRequestBody() instanceof String
                                        ? ((String) mockParams.getRequestBody()).getBytes(StandardCharsets.UTF_8)
                                        : mockParams.getRequestBody().toString().getBytes(StandardCharsets.UTF_8);
                                clientHttpRequest.getBody().write(bodyBytes);
                            }
                        },
                        clientHttpResponse -> {
                            // 逐行读取 SSE 响应并转发
                            log.info("开始读取 SSE 响应流，状态码: {}", clientHttpResponse.getStatusCode());

                            try (BufferedReader reader = new BufferedReader(
                                    new InputStreamReader(clientHttpResponse.getBody(), StandardCharsets.UTF_8))) {
                                String line;
                                Map<String, Object> eventMap = new LinkedHashMap<>();
                                boolean hasData = false;
                                StringBuilder dataBuffer = new StringBuilder();

                                while (!cancelled.get() && (line = reader.readLine()) != null) {
                                    if (line.isEmpty()) {
                                        // 遇到空行，发送之前累积的事件
                                        if (hasData) {
                                            if (dataBuffer.length() > 0) {
                                                eventMap.put("data", dataBuffer.toString());
                                                dataBuffer.setLength(0);
                                            }
                                            try {
                                                log.debug("发送 SSE 事件对象");
                                                Object processed = mockPostScriptProcessor.processSse(mockRequest,
                                                        mockData, eventMap);
                                                if (processed != null) {
                                                    if (processed instanceof Map) {
                                                        Map<?, ?> map = (Map<?, ?>) processed;
                                                        SseEmitter.SseEventBuilder builder = SseEmitter.event();
                                                        if (map.containsKey("id")) {
                                                            builder.id(String.valueOf(map.get("id")));
                                                        }
                                                        if (map.containsKey("event")) {
                                                            builder.name(String.valueOf(map.get("event")));
                                                        }
                                                        if (map.containsKey("retry")) {
                                                            try {
                                                                builder.reconnectTime(Long
                                                                        .parseLong(String.valueOf(map.get("retry"))));
                                                            } catch (Exception ignored) {
                                                            }
                                                        }
                                                        if (map.containsKey("data")) {
                                                            builder.data(map.get("data"));
                                                        }
                                                        sseEmitter.send(builder);
                                                    } else {
                                                        sseEmitter.send(processed);
                                                    }
                                                }
                                            } catch (Exception e) {
                                                log.warn("发送 SSE 事件失败，客户端可能已断开: {}", e.getMessage());
                                                cancelled.set(true);
                                                break;
                                            }
                                        }
                                        eventMap.clear();
                                        hasData = false;
                                    } else {
                                        if (line.startsWith("data:")) {
                                            String dataContent = line.substring(5);
                                            if (dataContent.startsWith(" ")) {
                                                dataContent = dataContent.substring(1);
                                            }
                                            if (dataBuffer.length() > 0) {
                                                dataBuffer.append("\n");
                                            }
                                            dataBuffer.append(dataContent);
                                            hasData = true;
                                        } else if (line.startsWith("event:")) {
                                            eventMap.put("event", line.substring(6).trim());
                                        } else if (line.startsWith("id:")) {
                                            eventMap.put("id", line.substring(3).trim());
                                        } else if (line.startsWith("retry:")) {
                                            try {
                                                eventMap.put("retry", Long.parseLong(line.substring(6).trim()));
                                            } catch (Exception ignored) {
                                            }
                                        }
                                    }
                                }

                                if (!cancelled.get()) {
                                    // 发送最后一个事件（如果有）
                                    if (hasData) {
                                        if (dataBuffer.length() > 0) {
                                            eventMap.put("data", dataBuffer.toString());
                                        }
                                        try {
                                            Object processed = mockPostScriptProcessor.processSse(mockRequest, mockData,
                                                    eventMap);
                                            if (processed != null) {
                                                if (processed instanceof Map) {
                                                    Map<?, ?> map = (Map<?, ?>) processed;
                                                    SseEmitter.SseEventBuilder builder = SseEmitter.event();
                                                    if (map.containsKey("id")) {
                                                        builder.id(String.valueOf(map.get("id")));
                                                    }
                                                    if (map.containsKey("event")) {
                                                        builder.name(String.valueOf(map.get("event")));
                                                    }
                                                    if (map.containsKey("retry")) {
                                                        builder.reconnectTime(
                                                                Long.parseLong(String.valueOf(map.get("retry"))));
                                                    }
                                                    if (map.containsKey("data")) {
                                                        builder.data(map.get("data"));
                                                    }
                                                    sseEmitter.send(builder);
                                                } else {
                                                    sseEmitter.send(processed);
                                                }
                                            }
                                        } catch (Exception e) {
                                            log.warn("发送最后的 SSE 事件失败: {}", e.getMessage());
                                        }
                                    }
                                    sseEmitter.complete();
                                    log.info("SSE 代理请求完成");
                                } else {
                                    log.info("SSE 代理请求被取消");
                                }
                            }
                            return null;
                        });
            } catch (Exception e) {
                if (!cancelled.get()) {
                    log.error("SSE 代理请求错误: {}", requestUrl, e);
                    try {
                        // 尝试发送错误信息给客户端
                        sseEmitter.send(SseEmitter.event()
                                .name("error")
                                .data("SSE Proxy Error: " + e.getMessage()));
                    } catch (Exception sendError) {
                        log.error("发送错误事件失败", sendError);
                    }
                    sseEmitter.completeWithError(e);
                }
            } finally {
                // Clean up request context
                MockJsUtils.removeCurrentRequestVo();
            }
        }, eventStreamThreadPool);

        return sseEmitter;
    }
}
