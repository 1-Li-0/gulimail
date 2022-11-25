package com.example.gulimall.product.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.example.common.utils.R;
import com.example.gulimall.product.entity.SkuImagesEntity;
import com.example.gulimall.product.entity.SpuInfoDescEntity;
import com.example.gulimall.product.feign.SeckillFeignServer;
import com.example.gulimall.product.service.*;
import com.example.gulimall.product.vo.SeckillInfoVo;
import com.example.gulimall.product.vo.SkuItemSaleAttrVo;
import com.example.gulimall.product.vo.SkuItemVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.common.utils.PageUtils;
import com.example.common.utils.Query;

import com.example.gulimall.product.dao.SkuInfoDao;
import com.example.gulimall.product.entity.SkuInfoEntity;
import org.springframework.util.StringUtils;


@Service("skuInfoService")
public class SkuInfoServiceImpl extends ServiceImpl<SkuInfoDao, SkuInfoEntity> implements SkuInfoService {
    @Autowired
    private SkuImagesService imagesService;
    @Autowired
    private SpuInfoDescService spuInfoDescService;
    @Autowired
    private AttrGroupService attrGroupService;
    @Autowired
    private SkuSaleAttrValueService skuSaleAttrValueService;
    @Autowired
    private ThreadPoolExecutor executor;
    @Autowired
    SeckillFeignServer seckillFeignServer;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SkuInfoEntity> page = this.page(
                new Query<SkuInfoEntity>().getPage(params),
                new QueryWrapper<SkuInfoEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void saveSkuInfo(SkuInfoEntity skuInfoEntity) {
        this.baseMapper.insert(skuInfoEntity);
    }

    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {
        QueryWrapper<SkuInfoEntity> wrapper = new QueryWrapper<>();
        String key = (String) params.get("key");
        if (!StringUtils.isEmpty(key)) {
            wrapper.and(w -> {
                w.eq("sku_id", key).or().like("sku_name", key);
            });
        }
        String brandId = (String) params.get("brandId");
        if (!StringUtils.isEmpty(brandId) && !"0".equalsIgnoreCase(brandId)) {
            wrapper.eq("brand_id", brandId);
        }
        String catelogId = (String) params.get("catelogId");
        if (!StringUtils.isEmpty(catelogId) && !"0".equalsIgnoreCase(catelogId)) {
            wrapper.eq("catalog_id", catelogId);
        }
        String min = (String) params.get("min");
        if (!StringUtils.isEmpty(min)) {
            wrapper.ge("price", min);
        }
        String max = (String) params.get("max");
        if (!StringUtils.isEmpty(max)) {
            try {
                BigDecimal decimal = new BigDecimal(max);
                if (decimal.compareTo(new BigDecimal("0")) == 1) {
                    wrapper.le("price", max);
                }
            } catch (Exception e) {

            }
        }
        IPage<SkuInfoEntity> page = this.page(
                new Query<SkuInfoEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

    @Override
    public List<SkuInfoEntity> getSkusBySpuId(Long spuId) {
        List<SkuInfoEntity> list = this.list(new QueryWrapper<SkuInfoEntity>().eq("spu_id", spuId));
        return list;
    }

    /**
     * 使用异步线程方式，进行多表查询
     */
    @Override
    public SkuItemVo item(Long skuId) throws ExecutionException, InterruptedException {
        SkuItemVo skuItemVo = new SkuItemVo();
        CompletableFuture<SkuInfoEntity> infoFuture = CompletableFuture.supplyAsync(() -> {
            //根据skuId查询基本信息
            SkuInfoEntity info = getById(skuId);
            skuItemVo.setInfo(info);
            return info;
        }, executor);

        CompletableFuture<Void> saleAttrFuture = infoFuture.thenAcceptAsync(res -> {
            //查询spu的销售属性
            List<SkuItemSaleAttrVo> saleAttrs = skuSaleAttrValueService.getSaleAttrsBySpuId(res.getSpuId());
            skuItemVo.setSaleAttr(saleAttrs);
        }, executor);
        CompletableFuture<Void> descFuture = infoFuture.thenAcceptAsync(res -> {
            //查询spu的介绍
            SpuInfoDescEntity spuInfoDesc = spuInfoDescService.getById(res.getSpuId());
            skuItemVo.setDesc(spuInfoDesc);
        }, executor);
        CompletableFuture<Void> baseAttrFuture = infoFuture.thenAcceptAsync(res -> {
            //查询spu的规则参数属性
            List<SkuItemVo.SpuItemAttrGroupVo> attrGroupVos = attrGroupService.getAttrGroupWithAttrsBySpuId(res.getSpuId(), res.getCatalogId());
            skuItemVo.setGroupAttrs(attrGroupVos);
        }, executor);

        CompletableFuture<Void> imagesFuture = CompletableFuture.runAsync(() -> {
            //查询sku默认图片的集合
            List<SkuImagesEntity> images = imagesService.getImagesBySkuId(skuId);
            skuItemVo.setImages(images);
        }, executor);

        //远程调用秒杀服务查询秒杀信息
        CompletableFuture<Void> seckillFuture = CompletableFuture.runAsync(() -> {
            R r = seckillFeignServer.getSkuSeckillInfo(skuId);
            if (r.getCode() == 0) {
                SeckillInfoVo redisTo = r.getData("data", new TypeReference<SeckillInfoVo>() {
                });
                if (redisTo != null) {
                    skuItemVo.setSeckillInfoVo(redisTo);
                }
            }
        }, executor);

        //等待线程全部完成
        CompletableFuture.allOf(saleAttrFuture, descFuture, baseAttrFuture, imagesFuture, seckillFuture).get();
        return skuItemVo;
    }

}