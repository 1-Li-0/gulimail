package com.gulimall.seckill.feign;

import com.example.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@FeignClient("product")
public interface ProductFeignServer {

    @RequestMapping("/product/skuinfo/info/{skuId}")
    R getSkuInfo(@PathVariable("skuId") Long skuId);
}
