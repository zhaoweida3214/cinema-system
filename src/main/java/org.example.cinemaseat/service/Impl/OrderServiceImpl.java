package org.example.cinemaseat.service.Impl;

import lombok.extern.slf4j.Slf4j;
import org.example.cinemaseat.common.BaseContext;
import org.example.cinemaseat.common.BusinessException;
import org.example.cinemaseat.common.cache.MultiLevelCacheManager;
import org.example.cinemaseat.common.config.DistributedLockUtil;
import org.example.cinemaseat.mapper.OrderMapper;
import org.example.cinemaseat.mapper.SeatStatusMapper;
import org.example.cinemaseat.pojo.DTO.OrderCreateDTO;
import org.example.cinemaseat.pojo.DTO.OrderMessageDTO;
import org.example.cinemaseat.pojo.VO.OrderCreateVO;
import org.example.cinemaseat.pojo.VO.UserOrderVO;
import org.example.cinemaseat.pojo.entity.SeatStatus;
import org.example.cinemaseat.pojo.entity.Order;
import org.example.cinemaseat.service.MessageService;
import org.example.cinemaseat.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    @Autowired
    private SeatStatusMapper seatStatusMapper;
    
    @Autowired
    private OrderMapper orderMapper;
    
    @Autowired
    private MultiLevelCacheManager cacheManager;
    
    @Autowired
    private DistributedLockUtil distributedLockUtil;
    
    @Autowired
    private MessageService messageService;
    
    // 缓存配置常量
    private static final String ORDER_CACHE = "order_cache";
    private static final String SEAT_LAYOUT_CACHE = "seat_layout_cache";
    private static final String SEAT_INVENTORY_LOCK_PREFIX = "seat_inventory_lock:";
    private static final long CACHE_TTL = 300; // 5 分钟

    // 创建订单
    @Override
    public OrderCreateVO createOrder(OrderCreateDTO dto) {
        Long userId = BaseContext.getCurrentUserId(); // 从 JWT 解析
        Long scheduleId = dto.getScheduleId();
        List<Long> seatIds = dto.getSeatIds();
            
        // 构建分布式锁的 key：基于排期 ID，确保同一场次的座位锁定互斥
        String lockKey = SEAT_INVENTORY_LOCK_PREFIX + scheduleId;
            
        log.info("【创建订单】userId={}, scheduleId={}, seatCount={}", userId, scheduleId, seatIds.size());
            
        // 使用 Redisson 分布式重锁，将并发请求串化
        return distributedLockUtil.executeWithLock(lockKey, () -> {
            log.info("【获取锁成功，开始处理订单】userId={}, scheduleId={}", userId, scheduleId);
                
            // 1. 检查座位是否可锁定（状态为 AVAILABLE）
            // 注意：由于已经加了分布式锁，这里不需要 FOR UPDATE 悲观锁
            List<SeatStatus> currentSeats = seatStatusMapper.selectByIds(seatIds);
            List<Long> unavailable = currentSeats.stream()
                    .filter(s -> !"AVAILABLE".equals(s.getStatus()))
                    .map(SeatStatus::getId)
                    .collect(Collectors.toList());
            if (!unavailable.isEmpty()) {
                throw new BusinessException("以下座位不可用：" + unavailable);
            }
                
            // 2. 计算订单总金额
            List<Double> seatprices = seatStatusMapper.getPricesByIds(seatIds);
            double totalPrice = seatprices.stream().mapToDouble(Double::doubleValue).sum();
                
            // 3. 创建订单（状态 PENDING）
            Order order = new Order();
            order.setUserId(userId);
            order.setScheduleId(scheduleId);
            order.setStatus("PENDING");
            order.setCreatedAt(LocalDateTime.now());
            order.setExpiresAt(LocalDateTime.now().plusMinutes(15));
            order.setTotalAmount(totalPrice);
            orderMapper.insert(order);
                
            // 4. 锁定座位（更新状态为 LOCKED，设置过期时间，并关联订单ID）
            LocalDateTime expireTime = LocalDateTime.now().plusMinutes(15);
            seatStatusMapper.lockSeats(seatIds, "LOCKED", expireTime, order.getId());
                
            // 5. 清理缓存 - 使用延迟双删策略保证缓存一致性
            clearSeatLayoutCache(scheduleId);
                
            log.info("【订单创建成功】orderId={}, scheduleId={}", order.getId(), scheduleId);
                
            // 6. 异步发送消息（短信通知、积分变动、延迟取消）
            sendOrderMessages(order);
                
            // 7. 返回结果
            return OrderCreateVO.builder()
                    .orderId(order.getId())
                    .expiresAt(expireTime)
                    .lockedSeats(seatIds)
                    .build();
        });
    }

    // 支付订单
    @Override
    @Transactional
    public void payOrder(Long orderId) {
        log.info("【支付订单】orderId={}", orderId);
        
        Order order = orderMapper.getById(orderId);
        if (!"PENDING".equals(order.getStatus())) {
            throw new BusinessException("订单不可支付");
        }
        
        // 更新订单状态为 PAID
        orderMapper.updateStatus(orderId, "PAID");
        
        // 座位状态改为 SOLD
        seatStatusMapper.updateStatusByOrderId(orderId, "SOLD");
        
        // 清理座位布局缓存
        clearSeatLayoutCache(order.getScheduleId());
        
        log.info("【订单支付成功】orderId={}", orderId);
        
        // 异步发送积分消息和短信通知
        sendPaymentSuccessMessages(order);
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
        log.info("【取消订单】orderId={}", orderId);
        
        // 1. 获取订单信息
        Order order = orderMapper.getById(orderId);
        
        // 2. 检查订单是否存在且状态为 PENDING
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!"PENDING".equals(order.getStatus()) && !"PAID".equals(order.getStatus())) {
            throw new BusinessException("订单不可取消");
        }
        
        // 3. 更新订单状态为 CANCELLED
        orderMapper.updateStatus(orderId, "CANCELLED");
        
        // 4. 释放锁定的座位（将座位状态改为 AVAILABLE，清除锁定信息和订单关联）
        seatStatusMapper.releaseSeatsByOrderId(orderId);
        
        // 5. 清理座位布局缓存
        clearSeatLayoutCache(order.getScheduleId());
        
        log.info("【订单取消成功】orderId={}", orderId);
    }
    
    /**
     * 清理座位布局缓存 - 使用延迟双删策略
     * 
     * @param scheduleId 排期 ID
     */
    private void clearSeatLayoutCache(Long scheduleId) {
        String cacheKey = "schedule_" + scheduleId;
        // 延迟双删：立即删除 + 延迟删除
        cacheManager.delayedEvict(SEAT_LAYOUT_CACHE, cacheKey, 500);
        log.debug("【清理座位缓存】scheduleId={}", scheduleId);
    }
    
    /**
     * 发送订单相关消息（异步化）
     * 
     * @param order 订单信息
     */
    private void sendOrderMessages(Order order) {
        try {
            // 构建消息对象
            OrderMessageDTO message = OrderMessageDTO.builder()
                    .orderId(order.getId())
                    .userId(order.getUserId())
                    .scheduleId(order.getScheduleId())
                    .totalAmount(order.getTotalAmount())
                    .messageType("ORDER_CREATED")
                    .content("您已成功选座，请在 15 分钟内完成支付")
                    .build();
            
            // 发送延迟取消消息（15 分钟后检查订单是否支付）
            messageService.sendDelayMessage(message);
            
            log.info("【订单消息已发送】orderId={}", order.getId());
        } catch (Exception e) {
            log.error("【订单消息发送失败】orderId={}, error={}", order.getId(), e.getMessage());
            // 消息发送失败不影响主流程，记录日志即可
        }
    }
    
    /**
     * 发送支付成功后的消息（异步化）
     * 
     * @param order 订单信息
     */
    private void sendPaymentSuccessMessages(Order order) {
        try {
            // 计算积分（假设 1 元=10 积分）
            Integer points = (int) (order.getTotalAmount() * 10);
            
            // 构建短信消息
            OrderMessageDTO smsMessage = OrderMessageDTO.builder()
                    .orderId(order.getId())
                    .userId(order.getUserId())
                    .phoneNumber("138****1234") // TODO: 从用户表获取真实手机号
                    .content("您的电影票已出票成功！订单号：" + order.getId() + "，金额：￥" + order.getTotalAmount())
                    .messageType("SMS")
                    .build();
            
            // 构建积分消息
            OrderMessageDTO pointsMessage = OrderMessageDTO.builder()
                    .orderId(order.getId())
                    .userId(order.getUserId())
                    .points(points)
                    .content("订单支付成功，获得" + points + "积分")
                    .messageType("POINTS")
                    .build();
            
            // 异步发送消息
            messageService.sendSmsMessage(smsMessage);
            messageService.sendPointsMessage(pointsMessage);
            
            log.info("【支付成功消息已发送】orderId={}, points={}", order.getId(), points);
        } catch (Exception e) {
            log.error("【支付成功消息发送失败】orderId={}, error={}", order.getId(), e.getMessage());
            // 消息发送失败不影响主流程
        }
    }
}
