package com.example.gulimall.order.config;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *  MessageConverter依赖于RabbitTemplate，但是自定义的MyRabbitConfig中又注入了RabbitTemplate
 *  两个配置不能放在一个类中
 */
@Configuration
public class MsgConverterConfig {
    //使用JSON序列化机制进行消息对象转换
    @Bean
    public MessageConverter messageConverter(){
        return new Jackson2JsonMessageConverter();
    }
}
