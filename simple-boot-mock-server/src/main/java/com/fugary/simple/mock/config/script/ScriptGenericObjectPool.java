package com.fugary.simple.mock.config.script;

import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.AbandonedConfig;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.InitializingBean;

/**
 * Create date 2024/7/8<br>
 *
 * @author gary.fu
 */
public class ScriptGenericObjectPool<T> extends GenericObjectPool<T> implements InitializingBean {

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
            super.addObjects(getMinIdle()); // 提前创建对象
        }
    }
}
