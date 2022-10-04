package com.xunqi.gulimall.search.vo;

import com.example.common.to.es.SkuEsModel;
import lombok.Data;

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
        private String attrValue;
    }
}
