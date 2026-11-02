package com.fugary.simple.mock.service.ai.provider;

import com.fugary.simple.mock.service.ai.AiException;
import com.fugary.simple.mock.utils.JsonUtils;
import com.fugary.simple.mock.utils.SimpleResultUtils;
import com.fugary.simple.mock.utils.servlet.HttpRequestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import javax.servlet.http.HttpServletResponse;

/** AI 流式请求的线程上下文、心跳和连接清理。 */
@Component
public class AiSseSupport {
    @Autowired
    @Qualifier("taskScheduler")
    private ThreadPoolTaskScheduler scheduler;

    @Autowired
    @Qualifier("aiStreamExecutor")
    private TaskExecutor executor;

    public SseEmitter start(BiConsumer<SseEmitter, AiStreamSession> action) {
        HttpServletResponse response = HttpRequestUtils.getCurrentResponse();
        if (response != null) {
            response.setHeader("Cache-Control", "no-cache, no-transform");
            response.setHeader("X-Accel-Buffering", "no");
        }
        SseEmitter emitter = new SseEmitter(180_000L);
        AtomicBoolean closed = new AtomicBoolean();
        AtomicReference<ScheduledFuture<?>> heartbeatRef = new AtomicReference<>();
        AiStreamSession stream = new AiStreamSession(text -> send(emitter, "delta", Map.of("text", text)));
        Runnable cleanup = () -> {
            closed.set(true);
            stream.cancel();
            ScheduledFuture<?> task = heartbeatRef.get();
            if (task != null) task.cancel(false);
        };
        Runnable close = () -> {
            cleanup.run();
            emitter.complete();
        };
        emitter.onCompletion(cleanup);
        emitter.onTimeout(close);
        emitter.onError(error -> close.run());
        LocaleContext locale = LocaleContextHolder.getLocaleContext();
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        // 首字生成期间也发送心跳，以识别浏览器断开，并避免代理空闲超时。
        try {
            ScheduledFuture<?> heartbeat = scheduler.scheduleAtFixedRate(() -> {
                if (closed.get()) return;
                try {
                    emitter.send(SseEmitter.event().comment("keep-alive"));
                } catch (Exception e) {
                    cleanup.run();
                    emitter.completeWithError(e);
                }
            }, 5000);
            heartbeatRef.set(heartbeat);
            if (closed.get()) heartbeat.cancel(false);
            executor.execute(() -> {
                try {
                    if (closed.get()) return;
                    if (attributes != null) RequestContextHolder.setRequestAttributes(attributes);
                    LocaleContextHolder.setLocaleContext(locale);
                    action.accept(emitter, stream);
                } catch (Exception e) {
                    if (!closed.get() && !stream.isCancelled()) {
                        sendError(emitter, e);
                    }
                } finally {
                    RequestContextHolder.resetRequestAttributes();
                    LocaleContextHolder.resetLocaleContext();
                    close.run();
                }
            });
        } catch (RuntimeException e) {
            sendError(emitter, e);
            close.run();
        }
        return emitter;
    }

    private void sendError(SseEmitter emitter, Exception error) {
        String message = error instanceof AiException
            ? SimpleResultUtils.getErrorMsg(((AiException) error).getMessageKey(), ((AiException) error).getArguments())
            : SimpleResultUtils.getErrorMsg("simple.ai.requestFailed", null);
        try {
            send(emitter, "error", Map.of("message", message));
        } catch (CancellationException ignored) {
            // 客户端已断开，仍由调用方完成清理。
        }
    }

    public static void send(SseEmitter emitter, String event, Object data) {
        try {
            emitter.send(SseEmitter.event().name(event).data(JsonUtils.toJson(data)));
        } catch (Exception e) {
            throw new CancellationException("AI 连接已断开");
        }
    }
}
