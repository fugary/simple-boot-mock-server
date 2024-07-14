package com.fugary.simple.mock.config.script;

import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.AbandonedConfig;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import java.util.stream.IntStream;

/**
 * Create date 2024/7/8<br>
 *
 * @author gary.fu
 */
public class ScriptGenericObjectPool<T> extends GenericObjectPool<T> implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(ScriptGenericObjectPool.class);

    public ScriptGenericObjectPool(PooledObjectFactory<T> factory) {
        super(factory);
    }

    public ScriptGenericObjectPool(PooledObjectFactory<T> factory, GenericObjectPoolConfig<T> config) {
        super(factory, config);
    }

    public ScriptGenericObjectPool(PooledObjectFactory<T> factory, GenericObjectPoolConfig<T> config, AbandonedConfig abandonedConfig) {
        super(factory, config, abandonedConfig);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (getMinIdle() > 0) {
//                super.addObjects(getMinIdle()); // 提前创建对象
            IntStream.range(0, getMinIdle())
                    .parallel().forEach(i -> {
                        try {
                            super.addObject();
                        } catch (Exception e) {
                            log.error("创建脚本对象失败", e);
                        }
                    });
        }
    }
}
