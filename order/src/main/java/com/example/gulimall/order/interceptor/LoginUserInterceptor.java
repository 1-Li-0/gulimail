package com.example.gulimall.order.interceptor;

import com.example.common.constant.AuthServerConstant;
import com.example.common.to.MemberRespVo;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class LoginUserInterceptor implements HandlerInterceptor {

    public static ThreadLocal<MemberRespVo> loginUser = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        MemberRespVo obj = (MemberRespVo) request.getSession().getAttribute(AuthServerConstant.LOGIN_USER);
        if (obj!=null){
            loginUser.set(obj);
            return true;
        }else {
            //没登陆，需要重定向到登陆页面
            response.sendRedirect("http://auth.gulimall.com/login.html");
            request.getSession().setAttribute("noLogin","请先登录");
            return false;
        }
    }
}