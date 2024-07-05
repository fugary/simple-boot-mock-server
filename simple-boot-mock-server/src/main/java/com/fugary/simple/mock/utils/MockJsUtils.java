package com.fugary.simple.mock.utils;

import com.fugary.simple.mock.web.vo.http.HttpRequestVo;
import com.fugary.simple.mock.utils.servlet.HttpRequestUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.script.Bindings;
import javax.servlet.http.HttpServletRequest;

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
     * json数据判断
     *
     * @param template
     * @return
     */
    public static boolean isJson(String template) {
        return StringUtils.isNotBlank(template) && StringUtils.startsWithAny(template, "{", "[");
    }

    public static void addRequestInfo(Bindings bindings) {
        HttpServletRequest request = HttpRequestUtils.getCurrentRequest();
        HttpRequestVo requestVo = new HttpRequestVo();
        if (request != null) {
            requestVo = HttpRequestUtils.parseRequestVo(request);
        }
        bindings.put("request", requestVo);
    }

}
