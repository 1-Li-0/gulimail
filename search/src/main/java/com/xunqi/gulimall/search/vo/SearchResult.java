package com.xunqi.gulimall.search.vo;

import com.example.common.to.es.SkuEsModel;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SearchResult {
    private List<SkuEsModel> products; //查询结果
    private List<BrandVo> brands; //查询出的结果所涉及到的全部品牌
    private List<CatalogVo> catalogs; //查询出的结果所涉及到的全部分类
    private List<AttrVo> attrs; //查询出的结果所涉及到的全部属性和属性值

    private Integer pageNum; //当前页码
    private Long total; //总记录
    private Integer totalPages; //总页数
    private List<Integer> pageNavs; //页码导航

    //================以上是页面需要的信息===========================

    private List<NavVo> navVos = new ArrayList<>(); //面包屑导航，此处初始化集合防止为空
    private List<Long> attrIds = new ArrayList<>(); //属性id的集合

    @Data
    public static class NavVo{
        private Long navId;
        private String navName;
        private String navValue;
        private String link; //面包屑导航生成的url
    }

    @Data
    public static class BrandVo {
        private Long brandId;
        private String brandName;
        private String brandImg;
    }
    @Data
    public static class CatalogVo {
        private Long catalogId;
        private String catalogName;
    }
    @Data
    public static class AttrVo {
        private Long attrId;
        private String attrName;
        private List<String> attrValue;
    }
}
