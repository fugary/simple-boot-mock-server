package com.fugary.simple.mock.script;

import com.fugary.simple.mock.push.ScriptWithFetchProvider;
import com.fugary.simple.mock.web.vo.http.HttpRequestVo;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SimpleMockProxyObjectTest {

    private Bindings bindings;

    private ScriptEngine scriptEngine;

    @BeforeEach
    void setUp() throws Exception {
        JavaScriptEngineFactory factory = new JavaScriptEngineFactory();
        factory.setScriptWithFetchProvider(createFetchProvider());
        scriptEngine = factory.create();
        bindings = scriptEngine.getBindings(ScriptContext.GLOBAL_SCOPE);
        bindings.put("request", new JavaScriptEngineProviderImpl().processValue(createRequestVo()));
    }

    @Test
    void stringifyRequestBodyShouldReturnRequestJson() throws ScriptException {
        Object result = scriptEngine.eval("(function () { return JSON.stringify(request.body); })()", bindings);

        assertEquals("{\"test\":1,\"test2\":{\"a\":\"test\"}}", result);
    }

    @Test
    void returnRequestBodyShouldStillSerializeWithMockStringify() throws ScriptException {
        Object result = scriptEngine.eval("mockStringify((function () { return request.body; }))", bindings);

        assertEquals("{\"test\":1,\"test2\":{\"a\":\"test\"}}", result);
    }

    @Test
    void emptyRequestBodyShouldSerializeAsEmptyJson() throws ScriptException {
        HttpRequestVo requestVo = new HttpRequestVo();
        requestVo.setBody(new LinkedHashMap<>());
        bindings.put("request", new JavaScriptEngineProviderImpl().processValue(requestVo));

        Object result = scriptEngine.eval("mockStringify((function () { return request.body; }))", bindings);

        assertEquals("{}", result);
    }

    @Test
    void arrayRequestBodyShouldSerializeAsJsonArray() throws ScriptException {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("a", "test");
        HttpRequestVo requestVo = new HttpRequestVo();
        requestVo.setBody(List.of(item));
        bindings.put("request", new JavaScriptEngineProviderImpl().processValue(requestVo));

        Object result = scriptEngine.eval("mockStringify((function () { return request.body; }))", bindings);

        assertEquals("[{\"a\":\"test\"}]", result);
    }

    @Test
    void rawJavaMapShouldStillSerializeWithJava2Json() throws ScriptException {
        bindings.put("rawBody", createRequestBody());

        Object result = scriptEngine.eval("mockStringify(rawBody)", bindings);

        assertEquals("{\"test\":1,\"test2\":{\"a\":\"test\"}}", result);
    }

    @Test
    void requestMetadataShouldBeAccessibleFromJavascript() throws ScriptException {
        HttpRequestVo requestVo = createRequestVo();
        requestVo.setIp("203.0.113.9");
        requestVo.setUserAgent("JUnit-Agent");
        requestVo.getCookies().put("sid", "abc123");
        bindings.put("request", new JavaScriptEngineProviderImpl().processValue(requestVo));

        Object result = scriptEngine.eval(
                "request.ip + '|' + request.userAgent + '|' + request.cookies.sid + '|' + (request.ua === undefined)",
                bindings);

        assertEquals("203.0.113.9|JUnit-Agent|abc123|true", result);
    }

    private HttpRequestVo createRequestVo() {
        HttpRequestVo requestVo = new HttpRequestVo();
        requestVo.setBody(createRequestBody());
        return requestVo;
    }

    private Map<String, Object> createRequestBody() {
        Map<String, Object> nestedBody = new LinkedHashMap<>();
        nestedBody.put("a", "test");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("test", 1);
        body.put("test2", nestedBody);
        return body;
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
}
