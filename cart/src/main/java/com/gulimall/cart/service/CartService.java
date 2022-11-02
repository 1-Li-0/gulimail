package com.gulimall.cart.service;

import com.gulimall.cart.vo.CartItemVo;
import com.gulimall.cart.vo.CartVo;

import java.util.List;
import java.util.concurrent.ExecutionException;

public interface CartService {

    //将购物项添加到购物车
    CartItemVo addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException;

    //获取某个购物项
    CartItemVo getCartItem(Long skuId);

    //获取整个购物车
    CartVo getCart() throws ExecutionException, InterruptedException;

    //清空购物车
    void clearCart(String userKey);

    CartVo updateItem(Long skuId, Integer check, Integer count) throws ExecutionException, InterruptedException;

    CartVo deleteItem(Long skuId) throws ExecutionException, InterruptedException;

    List<CartItemVo> getOrderItems();
}
