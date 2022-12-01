package com.example.gulimall.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 *  网关的限流需要使用1.7.1版本的控制台jar包（gateway中有API管理），旧版不能使用
 *  网关设置限流的响应，使用的类和方法：GatewayCallbackManager.setBlockHandler(new BlockRequestHandler(){})
 */
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
@EnableDiscoveryClient
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

}
