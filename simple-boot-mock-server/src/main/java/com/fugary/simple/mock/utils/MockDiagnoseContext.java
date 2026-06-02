package com.fugary.simple.mock.utils;

import com.fugary.simple.mock.web.vo.result.MockDiagnoseVo;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.function.Supplier;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MockDiagnoseContext {

    private static final ThreadLocal<MockDiagnoseVo> CURRENT = new ThreadLocal<>();
    private static final ThreadLocal<String> POST_PROCESSOR_STAGE_GROUP = new ThreadLocal<>();

    public static void set(MockDiagnoseVo diagnose) {
        if (diagnose == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(diagnose);
        }
    }

    public static MockDiagnoseVo get() {
        return CURRENT.get();
    }

    public static void setPostProcessorStageGroup(String stageGroup) {
        if (stageGroup == null || stageGroup.trim().isEmpty()) {
            POST_PROCESSOR_STAGE_GROUP.remove();
        } else {
            POST_PROCESSOR_STAGE_GROUP.set(stageGroup);
        }
    }

    public static String getPostProcessorStageGroup() {
        return POST_PROCESSOR_STAGE_GROUP.get();
    }

    public static <T> T callWithPostProcessorStageGroup(String stageGroup, Supplier<T> supplier) {
        String previousStageGroup = POST_PROCESSOR_STAGE_GROUP.get();
        try {
            setPostProcessorStageGroup(stageGroup);
            return supplier.get();
        } finally {
            setPostProcessorStageGroup(previousStageGroup);
        }
    }

    public static void runWith(MockDiagnoseVo diagnose, String stageGroup, Runnable runnable) {
        MockDiagnoseVo previous = CURRENT.get();
        String previousStageGroup = POST_PROCESSOR_STAGE_GROUP.get();
        try {
            set(diagnose);
            setPostProcessorStageGroup(stageGroup);
            runnable.run();
        } finally {
            set(previous);
            setPostProcessorStageGroup(previousStageGroup);
        }
    }

    public static void clear() {
        CURRENT.remove();
        POST_PROCESSOR_STAGE_GROUP.remove();
    }
}
