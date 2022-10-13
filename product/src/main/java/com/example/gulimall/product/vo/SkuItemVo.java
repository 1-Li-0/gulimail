package com.example.gulimall.product.vo;

import com.example.gulimall.product.entity.SkuImagesEntity;
import com.example.gulimall.product.entity.SkuInfoEntity;
import com.example.gulimall.product.entity.SpuInfoDescEntity;
import lombok.Data;

import java.util.List;

//商品详情类
@Data
public class SkuItemVo {
    //根据skuId查询基本信息
    SkuInfoEntity info;

    boolean hasStock = true; //是否有货

    //查询sku默认图片的集合
    List<SkuImagesEntity> images;

    //查询spu的销售属性
    List<SkuItemSaleAttrVo> saleAttr;

    //查询spu的介绍
    SpuInfoDescEntity desc;

    //查询spu的规则参数属性
    List<SpuItemAttrGroupVo> groupAttrs;

    @Data
    public static class SpuItemAttrGroupVo{
        private String groupName;
        private List<Attr> attrs;
    }
}
