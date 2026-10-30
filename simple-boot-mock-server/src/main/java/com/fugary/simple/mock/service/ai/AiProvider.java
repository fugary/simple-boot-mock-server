package com.fugary.simple.mock.service.ai;

import java.util.Locale;

public enum AiProvider {
    OPENAI, ANTHROPIC, GEMINI;

    public String getCode() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static AiProvider fromCode(String code) {
        try {
            return valueOf(code.toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw new AiException(400, "simple.ai.invalidConfig");
        }
    }
}
