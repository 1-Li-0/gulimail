package com.gulimall.seckill.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.example.common.to.MemberRespVo;
import com.example.common.to.mq.SeckillOrderTo;
import com.example.common.utils.R;
import com.gulimall.seckill.feign.CouponFeignServer;
import com.gulimall.seckill.feign.ProductFeignServer;
import com.gulimall.seckill.interceptor.LoginUserInterceptor;
import com.gulimall.seckill.service.SeckillService;
import com.gulimall.seckill.vo.SeckillSessionsWithSkus;
import com.gulimall.seckill.to.SeckillSkusInfoRedisTo;
import com.gulimall.seckill.vo.SeckillSkuRelationVo;
import com.gulimall.seckill.vo.SkuInfoVo;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class SeckillServiceImpl implements SeckillService {
    @Autowired
    CouponFeignServer couponFeignServer;
    @Autowired
    StringRedisTemplate redisTemplate;
    @Autowired
    ProductFeignServer productFeignServer;
    @Autowired
    RedissonClient redissonClient;
    @Autowired
    RabbitTemplate rabbitTemplate;

    private final String SESSIONS_CACHE_PREFIX = "seckill:sessions:";
    private final String SKUSECKILL_CACHE_PREFIX = "seckill:skus:";
    private final String SKU_STOCK_SEMAPHORE = "seckill:stock:"; //不能用skuId当作信号量扣减的key

    @Override
    public void upSeckillSkuLatest3Days() {
        //coupon系统中扫描需要参与秒杀上架的商品--数据库`gulimall_sms`
        R r = couponFeignServer.getLatest3DaysSession();
        if (r.getCode() == 0) {
            List<SeckillSessionsWithSkus> sessions = r.getData("data", new TypeReference<List<SeckillSessionsWithSkus>>() {
            });
            if (sessions != null && sessions.size() > 0) {
                //秒杀信息保存到redis中
                saveSessionInfos(sessions);
                saveSessionSkuInfos(sessions);
            }
        }
    }

    @Override
    public List<SeckillSkusInfoRedisTo> getCurrentSeckillSkus() {
        List<SeckillSkusInfoRedisTo> tos = new ArrayList<>();
        Long now = System.currentTimeMillis();
        Set<String> keys = redisTemplate.keys(SESSIONS_CACHE_PREFIX + "*");
        for (String key : keys) {
            String[] startAndEnd = key.replace(SESSIONS_CACHE_PREFIX, "").split("_");
            if (now >= Long.parseLong(startAndEnd[0]) && now <= Long.parseLong(startAndEnd[1])) {
                //获取符合条件的session
                List<String> ids = redisTemplate.opsForList().range(key, -100, 100);
                //查询hash结构的redis数据
                BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SKUSECKILL_CACHE_PREFIX);
                List<String> list = hashOps.multiGet(ids);
                if (list != null && list.size() > 0) {
                    //根据key查询sku详情
                    for (String info : list) {
                        //转换数据类型
                        SeckillSkusInfoRedisTo redisTo = JSON.parseObject(info, SeckillSkusInfoRedisTo.class);
                        //当前秒杀已经开始，不需要把随机码randomCode设置为null进行保密
                        tos.add(redisTo);
                    }
                }
                //当前时间只能属于一个场次，找到后结束当前循环
                break;
            }
        }
        return tos;
    }

    @Override
    public SeckillSkusInfoRedisTo getSkuSeckillInfo(Long skuId) {
        //取出所有该skuId对应的redis数据（hash数据）
        BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SKUSECKILL_CACHE_PREFIX);
        Set<String> keys = hashOps.keys();
        if (keys != null && keys.size() > 0) {
            List<SeckillSkusInfoRedisTo> tos = keys.stream()
                    .filter(key -> skuId == Long.parseLong(key.split("_")[1]))
                    .map(key -> {
                        SeckillSkusInfoRedisTo redisTo = JSON.parseObject(hashOps.get(key), SeckillSkusInfoRedisTo.class);
                        //秒杀活动结束的场次，不需要返回数据
                        if (redisTo.getEndTime() >= System.currentTimeMillis()) {
                            //秒杀活动还没开始，不能暴露随机码
                            if (redisTo.getStartTime() >= System.currentTimeMillis()) {
                                redisTo.setRandomCode(null);
                            }
                            return redisTo;
                        } else return null;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            if (tos != null && tos.size() > 0) {
                return tos.stream().min(Comparator.comparing(SeckillSkusInfoRedisTo::getStartTime)).get();
            } else return null;
        } else return null;
    }

    @Override
    public String seckill(String seckillid, String code, Integer num) {
        MemberRespVo memberRespVo = LoginUserInterceptor.loginUser.get();
        BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SKUSECKILL_CACHE_PREFIX);
        String s = hashOps.get(seckillid);
        if (!StringUtils.isEmpty(s)) {
            SeckillSkusInfoRedisTo info = JSON.parseObject(s, SeckillSkusInfoRedisTo.class);
            //校验数据
            Long now = System.currentTimeMillis();
            if (now >= info.getStartTime() && now <= info.getEndTime()) {
                if (code.equals(info.getRandomCode())) {
                    if (num <= info.getSeckillLimit()) {
                        //TODO 幂等性处理 userId_sessionId_skuId 买过的用户进行标记，不能继续购买 (秒杀结束时自动删除标记)
                        String key = memberRespVo.getId() + "_" + seckillid;
                        Long ttl = info.getEndTime() - now;
                        if (redisTemplate.opsForValue().setIfAbsent(key, num.toString(), ttl, TimeUnit.MILLISECONDS)) {
                            //获取信号量，验证库存
                            RSemaphore semaphore = redissonClient.getSemaphore(SKU_STOCK_SEMAPHORE + code);
                            boolean b = semaphore.tryAcquire(num);
                            if (b) {
                                String orderSn = IdWorker.getTimeId();
                                //封装订单数据
                                SeckillOrderTo orderTo = new SeckillOrderTo();
                                orderTo.setOrderSn(orderSn);
                                orderTo.setPromotionSessionId(info.getPromotionSessionId());
                                orderTo.setSkuId(info.getSkuId());
                                orderTo.setSeckillPrice(info.getSeckillPrice());
                                orderTo.setNum(num);
                                orderTo.setMemberId(memberRespVo.getId());
                                //MQ发消息（order服务监听，所以本服务不需要创建交换机和队列，也不用配置手动ack）
                                rabbitTemplate.convertAndSend("order-event-exchange", "order.seckill.order", orderTo);
                                return orderSn;
                            } else return "库存不足";
                        } else return "秒杀商品已经购买过，不能重复购买";
                    } else return "购买的数量超过限制";
                } else return "秒杀验证失败";
            } else return "秒杀超时";
        } else return "秒杀商品不存在";
    }

    private void saveSessionInfos(List<SeckillSessionsWithSkus> sessions) {
        sessions.forEach(session -> {
            //秒杀活动需要按时间段划分场次
            Long start = session.getStartTime().getTime();
            Long end = session.getEndTime().getTime();
            String key = SESSIONS_CACHE_PREFIX + start + "_" + end;
            //判断是否已经上架过了
            if (!redisTemplate.hasKey(key)) {
                List<String> skus = session.getRelationSkus().stream().map(skuRelationVo -> skuRelationVo.getPromotionSessionId().toString() + "_" + skuRelationVo.getSkuId().toString()).collect(Collectors.toList());
                redisTemplate.opsForList().leftPushAll(key, skus);
            }
        });
    }

    private void saveSessionSkuInfos(List<SeckillSessionsWithSkus> sessions) {
        /**
         *  注意：操作普通的redis数据使用redisTemplate，操作hash类型的数据使用BoundHashOperations
         */
        BoundHashOperations<String, String, Object> hashOps = redisTemplate.boundHashOps(SKUSECKILL_CACHE_PREFIX);
        sessions.forEach(session -> {
            List<SeckillSkuRelationVo> relationSkus = session.getRelationSkus();
            if (relationSkus != null && relationSkus.size() > 0) {
                relationSkus.forEach(skuRelationVo -> {
                    //不同场次可能有相同sku，需要用场次id+skuId作为redis中保存的key
                    String key = skuRelationVo.getPromotionSessionId().toString() + "_" + skuRelationVo.getSkuId().toString();
                    if (!hashOps.hasKey(key)) {
                        SeckillSkusInfoRedisTo redisTo = new SeckillSkusInfoRedisTo();
                        BeanUtils.copyProperties(skuRelationVo, redisTo);
                        //设置随机码
                        String randomCode = UUID.randomUUID().toString().replace("-", "");
                        redisTo.setRandomCode(randomCode);
                        //远程查询sku详情
                        R r = productFeignServer.getSkuInfo(skuRelationVo.getSkuId());
                        if (r.getCode() == 0) {
                            SkuInfoVo skuInfo = r.getData("skuInfo", new TypeReference<SkuInfoVo>() {
                            });
                            redisTo.setSkuInfoVo(skuInfo);
                        }
                        //保存商品秒杀的开始时间和结束时间
                        redisTo.setStartTime(session.getStartTime().getTime());
                        redisTo.setEndTime(session.getEndTime().getTime());
                        // redisson支持设置信号量来代替mysql扣减库存，库存数量就是信号量的值 --> 【限流】
                        RSemaphore semaphore = redissonClient.getSemaphore(SKU_STOCK_SEMAPHORE + randomCode);
                        semaphore.trySetPermits(skuRelationVo.getSeckillCount());
                        //操作redis
                        hashOps.put(key, JSON.toJSONString(redisTo));
                    }
                });
            }
        });
    }
}
