package com.example.gulimall.ware.dao;

import com.example.gulimall.ware.entity.WareSkuEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 商品库存
 * 
 * @author Li
 * @email Li@163.com
 * @date 2022-08-18 15:53:01
 */
@Mapper
public interface WareSkuDao extends BaseMapper<WareSkuEntity> {

    void addStock(@Param("skuId") Long skuId, @Param("wareId") Long wareId, @Param("stock") Integer stock);

    Long getSkuStock(@Param("skuId") Long skuId);

    Integer lockSkuStock(@Param("wareId") Long wareId, @Param("skuId") Long skuId, @Param("count") Integer count);

    void unLockStock(@Param("wareId") Long wareId, @Param("skuId") Long skuId, @Param("skuNum") Integer skuNum);
}
