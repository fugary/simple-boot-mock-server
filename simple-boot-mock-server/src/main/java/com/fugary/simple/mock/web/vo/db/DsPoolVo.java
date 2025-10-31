package com.fugary.simple.mock.web.vo.db;

import lombok.Data;

import java.io.Serializable;

@Data
public class DsPoolVo implements Serializable {

    private int maxPoolSize;

    private int totalCount;

    private int activeCount;

    private int idleCount;

    private int waitingCount;

    private String userName;

    private String url;

    private String driver;

    public DsPoolVo(String username, String jdbcUrl, String driverClassName) {
        this.userName = username;
        this.url = jdbcUrl;
        this.driver = driverClassName;
    }
}
