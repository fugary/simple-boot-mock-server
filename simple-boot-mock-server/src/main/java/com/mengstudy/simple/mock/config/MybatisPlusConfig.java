package com.mengstudy.simple.mock.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Created on 2020/5/4 14:39 .<br>
 *
 * @author gary.fu
 */
@EnableTransactionManagement
@Configuration
@MapperScan("com.mengstudy.simple.mock.mapper")
public class MybatisPlusConfig {

    @Bean
    public PaginationInnerInterceptor paginationInterceptor() {
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor();
        paginationInterceptor.setOverflow(true);  // 设置请求的页面大于最大页后操作， true调回到首页，false 继续请求  默认false
        // paginationInterceptor.setLimit(500); // 设置最大单页限制数量，默认 500 条，-1 不受限制
        // 开启 count 的 join 优化,只针对部分 left join
        paginationInterceptor.setOptimizeJoin(true);
        return paginationInterceptor;
    }

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(paginationInterceptor());
        return interceptor;
    }
}
