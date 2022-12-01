package com.gulimall.seckill;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
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
 *
 *      6）使用sentinel保护feign远程调用服务：熔断/降级机制。
 *      熔断：
 *          1.开启对feign的支持配置feign.sentinel.enabled=true
 *          2.写一个实现类实现feign接口，放入容器
 *          3.feign接口注解上添加回调属性 fallback=xxx.class(默认实现类)
 *      降级：
 *          4.还可以手动指定远程调用的降级策略（会触发熔断方法）：
 *            1s内的5个请求都超过了设定的阈值（单位ms），指定时间（单位s）内重复调用都是直接使用熔断方法。
 *          5.超大浏览时，必须牺牲一些远程服务。远程服务提供方可以指定降级策略；
 *            远程调用虽然还在运行，但是提供方没有执行业务逻辑，返回的是降级数据（限流或者熔断的方法返回值）。
 *      7）自定义受保护的资源
 *         1.代码的方式
 *          try(Entry entry = SphU.entry(自定义资源名)){
 *              受保护的资源
 *          }catch(BlockException e){
 *              限流降级的方法
 *          }
 *         2.注解的方式
 *           @SentinelResource(value = "自定义资源名",blockHandler = "降级/限流回调方法",fallback = "对所有类型异常的回调方法")
 *           blockHandler必须在同一个类中，方法签名、返回值一致，入参除了原参数，还能带BlockException e获取异常信息。
 *           fallback a>在同一个类中时，方法签名、返回值一致，入参除了原参数，还能带Throwable throw获取异常信息；
 *                    b>在其他类中时，方法签名、返回值一致，需要声明fallbackClass="xx.class"，并且方法必须是静态方法。
 *
 * 2.sentinel和nacos结合，可以实现流控规则的持久化（详情见order服务的配置文件）
 *      1）依赖sentinel-datasource-nacos
 *      2）配置文件中设置nacos地址，分组，文件名等信息
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