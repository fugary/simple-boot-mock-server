package com.fugary.simple.mock.cache;

import com.fugary.simple.mock.entity.mock.MockLog;
import com.fugary.simple.mock.web.vo.result.MockPreviewMetaVo;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Caches preview diagnose meta before the async log event is persisted.
 *
 * @author gary.fu
 */
@Component
public class MockPreviewMetaCache {

    private final Cache cache;

    public MockPreviewMetaCache(CacheManager cacheManager) {
        this.cache = Objects.requireNonNull(cacheManager.getCache(MockCacheNames.PREVIEW_META));
    }

    public MockPreviewMetaVo get(String diagnoseId) {
        return StringUtils.isBlank(diagnoseId) ? null : cache.get(diagnoseId, MockPreviewMetaVo.class);
    }

    public MockPreviewMetaVo put(MockLog mockLog) {
        MockPreviewMetaVo meta = MockPreviewMetaVo.of(mockLog);
        if (meta != null) {
            cache.put(meta.getDiagnoseId(), meta);
        }
        return meta;
    }
}
