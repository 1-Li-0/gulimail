package com.example.gulimall.order.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderSubmitVo {

    private Long addrId;
    private String orderToken;
    private BigDecimal payPrice;
    private Integer payType; //支付方式
    private String note; //订单备注

}
