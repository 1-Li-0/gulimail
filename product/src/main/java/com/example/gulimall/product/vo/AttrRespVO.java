package com.example.gulimall.product.vo;

import lombok.Data;

@Data
public class AttrRespVO extends AttrVO {

    //所属分组
    private String groupName;
    //所属分类
    private String catelogName;
    //修改时需要回显三级分类（categoryPath）和分组
    private Long[] catelogPath;
}
