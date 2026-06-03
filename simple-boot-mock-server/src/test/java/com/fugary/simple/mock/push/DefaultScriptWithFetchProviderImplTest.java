package com.fugary.simple.mock.push;

import com.fugary.simple.mock.entity.mock.MockData;
import com.fugary.simple.mock.script.JavaScriptEngineFactory;
import com.fugary.simple.mock.utils.MockDiagnoseContext;
import com.fugary.simple.mock.web.vo.result.MockDiagnoseVo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.SimpleScriptContext;
import javax.script.ScriptException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static com.fugary.simple.mock.contants.MockDiagnoseConstants.GROUP_POST_PROCESSOR;
import static com.fugary.simple.mock.contants.MockDiagnoseConstants.KEY_MESSAGE;
import static com.fugary.simple.mock.contants.MockDiagnoseConstants.KEY_URL;
import static com.fugary.simple.mock.contants.MockDiagnoseConstants.STAGE_EXTERNAL_FETCH;
import static com.fugary.simple.mock.utils.MockJsUtils.formatScriptError;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultScriptWithFetchProviderImplTest {

    private DefaultScriptWithFetchProviderImpl fetchProvider;

    private ExecutorService fetchExecutor;

    private ScriptEngine scriptEngine;

    @BeforeEach
    void setUp() throws Exception {
        fetchExecutor = Executors.newFixedThreadPool(2);
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
