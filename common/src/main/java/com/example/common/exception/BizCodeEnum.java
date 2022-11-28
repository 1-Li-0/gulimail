package com.example.common.exception;

/**
 * 10 通用
 *    001参数校验
 *    002短信验证码校验
 * 11 商品
 * 12 订单
 * 13 购物车
 * 14 物流
 * 15 用户
 * 21 库存
 */
public enum BizCodeEnum {
    UNKNOW_EXCEPTION(100000, "系统未知异常"),
    VALID_EXCEPTION(10001, "参数格式校验失败"),
    SMS_CODE_EXCEPTION(10002, "验证码发送太频繁，请稍后再试"),
    TOO_MANY_REQUEST(10003, "系统限流"),
    PRODUCT_UP_EXCEPTION(11000, "商品上架异常"),
    USER_EXIST_EXCEPTION(15001, "用户已存在"),
    PHONE_EXIST_EXCEPTION(15002, "手机号已注册"),
    ACCOUNT_PASSWORD_INVALID_EXCEPTION(15003, "账号或密码错误"),
    MEMBER_ACCOUNT_EXCEPTION(15004, "社交账号异常，登陆失败"),
    NO_STOCK_EXCEPTION(21001, "库存不足");

    private int code;
    private String msg;

    BizCodeEnum(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }

}
