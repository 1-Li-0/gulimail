package com.example.gulimall.order.listener;

import com.example.common.to.mq.SeckillOrderTo;
import com.example.gulimall.order.entity.OrderEntity;
import com.example.gulimall.order.service.OrderService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@RabbitListener(queues = {"stock.seckill.order.queue"})
@Component
public class OrderSeckillListener {
    @Autowired
    OrderService orderService;

    @RabbitHandler
    public void orderSeckillListener(SeckillOrderTo seckillOrder, Message message, Channel channel) throws IOException {
        try {
            //创建秒杀订单
            orderService.createSeckillOrder(seckillOrder);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);//不批量确认
        }catch (Exception e){
            channel.basicReject(message.getMessageProperties().getDeliveryTag(),true);//拒绝后，消息回到队列
        }
    }
}
