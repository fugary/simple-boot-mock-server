package com.fugary.simple.mock.utils;

import com.fugary.simple.mock.utils.servlet.HttpRequestUtils;
import com.fugary.simple.mock.web.vo.http.HttpRequestVo;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;

import javax.script.Bindings;
import javax.servlet.http.HttpServletRequest;
import java.text.MessageFormat;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private static final String REQUEST_PREFIX_KEY = "request.";

    private static final ThreadLocal<HttpRequestVo> CURRENT_REQUEST_VO = new ThreadLocal<>();

    public static void setCurrentRequestVo(HttpRequestVo requestVo) {
        CURRENT_REQUEST_VO.set(requestVo);
    }

    public static HttpRequestVo getCurrentRequestVo() {
        return CURRENT_REQUEST_VO.get();
    }

    public static void removeCurrentRequestVo() {
        CURRENT_REQUEST_VO.remove();
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
     * 添加js参数
     *
     * @param bindings
     */
    public static void addRequestInfo(Bindings bindings) {
        HttpRequestVo requestVo = getCurrentRequestVo();
        if (requestVo == null) {
            HttpServletRequest request = HttpRequestUtils.getCurrentRequest(); // 从request中获取
            requestVo = new HttpRequestVo();
            if (request != null) {
                requestVo = HttpRequestUtils.parseRequestVo(request);
            }
        }
        bindings.put("request", requestVo);
        bindings.put("_req", requestVo);
    }

    /**
     * 参数替换
     *
     * @param responseBody
     * @param requestVo
     * @return
     */
    public static String processResponseBody(String responseBody, HttpRequestVo requestVo) {
        Matcher matcher = Pattern.compile("\\{\\{(.+?)}}").matcher(responseBody);
        String result = responseBody;
        // 从responseBody中解析出{{xxx.xx}}格式的参数，从requestVo中取出值替换上去
        while (matcher.find()) {
            String paramKey = matcher.group(1); // 完整参数
            String beanParamKey = paramKey;
            if (paramKey.startsWith(REQUEST_PREFIX_KEY)) {
                beanParamKey = paramKey.substring(REQUEST_PREFIX_KEY.length());
            }
            Object paramValue = null;
            try {
                paramValue = BeanUtils.getProperty(requestVo, beanParamKey);
            } catch (Exception e) {
                log.error(MessageFormat.format("处理属性{0}错误", paramKey), e);
            }
            log.info("{}={}", paramKey, paramValue);
            String paramValueStr = Objects.requireNonNullElse(paramValue, StringUtils.EMPTY).toString();
            result = result.replace("{{" + paramKey + "}}", paramValueStr);
        }
        return result;
    }

    public static void main(String[] args) {
        String responseBody = "<abc>{{request.parameters.a}}</abc><bcd>{{request.parameters.b}}</bcd>";
        HttpRequestVo requestVo = new HttpRequestVo();
        requestVo.getParameters().put("a", "test");
        String result = processResponseBody(responseBody, requestVo);
        log.info("result={}", result);
    }
}
