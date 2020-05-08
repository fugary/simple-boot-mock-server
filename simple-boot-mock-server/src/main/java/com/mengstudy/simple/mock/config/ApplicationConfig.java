package com.mengstudy.simple.mock.config;

import com.mengstudy.simple.mock.web.filters.MockMetaDataFilter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.mengstudy.simple.mock.contants.MockConstants.*;

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
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration allConfig = getCorsConfiguration();
        CorsConfiguration mockConfig = getCorsConfiguration();
        mockConfig.setExposedHeaders(Arrays.asList(" * ")); // spring boot目前强制不能使用*，但是没有trim处理，因此这样配置算是一个漏洞
        source.registerCorsConfiguration(MOCK_PREFIX + ALL_PATH_PATTERN, mockConfig);
        source.registerCorsConfiguration(ALL_PATH_PATTERN, allConfig);
        return new CorsFilter(source);
    }

    protected CorsConfiguration getCorsConfiguration() {
        final CorsConfiguration config = new CorsConfiguration();
        config.setAllowedMethods(Stream.of(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.POST, HttpMethod.OPTIONS)
                .map(Enum::name).collect(Collectors.toList()));
        config.setAllowCredentials(true);
        config.addAllowedOrigin("*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setMaxAge(7 * 24 * 60 * 60L); // 设置跨域check缓存时间
        return config;
    }

    @Bean
    public FilterRegistrationBean mockMetaDataFilter() {
        FilterRegistrationBean registration = new FilterRegistrationBean();
        registration.setFilter(new MockMetaDataFilter());
        registration.addUrlPatterns(MOCK_PREFIX + FILTER_PATH_PATTERN);
        registration.setName("mockMetaDataFilter");
        registration.setOrder(2);
        return registration;
    }
}
