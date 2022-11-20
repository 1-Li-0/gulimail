package com.example.gulimall.member.config;

import com.baomidou.mybatisplus.extension.plugins.PaginationInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@MapperScan(basePackages = "com.example.gulimall.member.dao")
public class MybatisConfig {
    //引入mybatis的分页插件
    @Bean
    public PaginationInterceptor paginationInterceptor() {
        PaginationInterceptor paginationInterceptor = new PaginationInterceptor();
        //设置请求的页数大于最大页时，true调回首页，默认false继续请求返回空
        paginationInterceptor.setOverflow(true);
        //设置最大单页限制条数，默认500，-1表示不受限制
        paginationInterceptor.setLimit(1000);
        return paginationInterceptor;
    }
}
