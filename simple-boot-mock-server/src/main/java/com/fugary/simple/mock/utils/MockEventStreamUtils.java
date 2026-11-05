package com.fugary.simple.mock.utils;

import com.fugary.simple.mock.entity.mock.MockData;
import com.fugary.simple.mock.utils.servlet.HttpRequestUtils;
import com.fugary.simple.mock.web.vo.http.HttpRequestVo;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import com.fugary.simple.mock.entity.mock.MockRequest;
import com.fugary.simple.mock.push.MockPostScriptProcessor;

/**
 * Utility for SSE.
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MockEventStreamUtils {

    public static SseEmitter processSseRequest(HttpServletRequest request, HttpServletResponse response, MockData data,
            ExecutorService executorService, MockPostScriptProcessor postProcessor, MockRequest mockRequest) {
        SseEmitter sseEmitter = new SseEmitter(0L); // Infinite timeout
        HttpRequestVo requestVo = HttpRequestUtils.parseRequestVo(request);
        CountDownLatch disconnected = new CountDownLatch(1);
        onDisconnect(sseEmitter, disconnected::countDown);
        executorService.execute(() -> {
            if (disconnected.getCount() == 0) {
                return;
            }
            try (AutoCloseable scope = postProcessor.openSseScriptScope(mockRequest, data)) {
                MockJsUtils.setCurrentRequestVo(requestVo);
                Pair<String, Object> mockResponse = SimpleMockUtils.getMockResponseBody(data,
                        MediaType.TEXT_EVENT_STREAM_VALUE);
                Object body = mockResponse.getValue();
                if (body instanceof String && MockJsUtils.isJson((String) body)) {
                    body = JsonUtils.fromJson((String) body, Object.class);
                }
                Object streamField;
                if (body instanceof Map && (streamField = ((Map<?, ?>) body).get("stream")) instanceof Collection) {
                    body = streamField;
                }
                boolean structured = body instanceof Collection || body instanceof Map;
                Collection<?> items = body instanceof Collection ? (Collection<?>) body : Collections.singletonList(body);
                for (Object item : items) {
                    if (disconnected.getCount() == 0) {
                        return;
                    }
                    Object processed = postProcessor.processSse(mockRequest, data, item);
                    if (structured) {
                        sendSseItem(sseEmitter, processed, disconnected);
                    } else {
                        sseEmitter.send(processed);
                    }
                }
                if (disconnected.getCount() != 0) {
                    sseEmitter.complete();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                sseEmitter.completeWithError(e);
            } catch (Exception e) {
                log.error("SSE Error", e);
                sseEmitter.completeWithError(e);
            } finally {
                MockJsUtils.removeCurrentRequestVo();
            }
        });
        // Headers for SSE
        response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        SimpleLogUtils.addResponseData("SSE Stream Started");
        return sseEmitter;
    }

    public static void onDisconnect(SseEmitter emitter, Runnable cancel) {
        emitter.onCompletion(cancel);
        emitter.onTimeout(cancel);
        emitter.onError(error -> cancel.run());
    }

    private static void sendSseItem(SseEmitter sseEmitter, Object item, CountDownLatch disconnected)
            throws IOException, InterruptedException {
        long delay = 0;
        Object data = item;
        if (item instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) item;
            if (map.containsKey("delay")) {
                Object delayObj = map.get("delay");
                if (delayObj instanceof Number) {
                    delay = ((Number) delayObj).longValue();
                } else if (delayObj instanceof String && NumberUtils.isDigits((String) delayObj)) {
                    delay = NumberUtils.toLong((String) delayObj);
                }
            }
            if (map.containsKey("data")) {
                data = map.get("data");
            }
        }
        if (!disconnected.await(delay, TimeUnit.MILLISECONDS) && data != null) {
            sseEmitter.send(data);
        }
    }
}
