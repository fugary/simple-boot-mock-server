package com.fugary.simple.mock.script;

import com.fugary.simple.mock.utils.MockJsUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import javax.script.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Create date 2024/7/4<br>
 *
 * @author gary.fu
 */
@Slf4j
@Getter
@Setter
public class JavaScriptEngineFactory extends BasePooledObjectFactory<ScriptEngine> {

    /**
     * mockjs的资源路径
     */
    private static final String MOCK_JS_PATH = "js/mock-min.js";

    private final ScriptEngineManager manager;

    private String baseJs = MOCK_JS_PATH;

    public JavaScriptEngineFactory() {
        this(new ScriptEngineManager());
    }

    public JavaScriptEngineFactory(ScriptEngineManager manager) {
        this.manager = manager;
    }

    @Override
    public ScriptEngine create() throws Exception {
//        System.setProperty("nashorn.args", "--language=es6");
        System.setProperty("polyglot.js.nashorn-compat", "true");
        System.setProperty("polyglot.engine.WarnInterpreterOnly", "false");
        System.setProperty("polyglot.js.ecmascript-version", "2022");
        ScriptEngine scriptEngine = manager.getEngineByName("graal.js");
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
        return scriptEngine;
    }

    @Override
    public PooledObject<ScriptEngine> wrap(ScriptEngine scriptEngine) {
        return new DefaultPooledObject<>(scriptEngine);
    }
}
