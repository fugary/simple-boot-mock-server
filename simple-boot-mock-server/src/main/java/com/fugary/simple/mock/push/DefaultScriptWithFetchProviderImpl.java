package com.fugary.simple.mock.push;

import com.fugary.simple.mock.service.impl.mock.MockDiagnoseRecorder;
import com.fugary.simple.mock.utils.MockDiagnoseContext;
import com.fugary.simple.mock.utils.SimpleMockUtils;
import com.fugary.simple.mock.web.vo.NameValue;
import com.fugary.simple.mock.web.vo.query.MockParamsVo;
import com.fugary.simple.mock.web.vo.result.MockDiagnoseVo;
import com.oracle.truffle.js.scriptengine.GraalJSScriptEngine;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Create date 2025/6/17<br>
 *
 * @author gary.fu
 */
@Getter
@Setter
@Slf4j
@Component
public class DefaultScriptWithFetchProviderImpl implements ScriptWithFetchProvider {

    @Autowired
    private MockPushProcessor mockPushProcessor;

    @Autowired
    @Qualifier("fetchScriptThreadPool")
    private ExecutorService fetchScriptThreadPool;

    @Override
    public Object internalEval(String script, ScriptEngine scriptEngine, ScriptContext scriptContext) throws ScriptException {
        Source source = Source.newBuilder("js", script, "<eval>").buildLiteral();
        if (scriptEngine instanceof GraalJSScriptEngine) {
            Bindings engineBindings = (Bindings) invokeMethod(scriptEngine, getMethod(GraalJSScriptEngine.class, "getOrCreateGraalJSBindings", ScriptContext.class), scriptContext);
            Assert.notNull(engineBindings, "polyglot graal.js binding is null");
            Context polyglotContext = (Context) invokeMethod(engineBindings, getMethod(engineBindings.getClass(), "getContext"));
            Assert.notNull(polyglotContext, "polyglot context is null");
            invokeMethod(GraalJSScriptEngine.class, getMethod(GraalJSScriptEngine.class, "updateDelegatingIOStreams", Context.class, ScriptContext.class), polyglotContext, scriptContext);
            try {
                invokeMethod(engineBindings, getMethod(engineBindings.getClass(), "importGlobalBindings", ScriptContext.class), scriptContext);
                Value value = polyglotContext.eval(source);
                return SimpleMockUtils.executeValue(value, 30);
            } catch (PolyglotException e) {
                ScriptException scriptException = (ScriptException) invokeMethod(GraalJSScriptEngine.class, getMethod(GraalJSScriptEngine.class, "toScriptException", PolyglotException.class), e);
                if (scriptException != null) {
                    throw scriptException;
                }
            }
        }
        return null;
    }

    @Override
    public ProxyExecutable getFetchFunction(Context context) {
        return args -> {
            if (args.length < 1) throw new IllegalArgumentException("fetch requires at least 1 argument");
            String url = args[0].asString();
            Value optionsValue = args.length > 1 ? args[1] : null;
            String method = "GET";
            List<NameValue> headers = new ArrayList<>();
            String body = null;
            Long timeout = null;
            if (optionsValue != null && optionsValue.hasMembers()) {
                if (optionsValue.hasMember("method")) {
                    method = optionsValue.getMember("method").asString().toUpperCase();
                }
                if (optionsValue.hasMember("headers")) {
                    Value headersVal = optionsValue.getMember("headers");
                    for (String key : headersVal.getMemberKeys()) {
                        headers.add(new NameValue(key, headersVal.getMember(key).asString()));
                    }
                }
                if (optionsValue.hasMember("body")) {
                    try {
                        Value bodyValue = context.eval("js", "mockStringify").execute(optionsValue.getMember("body"));
                        body = SimpleMockUtils.executeValue(bodyValue, 10).toString();
                    } catch (ScriptException e) {
                        body = optionsValue.getMember("body").asString();
                    }
                }
                if (optionsValue.hasMember("timeout")) {
                    timeout = optionsValue.getMember("timeout").asLong();
                }
            }
            MockParamsVo mockParams = new MockParamsVo();
            mockParams.setTargetUrl(url);
            mockParams.setRequestPath(StringUtils.EMPTY);
            mockParams.setMethod(method);
            mockParams.setRequestBody(body);
            mockParams.setHeaderParams(headers);
            CompletableFuture<Value> future = new CompletableFuture<>();
            AtomicBoolean diagnoseRecorded = new AtomicBoolean(false);
            MockDiagnoseVo diagnose = MockDiagnoseContext.get();
            MockDiagnoseRecorder diagnoseRecorder = MockDiagnoseRecorder.of(diagnose);
            String fetchMethod = method;
            if (timeout != null) {
                future.orTimeout(timeout, TimeUnit.MILLISECONDS);
            }
            log.info("fetch请求url:{}", url);
            fetchScriptThreadPool.execute(() -> {
                long startTime = System.currentTimeMillis();
                try {
                    ResponseEntity<byte[]> response = mockPushProcessor.doPush(mockParams);
                    log.info("fetch请求url完成:{}/{}", url, response);
                    HttpStatus httpStatus = response.getStatusCode();
                    byte[] responseBytes = Objects.requireNonNullElse(response.getBody(), new byte[0]);
                    Charset charset = StandardCharsets.UTF_8;
                    MediaType contentType = response.getHeaders().getContentType();
                    if (contentType != null && contentType.getCharset() != null) {
                        charset = contentType.getCharset();
                    }
                    boolean isSuccess = httpStatus.is2xxSuccessful();
                    recordFetchDiagnose(diagnoseRecorder, diagnoseRecorded, fetchMethod, url,
                            httpStatus.value(), contentType, System.currentTimeMillis() - startTime, null);
                    String responseText = new String(responseBytes, charset);
                    Map<String, String> headerMap = response.getHeaders().toSingleValueMap();
                    Map<String, Object> headerObj = new LinkedHashMap<>(headerMap);
                    headerObj.put("get", (ProxyExecutable) headerArgs -> getHeaderIgnoreCase(headerMap, headerArgs));
                    headerObj.put("has", (ProxyExecutable) headerArgs -> getHeaderIgnoreCase(headerMap, headerArgs) != null);
                    ProxyObject responseObj = ProxyObject.fromMap(Map.of(
                            "status", httpStatus.value(),
                            "ok", isSuccess,
                            "statusText", httpStatus.getReasonPhrase(),
                            "headers", ProxyObject.fromMap(headerObj),
                            "url", url,
                            "text", (ProxyExecutable) ignored -> Value.asValue(responseText),
                            "json", (ProxyExecutable) ignored -> {
                                try {
                                    return context.eval("js", "JSON.parse").execute(responseText);
                                } catch (Exception e) {
                                    throw new RuntimeException("Invalid JSON in response");
                                }
                            },
                            "arrayBuffer", (ProxyExecutable) ignored -> context.eval("js", "Uint8Array").newInstance(responseBytes).getMember("buffer"),
                            "blob", (ProxyExecutable) ignored -> context.eval("js", "Uint8Array").newInstance(responseBytes)
                    ));
                    future.complete(Value.asValue(responseObj));
                } catch (Throwable e) {
                    recordFetchDiagnose(diagnoseRecorder, diagnoseRecorded, fetchMethod, url,
                            null, null, System.currentTimeMillis() - startTime, e);
                    future.completeExceptionally(e);
                }
            });
            Value promiseConstructor = context.eval("js", "(f => new Promise(f))");
            return promiseConstructor.execute((ProxyExecutable) (promiseArgs) -> {
                Value resolve = promiseArgs[0];
                Value reject = promiseArgs[1];
                future.whenComplete((result, err) -> {
                    runWithDiagnoseContext(diagnose, () -> {
                        log.info("fetch请求构建Promise:{}/{}", url, result, err);
                        if (err != null) {
                            recordFetchDiagnose(diagnoseRecorder, diagnoseRecorded, fetchMethod, url,
                                    null, null, null, err);
                            reject.executeVoid(Value.asValue(err));
                        } else {
                            resolve.executeVoid(result);
                        }
                    });
                });
                return null;
            });
        };
    }

    private void recordFetchDiagnose(MockDiagnoseRecorder diagnoseRecorder, AtomicBoolean recorded, String method, String url,
            Integer statusCode, MediaType contentType, Long durationMs, Throwable error) {
        if (recorded.compareAndSet(false, true)) {
            diagnoseRecorder.externalFetch(method, url, statusCode,
                    contentType == null ? null : contentType.toString(), durationMs, error);
        }
    }

    private void runWithDiagnoseContext(MockDiagnoseVo diagnose, Runnable runnable) {
        if (diagnose == null) {
            runnable.run();
            return;
        }
        MockDiagnoseVo previous = MockDiagnoseContext.get();
        try {
            MockDiagnoseContext.set(diagnose);
            runnable.run();
        } finally {
            if (previous == null) {
                MockDiagnoseContext.clear();
            } else {
                MockDiagnoseContext.set(previous);
            }
        }
    }

    private static String getHeaderIgnoreCase(Map<String, String> headerMap, Value[] args) {
        if (args.length == 0 || args[0] == null || args[0].isNull()) {
            return null;
        }
        String headerName = args[0].isString() ? args[0].asString() : args[0].toString();
        for (Map.Entry<String, String> entry : headerMap.entrySet()) {
            if (StringUtils.equalsIgnoreCase(entry.getKey(), headerName)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static <T> Method getMethod(Class<T> clazz, String methodName, Class<?>... classes) {
        try {
            Method evalMethod = clazz.getDeclaredMethod(methodName, classes);
            evalMethod.setAccessible(true);
            return evalMethod;
        } catch (Exception e) {
            log.error(methodName + "方法不存在", e);
        }
        return null;
    }

    private static Object invokeMethod(Object target, Method method, Object... args) {
        if (method != null) {
            try {
                method.setAccessible(true);
                return method.invoke(target, args);
            } catch (Exception e) {
                log.error(MessageFormat.format("执行方法【{0}】错误", method.getName()), e);
            }
        }
        return null;
    }

}
