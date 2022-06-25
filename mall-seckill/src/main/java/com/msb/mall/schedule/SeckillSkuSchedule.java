package com.msb.mall.schedule;

import com.msb.mall.service.SeckillService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时上架秒杀商品信息
 */
@Slf4j
@Component
public class SeckillSkuSchedule {

    @Autowired
    SeckillService seckillService;

    /**
     *
     */
    @Async
    @Scheduled(cron = "0 0 2 * * *")
    public void uploadSeckillSku3Days(){
        // 调用上架商品的方法
        seckillService.uploadSeckillSku3Days();
    }

}
