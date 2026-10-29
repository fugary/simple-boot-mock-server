package com.fugary.simple.mock.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

@Configuration(proxyBeanMethods = false)
public class MockCacheConfig {

    @Bean
    public CaffeineCacheManager cacheManager(SimpleMockConfigProperties properties) {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        // 只允许使用已配置的名称，避免拼写错误创建无容量限制的缓存。
        manager.setCacheNames(Collections.emptyList());
        manager.setAllowNullValues(false);
        properties.getCache().getSpecs().forEach((name, spec) -> {
            try {
                manager.registerCustomCache(name, Caffeine.from(spec).build());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid cache spec for " + name + ": " + spec, e);
            }
        });
        return manager;
    }
}
