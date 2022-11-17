package com.example.gulimall.order.listener;

import com.example.gulimall.order.entity.OrderEntity;
import com.example.gulimall.order.service.OrderService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@RabbitListener(queues = {"order.release.order.queue"})
@Service
public class OrderCloseListener {
    @Autowired
    OrderService orderService;

    @RabbitHandler
    public void orderListener(OrderEntity entity, Message message, Channel channel) throws IOException {
        System.out.println("订单已过期!");
        try {
            orderService.closeOrder(entity);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);//不批量确认
        }catch (Exception e){
            channel.basicReject(message.getMessageProperties().getDeliveryTag(),true);//拒绝后，消息回到队列
        }
    }
}
