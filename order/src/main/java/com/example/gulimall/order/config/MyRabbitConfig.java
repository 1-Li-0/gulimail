package com.example.gulimall.order.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ整合springboot的配置
 */
@Configuration
public class MyRabbitConfig {
    @Autowired
    RabbitTemplate rabbitTemplate;

    //使用JSON序列化机制进行消息对象转换
    @Bean
    public MessageConverter messageConverter(){
        return new Jackson2JsonMessageConverter();
    }

    /** 定制RabbitTemplate【消息确认机制pulisher，consumer（手动ack）】
     *  1.每个消息都要在数据库【redis/mysql】中记录日志【成功抵达/错误抵达】；定期将失败的消息再发一次
     *    【数据库中保存了消息的交换机，路由键，消息对象的JSON字符串，消息对象类型...】
     *  2.开启消息确认的配置功能
     *    spring.rabbitmq.publisher-confirms=true；
     *    spring.rabbitmq.publisher-returns=true；
     *  3.消息确认的回调函数
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
                if (b){
                    //correlationData.getId()作为id，查询并修改日志状态为1-已发送
                }else {
                    //重发消息（查询数据库保存的消息后重发）
                }
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
                //修改日志状态为2-错误抵达
            }
        });
    }

    @Bean
    public Exchange orderEventExchange(){
        return new TopicExchange("order-event-exchange",true,false);
    }

    @Bean
    public Queue orderReleaseOrderQueue(){
        return new Queue("order.release.order.queue",true,false,false);
    }

    /**
     * 延迟队列
     */
    @Bean
    public Queue orderDelayQueue(){
        Map<String,Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange","order-event-exchange");
        args.put("x-dead-letter-routing-key","order.release.order");
        args.put("x-message-ttl",30000);
        return new Queue("order.delay.queue",true,false,false, args);
    }

    @Bean
    public Binding orderDelayBinding(){
        return new Binding("order.delay.queue",
                Binding.DestinationType.QUEUE,
                "order-event-exchange",
                "order.create.order",
                null);
    }

    @Bean
    public Binding orderReleaseOrderBinding(){
        return new Binding("order.release.order.queue",
                Binding.DestinationType.QUEUE,
                "order-event-exchange",
                "order.release.order",
                null);
    }

    /**
     *  订单解锁后还需要绑定库存解锁队列，检查库存是否解锁
     * @return
     */
    @Bean
    public Binding orderReleaseOtherBinding(){
        return new Binding("stock.release.stock.queue",
                Binding.DestinationType.QUEUE,
                "order-event-exchange",
                "order.release.other.#",
                null);
    }

    /**
     *  监听秒杀服务需要的队列和绑定关系
     */
    @Bean
    public Queue orderSeckillOrderQueue(){
        return new Queue("order.seckill.order.queue",true,false,false);
    }
    @Bean
    public Binding orderSeckillOrderBinding(){
        return new Binding("order.seckill.order.queue",
                Binding.DestinationType.QUEUE,
                "order-event-exchange",
                "order.seckill.order",
                null);
    }
}
