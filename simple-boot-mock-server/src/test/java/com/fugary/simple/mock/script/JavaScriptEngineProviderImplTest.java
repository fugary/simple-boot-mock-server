package com.fugary.simple.mock.script;

import com.fugary.simple.mock.config.SimpleMockConfigProperties;
import com.fugary.simple.mock.config.script.ScriptEngineConfig;
import com.fugary.simple.mock.push.DefaultScriptWithFetchProviderImpl;
import com.fugary.simple.mock.push.ScriptWithFetchProvider;
import com.fugary.simple.mock.security.MockScriptInterceptor;
import com.fugary.simple.mock.utils.JsonUtils;
import com.fugary.simple.mock.utils.MockDiagnoseContext;
import com.fugary.simple.mock.utils.MockJsUtils;
import com.fugary.simple.mock.web.vo.SimpleResult;
import com.fugary.simple.mock.web.vo.http.HttpRequestVo;
import com.fugary.simple.mock.web.vo.http.HttpResponseVo;
import com.fugary.simple.mock.web.vo.result.MockDiagnoseVo;
import com.oracle.truffle.js.scriptengine.GraalJSScriptEngine;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.fugary.simple.mock.contants.MockDiagnoseConstants.CODE_CONSOLE;
import static com.fugary.simple.mock.contants.MockDiagnoseConstants.KEY_MESSAGE;
import static com.fugary.simple.mock.contants.MockDiagnoseConstants.STAGE_CONSOLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class JavaScriptEngineProviderImplTest {

    private JavaScriptEngineProviderImpl provider;

    private GraalJSScriptEngine scriptEngine;

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
        MockJsUtils.removeCurrentRequestVo();
        MockJsUtils.removeCurrentResponseVo();
        MockDiagnoseContext.clear();
        if (scriptEngine != null) {
            scriptEngine.close();
        }
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
    void invalidateObjectShouldCloseUnderlyingGraalContext() throws Exception {
        try (GenericObjectPool<ScriptEngine> pool = createSingleEnginePool()) {
            ScriptEngine borrowed = pool.borrowObject();
            Context polyglotContext = ((GraalJSScriptEngine) borrowed).getPolyglotContext();

            pool.invalidateObject(borrowed);

            assertThrows(IllegalStateException.class, () -> polyglotContext.eval("js", "1 + 1"));
        }
    }

    @Test
    void scriptEnginePoolShouldEnforceConfiguredMaxWaitWhenExhausted() throws Exception {
        MockJsUtils.removeCurrentScriptEngine();
        try (GenericObjectPool<ScriptEngine> pool = createSingleEnginePool()) {
            assertEquals(Duration.ofMillis(150L), pool.getMaxWaitDuration());
            ScriptEngine occupied = pool.borrowObject();
            try {
                JavaScriptEngineProviderImpl pooledProvider = createPooledProvider(pool);

                assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
                    Object res = pooledProvider.eval("1 + 1");
                    SimpleResult<?> errorResult = assertInstanceOf(SimpleResult.class, res);
                    assertFalse(errorResult.isSuccess());
                });
            } finally {
                pool.returnObject(occupied);
            }
        }
    }

    @Test
    void temporaryEvalShouldIsolateGlobalsAndReturnDetachedObjects() throws Exception {
        MockJsUtils.removeCurrentScriptEngine();
        try (GenericObjectPool<ScriptEngine> pool = createSingleEnginePool()) {
            JavaScriptEngineProviderImpl pooledProvider = createPooledProvider(pool);

            HttpRequestVo requestVo = new HttpRequestVo();
            requestVo.getParameters().put("id", "42");
            HttpResponseVo responseVo = new HttpResponseVo();
            responseVo.setStatusCode(201);
            MockJsUtils.setCurrentRequestVo(requestVo);
            MockJsUtils.setCurrentResponseVo(responseVo);
            MockDiagnoseVo previewDiagnose = new MockDiagnoseVo();
            MockDiagnoseContext.set(previewDiagnose);
            MockDiagnoseContext.setScriptConsoleEnabled(true);

            Object evalResult = pooledProvider.eval(
                    "globalThis.savedRequest = request; console.log('preview-log'); ({ id: request.params.id, status: response.statusCode, items: [1, { nested: true }] })");
            assertEquals(1, consoleSteps(previewDiagnose).size());

            MockJsUtils.removeCurrentRequestVo();
            MockJsUtils.removeCurrentResponseVo();
            MockDiagnoseVo nonPreviewDiagnose = new MockDiagnoseVo();
            MockDiagnoseContext.set(nonPreviewDiagnose);
            MockDiagnoseContext.setScriptConsoleEnabled(false);

            Object secondResult = pooledProvider.eval(
                    "console.log('should-not-record'); (typeof request === 'undefined') + '|' + (typeof response === 'undefined') + '|' + (typeof savedRequest === 'undefined')");
            assertEquals("true|true|true", secondResult);
            assertTrue(consoleSteps(nonPreviewDiagnose).isEmpty());

            assertEquals(0, pool.getNumActive());
            assertEquals(0, pool.getNumIdle());
            Map<?, ?> detachedMap = assertInstanceOf(Map.class, evalResult);
            assertFalse(detachedMap.getClass().getName().startsWith("com.oracle.truffle."));
            assertEquals("42", detachedMap.get("id"));
            assertEquals(201, detachedMap.get("status"));
            String json = JsonUtils.toJson(detachedMap);
            assertTrue(json.contains("\"nested\":true"), json);
        }
    }

    @Test
    void resultConversionGetterShouldBeEnforcedByScriptExecutionTimeout() {
        JavaScriptEngineProviderImpl realProvider = createRealFetchProvider(1);

        withTimedEngine(() -> assertScriptError(realProvider.eval(
                "({ get id() { const end = Date.now() + 1600; while (Date.now() < end) {} return 42; } })"),
                "timed out after 1 seconds"));
    }

    @Test
    void closingTemporaryEngineShouldNeverInvokeUserGetters() throws Exception {
        MockJsUtils.removeCurrentScriptEngine();
        try (GenericObjectPool<ScriptEngine> pool = createSingleEnginePool()) {
            JavaScriptEngineProviderImpl pooledProvider = createPooledProvider(pool);

            Object firstRes = assertTimeoutPreemptively(Duration.ofSeconds(5), () -> pooledProvider.eval(
                    "Object.defineProperty(globalThis, '__simpleMockOriginalConsole__', { get() { while (true) {} } }); 'done';"));
            assertEquals("done", firstRes);
            assertEquals(0, pool.getNumActive());

            MockDiagnoseContext.set(new MockDiagnoseVo());
            MockDiagnoseContext.setScriptConsoleEnabled(true);
            Object secondRes = pooledProvider.eval("console.log('after-invalidate'); 1 + 1");
            assertEquals(2, secondRes);
            assertEquals(0, pool.getNumActive());
        }
    }

    @Test
    void resultConversionShouldPreserveLargeJsonAndOrdinarySharedReferences() {
        JavaScriptEngineProviderImpl realProvider = createRealFetchProvider(30);
        List<?> rows = assertInstanceOf(List.class, realProvider.eval(
                "Array.from({length: 20001}, (_, id) => ({id, ok: true}))"));
        assertEquals(20001, rows.size());
        assertEquals(Map.of("id", 20000, "ok", true), rows.get(20000));

        List<?> shared = assertInstanceOf(List.class, realProvider.eval("Array(20001).fill({id: 1})"));
        assertEquals(20001, shared.size());
        assertEquals(Map.of("id", 1), shared.get(20000));
    }

    @Test
    void sharedReferenceBudgetShouldBeIndependentOfFieldOrder() {
        JavaScriptEngineProviderImpl realProvider = createRealFetchProvider(30);
        String setup = "const rows = Array(200).fill(Array(100).fill(1)), tail = Array(1000).fill(1);";
        for (String fields : List.of("rows, tail", "tail, rows")) {
            Map<?, ?> result = assertInstanceOf(Map.class, realProvider.eval(
                    "(() => {" + setup + "return {" + fields + "}})()"));
            assertEquals(200, ((List<?>) result.get("rows")).size());
            assertEquals(1000, ((List<?>) result.get("tail")).size());
        }
    }

    @Test
    void resultConversionShouldRejectExcessiveDagAndCircularReferences() {
        JavaScriptEngineProviderImpl realProvider = createRealFetchProvider(30);
        withTimedEngine(() -> {
            String dag = "(() => { let n = { v: 1 }; for (let i = 0; i < 30; i++) { n = { left: n, right: n }; } return n; })()";
            assertScriptError(realProvider.eval(dag), "Repeated references");
            assertScriptError(realProvider.eval("mockStringify(" + dag + ")"), "Repeated references");
            assertScriptError(realProvider.eval("(() => { const a = {}; a.self = a; return a; })()"), "Circular reference");
        });
    }

    private void assertScriptError(Object result, String expected) {
        SimpleResult<?> error = assertInstanceOf(SimpleResult.class, result);
        assertFalse(error.isSuccess());
        String message = String.valueOf(error.getResultData());
        assertTrue(message.contains(expected), message);
    }

    private void withTimedEngine(Executable test) {
        assertTimeoutPreemptively(Duration.ofSeconds(3), () -> {
            MockJsUtils.setCurrentScriptEngine(scriptEngine);
            try {
                test.execute();
            } finally {
                MockJsUtils.removeCurrentScriptEngine();
            }
        });
    }

    @Test
    void asyncRedispatchShouldLeaveCommittedSseResponseAndThreadContextAlone() throws Exception {
        MockScriptInterceptor interceptor = new MockScriptInterceptor();
        @SuppressWarnings("unchecked")
        GenericObjectPool<ScriptEngine> pool = mock(GenericObjectPool.class);
        interceptor.setScriptEnginePool(pool);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/mock/stream");
        MockHttpServletResponse response = new MockHttpServletResponse();
        interceptor.afterConcurrentHandlingStarted(request, response, this);
        assertNull(MockJsUtils.getCurrentScriptEngine());

        request.setDispatcherType(javax.servlet.DispatcherType.ASYNC);
        MockJsUtils.setCurrentScriptEngine(scriptEngine); // 模拟分派线程已有绑定
        response.getOutputStream().print("data: done\n\n");
        response.flushBuffer();
        assertTrue(interceptor.preHandle(request, response, this));
        interceptor.afterCompletion(request, response, this, null);
        assertEquals("data: done\n\n", response.getContentAsString());
        assertEquals(scriptEngine, MockJsUtils.getCurrentScriptEngine());
        verify(pool).invalidateObject(scriptEngine);
    }

    @Test
    void mockScriptInterceptorShouldFailFastWhenPoolIsExhausted() throws Exception {
        MockJsUtils.removeCurrentScriptEngine();
        try (GenericObjectPool<ScriptEngine> pool = createSingleEnginePool()) {
            ScriptEngine occupied = pool.borrowObject();
            try {
                MockScriptInterceptor interceptor = new MockScriptInterceptor();
                interceptor.setScriptEnginePool(pool);
                MockHttpServletRequest request = new MockHttpServletRequest("GET", "/mock/demo/test");
                MockHttpServletResponse response = new MockHttpServletResponse();

                assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
                    boolean allowed = interceptor.preHandle(request, response, new Object());
                    assertFalse(allowed);
                    assertEquals(503, response.getStatus());
                    assertNull(MockJsUtils.getCurrentScriptEngine());
                    assertNull(MockDiagnoseContext.get());
                    assertTrue(response.getContentAsString().contains("503"));
                });
            } finally {
                pool.returnObject(occupied);
            }
        }
    }

    private GenericObjectPool<ScriptEngine> createSingleEnginePool() {
        SimpleMockConfigProperties properties = new SimpleMockConfigProperties();
        properties.getPool().setScriptEngineMaxSize(1);
        properties.getPool().setScriptEngineMaxWaitMillis(150L);
        ScriptEngineConfig engineConfig = new ScriptEngineConfig();
        ReflectionTestUtils.setField(engineConfig, "simpleMockConfigProperties", properties);
        return engineConfig.scriptEnginePool();
    }

    private JavaScriptEngineProviderImpl createPooledProvider(GenericObjectPool<ScriptEngine> pool) {
        JavaScriptEngineProviderImpl pooledProvider = new JavaScriptEngineProviderImpl();
        pooledProvider.setScriptEnginePool(pool);
        pooledProvider.setScriptWithFetchProvider(createFetchProvider());
        return pooledProvider;
    }

    private JavaScriptEngineProviderImpl createRealFetchProvider(int timeoutSeconds) {
        DefaultScriptWithFetchProviderImpl fetchProvider = new DefaultScriptWithFetchProviderImpl();
        fetchProvider.setExecutionTimeoutSeconds(timeoutSeconds);
        JavaScriptEngineProviderImpl realProvider = new JavaScriptEngineProviderImpl();
        realProvider.setScriptWithFetchProvider(fetchProvider);
        realProvider.setFetchEnabled(true);
        return realProvider;
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
