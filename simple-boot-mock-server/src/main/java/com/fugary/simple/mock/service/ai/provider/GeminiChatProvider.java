package com.fugary.simple.mock.service.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fugary.simple.mock.entity.mock.AiConfig;
import com.fugary.simple.mock.service.ai.AiProvider;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.ArrayList;
import com.fugary.simple.mock.service.ai.AiException;
import org.springframework.web.util.UriUtils;
import java.nio.charset.StandardCharsets;

@Component
public class GeminiChatProvider extends AbstractAiChatProvider {
    @Override
    public AiProvider getProvider() {
        return AiProvider.GEMINI;
    }

    @Override
    public AiChatResponse chat(AiConfig config, AiChatRequest request) {
        String model = config.getDefaultModel().replaceFirst("^models/", "");
        String path = "/models/" + UriUtils.encodePathSegment(model, StandardCharsets.UTF_8) + ":generateContent";
        Map<String, Object> body = Map.of("contents", List.of(Map.of("role", "user",
            "parts", List.of(Map.of("text", request.getPrompt())))));
        JsonNode data = post(url(config.getBaseUrl(), path), headers("x-goog-api-key", config.getApiKey()), body);
        StringBuilder text = new StringBuilder();
        for (JsonNode part : data.path("candidates").path(0).path("content").path("parts")) {
            if (!part.path("thought").asBoolean() && part.path("text").isTextual()) {
                text.append(part.path("text").asText());
            }
        }
        AiChatResponse response = response(text.toString());
        JsonNode usage = data.path("usageMetadata");
        response.setPromptTokens(tokenCount(usage, "promptTokenCount"));
        response.setCompletionTokens(tokenCount(usage, "candidatesTokenCount"));
        response.setTotalTokens(tokenCount(usage, "totalTokenCount"));
        return response;
    }

    @Override
    public List<String> loadModels(AiConfig config) {
        JsonNode models = get(url(config.getBaseUrl(), "/models"), headers("x-goog-api-key", config.getApiKey())).path("models");
        if (!models.isArray()) {
            throw new AiException(500, "simple.ai.invalidResponse");
        }
        Set<String> names = new TreeSet<>();
        for (JsonNode model : models) {
            String name = model.path("name").asText().replaceFirst("^models/", "");
            if (!name.isBlank() && supportsChat(model)) {
                names.add(name);
            }
            if (names.size() >= 1000) {
                break;
            }
        }
        return new ArrayList<>(names);
    }

    private boolean supportsChat(JsonNode model) {
        JsonNode methods = model.path("supportedGenerationMethods");
        if (!methods.isArray()) {
            return true;
        }
        for (JsonNode method : methods) {
            if ("generateContent".equals(method.asText())) {
                return true;
            }
        }
        return false;
    }

}
