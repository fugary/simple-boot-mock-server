package com.fugary.simple.mock.config.script;

import com.fugary.simple.mock.script.JavaScriptEngineFactory;
import com.fugary.simple.mock.script.JavaScriptEngineProviderImpl;
import com.fugary.simple.mock.script.ScriptEngineProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.script.ScriptEngine;

/**
 * Create date 2024/7/4<br>
 *
 * @author gary.fu
 */
@Configuration
@Slf4j
public class ScriptEngineConfig {

    @Bean
    public GenericObjectPool<ScriptEngine> scriptEnginePool() {
        GenericObjectPoolConfig<ScriptEngine> config = new GenericObjectPoolConfig<>();
        config.setMaxTotal(10); // 设置池中最大对象数
        config.setJmxEnabled(false);
        config.setMinIdle(2);
        return new GenericObjectPool<>(javaScriptEngineFactory(), config);
    }

    @Bean
    public JavaScriptEngineFactory javaScriptEngineFactory(){
        return new JavaScriptEngineFactory();
    }

    @Bean
    public ScriptEngineProvider scriptEngineProvider() {
        JavaScriptEngineProviderImpl javaScriptEngineProvider = new JavaScriptEngineProviderImpl();
        javaScriptEngineProvider.setScriptEnginePool(scriptEnginePool());
        return javaScriptEngineProvider;
    }
}
