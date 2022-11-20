package com.example.gulimall.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.common.to.MemberRespVo;
import com.example.common.utils.PageUtils;
import com.example.gulimall.order.entity.OrderEntity;
import com.example.gulimall.order.vo.*;

import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * 订单
 *
 * @author Li
 * @email Li@163.com
 * @date 2022-08-18 15:51:01
 */
public interface OrderService extends IService<OrderEntity> {

    PageUtils queryPage(Map<String, Object> params);

    //订单确认页需要的数据
    OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException;

    OrderSubmitRespVo submitOrder(OrderSubmitVo vo);

    OrderEntity getOrderByOrderSn(String orderSn);

    void closeOrder(OrderEntity entity);

    PayVo getOrderPayInfo(String orderSn);

    PageUtils queryPageWithItems(Map<String, Object> params);

    String handlePayResult(PayAsyncVo vo);
}

