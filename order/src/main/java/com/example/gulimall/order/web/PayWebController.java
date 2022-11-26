package com.example.gulimall.order.web;

import com.alipay.api.AlipayApiException;
import com.example.gulimall.order.config.AlipayTemplate;
import com.example.gulimall.order.service.OrderService;
import com.example.gulimall.order.vo.PayVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class PayWebController {
    @Autowired
    AlipayTemplate alipayTemplate;
    @Autowired
    OrderService orderService;

    /**
     *  注解参数produces：声明产生的数据是html页面，而不是json（String pay实际是一个表单页面的html代码，提交给支付宝，自动跳转支付界面）
     */
    @GetMapping(value = "/payOrder",produces = "text/html")
    public @ResponseBody String payOrder(@RequestParam("orderSn") String orderSn) throws AlipayApiException {
        PayVo payVo = orderService.getOrderPayInfo(orderSn);
        String pay = alipayTemplate.pay(payVo);
        return pay;
    }
}
