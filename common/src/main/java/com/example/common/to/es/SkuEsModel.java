package com.example.common.to.es;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class SkuEsModel {
    private Long skuId;//sku_id
    private Long spuId;//spu_id
    private String skuTitle;//sku_title
    private BigDecimal skuPrice;
    private String skuImg;
    private Long saleCount;//sale_count
    private Boolean hasStock;
    private Long hotScore;
    private Long brandId;//brand_id
    private Long catalogId;//catalog_id
    private String brandName;
    private String brandImg;
    private String catalogName;
    private List<Attrs> attrs;
    @Data
    public static class Attrs {
        private Long attrId;
        private String attrName;
        private String attrValue;
    }
}
