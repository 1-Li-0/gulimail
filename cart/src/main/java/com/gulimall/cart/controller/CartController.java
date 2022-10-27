package com.gulimall.cart.controller;

import com.example.common.constant.AuthServerConstant;
import com.example.common.utils.R;
import com.gulimall.cart.interceptor.CartInterceptor;
import com.gulimall.cart.service.CartService;
import com.gulimall.cart.vo.CartVo;
import com.gulimall.cart.vo.UserInfoTo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpSession;

@Controller
public class CartController {
    @Autowired
    private CartService cartService;

    /**
     * @return 跳转到购物车页面
     */
    @GetMapping("/cart.html")
    public String cartListPage(HttpSession session){
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        return "cartList";
    }

    /**
     * @return 购物车Ajax请求
     */
    @GetMapping("/cart.do")
    public R cartList(){
        return R.ok();
    }
}
