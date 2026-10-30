package com.fugary.simple.mock.service.ai.provider;

import lombok.Data;

@Data
public class AiChatResponse {
    private String content;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private Long durationMs;
}
