package com.fugary.simple.mock.service.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fugary.simple.mock.entity.mock.AiConfig;
import com.fugary.simple.mock.service.ai.AiProvider;
import org.springframework.stereotype.Component;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import java.util.List;
import java.util.Map;

@Component
public class OpenAiChatProvider extends AbstractAiChatProvider {
    @Override
    public AiProvider getProvider() {
        return AiProvider.OPENAI;
    }

    @Override
    public AiChatResponse chat(AiConfig config, AiChatRequest request) {
        HttpHeaders headers = headers("Authorization", StringUtils.isBlank(config.getApiKey()) ? null : "Bearer " + config.getApiKey());
        Map<String, Object> body = Map.of("model", config.getDefaultModel(), "messages",
            List.of(Map.of("role", "user", "content", request.getPrompt())));
        JsonNode data = post(url(config.getBaseUrl(), "/chat/completions"), headers, body);
        AiChatResponse response = response(data.path("choices").path(0).path("message").path("content").asText());
        JsonNode usage = data.path("usage");
        response.setPromptTokens(tokenCount(usage, "prompt_tokens"));
        response.setCompletionTokens(tokenCount(usage, "completion_tokens"));
        response.setTotalTokens(tokenCount(usage, "total_tokens"));
        return response;
    }

    @Override
    public List<String> loadModels(AiConfig config) {
        HttpHeaders headers = headers("Authorization", StringUtils.isBlank(config.getApiKey()) ? null : "Bearer " + config.getApiKey());
        return modelIds(get(url(config.getBaseUrl(), "/models"), headers).path("data"));
    }

}
