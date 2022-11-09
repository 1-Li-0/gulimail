package com.example.gulimall.ware.service.impl;

import com.example.common.to.SkuHasStockVo;
import com.example.common.utils.R;
import com.example.gulimall.ware.exception.NoStockException;
import com.example.gulimall.ware.feign.ProductFeignService;
import com.example.gulimall.ware.vo.OrderItemVo;
import com.example.gulimall.ware.vo.OrderLockStockVo;
import com.example.gulimall.ware.vo.SkuWareHasStock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.common.utils.PageUtils;
import com.example.common.utils.Query;

import com.example.gulimall.ware.dao.WareSkuDao;
import com.example.gulimall.ware.entity.WareSkuEntity;
import com.example.gulimall.ware.service.WareSkuService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {
    @Autowired
    private ProductFeignService productFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        QueryWrapper<WareSkuEntity> wrapper = new QueryWrapper<>();
        String skuId = (String) params.get("skuId");
        if (!StringUtils.isEmpty(skuId)) {
            wrapper.eq("sku_id", skuId);
        }
        String wareId = (String) params.get("wareId");
        if (!StringUtils.isEmpty(wareId)) {
            wrapper.eq("ware_id", wareId);
        }
        IPage<WareSkuEntity> page = this.page(
                new Query<WareSkuEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

    @Override
    public void addStock(Long skuId, Long wareId, Integer stock) {
        List<WareSkuEntity> entities = this.baseMapper.selectList(new QueryWrapper<WareSkuEntity>().eq("sku_id", skuId).eq("ware_id", wareId));
        if (entities == null || entities.size() == 0) {
            WareSkuEntity wareSkuEntity = new WareSkuEntity();
            wareSkuEntity.setSkuId(skuId);
            wareSkuEntity.setWareId(wareId);
            wareSkuEntity.setStock(stock);
            //此处失败，整个事务无需回滚
            try {
                R info = productFeignService.info(skuId);
                if (info.getCode() == 0) {
                    Map<String, Object> skuInfo = (Map<String, Object>) info.get("skuInfo");
                    wareSkuEntity.setSkuName((String) skuInfo.get("skuName"));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            this.baseMapper.insert(wareSkuEntity);
        } else {
            this.baseMapper.addStock(skuId, wareId, stock);
        }
    }

    @Override
    public List<SkuHasStockVo> getSkuHasStock(List<Long> skuIds) {
        List<SkuHasStockVo> collect = skuIds.stream()
                .map(skuId -> {
                    SkuHasStockVo vo = new SkuHasStockVo();
                    vo.setSkuId(skuId);
                    Long count = baseMapper.getSkuStock(skuId);
                    vo.setHasStock(count != null && count > 0);
                    return vo;
                })
                .collect(Collectors.toList());
        return collect;
    }

    /**
     * 查出库存足够的wareId，依次将商品锁定库存，只要有一个失败就返回false
     * 抛异常，需要回滚事务，将已经锁定库存的操作取消
     * (rollbackFor = NoStockException.class) 默认“运行时异常”都会回滚
     * @return
     */
    @Override
    @Transactional
    public Boolean orderLockStock(OrderLockStockVo vo) {
        List<OrderItemVo> orderItems = vo.getOrderItems();
        List<SkuWareHasStock> locks = new ArrayList<>();
        for (OrderItemVo orderItem : orderItems) {
            SkuWareHasStock lock = new SkuWareHasStock();
            Long skuId = orderItem.getSkuId();
            lock.setSkuId(skuId);
            lock.setCount(orderItem.getCount());
            List<WareSkuEntity> entities = this.baseMapper.selectList(new QueryWrapper<WareSkuEntity>().eq("sku_id", skuId));
            if (entities != null && entities.size() > 0) {
                List<Long> wareIds = entities.stream().filter(wareSkuEntity -> wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > orderItem.getCount()).map(wareSkuEntity -> wareSkuEntity.getWareId()).collect(Collectors.toList());
                if (wareIds != null && wareIds.size() > 0) {
                    //查找库存成功，保存库存id
                    lock.setWareId(wareIds);
                    locks.add(lock);
                }else {
                    //抛异常结束循环
                    throw new NoStockException(lock.getSkuId());
                }
            }
        }
        //没有异常则锁定库存
        for (SkuWareHasStock lock : locks) {
            //循环遍历尝试锁库存
            Boolean skuStock = false;
            for (Long wareId : lock.getWareId()) {
                Integer result = this.baseMapper.lockSkuStock(wareId, lock.getSkuId(), lock.getCount());
                if (result == 1){
                    //成功则终止循环
                    skuStock = true;
                    break;
                }
            }
            if (!skuStock){
                //有一个没锁成功，则抛异常结束循环
                throw new NoStockException(lock.getSkuId());
            }
        }
        //没有异常,方法才能执行到这里
        return true;
    }
}