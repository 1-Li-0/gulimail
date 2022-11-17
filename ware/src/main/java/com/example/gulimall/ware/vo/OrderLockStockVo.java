package com.example.gulimall.ware.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class OrderLockStockVo {
    private String orderSn; //订单的唯一标识
    List<OrderItemVo> orderItems; //锁定库存需要订单项的id和购买数量
}
