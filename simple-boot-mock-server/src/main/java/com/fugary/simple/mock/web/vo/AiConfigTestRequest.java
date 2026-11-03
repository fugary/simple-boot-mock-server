package com.fugary.simple.mock.web.vo;

import com.fugary.simple.mock.entity.mock.AiConfig;
import com.fugary.simple.mock.service.ai.provider.AiChatRequest;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiConfigTestRequest extends AiChatRequest {
    private AiConfig config;
}
