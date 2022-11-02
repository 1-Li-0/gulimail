package com.example.gulimall.order.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class PageController {
    @GetMapping("/{pageName}")
    public String toPage(@PathVariable("pageName") String pageName){
        return pageName;
    }
}
