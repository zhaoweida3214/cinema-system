package org.example.cinemaseat.common.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置类
 * 配置交换机、队列、绑定关系及消息转换器
 */
@Configuration
public class RabbitMQConfig {

    @Value("${mq.exchange.order}")
    private String orderExchange;

    @Value("${mq.exchange.direct}")
    private String directExchange;

    @Value("${mq.queue.sms}")
    private String smsQueue;

    @Value("${mq.queue.points}")
    private String pointsQueue;

    @Value("${mq.queue.delay}")
    private String delayQueue;

    @Value("${mq.routing-key.sms}")
    private String smsRoutingKey;

    @Value("${mq.routing-key.points}")
    private String pointsRoutingKey;

    @Value("${mq.routing-key.delay}")
    private String delayRoutingKey;

    @Value("${mq.message-ttl}")
    private Integer messageTtl;

    /**
     * 订单主题交换机
     */
    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(orderExchange, true, false);
    }

    /**
     * 直连交换机（用于延迟消息）
     */
    @Bean
    public DirectExchange directExchange() {
        return new DirectExchange(directExchange, true, false);
    }

    /**
     * 短信队列 - 持久化
     */
    @Bean
    public Queue smsQueue() {
        return QueueBuilder.durable(smsQueue).build();
    }

    /**
     * 积分队列 - 持久化
     */
    @Bean
    public Queue pointsQueue() {
        return QueueBuilder.durable(pointsQueue).build();
    }

    /**
     * 延迟队列 - 带 TTL 设置
     */
    @Bean
    public Queue delayQueue() {
        return QueueBuilder.durable(delayQueue)
                .withArgument("x-message-ttl", messageTtl) // 消息存活时间 15 分钟
                .build();
    }

    /**
     * 短信队列绑定到订单交换机
     */
    @Bean
    public Binding smsBinding(Queue smsQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(smsQueue).to(orderExchange).with(smsRoutingKey);
    }

    /**
     * 积分队列绑定到订单交换机
     */
    @Bean
    public Binding pointsBinding(Queue pointsQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(pointsQueue).to(orderExchange).with(pointsRoutingKey);
    }

    /**
     * 延迟队列绑定到直连交换机
     */
    @Bean
    public Binding delayBinding(Queue delayQueue, DirectExchange directExchange) {
        return BindingBuilder.bind(delayQueue).to(directExchange).with(delayRoutingKey);
    }

    /**
     * JSON 消息转换器
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitTemplate 配置
     * 启用生产者确认机制和返回机制
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter jsonMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter);
        
        // 启用生产者确认机制
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                // 消息发送失败，记录日志或进行补偿处理
                System.err.println("消息发送失败：" + (cause != null ? cause : "未知原因"));
            }
        });
        
        // 启用返回机制（消息未被路由到队列）
        rabbitTemplate.setReturnsCallback(returnedMessage -> {
            System.err.println("消息未被路由：" + returnedMessage.getMessage());
        });
        
        return rabbitTemplate;
    }
}
