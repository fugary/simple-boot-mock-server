package com.fugary.simple.mock.web.controllers.admin;

import com.fugary.simple.mock.utils.SimpleResultUtils;
import com.fugary.simple.mock.web.vo.SimpleResult;
import com.fugary.simple.mock.web.vo.db.DsPoolVo;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.HikariPool;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolExecutorFactoryBean;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Create date 2025/3/28<br>
 *
 * @author gary.fu
 */
@Slf4j
@RestController
@RequestMapping("/admin/dbs")
public class DataSourceController {

    @Autowired
    private List<DataSource> dataSourceList;

    @Autowired
    private List<GenericObjectPool<?>> genericObjectList;

    @Autowired
    private List<ExecutorService> executorServices;

    @GetMapping
    public SimpleResult<List<DsPoolVo>> list() {
        List<DsPoolVo> results = new ArrayList<>();
        for (DataSource dataSource : dataSourceList) {
            if (dataSource instanceof HikariDataSource) {
                HikariDataSource ds = (HikariDataSource) dataSource;
                Map<String, String> info = new LinkedHashMap<>();
                info.put("url", ds.getJdbcUrl());
                info.put("user", ds.getUsername());
                info.put("driver", ds.getDriverClassName());
                DsPoolVo dsPool = new DsPoolVo(dataSource.getClass().getSimpleName(), info);
                HikariPool pool = (HikariPool) (ds).getHikariPoolMXBean();
                dsPool.setMaxPoolSize(ds.getMaximumPoolSize());
                if (pool != null) {
                    dsPool.setTotalCount(pool.getTotalConnections());
                    dsPool.setActiveCount(pool.getActiveConnections());
                    dsPool.setIdleCount(pool.getIdleConnections());
                    dsPool.setWaitingCount(pool.getThreadsAwaitingConnection());
                }
                results.add(dsPool);
            }
        }
        for (GenericObjectPool<?> objectPool : genericObjectList) {
            Map<String, String> info = new LinkedHashMap<>();
            info.put("factory", objectPool.getFactoryType());
            DsPoolVo dsPool = new DsPoolVo(objectPool.getClass().getSimpleName(), info);
            dsPool.setMaxPoolSize(objectPool.getMaxTotal());
            dsPool.setTotalCount(objectPool.getNumActive() + objectPool.getNumIdle());
            dsPool.setActiveCount(objectPool.getNumActive());
            dsPool.setIdleCount(objectPool.getNumIdle());
            dsPool.setWaitingCount(objectPool.getNumWaiters());
            results.add(dsPool);
        }
        for (ExecutorService executorService : executorServices) {
            if (executorService instanceof ThreadPoolExecutor) {
                ThreadPoolExecutor threadPool = (ThreadPoolExecutor) executorService;
                String name = threadPool.getClass().getSimpleName();
                Map<String, String> info = new LinkedHashMap<>();
                if (threadPool.getThreadFactory() instanceof ThreadPoolExecutorFactoryBean) {
                    name = getBeanName((ThreadPoolExecutorFactoryBean) threadPool.getThreadFactory());
                }
                info.put("factory", threadPool.getThreadFactory().getClass().getName());
                DsPoolVo dsPool = new DsPoolVo(name, info);
                dsPool.setMaxPoolSize(threadPool.getMaximumPoolSize());
                dsPool.setTotalCount(threadPool.getPoolSize());
                dsPool.setActiveCount(threadPool.getActiveCount());
                dsPool.setIdleCount(threadPool.getPoolSize() - threadPool.getActiveCount());
                dsPool.setWaitingCount(threadPool.getQueue().size());
                results.add(dsPool);
            }
        }
        return SimpleResultUtils.createSimpleResult(results);
    }

    private String getBeanName(ThreadPoolExecutorFactoryBean threadPoolFactory) {
        return (String) ReflectionUtils.getField(FieldUtils.getField(ThreadPoolExecutorFactoryBean.class,
                "beanName", true), threadPoolFactory);
    }
}
