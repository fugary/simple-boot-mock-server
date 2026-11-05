package com.fugary.simple.mock.push;

import com.fugary.simple.mock.config.script.ScriptEngineConfig;
import com.fugary.simple.mock.config.SimpleMockConfigProperties;
import com.fugary.simple.mock.entity.mock.MockData;
import com.fugary.simple.mock.script.JavaScriptEngineFactory;
import com.fugary.simple.mock.utils.MockDiagnoseContext;
import com.fugary.simple.mock.web.vo.result.MockDiagnoseVo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.scheduling.concurrent.ThreadPoolExecutorFactoryBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.http.ResponseEntity;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.SimpleScriptContext;
import javax.script.ScriptException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.Collectors;

import static com.fugary.simple.mock.contants.MockDiagnoseConstants.GROUP_POST_PROCESSOR;
import static com.fugary.simple.mock.contants.MockDiagnoseConstants.KEY_MESSAGE;
import static com.fugary.simple.mock.contants.MockDiagnoseConstants.KEY_URL;
import static com.fugary.simple.mock.contants.MockDiagnoseConstants.STAGE_EXTERNAL_FETCH;
import static com.fugary.simple.mock.utils.MockJsUtils.formatScriptError;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultScriptWithFetchProviderImplTest {

    private DefaultScriptWithFetchProviderImpl fetchProvider;

    private ExecutorService fetchExecutor;

    private ScriptEngine scriptEngine;

    @BeforeEach
    void setUp() throws Exception {
        fetchExecutor = Executors.newFixedThreadPool(4);
        fetchProvider = new DefaultScriptWithFetchProviderImpl();
        fetchProvider.setFetchScriptThreadPool(fetchExecutor);
        fetchProvider.setMockPushProcessor(mockParams -> {
            sleepQuietly(50);
            String body = "{\"url\":\"" + mockParams.getTargetUrl() + "\"}";
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body.getBytes(StandardCharsets.UTF_8));
        });

        JavaScriptEngineFactory factory = new JavaScriptEngineFactory();
        factory.setScriptWithFetchProvider(fetchProvider);
        scriptEngine = factory.create();
    }

    @AfterEach
    void tearDown() {
        MockDiagnoseContext.clear();
        fetchExecutor.shutdownNow();
    }

    @Test
    void sequentialAwaitFetchShouldRecordEveryFetchDiagnose() throws Exception {
        MockDiagnoseVo diagnose = new MockDiagnoseVo();
        MockDiagnoseContext.set(diagnose);

        Object result = evalSequentialFetch();

        assertEquals("https://example.com/first|https://example.com/second", result);
        List<MockDiagnoseVo.Step> fetchSteps = fetchSteps(diagnose);
        assertEquals(2, fetchSteps.size());
        assertEquals("https://example.com/first", fetchSteps.get(0).getDetails().get(KEY_URL));
        assertEquals("https://example.com/second", fetchSteps.get(1).getDetails().get(KEY_URL));
    }

    @Test
    void sequentialAwaitFetchShouldKeepPostProcessorStageGroup() throws Exception {
        MockDiagnoseVo diagnose = new MockDiagnoseVo();
        diagnose.setDataInfo(new MockData());
        MockDiagnoseContext.set(diagnose);
        MockDiagnoseContext.setPostProcessorStageGroup(GROUP_POST_PROCESSOR);

        Object result = evalSequentialFetch();

        assertEquals("https://example.com/first|https://example.com/second", result);
        List<MockDiagnoseVo.Step> fetchSteps = fetchSteps(diagnose);
        assertEquals(2, fetchSteps.size());
        assertEquals(GROUP_POST_PROCESSOR, fetchSteps.get(0).getStageGroup());
        assertEquals(GROUP_POST_PROCESSOR, fetchSteps.get(1).getStageGroup());
    }

    @Test
    void requireShouldNotFetchBareCommonJsModuleProbe() throws Exception {
        String libraryUrl = "https://cdn.example.com/crypto-js.min.js";
        String targetUrl = "https://example.com/asset.js";
        fetchProvider.setMockPushProcessor(mockParams -> {
            sleepQuietly(50);
            if (libraryUrl.equals(mockParams.getTargetUrl())) {
                String script = "let cryptoProbeError;\n"
                        + "try {\n"
                        + "  require('crypto');\n"
                        + "} catch (e) {\n"
                        + "  cryptoProbeError = e.message;\n"
                        + "}\n"
                        + "module.exports = { loaded: true, cryptoProbeError: cryptoProbeError };";
                return ResponseEntity.ok()
                        .contentType(MediaType.valueOf("application/javascript"))
                        .body(script.getBytes(StandardCharsets.UTF_8));
            }
            String body = "{\"url\":\"" + mockParams.getTargetUrl() + "\"}";
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body.getBytes(StandardCharsets.UTF_8));
        });
        MockDiagnoseVo diagnose = new MockDiagnoseVo();
        MockDiagnoseContext.set(diagnose);

        Object result = fetchProvider.internalEval(
                "(async function(){\n"
                        + "  const lib = await require('" + libraryUrl + "');\n"
                        + "  const response = await fetch('" + targetUrl + "');\n"
                        + "  const responseBody = await response.json();\n"
                        + "  return lib.loaded + '|' + responseBody.url + '|' + lib.cryptoProbeError;\n"
                        + "})()",
                scriptEngine,
                createScriptContext());

        assertEquals("true|" + targetUrl + "|Unsupported require module specifier: crypto", result);
        List<MockDiagnoseVo.Step> fetchSteps = fetchSteps(diagnose);
        assertEquals(2, fetchSteps.size());
        assertEquals(libraryUrl, fetchSteps.get(0).getDetails().get(KEY_URL));
        assertEquals(targetUrl, fetchSteps.get(1).getDetails().get(KEY_URL));
    }

    @Test
    void invalidJsonResponseShouldReportJsonCallSourceLine() {
        String targetUrl = "https://example.com/invalid-json";
        fetchProvider.setMockPushProcessor(mockParams -> ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body("{invalid-json".getBytes(StandardCharsets.UTF_8)));
        String script = "(async function(){\n"
                + "  const response = await fetch('" + targetUrl + "');\n"
                + "  const responseBody = await response.json();\n"
                + "  return responseBody.url;\n"
                + "})()";

        ScriptException exception = assertThrows(ScriptException.class, () -> fetchProvider.internalEval(
                script,
                scriptEngine,
                createScriptContext()));
        String result = formatScriptError(script, exception);

        assertTrue(result.contains("Script execution failed: Invalid JSON in response"), result);
        assertTrue(result.contains("Source: const responseBody = await response.json();"), result);
        assertFalse(result.contains("__simpleMockScriptLine__"), result);
    }

    @Test
    void fetchTimeoutShouldReportUrlTimeoutAndSourceLine() {
        String targetUrl = "https://example.com/timeout";
        MockDiagnoseVo diagnose = new MockDiagnoseVo();
        MockDiagnoseContext.set(diagnose);
        String script = "(async function(){\n"
                + "  const response = await fetch('" + targetUrl + "', { timeout: 20 });\n"
                + "  return await response.text();\n"
                + "})()";
        String expectedError = "Fetch request timeout after 20 ms: GET " + targetUrl;

        ScriptException exception = assertThrows(ScriptException.class, () -> fetchProvider.internalEval(
                script,
                scriptEngine,
                createScriptContext()));
        String result = formatScriptError(script, exception);

        assertTrue(result.contains("Script execution failed: " + expectedError), result);
        assertTrue(result.contains("Source: const response = await fetch('" + targetUrl
                + "', { timeout: 20 });"), result);
        assertFalse(result.contains("TimeoutException"), result);
        assertFalse(result.contains("__simpleMockScriptLine__"), result);
        List<MockDiagnoseVo.Step> fetchSteps = fetchSteps(diagnose);
        assertEquals(1, fetchSteps.size());
        assertEquals(targetUrl, fetchSteps.get(0).getDetails().get(KEY_URL));
        assertTrue(String.valueOf(fetchSteps.get(0).getDetails().get(KEY_MESSAGE))
                .contains(expectedError), fetchSteps.get(0).getDetails().toString());
    }

    @Test
    void fetchCompletingDuringSynchronousExecutionShouldNotContendWithContext() throws Exception {
        fetchExecutor.shutdownNow();
        fetchExecutor = Executors.newSingleThreadExecutor();
        fetchProvider.setFetchScriptThreadPool(fetchExecutor);
        ScriptContext context = createScriptContext();
        context.getBindings(ScriptContext.GLOBAL_SCOPE).put("waitForHttp", (Runnable) () -> {
            try {
                // 同一线程池的后续任务开始时，前一个请求已完成且回调已入队。
                fetchExecutor.submit(() -> {}).get(3, TimeUnit.SECONDS);
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        });
        fetchProvider.internalEval(
                "globalThis.done = false;\n"
                        + "fetch('https://example.com/overlap').then(() => { globalThis.done = true; });\n"
                        + "waitForHttp.run();\nvoid 0;",
                scriptEngine,
                context);

        assertEquals(Boolean.TRUE, fetchProvider.internalEval("globalThis.done", scriptEngine, context));

        Object awaitedResult = fetchProvider.internalEval(
                "(async function(){\n"
                        + "  const pending = fetch('https://example.com/overlap-async').then(r => r.json());\n"
                        + "  waitForHttp.run();\n"
                        + "  const data = await pending;\n"
                        + "  return data.url;\n"
                        + "})()",
                scriptEngine,
                context);
        assertEquals("https://example.com/overlap-async", awaitedResult);
    }

    @Test
    void parallelFetchCallsShouldExecuteCallbacksSeriallyOnEngineThread() throws Exception {
        CountDownLatch allStarted = new CountDownLatch(4);
        fetchProvider.setMockPushProcessor(mockParams -> {
            allStarted.countDown();
            try {
                assertTrue(allStarted.await(3, TimeUnit.SECONDS));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            String body = "{\"url\":\"" + mockParams.getTargetUrl() + "\"}";
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body.getBytes(StandardCharsets.UTF_8));
        });
        MockDiagnoseVo diagnose = new MockDiagnoseVo();
        MockDiagnoseContext.set(diagnose);
        Object result = fetchProvider.internalEval(
                "(async function(){\n"
                        + "  const urls = ['https://example.com/1', 'https://example.com/2', "
                        + "'https://example.com/3', 'https://example.com/4'];\n"
                        + "  const responses = await Promise.all(urls.map(u => fetch(u)));\n"
                        + "  const bodies = await Promise.all(responses.map(r => r.json()));\n"
                        + "  return bodies.map(b => b.url).join('|');\n"
                        + "})()",
                scriptEngine,
                createScriptContext());

        assertEquals("https://example.com/1|https://example.com/2|https://example.com/3|https://example.com/4", result);
        assertEquals(4, fetchSteps(diagnose).size());
    }

    @Test
    void lateFetchCallbackAfterScriptEndsShouldNotAccessEngineOrNextExecutionState() throws Exception {
        CountDownLatch slowFetchStarted = new CountDownLatch(1);
        CountDownLatch slowFetchFinished = new CountDownLatch(1);
        AtomicBoolean taskInterrupted = new AtomicBoolean(false);
        CountDownLatch releaseHttp = new CountDownLatch(1);
        fetchProvider.setMockPushProcessor(mockParams -> {
            slowFetchStarted.countDown();
            try {
                releaseHttp.await(4, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                taskInterrupted.set(true);
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(150));
            }
            slowFetchFinished.countDown();
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("ok".getBytes(StandardCharsets.UTF_8));
        });

        MockDiagnoseVo diagnose = new MockDiagnoseVo();
        MockDiagnoseContext.set(diagnose);
        ScriptContext context = createScriptContext();

        context.getBindings(ScriptContext.GLOBAL_SCOPE).put("started", slowFetchStarted);
        fetchProvider.internalEval(
                "globalThis.requestTag = 'A';\n"
                        + "globalThis.seen = 'pending';\n"
                        + "fetch('https://example.com/late').then(() => { globalThis.seen = globalThis.requestTag; });\n"
                        + "started.await();\n"
                        + "'done';",
                scriptEngine,
                context);
        releaseHttp.countDown();

        fetchProvider.internalEval("globalThis.requestTag = 'B';", scriptEngine, context);
        assertTrue(slowFetchFinished.await(1, TimeUnit.SECONDS));
        sleepQuietly(50);

        assertTrue(taskInterrupted.get());
        assertEquals("pending", fetchProvider.internalEval("globalThis.seen", scriptEngine, context));
        assertTrue(fetchSteps(diagnose).isEmpty());
    }

    @Test
    void lateFetchCallbackAfterExecuteValueTimeoutShouldBeCancelledAndBlocked() throws Exception {
        CountDownLatch slowFetchFinished = new CountDownLatch(1);
        AtomicBoolean taskInterrupted = new AtomicBoolean(false);
        fetchProvider.setExecutionTimeoutSeconds(1);
        fetchProvider.setMockPushProcessor(mockParams -> {
            try {
                Thread.sleep(1200);
            } catch (InterruptedException e) {
                taskInterrupted.set(true);
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(100));
            }
            slowFetchFinished.countDown();
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("ok".getBytes(StandardCharsets.UTF_8));
        });

        ScriptContext context = createScriptContext();
        assertThrows(ScriptException.class, () -> fetchProvider.internalEval(
                "globalThis.timeoutTag = 'A';\n"
                        + "globalThis.timeoutSeen = 'pending';\n"
                        + "fetch('https://example.com/slow-timeout').then(() => { globalThis.timeoutSeen = globalThis.timeoutTag; });",
                scriptEngine,
                context));

        fetchProvider.setExecutionTimeoutSeconds(30);
        fetchProvider.internalEval("globalThis.timeoutTag = 'B';", scriptEngine, context);
        assertTrue(slowFetchFinished.await(1, TimeUnit.SECONDS));
        sleepQuietly(50);

        assertTrue(taskInterrupted.get());
        assertEquals("pending", fetchProvider.internalEval("globalThis.timeoutSeen", scriptEngine, context));
    }

    @Test
    void infiniteLoopInInitialEvalOrAfterAwaitFetchShouldBeInterruptedByTimeout() throws Exception {
        fetchProvider.setExecutionTimeoutSeconds(1);
        ScriptContext context = createScriptContext();

        assertTimeoutPreemptively(Duration.ofSeconds(3), () -> {
            ScriptException ex = assertThrows(ScriptException.class, () -> fetchProvider.internalEval(
                    "while (true) {}",
                    scriptEngine,
                    context));
            assertTrue(ex.getMessage().contains("timed out"), ex.getMessage());
        });

        assertTimeoutPreemptively(Duration.ofSeconds(3), () -> {
            ScriptException ex = assertThrows(ScriptException.class, () -> fetchProvider.internalEval(
                    "(async function() {\n"
                            + "  await fetch('https://example.com/fast');\n"
                            + "  while (true) {}\n"
                            + "})()",
                    scriptEngine,
                    context));
            assertTrue(ex.getMessage().contains("timed out"), ex.getMessage());
        });

        fetchProvider.setExecutionTimeoutSeconds(30);
        assertEquals("recovered", fetchProvider.internalEval("'recovered'", scriptEngine, context));
    }

    @Test
    void innerBodyPromiseTimeoutShouldNotCancelOuterScriptTimeoutProtection() throws Exception {
        fetchProvider.setBodyExecutionTimeoutSeconds(1);
        fetchProvider.setExecutionTimeoutSeconds(2);
        ScriptContext context = createScriptContext();

        assertTimeoutPreemptively(Duration.ofSeconds(4), () -> {
            ScriptException ex = assertThrows(ScriptException.class, () -> fetchProvider.internalEval(
                    "(async () => {try {await fetch('/post', {body:new Promise(() => {})});}"
                            + "catch (e) {} while (true) {} })()",
                    scriptEngine,
                    context));
            assertTrue(ex.getMessage().contains("timed out"), ex.getMessage());
        });
    }

    @Test
    void resolvedPromiseBodyShouldBeSentOnTheProductionContext() throws Exception {
        AtomicReference<String> posted = new AtomicReference<>();
        fetchProvider.setMockPushProcessor(params -> {
            if ("/post".equals(params.getTargetUrl())) {
                posted.set(params.getRequestBody());
            }
            return ResponseEntity.ok("valid".getBytes(StandardCharsets.UTF_8));
        });
        for (String body : List.of("Promise.resolve('valid')", "fetch('/source').then(r => r.text())")) {
            posted.set(null);
            Object status = fetchProvider.internalEval(
                    "fetch('/post', {method:'POST', body:" + body + "}).then(r => r.status)",
                    scriptEngine, scriptEngine.getContext());
            assertEquals(200, status);
            assertEquals("valid", posted.get());
        }
    }

    @Test
    void cancelledAndTimedOutFetchesShouldFreeTheOnlyQueueSlot() throws Exception {
        SimpleMockConfigProperties properties = new SimpleMockConfigProperties();
        properties.getPool().setFetchScriptMaxSize(1);
        ScriptEngineConfig config = new ScriptEngineConfig();
        ReflectionTestUtils.setField(config, "simpleMockConfigProperties", properties);
        ThreadPoolExecutorFactoryBean bean = config.fetchScriptThreadPool();
        bean.afterPropertiesSet();
        CountDownLatch release = new CountDownLatch(1);
        try {
            ExecutorService executor = bean.getObject();
            executor.submit(() -> { release.await(); return null; });
            fetchProvider.setFetchScriptThreadPool(executor);
            fetchProvider.setMockPushProcessor(params -> { throw new AssertionError("Unexpected upstream request"); });
            fetchProvider.internalEval("fetch('/abandoned'); 'done'", scriptEngine, scriptEngine.getContext());
            Object timeout = fetchProvider.internalEval("fetch('/queued', {timeout: 50}).catch(String)",
                    scriptEngine, scriptEngine.getContext());
            assertTrue(timeout.toString().contains("Fetch request timeout"), timeout.toString());
            // 工作线程仍然阻塞；再次入队成功证明超时任务也已移除。
            executor.submit(() -> {});
            Object error = fetchProvider.internalEval("fetch('/full').then(() => 'sent', e => String(e))",
                    scriptEngine, scriptEngine.getContext());
            assertTrue(error.toString().contains("Fetch queue is full"), error.toString());
        } finally {
            release.countDown();
            bean.destroy();
        }
    }

    @Test
    void rejectedOrTimedOutBodyShouldNeverSendThePost() throws Exception {
        AtomicBoolean sent = new AtomicBoolean();
        fetchProvider.setMockPushProcessor(params -> {
            sent.set(true);
            return ResponseEntity.ok(new byte[0]);
        });
        fetchProvider.setBodyExecutionTimeoutSeconds(1);
        String script = "fetch('/post', {method:'POST', body:%s}).then(() => 'sent', e => String(e))";
        Object failure = fetchProvider.internalEval(String.format(script, "Promise.reject('invalid body')"),
                scriptEngine, scriptEngine.getContext());
        assertTrue(failure.toString().contains("invalid body"), failure.toString());
        Object timeout = fetchProvider.internalEval(String.format(script, "new Promise(r => globalThis.finishBody = r)"),
                scriptEngine, scriptEngine.getContext());
        assertTrue(timeout.toString().contains("Fetch body timed out"), timeout.toString());
        fetchProvider.internalEval("finishBody('late')", scriptEngine, scriptEngine.getContext());
        assertFalse(sent.get());
    }

    @Test
    void totalDeadlineShouldSurviveCaughtBodyTimeoutAndEngineReuse() throws Exception {
        fetchProvider.setExecutionTimeoutSeconds(1);
        fetchProvider.setBodyExecutionTimeoutSeconds(5);
        ScriptContext context = createScriptContext();
        assertTimeoutPreemptively(Duration.ofSeconds(3), () -> {
            ScriptException ex = assertThrows(ScriptException.class, () -> fetchProvider.internalEval(
                    "(async () => {try { await fetch('/post', {body:new Promise(() => {})}); }"
                            + " catch (e) {} while (true) {} })()", scriptEngine, context));
            assertTrue(ex.getMessage().contains("timed out"), ex.getMessage());
        });
        assertEquals("ok", fetchProvider.internalEval("'ok'", scriptEngine, context));
    }

    @Test
    void synchronousModeShouldAlsoInterruptExpiredScripts() {
        fetchProvider.setExecutionTimeoutSeconds(1);
        ScriptContext context = createScriptContext();
        assertTimeoutPreemptively(Duration.ofSeconds(3), () -> {
            ScriptException ex = assertThrows(ScriptException.class,
                    () -> fetchProvider.eval("while (true) {}", scriptEngine, context, false));
            assertTrue(ex.getMessage().contains("timed out"), ex.getMessage());
            assertEquals("ok", fetchProvider.eval("'ok'", scriptEngine, context, false));
        });
    }

    @Test
    void closingExecutionScopeShouldDeterministicallyCancelFetchResultFuture() throws Exception {
        AtomicReference<CompletableFuture<DefaultScriptWithFetchProviderImpl.FetchResponseData>> capturedFuture
                = new AtomicReference<>();
        DefaultScriptWithFetchProviderImpl spyFetchProvider = new DefaultScriptWithFetchProviderImpl() {
            @Override
            CompletableFuture<FetchResponseData> createFetchFuture() {
                CompletableFuture<FetchResponseData> future = super.createFetchFuture();
                capturedFuture.set(future);
                return future;
            }
        };
        spyFetchProvider.setFetchScriptThreadPool(fetchExecutor);
        CountDownLatch releaseHttp = new CountDownLatch(1);
        spyFetchProvider.setMockPushProcessor(mockParams -> {
            try {
                releaseHttp.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return ResponseEntity.ok("ok".getBytes(StandardCharsets.UTF_8));
        });
        JavaScriptEngineFactory factory = new JavaScriptEngineFactory();
        factory.setScriptWithFetchProvider(spyFetchProvider);
        ScriptEngine engine = factory.create();

        ScriptContext context = new SimpleScriptContext();
        context.setBindings(engine.getBindings(ScriptContext.GLOBAL_SCOPE), ScriptContext.GLOBAL_SCOPE);
        try {
            spyFetchProvider.internalEval(
                    "globalThis.lateCalled = false;\n"
                            + "fetch('https://example.com/long-timeout', { timeout: 600000 })"
                            + ".then(() => { globalThis.lateCalled = true; });\n"
                            + "'returned-early';",
                    engine,
                    context);

            assertNotNull(capturedFuture.get());
            assertTrue(capturedFuture.get().isDone());
            assertTrue(capturedFuture.get().isCancelled());
        } finally {
            releaseHttp.countDown();
        }
        assertEquals(Boolean.FALSE, spyFetchProvider.internalEval("globalThis.lateCalled", engine, context));
    }

    private ScriptContext createScriptContext() {
        ScriptContext context = new SimpleScriptContext();
        context.setBindings(scriptEngine.getBindings(ScriptContext.GLOBAL_SCOPE), ScriptContext.GLOBAL_SCOPE);
        return context;
    }

    private Object evalSequentialFetch() throws Exception {
        return fetchProvider.internalEval(
                "(async function(){\n"
                        + "  const first = await fetch('https://example.com/first');\n"
                        + "  const firstBody = await first.json();\n"
                        + "  const second = await fetch('https://example.com/second');\n"
                        + "  const secondBody = await second.json();\n"
                        + "  return firstBody.url + '|' + secondBody.url;\n"
                        + "})()",
                scriptEngine,
                createScriptContext());
    }

    private List<MockDiagnoseVo.Step> fetchSteps(MockDiagnoseVo diagnose) {
        return diagnose.getSteps().stream()
                .filter(step -> STAGE_EXTERNAL_FETCH.equals(step.getStage()))
                .collect(Collectors.toList());
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
