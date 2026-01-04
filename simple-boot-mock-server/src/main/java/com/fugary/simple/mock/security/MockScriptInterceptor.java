package com.fugary.simple.mock.security;

import com.fugary.simple.mock.utils.MockJsUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.script.ScriptEngine;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Getter
@Setter
@Slf4j
public class MockScriptInterceptor implements HandlerInterceptor {

    @Autowired
    private GenericObjectPool<ScriptEngine> scriptEnginePool;

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        ScriptEngine scriptEngine = MockJsUtils.getCurrentScriptEngine();
        MockJsUtils.removeCurrentScriptEngine();
        if (scriptEngine != null) {
//            scriptEnginePool.invalidateObject(scriptEngine);
//            scriptEnginePool.preparePool();
            scriptEnginePool.returnObject(scriptEngine);
        }
    }
}
