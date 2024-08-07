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
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;

import javax.script.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

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
        ScriptEngine scriptEngine = MockJsUtils.getCurrentScriptEngine(); // 优先从线程中获取
        boolean isThreadEngine = scriptEngine != null;
        try {
            if (!isThreadEngine) {
                scriptEngine = scriptEnginePool.borrowObject();
            }
            ScriptContext context = getScriptContext(scriptEngine);
            return scriptEngine.eval(script, context);
        } catch (Exception e) {
            log.error(MessageFormat.format("执行MockJs错误：{0}", script), e);
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_400, ExceptionUtils.getStackTrace(e));
        } finally {
            if (scriptEngine != null && !isThreadEngine) {
                scriptEnginePool.returnObject(scriptEngine);
            }
        }
    }

    /**
     * 添加其他绑定
     * @param scriptEngine
     */
    protected ScriptContext getScriptContext(ScriptEngine scriptEngine) throws ScriptException {
        HttpRequestVo requestVo = MockJsUtils.getHttpRequestVo();
        ScriptContext scriptContext = new SimpleScriptContext();
        scriptContext.setBindings(scriptEngine.getBindings(ScriptContext.GLOBAL_SCOPE), ScriptContext.GLOBAL_SCOPE);
        if (requestVo != null) {
            scriptContext.setAttribute("request", processValue(requestVo), ScriptContext.ENGINE_SCOPE);
            scriptEngine.eval(FAST_MOCK_JS_CONTENT, scriptContext);
        }
        return scriptContext;
    }

    protected Object processValue(Object value) {
        if (value != null && !ClassUtils.isPrimitiveOrWrapper(value.getClass())
                && (!(value instanceof String))) { // 转json
            String jsonValue = JsonUtils.toJson(value);
            if (isJson(jsonValue)) {
                return JsonUtils.fromJson(jsonValue, Map.class);
            } else if(isJsonArray(jsonValue)) {
                return JsonUtils.fromJson(jsonValue, List.class);
            }
        }
        return value;
    }

    protected boolean isJson(String value) {
        return StringUtils.isNotBlank(value) && StringUtils.startsWith(value, "{");
    }

    protected boolean isJsonArray(String value) {
        return StringUtils.isNotBlank(value) && StringUtils.startsWith(value, "[");
    }
}
