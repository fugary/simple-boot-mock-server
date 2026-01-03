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

        log.info("开始处理 SSE 代理请求: {}", requestUrl);

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

                                while ((line = reader.readLine()) != null) {
                                    eventBuilder.append(line).append("\n");

                                    // SSE 事件以空行分隔
                                    if (line.isEmpty() && eventBuilder.length() > 1) {
                                        String event = eventBuilder.toString();
                                        log.debug("发送 SSE 事件: {}", event.trim());
                                        sseEmitter.send(event);
                                        eventBuilder.setLength(0); // 清空 builder
                                    }
                                }

                                // 发送最后一个事件（如果有）
                                if (eventBuilder.length() > 0) {
                                    sseEmitter.send(eventBuilder.toString());
                                }

                                sseEmitter.complete();
                                log.info("SSE 代理请求完成");
                            }
                            return null;
                        });
            } catch (Exception e) {
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
        });

        return sseEmitter;
    }
}
