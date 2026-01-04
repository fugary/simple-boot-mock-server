package com.fugary.simple.mock.push;

import com.fugary.simple.mock.web.vo.query.MockParamsVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
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

    @Override
    public SseEmitter processSseProxy(MockParamsVo mockParams) {
        SseEmitter sseEmitter = new SseEmitter(0L); // 无限超时
        String requestUrl = defaultPushProcessor.getRequestUrl(mockParams.getTargetUrl(), mockParams);
        AtomicBoolean cancelled = new AtomicBoolean(false);

        log.info("开始处理 SSE 代理请求: {}", requestUrl);

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
                                SseEmitter.SseEventBuilder eventBuilder = null;
                                boolean hasData = false;

                                while (!cancelled.get() && (line = reader.readLine()) != null) {
                                    if (line.isEmpty()) {
                                        // 遇到空行，发送之前累积的事件
                                        if (eventBuilder != null && hasData) {
                                            try {
                                                log.debug("发送 SSE 事件对象");
                                                sseEmitter.send(eventBuilder);
                                            } catch (Exception e) {
                                                log.warn("发送 SSE 事件失败，客户端可能已断开: {}", e.getMessage());
                                                cancelled.set(true);
                                                break;
                                            }
                                        }
                                        eventBuilder = null;
                                        hasData = false;
                                    } else {
                                        if (eventBuilder == null) {
                                            eventBuilder = SseEmitter.event();
                                        }
                                        if (line.startsWith("data:")) {
                                            String dataContent = line.substring(5);
                                            if (dataContent.startsWith(" ")) {
                                                dataContent = dataContent.substring(1);
                                            }
                                            eventBuilder.data(dataContent);
                                            hasData = true;
                                        } else if (line.startsWith("event:")) {
                                            eventBuilder.name(line.substring(6).trim());
                                        } else if (line.startsWith("id:")) {
                                            eventBuilder.id(line.substring(3).trim());
                                        } else if (line.startsWith("retry:")) {
                                            try {
                                                eventBuilder.reconnectTime(Long.parseLong(line.substring(6).trim()));
                                            } catch (Exception ignored) {
                                            }
                                        }
                                    }
                                }

                                if (!cancelled.get()) {
                                    // 发送最后一个事件（如果有）
                                    if (eventBuilder != null && hasData) {
                                        try {
                                            sseEmitter.send(eventBuilder);
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
            }
        }, eventStreamThreadPool);

        return sseEmitter;
    }
}
