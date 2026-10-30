package com.fugary.simple.mock.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fugary.simple.mock.entity.mock.AiConfig;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import com.fugary.simple.mock.service.ai.provider.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class AiProviderTest {
    @Test
    void supportsProviderProtocolsWithoutExposingKeys() throws Exception {
        for (String provider : new String[]{"openai", "anthropic", "gemini"}) {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            AtomicReference<String> path = new AtomicReference<>();
            AtomicReference<String> body = new AtomicReference<>();
            AtomicReference<String> credential = new AtomicReference<>();
            AtomicReference<String> version = new AtomicReference<>();
            String header = "openai".equals(provider) ? "Authorization" : "anthropic".equals(provider) ? "x-api-key" : "x-goog-api-key";
            server.createContext("/", exchange -> {
                path.set(exchange.getRequestURI().getPath());
                body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                credential.set(exchange.getRequestHeaders().getFirst(header));
                version.set(exchange.getRequestHeaders().getFirst("anthropic-version"));
                String response = "openai".equals(provider) ? "{\"choices\":[{\"message\":{\"content\":\"hello\"}}]}"
                    : "anthropic".equals(provider) ? "{\"content\":[{\"type\":\"thinking\"},{\"type\":\"text\",\"text\":\"hel\"},{\"type\":\"text\",\"text\":\"lo\"}]}"
                    : "{\"candidates\":[{\"content\":{\"parts\":[{\"thought\":true,\"text\":\"hidden\"},{\"text\":\"hel\"},{\"text\":\"lo\"}]}}]}";
                String usage = "openai".equals(provider) ? "\"usage\":{\"prompt_tokens\":2,\"completion_tokens\":3,\"total_tokens\":5},"
                    : "anthropic".equals(provider) ? "\"usage\":{\"input_tokens\":2,\"output_tokens\":3},"
                    : "\"usageMetadata\":{\"promptTokenCount\":2,\"candidatesTokenCount\":3,\"totalTokenCount\":5},";
                response = "{" + usage + response.substring(1);
                byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.close();
            });
            server.start();
            try {
                AiChatProvider client = "openai".equals(provider) ? new OpenAiChatProvider()
                    : "anthropic".equals(provider) ? new AnthropicChatProvider() : new GeminiChatProvider();
                ObjectMapper mapper = new ObjectMapper();

                AiConfig config = new AiConfig();
                config.setProvider(provider);
                config.setDefaultModel("gemini".equals(provider) ? "models/model" : "model");
                config.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/v1");
                config.setApiKey("secret-key");
                AiChatRequest request = new AiChatRequest();
                request.setPrompt("test prompt");
                AiChatResponse result = client.chat(config, request);
                assertEquals("hello", result.getContent());
                assertEquals(2, result.getPromptTokens());
                assertEquals(3, result.getCompletionTokens());
                assertEquals(5, result.getTotalTokens());
                com.fasterxml.jackson.databind.JsonNode sent = mapper.readTree(body.get());
                if ("gemini".equals(provider)) {
                    assertEquals("test prompt", sent.path("contents").path(0).path("parts").path(0).path("text").asText());
                    assertFalse(sent.has("model"));
                    assertFalse(sent.has("messages"));
                } else {
                    assertEquals("model", sent.path("model").asText());
                    assertEquals("test prompt", sent.path("messages").path(0).path("content").asText());
                }
                if ("anthropic".equals(provider)) {
                    assertEquals(1024, sent.path("max_tokens").asInt());
                    assertEquals("2023-06-01", version.get());
                }
                assertTrue(body.get().contains("test prompt"));
                assertEquals("openai".equals(provider) ? "/v1/chat/completions" : "anthropic".equals(provider) ? "/v1/messages" : "/v1/models/model:generateContent", path.get());
                assertTrue(credential.get().endsWith("secret-key"));
                assertFalse(path.get().contains("secret-key"));
                assertFalse(config.toString().contains("secret-key"));
            } finally {
                server.stop(0);
            }
        }
    }

    @Test
    void sanitizesHttpFailuresAndInvalidJson() throws Exception {
        for (int status : new int[]{429, 200}) {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> {
                byte[] bytes = "private-key malformed-response".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(status, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.close();
            });
            server.start();
            try {
                AiConfig config = new AiConfig();
                config.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
                config.setDefaultModel("model");
                AiChatRequest request = new AiChatRequest();
                request.setPrompt("hello");
                AiException error = assertThrows(AiException.class,
                    () -> new OpenAiChatProvider().chat(config, request));
                assertEquals(status == 429 ? "simple.ai.httpError" : "simple.ai.invalidResponse", error.getMessageKey());
                assertFalse(error.toString().contains("private-key"));
                if (status == 429) {
                    assertEquals(429, error.getArguments()[0]);
                }
            } finally {
                server.stop(0);
            }
        }
    }


    @Test
    void discoversSortedModelsUsingProviderGetProtocols() throws Exception {
        for (AiChatProvider provider : java.util.List.of(new OpenAiChatProvider(), new AnthropicChatProvider(), new GeminiChatProvider())) {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            AtomicReference<String> method = new AtomicReference<>();
            AtomicReference<String> credential = new AtomicReference<>();
            server.createContext("/v1/models", exchange -> {
                method.set(exchange.getRequestMethod());
                String header = provider.getProvider() == AiProvider.OPENAI ? "Authorization"
                    : provider.getProvider() == AiProvider.ANTHROPIC ? "x-api-key" : "x-goog-api-key";
                credential.set(exchange.getRequestHeaders().getFirst(header));
                String body = provider.getProvider() == AiProvider.GEMINI
                    ? "{\"models\":[{\"name\":\"models/z\"},{\"name\":\"models/a\",\"supportedGenerationMethods\":[\"generateContent\"]},{\"name\":\"models/embed\",\"supportedGenerationMethods\":[\"embedContent\"]},{\"name\":\"models/a\"}]}"
                    : "{\"data\":[{\"id\":\"z\"},{\"id\":\"a\"},{\"id\":\"a\"},{\"id\":\"\"}]}";
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.close();
            });
            server.start();
            try {
                AiConfig config = new AiConfig();
                config.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/v1");
                config.setApiKey("secret");
                assertEquals(java.util.List.of("a", "z"), provider.loadModels(config));
                assertEquals("GET", method.get());
                assertTrue(credential.get().endsWith("secret"));
            } finally {
                server.stop(0);
            }
        }
    }

}
