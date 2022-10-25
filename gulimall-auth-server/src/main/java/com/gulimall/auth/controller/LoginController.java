package com.gulimall.auth.controller;

import com.alibaba.fastjson.TypeReference;
import com.example.common.constant.AuthServerConstant;
import com.example.common.to.MemberRespVo;
import com.example.common.utils.R;
import com.gulimall.auth.feign.MemberFeignServer;
import com.gulimall.auth.vo.UserLoginVo;
import com.gulimall.auth.vo.UserRegistVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Controller
public class LoginController {
    /**
     * 登录和注册页面可以由WebMvc的视图WebMvcConfigurer配置跳转
     *
     * @GetMapping("/login") public String loginPage() {
     * return "login";
     * }
     * @GetMapping("/reg") public String regPage() {
     * return "reg";
     * }
     */
    @Autowired
    StringRedisTemplate redisTemplate;
    @Autowired
    MemberFeignServer memberFeignServer;

    //注册
    @PostMapping("/registForm")
    public String registForm(@Valid UserRegistVo userRegistVo, BindingResult result, RedirectAttributes redirectAttributes) {
        //JSR303参数校验
        if (result.hasErrors()) {
            Map<String, String> errors = result.getFieldErrors().stream().collect(Collectors.toMap(FieldError::getField, DefaultMessageSourceResolvable::getDefaultMessage));
            //重定向不能使用Model来转发数据
            redirectAttributes.addFlashAttribute("errors", errors);
            //TODO 重定向实际是把数据保存在session中，分布式项目中会有session会话的问题
            return "redirect:http://auth.gulimall.com/reg.html";
        }
        System.out.println(userRegistVo);
        //后台校验和保存数据
        String code = userRegistVo.getCode();
        String s = redisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + userRegistVo.getPhone());
        //验证码不能为空
        if (s != null && code != "") {
            //验证码错误
            if (!code.equals(s.split("_")[0])) {
                Map<String, String> errors = new HashMap<>();
                errors.put("code", "验证码错误");
                redirectAttributes.addFlashAttribute("errors", errors);
                return "redirect:http://auth.gulimall.com/reg.html";
            } else {
                //验证码正确，删除redis保存的验证码
                redisTemplate.delete(AuthServerConstant.SMS_CODE_CACHE_PREFIX + userRegistVo.getPhone());
                //调用远程服务
                R r = memberFeignServer.regist(userRegistVo);
                if (r.getCode() == 0) {
                    //调用远程服务member保存数据，成功时跳转到登陆页面
                    return "redirect:http://auth.gulimall.com/login.html";
                } else {
                    //远程服务失败
                    Map<String, String> errors = new HashMap<>();
                    errors.put("msg", r.getData("msg", new TypeReference<String>() {
                    }));
                    redirectAttributes.addFlashAttribute("errors", errors);
                    return "redirect:http://auth.gulimall.com/reg.html";
                }
            }

        } else {
            Map<String, String> errors = new HashMap<>();
            errors.put("code", "验证码不能为空");
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:http://auth.gulimall.com/reg.html";
        }
    }

    @PostMapping("/userLogin")
    public String userLogin(UserLoginVo vo, RedirectAttributes redirectAttributes, HttpSession session) {
        R r = memberFeignServer.login(vo);
        if (r.getCode() == 0) {
            //保存用户信息到session
            MemberRespVo data = r.getData("data", new TypeReference<MemberRespVo>() {
            });
            session.setAttribute(AuthServerConstant.LOGIN_USER, data);
            return "redirect:http://gulimall.com";
        } else {
            //登录失败
            Map<String, String> errors = new HashMap<>();
            errors.put("msg", r.getData("msg", new TypeReference<String>() {
            }));
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:http://auth.gulimall.com/login.html";
        }
    }

    @GetMapping("/login.html")
    public String loginPage(HttpSession session) {
        //session账号验证
        MemberRespVo data = (MemberRespVo) session.getAttribute(AuthServerConstant.LOGIN_USER);
        if (data != null) {
            UserLoginVo userLogin = new UserLoginVo();
            userLogin.setLoginAccount(data.getUsername());
            userLogin.setPassword(data.getPassword());
            R r = memberFeignServer.login(userLogin);
            if (r.getCode() == 0) {
                return "redirect:http://gulimall.com";
            } else {
                return "login";
            }
        }else return "login";
    }
}
