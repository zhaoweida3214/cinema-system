package org.example.cinemaseat.pojo.entity;

import lombok.extern.slf4j.Slf4j;
import org.example.cinemaseat.common.cache.MultiLevelCacheManager;
import org.example.cinemaseat.mapper.OrderMapper;
import org.example.cinemaseat.mapper.SeatStatusMapper;
import org.example.cinemaseat.pojo.entity.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class OrderExpirationTask {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private SeatStatusMapper seatStatusMapper;
    
    @Autowired
    private MultiLevelCacheManager cacheManager;
    
    private static final String SEAT_LAYOUT_CACHE = "seat_layout_cache";

    // 每分钟执行一次
    @Scheduled(cron = "0 * * * * ?")
    @Transactional
    public void checkExpiredOrders() {
        LocalDateTime now = LocalDateTime.now();
        // 查询所有过期的待支付订单
        List<Order> expiredOrders = orderMapper.selectExpiredOrders(now);
        
        log.info("【定时任务 - 检查过期订单】过期订单数量={}", expiredOrders.size());

        for (Order order : expiredOrders) {
            try {
                // 更新订单状态为 CANCELLED
                orderMapper.updateStatus(order.getId(), "CANCELLED");
                // 释放锁定的座位
                seatStatusMapper.releaseSeatsByOrderId(order.getId());
                
                // 清理座位布局缓存
                clearSeatLayoutCache(order.getScheduleId());
                
                log.info("【定时任务 - 订单过期处理】orderId={}, scheduleId={}", order.getId(), order.getScheduleId());
            } catch (Exception e) {
                // 记录异常，但不中断其他订单的处理
                log.error("【定时任务 - 订单过期处理失败】orderId={}, error={}", order.getId(), e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * 清理座位布局缓存 - 使用延迟双删策略
     */
    private void clearSeatLayoutCache(Long scheduleId) {
        String cacheKey = "schedule_" + scheduleId;
        cacheManager.delayedEvict(SEAT_LAYOUT_CACHE, cacheKey, 500);
        log.debug("【定时任务 - 清理座位缓存】scheduleId={}", scheduleId);
    }
}