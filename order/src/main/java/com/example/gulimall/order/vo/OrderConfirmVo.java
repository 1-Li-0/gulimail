package com.example.gulimall.order.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderConfirmVo {

    //用户收件地址列表
    List<MemberAddressVo> memberAddressList;
    //选中的商品列表
    List<OrderItemVo> items;
    //优惠卷，积分信息
    Integer integration;
    //总价
    BigDecimal total;
    //应付总额
    BigDecimal payPrice;
}
