package com.fugary.simple.mock.cache;

import org.graalvm.polyglot.proxy.ProxyObject;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** 只共享源码和版本，不在引擎之间共享 GraalJS 对象。 */
@Component
public class RequireSourceCache {
    private final Cache cache;
    private String generation = UUID.randomUUID().toString();

    public RequireSourceCache(CacheManager cacheManager) {
        cache = Objects.requireNonNull(cacheManager.getCache(MockCacheNames.REQUIRE_SOURCE));
    }

    public synchronized ProxyObject get(String url) {
        return cache.get(url, ProxyObject.class);
    }

    public synchronized String generation() {
        return generation;
    }

    public synchronized ProxyObject getOrPut(String url, String script, String expectedGeneration) {
        // 清理之前发起的请求可以完成，但不能重新填入已清理的缓存。
        if (!generation.equals(expectedGeneration)) {
            return null;
        }
        // 并发加载完成时复用先发布的源码和版本，不能覆盖仍然有效的模块。
        return cache.get(url, () -> ProxyObject.fromMap(Map.of(
                "script", script, "version", UUID.randomUUID().toString())));
    }

    public synchronized void invalidate(String url, String version) {
        ProxyObject source = get(url);
        if (source != null && version.equals(source.getMember("version"))) {
            cache.evict(url);
        }
    }

    public synchronized void clear() {
        generation = UUID.randomUUID().toString();
        cache.clear();
    }
}
