package com.fugary.simple.mock.service.ai.provider;

import lombok.Data;

@Data
public class AiChatRequest {
    private String prompt;
    private String model;
}
