package com.fugary.simple.mock.push;

import com.fugary.simple.mock.web.vo.query.MockParamsVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
                                StringBuilder eventBuilder = new StringBuilder();

                                // 检查是否被取消，及时退出循环
                                while (!cancelled.get() && (line = reader.readLine()) != null) {
                                    // SSE 事件以空行分隔
                                    if (line.isEmpty()) {
                                        // 遇到空行，发送之前累积的事件（不包含这个空行）
                                        if (eventBuilder.length() > 0) {
                                            String event = eventBuilder.toString();
                                            log.debug("发送 SSE 事件: {}", event.trim());

                                            try {
                                                sseEmitter.send(event);
                                            } catch (Exception e) {
                                                log.warn("发送 SSE 事件失败，客户端可能已断开: {}", e.getMessage());
                                                cancelled.set(true);
                                                break;
                                            }
                                            eventBuilder.setLength(0); // 清空 builder
                                        }
                                    } else {
                                        // 非空行，添加到 builder
                                        eventBuilder.append(line).append("\n");
                                    }
                                }

                                if (!cancelled.get()) {
                                    // 发送最后一个事件（如果有）
                                    if (eventBuilder.length() > 0) {
                                        try {
                                            sseEmitter.send(eventBuilder.toString());
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
        });

        return sseEmitter;
    }
}
