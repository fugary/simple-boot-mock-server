package com.fugary.simple.mock.utils;

import com.fugary.simple.mock.contants.MockErrorConstants;
import com.fugary.simple.mock.push.ScriptWithFetchProvider;
import com.fugary.simple.mock.script.JavaScriptEngineFactory;
import com.fugary.simple.mock.script.JavaScriptEngineProviderImpl;
import com.fugary.simple.mock.web.vo.http.HttpRequestVo;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.junit.jupiter.api.Test;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MockJsUtilsScriptErrorTest {

    @Test
    void formatScriptErrorShouldKeepBasicErrorWhenLocationIsMissing() {
        RuntimeException exception = new RuntimeException(
                "ScriptException: org.graalvm.polyglot.PolyglotException: TypeError: "
                        + "Cannot read property 'id' of undefined");

        String result = MockJsUtils.formatScriptError(null, exception);

        assertEquals("Script execution failed: TypeError: Cannot read property 'id' of undefined", result);
        assertFalse(result.contains("PolyglotException"));
        assertFalse(result.contains("Source:"));
        assertFalse(result.contains("Hint:"));
        assertFalse(result.contains("Possible undefined segment"));
    }

    @Test
    void formatScriptErrorShouldAppendScriptSourceLineWhenAvailable() {
        ScriptException exception = new ScriptException("TypeError: Cannot read properties of undefined "
                + "(reading 'name')", "mock-response", 2, 16);

        String result = MockJsUtils.formatScriptError("mockStringify(Mock.mock({\n"
                + "    \"test\": request.param.ab\n"
                + "}))", exception);

        assertTrue(result.contains("Source: \"test\": request.param.ab"));
        assertFalse(result.contains("line 2"));
        assertFalse(result.contains("column 16"));
        assertFalse(result.contains("Hint:"));
    }

    @Test
    void formatScriptErrorShouldHideMockStringifyWrapper() {
        ScriptException exception = new ScriptException("TypeError: Cannot read property 'id' of undefined",
                "mock-response", 1, 13);

        String result = MockJsUtils.formatScriptError("mockStringify(request.param.id)", exception);

        assertTrue(result.contains("Source: request.param.id"));
        assertFalse(result.contains("Source: mockStringify"));
    }

    @Test
    void processResponseBodyShouldRenderScriptErrorJson() {
        String scriptError = MockJsUtils.formatScriptError("mockStringify(request.param.id)",
                new RuntimeException("TypeError: Cannot read property 'id' of undefined"));

        String result = MockJsUtils.processResponseBody("<root>{{request.param.id}}</root>", new HttpRequestVo(),
                expression -> SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_400, scriptError));

        assertTrue(result.startsWith("<root>{"));
        assertTrue(result.endsWith("}</root>"));
        assertTrue(result.contains("\"code\":400"));
        assertTrue(result.contains("\"resultData\":\"" + scriptError));
        assertTrue(result.contains("\"success\":false"));
        assertTrue(result.contains("Script execution failed: TypeError"));
        assertFalse(result.contains("Source:"));
        assertFalse(result.contains("Possible undefined segment"));
    }

    @Test
    void evalStrShouldKeepSimpleResultJsonWhenScriptFails() {
        JavaScriptEngineProviderImpl provider = new JavaScriptEngineProviderImpl() {
            @Override
            public Object eval(String script) {
                return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_400,
                        "Script execution failed: TypeError");
            }
        };

        String result = provider.evalStr("mockStringify(request.param.id)");

        assertTrue(result.contains("\"code\":400"));
        assertTrue(result.contains("\"resultData\":\"Script execution failed: TypeError\""));
        assertTrue(result.contains("\"success\":false"));
    }

    @Test
    void evalStrShouldKeepSimpleResultMessageJsonWhenResultDataIsMissing() {
        JavaScriptEngineProviderImpl provider = new JavaScriptEngineProviderImpl() {
            @Override
            public Object eval(String script) {
                return SimpleResultUtils.createError(MockErrorConstants.CODE_400, "script failed");
            }
        };

        String result = provider.evalStr("mockStringify(request.param.id)");

        assertTrue(result.contains("\"code\":400"));
        assertTrue(result.contains("\"message\":\"script failed\""));
        assertTrue(result.contains("\"success\":false"));
    }

    @Test
    void processResponseBodyShouldKeepNormalExpressionReplacement() {
        String result = MockJsUtils.processResponseBody("<root>{{request.params.id}}</root>", new HttpRequestVo(),
                expression -> "1001");

        assertEquals("<root>1001</root>", result);
    }

    @Test
    void formatScriptErrorShouldUseRealScriptEngineLocationForMockJsRuntimeError() throws Exception {
        String script = "mockStringify(Mock.mock({\n"
                + "    \"test\": request.param.ab\n"
                + "}))";
        ScriptEngine scriptEngine = createScriptEngine();
        Bindings bindings = scriptEngine.getBindings(ScriptContext.GLOBAL_SCOPE);
        bindings.put("request", ProxyObject.fromMap(new LinkedHashMap<>()));

        ScriptException exception = assertThrows(ScriptException.class, () -> scriptEngine.eval(script, bindings));
        String resultData = MockJsUtils.formatScriptError(script, exception);

        assertTrue(resultData.contains("Script execution failed: TypeError"));
        assertTrue(resultData.contains("Source: \"test\": request.param.ab"));
        assertFalse(resultData.contains("line 2"));
        assertFalse(resultData.contains("column"));
        assertTrue(resultData.contains("\"test\": request.param.ab"));
        assertFalse(resultData.contains("Hint:"));
    }

    private static ScriptEngine createScriptEngine() throws Exception {
        JavaScriptEngineFactory factory = new JavaScriptEngineFactory();
        factory.setScriptWithFetchProvider(new ScriptWithFetchProvider() {
            @Override
            public Object internalEval(String script, ScriptEngine scriptEngine, ScriptContext scriptContext)
                    throws ScriptException {
                return scriptEngine.eval(script, scriptContext);
            }

            @Override
            public ProxyExecutable getFetchFunction(Context context) {
                return args -> null;
            }
        });
        return factory.create();
    }
}
