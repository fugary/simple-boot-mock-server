package com.fugary.simple.mock.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Create date 2026/2/4<br>
 *
 * @author gary.fu
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public class AsyncUtils {

    /**
     * 从Future获取数据
     *
     * @param future
     * @param supplier
     * @param <T>
     * @return
     */
    public static <T> T get(Future<T> future, Supplier<T> supplier) {
        try {
            if (future != null) {
                return future.get();
            }
        } catch (Exception e) {
            log.error("获取数据错误", e);
        }
        return supplier.get();
    }

    /**
     * 从Future获取数据
     *
     * @param future
     * @param supplier
     * @param timeout
     * @param <T>
     * @return
     */
    public static <T> T get(Future<T> future, long timeout, Supplier<T> supplier) {
        try {
            if (future != null) {
                return future.get(timeout, TimeUnit.MILLISECONDS);
            }
        } catch (Exception e) {
            log.error("获取数据错误", e);
        }
        return supplier.get();
    }
}
