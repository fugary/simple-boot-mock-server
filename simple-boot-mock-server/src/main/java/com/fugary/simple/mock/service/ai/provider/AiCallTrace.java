package com.fugary.simple.mock.service.ai.provider;

import com.fugary.simple.mock.utils.JsonUtils;
import lombok.Data;

import java.util.Map;

/** 单次调用的有界诊断信息，不记录认证请求头。 */
@Data
public class AiCallTrace {
    private static final int LIMIT = 64000;
    private String requestBody;
    private Integer httpStatus;
    private boolean streaming;
    private boolean truncated;
    private final StringBuilder raw = new StringBuilder();

    public void append(String value) {
        if (value == null) return;
        int remaining = LIMIT - raw.length();
        raw.append(value, 0, Math.min(remaining, value.length()));
        truncated |= value.length() > remaining;
    }

    public String rawResponse() {
        if (raw.length() == 0) return null;
        // SSE 和被截断的 JSON 以有效 JSON 包装，避免把拼装后的正文冒充原始响应。
        if (streaming || truncated) return JsonUtils.toJson(Map.of(
            "format", streaming ? "sse" : "text", "content", raw.toString(), "truncated", truncated));
        return raw.toString();
    }
}
