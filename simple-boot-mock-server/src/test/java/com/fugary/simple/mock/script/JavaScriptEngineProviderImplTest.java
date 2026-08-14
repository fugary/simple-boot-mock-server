package com.fugary.simple.mock.script;

import com.fugary.simple.mock.push.ScriptWithFetchProvider;
import com.fugary.simple.mock.utils.MockDiagnoseContext;
import com.fugary.simple.mock.utils.MockJsUtils;
import com.fugary.simple.mock.web.vo.http.HttpRequestVo;
import com.fugary.simple.mock.web.vo.result.MockDiagnoseVo;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.fugary.simple.mock.contants.MockDiagnoseConstants.CODE_CONSOLE;
import static com.fugary.simple.mock.contants.MockDiagnoseConstants.KEY_MESSAGE;
import static com.fugary.simple.mock.contants.MockDiagnoseConstants.STAGE_CONSOLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaScriptEngineProviderImplTest {

    private JavaScriptEngineProviderImpl provider;

    private ScriptEngine scriptEngine;

    @BeforeEach
    void setUp() throws Exception {
        JavaScriptEngineFactory factory = new JavaScriptEngineFactory();
        factory.setScriptWithFetchProvider(createFetchProvider());
        scriptEngine = factory.create();
        provider = new TestJavaScriptEngineProvider();
        MockJsUtils.setCurrentScriptEngine(scriptEngine);
    }

    @AfterEach
    void tearDown() {
        MockJsUtils.removeCurrentScriptEngine();
        MockDiagnoseContext.clear();
    }

    @Test
    void previewScriptConsoleShouldRecordDiagnoseSteps() {
        MockDiagnoseVo diagnose = new MockDiagnoseVo();
        MockDiagnoseContext.set(diagnose);
        MockDiagnoseContext.setScriptConsoleEnabled(true);

        Object result = provider.eval("console.log('hello', {a: 1}); console.error('password\":\"secret1\"'); 'ok';");

        assertEquals("ok", result);
        List<MockDiagnoseVo.Step> consoleSteps = consoleSteps(diagnose);
        assertEquals(2, consoleSteps.size());
        assertEquals(CODE_CONSOLE + ".log", consoleSteps.get(0).getCode());
        assertEquals("log", consoleSteps.get(0).getDetails().get("level"));
        assertTrue(String.valueOf(consoleSteps.get(0).getDetails().get(KEY_MESSAGE)).contains("hello"));
        assertEquals(CODE_CONSOLE + ".error", consoleSteps.get(1).getCode());
        assertEquals("error", consoleSteps.get(1).getDetails().get("level"));
        assertTrue(String.valueOf(consoleSteps.get(1).getDetails().get(KEY_MESSAGE)).contains("***"));
    }

    @Test
    void previewScriptConsoleShouldFormatProxyObject() {
        MockDiagnoseVo diagnose = new MockDiagnoseVo();
        MockDiagnoseContext.set(diagnose);
        MockDiagnoseContext.setScriptConsoleEnabled(true);

        Object result = provider.eval("console.log('proxy', proxyData); 'ok';");

        assertEquals("ok", result);
        String message = String.valueOf(consoleSteps(diagnose).get(0).getDetails().get(KEY_MESSAGE));
        assertTrue(message.contains("proxy"));
        assertTrue(message.contains("\"name\":\"gary\""));
        assertTrue(message.contains("\"nested\":{\"id\":1}"));
        assertFalse(message.contains("ProxyObject"));
    }

    @Test
    void nonPreviewScriptConsoleShouldNotRecordDiagnoseSteps() {
        MockDiagnoseVo diagnose = new MockDiagnoseVo();
        MockDiagnoseContext.set(diagnose);

        Object result = provider.eval("console.log('hidden'); 'ok';");

        assertEquals("ok", result);
        assertTrue(consoleSteps(diagnose).isEmpty());
    }

    @Test
    void testFastMockFunction() {
        JavaScriptEngineProviderImpl realProvider = new JavaScriptEngineProviderImpl();
        realProvider.setScriptWithFetchProvider(createFetchProvider());

        HttpRequestVo requestVo = new HttpRequestVo();
        requestVo.getParameters().put("id", "1");
        MockJsUtils.setCurrentRequestVo(requestVo);
        try {
            String template = "{\n" +
                    "  \"success\": true,\n" +
                    "  \"message\": \"Success\",\n" +
                    "  \"resultData\": function({_req}){\n" +
                    "    var menuList = [\n" +
                    "            {\n" +
                    "                \"id\": 1,\n" +
                    "                \"iconCls\": \"setting\",\n" +
                    "                \"nameCn\": \"系统管理\",\n" +
                    "                \"nameEn\": \"System\"\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"id\": 11,\n" +
                    "                \"parentId\": 1,\n" +
                    "                \"iconCls\": \"user\",\n" +
                    "                \"nameCn\": \"用户管理\",\n" +
                    "                \"nameEn\": \"Users\",\n" +
                    "                \"menuUrl\": \"/admin/users\"\n" +
                    "            }\n" +
                    "        ];\n" +
                    "  return {\n" +
                    "    menu: menuList.filter(menu=>menu.id==_req.params.id)[0]\n" +
                    "  };\n" +
                    "}\n" +
                    "}";
            String result = realProvider.mock(template);
            assertTrue(result.contains("\"nameCn\":\"系统管理\""));
            assertTrue(result.contains("\"nameEn\":\"System\""));
            assertTrue(result.contains("\"success\":true"));
        } finally {
            MockJsUtils.removeCurrentRequestVo();
        }
    }

    @Test
    void testMockStringifyWithFastMockFunction() {
        JavaScriptEngineProviderImpl realProvider = new JavaScriptEngineProviderImpl();
        realProvider.setScriptWithFetchProvider(createFetchProvider());

        HttpRequestVo requestVo = new HttpRequestVo();
        requestVo.getParameters().put("id", "1");
        MockJsUtils.setCurrentRequestVo(requestVo);
        try {
            String template = "{\n" +
                    "  \"debugReq\": function({_req}) {\n" +
                    "    return {\n" +
                    "      params: _req.params,\n" +
                    "      query: _req.query,\n" +
                    "      body: _req.body\n" +
                    "    };\n" +
                    "  }\n" +
                    "}";
            String resultMock = realProvider.mock(template);
            assertTrue(resultMock.contains("\"id\":\"1\""), resultMock);

            String resultEval = realProvider.evalStr("mockStringify(Mock.mock(" + template + "))");
            assertTrue(resultEval.contains("\"id\":\"1\""), resultEval);
        } finally {
            MockJsUtils.removeCurrentRequestVo();
        }
    }

    @Test
    void testMockJsPlaceholdersAndRules() {
        JavaScriptEngineProviderImpl realProvider = new JavaScriptEngineProviderImpl();
        realProvider.setScriptWithFetchProvider(createFetchProvider());

        String template = "{\n" +
                "  \"name\": \"@cname\",\n" +
                "  \"age|18-60\": 1,\n" +
                "  \"city\": \"@city\",\n" +
                "  \"list|3\": [{\n" +
                "    \"id|+1\": 100\n" +
                "  }],\n" +
                "  \"computed\": function() {\n" +
                "    return this.city + '-test';\n" +
                "  }\n" +
                "}";
        String result = realProvider.mock(template);
        assertTrue(result.contains("\"list\":["));
        assertTrue(result.contains("-test"));
    }

    @Test
    void testFastMockWithArrowFunctionAndMethodShorthand() {
        JavaScriptEngineProviderImpl realProvider = new JavaScriptEngineProviderImpl();
        realProvider.setScriptWithFetchProvider(createFetchProvider());

        HttpRequestVo requestVo = new HttpRequestVo();
        requestVo.getParameters().put("name", "Alice");
        MockJsUtils.setCurrentRequestVo(requestVo);
        try {
            String arrowTemplate = "{\n" +
                    "  \"user\": ({_req}) => ({ name: _req.params.name })\n" +
                    "}";
            String result = realProvider.mock(arrowTemplate);
            assertTrue(result.contains("\"name\":\"Alice\""));
        } finally {
            MockJsUtils.removeCurrentRequestVo();
        }
    }

    private List<MockDiagnoseVo.Step> consoleSteps(MockDiagnoseVo diagnose) {
        return diagnose.getSteps().stream()
                .filter(step -> STAGE_CONSOLE.equals(step.getStage()))
                .collect(Collectors.toList());
    }

    private ScriptWithFetchProvider createFetchProvider() {
        return new ScriptWithFetchProvider() {

            @Override
            public Object internalEval(String script, ScriptEngine scriptEngine, ScriptContext scriptContext)
                    throws ScriptException {
                return scriptEngine.eval(script, scriptContext);
            }

            @Override
            public ProxyExecutable getFetchFunction(Context context) {
                return args -> null;
            }
        };
    }

    private static class TestJavaScriptEngineProvider extends JavaScriptEngineProviderImpl {

        @Override
        protected ScriptContext getScriptContext(ScriptEngine scriptEngine) throws ScriptException {
            ScriptContext scriptContext = new SimpleScriptContext();
            scriptContext.setBindings(scriptEngine.getBindings(ScriptContext.GLOBAL_SCOPE),
                    ScriptContext.GLOBAL_SCOPE);
            scriptContext.setWriter(new PrintWriter(new StringWriter()));
            scriptContext.setErrorWriter(new PrintWriter(new StringWriter()));
            scriptContext.setAttribute("proxyData", ProxyObject.fromMap(createProxyData()),
                    ScriptContext.ENGINE_SCOPE);
            if (MockDiagnoseContext.isScriptConsoleEnabled()) {
                ScriptConsoleBridge.install(scriptEngine, scriptContext);
            }
            return scriptContext;
        }

        private Map<String, Object> createProxyData() {
            Map<String, Object> nested = new LinkedHashMap<>();
            nested.put("id", 1);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("name", "gary");
            data.put("nested", ProxyObject.fromMap(nested));
            return data;
        }
    }
}
