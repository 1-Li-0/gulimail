package com.example.gulimall.order.vo;


import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

public class OrderConfirmVo {

    //用户收件地址列表
    @Getter @Setter
    List<MemberAddressVo> memberAddressList;
    //选中的商品列表
    @Getter @Setter
    List<OrderItemVo> items;
    //优惠卷，积分信息
    @Getter @Setter
    Integer integration;
    //总价
    public BigDecimal getTotal() {
        BigDecimal total = new BigDecimal("0");
        if (items!=null && items.size()>0){
            for (OrderItemVo item : items) {
                BigDecimal multiply = item.getPrice().multiply(new BigDecimal(item.getCount()));
                total = total.add(multiply);
            }
        }
        return total;
    }

    //应付总额
    public BigDecimal getPayPrice() {
        return getTotal();
    }
}
