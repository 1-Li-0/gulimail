package com.example.gulimall.order.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.example.common.constant.OrderConstant;
import com.example.common.constant.OrderStatusEnum;
import com.example.common.to.MemberRespVo;
import com.example.common.to.SkuHasStockVo;
import com.example.common.utils.R;
import com.example.gulimall.order.dao.OrderItemDao;
import com.example.gulimall.order.entity.OrderItemEntity;
import com.example.gulimall.order.feign.CartFeignServer;
import com.example.gulimall.order.feign.MemberFeignServer;
import com.example.gulimall.order.feign.ProductFeignServer;
import com.example.gulimall.order.feign.WareFeignServer;
import com.example.gulimall.order.interceptor.LoginUserInterceptor;
import com.example.gulimall.order.service.OrderItemService;
import com.example.gulimall.order.vo.*;
import io.seata.spring.annotation.GlobalTransactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {

    ThreadLocal<OrderSubmitVo> submitVoThreadLocal = new ThreadLocal<>();

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
    @Autowired
    ProductFeignServer productFeignServer;
    @Autowired
    OrderItemService orderItemService;

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
        //选择支付的购物项【重新从购物车获取数据】
        CompletableFuture<Void> getItemFuture = CompletableFuture.runAsync(() -> {
            //异步线程请求无法获取原请求的ThreadLocal，需要重新设置后再远程调用
            RequestContextHolder.setRequestAttributes(attributes);
            List<OrderItemVo> orderItems = cartFeignServer.getCurrentUserItems();
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


    /** @GlobalTransactional 分布式事务不适合高并发，事务管理用了锁机制---串行化，效率低
     *  高并发可以使用RabbitMQ的“延时队列”机制
     */
    @Override
    @Transactional
    public OrderSubmitRespVo submitOrder(OrderSubmitVo vo) {
        submitVoThreadLocal.set(vo); //后面的线程需要用到
        OrderSubmitRespVo respVo = new OrderSubmitRespVo();
        respVo.setCode(0);
        //校验token【token令牌的对比和删除必须保证原子性---lua脚本】
        MemberRespVo memberRespVo = LoginUserInterceptor.loginUser.get();
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        Long result = redisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class), Arrays.asList(OrderConstant.ORDER_REDIS_TOKEN_PREFIX + memberRespVo.getId()), vo.getOrderToken());
        if (result == 1) {
            //对比删除操作成功，生成订单
            OrderCreateTo order = createOrder();
            if (order.getOrder().getPayAmount().subtract(vo.getPayPrice()).abs().doubleValue() < 0.01) {
                //验价成功，保存到数据库
                saveOrder(order);
                //锁定库存，失败时回滚全部操作【事务】
                OrderLockStockVo stockVo = new OrderLockStockVo();
                stockVo.setOrderSn(order.getOrder().getOrderSn());
                List<OrderItemVo> itemVoList = order.getOrderItems().stream().map(item -> {
                    OrderItemVo itemVo = new OrderItemVo();
                    itemVo.setSkuId(item.getSkuId());
                    itemVo.setCount(item.getSkuQuantity());
                    return itemVo;
                }).collect(Collectors.toList());
                stockVo.setOrderItems(itemVoList);
                R r = wareFeignServer.orderLockStock(stockVo);
                if (r.getCode() == 0) {
                    //成功，设置返回的对象信息
                    respVo.setOrder(order.getOrder());
                } else {
                    //锁库存异常
                    respVo.setCode(r.getCode());
                }
            } else {
                //验价失败
                respVo.setCode(2);
            }
        } else {
            //token失效
            respVo.setCode(1);
        }
        return respVo;
    }

    /**
     * 保存订单数据
     *
     * @param order
     */
    private void saveOrder(OrderCreateTo order) {
        OrderEntity orderEntity = order.getOrder();
        orderEntity.setCreateTime(new Date());
        this.save(orderEntity);
        List<OrderItemEntity> orderItems = order.getOrderItems();
        for (OrderItemEntity orderItem : orderItems) {
            orderItemService.save(orderItem);
        }
    }

    /**
     * 构建订单
     *
     * @return
     */
    private OrderCreateTo createOrder() {
        OrderCreateTo createTo = new OrderCreateTo();
        String orderSn = IdWorker.getTimeId();
        //封装购物项
        List<OrderItemEntity> orderItems = buildOrderItems(orderSn);
        createTo.setOrderItems(orderItems);
        //封装订单
        OrderEntity order = buildOrder(orderSn);
        MemberRespVo member = LoginUserInterceptor.loginUser.get();
        order.setMemberId(member.getId());
        order.setMemberUsername(member.getUsername());
        //封装价格和积分
        computePrice(orderItems, order);
        createTo.setOrder(order);
        return createTo;
    }

    private void computePrice(List<OrderItemEntity> orderItems, OrderEntity order) {
        BigDecimal total = new BigDecimal("0");
        BigDecimal couponAmount = new BigDecimal("0");
        BigDecimal integrationAmount = new BigDecimal("0");
        BigDecimal promotionAmount = new BigDecimal("0");
        Integer giftIntegration = 0;
        Integer giftGrowth = 0;
        //叠加订单项的所有金额
        for (OrderItemEntity entity : orderItems) {
            couponAmount = couponAmount.add(entity.getCouponAmount());
            integrationAmount = integrationAmount.add(entity.getIntegrationAmount());
            promotionAmount = promotionAmount.add(entity.getPromotionAmount());
            total = total.add(entity.getRealAmount());
            giftIntegration += entity.getGiftIntegration();
            giftGrowth += entity.getGiftGrowth();
        }
        order.setTotalAmount(total); //订单总额
        order.setPayAmount(total.add(order.getFreightAmount())); //应付总额
        //优惠
        order.setPromotionAmount(promotionAmount);
        order.setIntegrationAmount(integrationAmount);
        order.setCouponAmount(couponAmount);
        //积分和成长值
        order.setIntegration(giftIntegration);
        order.setGrowth(giftGrowth);
        order.setDeleteStatus(0); //0代表未删除
    }

    /**
     * 从购物车获取购物项集合
     */
    private List<OrderItemEntity> buildOrderItems(String orderSn) {
        List<OrderItemEntity> orderItems = new ArrayList<>();
        List<OrderItemVo> currentUserCartItems = cartFeignServer.getCurrentUserItems();
        if (currentUserCartItems != null && currentUserCartItems.size() > 0) {
            orderItems = currentUserCartItems.stream().map(cartItemVo -> {
                OrderItemEntity entity = buildOrderItem(cartItemVo);
                entity.setOrderSn(orderSn);
                return entity;
            }).collect(Collectors.toList());
        }
        return orderItems;
    }

    //构建某一个订单项
    private OrderItemEntity buildOrderItem(OrderItemVo cartItem) {
        OrderItemEntity entity = new OrderItemEntity();
        //封装SKU信息
        Long skuId = cartItem.getSkuId();
        entity.setSkuId(skuId);
        entity.setSkuName(cartItem.getTitle());
        entity.setSkuPic(cartItem.getImage());
        entity.setSkuPrice(cartItem.getPrice());
        entity.setSkuQuantity(cartItem.getCount());
        String skuAttr = StringUtils.collectionToDelimitedString(cartItem.getSkuAttr(), ";");
        entity.setSkuAttrsVals(skuAttr);
        //封装SPU信息【通过skuId查询spu信息】
        R r = productFeignServer.getSpuInfoBySkuId(skuId);
        SpuInfoVo spuInfo = r.getData("data", new TypeReference<SpuInfoVo>() {
        });
        entity.setCategoryId(spuInfo.getCatalogId());
        entity.setSpuId(spuInfo.getId());
        entity.setSpuName(spuInfo.getSpuName());
        //品牌
        entity.setSpuBrand(spuInfo.getBrandId().toString());
        //积分和成长值
        Integer gift = cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount().toString())).intValue();
        entity.setGiftGrowth(gift);
        entity.setGiftIntegration(gift);
        //订单项的价格信息【优惠没有做】
        entity.setPromotionAmount(new BigDecimal("0"));
        entity.setCouponAmount(new BigDecimal("0"));
        entity.setIntegrationAmount(new BigDecimal("0"));
        BigDecimal origin = cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount().toString()));
        BigDecimal realPrice = origin.subtract(entity.getPromotionAmount())
                .subtract(entity.getIntegrationAmount())
                .subtract(entity.getCouponAmount());
        entity.setRealAmount(realPrice);
        return entity;
    }

    private OrderEntity buildOrder(String timeId) {
        //创建订单
        OrderEntity order = new OrderEntity();
        order.setOrderSn(timeId);
        //获取收货地址，计算运费
        R r = wareFeignServer.getFare(submitVoThreadLocal.get().getAddrId());
        FareVo fareVo = r.getData("data", new TypeReference<FareVo>() {
        });
        MemberAddressVo address = fareVo.getAddress();
        order.setFreightAmount(fareVo.getFarePrice());
        order.setReceiverCity(address.getCity());
        order.setReceiverName(address.getName());
        order.setReceiverDetailAddress(address.getDetailAddress());
        order.setReceiverPhone(address.getPhone());
        order.setReceiverProvince(address.getProvince());
        order.setReceiverPostCode(address.getPostCode());
        order.setReceiverRegion(address.getRegion());
        //修改订单的状态为“未付款”
        order.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        return order;
    }

}