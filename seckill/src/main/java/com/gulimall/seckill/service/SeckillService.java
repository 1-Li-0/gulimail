package com.gulimall.seckill.service;

import com.gulimall.seckill.to.SeckillSkusInfoRedisTo;
import com.gulimall.seckill.vo.SeckillRespVo;

import java.util.List;

public interface SeckillService {
    void upSeckillSkuLatest3Days();

    List<SeckillSkusInfoRedisTo> getCurrentSeckillSkus();

    SeckillSkusInfoRedisTo getSkuSeckillInfo(Long skuId);

    SeckillRespVo seckill(String seckillid, String code, Integer num);
}
