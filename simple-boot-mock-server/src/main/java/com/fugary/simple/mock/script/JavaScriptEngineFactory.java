package com.fugary.simple.mock.script;

import com.fugary.simple.mock.push.ScriptWithFetchProvider;
import com.oracle.truffle.js.scriptengine.GraalJSScriptEngine;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.StreamUtils;

import javax.script.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

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
    private static final String MOCK_JS_PATH = "classpath*:META-INF/resources/webjars/mockjs/**/mock-min.js";

    private static final String MOCK_JS_CONTENT;

    private static final String DAY_JS_PATH = "classpath*:META-INF/resources/webjars/dayjs/**/dayjs.min.js";

    private static final String DAY_JS_CONTENT;

    private static final String SIMPLE_MOCK_PATH = "classpath*:js/simple-mock.js";

    private static final String SIMPLE_MOCK_CONTENT;

    static {
        ResourcePatternResolver patternResolver = new PathMatchingResourcePatternResolver();
        try {
            Resource mockJs = patternResolver.getResources(MOCK_JS_PATH)[0];
            Resource dayJs = patternResolver.getResources(DAY_JS_PATH)[0];
            Resource simpleMock = patternResolver.getResources(SIMPLE_MOCK_PATH)[0];
            MOCK_JS_CONTENT = StreamUtils.copyToString(mockJs.getInputStream(), StandardCharsets.UTF_8);
            DAY_JS_CONTENT = StreamUtils.copyToString(dayJs.getInputStream(), StandardCharsets.UTF_8);
            SIMPLE_MOCK_CONTENT = StreamUtils.copyToString(simpleMock.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private final ScriptEngineManager manager;

    private String baseJs = MOCK_JS_PATH;

    private ScriptWithFetchProvider scriptWithFetchProvider;

    public JavaScriptEngineFactory() {
        this(new ScriptEngineManager());
    }

    public JavaScriptEngineFactory(ScriptEngineManager manager) {
        this.manager = manager;
    }

    @Override
    public ScriptEngine create() throws Exception {
//        System.setProperty("nashorn.args", "--language=es6");
//        System.setProperty("polyglot.js.nashorn-compat", "true");
//        System.setProperty("polyglot.js.ecmascript-version", "2022");
//        ScriptEngine scriptEngine = manager.getEngineByName("graal.js");
        long start = System.currentTimeMillis();
        log.info("开始创建JavaScript脚本引擎");
        System.setProperty("polyglot.engine.WarnInterpreterOnly", "false");
        GraalJSScriptEngine scriptEngine = GraalJSScriptEngine.create(null,
                Context.newBuilder("js") // 安全选项考虑不启用用
                        .allowHostAccess(HostAccess.ALL)
//                        .allowExperimentalOptions(true)
                        .allowNativeAccess(true)
//                        .allowHostClassLookup(s -> true)
                        .option("js.ecmascript-version", "2022"));
        try {
            Bindings bindings = scriptEngine.createBindings();
            scriptEngine.setBindings(bindings, ScriptContext.GLOBAL_SCOPE);
            JsHelper jsHelper = new JsHelper();
            bindings.put("JsHelper", jsHelper);
            scriptEngine.eval(MOCK_JS_CONTENT, bindings);
            scriptEngine.eval(DAY_JS_CONTENT, bindings);
            scriptEngine.eval(jsHelper.getInitStr(), bindings);
            bindings.put("fetch", scriptWithFetchProvider.getFetchFunction((Context) MethodUtils.invokeMethod(bindings, "getContext")));
            bindings.put("__requireCache__", ProxyObject.fromMap(new HashMap<>()));
            scriptEngine.eval(SIMPLE_MOCK_CONTENT, bindings);
        } catch (ScriptException e) {
            log.error("执行MockJs错误", e);
        }
        log.info("创建JavaScript脚本引擎结束，耗时：{}ms", System.currentTimeMillis() - start);
        return scriptEngine;
    }

    @Override
    public PooledObject<ScriptEngine> wrap(ScriptEngine scriptEngine) {
        return new DefaultPooledObject<>(scriptEngine);
    }
}
