package com.example.gulimall.product.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.example.common.constant.ProductConstant;
import com.example.common.to.SkuHasStockVo;
import com.example.common.to.SkuReductionTo;
import com.example.common.to.SpuBoundsTo;
import com.example.common.to.es.SkuEsModel;
import com.example.common.utils.R;
import com.example.gulimall.product.entity.*;
import com.example.gulimall.product.feign.CouponFeignService;
import com.example.gulimall.product.feign.SearchFeignService;
import com.example.gulimall.product.feign.WareFeignService;
import com.example.gulimall.product.service.*;
import com.example.gulimall.product.vo.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.common.utils.PageUtils;
import com.example.common.utils.Query;

import com.example.gulimall.product.dao.SpuInfoDao;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


@Service("spuInfoService")
public class SpuInfoServiceImpl extends ServiceImpl<SpuInfoDao, SpuInfoEntity> implements SpuInfoService {
    @Autowired
    private SpuInfoDescService spuInfoDescService;
    @Autowired
    private SpuImagesService spuImagesService;
    @Autowired
    private AttrService attrService;
    @Autowired
    private ProductAttrValueService attrValueService;
    @Autowired
    private SkuInfoService skuInfoService;
    @Autowired
    private SkuImagesService skuImagesService;
    @Autowired
    private SkuSaleAttrValueService skuSaleAttrValueService;
    @Autowired
    private CouponFeignService couponFeignService;
    @Autowired
    private WareFeignService wareFeignService;
    @Autowired
    private BrandService brandService;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private SearchFeignService searchFeignService;

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
        spuImagesService.saveImages(infoEntity.getId(), images);

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

        //提取sku所有信息，每个sku都是一个具体的商品，有各自的销售属性
        List<Skus> skus = vo.getSkus();
        if (skus != null && skus.size() > 0) {
            skus.forEach(sku -> {
                //保存sku基本信息pms_sku_info
                SkuInfoEntity skuInfoEntity = new SkuInfoEntity();
                BeanUtils.copyProperties(sku, skuInfoEntity);
                Long spuId = infoEntity.getId();
                skuInfoEntity.setSpuId(spuId);
                skuInfoEntity.setBrandId(infoEntity.getBrandId());
                skuInfoEntity.setCatalogId(infoEntity.getCatalogId());
                skuInfoEntity.setSaleCount(0L);
                //保存sku_default_img属性值
                String defaultImg = "";
                for (Images image : sku.getImages()) {
                    if (image.getDefaultImg() == 1) {
                        defaultImg = image.getImgUrl();
                    }
                }
                skuInfoEntity.setSkuDefaultImg(defaultImg);
                //保存skuInfoEntity
                skuInfoService.saveSkuInfo(skuInfoEntity);
                SkuInfoEntity skuInfo = skuInfoService.getOne(new QueryWrapper<SkuInfoEntity>().eq("spu_id", spuId).eq("sku_name", skuInfoEntity.getSkuName()).eq("brand_id", skuInfoEntity.getBrandId()).eq("catalog_id", skuInfoEntity.getCatalogId()));
                Long skuId = skuInfo.getSkuId();

                //保存sku图片信息pms_sku_images
                List<Images> skuImages = sku.getImages();
                if (skuImages != null && skuImages.size() > 0) {
                    List<SkuImagesEntity> skuImagesEntities = skuImages.stream().map(img -> {
                        SkuImagesEntity skuImagesEntity = new SkuImagesEntity();
                        skuImagesEntity.setSkuId(skuId);
                        skuImagesEntity.setImgUrl(img.getImgUrl());
                        skuImagesEntity.setDefaultImg(img.getDefaultImg());
                        return skuImagesEntity;
                    }).filter(skuImagesEntity -> !StringUtils.isEmpty(skuImagesEntity.getImgUrl())).collect(Collectors.toList());
                    skuImagesService.saveBatch(skuImagesEntities);
                }

                //保存sku销售信息pms_sku_sale_attr_value
                List<Attr> attr = sku.getAttr();
                if (attr != null && attr.size() > 0) {
                    List<SkuSaleAttrValueEntity> saleAttrValueEntities = attr.stream().map(a -> {
                        SkuSaleAttrValueEntity attrValueEntity = new SkuSaleAttrValueEntity();
                        BeanUtils.copyProperties(a, attrValueEntity);
                        attrValueEntity.setSkuId(skuId);
                        return attrValueEntity;
                    }).collect(Collectors.toList());
                    skuSaleAttrValueService.saveBatch(saleAttrValueEntities);
                }

                /**
                 * 使用其他服务，需要调用远程接口
                 */
                //保存spu积分sms_spu_bounds
                Bounds bounds = vo.getBounds();
                if (bounds != null) {
                    SpuBoundsTo spuBoundsTo = new SpuBoundsTo();
                    spuBoundsTo.setSpuId(spuId);
                    BeanUtils.copyProperties(bounds, spuBoundsTo);
                    R r1 = couponFeignService.saveSpuBounds(spuBoundsTo);
                    if (r1.getCode() != 0) {
                        log.error("远程保存spu积分信息失败");
                    }
                }

                //保存sku优惠sms_sku_ladder/满减信息sms_sku_full_reduction/会员价格sms_member_price
                SkuReductionTo skuReductionTo = new SkuReductionTo();
                skuReductionTo.setSkuId(skuId);
                BeanUtils.copyProperties(sku, skuReductionTo);
                if (skuReductionTo.getFullCount() <= 0 || skuReductionTo.getFullPrice().compareTo(new BigDecimal("0")) == 1) {
                    R r2 = couponFeignService.saveSkuReduction(skuReductionTo);
                    if (r2.getCode() != 0) {
                        log.error("远程保存sku优惠信息失败");
                    }
                }

            });
        }
    }

    @Override
    public void saveBaseSpuInfo(SpuInfoEntity spuInfoEntity) {
        this.baseMapper.insert(spuInfoEntity);
    }

    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {
        QueryWrapper<SpuInfoEntity> wrapper = new QueryWrapper<>();
        String key = (String) params.get("key");
        if (!StringUtils.isEmpty(key)) {
            wrapper.and((w) -> {
                w.eq("id", key).or().like("spu_name", key);
            });
        }
        String status = (String) params.get("status");
        if (!StringUtils.isEmpty(status)) {
            wrapper.eq("publish_status", status);
        }
        String brandId = (String) params.get("brandId");
        if (!StringUtils.isEmpty(brandId) && !"0".equalsIgnoreCase(brandId)) {
            wrapper.eq("brand_id", brandId);
        }
        String catelogId = (String) params.get("catelogId");
        if (!StringUtils.isEmpty(catelogId) && !"0".equalsIgnoreCase(catelogId)) {
            wrapper.eq("catalog_id", catelogId);
        }
        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                wrapper
        );
        return new PageUtils(page);
    }

    @Override
    public void up(Long spuId) {
        //查出可检索属性 (同一种商品的基本规格是一样的，只需要查询一次)
        List<ProductAttrValueEntity> attrValueEntities = attrValueService.queryAttrListForSpu(spuId);
        List<Long> attrIds = attrValueEntities.stream().map(entity -> entity.getAttrId()).collect(Collectors.toList());
        List<Long> attrEntities = attrService.selectSearchAttrIds(attrIds);
        Set<Long> idSet = new HashSet<>(attrEntities);
        List<SkuEsModel.Attrs> attrsList = attrValueEntities.stream()
                .filter(entity -> idSet.contains(entity.getAttrId()))
                .map(item -> {
                    SkuEsModel.Attrs attr = new SkuEsModel.Attrs();
                    BeanUtils.copyProperties(item, attr);
                    return attr;
                })
                .collect(Collectors.toList());

        //远程调用库存服务，一次性查出所有库存信息
        List<SkuInfoEntity> skus = skuInfoService.getSkusBySpuId(spuId);
        List<Long> skuIds = skus.stream().map(SkuInfoEntity::getSkuId).collect(Collectors.toList());
        Map<Long, Boolean> stockMap = null;
        try {
            R r = wareFeignService.getSkuHasStock(skuIds);
            stockMap = r.getData(new TypeReference<List<SkuHasStockVo>>(){}).stream().collect(Collectors.toMap(SkuHasStockVo::getSkuId, item -> item.getHasStock()));
        } catch (Exception e) {
            log.error("库存服务查询异常：原因{}", e);
        }

        //组装数据
        Map<Long, Boolean> finalStockMap = stockMap;
        List<SkuEsModel> upProducts = skus.stream()
                .map(sku -> {
                    SkuEsModel esModel = new SkuEsModel();
                    //属性设置
                    BeanUtils.copyProperties(sku, esModel);

                    esModel.setSkuPrice(sku.getPrice());
                    esModel.setSkuImg(sku.getSkuDefaultImg());

                    //设置库存信息
                    if (finalStockMap != null) {
                        esModel.setHasStock(finalStockMap.get(sku.getSkuId()));
                    } else {
                        esModel.setHasStock(true);
                    }

                    //热度评分
                    esModel.setHotScore(0L);
                    BrandEntity brand = brandService.getById(sku.getBrandId());
                    if (brand != null) {
                        esModel.setBrandName(brand.getName());
                        esModel.setBrandImg(brand.getLogo());
                    }
                    CategoryEntity category = categoryService.getById(sku.getCatalogId());
                    if (category != null) {
                        esModel.setCatalogName(category.getName());
                    }

                    //设置需要用来查询的属性（不需要检索的冗余属性不用设置）
                    esModel.setAttrs(attrsList);
                    return esModel;
                })
                .collect(Collectors.toList());

        //发送给ES保存
        R r = searchFeignService.productStatusUp(upProducts);
        if (r.getCode() == 0 ){
            //远程调用成功，修改spu状态publish_status
            baseMapper.updateSpuStatus(spuId, ProductConstant.StatusEnum.SPU_UP.getCode());
        }else {
            //远程调用失败
            //TODO 重复调用？接口幂等性；重试机制?
        }
    }

}