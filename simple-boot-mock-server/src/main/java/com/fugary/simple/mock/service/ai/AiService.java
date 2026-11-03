package com.fugary.simple.mock.service.ai;

import com.fugary.simple.mock.entity.mock.AiConfig;
import com.fugary.simple.mock.service.ai.provider.AiChatRequest;
import com.fugary.simple.mock.service.ai.provider.AiChatResponse;
import com.fugary.simple.mock.service.ai.provider.AiStreamSession;

import java.util.List;

public interface AiService {
    List<String> loadModels(Integer configId);
    List<String> loadModels(AiConfig config);
    AiChatResponse test(Integer configId, AiChatRequest request);
    AiChatResponse test(Integer configId, AiChatRequest request, AiStreamSession stream);
    AiChatResponse testDraft(AiConfig config, AiChatRequest request, AiStreamSession stream);
}
