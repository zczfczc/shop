package com.msb.mall.product.fegin;

import com.msb.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient("mall-seckill")
public interface SeckillFeignService {

    @GetMapping("/seckill/seckillSessionBySkuId")
    R getSeckillSessionBySkuId(@RequestParam("skuId") Long skuId);
}
