package com.fugary.simple.mock.script;

import com.fugary.simple.mock.contants.MockErrorConstants;
import com.fugary.simple.mock.utils.JsonUtils;
import com.fugary.simple.mock.utils.MockJsUtils;
import com.fugary.simple.mock.utils.SimpleResultUtils;
import com.fugary.simple.mock.web.vo.SimpleResult;
import com.fugary.simple.mock.web.vo.http.HttpRequestVo;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;

/**
 * Create date 2024/7/4<br>
 *
 * @author gary.fu
 */
@Slf4j
@Setter
@Getter
public class JavaScriptEngineProviderImpl implements ScriptEngineProvider {

    private GenericObjectPool<ScriptEngine> scriptEnginePool;

    private static final String FAST_MOCK_JS_PATH = "js/fastmock.js";

    private static final String FAST_MOCK_JS_CONTENT;

    static {
        Resource fastMockJs = new ClassPathResource(FAST_MOCK_JS_PATH);
        try {
            FAST_MOCK_JS_CONTENT = StreamUtils.copyToString(fastMockJs.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isMockJSFragment(String template) {
        return template.startsWith("Mock.mock(");
    }

    private String parseMockJSFragment(String template) {
        if (!isMockJSFragment(template)) {
            template = "Mock.mock(" + template + ")";
        }
        template = template.replaceAll("(.*)(;+)$", "$1");
        return "JSON.stringify(" + template + ")";
    }

    /**
     * 针对json使用mockjs解析后输出
     *
     * @param template
     * @return
     */
    @Override
    public String mock(String template) {
        String result = StringUtils.trimToEmpty(template);
        if (MockJsUtils.isJson(result) || isMockJSFragment(template)) {
            Object mockRes = eval(parseMockJSFragment(template));
            if (mockRes instanceof SimpleResult) {
                result = JsonUtils.toJson(mockRes);
            } else if (mockRes != null) {
                result = mockRes.toString();
            }
        }
        return result;
    }

    @Override
    public Object eval(String script) {
        ScriptEngine scriptEngine = null;
        try {
            scriptEngine = scriptEnginePool.borrowObject();
//            addAdditionalBindings(scriptEngine);
            addRequestVo(scriptEngine);
            return scriptEngine.eval(script);
        } catch (Exception e) {
            log.error(MessageFormat.format("执行MockJs错误：{0}", script), e);
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_400, ExceptionUtils.getStackTrace(e));
        } finally {
            if (scriptEngine != null) {
                scriptEnginePool.returnObject(scriptEngine);
            }
        }
    }

    /**
     * 添加其他绑定
     * @deprecated
     * @param scriptEngine
     */
    protected void addAdditionalBindings(ScriptEngine scriptEngine) {
        Bindings bindings = scriptEngine.createBindings();
        MockJsUtils.addRequestInfo(bindings);
        scriptEngine.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
    }

    /**
     * 添加其他绑定
     * @param scriptEngine
     */
    protected void addRequestVo(ScriptEngine scriptEngine) throws ScriptException {
        HttpRequestVo requestVo = MockJsUtils.getHttpRequestVo();
        scriptEngine.eval("globalThis.request = " + JsonUtils.toJson(requestVo)  + ";");
        scriptEngine.eval(FAST_MOCK_JS_CONTENT);
    }
}
