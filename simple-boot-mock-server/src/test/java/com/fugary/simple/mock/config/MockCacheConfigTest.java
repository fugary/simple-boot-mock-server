package com.fugary.simple.mock.config;

import com.fugary.simple.mock.cache.MockCacheNames;
import com.github.benmanes.caffeine.cache.Cache;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.cache.caffeine.CaffeineCacheManager;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class MockCacheConfigTest {
    @Test
    void shouldKeepSeparatePoliciesAndRejectUnknownNames() {
        CaffeineCacheManager manager = new MockCacheConfig().cacheManager(new SimpleMockConfigProperties());
        Cache<?, ?> preview = (Cache<?, ?>) manager.getCache(MockCacheNames.PREVIEW_META).getNativeCache();
        Cache<?, ?> roundRobin = (Cache<?, ?>) manager.getCache(MockCacheNames.ROUND_ROBIN).getNativeCache();
        assertEquals(10000, preview.policy().eviction().orElseThrow().getMaximum());
        assertEquals(10, preview.policy().expireAfterWrite().orElseThrow().getExpiresAfter(TimeUnit.MINUTES));
        assertEquals(30, roundRobin.policy().expireAfterAccess().orElseThrow().getExpiresAfter(TimeUnit.MINUTES));
        assertNull(manager.getCache("typo"));
        manager.getCache(MockCacheNames.PREVIEW_META).put("same-key", "preview");
        assertNull(manager.getCache(MockCacheNames.ROUND_ROBIN).get("same-key"));
    }

    @Test
    void shouldOverrideOneSpecAndRetainOtherDefaults() {
        SimpleMockConfigProperties properties = new SimpleMockConfigProperties();
        new Binder(new MapConfigurationPropertySource(Map.of(
                "simple.mock.cache.specs.round-robin", "maximumSize=20,expireAfterAccess=5m")))
                .bind("simple.mock", Bindable.ofInstance(properties));
        Cache<?, ?> cache = (Cache<?, ?>) new MockCacheConfig().cacheManager(properties)
                .getCache(MockCacheNames.ROUND_ROBIN).getNativeCache();
        assertEquals(20, cache.policy().eviction().orElseThrow().getMaximum());
        assertEquals(5, cache.policy().expireAfterAccess().orElseThrow().getExpiresAfter(TimeUnit.MINUTES));
        assertTrue(properties.getCache().getSpecs().containsKey(MockCacheNames.PREVIEW_META));
    }

    @Test
    void shouldFailFastForInvalidSpec() {
        SimpleMockConfigProperties properties = new SimpleMockConfigProperties();
        properties.getCache().getSpecs().put(MockCacheNames.ROUND_ROBIN, "expireAfterAccess=wrong");
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> new MockCacheConfig().cacheManager(properties));
        assertTrue(error.getMessage().contains(MockCacheNames.ROUND_ROBIN));
    }

    @Test
    void shouldCreateOneCounterUnderConcurrentAccess() {
        org.springframework.cache.Cache cache = new MockCacheConfig().cacheManager(new SimpleMockConfigProperties())
                .getCache(MockCacheNames.ROUND_ROBIN);
        AtomicInteger loads = new AtomicInteger();
        IntStream.range(0, 1000).parallel().forEach(i -> cache.get("key", () -> {
            loads.incrementAndGet();
            return new AtomicInteger();
        }).incrementAndGet());
        assertEquals(1, loads.get());
        assertEquals(1000, cache.get("key", AtomicInteger.class).get());
    }
}
