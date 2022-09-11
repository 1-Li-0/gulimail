package com.example.gulimall.product.vo;

import lombok.Data;

@Data
public class AttrVo {
    private Long attrId;

    private String attrName;

    private Integer searchType;

    private Integer valueType;

    private String icon;

    private String valueSelect;

    private Integer attrType;

    private Long enable;

    private Long catelogId;

    private Integer showDesc;

    //需要比AttrEntity对象多接收的数据
    private Long attrGroupId;
}
