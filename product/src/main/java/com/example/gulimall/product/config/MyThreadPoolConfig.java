package com.example.gulimall.product.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;

//@EnableConfigurationProperties(ThreadPoolConfigProperties.class) //如果没有将类加入容器，需要使用此注解
@Configuration
public class MyThreadPoolConfig {
    /**
     * @param properties 配置类已经加注解注入容器，可以直接使用
     * @return threadPoolExecutor 自定义线程池
     */
    @Bean
    public ThreadPoolExecutor threadPoolExecutor(ThreadPoolConfigProperties properties) {
        return new ThreadPoolExecutor(
                properties.getCoreSize(),
                properties.getMaxSize(),
                properties.getKeepAliveTime(),
                TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(50000),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy()
        );
    }
}
