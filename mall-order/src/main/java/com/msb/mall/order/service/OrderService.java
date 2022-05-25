package com.msb.mall.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.msb.common.utils.PageUtils;
import com.msb.mall.order.entity.OrderEntity;
import com.msb.mall.order.fegin.MemberFeginService;
import com.msb.mall.order.vo.OrderConfirmVo;

import java.util.Map;

/**
 * 订单
 *
 * @author dpb
 * @email dengpbs@163.com
 * @date 2021-11-24 19:48:00
 */
public interface OrderService extends IService<OrderEntity> {

    PageUtils queryPage(Map<String, Object> params);


    /**
     * 获取订单确认页中需要获取的相关信息
     * @return
     */
    OrderConfirmVo confirmOrder();
}

