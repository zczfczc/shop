package com.msb.mall.order.fegin;

import com.msb.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * @FeignClient 指明我们要从注册中心中发现的服务的名称
 */
@FeignClient(name = "mall-product")
public interface ProductService {

    /**
     * 需要访问的远程方法
     * @return
     */
    @GetMapping("/product/brand/all")
    public R queryAllBrand();
}
