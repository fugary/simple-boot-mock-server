package com.mengstudy.simple.mock.config;

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

    private String jwtPassword = "1234567890";

    private Integer jwtExpire = 7;
}
