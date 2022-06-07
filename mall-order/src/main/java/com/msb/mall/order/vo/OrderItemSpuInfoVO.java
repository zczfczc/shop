package com.msb.mall.order.vo;

import lombok.Data;

@Data
public class OrderItemSpuInfoVO {
    private Long id;
    private String spuName;
    private String spuDescription;
    private long catalogId;
    private String catalogName;
    private long brandId;
    private String brandName;
    private String img;
}
