package org.example.cinemaseat.common.mq;

import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.example.cinemaseat.mapper.OrderMapper;
import org.example.cinemaseat.pojo.DTO.OrderMessageDTO;
import org.example.cinemaseat.pojo.entity.Order;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * 订单消息消费者
 * 特性：
 * 1. 手动 ACK 机制
 * 2. 幂等性校验（基于订单ID）
 * 3. 异常重试机制
 */
@Slf4j
@Component
public class OrderMessageConsumer {

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 短信消息消费者
     * 
     * @param message 消息内容
     * @param channel Channel
     * @param deliveryTag 投递标签
     */
    @RabbitListener(queuesToDeclare = @Queue(value = "${mq.queue.sms}", durable = "true"))
    public void consumeSmsMessage(OrderMessageDTO message, Channel channel, long deliveryTag) {
        log.info("【收到短信消息】orderId={}, phoneNumber={}", message.getOrderId(), message.getPhoneNumber());
        
        try {
            // 1. 幂等性校验：检查订单是否存在
            Order order = orderMapper.getById(message.getOrderId());
            if (order == null) {
                log.warn("【幂等性校验失败】订单不存在，orderId={}", message.getOrderId());
                // 订单不存在，确认消息（避免重复消费）
                channel.basicAck(deliveryTag, false);
                return;
            }
            
            // 2. 模拟发送短信（实际应调用短信服务 API）
            sendSmsNotification(message.getPhoneNumber(), message.getContent());
            
            // 3. 手动 ACK
            channel.basicAck(deliveryTag, false);
            log.info("【短信发送成功】orderId={}, phoneNumber={}", message.getOrderId(), message.getPhoneNumber());
            
        } catch (IOException e) {
            log.error("【短信消息处理失败】orderId={}, error={}", message.getOrderId(), e.getMessage());
            try {
                // 拒绝消息并重新入队（会触发重试机制）
                channel.basicNack(deliveryTag, false, true);
            } catch (IOException ex) {
                log.error("【消息 NACK 失败】orderId={}, error={}", message.getOrderId(), ex.getMessage());
            }
        } catch (Exception e) {
            log.error("【短信发送异常】orderId={}, error={}", message.getOrderId(), e.getMessage());
            try {
                // 业务异常，确认消息（避免死循环）
                channel.basicAck(deliveryTag, false);
            } catch (IOException ex) {
                log.error("【消息 ACK 失败】orderId={}, error={}", message.getOrderId(), ex.getMessage());
            }
        }
    }

    /**
     * 积分消息消费者
     */
    @RabbitListener(queuesToDeclare = @Queue(value = "${mq.queue.points}", durable = "true"))
    public void consumePointsMessage(OrderMessageDTO message, Channel channel, long deliveryTag) {
        log.info("【收到积分消息】orderId={}, userId={}, points={}", 
                message.getOrderId(), message.getUserId(), message.getPoints());
        
        try {
            // 1. 幂等性校验：检查订单是否存在且已支付
            Order order = orderMapper.getById(message.getOrderId());
            if (order == null || !"PAID".equals(order.getStatus())) {
                log.warn("【幂等性校验失败】订单不存在或未支付，orderId={}", message.getOrderId());
                channel.basicAck(deliveryTag, false);
                return;
            }
            
            // 2. 增加用户积分（实际应调用积分服务）
            addPointsForUser(message.getUserId(), message.getPoints());
            
            // 3. 手动 ACK
            channel.basicAck(deliveryTag, false);
            log.info("【积分增加成功】orderId={}, userId={}, points={}", 
                    message.getOrderId(), message.getUserId(), message.getPoints());
            
        } catch (IOException e) {
            log.error("【积分消息处理失败】orderId={}, error={}", message.getOrderId(), e.getMessage());
            try {
                channel.basicNack(deliveryTag, false, true);
            } catch (IOException ex) {
                log.error("【消息 NACK 失败】orderId={}, error={}", message.getOrderId(), ex.getMessage());
            }
        } catch (Exception e) {
            log.error("【积分增加异常】orderId={}, error={}", message.getOrderId(), e.getMessage());
            try {
                channel.basicAck(deliveryTag, false);
            } catch (IOException ex) {
                log.error("【消息 ACK 失败】orderId={}, error={}", message.getOrderId(), ex.getMessage());
            }
        }
    }

    /**
     * 延迟取消订单消费者
     * 15 分钟后收到消息，自动取消超时未支付的订单
     */
    @RabbitListener(queuesToDeclare = @Queue(value = "${mq.queue.delay}", durable = "true"))
    public void consumeDelayMessage(OrderMessageDTO message, Channel channel, long deliveryTag) {
        log.info("【收到延迟消息】orderId={}, 检查是否超时未支付", message.getOrderId());
        
        try {
            // 1. 幂等性校验：检查订单是否存在
            Order order = orderMapper.getById(message.getOrderId());
            if (order == null) {
                log.warn("【幂等性校验失败】订单不存在，orderId={}", message.getOrderId());
                channel.basicAck(deliveryTag, false);
                return;
            }
            
            // 2. 检查订单状态是否为 PENDING（未支付）
            if (!"PENDING".equals(order.getStatus())) {
                log.info("【订单已处理】orderId={}, status={}, 跳过取消", 
                        message.getOrderId(), order.getStatus());
                channel.basicAck(deliveryTag, false);
                return;
            }
            
            // 3. 检查是否真的超时
            if (order.getExpiresAt() != null && order.getExpiresAt().isAfter(LocalDateTime.now())) {
                log.info("【订单未超时】orderId={}, expiresAt={}", 
                        message.getOrderId(), order.getExpiresAt());
                channel.basicAck(deliveryTag, false);
                return;
            }
            
            // 4. 取消订单
            log.info("【执行订单取消】orderId={}", message.getOrderId());
            orderMapper.updateStatus(message.getOrderId(), "CANCELLED");
            
            // TODO: 释放座位（需要调用 SeatStatusMapper）
            // seatStatusMapper.releaseSeatsByOrderId(message.getOrderId());
            
            // 5. 手动 ACK
            channel.basicAck(deliveryTag, false);
            log.info("【订单超时取消成功】orderId={}", message.getOrderId());
            
        } catch (IOException e) {
            log.error("【延迟消息处理失败】orderId={}, error={}", message.getOrderId(), e.getMessage());
            try {
                channel.basicNack(deliveryTag, false, false); // 不重新入队，避免死循环
            } catch (IOException ex) {
                log.error("【消息 NACK 失败】orderId={}, error={}", message.getOrderId(), ex.getMessage());
            }
        } catch (Exception e) {
            log.error("【订单取消异常】orderId={}, error={}", message.getOrderId(), e.getMessage());
            try {
                channel.basicAck(deliveryTag, false);
            } catch (IOException ex) {
                log.error("【消息 ACK 失败】orderId={}, error={}", message.getOrderId(), ex.getMessage());
            }
        }
    }

    /**
     * 模拟发送短信
     */
    private void sendSmsNotification(String phoneNumber, String content) {
        log.info("【模拟发送短信】phoneNumber={}, content={}", phoneNumber, content);
        // TODO: 实际应调用短信服务商 API（如阿里云、腾讯云）
        try {
            Thread.sleep(100); // 模拟网络请求延迟
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 模拟增加用户积分
     */
    private void addPointsForUser(Long userId, Integer points) {
        log.info("【模拟增加积分】userId={}, points={}", userId, points);
        // TODO: 实际应调用积分服务或更新数据库
        try {
            Thread.sleep(100); // 模拟数据库操作延迟
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
