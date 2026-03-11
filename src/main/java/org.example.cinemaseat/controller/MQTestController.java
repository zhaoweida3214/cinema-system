package org.example.cinemaseat.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.cinemaseat.common.Result;
import org.example.cinemaseat.pojo.DTO.OrderMessageDTO;
import org.example.cinemaseat.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * RabbitMQ 消息测试控制器
 */
@Slf4j
@RestController
@RequestMapping("/test/mq")
public class MQTestController {

    @Autowired
    private MessageService messageService;

    /**
     * 测试发送短信消息
     */
    @GetMapping("/sms")
    public Result<String> testSmsMessage() {
        OrderMessageDTO message = OrderMessageDTO.builder()
                .orderId(1L)
                .userId(1L)
                .phoneNumber("138****1234")
                .content("【测试短信】这是一条测试短信，请忽略。")
                .messageType("SMS")
                .build();
        
        messageService.sendSmsMessage(message);
        
        return Result.success("短信消息已发送，请查看消费者日志");
    }

    /**
     * 测试发送积分消息
     */
    @GetMapping("/points")
    public Result<String> testPointsMessage() {
        OrderMessageDTO message = OrderMessageDTO.builder()
                .orderId(1L)
                .userId(1L)
                .points(100)
                .content("【测试积分】获得 100 积分")
                .messageType("POINTS")
                .build();
        
        messageService.sendPointsMessage(message);
        
        return Result.success("积分消息已发送，请查看消费者日志");
    }

    /**
     * 测试发送延迟消息
     */
    @GetMapping("/delay")
    public Result<String> testDelayMessage() {
        OrderMessageDTO message = OrderMessageDTO.builder()
                .orderId(System.currentTimeMillis())
                .userId(1L)
                .messageType("DELAY")
                .content("【延迟消息】15 分钟后将检查订单支付状态")
                .build();
        
        messageService.sendDelayMessage(message);
        
        return Result.success("延迟消息已发送，15 分钟后将被消费");
    }

    /**
     * 批量测试 - 模拟真实订单场景
     */
    @GetMapping("/batch/{count}")
    public Result<String> testBatchMessages(@PathVariable Integer count) {
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < count; i++) {
            final int index = i;
            Long orderId = 1000L + i;
            
            // 发送延迟取消消息
            OrderMessageDTO delayMessage = OrderMessageDTO.builder()
                    .orderId(orderId)
                    .userId(1L)
                    .messageType("DELAY")
                    .content("订单" + orderId + "超时取消通知")
                    .build();
            messageService.sendDelayMessage(delayMessage);
            
            // 发送短信消息
            OrderMessageDTO smsMessage = OrderMessageDTO.builder()
                    .orderId(orderId)
                    .userId(1L)
                    .phoneNumber("138****1234")
                    .content("订单" + orderId + "支付成功通知")
                    .messageType("SMS")
                    .build();
            messageService.sendSmsMessage(smsMessage);
            
            // 发送积分消息
            OrderMessageDTO pointsMessage = OrderMessageDTO.builder()
                    .orderId(orderId)
                    .userId(1L)
                    .points(100)
                    .content("订单" + orderId + "获得 100 积分")
                    .messageType("POINTS")
                    .build();
            messageService.sendPointsMessage(pointsMessage);
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        log.info("【批量消息发送完成】count={}, duration={}ms", count, duration);
        
        return Result.success(String.format("批量发送%d个订单的消息完成，耗时%dms", count, duration));
    }
}
