package com.fugary.simple.mock.web.controllers.admin;

import com.fugary.simple.mock.utils.SimpleResultUtils;
import com.fugary.simple.mock.web.vo.SimpleResult;
import com.fugary.simple.mock.web.vo.db.DsPoolVo;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.HikariPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

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

    @GetMapping
    public SimpleResult<List<DsPoolVo>> list() {
        List<DsPoolVo> results = new ArrayList<>();
        for (DataSource dataSource : dataSourceList) {
            if (dataSource instanceof HikariDataSource) {
                HikariDataSource ds = (HikariDataSource) dataSource;
                DsPoolVo dsPool = new DsPoolVo(ds.getUsername(), ds.getJdbcUrl(), ds.getDriverClassName());
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
        return SimpleResultUtils.createSimpleResult(results);
    }
}
