package com.example.gulimall.product.web;

import com.example.gulimall.product.entity.CategoryEntity;
import com.example.gulimall.product.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class IndexController {
    @Autowired
    private CategoryService categoryService;

    @GetMapping({"/","/index.html"})
    public String indexPage(Model model){
        List<CategoryEntity> categoryEntities = categoryService.getLevel1Catagorys();
        model.addAttribute("categorys",categoryEntities);
        //视图解析器classpath:templates/{返回值}.html
        return "index";
    }
}
