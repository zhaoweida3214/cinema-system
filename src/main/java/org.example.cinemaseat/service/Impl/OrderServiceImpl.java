package org.example.cinemaseat.service.Impl;

import org.example.cinemaseat.common.BaseContext;
import org.example.cinemaseat.common.BusinessException;
import org.example.cinemaseat.mapper.OrderMapper;
import org.example.cinemaseat.mapper.SeatStatusMapper;
import org.example.cinemaseat.pojo.DTO.OrderCreateDTO;
import org.example.cinemaseat.pojo.VO.OrderCreateVO;
import org.example.cinemaseat.pojo.VO.UserOrderVO;
import org.example.cinemaseat.pojo.entity.SeatStatus;
import org.example.cinemaseat.pojo.entity.Order;
import org.example.cinemaseat.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    @Autowired
    private SeatStatusMapper seatStatusMapper;
    @Autowired
    private OrderMapper orderMapper;

    // 创建订单
    @Override
    public OrderCreateVO createOrder(OrderCreateDTO dto) {
        Long userId = BaseContext.getCurrentUserId(); // 从 JWT 解析
        Long scheduleId = dto.getScheduleId();
        List<Long> seatIds = dto.getSeatIds();
        // 1. 检查座位是否可锁定（状态为 AVAILABLE）
        List<SeatStatus> currentSeats = seatStatusMapper.selectByIds(seatIds);
        List<Long> unavailable = currentSeats.stream()
                .filter(s -> !"AVAILABLE".equals(s.getStatus()))
                .map(SeatStatus::getId)
                .collect(Collectors.toList());
        if (!unavailable.isEmpty()) {
            throw new BusinessException("以下座位不可用: " + unavailable);
        }
        // 2. 创建订单（状态 PENDING）
        Order order = new Order();
        order.setUserId(userId);
        order.setScheduleId(scheduleId);
        order.setStatus("PENDING");
        order.setCreatedAt(LocalDateTime.now());
        order.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        orderMapper.insert(order);

        // 3. 锁定座位（更新状态为 LOCKED，设置过期时间，并关联订单ID）
        LocalDateTime expireTime = LocalDateTime.now().plusMinutes(15);
        seatStatusMapper.lockSeats(seatIds, "LOCKED", expireTime, order.getId());

        // 4. 返回结果
        return OrderCreateVO.builder()
                .orderId(order.getId())
                .expiresAt(expireTime)
                .lockedSeats(seatIds)
                .build();
    }

    // 支付订单
    @Override
    @Transactional
    public void payOrder(Long orderId) {
        Order order = orderMapper.getById(orderId);
        if (!"PENDING".equals(order.getStatus())) {
            throw new BusinessException("订单不可支付");
        }
        // 更新订单状态为 PAID
        orderMapper.updateStatus(orderId, "PAID");
        // 座位状态改为 SOLD
        seatStatusMapper.updateStatusByOrderId(orderId, "SOLD");
    }

    // 查询用户订单列表
    @Override
    public List<UserOrderVO> listUserOrders(Long userId) {
        return orderMapper.selectUserOrders(userId);
    }
    // 取消订单
    @Override
    @Transactional
    public void cancelOrder(Long orderId) {
        // 1. 获取订单信息
        Order order = orderMapper.getById(orderId);

        // 2. 检查订单是否存在且状态为PENDING
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!"PENDING".equals(order.getStatus()) && !"PAID".equals(order.getStatus())) {
            throw new BusinessException("订单不可取消");
        }

        // 3. 更新订单状态为CANCELLED
        orderMapper.updateStatus(orderId, "CANCELLED");

        // 4. 释放锁定的座位（将座位状态改为AVAILABLE，清除锁定信息和订单关联）
        seatStatusMapper.releaseSeatsByOrderId(orderId);
    }
}
