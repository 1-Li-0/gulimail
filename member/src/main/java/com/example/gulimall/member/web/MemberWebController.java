package com.example.gulimall.member.web;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.example.common.to.OrderTo;
import com.example.common.utils.Constant;
import com.example.common.utils.R;
import com.example.gulimall.member.feign.OrderFeignService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.Map;

@Controller
public class MemberWebController {
    @Autowired
    OrderFeignService orderFeignService;

    //查出登陆用户的所有订单数据
    @GetMapping("/memberOrder.do")
    public String memberOrderPage(@RequestParam(value = "pageNum",defaultValue = "1") Integer pageNum,@RequestParam(value = "pageSize",defaultValue = "10")Integer pageSize, Model model){
        Map<String, Object> params = new HashMap<>();
        params.put(Constant.PAGE,pageNum);
        params.put(Constant.LIMIT,pageSize);
        //会员订单页需要远程查询订单状态
        R r = orderFeignService.listWithItems(params);
        System.out.println(JSON.toJSONString(r));
        model.addAttribute("orders",r);
        return "orderList";
    }
}
