package com.example.gulimall.product.service.impl;

import com.example.gulimall.product.entity.*;
import com.example.gulimall.product.service.*;
import com.example.gulimall.product.vo.Attr;
import com.example.gulimall.product.vo.BaseAttrs;
import com.example.gulimall.product.vo.Skus;
import com.example.gulimall.product.vo.SpuSaveVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.common.utils.PageUtils;
import com.example.common.utils.Query;

import com.example.gulimall.product.dao.SpuInfoDao;
import org.springframework.transaction.annotation.Transactional;


@Service("spuInfoService")
public class SpuInfoServiceImpl extends ServiceImpl<SpuInfoDao, SpuInfoEntity> implements SpuInfoService {
    @Autowired
    private SpuInfoDescService spuInfoDescService;
    @Autowired
    private SpuImagesService imagesService;
    @Autowired
    private AttrService attrService;
    @Autowired
    private ProductAttrValueService attrValueService;
    @Autowired
    private SkuInfoService skuInfoService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                new QueryWrapper<SpuInfoEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    @Transactional //数据量较多，保证事务的一致性
    public void saveSpuInfo(SpuSaveVo vo) {
        //保存spu基本信息pms_spu_info
        SpuInfoEntity spuInfoEntity = new SpuInfoEntity();
        BeanUtils.copyProperties(vo, spuInfoEntity);
        spuInfoEntity.setCreateTime(new Date());
        spuInfoEntity.setUpdateTime(new Date());
        this.saveBaseSpuInfo(spuInfoEntity);

        //保存spu描述信息pms_spu_info_desc
        String spuDescription = vo.getSpuDescription();
        SpuInfoDescEntity descEntity = new SpuInfoDescEntity();
        SpuInfoEntity infoEntity = this.baseMapper.selectOne(new QueryWrapper<SpuInfoEntity>().eq("create_time", spuInfoEntity.getCreateTime()).eq("update_time", spuInfoEntity.getUpdateTime()));
        descEntity.setSpuId(infoEntity.getId());
        descEntity.setDecript(String.join(",", spuDescription));
        spuInfoDescService.saveSpuInfoDesc(descEntity);

        //保存spu图片路径pms_spu_images
        List<String> images = vo.getImages();
        imagesService.saveImages(infoEntity.getId(), images);

        //保存spu基本属性的值pms_product_attr_value
        List<BaseAttrs> baseAttrs = vo.getBaseAttrs();
        List<ProductAttrValueEntity> collect = baseAttrs.stream().map(attr -> {
            ProductAttrValueEntity attrValueEntity = new ProductAttrValueEntity();
            attrValueEntity.setAttrValue(attr.getAttrValues());
            attrValueEntity.setAttrId(attr.getAttrId());
            attrValueEntity.setQuickShow(attr.getShowDesc());
            attrValueEntity.setSpuId(infoEntity.getId());
            AttrEntity attrEntity = attrService.getById(attr.getAttrId());
            attrValueEntity.setAttrName(attrEntity.getAttrName());
            return attrValueEntity;
        }).collect(Collectors.toList());
        attrValueService.saveProductAttrValue(collect);

        //保存spu积分sms_spu_bounds

        //提取sku所有信息，每个sku都是一个具体的商品，有各自的销售属性
        List<Skus> skus = vo.getSkus();
        if (skus != null && skus.size() > 0) {
            skus.forEach(sku -> {
                //保存sku基本信息pms_sku_info
                SkuInfoEntity skuInfoEntity = new SkuInfoEntity();
                BeanUtils.copyProperties(sku,skuInfoEntity);
                skuInfoEntity.setSpuId(infoEntity.getId());
                skuInfoEntity.setBrandId();
                //保存sku图片信息pms_sku_images
                //保存sku销售信息pms_sku_sale_attr_value
                //保存sku优惠sms_sku_ladder/满减信息sms_sku_full_reduction
            });
        }
    }

    @Override
    public void saveBaseSpuInfo(SpuInfoEntity spuInfoEntity) {
        this.baseMapper.insert(spuInfoEntity);
    }

}