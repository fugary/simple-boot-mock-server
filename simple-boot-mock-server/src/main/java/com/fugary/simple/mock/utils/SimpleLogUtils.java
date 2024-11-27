package com.fugary.simple.mock.utils;

import com.fugary.simple.mock.entity.mock.MockLog;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * Create date 2024/11/27<br>
 *
 * @author gary.fu
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SimpleLogUtils {

    private static final ThreadLocal<MockLog.MockLogBuilder> LOG_BUILDER_HOLDER = new ThreadLocal<>();

    public static void setLogBuilder(MockLog.MockLogBuilder logBuilder) {
        LOG_BUILDER_HOLDER.set(logBuilder);
    }

    public static MockLog.MockLogBuilder getLogBuilder() {
        return LOG_BUILDER_HOLDER.get();
    }

    public static void clearLogBuilder() {
        LOG_BUILDER_HOLDER.remove();
    }

    /**
     * 添加日志数据
     *
     * @param consumer
     */
    public static void addLogData(Consumer<MockLog.MockLogBuilder> consumer) {
        MockLog.MockLogBuilder logBuilder = getLogBuilder();
        if (logBuilder != null) {
            consumer.accept(logBuilder);
        }
    }

    /**
     * 添加responseBody数据
     *
     * @param responseBody
     */
    public static void addResponseData(String responseBody) {
        addLogData(logBuilder -> logBuilder.responseBody(responseBody));
    }

    /**
     * 添加responseBody数据
     *
     * @param responseEntity
     */
    public static void addResponseData(ResponseEntity<?> responseEntity) {
        if (responseEntity != null && responseEntity.getBody() != null) {
            if(responseEntity.getBody() instanceof byte[]){
                String responseBody = new String((byte[]) responseEntity.getBody(), StandardCharsets.UTF_8);
                addResponseData(responseBody);
            }
        }
    }
}
