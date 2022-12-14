package com.example.gulimall.order.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.example.common.constant.OrderConstant;
import com.example.common.constant.OrderStatusEnum;
import com.example.common.to.MemberRespVo;
import com.example.common.to.OrderTo;
import com.example.common.to.SkuHasStockVo;
import com.example.common.to.mq.SeckillOrderTo;
import com.example.common.utils.R;
import com.example.gulimall.order.dao.OrderItemDao;
import com.example.gulimall.order.entity.MqMessageEntity;
import com.example.gulimall.order.entity.OrderItemEntity;
import com.example.gulimall.order.entity.PaymentInfoEntity;
import com.example.gulimall.order.feign.CartFeignServer;
import com.example.gulimall.order.feign.MemberFeignServer;
import com.example.gulimall.order.feign.ProductFeignServer;
import com.example.gulimall.order.feign.WareFeignServer;
import com.example.gulimall.order.interceptor.LoginUserInterceptor;
import com.example.gulimall.order.service.OrderItemService;
import com.example.gulimall.order.service.PaymentInfoService;
import com.example.gulimall.order.vo.*;
import io.seata.spring.annotation.GlobalTransactional;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    @Autowired
    RabbitTemplate rabbitTemplate;
    @Autowired
    PaymentInfoService paymentInfoService;

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
        //?????????????????????????????????
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
//        RequestAttributes attributes = RequestContextHolder.currentRequestAttributes();
        //????????????????????????????????????????????????????????????
        CompletableFuture<Void> getItemFuture = CompletableFuture.runAsync(() -> {
            //??????????????????????????????????????????ThreadLocal???????????????????????????????????????
            RequestContextHolder.setRequestAttributes(attributes);
            List<OrderItemVo> orderItems = cartFeignServer.getCurrentUserItems();
            confirmVo.setItems(orderItems);
            RequestContextHolder.resetRequestAttributes(); //??????RequestContextHolder
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

        //????????????
        CompletableFuture<Void> getAddressFuture = CompletableFuture.runAsync(() -> {
            //??????????????????????????????????????????ThreadLocal???????????????????????????????????????
            RequestContextHolder.setRequestAttributes(attributes);
            List<MemberAddressVo> memberAddress = memberFeignServer.getMemberAddress(memberRespVo.getId());
            confirmVo.setMemberAddressList(memberAddress);
            RequestContextHolder.resetRequestAttributes(); //??????RequestContextHolder
        }, executor);

        //????????????
        confirmVo.setIntegration(memberRespVo.getIntegration());
        //??????????????????????????????????????????redis
        String s = UUID.randomUUID().toString().replace("-", "");
        confirmVo.setOrderToken(s);
        redisTemplate.opsForValue().set(OrderConstant.ORDER_REDIS_TOKEN_PREFIX + memberRespVo.getId().toString(), s, 30, TimeUnit.MINUTES);

        CompletableFuture.allOf(getItemFuture, getAddressFuture).get();
        return confirmVo;
    }


    /**
     * @GlobalTransactional ???????????????????????????????????????????????????????????????---?????????????????????
     * ?????????????????????RabbitMQ???????????????????????????
     */
    @Override
    @Transactional
    public OrderSubmitRespVo submitOrder(OrderSubmitVo vo) {
        submitVoThreadLocal.set(vo); //???????????????????????????
        OrderSubmitRespVo respVo = new OrderSubmitRespVo();
        respVo.setCode(0);
        //??????token???token?????????????????????????????????????????????---lua?????????
        MemberRespVo memberRespVo = LoginUserInterceptor.loginUser.get();
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        Long result = redisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class), Arrays.asList(OrderConstant.ORDER_REDIS_TOKEN_PREFIX + memberRespVo.getId()), vo.getOrderToken());
        if (result == 1) {
            //???????????????????????????????????????
            OrderCreateTo order = createOrder();
            //??????
            if (order.getOrder().getPayAmount().subtract(vo.getPayPrice()).abs().doubleValue() < 0.01) {
                //?????????????????????????????????
                saveOrder(order);
                //??????????????????????????????????????????????????????
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
                    //????????????????????????????????????????????????
                    respVo.setOrder(order.getOrder());
                    //???RabbitMQ????????????
                    rabbitTemplate.convertAndSend("order-event-exchange", "order.create.order", order.getOrder());
                } else {
                    //???????????????
                    respVo.setCode(r.getCode());
                }
            } else {
                //????????????
                respVo.setCode(2);
            }
        } else {
            //redis????????????
            respVo.setCode(1);
        }
        return respVo;
    }

    @Override
    public OrderEntity getOrderByOrderSn(String orderSn) {
        return this.getOne(new QueryWrapper<OrderEntity>().eq("order_sn", orderSn));
    }

    /**
     * ????????????(??????)
     */
    @Override
    public void closeOrder(OrderEntity entity) {
        //?????????????????????????????????????????????????????????
        OrderEntity orderEntity = this.getById(entity.getId());
        if (orderEntity.getStatus() == OrderStatusEnum.CREATE_NEW.getCode()) {
            OrderEntity update = new OrderEntity();
            update.setId(entity.getId());
            update.setStatus(OrderStatusEnum.CANCLED.getCode());
            this.updateById(update);
            //??????MQ????????????
            OrderTo orderTo = new OrderTo();
            BeanUtils.copyProperties(orderEntity, orderTo);
            //TODO ????????????????????????/????????????????????????????????????/????????????
            CorrelationData correlationData = new CorrelationData(orderTo.getOrderSn());
            String exchange = "order-event-exchange";
            String routingKey = "order.release.other";
            /** ???????????????????????????service???dao
             * ???correlationData.getId()????????????id???orderTo??????JSON?????????????????????????????????????????????????????????????????????0-????????????????????????????????????
             * MqMessageEntity messageEntity = new MqMessageEntity();
             * messageEntity.setMessageId(correlationData.getId());
             * messageEntity.setMessageStatus(0);
             * messageEntity.setContent(JSON.toJSONString(orderTo));
             * messageEntity.setClassType(orderTo.getClass().toString());
             * messageEntity.setToExchane(exchange);
             * messageEntity.setRoutingKey(routingKey);
             * Date date = new Date();
             * messageEntity.setCreateTime(date);
             * messageEntity.setUpdateTime(date);
             * mqMessageService.save(messageEntity);
             */
            try {
                rabbitTemplate.convertAndSend(exchange, routingKey, orderTo, correlationData);
            } catch (AmqpException e) {
                //TODO ????????????
            }
        }
    }

    @Override
    public PayVo getOrderPayInfo(String orderSn) {
        PayVo payVo = new PayVo();
        payVo.setOut_trade_no(orderSn);

        OrderEntity order = this.getOrderByOrderSn(orderSn);
        BigDecimal bigDecimal = order.getPayAmount().setScale(2, RoundingMode.UP);
        payVo.setTotal_amount(bigDecimal.toString());
        List<OrderItemEntity> orderItemEntities = orderItemService.list(new QueryWrapper<OrderItemEntity>().eq("order_sn", orderSn));
        payVo.setSubject(orderItemEntities.get(0).getSkuName());
        payVo.setBody(orderItemEntities.get(0).getSkuAttrsVals());
        return payVo;
    }

    @Override
    public PageUtils queryPageWithItems(Map<String, Object> params) {
        MemberRespVo memberRespVo = LoginUserInterceptor.loginUser.get();
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>().eq("member_id", memberRespVo.getId()).orderByDesc("id")
        );
        List<OrderEntity> orderEntities = page.getRecords().stream().map(order -> {
            List<OrderItemEntity> itemEntities = orderItemService.list(new QueryWrapper<OrderItemEntity>().eq("order_sn", order.getOrderSn()));
            order.setItemEntities(itemEntities);
            return order;
        }).collect(Collectors.toList());
        //?????????????????????
        page.setRecords(orderEntities);
        return new PageUtils(page);
    }

    @Override
    public String handlePayResult(PayAsyncVo vo) {
        //??????????????????
        PaymentInfoEntity paymentInfo = new PaymentInfoEntity();
        paymentInfo.setOrderSn(vo.getOut_trade_no());
        paymentInfo.setAlipayTradeNo(vo.getTrade_no());
        paymentInfo.setTotalAmount(new BigDecimal(vo.getTotal_amount()));
        paymentInfo.setPaymentStatus(vo.getTrade_status());
        paymentInfo.setCreateTime(new Date());
        paymentInfo.setSubject(vo.getSubject());
        paymentInfo.setCallbackTime(vo.getNotify_time());
        paymentInfoService.save(paymentInfo);
        //??????????????????
        if ("TRADE_SUCCESS".equals(vo.getTrade_status()) || "TRADE_FINISH".equals(vo.getTrade_status())) {
            this.baseMapper.updateOrderStatus(vo.getOut_trade_no(), OrderStatusEnum.PAYED.getCode());
        }
        return "success";
    }

    /**
     * ?????????????????????????????????????????????
     */
    @Override
    public void createSeckillOrder(SeckillOrderTo seckillOrder) {
        //????????????
        OrderEntity order = new OrderEntity();
        order.setOrderSn(seckillOrder.getOrderSn());
        order.setCreateTime(new Date());
        order.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        order.setMemberId(seckillOrder.getMemberId());
        BigDecimal multiply = seckillOrder.getSeckillPrice().multiply(new BigDecimal("" + seckillOrder.getNum()));
        order.setPayAmount(multiply);
        this.save(order);
        //???????????????
        OrderItemEntity orderItem = new OrderItemEntity();
        orderItem.setOrderSn(seckillOrder.getOrderSn());
        orderItem.setRealAmount(multiply);
        orderItem.setSkuQuantity(seckillOrder.getNum());
        orderItem.setSkuId(seckillOrder.getSkuId());
        //TODO ??????????????????sku???spu??????(????????????????????????/????????????)
        R r = productFeignServer.getSpuInfoBySkuId(seckillOrder.getSkuId());
        if (r.getCode() == 0) {
            SpuInfoVo spuInfoVo = r.getData("data", new TypeReference<SpuInfoVo>() {
            });
            orderItem.setSpuId(spuInfoVo.getId());
            orderItem.setSpuName(spuInfoVo.getSpuName());
            orderItem.setSpuBrand(spuInfoVo.getBrandId().toString());
        }
        orderItemService.save(orderItem);
    }

    /**
     * ??????????????????
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
     * ????????????
     *
     * @return
     */
    private OrderCreateTo createOrder() {
        OrderCreateTo createTo = new OrderCreateTo();
        String orderSn = IdWorker.getTimeId();
        //???????????????
        List<OrderItemEntity> orderItems = buildOrderItems(orderSn);
        createTo.setOrderItems(orderItems);
        //????????????
        OrderEntity order = buildOrder(orderSn);
        MemberRespVo member = LoginUserInterceptor.loginUser.get();
        order.setMemberId(member.getId());
        order.setMemberUsername(member.getUsername());
        //?????????????????????
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
        //??????????????????????????????
        for (OrderItemEntity entity : orderItems) {
            couponAmount = couponAmount.add(entity.getCouponAmount());
            integrationAmount = integrationAmount.add(entity.getIntegrationAmount());
            promotionAmount = promotionAmount.add(entity.getPromotionAmount());
            total = total.add(entity.getRealAmount());
            giftIntegration += entity.getGiftIntegration();
            giftGrowth += entity.getGiftGrowth();
        }
        order.setTotalAmount(total); //????????????
        order.setPayAmount(total.add(order.getFreightAmount())); //????????????
        //??????
        order.setPromotionAmount(promotionAmount);
        order.setIntegrationAmount(integrationAmount);
        order.setCouponAmount(couponAmount);
        //??????????????????
        order.setIntegration(giftIntegration);
        order.setGrowth(giftGrowth);
        order.setDeleteStatus(0); //0???????????????
    }

    /**
     * ?????????????????????????????????
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

    //????????????????????????
    private OrderItemEntity buildOrderItem(OrderItemVo cartItem) {
        OrderItemEntity entity = new OrderItemEntity();
        //??????SKU??????
        Long skuId = cartItem.getSkuId();
        entity.setSkuId(skuId);
        entity.setSkuName(cartItem.getTitle());
        entity.setSkuPic(cartItem.getImage());
        entity.setSkuPrice(cartItem.getPrice());
        entity.setSkuQuantity(cartItem.getCount());
        String skuAttr = StringUtils.collectionToDelimitedString(cartItem.getSkuAttr(), ";");
        entity.setSkuAttrsVals(skuAttr);
        //??????SPU???????????????skuId??????spu?????????
        R r = productFeignServer.getSpuInfoBySkuId(skuId);
        SpuInfoVo spuInfo = r.getData("data", new TypeReference<SpuInfoVo>() {
        });
        entity.setCategoryId(spuInfo.getCatalogId());
        entity.setSpuId(spuInfo.getId());
        entity.setSpuName(spuInfo.getSpuName());
        //??????
        entity.setSpuBrand(spuInfo.getBrandId().toString());
        //??????????????????
        Integer gift = cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount().toString())).intValue();
        entity.setGiftGrowth(gift);
        entity.setGiftIntegration(gift);
        //?????????????????????????????????????????????
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
        //????????????
        OrderEntity order = new OrderEntity();
        order.setOrderSn(timeId);
        //?????????????????????????????????
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
        //???????????????????????????????????????
        order.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        return order;
    }

}