package com.msb.mall.service.impl;

import com.msb.common.constant.SeckillConstant;
import com.msb.common.utils.R;
import com.msb.mall.dto.SeckillSkuRedisDto;
import com.msb.mall.feign.CouponFeignService;
import com.msb.mall.feign.ProductFeignService;
import com.msb.mall.service.SeckillService;
import com.msb.mall.vo.SeckillSessionEntity;
import com.msb.mall.vo.SeckillSkuRelationEntity;
import com.msb.mall.vo.SkuInfoVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SeckillServiceImpl implements SeckillService {

    @Autowired
    CouponFeignService couponFeignService;

    @Autowired
    ProductFeignService productFeignService;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Override
    public void uploadSeckillSku3Days() {
        // 1. 通过OpenFegin 远程调用Coupon服务中接口来获取未来三天的秒杀活动的商品
        R r = couponFeignService.getLates3DaysSession();
        if(r.getCode() == 0){
            // 表示查询操作成功
            List<SeckillSessionEntity> seckillSessionEntities = (List<SeckillSessionEntity>) r.get("data");
            // 2. 上架商品  Redis数据保存
            // 缓存商品
            //  2.1 缓存每日秒杀的SKU基本信息
            saveSessionInfos(seckillSessionEntities);
            // 2.2  缓存每日秒杀的商品信息
            saveSessionSkuInfos(seckillSessionEntities);

        }
    }

    /**
     * 保存每日活动的信息到Redis中
     * @param seckillSessionEntities
     */
    private void saveSessionInfos(List<SeckillSessionEntity> seckillSessionEntities) {
        for (SeckillSessionEntity seckillSessionEntity : seckillSessionEntities) {
            // 循环缓存每一个活动  key： start_endTime
            long start = seckillSessionEntity.getStartTime().getTime();
            long end = seckillSessionEntity.getEndTime().getTime();
            // 生成Key
            String key = SeckillConstant.SESSION_CHACE_PREFIX+start+"_"+end;
            // 需要存储到Redis中的这个秒杀活动涉及到的相关的商品信息的SKUID
            List<String> collect = seckillSessionEntity.getRelationEntities().stream().map(item -> {
                return item.getSkuId().toString();
            }).collect(Collectors.toList());
            redisTemplate.opsForList().leftPushAll(key,collect);
        }
    }

    /**
     * 存储活动对应的 SKU信息
     * @param seckillSessionEntities
     */
    private void saveSessionSkuInfos(List<SeckillSessionEntity> seckillSessionEntities) {
        seckillSessionEntities.stream().forEach(session -> {
            // 循环取出每个Session，然后取出对应SkuID 封装相关的信息
            BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(SeckillConstant.SKU_CHACE_PREFIX);
            session.getRelationEntities().stream().forEach(item->{
                SeckillSkuRedisDto dto = new SeckillSkuRedisDto();
                // 1.获取SKU的基本信息
                R info = productFeignService.info(item.getSkuId());
                if(info.getCode() == 0){
                    // 表示查询成功
                    SkuInfoVo skuInfoVo = (SkuInfoVo) info.get("skuInfo");
                    dto.setSkuInfoVo(skuInfoVo);
                }
                // 2.获取SKU的秒杀信息
                /*dto.setSkuId(item.getSkuId());
                dto.setSeckillPrice(item.getSeckillPrice());
                dto.setSeckillCount(item.getSeckillCount());
                dto.setSeckillLimit(item.getSeckillLimit());
                dto.setSeckillSort(item.getSeckillSort());*/
                BeanUtils.copyProperties(info,dto);
                // 3.设置当前商品的秒杀时间
                dto.setStartTime(session.getStartTime().getTime());
                dto.setEndTime(session.getEndTime().getTime());

                // 4. 随机码
                String token = UUID.randomUUID().toString().replace("-","");
                dto.setRandCode(token);

                hashOps.put(item.getSkuId(),dto);
            });
        });
    }


}
