package com.example.gulimall.product.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 *  redis中需要保存场次信息和sku商品详情
 */
@Data
public class SeckillInfoVo {
    private Long id;
    /**
     * 活动id
     */
    private Long promotionId;
    /**
     * 活动场次id
     */
    private Long promotionSessionId;
    /**
     * 商品id
     */
    private Long skuId;
    /**
     * 秒杀价格
     */
    private BigDecimal seckillPrice;
    /**
     * 秒杀总量
     */
    private Integer seckillCount;
    /**
     * 每人限购数量
     */
    private Integer seckillLimit;
    /**
     * 排序
     */
    private Integer seckillSort;

    private Long startTime;
    private Long endTime;
    //商品的秒杀随机码
    private String randomCode;
}