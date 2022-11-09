package com.example.gulimall.order.web;

import com.example.gulimall.order.service.OrderService;
import com.example.gulimall.order.vo.OrderConfirmVo;
import com.example.gulimall.order.vo.OrderSubmitRespVo;
import com.example.gulimall.order.vo.OrderSubmitVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.concurrent.ExecutionException;

@Controller
public class OrderWebController {
    @Autowired
    OrderService orderService;

    @GetMapping("/toTrade")
    public String toTrade(Model model) throws ExecutionException, InterruptedException {
        OrderConfirmVo confirmVo = orderService.confirmOrder();
        model.addAttribute("orderConfirmData",confirmVo);
        return "confirm";
    }

    @PostMapping("/submitOrder.do")
    public String submitOrder(OrderSubmitVo vo,Model model){
        OrderSubmitRespVo respVo = orderService.submitOrder(vo);
        if (respVo.getCode() != 0){
            //失败
            return "redirect:http://order.gulimall.com/toTrade";
        }else {
            //成功
            model.addAttribute("submitOrder",respVo);
            return "pay";
        }
    }
}
