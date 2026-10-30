package com.fugary.simple.mock.service.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fugary.simple.mock.entity.mock.AiConfig;
import com.fugary.simple.mock.service.ai.AiProvider;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpHeaders;
import java.util.List;
import java.util.Map;

@Component
public class AnthropicChatProvider extends AbstractAiChatProvider {
    @Override
    public AiProvider getProvider() {
        return AiProvider.ANTHROPIC;
    }

    @Override
    public AiChatResponse chat(AiConfig config, AiChatRequest request) {
        HttpHeaders headers = headers("x-api-key", config.getApiKey());
        headers.set("anthropic-version", "2023-06-01");
        Map<String, Object> body = Map.of("model", config.getDefaultModel(), "max_tokens", 1024,
            "messages", List.of(Map.of("role", "user", "content", request.getPrompt())));
        JsonNode data = post(url(config.getBaseUrl(), "/messages"), headers, body);
        StringBuilder text = new StringBuilder();
        for (JsonNode block : data.path("content")) {
            if ("text".equals(block.path("type").asText()) && block.path("text").isTextual()) {
                text.append(block.path("text").asText());
            }
        }
        AiChatResponse response = response(text.toString());
        response.setPromptTokens(tokenCount(data.path("usage"), "input_tokens"));
        response.setCompletionTokens(tokenCount(data.path("usage"), "output_tokens"));
        if (response.getPromptTokens() != null && response.getCompletionTokens() != null) {
            response.setTotalTokens((int) Math.min(Integer.MAX_VALUE,
                (long) response.getPromptTokens() + response.getCompletionTokens()));
        }
        return response;
    }

    @Override
    public List<String> loadModels(AiConfig config) {
        HttpHeaders headers = headers("x-api-key", config.getApiKey());
        headers.set("anthropic-version", "2023-06-01");
        return modelIds(get(url(config.getBaseUrl(), "/models"), headers).path("data"));
    }

}
