package com.fugary.simple.mock.utils;

import com.fugary.simple.mock.web.vo.result.MockDiagnoseVo;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MockDiagnoseContext {

    private static final ThreadLocal<MockDiagnoseVo> CURRENT = new ThreadLocal<>();

    public static void set(MockDiagnoseVo diagnose) {
        CURRENT.set(diagnose);
    }

    public static MockDiagnoseVo get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
