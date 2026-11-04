package com.fugary.simple.mock.service.ai.provider;

import com.fugary.simple.mock.entity.mock.AiConfig;
import com.fugary.simple.mock.service.ai.AiException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiUpstreamErrorTest {
    @ParameterizedTest
    @ValueSource(strings = {"openai", "anthropic", "gemini"})
    void preservesUsefulErrorsWithoutCredentialsForHttpAndStreams(String name) throws Exception {
        AbstractAiChatProvider provider = "openai".equals(name) ? new OpenAiChatProvider()
                : "anthropic".equals(name) ? new AnthropicChatProvider() : new GeminiChatProvider();
        AtomicReference<String> body = new AtomicReference<>();
        AtomicInteger status = new AtomicInteger(429);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            byte[] bytes = body.get().getBytes(StandardCharsets.UTF_8);
            boolean sse = body.get().startsWith("data:");
            exchange.getResponseHeaders().set("Content-Type", sse ? "text/event-stream" : "application/json");
            exchange.sendResponseHeaders(sse ? 200 : status.get(), bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        try {
            AiConfig config = new AiConfig();
            config.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
            config.setDefaultModel("model");
            config.setApiKey("private-key");
            for (Map.Entry<String, String> sample : Map.of(
                    "{\"error\":{\"message\":\"Quota exceeded: private-key\"}}", "Quota exceeded: [REDACTED]",
                    "{\"detail\":\"Model unavailable\"}", "Model unavailable",
                    "{\"errors\":[{\"message\":\"Invalid model\"}]}", "Invalid model",
                    "Gateway unavailable: private-key", "Gateway unavailable: [REDACTED]",
                    "<html>private-key</html>", "",
                    "{malformed private-key", "",
                    "{\"error\":\"private-key " + "x".repeat(2200) + "\"}", "[REDACTED] " + "x".repeat(1986) + "...").entrySet()) {
                body.set(sample.getKey());
                for (boolean streaming : new boolean[]{false, true}) {
                    AiChatRequest request = new AiChatRequest();
                    request.setPrompt("test");
                    AiException error = assertThrows(AiException.class, () -> provider.chat(config, request,
                            streaming ? new AiStreamSession(text -> {}) : null));
                    assertThat(error.getMessageKey()).isEqualTo("simple.ai.httpError");
                    assertThat(error.getArguments()[0]).isEqualTo(429);
                    assertThat(error.getDetail() == null ? "" : error.getDetail()).isEqualTo(sample.getValue());
                    assertThat(error.getErrorMessage()).doesNotContain("private-key", "<html>");
                    assertThat(request.getTrace().rawResponse()).isEqualTo(sample.getKey());
                }
            }
            body.set("{\"error\":{\"message\":\"Unknown model private-key\"}}");
            AiException modelError = assertThrows(AiException.class, () -> provider.loadModels(config));
            assertThat(modelError.getDetail()).isEqualTo("Unknown model [REDACTED]");
            status.set(200);
            for (boolean streaming : new boolean[]{false, true}) {
                AiChatRequest request = new AiChatRequest();
                request.setPrompt("test");
                AiException error = assertThrows(AiException.class, () -> provider.chat(config, request,
                        streaming ? new AiStreamSession(text -> {}) : null));
                assertThat(error.getMessageKey()).isEqualTo(streaming ? "simple.ai.invalidResponse" : "simple.ai.requestFailed");
                assertThat(error.getDetail()).isEqualTo("Unknown model [REDACTED]");
            }
            body.set("data: {\"error\":{\"message\":\"Overloaded private-key\"}}\n\n");
            AiChatRequest request = new AiChatRequest();
            request.setPrompt("test");
            AiException streamError = assertThrows(AiException.class,
                    () -> provider.chat(config, request, new AiStreamSession(text -> {})));
            assertThat(streamError.getMessageKey()).isEqualTo("simple.ai.requestFailed");
            assertThat(streamError.getDetail()).isEqualTo("Overloaded [REDACTED]");
        } finally {
            server.stop(0);
        }
    }
}
