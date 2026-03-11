package org.example.cinemaseat.service;

import org.example.cinemaseat.pojo.DTO.OrderMessageDTO;

/**
 * 消息发送服务
 */
public interface MessageService {
    
    /**
     * 发送短信通知消息
     * 
     * @param message 消息内容
     */
    void sendSmsMessage(OrderMessageDTO message);
    
    /**
     * 发送积分变动消息
     * 
     * @param message 消息内容
     */
    void sendPointsMessage(OrderMessageDTO message);
    
    /**
     * 发送延迟取消订单消息
     * 
     * @param message 消息内容
     */
    void sendDelayMessage(OrderMessageDTO message);
}
