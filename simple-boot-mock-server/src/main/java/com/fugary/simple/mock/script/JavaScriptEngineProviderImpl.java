package com.fugary.simple.mock.script;

import com.fugary.simple.mock.contants.MockErrorConstants;
import com.fugary.simple.mock.utils.JsonUtils;
import com.fugary.simple.mock.utils.MockJsUtils;
import com.fugary.simple.mock.utils.SimpleResultUtils;
import com.fugary.simple.mock.web.vo.SimpleResult;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.pool2.impl.GenericObjectPool;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;

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
            if (mockRes != null) {
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
            addAdditionalBindings(scriptEngine);
            return scriptEngine.eval(script);
        } catch (Exception e) {
            log.error("执行MockJs错误", e);
            SimpleResult<String> result = SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_400, ExceptionUtils.getStackTrace(e));
            return JsonUtils.toJson(result);
        } finally {
            if (scriptEngine != null) {
                scriptEnginePool.returnObject(scriptEngine);
            }
        }
    }

    /**
     * 添加其他绑定
     * @param scriptEngine
     */
    protected void addAdditionalBindings(ScriptEngine scriptEngine) {
        Bindings bindings = scriptEngine.createBindings();
        MockJsUtils.addRequestInfo(bindings);
        scriptEngine.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
    }
}
