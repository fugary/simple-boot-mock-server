package com.fugary.simple.mock.push;

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
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

        Object result = fetchProvider.internalEval(
                "(async function(){\n"
                        + "  const first = await fetch('https://example.com/first');\n"
                        + "  const firstBody = await first.json();\n"
                        + "  const second = await fetch('https://example.com/second');\n"
                        + "  const secondBody = await second.json();\n"
                        + "  return firstBody.url + '|' + secondBody.url;\n"
                        + "})()",
                scriptEngine,
                createScriptContext());

        assertEquals("https://example.com/first|https://example.com/second", result);
        List<MockDiagnoseVo.Step> fetchSteps = diagnose.getSteps().stream()
                .filter(step -> "external_fetch".equals(step.getStage()))
                .collect(Collectors.toList());
        assertEquals(2, fetchSteps.size());
        assertEquals("https://example.com/first", fetchSteps.get(0).getDetails().get("url"));
        assertEquals("https://example.com/second", fetchSteps.get(1).getDetails().get("url"));
    }

    private ScriptContext createScriptContext() {
        ScriptContext context = new SimpleScriptContext();
        context.setBindings(scriptEngine.getBindings(ScriptContext.GLOBAL_SCOPE), ScriptContext.GLOBAL_SCOPE);
        return context;
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
