package com.fugary.simple.mock.cache;

import com.fugary.simple.mock.entity.mock.MockLog;
import com.fugary.simple.mock.web.vo.result.MockPreviewMetaVo;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Caches preview diagnose meta before the async log event is persisted.
 *
 * @author gary.fu
 */
@Component
public class MockPreviewMetaCache {

    private static final int MAX_CACHE_SIZE = 10000;

    private static final Duration EXPIRE_AFTER_WRITE = Duration.ofMinutes(10);

    private final Cache<String, MockPreviewMetaVo> cache = Caffeine.newBuilder()
            .maximumSize(MAX_CACHE_SIZE)
            .expireAfterWrite(EXPIRE_AFTER_WRITE)
            .build();

    public MockPreviewMetaVo get(String diagnoseId) {
        return StringUtils.isBlank(diagnoseId) ? null : cache.getIfPresent(diagnoseId);
    }

    public MockPreviewMetaVo put(MockLog mockLog) {
        MockPreviewMetaVo meta = MockPreviewMetaVo.of(mockLog);
        if (meta != null) {
            cache.put(meta.getDiagnoseId(), meta);
        }
        return meta;
    }
}
