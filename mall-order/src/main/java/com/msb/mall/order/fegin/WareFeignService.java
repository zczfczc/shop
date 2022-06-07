package com.msb.mall.order.fegin;

import com.msb.mall.order.vo.LockStockResult;
import com.msb.mall.order.vo.WareSkuLockVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient("mall-ware")
public interface WareFeignService {

    @GetMapping("/lock/order")
    public List<LockStockResult> orderLockStock(@RequestBody WareSkuLockVO vo);
}
