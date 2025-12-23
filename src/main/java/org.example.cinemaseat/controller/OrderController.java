package org.example.cinemaseat.controller;

import org.example.cinemaseat.common.BaseContext;
import org.example.cinemaseat.common.Result;
import org.example.cinemaseat.pojo.DTO.OrderCreateDTO;
import org.example.cinemaseat.pojo.VO.OrderCreateVO;
import org.example.cinemaseat.pojo.VO.UserOrderVO;
import org.example.cinemaseat.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {
    @Autowired
    private OrderService orderService;
    // 创建订单
    @PostMapping
    public Result<OrderCreateVO> createOrder(@RequestBody OrderCreateDTO dto) {
        OrderCreateVO vo = orderService.createOrder(dto);
        return Result.success(vo);
    }
    //模拟支付订单
    @PutMapping("/{orderId}/pay")
    public Result<Void> payOrder(@PathVariable Long orderId) {
        orderService.payOrder(orderId);
        return Result.success();
    }
    //查询用户当前订单列表
    @GetMapping
    public Result<List<UserOrderVO>> listUserOrders() {
        Long userId = BaseContext.getCurrentUserId();
        List<UserOrderVO> orders = orderService.listUserOrders(userId);
        return Result.success(orders);
    }

}
