package com.gulimall.auth.controller;

import com.example.common.constant.AuthServerConstant;
import com.example.common.exception.BizCodeEnum;
import com.example.common.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/sms")
public class SmsSendController {
    @Autowired
    StringRedisTemplate redisTemplate;

    /**
     * 生产上应该使用第三方短信服务
     */
    @GetMapping("/sendCode")
    public R sendCode(@RequestParam("phone") String phone) {
        //TODO 接口防刷

        //查询redis中是否有验证码保存
        String s = redisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone);
        if (s != null && s != "" && System.currentTimeMillis() - Long.parseLong(s.split("_")[1]) < 60 * 1000) {
            //60s内不能再次发送请求
            return R.error(BizCodeEnum.SMS_CODE_EXCEPTION.getCode(), BizCodeEnum.SMS_CODE_EXCEPTION.getMsg());
        } else {
            String code = UUID.randomUUID().toString().substring(0, 5) + "_" + System.currentTimeMillis();
            //将验证码保存在redis中，设置5分钟过期时间
            redisTemplate.opsForValue().set(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone, code, 5, TimeUnit.MINUTES);
            System.out.println("手机号是：" + phone + "；验证码是：" + code.split("_")[0]);
            return R.ok();
        }
    }
}
