package com.fugary.simple.mock.config;

import com.fugary.simple.mock.security.MockScriptInterceptor;
import com.fugary.simple.mock.security.UserSecurityInterceptor;
import com.fugary.simple.mock.utils.http.SimpleHttpClientUtils;
import com.fugary.simple.mock.web.filters.MockMetaDataFilter;
import com.fugary.simple.mock.web.filters.locale.CustomHeaderLocaleContextResolver;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.fugary.simple.mock.contants.MockConstants.*;

/**
 * Created on 2020/5/5 20:44 .<br>
 *
 * @author gary.fu
 */
@Configuration
@EnableConfigurationProperties({SimpleMockConfigProperties.class})
public class ApplicationConfig implements WebMvcConfigurer {

    @Bean
    public CommonsMultipartResolver multipartResolver() {
        CommonsMultipartResolver multipartResolver = new CommonsMultipartResolver();
        multipartResolver.setMaxUploadSize(5 * 1024 * 1024); // 5MB
        multipartResolver.setDefaultEncoding(StandardCharsets.UTF_8.name());
        return multipartResolver;
    }

    @Bean
    public LocaleResolver localeResolver(){
        CustomHeaderLocaleContextResolver headerLocaleResolver = new CustomHeaderLocaleContextResolver();
        headerLocaleResolver.setDefaultLocale(Locale.CHINA);
        headerLocaleResolver.setSupportedLocales(Arrays.asList(Locale.CHINA, Locale.US));
        return headerLocaleResolver;
    }

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
        config.setAllowedMethods(Stream.of(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.POST,
                        HttpMethod.OPTIONS, HttpMethod.PATCH, HttpMethod.PUT)
                .map(Enum::name).collect(Collectors.toList()));
        config.setAllowCredentials(true);
        config.addAllowedOriginPattern("*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setExposedHeaders(Collections.singletonList("Content-Disposition"));
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

    @Bean
    public RestTemplate restTemplate() {
        ClientHttpRequestFactory clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory(
                SimpleHttpClientUtils.getHttpsClient());
        RestTemplate restTemplate = new RestTemplate(clientHttpRequestFactory);
        restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        return restTemplate;
    }

    @Bean
    public UserSecurityInterceptor userSecurityInterceptor(){
        return new UserSecurityInterceptor();
    }

    @Bean
    public MockScriptInterceptor mockScriptInterceptor() {
        return new MockScriptInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(userSecurityInterceptor()).addPathPatterns("/admin/**");
        registry.addInterceptor(mockScriptInterceptor()).addPathPatterns("/mock/**");
    }
}
