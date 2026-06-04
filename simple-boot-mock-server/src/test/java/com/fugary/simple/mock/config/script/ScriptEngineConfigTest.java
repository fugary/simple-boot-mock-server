package com.fugary.simple.mock.config.script;

import com.fugary.simple.mock.config.SimpleMockConfigProperties;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.scheduling.concurrent.ThreadPoolExecutorFactoryBean;
import org.springframework.test.util.ReflectionTestUtils;

import javax.script.ScriptEngine;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

import static org.assertj.core.api.Assertions.assertThat;

class ScriptEngineConfigTest {

    private final List<GenericObjectPool<ScriptEngine>> scriptEnginePools = new ArrayList<>();

    @AfterEach
    void closeScriptEnginePools() {
        scriptEnginePools.forEach(GenericObjectPool::close);
    }

    @Test
    void shouldBindShortPoolProperties() {
        SimpleMockConfigProperties properties = Binder.get(new MockEnvironment()
                        .withProperty("simple.mock.pool.script-engine-max-size", "7")
                        .withProperty("simple.mock.pool.event-stream-max-size", "8")
                        .withProperty("simple.mock.pool.fetch-script-max-size", "9")
                        .withProperty("simple.mock.pool.async-query-max-size", "11"))
                .bind("simple.mock", Bindable.of(SimpleMockConfigProperties.class))
                .orElseThrow(IllegalStateException::new);

        assertThat(properties.getPool().getScriptEngineMaxSize()).isEqualTo(7);
        assertThat(properties.getPool().getEventStreamMaxSize()).isEqualTo(8);
        assertThat(properties.getPool().getFetchScriptMaxSize()).isEqualTo(9);
        assertThat(properties.getPool().getAsyncQueryMaxSize()).isEqualTo(11);
    }

    @Test
    void shouldUseDefaultPoolPropertiesWhenOldConfigHasNoPoolSection() {
        SimpleMockConfigProperties properties = Binder.get(new MockEnvironment()
                        .withProperty("simple.mock.jwt-expire", "7"))
                .bind("simple.mock", Bindable.of(SimpleMockConfigProperties.class))
                .orElseThrow(IllegalStateException::new);

        assertThat(properties.getPool().getScriptEngineMaxSize())
                .isEqualTo(SimpleMockConfigProperties.DEFAULT_SCRIPT_ENGINE_MAX_SIZE);
        assertThat(properties.getPool().getEventStreamMaxSize())
                .isEqualTo(SimpleMockConfigProperties.DEFAULT_THREAD_POOL_MAX_SIZE);
        assertThat(properties.getPool().getFetchScriptMaxSize())
                .isEqualTo(SimpleMockConfigProperties.DEFAULT_THREAD_POOL_MAX_SIZE);
        assertThat(properties.getPool().getAsyncQueryMaxSize())
                .isEqualTo(SimpleMockConfigProperties.DEFAULT_THREAD_POOL_MAX_SIZE);
    }

    @Test
    void shouldUseConfiguredPoolMaxSizes() throws Exception {
        SimpleMockConfigProperties properties = new SimpleMockConfigProperties();
        properties.getPool().setScriptEngineMaxSize(3);
        properties.getPool().setEventStreamMaxSize(4);
        properties.getPool().setFetchScriptMaxSize(5);
        properties.getPool().setAsyncQueryMaxSize(6);

        ScriptEngineConfig config = createConfig(properties);

        GenericObjectPool<ScriptEngine> scriptEnginePool = createScriptEnginePool(config);
        assertThat(scriptEnginePool.getMaxTotal()).isEqualTo(3);
        assertThat(scriptEnginePool.getMinIdle()).isEqualTo(3);
        assertThreadPool(config.eventStreamThreadPool(), 4, 4);
        assertThreadPool(config.fetchScriptThreadPool(), 5, 5);
        assertThreadPool(config.asyncQueryThreadPool(), 6, 6);
    }

    @Test
    void shouldFallbackToDefaultsForInvalidPoolMaxSizes() throws Exception {
        SimpleMockConfigProperties properties = new SimpleMockConfigProperties();
        properties.getPool().setScriptEngineMaxSize(0);
        properties.getPool().setEventStreamMaxSize(-1);

        ScriptEngineConfig config = createConfig(properties);

        GenericObjectPool<ScriptEngine> scriptEnginePool = createScriptEnginePool(config);
        assertThat(scriptEnginePool.getMaxTotal())
                .isEqualTo(SimpleMockConfigProperties.DEFAULT_SCRIPT_ENGINE_MAX_SIZE);
        assertThat(scriptEnginePool.getMinIdle()).isEqualTo(5);
        assertThreadPool(config.eventStreamThreadPool(), SimpleMockConfigProperties.DEFAULT_THREAD_POOL_MAX_SIZE, 10);
    }

    @Test
    void shouldFallbackToDefaultsWhenPoolPropertiesIsNull() throws Exception {
        SimpleMockConfigProperties properties = new SimpleMockConfigProperties();
        properties.setPool(null);

        ScriptEngineConfig config = createConfig(properties);

        GenericObjectPool<ScriptEngine> scriptEnginePool = createScriptEnginePool(config);
        assertThat(scriptEnginePool.getMaxTotal())
                .isEqualTo(SimpleMockConfigProperties.DEFAULT_SCRIPT_ENGINE_MAX_SIZE);
        assertThreadPool(config.fetchScriptThreadPool(), SimpleMockConfigProperties.DEFAULT_THREAD_POOL_MAX_SIZE, 10);
    }

    private ScriptEngineConfig createConfig(SimpleMockConfigProperties properties) {
        ScriptEngineConfig config = new ScriptEngineConfig();
        ReflectionTestUtils.setField(config, "simpleMockConfigProperties", properties);
        return config;
    }

    private GenericObjectPool<ScriptEngine> createScriptEnginePool(ScriptEngineConfig config) {
        GenericObjectPool<ScriptEngine> scriptEnginePool = config.scriptEnginePool();
        scriptEnginePools.add(scriptEnginePool);
        return scriptEnginePool;
    }

    private void assertThreadPool(ThreadPoolExecutorFactoryBean factoryBean, int maxPoolSize, int corePoolSize)
            throws Exception {
        factoryBean.afterPropertiesSet();
        ExecutorService executorService = factoryBean.getObject();
        try {
            assertThat(executorService).isInstanceOf(ThreadPoolExecutor.class);
            ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) executorService;
            assertThat(threadPoolExecutor.getMaximumPoolSize()).isEqualTo(maxPoolSize);
            assertThat(threadPoolExecutor.getCorePoolSize()).isEqualTo(corePoolSize);
        } finally {
            factoryBean.destroy();
        }
    }
}
