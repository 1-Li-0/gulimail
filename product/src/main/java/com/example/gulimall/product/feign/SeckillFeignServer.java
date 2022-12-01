package com.example.gulimall.product.feign;

import com.example.common.utils.R;
import com.example.gulimall.product.feign.fallback.SeckillFeignFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(value = "seckill",fallback = SeckillFeignFallback.class)
public interface SeckillFeignServer {
    @GetMapping("/skuSeckill/{skuId}")
    R getSkuSeckillInfo(@PathVariable("skuId") Long skuId);
}
