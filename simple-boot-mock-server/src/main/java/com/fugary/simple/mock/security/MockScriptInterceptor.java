package com.fugary.simple.mock.security;

import com.fugary.simple.mock.utils.JsonUtils;
import com.fugary.simple.mock.utils.MockDiagnoseContext;
import com.fugary.simple.mock.utils.MockJsUtils;
import com.fugary.simple.mock.utils.SimpleMockUtils;
import com.fugary.simple.mock.utils.SimpleResultUtils;
import com.fugary.simple.mock.web.vo.result.MockDiagnoseVo;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.AsyncHandlerInterceptor;

import javax.script.ScriptEngine;
import javax.servlet.DispatcherType;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

@Getter
@Setter
@Slf4j
public class MockScriptInterceptor implements AsyncHandlerInterceptor {

    @Autowired
    private GenericObjectPool<ScriptEngine> scriptEnginePool;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if (request.getDispatcherType() == DispatcherType.ASYNC) {
            return true;
        }
        MockDiagnoseContext.set(new MockDiagnoseVo());
        MockDiagnoseContext.setScriptConsoleEnabled(SimpleMockUtils.isMockPreview(request));
        try {
            ScriptEngine scriptEngine = scriptEnginePool.borrowObject();
            MockJsUtils.setCurrentScriptEngine(scriptEngine);
            return true;
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("获取ScriptEngine失败", e);
            MockDiagnoseContext.clear();
            responseBusy(response);
            return false;
        }
    }

    protected void responseBusy(HttpServletResponse response) throws IOException {
        int status = HttpStatus.SERVICE_UNAVAILABLE.value();
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        try (PrintWriter out = response.getWriter()) {
            out.write(JsonUtils.toJson(SimpleResultUtils.createSimpleResult(status,
                    "Server busy: script engine pool exhausted")));
        }
    }

    @Override
    public void afterConcurrentHandlingStarted(HttpServletRequest request, HttpServletResponse response, Object handler) {
        clearRequestContext();
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (request.getDispatcherType() != DispatcherType.ASYNC) {
            clearRequestContext();
        }
    }

    private void clearRequestContext() {
        try {
            MockJsUtils.invalidateCurrentAndPrepareScriptEngine(scriptEnginePool);
        } finally {
            MockDiagnoseContext.clear();
        }
    }
}
