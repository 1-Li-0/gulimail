package com.gulimall.seckill.scheduled;

import com.gulimall.seckill.service.SeckillService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 秒杀商品定时上架（凌晨三点的定时任务，最近三天的秒杀商品）
 */
@Slf4j
@Service
public class SeckillSkuScheduled {
    @Autowired
    SeckillService seckillService;
    @Autowired
    RedissonClient redissonClient;

    private final String UPLOAD_LOCK = "seckill:upload:lock";
    private final String DOWNLOAD_LOCK = "seckill:download:lock";

    @Scheduled(cron = "0 * * * * ?")
    public void upSeckillSkuLatest3Days() {
        //重复上架无需处理，幂等性处理（分布式锁）
        System.out.println("开始上架商品");
        RLock lock = redissonClient.getLock(UPLOAD_LOCK);
        lock.lock(10, TimeUnit.SECONDS);
        try {
            seckillService.upSeckillSkuLatest3Days();
        } finally {
            lock.unlock();
        }
    }
    //每小时删除一次过期的秒杀商品
    @Scheduled(cron = "0 0 * * * ?")
    public void delSeckillSkus() {
        //重复上架无需处理，幂等性处理（分布式锁）
        System.out.println("下架过期秒杀商品");
        RLock lock = redissonClient.getLock(DOWNLOAD_LOCK);
        lock.lock(10, TimeUnit.SECONDS);
        try {
//            seckillService.delSeckillSkus(); //删除redis中过期秒杀的数据
        } finally {
            lock.unlock();
        }
    }
}
