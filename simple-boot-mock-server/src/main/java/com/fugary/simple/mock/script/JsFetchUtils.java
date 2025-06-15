package com.fugary.simple.mock.script;

import com.oracle.truffle.js.scriptengine.GraalJSScriptEngine;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

/**
 * @author Gary.Fu
 * @date 2025/6/15
 */
@Slf4j
public class JsFetchUtils {
    /**
     * 内部http客户端
     */
    private static final HttpClient client = HttpClient.newHttpClient();

    /**
     * 支持promise和fetch功能（使用反射比较多，谨慎选择）
     *
     * @param script
     * @param scriptEngine
     * @param scriptContext
     * @return
     * @throws ScriptException
     */
    public static Object internalEval(String script, ScriptEngine scriptEngine, ScriptContext scriptContext) throws ScriptException {
        Source source = Source.newBuilder("js", script, "<eval>").buildLiteral();
        if (scriptEngine instanceof GraalJSScriptEngine) {
            Bindings engineBindings = (Bindings) invokeMethod(scriptEngine, getMethod(GraalJSScriptEngine.class, "getOrCreateGraalJSBindings", ScriptContext.class), scriptContext);
            Assert.notNull(engineBindings, "polyglot graal.js binding is null");
            Context polyglotContext = (Context) invokeMethod(engineBindings, getMethod(engineBindings.getClass(), "getContext"));
            Assert.notNull(polyglotContext, "polyglot context is null");
            JsFetchUtils.injectFetch(polyglotContext);
            invokeMethod(GraalJSScriptEngine.class, getMethod(GraalJSScriptEngine.class, "updateDelegatingIOStreams", Context.class, ScriptContext.class), polyglotContext, scriptContext);
            try {
                invokeMethod(engineBindings, getMethod(engineBindings.getClass(), "importGlobalBindings", ScriptContext.class), scriptContext);
                Value value = polyglotContext.eval(source);
                // 判断是否是 Promise
                if (value.canInvokeMember("then")) {
                    CountDownLatch latch = new CountDownLatch(1);
                    final Object[] resultHolder = new Object[1];
                    value.invokeMember("then", (ProxyExecutable) (args) -> {
                        resultHolder[0] = args[0].as(Object.class);
                        latch.countDown();
                        return null;
                    }, (ProxyExecutable) (args) -> {
                        resultHolder[0] = args[0].as(Object.class);
                        latch.countDown();
                        return null;
                    });
                    latch.await();
                    return resultHolder[0];
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
            }
        }
        return null;
    }

    public static <T> Method getMethod(Class<T> clazz, String methodName, Class<?>... classes) {
        try {
            Method evalMethod = clazz.getDeclaredMethod(methodName, classes);
            evalMethod.setAccessible(true);
            return evalMethod;
        } catch (Exception e) {
            log.error(methodName + "方法不存在", e);
        }
        return null;
    }

    public static Object invokeMethod(Object target, Method method, Object... args) {
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

    /**
     * fetch注入上下文
     * @param context
     */
    public static void injectFetch(Context context) {
        if (context != null) {
            Value globalThis = context.eval("js", "globalThis");
            ProxyExecutable fetchFunction = getFetchFunction();
            globalThis.putMember("fetch", fetchFunction);
        }
    }

    private static ProxyExecutable getFetchFunction() {
        Context context = Context.newBuilder().allowAllAccess(true).build();
        return args -> {
            if (args.length < 1) throw new IllegalArgumentException("fetch requires at least 1 argument");
            String url = args[0].asString();
            Value optionsValue = args.length > 1 ? args[1] : null;
            String method = "GET";
            Map<String, String> headers = new HashMap<>();
            String body = null;
            if (optionsValue != null && optionsValue.hasMembers()) {
                if (optionsValue.hasMember("method")) {
                    method = optionsValue.getMember("method").asString().toUpperCase();
                }
                if (optionsValue.hasMember("headers")) {
                    Value headersVal = optionsValue.getMember("headers");
                    for (String key : headersVal.getMemberKeys()) {
                        headers.put(key, headersVal.getMember(key).asString());
                    }
                }
                if (optionsValue.hasMember("body")) {
                    body = optionsValue.getMember("body").asString();
                }
            }
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url));
            switch (method) {
                case "POST":
                    builder.POST(HttpRequest.BodyPublishers.ofString(body == null ? "" : body));
                    break;
                case "PUT":
                    builder.PUT(HttpRequest.BodyPublishers.ofString(body == null ? "" : body));
                    break;
                case "DELETE":
                    if (body != null) {
                        builder.method("DELETE", HttpRequest.BodyPublishers.ofString(body));
                    } else {
                        builder.DELETE();
                    }
                    break;
                default:
                    builder.GET();
            }
            headers.forEach(builder::header);
            HttpRequest request = builder.build();
            CompletableFuture<Value> future = new CompletableFuture<>();
            client.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                    .whenComplete((response, ex) -> {
                        Context asyncContext = Context.newBuilder().allowAllAccess(true).build();
                        if (ex != null) {
                            future.completeExceptionally(ex);
                        } else {
                            byte[] responseBytes = response.body();
                            String responseText = new String(responseBytes); // 简单用系统默认编码，按需改
                            Map<String, Object> headerMap = response.headers()
                                    .map()
                                    .entrySet()
                                    .stream()
                                    .collect(Collectors.toMap(
                                            Map.Entry::getKey,
                                            e -> String.join(", ", e.getValue())
                                    ));
                            HttpStatus httpStatus = HttpStatus.resolve(response.statusCode());
                            ProxyObject responseObj = ProxyObject.fromMap(Map.of(
                                    "status", response.statusCode(),
                                    "ok", response.statusCode() >= 200 && response.statusCode() < 300,
                                    "statusText", httpStatus != null ? httpStatus.getReasonPhrase() : StringUtils.EMPTY,
                                    "headers", ProxyObject.fromMap(headerMap),
                                    // text() 返回字符串
                                    "text", (ProxyExecutable) ignored -> Value.asValue(responseText),
                                    // json() 返回 JSON 解析结果
                                    "json", (ProxyExecutable) ignored -> {
                                        try {
                                            return asyncContext.eval("js", "JSON.parse").execute(responseText);
                                        } catch (Exception e) {
                                            throw new RuntimeException("Invalid JSON in response");
                                        }
                                    },
                                    // blob() 返回 Uint8Array
                                    "blob", (ProxyExecutable) ignored -> {
                                        // 先把字节数组转JS Uint8Array
                                        Value uint8ArrayConstructor = asyncContext.eval("js", "Uint8Array");
                                        return uint8ArrayConstructor.newInstance(responseBytes);
                                    }
                            ));
                            future.complete(Value.asValue(responseObj));
                        }
                    });

            Value promiseConstructor = context.eval("js", "(f => new Promise(f))");
            return promiseConstructor.execute((ProxyExecutable) (promiseArgs) -> {
                Value resolve = promiseArgs[0];
                Value reject = promiseArgs[1];
                future.whenComplete((result, err) -> {
                    if (err != null) {
                        reject.executeVoid(err.getMessage());
                    } else {
                        resolve.executeVoid(result);
                    }
                });
                return null;
            });
        };
    }

}
