package com.fugary.simple.mock.script;

import com.fugary.simple.mock.utils.MockJsUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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

    /**
     * 针对json使用mockjs解析后输出
     *
     * @param template
     * @return
     */
    @Override
    public String mock(String template) {
        String result = StringUtils.trimToEmpty(template);
        if (MockJsUtils.isJson(result)) {
            Object mockRes = eval("JSON.stringify(Mock.mock(" + result + "))");
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
        } finally {
            if (scriptEngine != null) {
                scriptEnginePool.returnObject(scriptEngine);
            }
        }
        return null;
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
