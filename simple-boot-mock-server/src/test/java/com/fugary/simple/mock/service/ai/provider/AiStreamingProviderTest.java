package com.fugary.simple.mock.service.ai.provider;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fugary.simple.mock.entity.mock.AiConfig;
import com.fugary.simple.mock.service.ai.AiException;
import com.fugary.simple.mock.service.ai.AiProvider;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.anything;

class AiStreamingProviderTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private static final String ANSWER = "**你好😀**\n```json\n{\"ok\":true}\n```";

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void distinguishesConnectionFailureAndLogsOriginalCause(boolean streaming) throws Exception {
        int port;
        try (ServerSocket socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }
        Logger logger = (Logger) LoggerFactory.getLogger(AbstractAiChatProvider.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        AiChatRequest request = request();
        try {
            assertThatThrownBy(() -> provider("openai").chat(config(port), request,
                    streaming ? new AiStreamSession(text -> {}) : null))
                .isInstanceOfSatisfying(AiException.class, error -> {
                    assertThat(error.getMessageKey()).isEqualTo("simple.ai.connectionFailed");
                    assertThat(error).hasRootCauseInstanceOf(ConnectException.class);
                    assertThat(appender.list).anySatisfy(event -> {
                        assertThat(event.getFormattedMessage()).contains("simple.ai.connectionFailed");
                        assertThat(event.getThrowableProxy()).isNotNull();
                        assertThat(event.getThrowableProxy().getClassName()).isEqualTo(error.getCause().getClass().getName());
                    });
                });
            assertThat(request.getTrace().getHttpStatus()).isNull();
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void distinguishesMalformedResponseFromConnectionFailure(boolean streaming) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            try {
                exchange.getRequestBody().readAllBytes();
                exchange.getResponseHeaders().set("Content-Type", streaming ? "text/event-stream" : "application/json");
                byte[] body = (streaming ? "data: {broken\n\n" : "{broken").getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
            } finally { exchange.close(); }
        });
        server.start();
        AiChatRequest request = request();
        try {
            assertThatThrownBy(() -> provider("openai").chat(config(server.getAddress().getPort()), request,
                    streaming ? new AiStreamSession(text -> {}) : null))
                .isInstanceOfSatisfying(AiException.class, error -> {
                    assertThat(error.getMessageKey()).isEqualTo("simple.ai.invalidResponse");
                    assertThat(error.getCause()).isInstanceOf(JsonProcessingException.class);
                });
            assertThat(request.getTrace().getHttpStatus()).isEqualTo(200);
            assertThat(request.getTrace().rawResponse()).contains("{broken");
        } finally { server.stop(0); }
    }

    @Test
    void distinguishesInterruptedResponseReadFromMalformedResponse() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            try {
                exchange.getRequestBody().readAllBytes();
                exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
                exchange.sendResponseHeaders(200, 100);
                exchange.getResponseBody().write(": heartbeat\n\n".getBytes(StandardCharsets.UTF_8));
                exchange.getResponseBody().flush();
            } finally { exchange.close(); }
        });
        server.start();
        ExecutorService worker = Executors.newSingleThreadExecutor();
        AiStreamSession stream = new AiStreamSession(text -> fail("Unexpected text"));
        AiChatRequest request = request();
        try {
            Future<?> future = worker.submit(() -> provider("openai").chat(config(server.getAddress().getPort()), request, stream));
            assertThatThrownBy(() -> future.get(5, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class)
                .satisfies(failure -> assertThat(failure.getCause()).isInstanceOfSatisfying(AiException.class, error -> {
                    assertThat(error.getMessageKey()).isEqualTo("simple.ai.responseReadFailed");
                    assertThat(error.getCause()).isInstanceOf(IOException.class);
                }));
            assertThat(request.getTrace().getHttpStatus()).isEqualTo(200);
            assertThat(request.getTrace().rawResponse()).contains("heartbeat");
        } finally {
            stream.cancel();
            server.stop(0);
            worker.shutdownNow();
        }
    }

    @Test
    void classifiesSynchronousBodyReadFailureAfterReceivingStatus() {
        AbstractAiChatProvider provider = provider("openai");
        RestTemplate client = (RestTemplate) ReflectionTestUtils.getField(provider, "restTemplate");
        MockRestServiceServer server = MockRestServiceServer.bindTo(client).build();
        IOException cause = new IOException("connection reset while reading body");
        server.expect(anything())
            .andRespond(request -> new MockClientHttpResponse(new byte[0], HttpStatus.OK) {
                @Override
                public InputStream getBody() {
                    return new InputStream() {
                        @Override
                        public int read() throws IOException { throw cause; }
                    };
                }
            });
        AiChatRequest request = request();
        assertThatThrownBy(() -> provider.chat(config(1), request))
            .isInstanceOfSatisfying(AiException.class, error -> {
                assertThat(error.getMessageKey()).isEqualTo("simple.ai.responseReadFailed");
                assertThat(error).hasRootCause(cause);
            });
        assertThat(request.getTrace().getHttpStatus()).isEqualTo(200);
        server.verify();
    }

    @Test
    void preservesNestedTimeoutClassification() {
        AbstractAiChatProvider provider = provider("openai");
        RestTemplate client = mock(RestTemplate.class);
        ReflectionTestUtils.setField(provider, "restTemplate", client);
        when(client.execute(any(URI.class),
                eq(HttpMethod.POST),
                nullable(RequestCallback.class),
                any(ResponseExtractor.class)))
            .thenThrow(new ResourceAccessException("read failed",
                    new IOException("wrapped", new SocketTimeoutException("timed out"))));
        assertThatThrownBy(() -> provider.chat(config(1), request()))
            .isInstanceOfSatisfying(AiException.class, error -> {
                assertThat(error.getMessageKey()).isEqualTo("simple.ai.timeout");
                assertThat(error).hasRootCauseInstanceOf(SocketTimeoutException.class);
            });
    }

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
            if ("anthropic".equals(name)) assertThat(requestBody.get().path("max_tokens").asInt()).isEqualTo(8192);
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
                .satisfies(failure -> assertThat(failure.getCause()).isInstanceOfSatisfying(AiException.class, error -> {
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
                .satisfies(failure -> assertThat(failure.getCause()).isInstanceOfSatisfying(AiException.class, error -> {
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
        AiStreamAccumulator accumulator = new AiStreamAccumulator(AiProvider.valueOf(name.toUpperCase()), mapper, new AiStreamSession(text -> {}));
        assertThatThrownBy(accumulator::result).isInstanceOf(IOException.class).hasMessageContaining("中断");
        assertThatThrownBy(() -> accumulator.accept("{\"error\":{\"message\":\"upstream failed\"}}"))
                .isInstanceOf(IOException.class).hasMessageContaining("upstream error");
        assertThatThrownBy(() -> accumulator.accept("{broken" )).isInstanceOf(IOException.class);
    }
    @Test
    void anthropicUsageUpdateDoesNotEraseTruncatedFinishReason() throws Exception {
        AiStreamAccumulator accumulator = new AiStreamAccumulator(
            AiProvider.ANTHROPIC, mapper, new AiStreamSession(text -> {}));
        accumulator.accept("{\"type\":\"message_delta\",\"delta\":{\"stop_reason\":\"max_tokens\"}}");
        accumulator.accept("{\"type\":\"message_delta\",\"usage\":{\"output_tokens\":4}}");
        accumulator.accept("{\"type\":\"message_stop\"}");
        assertThatThrownBy(accumulator::result).isInstanceOf(IOException.class).hasMessageContaining("max_tokens");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ANTHROPIC", "GEMINI"})
    void rejectsOpenAiEndMarkerForOtherProtocols(String name) {
        AiStreamAccumulator accumulator = new AiStreamAccumulator(
            AiProvider.valueOf(name), mapper, new AiStreamSession(text -> {}));
        assertThatThrownBy(() -> accumulator.accept("[DONE]")).isInstanceOf(IOException.class);
        assertThatThrownBy(accumulator::result).isInstanceOf(IOException.class);
    }

}
