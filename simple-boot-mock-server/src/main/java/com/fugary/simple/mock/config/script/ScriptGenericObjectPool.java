package com.fugary.simple.mock.config.script;

import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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
    public T borrowObject() throws Exception {
        log.info("borrow script engine...");
        return super.borrowObject();
    }

    @Override
    public void returnObject(T obj) {
        log.info("return script engine...");
        super.returnObject(obj);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (getMinIdle() > 0) {
            // 提前创建对象
            preparePool();
        }
    }
}
