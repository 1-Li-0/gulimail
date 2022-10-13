package com.xunqi.gulimall.search.vo;

import lombok.Data;

import java.util.List;

@Data
public class SearchParam {
    private String keyword; //关键字查询
    private Long catalog3Id; //分类查询
    private String sort; //排序（销量，价格，热度综合）
    private Integer hasStock; //是否有货
    private String skuPrice; //价格区间
    private List<Long> brandId; //品牌可以多选
    private List<String> attrs; //属性规格可以多选：attrs=2_5寸:6寸
    private Integer pageNum; //页码

    private String _queryString; //请求的查询参数: URL地址中？后面的全部字符串
}
