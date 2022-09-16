package com.example.gulimall.ware.feign;

import com.example.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@FeignClient("gateway")
public interface ProductFeignService {

    //通过网关调用远程服务，需要在路径前增加/api
    @RequestMapping("/api/product/skuinfo/info/{skuId}")
     R info(@PathVariable("skuId") Long skuId);
}
