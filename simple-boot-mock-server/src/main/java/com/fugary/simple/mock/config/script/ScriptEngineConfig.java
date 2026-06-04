package com.fugary.simple.mock.config.script;

import com.fugary.simple.mock.config.SimpleMockConfigProperties;
import com.fugary.simple.mock.push.DefaultScriptWithFetchProviderImpl;
import com.fugary.simple.mock.push.ScriptWithFetchProvider;
import com.fugary.simple.mock.script.JavaScriptEngineFactory;
import com.fugary.simple.mock.script.JavaScriptEngineProviderImpl;
import com.fugary.simple.mock.script.ScriptEngineProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolExecutorFactoryBean;

import javax.script.ScriptEngine;

/**
 * Create date 2024/7/4<br>
 *
 * @author gary.fu
 */
@Configuration
@Slf4j
public class ScriptEngineConfig {

    private static final int DEFAULT_SCRIPT_ENGINE_MIN_IDLE = 5;

    private static final int DEFAULT_THREAD_POOL_CORE_SIZE = 10;

    @Autowired
    private SimpleMockConfigProperties simpleMockConfigProperties;

    @Bean
    public ScriptWithFetchProvider scriptWithFetchProvider() {
        return new DefaultScriptWithFetchProviderImpl();
    }

    @Bean
    public GenericObjectPool<ScriptEngine> scriptEnginePool() {
        GenericObjectPoolConfig<ScriptEngine> config = new GenericObjectPoolConfig<>();
        int maxPoolSize = getPositivePoolSize(getPoolProperties().getScriptEngineMaxSize(),
                SimpleMockConfigProperties.DEFAULT_SCRIPT_ENGINE_MAX_SIZE);
        config.setMaxTotal(maxPoolSize);
        config.setJmxEnabled(false);
        config.setMinIdle(Math.min(DEFAULT_SCRIPT_ENGINE_MIN_IDLE, maxPoolSize));
        return new ScriptGenericObjectPool<>(javaScriptEngineFactory(), config);
    }

    @Bean
    public ThreadPoolExecutorFactoryBean eventStreamThreadPool() {
        return createThreadPool(getPoolProperties().getEventStreamMaxSize());
    }

    @Bean
    public ThreadPoolExecutorFactoryBean fetchScriptThreadPool() {
        return createThreadPool(getPoolProperties().getFetchScriptMaxSize());
    }

    @Bean
    public ThreadPoolExecutorFactoryBean asyncQueryThreadPool() {
        return createThreadPool(getPoolProperties().getAsyncQueryMaxSize());
    }

    private ThreadPoolExecutorFactoryBean createThreadPool(int configuredMaxPoolSize) {
        int maxPoolSize = getPositivePoolSize(configuredMaxPoolSize,
                SimpleMockConfigProperties.DEFAULT_THREAD_POOL_MAX_SIZE);
        ThreadPoolExecutorFactoryBean pool = new ThreadPoolExecutorFactoryBean();
        pool.setCorePoolSize(Math.min(DEFAULT_THREAD_POOL_CORE_SIZE, maxPoolSize));
        pool.setMaxPoolSize(maxPoolSize);
        return pool;
    }

    private int getPositivePoolSize(int configuredPoolSize, int defaultPoolSize) {
        return configuredPoolSize > 0 ? configuredPoolSize : defaultPoolSize;
    }

    private SimpleMockConfigProperties getConfigProperties() {
        return simpleMockConfigProperties == null ? new SimpleMockConfigProperties() : simpleMockConfigProperties;
    }

    private SimpleMockConfigProperties.PoolProperties getPoolProperties() {
        SimpleMockConfigProperties.PoolProperties poolProperties = getConfigProperties().getPool();
        return poolProperties == null ? new SimpleMockConfigProperties.PoolProperties() : poolProperties;
    }

    @Bean
    public JavaScriptEngineFactory javaScriptEngineFactory() {
        JavaScriptEngineFactory javaScriptEngineFactory = new JavaScriptEngineFactory();
        javaScriptEngineFactory.setScriptWithFetchProvider(scriptWithFetchProvider());
        return javaScriptEngineFactory;
    }

    @Bean
    public ScriptEngineProvider scriptEngineProvider() {
        JavaScriptEngineProviderImpl javaScriptEngineProvider = new JavaScriptEngineProviderImpl();
        javaScriptEngineProvider.setScriptEnginePool(scriptEnginePool());
        javaScriptEngineProvider.setScriptWithFetchProvider(scriptWithFetchProvider());
        javaScriptEngineProvider.setFetchEnabled(getConfigProperties().isFetchEnabled());
        return javaScriptEngineProvider;
    }
}
