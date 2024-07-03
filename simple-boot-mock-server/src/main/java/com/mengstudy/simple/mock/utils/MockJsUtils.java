package com.mengstudy.simple.mock.utils;

import com.mengstudy.simple.mock.utils.servlet.HttpRequestUtils;
import com.mengstudy.simple.mock.web.vo.http.HttpRequestVo;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.script.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created on 2020/5/5 12:29 .<br>
 * 利用Jdk中的JavaScript脚本引擎，用mockjs执行虚构数据<br>
 * 目前下载的是refactoring版本：地址：https://github.com/nuysoft/Mock/raw/refactoring/dist/mock-min.js<br>
 * 存放目录resources/js
 *
 * @author gary.fu
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MockJsUtils {
    /**
     * Javascript执行引擎
     */
    public static final ScriptEngine MOCK_JS_ENGINE;
    /**
     * mockjs的资源路径
     */
    private static final String MOCK_JS_PATH = "js/mock-min.js";

    static {
        MOCK_JS_ENGINE = new ScriptEngineManager().getEngineByName("js");
        try (
                InputStream mockJs = MockJsUtils.class.getClassLoader().getResourceAsStream(MOCK_JS_PATH);
                InputStreamReader reader = new InputStreamReader(mockJs)
        ) {
            Bindings bindings = MOCK_JS_ENGINE.createBindings();
            MOCK_JS_ENGINE.setBindings(bindings, ScriptContext.GLOBAL_SCOPE);
            MOCK_JS_ENGINE.eval(reader, bindings);
        } catch (ScriptException | IOException e) {
            log.error("执行MockJs错误", e);
        }
    }

    /**
     * json数据判断
     *
     * @param template
     * @return
     */
    public static boolean isJson(String template) {
        return StringUtils.isNotBlank(template) && StringUtils.startsWithAny(template, "{", "[");
    }

    /**
     * 针对json使用mockjs解析后输出
     *
     * @param template
     * @return
     */
    public static String mock(String template) {
        String result = StringUtils.trimToEmpty(template);
        if (isJson(result)) {
            Object mockRes = eval("JSON.stringify(Mock.mock(" + result + "))");
            if (mockRes != null) {
                result = mockRes.toString();
            }
        }
        return result;
    }

    public static void addRequestInfo(Bindings bindings) {
        HttpServletRequest request = HttpRequestUtils.getCurrentRequest();
        HttpRequestVo requestVo = new HttpRequestVo();
        if (request != null) {
            requestVo = HttpRequestUtils.parseRequestVo(request);
        }
        bindings.put("request", requestVo);
    }

    public static Object eval(String script) {
        try {
            Bindings bindings = MOCK_JS_ENGINE.createBindings();
            addRequestInfo(bindings);
            MOCK_JS_ENGINE.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
            return MOCK_JS_ENGINE.eval(script);
        } catch (ScriptException e) {
            log.error("执行MockJs错误", e);
        }
        return null;
    }

}
