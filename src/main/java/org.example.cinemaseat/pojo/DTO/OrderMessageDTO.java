package org.example.cinemaseat.pojo.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 订单消息传输对象
 * 用于 RabbitMQ 消息传递
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderMessageDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 订单ID（幂等性校验唯一标识）
     */
    private Long orderId;
    
    /**
     * 用户 ID
     */
    private Long userId;
    
    /**
     * 排期 ID
     */
    private Long scheduleId;
    
    /**
     * 订单总金额
     */
    private Double totalAmount;
    
    /**
     * 消息类型：SMS(短信) / POINTS(积分) / DELAY(延迟取消)
     */
    private String messageType;
    
    /**
     * 消息内容（短信内容或积分变动说明）
     */
    private String content;
    
    /**
     * 手机号（发送短信用）
     */
    private String phoneNumber;
    
    /**
     * 应增积分
     */
    private Integer points;
}
