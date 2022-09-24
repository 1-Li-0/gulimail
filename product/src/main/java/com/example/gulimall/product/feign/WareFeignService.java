package com.example.gulimall.product.feign;

import com.example.common.to.SkuHasStockVo;
import com.example.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient("ware")
public interface WareFeignService {

    @PostMapping("/ware/waresku/hasStock")
    R getSkuHasStock(@RequestBody List<Long> skuIds);
}
