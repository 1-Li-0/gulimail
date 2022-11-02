package com.example.gulimall.order.service.impl;

import com.example.common.to.MemberRespVo;
import com.example.gulimall.order.feign.CartFeignServer;
import com.example.gulimall.order.feign.MemberFeignServer;
import com.example.gulimall.order.interceptor.LoginUserInterceptor;
import com.example.gulimall.order.vo.MemberAddressVo;
import com.example.gulimall.order.vo.OrderConfirmVo;
import com.example.gulimall.order.vo.OrderItemVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.common.utils.PageUtils;
import com.example.common.utils.Query;

import com.example.gulimall.order.dao.OrderDao;
import com.example.gulimall.order.entity.OrderEntity;
import com.example.gulimall.order.service.OrderService;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {
    @Autowired
    private CartFeignServer cartFeignServer;
    @Autowired
    private MemberFeignServer memberFeignServer;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public OrderConfirmVo confirmOrder() {
        OrderConfirmVo confirmVo = new OrderConfirmVo();
        //选择支付的购物项
        List<OrderItemVo> orderItems = cartFeignServer.getOrderItems();
        confirmVo.setItems(orderItems);
        //用户地址
        MemberRespVo memberRespVo = LoginUserInterceptor.loginUser.get();
        List<MemberAddressVo> memberAddress = memberFeignServer.getMemberAddress(memberRespVo.getId());
        confirmVo.setMemberAddressList(memberAddress);
        //积分信息
        confirmVo.setIntegration(memberRespVo.getIntegration());
        return null;
    }

}