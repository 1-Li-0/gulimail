package com.gulimall.seckill.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.example.common.utils.R;
import com.gulimall.seckill.feign.CouponFeignServer;
import com.gulimall.seckill.feign.ProductFeignServer;
import com.gulimall.seckill.service.SeckillService;
import com.gulimall.seckill.vo.SeckillSessionsWithSkus;
import com.gulimall.seckill.vo.SeckillSkusInfoRedisTo;
import com.gulimall.seckill.vo.SkuInfoVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SeckillServiceImpl implements SeckillService {
    @Autowired
    CouponFeignServer couponFeignServer;
    @Autowired
    StringRedisTemplate redisTemplate;
    @Autowired
    ProductFeignServer productFeignServer;

    private final String SESSIONS_CACHE_PREFIX = "seckill:sessions";
    private final String SKUSECKILL_CACHE_PREFIX = "seckill:skus";

    @Override
    public void upSeckillSkuLatest3Days() {
        //coupon系统中扫描需要参与秒杀上架的商品--数据库`gulimall_sms`
        R r = couponFeignServer.getLatest3DaysSession();
        if (r.getCode() == 0){
            List<SeckillSessionsWithSkus> sessions = r.getData("data", new TypeReference<List<SeckillSessionsWithSkus>>() {
            });
            //秒杀信息保存到redis中
            saveSessionInfos(sessions);
            saveSessionSkuInfos(sessions);
        }
    }

    private void saveSessionInfos(List<SeckillSessionsWithSkus> sessions) {
        sessions.forEach(session -> {
            //秒杀活动需要按时间段划分场次
            Long start = session.getStartTime().getTime();
            Long end = session.getEndTime().getTime();
            String key = SESSIONS_CACHE_PREFIX+start+"_"+end;
            List<String> skuIds = session.getRelationSkus().stream().map(skuRelationVo -> skuRelationVo.getSkuId().toString()).collect(Collectors.toList());
            redisTemplate.opsForList().leftPushAll(key,skuIds);
        });
    }

    private void saveSessionSkuInfos(List<SeckillSessionsWithSkus> sessions) {
        BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(SKUSECKILL_CACHE_PREFIX);
        sessions.forEach(session -> {
            session.getRelationSkus().forEach(skuRelationVo -> {
                SeckillSkusInfoRedisTo redisTo = new SeckillSkusInfoRedisTo();
                BeanUtils.copyProperties(skuRelationVo,redisTo);
                //远程查询sku详情
                R r = productFeignServer.getSkuInfo(skuRelationVo.getSkuId());
                if (r.getCode() == 0){
                    SkuInfoVo skuInfo = r.getData("skuInfo",new TypeReference<SkuInfoVo>(){});
                    redisTo.setSkuInfoVo(skuInfo);
                }
                //保存商品秒杀的开始时间和结束时间
                redisTo.setStartTime(session.getStartTime().getTime());
                redisTo.setEndTime(session.getEndTime().getTime());
                //设置随机码
                String randomCode = UUID.randomUUID().toString().replace("-", "");
                redisTo.setRandomCode(randomCode);
                hashOps.put(skuRelationVo.getSkuId(), JSON.toJSONString(redisTo));
            });
        });
    }
}
