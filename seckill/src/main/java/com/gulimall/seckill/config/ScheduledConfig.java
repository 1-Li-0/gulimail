package com.gulimall.seckill.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.task.TaskExecutionProperties;
import org.springframework.boot.task.TaskExecutorBuilder;
import org.springframework.boot.task.TaskExecutorCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

/**
 * 定时任务的自动配置类TaskSchedulingAutoConfiguration
 * 异步任务的自动配置类TaskExecutionAutoConfiguration
 */
@EnableAsync
@EnableScheduling
@Slf4j
@Component
public class ScheduledConfig {
    /**
     * 1、spring中的定时任务可以使用@EnableScheduling+@Scheduled注解
     * 2、 cron在spring中不支持第七位“年”，而且第六位的周几就是数字几，也可以使用英文MON-SUN
     * 3、定时任务不应该阻塞。
     *      解决阻塞的方法：
     *      1）让业务使用异步线程执行（提交到线程池）
     *          CompletableFuture.runAsync(()->{},executor);
     *      2）设置TaskSchedulingAutoConfiguration的线程池配置，实现SchedulingConfigurer类
     *          spring.task.scheduling.pool.size=5
     *      3）让定时任务异步执行（此方法需要使用@EnableAsync+@Async注解），可以重新配置线程池
     */
/*    @Async
    @Scheduled(cron = "* * * ? * MON")
    public void hello() throws InterruptedException {
        log.info("hello...");
        Thread.sleep(3000);
    }*/

    //配置线程池
    @Autowired
    ObjectProvider<TaskExecutorCustomizer> taskExecutorCustomizers;
    @Autowired
    ObjectProvider<TaskDecorator> taskDecorator;
    @Autowired
    TaskExecutionProperties properties;

    @Bean
    public TaskExecutorBuilder taskExecutorBuilder() {
        TaskExecutionProperties.Pool pool = properties.getPool();
        TaskExecutorBuilder builder = new TaskExecutorBuilder();
        builder = builder.queueCapacity(pool.getQueueCapacity());
        builder = builder.corePoolSize(pool.getCoreSize());
        builder = builder.maxPoolSize(pool.getMaxSize());
        builder = builder.allowCoreThreadTimeOut(pool.isAllowCoreThreadTimeout());
        builder = builder.keepAlive(pool.getKeepAlive());
        builder = builder.threadNamePrefix(properties.getThreadNamePrefix());
        builder = builder.customizers(taskExecutorCustomizers);
        builder = builder.taskDecorator((TaskDecorator) taskDecorator.getIfUnique());
        return builder;
    }

    @Bean
    public ThreadPoolTaskExecutor applicationTaskExecutor(TaskExecutorBuilder builder) {
        return builder.build();
    }
}
