package com.gulimall.seckill.controller;

import com.example.common.utils.R;
import com.gulimall.seckill.service.SeckillService;
import com.gulimall.seckill.to.SeckillSkusInfoRedisTo;
import com.gulimall.seckill.vo.SeckillRespVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class SeckillController {

    @Autowired
    SeckillService seckillService;

    /**
     * 首页轮播
     * 获取当前时间参与秒杀的商品
     */
    @GetMapping("/currentSeckillSkus")
    @ResponseBody
    public R getCurrentSeckillSkus() {
        List<SeckillSkusInfoRedisTo> list = seckillService.getCurrentSeckillSkus();
        return R.ok().setData(list);
    }

    @GetMapping("/skuSeckill/{skuId}")
    @ResponseBody
    public R getSkuSeckillInfo(@PathVariable("skuId") Long skuId) {
        try {
            SeckillSkusInfoRedisTo to = seckillService.getSkuSeckillInfo(skuId);
            return R.ok().setData(to);
        } catch (Exception e) {
            return R.error();
        }
    }

    @GetMapping("/seckill")
    public String seckill(@RequestParam("seckillid") String seckillid, @RequestParam("code") String code, @RequestParam("num") Integer num, Model model) {
        SeckillRespVo respVo = seckillService.seckill(seckillid, code, num);
        model.addAttribute("data",respVo);
        return "success";
    }
}
