package com.example.gulimall.order.config;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Configuration
public class MyRabbitConfig {
    @Autowired
    RabbitTemplate rabbitTemplate;

    //使用JSON序列化机制进行消息转换
    @Bean
    public MessageConverter messageConverter(){
        return new Jackson2JsonMessageConverter();
    }

    /** 定制RabbitTemplate
     *  1.开启消息确认的配置功能
     *    spring.rabbitmq.publisher-confirms=true；
     *    spring.rabbitmq.publisher-returns=true
     *  2.消息确认的回调
     *   setConfirmCallback(new RabbitTemplate.ConfirmCallback() {})
     *   setReturnCallback(new RabbitTemplate.ReturnCallback() {})
     */
    @PostConstruct
    public void initRabbitTemplate(){
        rabbitTemplate.setConfirmCallback(new RabbitTemplate.ConfirmCallback() {
            /** 发送消息成功或者失败都触发这个回调函数
             * @param correlationData 消息的关联数据，唯一ID
             * @param b 消息是否抵达Broker
             * @param s 失败的原因
             */
            @Override
            public void confirm(CorrelationData correlationData, boolean b, String s) {

            }
        });
        rabbitTemplate.setReturnCallback(new RabbitTemplate.ReturnCallback() {
            /** 投递失败时会触发这个回调函数
             * @param message 投递失败的消息详细信息
             * @param i 失败的状态码
             * @param s 失败的具体原因
             * @param s1 失败时这个消息发给哪个交换机
             * @param s2 失败时消息所使用的路由键
             */
            @Override
            public void returnedMessage(Message message, int i, String s, String s1, String s2) {

            }
        });
    }
}
