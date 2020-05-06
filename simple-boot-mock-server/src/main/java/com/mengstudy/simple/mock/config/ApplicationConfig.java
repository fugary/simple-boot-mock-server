package com.mengstudy.simple.mock.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created on 2020/5/5 20:44 .<br>
 *
 * @author gary.fu
 */
@Configuration
@EnableConfigurationProperties({SimpleMockConfigProperties.class})
public class ApplicationConfig {

    @Bean
    public CorsFilter corsFilter() {
        final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        final CorsConfiguration config = new CorsConfiguration();
        config.setAllowedMethods(Stream.of(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.POST, HttpMethod.OPTIONS)
                .map(Enum::name).collect(Collectors.toList()));
        config.setAllowCredentials(true);
        config.addAllowedOrigin("*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setMaxAge(7 * 24 * 60 * 60L); // 设置跨域check缓存时间
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
