package com.fugary.simple.mock.service.ai.provider;

import lombok.Data;

@Data
public class AiChatRequest {
    @com.fasterxml.jackson.annotation.JsonIgnore
    private AiCallTrace trace = new AiCallTrace();
    private String prompt;
    private String model;
}
