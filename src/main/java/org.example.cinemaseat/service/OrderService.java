package org.example.cinemaseat.service;

import org.example.cinemaseat.pojo.DTO.OrderCreateDTO;
import org.example.cinemaseat.pojo.VO.OrderCreateVO;
import org.example.cinemaseat.pojo.VO.UserOrderVO;

import java.util.List;

public interface OrderService {
    // 创建订单
    OrderCreateVO createOrder(OrderCreateDTO dto);
    // 支付订单
    void payOrder(Long orderId);
    // 查询用户订单列表
    List<UserOrderVO> listUserOrders(Long userId);
}
