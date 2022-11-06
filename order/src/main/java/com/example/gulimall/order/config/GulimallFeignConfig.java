package com.example.gulimall.order.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

@Configuration
public class GulimallFeignConfig {

    /** 远程调用会重新生成请求模板，在请求到达前调用requestInterceptors中的全部拦截器
     * @return requestTemplate设置请求头，解决远程调用的请求头没有cookie的问题
     */
    @Bean("requestInterceptor")
    public RequestInterceptor requestInterceptor(){
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate requestTemplate) {
                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attributes!=null){
                    HttpServletRequest request = attributes.getRequest();
                    //将原请求头中的cookie设置到远程调用请求头中
                    if (request!=null){
                        requestTemplate.header("Cookie",request.getHeader("Cookie"));
                    }
                }
            }
        };
    }
}
