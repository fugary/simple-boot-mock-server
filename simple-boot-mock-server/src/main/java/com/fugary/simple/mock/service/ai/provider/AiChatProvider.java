package com.fugary.simple.mock.service.ai.provider;

import com.fugary.simple.mock.entity.mock.AiConfig;
import com.fugary.simple.mock.service.ai.AiProvider;
import java.util.List;

/** Provider-specific protocol mapping for chat requests. */
public interface AiChatProvider {
    AiProvider getProvider();

    List<String> loadModels(AiConfig config);

    AiChatResponse chat(AiConfig config, AiChatRequest request);
    AiChatResponse chat(AiConfig config, AiChatRequest request, AiStreamSession stream);
}
