package com.msb.common.exception;

/**
 * 错误编码和错误信息的枚举类
 * 10:通用的
 * 11：商品
 * 12：订单
 * 13：购物车
 * 14:物流
 */
public enum BizCodeEnume {

    UNKNOW_EXCEPTION(10000,"系统未知异常"),
    VALID_EXCEPTION(10001,"参数格式异常"),
    PRODUCT_UP_EXCEPTION(11001,"商城上架异常");

    private int code;
    private String msg;

    BizCodeEnume(int code,String msg){
        this.code = code;
        this.msg = msg;
    }
    public int getCode(){
        return code;
    }

    public String getMsg(){
        return msg;
    }
}
