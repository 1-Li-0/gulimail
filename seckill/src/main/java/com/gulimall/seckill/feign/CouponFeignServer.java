package com.gulimall.seckill.feign;

import com.example.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient("coupon")
public interface CouponFeignServer {

    @GetMapping("/coupon/seckillsession/getLatest3DaysSession")
    R getLatest3DaysSession();
}
