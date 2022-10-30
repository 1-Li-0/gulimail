package com.gulimall.cart.vo;

import lombok.Data;

@Data
public class UserInfoTo {
    private Long userId;
    private String userKey;
    //判断临时用户（false表示第一次使用购物车，没有user-key）
    private boolean tempUser = false;
}
