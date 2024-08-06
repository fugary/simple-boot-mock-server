package com.fugary.simple.mock;

import com.fugary.simple.mock.script.JsHelper;
import com.oracle.truffle.js.scriptengine.GraalJSScriptEngine;
import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.StreamUtils;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Created on 2020/5/14 21:10 .<br>
 *
 * @author gary.fu
 */
@Slf4j
public class MockJsUtilsTest {

    /**
     * mockjs的资源路径
     */
    private static final String MOCK_JS_PATH = "classpath*:META-INF/resources/webjars/mockjs/**/mock-min.js";

    private static final String MOCK_JS_CONTENT;

    private static final String DAY_JS_PATH = "classpath*:META-INF/resources/webjars/dayjs/**/dayjs.min.js";

    private static final String DAY_JS_CONTENT;

    static {
        ResourcePatternResolver patternResolver = new PathMatchingResourcePatternResolver();
        try {
            Resource mockJs = patternResolver.getResources(MOCK_JS_PATH)[0];
            Resource dayJs = patternResolver.getResources(DAY_JS_PATH)[0];
            MOCK_JS_CONTENT = StreamUtils.copyToString(mockJs.getInputStream(), StandardCharsets.UTF_8);
            DAY_JS_CONTENT = StreamUtils.copyToString(dayJs.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void test() throws ScriptException {
        System.setProperty("polyglot.engine.WarnInterpreterOnly", "false");
        ScriptEngine scriptEngine = GraalJSScriptEngine.create(null,
                Context.newBuilder("js") // 安全选项考虑不启用用
                        .allowHostAccess(HostAccess.ALL)
//                        .allowExperimentalOptions(true)
                        .allowNativeAccess(true)
//                        .allowHostClassLookup(s -> true)
                        .option("js.ecmascript-version", "2022"));
        Bindings bindings = scriptEngine.createBindings();
        try {
            scriptEngine.setBindings(bindings, ScriptContext.GLOBAL_SCOPE);
            JsHelper jsHelper = new JsHelper();
            bindings.put("JsHelper", jsHelper);
            scriptEngine.eval(MOCK_JS_CONTENT, bindings);
            scriptEngine.eval(DAY_JS_CONTENT, bindings);
            scriptEngine.eval(jsHelper.getInitStr(), bindings);
        } catch (ScriptException e) {
            log.error("执行MockJs错误", e);
        }
        String input = "{\n" +
                "    \"user|2\": [{\n" +
                "        'name': \"@name\",\n" +
                "        'id|+1': 1, \n" +
                "        'date': dayjs().format('YYYY-MM-DD')\n" +
                "    }]\n" +
                "}";
        System.out.println(input);
        Object output = scriptEngine.eval("JSON.stringify(Mock.mock(" + input + "))", bindings);
        scriptEngine.eval("let a = 1;");
        log.info("output: {}", output);
    }
}
