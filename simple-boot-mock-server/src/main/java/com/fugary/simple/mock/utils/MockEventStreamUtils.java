package com.fugary.simple.mock.utils;

import com.fugary.simple.mock.contants.MockConstants;
import com.fugary.simple.mock.entity.mock.MockData;
import com.fugary.simple.mock.entity.mock.MockGroup;
import com.fugary.simple.mock.utils.servlet.HttpRequestUtils;
import com.fugary.simple.mock.web.vo.http.HttpRequestVo;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Utility for SSE.
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MockEventStreamUtils {

    public static SseEmitter processSseRequest(HttpServletRequest request, HttpServletResponse response, MockData data,
            HttpHeaders httpHeaders, MockGroup mockGroup) {
        SseEmitter sseEmitter = new SseEmitter(0L); // Infinite timeout
        HttpRequestVo requestVo = HttpRequestUtils.parseRequestVo(request);
        MockJsUtils.setCurrentRequestVo(requestVo); // Set for async thread if using standard context, but Sse is async
        // For SSE, we use a separate thread or CompletableFuture
        CompletableFuture.runAsync(() -> {
            try {
                MockJsUtils.setCurrentRequestVo(requestVo); // Need to set in the new thread
                Pair<String, Object> mockResponse = SimpleMockUtils.getMockResponseBody(data, MediaType.TEXT_EVENT_STREAM_VALUE);
                Object body = mockResponse.getValue();
                if (body instanceof String && MockJsUtils.isJson((String) body)) {
                    body = JsonUtils.fromJson((String) body, Object.class);
                }
                if (body instanceof Collection) {
                    for (Object item : (Collection<?>) body) {
                        sendSseItem(sseEmitter, item);
                    }
                } else if (body instanceof Map) {
                    sendSseItem(sseEmitter, body);
                } else {
                    sseEmitter.send(body);
                }
                sseEmitter.complete();
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
        httpHeaders.forEach((k, v) -> v.forEach(val -> response.setHeader(k, val)));
        response.setHeader(MockConstants.MOCK_DATA_ID_HEADER, String.valueOf(data.getId()));
        if (mockGroup != null) {
            response.setHeader(MockConstants.MOCK_DATA_USER_HEADER, mockGroup.getUserName());
        }
        SimpleLogUtils.addResponseData("SSE Stream Started");
        return sseEmitter;
    }

    private static void sendSseItem(SseEmitter sseEmitter, Object item) throws IOException {
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
        if (delay > 0) {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (data != null) {
            sseEmitter.send(data);
        }
    }
}
