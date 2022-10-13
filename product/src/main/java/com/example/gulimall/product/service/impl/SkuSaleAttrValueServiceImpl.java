package com.example.gulimall.product.service.impl;

import com.example.gulimall.product.vo.SkuItemSaleAttrVo;
import com.example.gulimall.product.vo.SkuItemVo;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.common.utils.PageUtils;
import com.example.common.utils.Query;

import com.example.gulimall.product.dao.SkuSaleAttrValueDao;
import com.example.gulimall.product.entity.SkuSaleAttrValueEntity;
import com.example.gulimall.product.service.SkuSaleAttrValueService;


@Service("skuSaleAttrValueService")
public class SkuSaleAttrValueServiceImpl extends ServiceImpl<SkuSaleAttrValueDao, SkuSaleAttrValueEntity> implements SkuSaleAttrValueService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SkuSaleAttrValueEntity> page = this.page(
                new Query<SkuSaleAttrValueEntity>().getPage(params),
                new QueryWrapper<SkuSaleAttrValueEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<SkuItemSaleAttrVo> getSaleAttrsBySpuId(Long spuId) {
        List<SkuItemSaleAttrVo> saleAttrsBySpuId = this.baseMapper.getSaleAttrsBySpuId(spuId);
        return saleAttrsBySpuId;
    }

}