package com.example.gulimall.order.feign;

import com.example.common.utils.R;
import com.example.gulimall.order.vo.OrderLockStockVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient("ware")
public interface WareFeignServer {

    @PostMapping("/ware/waresku/hasStock")
    R getSkuHasStock(@RequestBody List<Long> skuIds);

    @GetMapping("/ware/wareinfo/getFare")
    R getFare(@RequestParam("addrId") Long addrId);

    @PostMapping("/ware/waresku/lockStock")
    R orderLockStock(@RequestBody OrderLockStockVo vo);
}
