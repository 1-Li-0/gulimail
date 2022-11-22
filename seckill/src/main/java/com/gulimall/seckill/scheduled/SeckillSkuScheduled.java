package com.gulimall.seckill.scheduled;

import com.gulimall.seckill.service.SeckillService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 秒杀商品定时上架（凌晨三点的定时任务，最近三天的秒杀商品）
 */
@Slf4j
@Service
public class SeckillSkuScheduled {
    @Autowired
    SeckillService seckillService;

    @Scheduled(cron = "0 0 3 * * ?")
    public void upSeckillSkuLatest3Days() {
        //重复上架无需处理
        seckillService.upSeckillSkuLatest3Days();
    }
}
