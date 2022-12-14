package com.example.gulimall.ware.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.example.common.constant.OrderConstant;
import com.example.common.constant.OrderStatusEnum;
import com.example.common.constant.WareConstant;
import com.example.common.to.OrderTo;
import com.example.common.to.SkuHasStockVo;
import com.example.common.to.mq.StockDetailTo;
import com.example.common.to.mq.StockLockedTo;
import com.example.common.utils.R;
import com.example.gulimall.ware.entity.WareOrderTaskDetailEntity;
import com.example.gulimall.ware.entity.WareOrderTaskEntity;
import com.example.gulimall.ware.exception.NoStockException;
import com.example.gulimall.ware.feign.OrderFeignServer;
import com.example.gulimall.ware.feign.ProductFeignService;
import com.example.gulimall.ware.service.WareOrderTaskDetailService;
import com.example.gulimall.ware.service.WareOrderTaskService;
import com.example.gulimall.ware.vo.OrderItemVo;
import com.example.gulimall.ware.vo.OrderLockStockVo;
import com.example.gulimall.ware.vo.OrderVo;
import com.example.gulimall.ware.vo.SkuWareHasStock;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
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
    @Autowired
    private WareOrderTaskService wareOrderTaskService;
    @Autowired
    private WareOrderTaskDetailService wareOrderTaskDetailService;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private OrderFeignServer orderFeignServer;

    @Override
    @Transactional
    public void unLockStock(StockLockedTo to) {
        StockDetailTo detail = to.getDetail();
        WareOrderTaskDetailEntity byId = wareOrderTaskDetailService.getById(detail.getId());
        //???????????????????????????????????????????????????????????????????????????????????????????????????
        if (byId != null) {
            //???????????????????????????????????????
            WareOrderTaskEntity taskEntity = wareOrderTaskService.getById(to.getId());
            /**
             *  ???????????????R?????????????????? --> ??????????????????????????????????????????????????????????????????????????????????????????
             */
            R r = orderFeignServer.getOrderStatus(taskEntity.getOrderSn());
            if (r.getCode() == 0) {
                OrderVo orderVo = r.getData("data", new TypeReference<OrderVo>() {
                });
                //?????????????????????????????????????????????????????????????????????????????????????????????????????????
                if (orderVo != null || orderVo.getStatus() == OrderStatusEnum.CANCLED.getCode()) {
                    if (byId.getLockStatus() == WareConstant.WareOrderTaskDetailStatusEnum.LOCKED.getCode()) {
                        unLockStock(detail.getId(), detail.getWareId(), detail.getSkuId(), detail.getSkuNum());
                    }
                }
            } else {
                //????????????????????????
                throw new RuntimeException("??????order??????????????????!");
            }
        }
    }

    @Override
    @Transactional
    public void unLockStock(OrderTo to) {
        //???????????????????????????mq????????????????????????????????????
        String orderSn = to.getOrderSn();
        WareOrderTaskEntity task = wareOrderTaskService.getTaskByOrderSn(orderSn);
        if (task != null) {
            List<WareOrderTaskDetailEntity> detailEntities = wareOrderTaskDetailService.list(new QueryWrapper<WareOrderTaskDetailEntity>()
                    .eq("task_id", task.getId())
                    .eq("lock_status", WareConstant.WareOrderTaskDetailStatusEnum.LOCKED.getCode()));
            for (WareOrderTaskDetailEntity detail : detailEntities) {
                unLockStock(detail.getId(), detail.getWareId(), detail.getSkuId(), detail.getSkuNum());
            }
        }
    }

    /**
     * ??????`wms_ware_order_task_detail`????????????`wms_ware_sku`??????????????????
     */
    private void unLockStock(Long detailId, Long wareId, Long skuId, Integer skuNum) {
        //???????????????????????????`wms_ware_sku`
        this.baseMapper.unLockStock(wareId, skuId, skuNum);
        //??????????????????????????????`wms_ware_order_task_detail`
        WareOrderTaskDetailEntity entity = new WareOrderTaskDetailEntity();
        entity.setId(detailId);
        entity.setLockStatus(WareConstant.WareOrderTaskDetailStatusEnum.OPEN_LOCK.getCode());
        wareOrderTaskDetailService.updateById(entity);
    }

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
            //???????????????????????????????????????
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
     * ?????????????????????wareId???????????????????????????????????????????????????????????????false
     * ?????????????????????????????????????????????????????????????????????
     * (rollbackFor = NoStockException.class) ???????????????????????????????????????
     *
     * @return
     */
    @Override
    @Transactional
    public Boolean orderLockStock(OrderLockStockVo vo) {
        //wms_ware_order_task???????????????
        WareOrderTaskEntity taskEntity = new WareOrderTaskEntity();
        taskEntity.setOrderSn(vo.getOrderSn());
        wareOrderTaskService.save(taskEntity);

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
                    //?????????????????????????????????id
                    lock.setWareId(wareIds);
                    locks.add(lock);
                } else {
                    //?????????????????????
                    throw new NoStockException(lock.getSkuId());
                }
            }
        }
        //???????????????????????????
        for (SkuWareHasStock lock : locks) {
            //???????????????????????????
            Boolean skuStock = false;
            for (Long wareId : lock.getWareId()) {
                Integer result = this.baseMapper.lockSkuStock(wareId, lock.getSkuId(), lock.getCount());
                if (result == 1) {
                    //????????????????????????wms_ware_order_task_detail???????????????????????????
                    WareOrderTaskDetailEntity detailEntity = new WareOrderTaskDetailEntity();
                    detailEntity.setSkuId(lock.getSkuId());
                    detailEntity.setWareId(wareId);
                    detailEntity.setLockStatus(WareConstant.WareOrderTaskDetailStatusEnum.LOCKED.getCode());
                    detailEntity.setSkuNum(lock.getCount());
                    detailEntity.setTaskId(taskEntity.getId());
                    wareOrderTaskDetailService.save(detailEntity);
                    //???RabbitMQ???????????????????????????????????????????????????id???????????????
                    StockLockedTo lockedTo = new StockLockedTo();
                    lockedTo.setId(taskEntity.getId());
                    StockDetailTo detail = new StockDetailTo();
                    BeanUtils.copyProperties(detailEntity, detail);
                    lockedTo.setDetail(detail);
                    rabbitTemplate.convertAndSend("stock-event-exchange", "stock.locked", lockedTo);
                    skuStock = true;
                    break;
                }
            }
            if (!skuStock) {
                //????????????????????????????????????????????????
                throw new NoStockException(lock.getSkuId());
            }
        }
        //????????????,???????????????????????????
        return true;
    }

}