package com.gulimall.cart.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.example.common.utils.R;
import com.gulimall.cart.feign.ProductFeignServer;
import com.gulimall.cart.interceptor.CartInterceptor;
import com.gulimall.cart.service.CartService;
import com.gulimall.cart.vo.CartItemVo;
import com.gulimall.cart.vo.CartVo;
import com.gulimall.cart.vo.SkuInfoVo;
import com.gulimall.cart.vo.UserInfoTo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private ProductFeignServer productFeignServer;
    @Autowired
    private ThreadPoolExecutor executor;

    public static String CART_PREFIX = "gulimall:cart:";

    @Override
    public CartItemVo addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException {
        //获取购物车redis操作对象
        BoundHashOperations<String, Object, Object> cartOps = this.getCartOps();
        //1.判断购物车中有没有相同的商品，有则只修改数量
        String item = (String) cartOps.get(skuId.toString());
        if (!StringUtils.isEmpty(item)) {
            CartItemVo cartItemVo = JSON.parseObject(item, CartItemVo.class);
            int count = cartItemVo.getCount() + num;
            cartItemVo.setCount(count);
            //将对象转换成json字符串信息，存储在购物车redis中
            cartOps.put(skuId.toString(), JSON.toJSONString(cartItemVo));
            return cartItemVo;
        } else {
            //2.如果没有，则封装新的购物项信息
            CartItemVo cartItemVo = new CartItemVo();
            //多线程远程调用服务，提高效率
            CompletableFuture<Void> getSkuInfoTask = CompletableFuture.runAsync(() -> {
                //远程查询sku信息
                R skuInfo = productFeignServer.getSkuInfo(skuId);
                SkuInfoVo data = skuInfo.getData("skuInfo", new TypeReference<SkuInfoVo>() {
                });

                cartItemVo.setCheck(true);
                cartItemVo.setSkuId(skuId);
                cartItemVo.setCount(num);
                cartItemVo.setImage(data.getSkuDefaultImg());
                cartItemVo.setPrice(data.getPrice());
                cartItemVo.setTitle(data.getSkuTitle());
            }, executor);

            CompletableFuture<Void> getSkuAttrValuesTask = CompletableFuture.runAsync(() -> {
                //远程查询sku销售属性
                List<String> saleAttrValues = productFeignServer.getSkuSaleAttrValues(skuId);
                cartItemVo.setSkuAttr(saleAttrValues);
            }, executor);

            CompletableFuture.allOf(getSkuInfoTask, getSkuAttrValuesTask).get();
            //将对象转换成json字符串信息，存储在购物车redis中
            cartOps.put(skuId.toString(), JSON.toJSONString(cartItemVo));
            return cartItemVo;
        }
    }

    @Override
    public CartItemVo getCartItem(Long skuId) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        String s = (String) cartOps.get(skuId.toString());
        return JSON.parseObject(s, CartItemVo.class);
    }

    @Override
    public CartVo getCart() throws ExecutionException, InterruptedException {
        CartVo cart = new CartVo();
        //不论登陆与否，都有临时购物车
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        List<CartItemVo> cartItems;
        //如果已登录，需要将购物车合并
        if (userInfoTo.getUserId() != null) {
            //查出所有临时购物项
            List<CartItemVo> tempCartItems = getCartItems(CART_PREFIX + userInfoTo.getUserKey());
            //将每个临时购物项添加到用户购物车（合并）
            if (tempCartItems != null && tempCartItems.size() > 0) {
                for (CartItemVo cartItem : tempCartItems) {
                    addToCart(cartItem.getSkuId(), cartItem.getCount());
                }
                //清空临时购物车
                clearCart(CART_PREFIX + userInfoTo.getUserKey());
            }
            //查询用户购物车
            cartItems = getCartItems(CART_PREFIX + userInfoTo.getUserId());
        } else {
            //查询临时购物车
            cartItems = getCartItems(CART_PREFIX + userInfoTo.getUserKey());
        }
        cart.setItems(cartItems);
        return cart;
    }

    @Override
    public void clearCart(String userKey) {
        redisTemplate.delete(userKey);
    }

    @Override
    public CartVo updateItem(Long skuId, Integer check, Integer count) throws ExecutionException, InterruptedException {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        CartItemVo cartItem = getCartItem(skuId);
        if (check != null) {
            cartItem.setCheck(check == 1);
        }
        if (count != null) {
            cartItem.setCount(count);
        }
        cartOps.put(skuId.toString(), JSON.toJSONString(cartItem));
        return getCart();
    }

    @Override
    public CartVo deleteItem(Long skuId) throws ExecutionException, InterruptedException {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        cartOps.delete(skuId.toString());
        return getCart();
    }

    @Override
    public List<CartItemVo> getOrderItems() {
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        if (userInfoTo.getUserId() == null) {
            return null;
        } else {
            String cartKey = CART_PREFIX + userInfoTo.getUserId();
            List<CartItemVo> cartItems = this.getCartItems(cartKey);
            List<Long> skuIds = cartItems.stream().map(cartItemVo -> cartItemVo.getSkuId()).collect(Collectors.toList());
            List<SkuInfoVo> skuInfoVos = productFeignServer.getCartItemsBySkuIds(skuIds);
            if (cartItems != null && skuInfoVos != null) {
                for (SkuInfoVo skuInfoVo : skuInfoVos) {
                    for (CartItemVo cartItem : cartItems) {
                        if (skuInfoVo.getSkuId() == cartItem.getSkuId()) {
                            cartItem.setPrice(skuInfoVo.getPrice());
                        }
                    }
                }
            }
            return cartItems;
        }
    }

    //获取购物项的集合（购物车Cart的主要属性）
    private List<CartItemVo> getCartItems(String cartKey) {
        List<CartItemVo> cartItems = new ArrayList<>();
        BoundHashOperations<String, Object, Object> cartOps = redisTemplate.boundHashOps(cartKey);
        List<Object> value = cartOps.values();
        if (value != null && value.size() > 0) {
            for (Object item : value) {
                cartItems.add(JSON.parseObject((String) item, CartItemVo.class));
            }
        }
        return cartItems;
    }

    //获取购物车的redis操作对象
    private BoundHashOperations<String, Object, Object> getCartOps() {
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        String cartKey = "";
        if (userInfoTo.getUserId() != null) {
            //登录的购物车key
            cartKey = CART_PREFIX + userInfoTo.getUserId();
        } else {
            //未登录的key
            cartKey = CART_PREFIX + userInfoTo.getUserKey();
        }
        return redisTemplate.boundHashOps(cartKey);
    }
}
