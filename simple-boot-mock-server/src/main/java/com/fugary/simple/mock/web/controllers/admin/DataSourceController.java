package com.fugary.simple.mock.web.controllers.admin;

import com.fugary.simple.mock.utils.SimpleResultUtils;
import com.fugary.simple.mock.web.vo.SimpleResult;
import com.fugary.simple.mock.web.vo.db.DsPoolVo;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.HikariPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Create date 2025/3/28<br>
 *
 * @author gary.fu
 */
@RestController
@RequestMapping("/admin/dbs")
public class DataSourceController {

    @Autowired
    private List<DataSource> dataSourceList;

    @Autowired
    private List<GenericObjectPool<?>> genericObjectList;

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
        return SimpleResultUtils.createSimpleResult(results);
    }
}
