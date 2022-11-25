package com.gulimall.seckill.controller;

import com.example.common.utils.R;
import com.gulimall.seckill.service.SeckillService;
import com.gulimall.seckill.to.SeckillSkusInfoRedisTo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SeckillController {

    @Autowired
    SeckillService seckillService;

    /**
     * 首页轮播
     * 获取当前时间参与秒杀的商品
     */
    @GetMapping("/currentSeckillSkus")
    public R getCurrentSeckillSkus() {
        List<SeckillSkusInfoRedisTo> list = seckillService.getCurrentSeckillSkus();
        return R.ok().setData(list);
    }

    @GetMapping("/skuSeckill/{skuId}")
    public R getSkuSeckillInfo(@PathVariable("skuId") Long skuId) {
        try {
            SeckillSkusInfoRedisTo to = seckillService.getSkuSeckillInfo(skuId);
            return R.ok().setData(to);
        } catch (Exception e) {
            return R.error();
        }
    }

    @GetMapping("/seckill")
    public R seckill(@RequestParam("seckillid") String seckillid, @RequestParam("code") String code, @RequestParam("num") Integer num) {
        String orderSn = seckillService.seckill(seckillid, code, num);
        System.out.println(orderSn);
        return R.ok().setData(orderSn);
    }
}
