package com.example.gulimall.order;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 *  例：在类A里面有方法a 和方法b， 然后方法b上面用 @Transactional加了方法级别的事务，在方法a里面 调用了方法b， 方法b里面的事务不会生效。
 *  本地事务在spring中失效，原因：绕过了代理对象，直接调用本类中的方法【相当于将调用的方法体直接复制在本方法事务中】
 *  解决方法：使用代理对象
 *  注意：不能在本类中注入自己的bean对象，循环依赖
 *  解决：1）pom中引入spring-boot-starter-aop的依赖
 *       2）启动类上使用注解@EnableAspectJAutoProxy(exposeProxy = true) 暴露代理对象，开启AspectJ动态代理
 *       3）方法中使用AopContext.currentProxy()调用本类的代理对象，实现本类中方法互调，并且事务不扩散导致事务失效
 *
 *  使用Seata控制分布式事务：
 *      1）每一个微服务的数据库需要创建一个undo_log表
 *      2)整合：
 *         1.导入依赖
 *         2.下载和依赖的seata-all-0.7.1相同版本号的seata-server事务协调器
 *         3.修改registry.conf中的配置（注册中心），和file.conf中的配置（日志保存）
 *         4.编写配置类配置DataSourceProxy数据源代理对象
 *         5.每个微服务都需要配置文件registry.conf，file.conf；
 *           修改file.conf中的vgroup_mapping.{application.name}-fescar-service-group = "default"
 *         6.使用@GlobalTransactional注解标注在“大事务方法”上，@Transactional标注在“小事务”上
 */

@EnableAspectJAutoProxy(exposeProxy = true)
@EnableRabbit
@EnableRedisHttpSession
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class OrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }

}
