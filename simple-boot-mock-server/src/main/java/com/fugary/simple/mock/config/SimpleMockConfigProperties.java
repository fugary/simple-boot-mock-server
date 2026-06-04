package com.fugary.simple.mock.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Created on 2020/5/5 20:43 .<br>
 *
 * @author gary.fu
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "simple.mock")
public class SimpleMockConfigProperties {

    public static final int DEFAULT_SCRIPT_ENGINE_MAX_SIZE = 10;

    public static final int DEFAULT_THREAD_POOL_MAX_SIZE = 20;

    private String jwtPassword = "";

    private Integer jwtExpire = 7;

    /**
     * 上传大小
     */
    private long maxUploadSize = 10 * 1024 * 1024;

    private boolean mockLogEnabled = true;

    /**
     * Mock日志保留天数，设置小于等于0时不自动清理
     */
    private Integer mockLogRetentionDays = 180;

    private boolean fetchEnabled = true;

    private PoolProperties pool = new PoolProperties();

    @Getter
    @Setter
    public static class PoolProperties {

        private int scriptEngineMaxSize = DEFAULT_SCRIPT_ENGINE_MAX_SIZE;

        private int eventStreamMaxSize = DEFAULT_THREAD_POOL_MAX_SIZE;

        private int fetchScriptMaxSize = DEFAULT_THREAD_POOL_MAX_SIZE;

        private int asyncQueryMaxSize = DEFAULT_THREAD_POOL_MAX_SIZE;
    }
}
