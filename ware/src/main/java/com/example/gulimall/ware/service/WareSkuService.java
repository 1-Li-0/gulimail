package com.example.gulimall.ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.common.to.OrderTo;
import com.example.common.to.SkuHasStockVo;
import com.example.common.to.mq.StockLockedTo;
import com.example.common.utils.PageUtils;
import com.example.gulimall.ware.entity.WareSkuEntity;
import com.example.gulimall.ware.vo.OrderLockStockVo;

import java.util.List;
import java.util.Map;

/**
 * 商品库存
 *
 * @author Li
 * @email Li@163.com
 * @date 2022-08-18 15:53:01
 */
public interface WareSkuService extends IService<WareSkuEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void addStock(Long skuId, Long wareId, Integer stock);

    List<SkuHasStockVo> getSkuHasStock(List<Long> skuIds);

    Boolean orderLockStock(OrderLockStockVo vo);

    void unLockStock(StockLockedTo to);

    void unLockStock(OrderTo to);
}

