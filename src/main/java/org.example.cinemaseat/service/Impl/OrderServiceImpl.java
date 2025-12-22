package org.example.cinemaseat.service.Impl;

import org.example.cinemaseat.common.BaseContext;
import org.example.cinemaseat.common.BusinessException;
import org.example.cinemaseat.mapper.OrderMapper;
import org.example.cinemaseat.mapper.SeatStatusMapper;
import org.example.cinemaseat.pojo.DTO.OrderCreateDTO;
import org.example.cinemaseat.pojo.VO.OrderCreateVO;
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
        // 2. 锁定座位（更新状态为 LOCKED，设置过期时间）
        LocalDateTime expireTime = LocalDateTime.now().plusMinutes(15);
        seatStatusMapper.lockSeats(seatIds, "LOCKED", expireTime);
        // 3. 创建订单（状态 PENDING）
        Order order = new Order();
        order.setUserId(userId);
        order.setScheduleId(scheduleId);
        order.setStatus("PENDING");
        order.setCreatedAt(LocalDateTime.now());
        order.setExpiresAt(expireTime);
        orderMapper.insert(order);
        // 4. 返回结果
        return OrderCreateVO.builder()
                .orderId(order.getId())
                .expiresAt(expireTime)
                .lockedSeats(seatIds)
                .build();
    }
}
