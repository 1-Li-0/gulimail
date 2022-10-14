package com.example.gulimall.product.web;

import com.example.gulimall.product.service.SkuInfoService;
import com.example.gulimall.product.vo.SkuItemVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.concurrent.ExecutionException;

@Controller
public class ItemController {
    @Autowired
    SkuInfoService skuInfoService;

    @GetMapping("/{skuId}.html")
    public String skuItem(@PathVariable Long skuId, Model model){
        SkuItemVo skuItemVo = null;
        try {
            skuItemVo = skuInfoService.item(skuId);
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        model.addAttribute("item",skuItemVo);
        return "item";
    }
}
