package com.example.gulimall.product.web;

import com.example.gulimall.product.entity.CategoryEntity;
import com.example.gulimall.product.service.CategoryService;
import com.example.gulimall.product.vo.Catelog2Vo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

@Controller
public class IndexController {
    @Autowired
    private CategoryService categoryService;

    //商城首页查询所有一级分类的请求
    @GetMapping({"/", "/index.html"})
    public String indexPage(Model model) {
        List<CategoryEntity> categoryEntities = categoryService.getLevel1Catagorys();
        model.addAttribute("categorys", categoryEntities);
        //视图解析器classpath:templates/{返回值}.html
        return "index";
    }

    @GetMapping("/index/catalog.json")
    public @ResponseBody
    Map<String, List<Catelog2Vo>> getCatalogJson() {
        Map<String, List<Catelog2Vo>> map = categoryService.getCatalogJson();
        return map;
    }
}
