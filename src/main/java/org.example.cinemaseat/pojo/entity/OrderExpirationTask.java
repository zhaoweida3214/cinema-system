package org.example.cinemaseat.pojo.entity;

import org.example.cinemaseat.mapper.OrderMapper;
import org.example.cinemaseat.mapper.SeatStatusMapper;
import org.example.cinemaseat.pojo.entity.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class OrderExpirationTask {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private SeatStatusMapper seatStatusMapper;

    // 每分钟执行一次
    @Scheduled(cron = "0 * * * * ?")
    @Transactional
    public void checkExpiredOrders() {
        LocalDateTime now = LocalDateTime.now();
        // 查询所有过期的待支付订单
        List<Order> expiredOrders = orderMapper.selectExpiredOrders(now);

        for (Order order : expiredOrders) {
            try {
                // 更新订单状态为CANCELLED
                orderMapper.updateStatus(order.getId(), "CANCELLED");
                // 释放锁定的座位
                seatStatusMapper.releaseSeatsByOrderId(order.getId());
            } catch (Exception e) {
                // 记录异常，但不中断其他订单的处理
                e.printStackTrace();
            }
        }
    }
}