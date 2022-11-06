package com.example.gulimall.order.vo;

import com.example.gulimall.order.entity.OrderEntity;
import lombok.Data;

@Data
public class OrderSubmitRespVo {
    //生成订单
    private OrderEntity order;
    //状态码，0表示成功，默认值是1
    private Integer code = 1;
}
