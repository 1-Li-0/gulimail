package com.example.gulimall.ware.listener;

import com.alibaba.fastjson.TypeReference;
import com.example.common.constant.OrderStatusEnum;
import com.example.common.to.OrderTo;
import com.example.common.to.mq.StockDetailTo;
import com.example.common.to.mq.StockLockedTo;
import com.example.common.utils.R;
import com.example.gulimall.ware.dao.WareSkuDao;
import com.example.gulimall.ware.entity.WareOrderTaskDetailEntity;
import com.example.gulimall.ware.entity.WareOrderTaskEntity;
import com.example.gulimall.ware.feign.OrderFeignServer;
import com.example.gulimall.ware.service.WareOrderTaskDetailService;
import com.example.gulimall.ware.service.WareOrderTaskService;
import com.example.gulimall.ware.service.WareSkuService;
import com.example.gulimall.ware.vo.OrderVo;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@RabbitListener(queues = {"stock.release.stock.queue"})
public class StockReleaseListener {
    @Autowired
    WareSkuService wareSkuService;

    @RabbitHandler
    public void handleStockLockedRelease(StockLockedTo to, Message message, Channel channel) throws IOException {
        System.out.println("监听到解锁库存的消息！");
        try {
            wareSkuService.unLockStock(to);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        } catch (Exception e) {
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), true);
        }
    }

    @RabbitHandler
    public void handleOrderCloseRelease(OrderTo to, Message message, Channel channel) throws IOException {
        System.out.println("订单关闭！");
        try {
            wareSkuService.unLockStock(to);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        } catch (Exception e) {
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), true);
        }
    }
}
