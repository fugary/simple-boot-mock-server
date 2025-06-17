package com.fugary.simple.mock.push;

import com.fugary.simple.mock.web.vo.NameValue;
import com.fugary.simple.mock.web.vo.query.MockParamsVo;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

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

    private ExecutorService fetchExecutor = Executors.newFixedThreadPool(20);

    @Override
    public Object internalEval(String script, ScriptEngine scriptEngine, ScriptContext scriptContext) throws ScriptException {
        Source source = Source.newBuilder("js", script, "<eval>").buildLiteral();
        if (scriptEngine instanceof GraalJSScriptEngine) {
            Bindings engineBindings = (Bindings) invokeMethod(scriptEngine, getMethod(GraalJSScriptEngine.class, "getOrCreateGraalJSBindings", ScriptContext.class), scriptContext);
            Assert.notNull(engineBindings, "polyglot graal.js binding is null");
            Context polyglotContext = (Context) invokeMethod(engineBindings, getMethod(engineBindings.getClass(), "getContext"));
            Assert.notNull(polyglotContext, "polyglot context is null");
            injectFetch(polyglotContext);
            invokeMethod(GraalJSScriptEngine.class, getMethod(GraalJSScriptEngine.class, "updateDelegatingIOStreams", Context.class, ScriptContext.class), polyglotContext, scriptContext);
            try {
                invokeMethod(engineBindings, getMethod(engineBindings.getClass(), "importGlobalBindings", ScriptContext.class), scriptContext);
                Value value = polyglotContext.eval(source);
                // 判断是否是 Promise
                if (value.canInvokeMember("then")) {
                    CompletableFuture<Object> future = new CompletableFuture<>();
                    value.invokeMember("then", (ProxyExecutable) (args) -> {
                        future.complete(args[0].as(Object.class));
                        return null;
                    }, (ProxyExecutable) (args) -> {
                        future.completeExceptionally(new ScriptException(args[0].toString()));
                        return null;
                    });
                    return future.get();
                }
                return value.as(Object.class);
            } catch (PolyglotException e) {
                ScriptException scriptException = (ScriptException) invokeMethod(GraalJSScriptEngine.class, getMethod(GraalJSScriptEngine.class, "toScriptException", PolyglotException.class), e);
                if (scriptException != null) {
                    throw scriptException;
                }
            } catch (InterruptedException e) {
                log.error("执行中断异常", e);
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                if (e.getCause() instanceof ScriptException) {
                    throw (ScriptException) e.getCause();
                }
                throw new ScriptException((Exception) e.getCause());
            }
        }
        return null;
    }

    protected void injectFetch(Context context) throws PolyglotException {
        if (context != null) {
            Value globalThis = context.eval("js", "globalThis");
            ProxyExecutable fetchFunction = getFetchFunction(context);
            globalThis.putMember("fetch", fetchFunction);
        }
    }

    protected ProxyExecutable getFetchFunction(Context context) throws PolyglotException {
        return args -> {
            if (args.length < 1) throw new IllegalArgumentException("fetch requires at least 1 argument");
            String url = args[0].asString();
            Value optionsValue = args.length > 1 ? args[1] : null;
            String method = "GET";
            List<NameValue> headers = new ArrayList<>();
            String body = null;
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
                    body = optionsValue.getMember("body").asString();
                }
            }
            MockParamsVo mockParams = new MockParamsVo();
            mockParams.setTargetUrl(url);
            mockParams.setRequestPath(StringUtils.EMPTY);
            mockParams.setMethod(method);
            mockParams.setRequestBody(body);
            mockParams.setHeaderParams(headers);
            CompletableFuture<Value> future = new CompletableFuture<>();
            log.info("fetch请求url:{}", url);
            fetchExecutor.execute(() -> {
                try {
                    ResponseEntity<byte[]> response = mockPushProcessor.doPush(mockParams);
                    log.info("fetch请求url完成:{}/{}", url, response);
                    HttpStatus httpStatus = response.getStatusCode();
                    byte[] responseBytes = response.getBody();
                    Charset charset = StandardCharsets.UTF_8;
                    MediaType contentType = response.getHeaders().getContentType();
                    if (contentType != null && contentType.getCharset() != null) {
                        charset = contentType.getCharset();
                    }
                    boolean isSuccess = httpStatus.is2xxSuccessful();
                    String responseText = new String(Objects.requireNonNullElse(responseBytes, new byte[0]), charset);
                    Map<String, String> headerMap = response.getHeaders().toSingleValueMap();
                    ProxyObject responseObj = ProxyObject.fromMap(Map.of(
                            "status", response.getStatusCode(),
                            "ok", isSuccess,
                            "statusText", httpStatus.getReasonPhrase(),
                            "headers", ProxyObject.fromMap(headerMap.entrySet()
                                    .stream()
                                    .collect(Collectors.toMap(
                                            Map.Entry::getKey,
                                            Map.Entry::getValue
                                    ))),
                            // text() 返回字符串
                            "text", (ProxyExecutable) ignored -> Value.asValue(responseText),
                            // json() 返回 JSON 解析结果
                            "json", (ProxyExecutable) ignored -> {
                                try {
                                    return context.eval("js", "JSON.parse").execute(responseText);
                                } catch (Exception e) {
                                    throw new RuntimeException("Invalid JSON in response");
                                }
                            },
                            // blob() 返回 Uint8Array
                            "blob", (ProxyExecutable) ignored -> {
                                // 先把字节数组转JS Uint8Array
                                Value uint8ArrayConstructor = context.eval("js", "Uint8Array");
                                return uint8ArrayConstructor.newInstance(responseBytes);
                            }
                    ));
                    future.complete(Value.asValue(responseObj));
                } catch (Throwable e) {
                    future.completeExceptionally(e);
                }
            });
            Value promiseConstructor = context.eval("js", "(f => new Promise(f))");
            return promiseConstructor.execute((ProxyExecutable) (promiseArgs) -> {
                Value resolve = promiseArgs[0];
                Value reject = promiseArgs[1];
                future.whenComplete((result, err) -> {
                    log.info("fetch请求构建Promise:{}/{}", url, result, err);
                    if (err != null) {
                        reject.executeVoid(context.eval("js", "new Error").newInstance(err.getMessage()));
                    } else {
                        resolve.executeVoid(result);
                    }
                });
                return null;
            });
        };
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
