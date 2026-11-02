package com.fugary.simple.mock.service.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fugary.simple.mock.entity.mock.AiConfig;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;

class AiStreamingProviderTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private static final String ANSWER = "**你好😀**\n```json\n{\"ok\":true}\n```";

    private AbstractAiChatProvider provider(String name) {
        AbstractAiChatProvider provider = "openai".equals(name) ? new OpenAiChatProvider()
                : "anthropic".equals(name) ? new AnthropicChatProvider() : new GeminiChatProvider();
        return provider;
    }

    private AiChatRequest request() {
        AiChatRequest request = new AiChatRequest();
        request.setPrompt("查询");
        return request;
    }

    private AiConfig config(int port) {
        AiConfig config = new AiConfig();
        config.setBaseUrl("http://127.0.0.1:" + port);
        config.setApiKey("test-key");
        config.setDefaultModel("test-model");
        return config;
    }

    private String fixture(String name) throws IOException {
        try (var in = getClass().getResourceAsStream("/ai-stream/" + name + ".sse")) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"openai", "anthropic", "gemini"})
    void emitsBeforeUpstreamCompletesAndPreservesUsage(String name) throws Exception {
        String fixture = fixture(name);
        int split = fixture.indexOf("\n\n", fixture.indexOf("你好")) + 2;
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch firstText = new CountDownLatch(1);
        AtomicReference<JsonNode> requestBody = new AtomicReference<>();
        AtomicReference<String> requestPath = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            try {
                requestBody.set(mapper.readTree(exchange.getRequestBody()));
                requestPath.set(exchange.getRequestURI().toString());
                exchange.getResponseHeaders().set("Content-Type", "text/event-stream; charset=utf-8");
                exchange.sendResponseHeaders(200, 0);
                // 每个字节单独写入，覆盖中文/emoji 跨网络分片。
                for (byte value : fixture.substring(0, split).getBytes(StandardCharsets.UTF_8)) {
                    exchange.getResponseBody().write(value);
                    exchange.getResponseBody().flush();
                }
                if (!release.await(5, TimeUnit.SECONDS)) throw new IOException("No incremental callback");
                exchange.getResponseBody().write(fixture.substring(split).getBytes(StandardCharsets.UTF_8));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally { exchange.close(); }
        });
        ExecutorService worker = Executors.newSingleThreadExecutor();
        server.start();
        try {
            StringBuilder emitted = new StringBuilder();
            AiStreamSession stream = new AiStreamSession(text -> { emitted.append(text); firstText.countDown(); });
            AiChatRequest request = request();
            Future<AiChatResponse> future = worker.submit(() -> provider(name).chat(config(server.getAddress().getPort()), request, stream));
            assertThat(firstText.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(future.isDone()).isFalse();
            release.countDown();
            AiChatResponse response = future.get(5, TimeUnit.SECONDS);
            assertThat(emitted.toString()).isEqualTo(ANSWER).doesNotContain("private", "sig");
            assertThat(response.getContent()).isEqualTo(ANSWER);
            JsonNode raw = mapper.readTree(request.getTrace().rawResponse());
            assertThat(raw.path("format").asText()).isEqualTo("sse");
            assertThat(raw.path("content").asText()).isEqualTo("gemini".equals(name) ? fixture : fixture.stripTrailing() + "\n\n");
            assertThat(raw.path("truncated").asBoolean()).isFalse();
            assertThat(request.getTrace().getHttpStatus()).isEqualTo(200);
            assertThat(response.getPromptTokens()).isEqualTo(10);
            assertThat(response.getCompletionTokens()).isEqualTo(4);
            assertThat(response.getTotalTokens()).isEqualTo(14);
            if ("gemini".equals(name)) assertThat(requestPath.get()).endsWith(":streamGenerateContent?alt=sse");
            else assertThat(requestBody.get().path("stream").asBoolean()).isTrue();
        } finally {
            release.countDown();
            server.stop(0);
            worker.shutdownNow();
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {429, 200})
    void stopsReadingOversizedErrorBodyBeforeUpstreamCloses(int status) throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            try {
                exchange.getRequestBody().readAllBytes();
                exchange.getResponseHeaders().set("Content-Type", "text/plain");
                exchange.sendResponseHeaders(status, 0);
                exchange.getResponseBody().write("x".repeat(70000).getBytes(StandardCharsets.UTF_8));
                exchange.getResponseBody().flush();
                release.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally { exchange.close(); }
        });
        server.start();
        ExecutorService worker = Executors.newSingleThreadExecutor();
        AiStreamSession stream = new AiStreamSession(text -> fail("Unexpected text"));
        try {
            AiChatRequest request = request();
            Future<?> future = worker.submit(() -> provider("openai").chat(config(server.getAddress().getPort()), request, stream));
            assertThatThrownBy(() -> future.get(5, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class)
                .satisfies(failure -> assertThat(failure.getCause()).isInstanceOfSatisfying(com.fugary.simple.mock.service.ai.AiException.class, error -> {
                    assertThat(error.getMessageKey()).isEqualTo(status == 429 ? "simple.ai.httpError" : "simple.ai.invalidResponse");
                    if (status == 429) assertThat(error.getArguments()[0]).isEqualTo(429);
                }));
            assertThat(release.getCount()).isEqualTo(1);
            assertThat(request.getTrace().getHttpStatus()).isEqualTo(status);
            JsonNode raw = mapper.readTree(request.getTrace().rawResponse());
            assertThat(raw.path("content").asText()).hasSize(64000);
            assertThat(raw.path("truncated").asBoolean()).isTrue();
        } finally {
            stream.cancel();
            release.countDown();
            server.stop(0);
            worker.shutdownNow();
        }
    }

    @Test
    void preservesHttpErrorWhenDiagnosticBodyIsIncomplete() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            try {
                exchange.getRequestBody().readAllBytes();
                exchange.sendResponseHeaders(429, 100);
                exchange.getResponseBody().write("partial".getBytes(StandardCharsets.UTF_8));
                exchange.getResponseBody().flush();
            } finally { exchange.close(); }
        });
        server.start();
        ExecutorService worker = Executors.newSingleThreadExecutor();
        AiStreamSession stream = new AiStreamSession(text -> fail("Unexpected text"));
        try {
            AiChatRequest request = request();
            Future<?> future = worker.submit(() -> provider("openai").chat(config(server.getAddress().getPort()), request, stream));
            assertThatThrownBy(() -> future.get(5, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class)
                .satisfies(failure -> assertThat(failure.getCause()).isInstanceOfSatisfying(com.fugary.simple.mock.service.ai.AiException.class, error -> {
                    assertThat(error.getMessageKey()).isEqualTo("simple.ai.httpError");
                    assertThat(error.getArguments()[0]).isEqualTo(429);
                }));
            assertThat(request.getTrace().rawResponse()).isEqualTo("partial");
        } finally {
            stream.cancel();
            server.stop(0);
            worker.shutdownNow();
        }
    }

    @Test
    void abortsUpstreamWhileWaitingForFirstToken() throws Exception {
        CountDownLatch requested = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requested.countDown();
            try { release.await(5, TimeUnit.SECONDS); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            finally { exchange.close(); }
        });
        server.start();
        ExecutorService worker = Executors.newSingleThreadExecutor();
        AiStreamSession stream = new AiStreamSession(text -> fail("Unexpected text"));
        try {
            AiChatRequest request = new AiChatRequest();
            request.setPrompt("问题");
            Future<?> future = worker.submit(() -> provider("openai").chat(config(server.getAddress().getPort()), request, stream));
            assertThat(requested.await(5, TimeUnit.SECONDS)).isTrue();
            stream.cancel();
            assertThatThrownBy(() -> future.get(2, TimeUnit.SECONDS)).isInstanceOf(ExecutionException.class)
                    .hasCauseInstanceOf(CancellationException.class);
        } finally {
            release.countDown();
            server.stop(0);
            worker.shutdownNow();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"openai", "anthropic", "gemini"})
    void rejectsTruncatedAndErrorStreams(String name) throws Exception {
        AiStreamAccumulator accumulator = new AiStreamAccumulator(com.fugary.simple.mock.service.ai.AiProvider.valueOf(name.toUpperCase()), mapper, new AiStreamSession(text -> {}));
        assertThatThrownBy(accumulator::result).isInstanceOf(IOException.class).hasMessageContaining("中断");
        assertThatThrownBy(() -> accumulator.accept("{\"error\":{\"message\":\"upstream failed\"}}"))
                .isInstanceOf(IOException.class).hasMessageContaining("upstream error");
        assertThatThrownBy(() -> accumulator.accept("{broken" )).isInstanceOf(IOException.class);
    }
    @Test
    void anthropicUsageUpdateDoesNotEraseTruncatedFinishReason() throws Exception {
        AiStreamAccumulator accumulator = new AiStreamAccumulator(
            com.fugary.simple.mock.service.ai.AiProvider.ANTHROPIC, mapper, new AiStreamSession(text -> {}));
        accumulator.accept("{\"type\":\"message_delta\",\"delta\":{\"stop_reason\":\"max_tokens\"}}");
        accumulator.accept("{\"type\":\"message_delta\",\"usage\":{\"output_tokens\":4}}");
        accumulator.accept("{\"type\":\"message_stop\"}");
        assertThatThrownBy(accumulator::result).isInstanceOf(IOException.class).hasMessageContaining("max_tokens");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ANTHROPIC", "GEMINI"})
    void rejectsOpenAiEndMarkerForOtherProtocols(String name) {
        AiStreamAccumulator accumulator = new AiStreamAccumulator(
            com.fugary.simple.mock.service.ai.AiProvider.valueOf(name), mapper, new AiStreamSession(text -> {}));
        assertThatThrownBy(() -> accumulator.accept("[DONE]")).isInstanceOf(IOException.class);
        assertThatThrownBy(accumulator::result).isInstanceOf(IOException.class);
    }

}
