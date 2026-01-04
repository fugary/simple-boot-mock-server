package com.fugary.simple.mock.security;

import com.fugary.simple.mock.utils.MockJsUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.script.ScriptEngine;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Getter
@Setter
@Slf4j
public class MockScriptInterceptor implements HandlerInterceptor {

    @Autowired
    private GenericObjectPool<ScriptEngine> scriptEnginePool;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        try {
            ScriptEngine scriptEngine = scriptEnginePool.borrowObject();
            MockJsUtils.setCurrentScriptEngine(scriptEngine);
        } catch (Exception e) {
            log.error("获取ScriptEngine失败", e);
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        ScriptEngine scriptEngine = MockJsUtils.getCurrentScriptEngine();
        MockJsUtils.removeCurrentScriptEngine();
        if (scriptEngine != null) {
//            scriptEnginePool.invalidateObject(scriptEngine);
//            scriptEnginePool.preparePool();
            scriptEnginePool.returnObject(scriptEngine);
        }
    }
}
