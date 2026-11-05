package com.fugary.simple.mock.push;

import com.fugary.simple.mock.service.impl.mock.MockDiagnoseRecorder;
import com.fugary.simple.mock.utils.MockDiagnoseContext;
import com.fugary.simple.mock.utils.SimpleMockUtils;
import com.fugary.simple.mock.web.vo.NameValue;
import com.fugary.simple.mock.web.vo.query.MockParamsVo;
import com.fugary.simple.mock.web.vo.result.MockDiagnoseVo;
import com.oracle.truffle.js.scriptengine.GraalJSScriptEngine;
import lombok.AllArgsConstructor;
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

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

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

    private static final ThreadLocal<FetchExecutionScope> CURRENT_SCOPE = new ThreadLocal<>();

    private static final AtomicInteger WATCHDOG_THREAD_COUNTER = new AtomicInteger(1);

    private static final ScheduledThreadPoolExecutor TIMEOUT_SCHEDULER = createTimeoutScheduler();

    @Autowired
    private MockPushProcessor mockPushProcessor;

    @Autowired
    @Qualifier("fetchScriptThreadPool")
    private ExecutorService fetchScriptThreadPool;

    private long executionTimeoutSeconds = 30;

    private long bodyExecutionTimeoutSeconds = 10;

    private static ScheduledThreadPoolExecutor createTimeoutScheduler() {
        ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(2, runnable -> {
            Thread thread = new Thread(runnable,
                    "mock-script-watchdog-" + WATCHDOG_THREAD_COUNTER.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        });
        scheduler.setRemoveOnCancelPolicy(true);
        return scheduler;
    }

    @Override
    public Object internalEval(String script, ScriptEngine scriptEngine, ScriptContext scriptContext) throws ScriptException {
        return eval(script, scriptEngine, scriptContext, true);
    }

    @Override
    public Object eval(String script, ScriptEngine engine, ScriptContext scriptContext, boolean fetchEnabled)
            throws ScriptException {
        if (!(engine instanceof GraalJSScriptEngine)) {
            return fetchEnabled ? null : engine.eval(script, scriptContext);
        }
        Context context = ((GraalJSScriptEngine) engine).getPolyglotContext(scriptContext);
        FetchExecutionScope previous = CURRENT_SCOPE.get();
        FetchExecutionScope scope = new FetchExecutionScope(context, executionTimeoutSeconds);
        CURRENT_SCOPE.set(fetchEnabled ? scope : null);
        try {
            Object result;
            if (fetchEnabled) {
                Bindings bindings = (Bindings) invokeMethod(engine, getMethod(GraalJSScriptEngine.class,
                        "getOrCreateGraalJSBindings", ScriptContext.class), scriptContext);
                invokeMethod(GraalJSScriptEngine.class, getMethod(GraalJSScriptEngine.class, "updateDelegatingIOStreams",
                        Context.class, ScriptContext.class), context, scriptContext);
                invokeMethod(bindings, getMethod(bindings.getClass(), "importGlobalBindings", ScriptContext.class), scriptContext);
                Value value = context.eval(Source.newBuilder("js", script, "<eval>").buildLiteral());
                result = SimpleMockUtils.executeValue(value, executionTimeoutSeconds, scope);
            } else {
                result = engine.eval(script, scriptContext);
            }
            scope.checkDeadline();
            return result;
        } catch (PolyglotException e) {
            if (scope.isTimedOut() || e.isInterrupted()) {
                throw scope.createTimeoutException(e);
            }
            ScriptException error = toScriptException(e);
            throw error != null ? error : new ScriptException(e);
        } catch (ScriptException e) {
            if (scope.isTimedOut()) {
                throw scope.createTimeoutException(e);
            }
            throw e;
        } finally {
            scope.close();
            if (previous == null) {
                CURRENT_SCOPE.remove();
            } else {
                CURRENT_SCOPE.set(previous);
            }
        }
    }

    @Override
    public ProxyExecutable getFetchFunction(Context context) {
        return args -> {
            if (args.length < 1) throw new IllegalArgumentException("fetch requires at least 1 argument");
            String url = args[0].asString();
            Value optionsValue = args.length > 1 ? args[1] : null;
            String method = "GET";
            List<NameValue> headers = new ArrayList<>();
            Value body = null;
            Long timeout = null;
            FetchExecutionScope executionScope = CURRENT_SCOPE.get();
            if (executionScope == null || !executionScope.isActive()) {
                return rejectedPromise(context, "Fetch is not available outside active script execution scope: " + url);
            }
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
                    body = context.eval("js", "mockStringify").execute(optionsValue.getMember("body"));
                }
                if (optionsValue.hasMember("timeout")) {
                    timeout = optionsValue.getMember("timeout").asLong();
                }
            }
            MockParamsVo mockParams = new MockParamsVo();
            mockParams.setTargetUrl(url);
            mockParams.setRequestPath(StringUtils.EMPTY);
            mockParams.setMethod(method);
            mockParams.setHeaderParams(headers);
            if (body != null && body.canInvokeMember("then")) {
                Long fetchTimeout = timeout;
                Value ready = awaitBody(context, executionScope, body);
                return ready.invokeMember("then", (ProxyExecutable) values -> {
                    mockParams.setRequestBody(values[0].asString());
                    return sendFetch(context, executionScope, mockParams, fetchTimeout);
                });
            }
            mockParams.setRequestBody(body == null ? null : body.asString());
            return sendFetch(context, executionScope, mockParams, timeout);
        };
    }

    private Value awaitBody(Context context, FetchExecutionScope scope, Value body) {
        // 只注册微任务，立即返回 JS；阻塞在宿主调用中会使已完成的 Promise 也无法推进。
        CompletableFuture<String> result = new CompletableFuture<>();
        result.orTimeout(bodyExecutionTimeoutSeconds, TimeUnit.SECONDS);
        body.invokeMember("then", (ProxyExecutable) values -> result.complete(values[0].asString()),
                (ProxyExecutable) errors -> result.completeExceptionally(new ScriptException(errors[0].toString())));
        return toPromise(context, scope, result, (resolve, reject, text, error) -> {
            if (error == null) {
                resolve.executeVoid(text);
            } else {
                reject.executeVoid(error instanceof TimeoutException
                        ? "Fetch body timed out after " + bodyExecutionTimeoutSeconds + " seconds"
                        : error.getMessage());
            }
        });
    }

    private Value sendFetch(Context context, FetchExecutionScope executionScope, MockParamsVo mockParams, Long fetchTimeout) {
        CompletableFuture<FetchResponseData> future = createFetchFuture();
        MockDiagnoseRecorder diagnoseRecorder = MockDiagnoseRecorder.of(MockDiagnoseContext.get());
        String fetchMethod = mockParams.getMethod();
        String url = mockParams.getTargetUrl();
        if (fetchTimeout != null) {
            future.orTimeout(fetchTimeout, TimeUnit.MILLISECONDS);
        }
        log.info("fetch请求url:{}", url);
        Future<?> httpTask = fetchScriptThreadPool.submit(() -> {
            if (!executionScope.isActive() || future.isDone()) {
                future.cancel(false);
                return;
            }
            long startTime = System.currentTimeMillis();
            try {
                ResponseEntity<byte[]> response = mockPushProcessor.doPush(mockParams);
                long durationMs = System.currentTimeMillis() - startTime;
                log.info("fetch请求url完成:{}/{}", url, response);
                HttpStatus httpStatus = response.getStatusCode();
                byte[] responseBytes = Objects.requireNonNullElse(response.getBody(), new byte[0]);
                Charset charset = StandardCharsets.UTF_8;
                MediaType contentType = response.getHeaders().getContentType();
                if (contentType != null && contentType.getCharset() != null) {
                    charset = contentType.getCharset();
                }
                String responseText = new String(responseBytes, charset);
                Map<String, String> headerMap = new LinkedHashMap<>(response.getHeaders().toSingleValueMap());
                // 作用域已关闭或超时时 future 已取消或回调会被丢弃，complete 无副作用。
                future.complete(new FetchResponseData(
                        httpStatus.value(),
                        httpStatus.is2xxSuccessful(),
                        httpStatus.getReasonPhrase(),
                        headerMap,
                        contentType,
                        durationMs,
                        responseText,
                        responseBytes));
            } catch (Throwable e) {
                future.completeExceptionally(new FetchExecutionException(e, System.currentTimeMillis() - startTime));
            }
        });
        // 作用域关闭会取消 future，同时中断仍在执行的 HTTP 请求。
        future.whenComplete((result, err) -> {
            if (future.isCancelled() || err instanceof TimeoutException) {
                httpTask.cancel(true);
            }
        });
        return toPromise(context, executionScope, future, (resolve, reject, result, err) -> {
            if (err == null) {
                diagnoseRecorder.externalFetch(fetchMethod, url, result.getStatusCode(),
                        Objects.toString(result.getContentType(), null), result.getDurationMs(), null);
                resolve.executeVoid(buildResponseProxy(context, url, result));
            } else {
                Throwable cause = unwrapError(err);
                String message = formatFetchError(fetchMethod, url, fetchTimeout, cause);
                diagnoseRecorder.externalFetch(fetchMethod, url, null, null, extractDurationMs(err),
                        cause instanceof TimeoutException ? new ScriptException(message) : cause);
                reject.executeVoid(message);
            }
        });
    }

    /**
     * 将宿主 future 桥接为 JS Promise，结果回调统一投递到脚本线程执行。
     */
    private <T> Value toPromise(Context context, FetchExecutionScope scope, CompletableFuture<T> future,
                                PromiseSettler<T> settler) {
        scope.registerTask(future);
        return context.eval("js", "f => new Promise(f)").execute((ProxyExecutable) callbacks -> {
            Value resolve = callbacks[0];
            Value reject = callbacks[1];
            future.whenComplete((result, error) -> {
                scope.unregisterTask(future);
                scope.enqueueCallback(() -> settler.settle(resolve, reject, result, error));
            });
            return null;
        });
    }

    private Value rejectedPromise(Context context, String message) {
        return context.eval("js", "message => Promise.reject(message)").execute(message);
    }

    CompletableFuture<FetchResponseData> createFetchFuture() {
        return new CompletableFuture<>();
    }

    private ProxyObject buildResponseProxy(Context context, String url, FetchResponseData result) {
        Map<String, String> headerMap = result.getHeaderMap();
        Map<String, Object> headerObj = new LinkedHashMap<>(headerMap);
        headerObj.put("get", (ProxyExecutable) headerArgs -> getHeaderIgnoreCase(headerMap, headerArgs));
        headerObj.put("has", (ProxyExecutable) headerArgs -> getHeaderIgnoreCase(headerMap, headerArgs) != null);
        String responseText = result.getResponseText();
        byte[] responseBytes = result.getResponseBytes();
        return ProxyObject.fromMap(Map.of(
                "status", result.getStatusCode(),
                "ok", result.isOk(),
                "statusText", result.getStatusText(),
                "headers", ProxyObject.fromMap(headerObj),
                "url", url,
                "text", (ProxyExecutable) ignored -> Value.asValue(responseText),
                "json", (ProxyExecutable) ignored -> context.eval("js", "JSON.parse").execute(responseText),
                "arrayBuffer", (ProxyExecutable) ignored -> context.eval("js", "Uint8Array").newInstance(responseBytes).getMember("buffer"),
                "blob", (ProxyExecutable) ignored -> context.eval("js", "Uint8Array").newInstance(responseBytes)
        ));
    }

    private static Throwable unwrapError(Throwable error) {
        while ((error instanceof CompletionException || error instanceof FetchExecutionException)
                && error.getCause() != null) {
            error = error.getCause();
        }
        return error;
    }

    private static Long extractDurationMs(Throwable error) {
        while (error instanceof CompletionException && error.getCause() != null) {
            error = error.getCause();
        }
        if (error instanceof FetchExecutionException) {
            return ((FetchExecutionException) error).getDurationMs();
        }
        return null;
    }

    private static ScriptException toScriptException(PolyglotException polyglotException) {
        return (ScriptException) invokeMethod(GraalJSScriptEngine.class,
                getMethod(GraalJSScriptEngine.class, "toScriptException", PolyglotException.class),
                polyglotException);
    }

    private static String formatFetchError(String method, String url, Long timeout, Throwable error) {
        if (error instanceof TimeoutException && timeout != null) {
            return "Fetch request timeout after " + timeout + " ms: " + method + " " + url;
        }
        String message = error == null ? null : StringUtils.defaultIfBlank(error.getMessage(), error.toString());
        return "Fetch request failed: " + method + " " + url
                + (StringUtils.isBlank(message) ? StringUtils.EMPTY : " - " + message);
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

    @FunctionalInterface
    private interface FetchCallbackTask {
        void run() throws ScriptException;
    }

    @FunctionalInterface
    private interface PromiseSettler<T> {
        void settle(Value resolve, Value reject, T result, Throwable error) throws ScriptException;
    }

    @Getter
    @AllArgsConstructor
    static final class FetchResponseData {
        private final int statusCode;
        private final boolean ok;
        private final String statusText;
        private final Map<String, String> headerMap;
        private final MediaType contentType;
        private final long durationMs;
        private final String responseText;
        private final byte[] responseBytes;
    }

    @Getter
    private static final class FetchExecutionException extends RuntimeException {
        private final long durationMs;

        private FetchExecutionException(Throwable cause, long durationMs) {
            super(cause);
            this.durationMs = durationMs;
        }
    }

    private static final class FetchExecutionScope implements SimpleMockUtils.ScriptAsyncTaskPoller {
        private final Context polyglotContext;
        private final long timeoutSeconds;
        private final long scopeDeadlineNanos;
        private final BlockingQueue<FetchCallbackTask> callbackQueue = new LinkedBlockingQueue<>();
        private final Set<Future<?>> pendingTasks = ConcurrentHashMap.newKeySet();
        private volatile boolean closed;
        private final ScheduledFuture<?> watchdog;

        private FetchExecutionScope(Context context, long timeoutSeconds) {
            this.polyglotContext = context;
            this.timeoutSeconds = timeoutSeconds;
            long timeoutNanos = TimeUnit.SECONDS.toNanos(timeoutSeconds);
            scopeDeadlineNanos = System.nanoTime() + timeoutNanos;
            // JS 可以捕获嵌套 body 的异常并继续执行，直到作用域退出都需要保留中断能力。
            watchdog = TIMEOUT_SCHEDULER.scheduleWithFixedDelay(this::interruptExpiredScript,
                    timeoutNanos, TimeUnit.MILLISECONDS.toNanos(10), TimeUnit.NANOSECONDS);
        }

        private boolean isTimedOut() {
            return scopeDeadlineNanos - System.nanoTime() <= 0;
        }

        private boolean isActive() {
            return !closed && !isTimedOut();
        }

        private void checkDeadline() throws ScriptException {
            if (isTimedOut()) {
                throw createTimeoutException(null);
            }
        }

        private void registerTask(Future<?> task) {
            pendingTasks.add(task);
        }

        private void unregisterTask(Future<?> task) {
            pendingTasks.remove(task);
        }

        private void enqueueCallback(FetchCallbackTask task) {
            if (isActive()) {
                callbackQueue.offer(task);
            }
        }

        @Override
        public void pollAndRun(long timeoutNanos) throws ScriptException, InterruptedException {
            checkDeadline();
            long remaining = scopeDeadlineNanos - System.nanoTime();
            FetchCallbackTask task = callbackQueue.poll(Math.min(timeoutNanos, remaining), TimeUnit.NANOSECONDS);
            checkDeadline();
            if (task != null) {
                task.run();
            }
        }

        @Override
        public void drainReady() throws ScriptException {
            FetchCallbackTask task;
            while ((task = callbackQueue.poll()) != null) {
                checkDeadline();
                task.run();
            }
        }

        // 仅定时线程中断 Context；与 close 互斥，避免旧定时任务中断下一次借用。
        private synchronized void interruptExpiredScript() {
            if (!closed) {
                try {
                    polyglotContext.interrupt(Duration.ofMillis(50));
                } catch (TimeoutException ignored) {
                    // 本轮未完成，下次调度继续，释放定时线程给其他请求。
                }
            }
        }

        private ScriptException createTimeoutException(Throwable cause) {
            TimeoutException error = new TimeoutException("Script execution timed out after " + timeoutSeconds + " seconds");
            error.initCause(cause);
            return new ScriptException(error);
        }

        private synchronized void close() {
            closed = true;
            watchdog.cancel(false);
            pendingTasks.forEach(task -> task.cancel(true));
            callbackQueue.clear();
        }
    }
}
