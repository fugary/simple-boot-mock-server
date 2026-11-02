package com.fugary.simple.mock.service.ai.provider;

import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/** 单次流式问答的取消信号；关闭页面时同时中断正在等待的上游请求。 */
public class AiStreamSession {
    private final Consumer<String> onText;
    private final StringBuilder content = new StringBuilder();
    private final AtomicBoolean cancelled = new AtomicBoolean();
    private final AtomicReference<Runnable> abort = new AtomicReference<>();

    public AiStreamSession(Consumer<String> onText) {
        this.onText = onText;
    }

    public void text(String text) {
        check();
        if (text != null && !text.isEmpty()) {
            synchronized (content) { content.append(text); }
            try {
                onText.accept(text);
            } catch (CancellationException e) {
                cancel();
                throw e;
            }
        }
    }

    public String getText() { synchronized (content) { return content.toString(); } }

    public boolean isCancelled() { return cancelled.get(); }

    public void check() {
        if (isCancelled()) throw new CancellationException("AI 请求已中断");
    }

    public void bind(Runnable action) {
        abort.set(action);
        if (isCancelled()) {
            cancel();
            check();
        }
    }

    public void unbind() { abort.set(null); }

    public void cancel() {
        cancelled.set(true);
        Runnable action = abort.getAndSet(null);
        if (action != null) action.run();
    }
}
