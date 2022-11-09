package com.example.gulimall.order.feign;

import com.example.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient("product")
public interface ProductFeignServer {
    @GetMapping("/product/spuinfo/{skuId}/getSpuInfo")
    R getSpuInfoBySkuId(@PathVariable("skuId")Long skuId);
}
