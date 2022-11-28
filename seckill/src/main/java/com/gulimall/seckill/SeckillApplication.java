package com.gulimall.seckill;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 *  1、整合Sentinel
 *      1）整个项目都需要流量监控，common中导入spring-cloud-starter-alibaba-sentinel依赖
 *      2）下载sentinel控制台【版本号要和依赖的核心包一致】，启动控制台
 *      3）common中导入spring-boot-starter-actuator依赖
 *      4）每个项目的配置文件中
 *          1.配置sentinel的地址信息spring.cloud.sentinel.transport.dashboard=？？？
 *          2.连接端口spring.cloud.sentinel.transport.port=8719，默认值可以不配置
 *          3.暴露项目的端口management.endpoints.web.exposure.include=*
 *      5）自定义流控的响应
 *          在配置类的bean中设置WebCallbackManager.setUrlBlockHandler(new UrlBlockHandler(){实现接口})
 */
@EnableDiscoveryClient
@EnableFeignClients
@EnableRedisHttpSession
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class SeckillApplication {

    public static void main(String[] args) {
        SpringApplication.run(SeckillApplication.class, args);
    }

}