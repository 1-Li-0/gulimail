package com.example.gulimall.product.feign.fallback;

import com.example.common.exception.BizCodeEnum;
import com.example.common.utils.R;
import com.example.gulimall.product.feign.SeckillFeignServer;
import org.springframework.stereotype.Component;

@Component
public class SeckillFeignFallback implements SeckillFeignServer {
    @Override
    public R getSkuSeckillInfo(Long skuId) {
        return R.error(BizCodeEnum.TOO_MANY_REQUEST.getCode(),BizCodeEnum.TOO_MANY_REQUEST.getMsg());
    }
}
