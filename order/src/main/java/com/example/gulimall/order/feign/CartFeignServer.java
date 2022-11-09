package com.example.gulimall.order.feign;

import com.example.gulimall.order.vo.OrderItemVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient("cart")
public interface CartFeignServer {
    @GetMapping("/getCurrentUserItems")
    List<OrderItemVo> getCurrentUserItems();
}
