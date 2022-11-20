package com.example.gulimall.member.feign;

import com.example.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient("order")
public interface OrderFeignService {

    @PostMapping("/order/order/listWithItems")
    R listWithItems(@RequestBody Map<String, Object> params);
}
