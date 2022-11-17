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
        //查到锁定订单的详情，说明这个数据库没有回滚；否则已经回滚，无需解锁
        if (byId != null) {
            //查询订单状态，分析异常情况
            WareOrderTaskEntity taskEntity = wareOrderTaskService.getById(to.getId());
            /**
             *  远程调用，R数据转换异常 --> 拦截器需要放行远程服务调用，否则数据会被拦截，跳转到登陆页面
             */
            R r = orderFeignServer.getOrderStatus(taskEntity.getOrderSn());
            if (r.getCode() == 0) {
                OrderVo orderVo = r.getData("data", new TypeReference<OrderVo>() {
                });
                //订单不存在或者被取消，并且库存是锁定状态时，需要解锁库存；否则无需解锁
                if (orderVo != null || orderVo.getStatus() == OrderStatusEnum.CANCLED.getCode()) {
                    if (byId.getLockStatus() == WareConstant.WareOrderTaskDetailStatusEnum.LOCKED.getCode()) {
                        unLockStock(detail.getId(), detail.getWareId(), detail.getSkuId(), detail.getSkuNum());
                    }
                }
            } else {
                //查询异常，抛异常
                throw new RuntimeException("远程order服务调用失败!");
            }
        }
    }

    @Override
    @Transactional
    public void unLockStock(OrderTo to) {
        //订单关闭后，监听到mq的消息，检查库存是否解锁
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
     * 根据`wms_ware_order_task_detail`信息解锁`wms_ware_sku`中对应的库存
     */
    private void unLockStock(Long detailId, Long wareId, Long skuId, Integer skuNum) {
        //修改锁定的库存数量`wms_ware_sku`
        this.baseMapper.unLockStock(wareId, skuId, skuNum);
        //更新工作单的锁定状态`wms_ware_order_task_detail`
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
     *
     * @return
     */
    @Override
    @Transactional
    public Boolean orderLockStock(OrderLockStockVo vo) {
        //wms_ware_order_task保存订单号
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
                    //查找库存成功，保存库存id
                    lock.setWareId(wareIds);
                    locks.add(lock);
                } else {
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
                if (result == 1) {
                    //成功则终止循环，wms_ware_order_task_detail保存锁定库存的详情
                    WareOrderTaskDetailEntity detailEntity = new WareOrderTaskDetailEntity();
                    detailEntity.setSkuId(lock.getSkuId());
                    detailEntity.setWareId(wareId);
                    detailEntity.setLockStatus(WareConstant.WareOrderTaskDetailStatusEnum.LOCKED.getCode());
                    detailEntity.setSkuNum(lock.getCount());
                    detailEntity.setTaskId(taskEntity.getId());
                    wareOrderTaskDetailService.save(detailEntity);
                    //向RabbitMQ发送消息，这个消息应该包含工作单的id和详情信息
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
                //有一个没锁成功，则抛异常结束循环
                throw new NoStockException(lock.getSkuId());
            }
        }
        //没有异常,方法才能执行到这里
        return true;
    }

}