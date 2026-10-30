package com.fugary.simple.mock.service.ai;

import com.fugary.simple.mock.service.ai.provider.AiChatRequest;
import com.fugary.simple.mock.entity.mock.AiConfig;
import java.util.List;
import com.fugary.simple.mock.service.ai.provider.AiChatResponse;

public interface AiService {
    List<String> loadModels(Integer configId);
    java.util.List<String> loadModels(AiConfig config);
    AiChatResponse test(Integer configId, AiChatRequest request);
}
