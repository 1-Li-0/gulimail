package com.gulimall.auth.controller;

import com.alibaba.fastjson.JSON;
import com.example.common.utils.HttpUtils;
import com.example.common.utils.R;
import com.gulimall.auth.feign.MemberFeignServer;
import com.gulimall.auth.vo.SocialUser;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Controller
public class OAuth2Controller {
    @Autowired
    MemberFeignServer memberFeignServer;
    @Autowired
    StringRedisTemplate redisTemplate;

    @GetMapping("/oauth2.0/weibo/success")
    public String weiboOAuth(@RequestParam("code") String code) throws Exception {
        //code换取token，设置请求体参数
        Map<String, String> map = new HashMap<>();
        map.put("client_id", "4172830295");
        map.put("client_secret", "d0da87cef3502503679ac5494afa2429");
        map.put("grant_type", "authorization_code");
        map.put("redirect_uri", "http://auth.gulimall.com/oauth2.0/weibo/success");
        map.put("code", code);
        //发送post请求
        HttpResponse response = HttpUtils.doPost("api.weibo.com", "/oauth2/access_token", "post", null, null, map);
        if (response.getStatusLine().getStatusCode() == 200) {
            //获取第三方平台授权成功，取得token获取用户信息
            String json = EntityUtils.toString(response.getEntity());
            SocialUser socialUser = JSON.parseObject(json, SocialUser.class);
            //调用远程服务，判断当前用户是否注册过帐号
            try {
                R r = memberFeignServer.login(socialUser);
                if (r.getCode() == 0) {
                    //登陆成功，保存token到redis中可以做免密登录
                    redisTemplate.opsForValue().set("token", socialUser.getAccess_token(), Long.parseLong(socialUser.getAccess_token()), TimeUnit.MILLISECONDS);

                    //跳转首页
                    return "redirect:http://gulimall.com";
                } else {
                    return "redirect:http://auth.gulimall.com/login";
                }
            } catch (Exception e) {
                e.printStackTrace();
                return "redirect:http://auth.gulimall.com/login";
            }

        } else {
            return "redirect:http://auth.gulimall.com/login";
        }
    }
}
