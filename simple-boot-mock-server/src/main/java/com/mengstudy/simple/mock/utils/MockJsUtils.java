package com.mengstudy.simple.mock.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
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
            MOCK_JS_ENGINE.eval(reader);
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
            try {
                result = MOCK_JS_ENGINE.eval("JSON.stringify(Mock.mock(" + result + "))").toString();
            } catch (ScriptException e) {
                log.error("执行Mock.mock错误", e);
            }
        }
        return result;
    }

}
