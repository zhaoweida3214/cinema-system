package org.example.cinemaseat.service.Impl;

import lombok.extern.slf4j.Slf4j;
import org.example.cinemaseat.common.config.RabbitMQConfig;
import org.example.cinemaseat.pojo.DTO.OrderMessageDTO;
import org.example.cinemaseat.service.MessageService;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 消息发送服务实现类
 * 基于 RabbitMQ 实现可靠消息投递
 */
@Slf4j
@Service
public class MessageServiceImpl implements MessageService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${mq.exchange.order}")
    private String orderExchange;

    @Value("${mq.exchange.direct}")
    private String directExchange;

    @Value("${mq.routing-key.sms}")
    private String smsRoutingKey;

    @Value("${mq.routing-key.points}")
    private String pointsRoutingKey;

    @Value("${mq.routing-key.delay}")
    private String delayRoutingKey;

    /**
     * 发送短信通知消息
     * 特性：
     * 1. 生产者 Confirm 确认
     * 2. 消息持久化
     * 3. 失败重试
     */
    @Override
    public void sendSmsMessage(OrderMessageDTO message) {
        String messageId = "SMS_" + message.getOrderId() + "_" + System.currentTimeMillis();
        CorrelationData correlationData = new CorrelationData(messageId);
        
        log.info("【发送短信消息】orderId={}, messageId={}", message.getOrderId(), messageId);
        
        try {
            rabbitTemplate.convertAndSend(
                orderExchange,
                smsRoutingKey,
                message,
                correlationData
            );
            log.info("【短信消息已发送】orderId={}, messageId={}", message.getOrderId(), messageId);
        } catch (Exception e) {
            log.error("【短信消息发送失败】orderId={}, messageId={}, error={}", 
                    message.getOrderId(), messageId, e.getMessage());
            // 这里可以记录到数据库，后续补偿
            throw new RuntimeException("短信消息发送失败", e);
        }
    }

    /**
     * 发送积分变动消息
     */
    @Override
    public void sendPointsMessage(OrderMessageDTO message) {
        String messageId = "POINTS_" + message.getOrderId() + "_" + System.currentTimeMillis();
        CorrelationData correlationData = new CorrelationData(messageId);
        
        log.info("【发送积分消息】orderId={}, messageId={}", message.getOrderId(), messageId);
        
        try {
            rabbitTemplate.convertAndSend(
                orderExchange,
                pointsRoutingKey,
                message,
                correlationData
            );
            log.info("【积分消息已发送】orderId={}, messageId={}", message.getOrderId(), messageId);
        } catch (Exception e) {
            log.error("【积分消息发送失败】orderId={}, messageId={}, error={}", 
                    message.getOrderId(), messageId, e.getMessage());
            throw new RuntimeException("积分消息发送失败", e);
        }
    }

    /**
     * 发送延迟取消订单消息
     * 消息会在 15 分钟后被消费者接收到，用于自动取消超时订单
     */
    @Override
    public void sendDelayMessage(OrderMessageDTO message) {
        String messageId = "DELAY_" + message.getOrderId() + "_" + System.currentTimeMillis();
        CorrelationData correlationData = new CorrelationData(messageId);
        
        log.info("【发送延迟消息】orderId={}, messageId={}, ttl=15 分钟", message.getOrderId(), messageId);
        
        try {
            rabbitTemplate.convertAndSend(
                directExchange,
                delayRoutingKey,
                message,
                correlationData
            );
            log.info("【延迟消息已发送】orderId={}, messageId={}", message.getOrderId(), messageId);
        } catch (Exception e) {
            log.error("【延迟消息发送失败】orderId={}, messageId={}, error={}", 
                    message.getOrderId(), messageId, e.getMessage());
            throw new RuntimeException("延迟消息发送失败", e);
        }
    }
}
