package com.gulimall.auth.feign;

import com.example.common.utils.R;
import com.gulimall.auth.vo.SocialUser;
import com.gulimall.auth.vo.UserLoginVo;
import com.gulimall.auth.vo.UserRegistVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "member")
public interface MemberFeignServer {
    //用户注册
    @PostMapping("/member/member/regist")
    R regist(@RequestBody UserRegistVo vo);
    //用户登录
    @PostMapping("/member/member/login")
    R login(@RequestBody UserLoginVo vo);
    //社交帐号登录
    @PostMapping("/member/member/oauth2/login")
    R oauthLogin(@RequestBody SocialUser vo) throws Exception;
}
