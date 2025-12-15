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

    private String jwtPassword = "";

    private Integer jwtExpire = 7;

    /**
     * 上传大小
     */
    private long maxUploadSize = 10 * 1024 * 1024;

    private boolean mockLogEnabled = true;

    private boolean fetchEnabled = true;
}
