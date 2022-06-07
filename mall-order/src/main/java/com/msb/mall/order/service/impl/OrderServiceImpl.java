package com.msb.mall.order.service.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.msb.common.constant.OrderConstant;
import com.msb.common.utils.R;
import com.msb.common.vo.MemberVO;
import com.msb.mall.order.dto.OrderCreateTO;
import com.msb.mall.order.entity.OrderItemEntity;
import com.msb.mall.order.fegin.CartFeginService;
import com.msb.mall.order.fegin.MemberFeginService;
import com.msb.mall.order.fegin.ProductService;
import com.msb.mall.order.fegin.WareFeignService;
import com.msb.mall.order.interceptor.AuthInterceptor;
import com.msb.mall.order.service.OrderItemService;
import com.msb.mall.order.vo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.msb.common.utils.PageUtils;
import com.msb.common.utils.Query;

import com.msb.mall.order.dao.OrderDao;
import com.msb.mall.order.entity.OrderEntity;
import com.msb.mall.order.service.OrderService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {

    @Autowired
    MemberFeginService memberFeginService;

    @Autowired
    CartFeginService cartFeginService;

    @Autowired
    ProductService productService;

    @Autowired
    ThreadPoolExecutor executor;

    @Autowired
    StringRedisTemplate redisTemplate;


    @Autowired
    OrderService orderService;

    @Autowired
    OrderItemService orderItemService;

    @Autowired
    WareFeignService wareFeignService;



    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public OrderConfirmVo confirmOrder() {
        OrderConfirmVo vo = new OrderConfirmVo();
        MemberVO memberVO = (MemberVO) AuthInterceptor.threadLocal.get();
        // 在主线程中获取 RequestAttributes
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        CompletableFuture<Void> future1 = CompletableFuture.runAsync(() -> {
            // RequestContextHolder 绑定主线程中的 RequestAttributes
            RequestContextHolder.setRequestAttributes(requestAttributes);
            // 1.查询当前登录用户对应的会员的地址信息
            Long id = memberVO.getId();
            List<MemberAddressVo> addresses = memberFeginService.getAddress(id);
            vo.setAddress(addresses);
        }, executor);

        CompletableFuture<Void> future2 = CompletableFuture.runAsync(() -> {
            // RequestContextHolder 绑定主线程中的 RequestAttributes
            RequestContextHolder.setRequestAttributes(requestAttributes);
            // 2.查询购物车中选中的商品信息
            List<OrderItemVo> userCartItems = cartFeginService.getUserCartItems();
            vo.setItems(userCartItems);
        }, executor);

        // 主线程需要等待所有的子线程完成后继续
        try {
            CompletableFuture.allOf(future1,future2).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }



        // 3.计算订单的总金额和需要支付的总金额 VO自动计算

        // 4.生成防重的Token
        String token = UUID.randomUUID().toString().replaceAll("-", "");
        // 我们需要把这个Token信息存储在Redis中
        // order:token:用户编号
        redisTemplate.opsForValue().set(OrderConstant.ORDER_TOKEN_PREFIX+":"+memberVO.getId(),token);
        // 然后我们需要将这个Token绑定在响应的数据对象中
        vo.setOrderToken(token);
        return vo;
    }

    private Lock lock = new ReentrantLock();

    @Transactional
    @Override
    public OrderResponseVO submitOrder(OrderSubmitVO vo) {
        // 需要返回响应的对象
        OrderResponseVO responseVO = new OrderResponseVO();

        // 获取当前登录的用户信息
        MemberVO memberVO = (MemberVO) AuthInterceptor.threadLocal.get();
        // 1.验证是否重复提交  保证Redis中的token 的查询和删除是一个原子性操作
        String key = OrderConstant.ORDER_TOKEN_PREFIX+":"+memberVO.getId();
        String script = "if redis.call('get',KEYS[1])==ARGV[1] then return redis.call('del',KEYS[1]) else return 0";
        Long result = redisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class)
                , Arrays.asList(key)
                , vo.getOrderToken());
        if(result == 0){
            // 表示验证失败 说明是重复提交
            responseVO.setCode(1);
            return responseVO;
        }

        // 2.创建订单和订单项信息
        OrderCreateTO orderCreateTO = createOrder(vo);

        // 3.保存订单信息
        saveOrder(orderCreateTO);
        // 4.锁定库存信息
        // 订单号  SKU_ID  SKU_NAME 商品数量
        // 封装 WareSkuLockVO 对象
        WareSkuLockVO wareSkuLockVO = new WareSkuLockVO();
        wareSkuLockVO.setOrderSN(orderCreateTO.getOrderEntity().getOrderSn());
        List<OrderItemVo> orderItemVos = orderCreateTO.getOrderItemEntitys().stream().map(item -> {
            OrderItemVo itemVo = new OrderItemVo();
            itemVo.setSkuId(item.getSkuId());
            itemVo.setTitle(item.getSkuName());
            itemVo.setCount(item.getSkuQuantity());
            return itemVo;
        }).collect(Collectors.toList());
        wareSkuLockVO.setItems(orderItemVos);
        List<LockStockResult> lockStockResults = wareFeignService.orderLockStock(wareSkuLockVO);
        if(lockStockResults != null && lockStockResults.size() > 0){
            // 表示锁定库存成功
        }else{
            // 表示锁定库存失败
        }
        return null;
    }

    /**
     * 生成订单数据
     * @param orderCreateTO
     */
    private void saveOrder(OrderCreateTO orderCreateTO) {
        // 1.订单数据
        OrderEntity orderEntity = orderCreateTO.getOrderEntity();
        orderService.save(orderEntity);
        // 2.订单项数据
        List<OrderItemEntity> orderItemEntitys = orderCreateTO.getOrderItemEntitys();
        orderItemService.saveBatch(orderItemEntitys);
    }

    /**
     * 创建订单的方法
     * @param vo
     * @return
     */
    private OrderCreateTO createOrder(OrderSubmitVO vo) {
        OrderCreateTO createTO = new OrderCreateTO();
        // 创建订单
        OrderEntity orderEntity = buildOrder(vo);
        createTO.setOrderEntity(orderEntity);
        // 创建OrderItemEntity 订单项
        List<OrderItemEntity> orderItemEntitys = buildOrderItems(orderEntity.getOrderSn());
        createTO.setOrderItemEntitys(orderItemEntitys);
        return createTO;
    }

    /**
     * 通过购物车中选中的商品来创建对应的购物项信息
     * @return
     */
    private List<OrderItemEntity> buildOrderItems(String orderSN) {
        List<OrderItemEntity> orderItemEntitys = new ArrayList<>();
        // 获取购物车中的商品信息 选中的
        List<OrderItemVo> userCartItems = cartFeginService.getUserCartItems();
        if(userCartItems != null && userCartItems.size() > 0){
            // 统一根据SKUID查询出对应的SPU的信息
            List<Long> spuIds = new ArrayList<>();
            for (OrderItemEntity orderItemEntity : orderItemEntitys) {
                if(!spuIds.contains(orderItemEntity.getSpuId())){
                    spuIds.add(orderItemEntity.getOrderId());
                }
            }
            // 远程调用商品服务获取到对应的SPU信息
            List<OrderItemSpuInfoVO> spuInfos = productService.getOrderItemSpuInfoBySpuId((Long[]) spuIds.toArray());
            Map<Long, OrderItemSpuInfoVO> map = spuInfos.stream().collect(Collectors.toMap(OrderItemSpuInfoVO::getId, item -> item));
            for (OrderItemVo userCartItem : userCartItems) {
                // 获取到商品信息对应的 SPU信息
                OrderItemSpuInfoVO spuInfo  = map.get(userCartItem.getSpuId());
                OrderItemEntity orderItemEntity = buildOrderItem(userCartItem,spuInfo);
                // 绑定对应的订单编号
                orderItemEntity.setOrderSn(orderSN);
                orderItemEntitys.add(orderItemEntity);
            }
        }

        return orderItemEntitys;
    }

    /**
     * 根据一个购物车中的商品创建对应的 订单项
     * @param userCartItem
     * @return
     */
    private OrderItemEntity buildOrderItem(OrderItemVo userCartItem,OrderItemSpuInfoVO spuInfo) {
        OrderItemEntity entity = new OrderItemEntity();
        // SKU信息
        entity.setSkuId(userCartItem.getSkuId());
        entity.setSkuName(userCartItem.getTitle());
        entity.setSkuPic(userCartItem.getImage());
        entity.setSkuQuantity(userCartItem.getCount());
        List<String> skuAttr = userCartItem.getSkuAttr();
        String skuAttrStr = StringUtils.collectionToDelimitedString(skuAttr, ";");
        entity.setSkuAttrsVals(skuAttrStr);
        // SPU信息
        entity.setSpuId(spuInfo.getId());
        entity.setSpuBrand(spuInfo.getBrandName());
        entity.setCategoryId(spuInfo.getCatalogId());
        entity.setSpuPic(spuInfo.getImg());
        // 优惠信息 忽略
        // 积分信息
        entity.setGiftGrowth(userCartItem.getPrice().intValue());
        entity.setGiftIntegration(userCartItem.getPrice().intValue());
        return entity;
    }

    private OrderEntity buildOrder(OrderSubmitVO vo) {
        // 创建OrderEntity
        OrderEntity orderEntity = new OrderEntity();
        // 创建订单编号
        String orderSn = IdWorker.getTimeId();
        orderEntity.setOrderSn(orderSn);
        MemberVO memberVO = (MemberVO) AuthInterceptor.threadLocal.get();
        // 设置会员相关的信息
        orderEntity.setMemberId(memberVO.getId());
        orderEntity.setMemberUsername(memberVO.getUsername());
        // 根据收获地址ID获取收获地址的详细信息
        MemberAddressVo memberAddressVo = memberFeginService.getAddressById(vo.getAddrId());
        orderEntity.setReceiverCity(memberAddressVo.getCity());
        orderEntity.setReceiverDetailAddress(memberAddressVo.getDetailAddress());
        orderEntity.setReceiverName(memberAddressVo.getName());
        orderEntity.setReceiverPhone(memberAddressVo.getPhone());
        orderEntity.setReceiverPostCode(memberAddressVo.getPostCode());
        orderEntity.setReceiverRegion(memberAddressVo.getRegion());
        orderEntity.setReceiverProvince(memberAddressVo.getProvince());
        // 设置订单的状态
        orderEntity.setStatus(OrderConstant.OrderStateEnum.FOR_THE_PAYMENT.getCode());
        return orderEntity;
    }

}