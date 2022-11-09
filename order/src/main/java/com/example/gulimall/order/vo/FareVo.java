package com.example.gulimall.order.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class FareVo {
    //用户的默认收件地址
    private MemberAddressVo address;
    //运费
    private BigDecimal farePrice;
}
