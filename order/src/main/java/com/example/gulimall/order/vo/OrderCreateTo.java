package com.example.gulimall.order.vo;

import com.example.gulimall.order.entity.OrderEntity;
import com.example.gulimall.order.entity.OrderItemEntity;
import lombok.Data;

import java.util.List;

@Data
public class OrderCreateTo {
    OrderEntity order;
    List<OrderItemEntity> orderItems;
}
