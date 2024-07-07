package com.fugary.simple.mock;

import com.fugary.simple.mock.utils.MockJsUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import javax.script.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created on 2020/5/14 21:10 .<br>
 *
 * @author gary.fu
 */
@Slf4j
public class MockJsUtilsTest {

    public static final String MOCK_JS_PATH = "js/mock-min.js";

    @Test
    public void test() throws ScriptException {
        System.setProperty("nashorn.args", "--language=es6");
        ScriptEngine scriptEngine = new ScriptEngineManager().getEngineByName("js");
        try (
                InputStream mockJs = MockJsUtils.class.getClassLoader().getResourceAsStream(MOCK_JS_PATH);
                InputStreamReader reader = new InputStreamReader(mockJs)
        ) {
            Bindings bindings = scriptEngine.createBindings();
            scriptEngine.setBindings(bindings, ScriptContext.GLOBAL_SCOPE);
            scriptEngine.eval(reader, bindings);
        } catch (ScriptException | IOException e) {
            log.error("执行MockJs错误", e);
        }
        String input = "{\n" +
                "    \"user|2\": [{\n" +
                "        'name': '@cname', \n" +
                "        'id|+1': 1\n" +
                "    }]\n" +
                "}";
        String output = (String) scriptEngine.eval("JSON.stringify(Mock.mock(" + input + "))");
        scriptEngine.eval("let a = 1;");
        log.info("output: {}", output);
    }
}
