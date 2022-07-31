package com.msb.mall.feign;

import com.msb.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient("mall-third")
public interface ThirdPartFeginService {
    @GetMapping("/sms/sendcode")
    public R sendSmsCode(@RequestParam("phone") String phone, @RequestParam("code") String code);
}
