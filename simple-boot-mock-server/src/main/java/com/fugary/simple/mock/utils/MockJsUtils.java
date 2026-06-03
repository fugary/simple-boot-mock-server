package com.fugary.simple.mock.utils;

import com.fugary.simple.mock.config.script.ScriptEngineConfig;
import com.fugary.simple.mock.script.ScriptEngineProvider;
import com.fugary.simple.mock.utils.servlet.HttpRequestUtils;
import com.fugary.simple.mock.web.vo.SimpleResult;
import com.fugary.simple.mock.web.vo.http.HttpRequestVo;
import com.fugary.simple.mock.web.vo.http.HttpResponseVo;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.SourceSection;

import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.servlet.http.HttpServletRequest;
import java.text.MessageFormat;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created on 2020/5/5 12:29 .<br>
 * 利用Jdk中的JavaScript脚本引擎，用mockjs执行虚构数据<br>
 *
 * @author gary.fu
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MockJsUtils {

    private static final String REQUEST_PREFIX_KEY = "request.";

    private static final String MOCK_STRINGIFY_PREFIX = "mockStringify(";

    private static final int SCRIPT_SOURCE_LINE_MAX_LENGTH = 240;

    private static final ThreadLocal<HttpRequestVo> CURRENT_REQUEST_VO = new ThreadLocal<>();

    private static final ThreadLocal<HttpResponseVo> CURRENT_RESPONSE_VO = new ThreadLocal<>();

    private static final ThreadLocal<ScriptEngine> CURRENT_SCRIPT_ENGINE = new ThreadLocal<>();

    public static void setCurrentRequestVo(HttpRequestVo requestVo) {
        CURRENT_REQUEST_VO.set(requestVo);
    }

    public static HttpRequestVo getCurrentRequestVo() {
        return CURRENT_REQUEST_VO.get();
    }

    public static void removeCurrentRequestVo() {
        CURRENT_REQUEST_VO.remove();
    }

    public static void setCurrentResponseVo(HttpResponseVo requestVo) {
        CURRENT_RESPONSE_VO.set(requestVo);
    }

    public static HttpResponseVo getCurrentResponseVo() {
        return CURRENT_RESPONSE_VO.get();
    }

    public static void removeCurrentResponseVo() {
        CURRENT_RESPONSE_VO.remove();
    }

    public static void setCurrentScriptEngine(ScriptEngine scriptEngine) {
        CURRENT_SCRIPT_ENGINE.set(scriptEngine);
    }

    public static ScriptEngine getCurrentScriptEngine() {
        return CURRENT_SCRIPT_ENGINE.get();
    }

    public static void removeCurrentScriptEngine() {
        CURRENT_SCRIPT_ENGINE.remove();
    }

    /**
     * json数据判断
     *
     * @param template
     * @return
     */
    public static boolean isJson(String template) {
        template = StringUtils.trimToEmpty(template);
        return StringUtils.isNotBlank(template)
                && StringUtils.startsWithAny(template, "{", "[")
                && StringUtils.endsWithAny(template, "}", "]");
    }

    /**
     * xml数据判断
     *
     * @param xmlData
     * @return
     */
    public static boolean isXml(String xmlData) {
        xmlData = StringUtils.trimToEmpty(xmlData);
        return StringUtils.isNotBlank(xmlData)
                && StringUtils.startsWith(xmlData, "<")
                && StringUtils.endsWith(xmlData, ">");
    }

    public static HttpRequestVo getHttpRequestVo() {
        HttpRequestVo requestVo = getCurrentRequestVo();
        if (requestVo == null) {
            HttpServletRequest request = HttpRequestUtils.getCurrentRequest(); // 从request中获取
            if (request != null) {
                requestVo = HttpRequestUtils.parseRequestVo(request);
            }
        }
        return requestVo;
    }

    /**
     * 获取Body数据
     *
     * @param bodyStr
     * @return
     */
    public static Object getObjectBody(String bodyStr) {
        if(MockJsUtils.isJson(bodyStr)){
            Object body = HttpRequestUtils.getJsonBody(bodyStr);
            if (body != null) {
                return body;
            }
        }
        if(MockJsUtils.isXml(bodyStr)){
            Object body = HttpRequestUtils.getXmlBody(bodyStr);
            if (body != null) {
                return body;
            }
        }
        return null;
    }

    /**
     * 去掉js后面的分号成为表达式
     *
     * @param expression
     * @return
     */
    public static String getJsExpression(String expression) {
        expression = StringUtils.trimToEmpty(expression);
        while (StringUtils.isNotBlank(expression) && expression.endsWith(";")) {
            expression = expression.substring(0, expression.length() - 1);
        }
        return expression;
    }

    public static String formatScriptError(String script, Throwable throwable) {
        String errorMessage = getScriptErrorMessage(throwable);
        StringBuilder builder = new StringBuilder("Script execution failed");
        if (StringUtils.isNotBlank(errorMessage)) {
            builder.append(": ").append(errorMessage);
        }
        String sourceLine = getScriptErrorSourceLine(script, throwable);
        if (StringUtils.isNotBlank(sourceLine)) {
            builder.append(". Source: ")
                    .append(StringUtils.abbreviate(StringUtils.normalizeSpace(unwrapMockStringify(sourceLine)),
                            SCRIPT_SOURCE_LINE_MAX_LENGTH));
        }
        return builder.toString();
    }

    private static String getScriptErrorMessage(Throwable throwable) {
        PolyglotException polyglotException = findCause(throwable, PolyglotException.class);
        if (polyglotException != null && StringUtils.isNotBlank(polyglotException.getMessage())) {
            return StringUtils.normalizeSpace(polyglotException.getMessage());
        }
        return unwrapJavaExceptionMessage(throwable == null ? null : throwable.getMessage());
    }

    private static String unwrapJavaExceptionMessage(String errorMessage) {
        String result = StringUtils.normalizeSpace(StringUtils.defaultString(errorMessage));
        int colonIndex = result.indexOf(": ");
        while (colonIndex > 0 && isJavaExceptionPrefix(result.substring(0, colonIndex))) {
            result = result.substring(colonIndex + 2);
            colonIndex = result.indexOf(": ");
        }
        return result;
    }

    private static boolean isJavaExceptionPrefix(String prefix) {
        return StringUtils.endsWith(prefix, "Exception")
                || (StringUtils.contains(prefix, ".") && StringUtils.endsWith(prefix, "Error"));
    }

    private static String getScriptErrorSourceLine(String script, Throwable throwable) {
        String sourceLine = getScriptSourceLine(script, getScriptErrorLineNumber(throwable));
        if (StringUtils.isBlank(sourceLine)) {
            sourceLine = getPolyglotSourceText(throwable);
        }
        return sourceLine;
    }

    private static int getScriptErrorLineNumber(Throwable throwable) {
        ScriptException scriptException = findCause(throwable, ScriptException.class);
        if (scriptException != null && scriptException.getLineNumber() > 0) {
            return scriptException.getLineNumber();
        }
        SourceSection sourceSection = getPolyglotSourceSection(throwable);
        return sourceSection == null ? -1 : sourceSection.getStartLine();
    }

    private static String getPolyglotSourceText(Throwable throwable) {
        SourceSection sourceSection = getPolyglotSourceSection(throwable);
        if (sourceSection != null) {
            return sourceSection.getCharacters().toString();
        }
        return null;
    }

    private static SourceSection getPolyglotSourceSection(Throwable throwable) {
        PolyglotException polyglotException = findCause(throwable, PolyglotException.class);
        SourceSection sourceSection = polyglotException == null ? null : polyglotException.getSourceLocation();
        return sourceSection != null && sourceSection.isAvailable() ? sourceSection : null;
    }

    private static <T extends Throwable> T findCause(Throwable throwable, Class<T> type) {
        Throwable current = throwable;
        while (current != null) {
            if (type.isInstance(current)) {
                return type.cast(current);
            }
            current = current.getCause();
        }
        return null;
    }

    private static String getScriptSourceLine(String script, int lineNumber) {
        if (StringUtils.isBlank(script) || lineNumber <= 0) {
            return null;
        }
        String normalizedScript = StringUtils.replace(StringUtils.replace(script, "\r\n", "\n"), "\r", "\n");
        String[] lines = StringUtils.splitByWholeSeparatorPreserveAllTokens(normalizedScript, "\n");
        return lineNumber <= lines.length ? lines[lineNumber - 1] : null;
    }

    private static String unwrapMockStringify(String sourceLine) {
        String result = getJsExpression(StringUtils.trimToEmpty(sourceLine));
        if (StringUtils.startsWith(result, MOCK_STRINGIFY_PREFIX) && StringUtils.endsWith(result, ")")) {
            return StringUtils.trimToEmpty(result.substring(MOCK_STRINGIFY_PREFIX.length(), result.length() - 1));
        }
        return sourceLine;
    }

    /**
     * 参数替换
     *
     * @param responseBody
     * @param requestVo
     * @return
     */
    public static String processResponseBody(String responseBody, HttpRequestVo requestVo, Function<String, Object> processor) {
        Matcher matcher = Pattern.compile("\\{\\{(.+?)}}", Pattern.DOTALL).matcher(responseBody);
        String result = responseBody;
        // 从responseBody中解析出{{xxx.xx}}格式的参数，从requestVo中取出值替换上去
        while (matcher.find()) {
            String paramKey = matcher.group(1); // 完整参数
            String beanParamKey = paramKey;
            Object paramValue = null;
            try {
                if (processor == null) {
                    if (paramKey.startsWith(REQUEST_PREFIX_KEY)) {
                        beanParamKey = paramKey.substring(REQUEST_PREFIX_KEY.length());
                    }
                    paramValue = BeanUtils.getProperty(requestVo, beanParamKey);
                } else {
                    paramValue = processor.apply(beanParamKey);
                    if (paramValue instanceof SimpleResult) {
                        SimpleResult<?> simpleResult = (SimpleResult<?>) paramValue;
                        log.error("处理表达式{}错误: {}", beanParamKey, simpleResult.getResultData());
                        paramValue = JsonUtils.toJson(simpleResult);
                    }
                }
            } catch (Exception e) {
                log.error(MessageFormat.format("处理属性{0}错误", paramKey), e);
            }
            log.info("{}={}", paramKey, paramValue);
            String paramValueStr = Objects.requireNonNullElse(paramValue, StringUtils.EMPTY).toString();
            result = result.replace("{{" + paramKey + "}}", paramValueStr);
        }
        return result;
    }

    /**
     * 失效并准备ScriptEngine
     *
     * @param scriptEnginePool
     */
    public static void invalidateCurrentAndPrepareScriptEngine(GenericObjectPool<ScriptEngine> scriptEnginePool) {
        ScriptEngine scriptEngine = MockJsUtils.getCurrentScriptEngine();
        if (scriptEngine != null) {
            MockJsUtils.removeCurrentRequestVo();
            MockJsUtils.removeCurrentResponseVo();
            MockJsUtils.removeCurrentScriptEngine();
            try {
                // 失效ScriptEngine，并生成新的
                scriptEnginePool.invalidateObject(scriptEngine);
                CompletableFuture.runAsync(() -> {
                    try {
                        scriptEnginePool.preparePool();
                    } catch (Exception e) {
                        log.error("准备新的ScriptEngine失败", e);
                    }
                });
            } catch (Exception e) {
                log.error("标记ScriptEngine失效错误", e);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        String responseBody = "<abc>{{request.parameters.a.split(' ')[0]}}</abc><bcd>{{request.parameters.a.split(' ')[1]}}</bcd>";
        ScriptEngineProvider scriptEngineProvider = new ScriptEngineConfig().scriptEngineProvider();
        HttpRequestVo requestVo = new HttpRequestVo();
        requestVo.getParameters().put("a", "hello world");
        MockJsUtils.setCurrentRequestVo(requestVo);
        String result = processResponseBody(responseBody, requestVo, scriptEngineProvider::eval);
        MockJsUtils.removeCurrentRequestVo();
        log.info("result={}", result);
        String xmlStr = "<book>\n" +
                "<title> Learning Amazon Web Services </title>\n" +
                "<author> Mark Wilkins </author>\n" +
                "</book>";
        Map map = XmlUtils.fromXml(xmlStr, Map.class);
        log.info("{}", map);
        requestVo = new HttpRequestVo();
        requestVo.getParameters().put("b", "test");
        MockJsUtils.setCurrentRequestVo(requestVo);
        Object execResult = scriptEngineProvider.eval("JSON.stringify(Mock.mock({a({_req}) {return Mock.mock(request.parameters)}, b: request.parameters}))");
        requestVo.getParameters().put("c", "test1");
        Object execResult1 = scriptEngineProvider.eval("JSON.stringify(Mock.mock({a({_req}) {return Mock.mock(request.parameters)}, b: request.parameters}))");
        MockJsUtils.removeCurrentRequestVo();
        Object execResult2 = scriptEngineProvider.eval("JSON.stringify(Mock.mock({a({_req}) {return Mock.mock(request.parameters)}, b: request.parameters}))");
        log.info("result={}", execResult);
        log.info("result1={}", execResult1);
        log.info("result1={}", execResult2);
    }
}
