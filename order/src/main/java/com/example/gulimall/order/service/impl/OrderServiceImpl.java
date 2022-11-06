package com.example.gulimall.order.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.example.common.constant.OrderConstant;
import com.example.common.to.MemberRespVo;
import com.example.common.to.SkuHasStockVo;
import com.example.common.utils.R;
import com.example.gulimall.order.feign.CartFeignServer;
import com.example.gulimall.order.feign.MemberFeignServer;
import com.example.gulimall.order.feign.WareFeignServer;
import com.example.gulimall.order.interceptor.LoginUserInterceptor;
import com.example.gulimall.order.vo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.common.utils.PageUtils;
import com.example.common.utils.Query;

import com.example.gulimall.order.dao.OrderDao;
import com.example.gulimall.order.entity.OrderEntity;
import com.example.gulimall.order.service.OrderService;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {
    @Autowired
    private CartFeignServer cartFeignServer;
    @Autowired
    private MemberFeignServer memberFeignServer;
    @Autowired
    private ThreadPoolExecutor executor;
    @Autowired
    WareFeignServer wareFeignServer;
    @Autowired
    StringRedisTemplate redisTemplate;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException {
        OrderConfirmVo confirmVo = new OrderConfirmVo();
        MemberRespVo memberRespVo = LoginUserInterceptor.loginUser.get();
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        //选择支付的购物项
        CompletableFuture<Void> getItemFuture = CompletableFuture.runAsync(() -> {
            //异步线程请求无法获取原请求的ThreadLocal，需要重新设置后再远程调用
            RequestContextHolder.setRequestAttributes(attributes);
            List<OrderItemVo> orderItems = cartFeignServer.getOrderItems();
            confirmVo.setItems(orderItems);
            RequestContextHolder.resetRequestAttributes(); //清空RequestContextHolder
        }, executor).thenRunAsync(() -> {
            List<OrderItemVo> items = confirmVo.getItems();
            if (items != null && items.size() > 0) {
                List<Long> skuIds = items.stream().map(orderItemVo -> orderItemVo.getSkuId()).collect(Collectors.toList());
                R r = wareFeignServer.getSkuHasStock(skuIds);
                List<SkuHasStockVo> hasStockVos = r.getData("data", new TypeReference<List<SkuHasStockVo>>() {
                });
                if (hasStockVos != null) {
                    Map<Long, Boolean> map = hasStockVos.stream().collect(Collectors.toMap(SkuHasStockVo::getSkuId, SkuHasStockVo::getHasStock));
                    confirmVo.setHasStock(map);
                }
            }
        }, executor);

        //用户地址
        CompletableFuture<Void> getAddressFuture = CompletableFuture.runAsync(() -> {
            //异步线程请求无法获取原请求的ThreadLocal，需要重新设置后再远程调用
            RequestContextHolder.setRequestAttributes(attributes);
            List<MemberAddressVo> memberAddress = memberFeignServer.getMemberAddress(memberRespVo.getId());
            confirmVo.setMemberAddressList(memberAddress);
            RequestContextHolder.resetRequestAttributes(); //清空RequestContextHolder
        }, executor);

        //积分信息
        confirmVo.setIntegration(memberRespVo.getIntegration());
        //生成令牌，返回数据同时保存在redis
        String s = UUID.randomUUID().toString().replace("-", "");
        confirmVo.setOrderToken(s);
        redisTemplate.opsForValue().set(OrderConstant.ORDER_REDIS_TOKEN_PREFIX + memberRespVo.getId().toString(), s, 30, TimeUnit.MINUTES);

        CompletableFuture.allOf(getItemFuture, getAddressFuture).get();
        return confirmVo;
    }

    @Override
    public OrderSubmitRespVo submitOrder(OrderSubmitVo vo) {
        OrderSubmitRespVo respVo = new OrderSubmitRespVo();
        //校验token【token令牌的对比和删除必须保证原子性】
        MemberRespVo memberRespVo = LoginUserInterceptor.loginUser.get();
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        Long result = redisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class), Arrays.asList(OrderConstant.ORDER_REDIS_TOKEN_PREFIX + memberRespVo.getId()), vo.getOrderToken());
        if (result == 1) {
            //对比删除操作成功
            respVo.setCode(0);
            //生成订单
            OrderEntity order = new OrderEntity();
            respVo.setOrder(order);
        }
        return respVo;
    }

}