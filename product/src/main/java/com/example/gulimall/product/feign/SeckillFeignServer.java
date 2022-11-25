package com.example.gulimall.product.feign;

import com.example.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient("seckill")
public interface SeckillFeignServer {
    @GetMapping("/skuSeckill/{skuId}")
    R getSkuSeckillInfo(@PathVariable("skuId") Long skuId);
}
