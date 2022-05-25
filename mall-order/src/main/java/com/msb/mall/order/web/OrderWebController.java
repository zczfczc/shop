package com.msb.mall.order.web;

import com.msb.mall.order.service.OrderService;
import com.msb.mall.order.vo.OrderConfirmVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

@Controller
public class OrderWebController {

    @Autowired
    private OrderService orderService;

    @GetMapping("/toTrade")
    public String toTrade(Model model){
        // TODO 查询订单确认页需要的信息
        OrderConfirmVo confirmVo = orderService.confirmOrder();
        model.addAttribute("confirmVo",confirmVo);
        return "confirm";
    }
}
