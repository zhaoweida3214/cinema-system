package org.example.cinemaseat.common.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 分布式锁配置
 */
@Configuration
public class RedissonConfig {

    @Value("${redis.host:localhost}")
    private String redisHost;

    @Value("${redis.port:6379}")
    private int redisPort;

    @Value("${redis.password:}")
    private String redisPassword;

    @Value("${redis.database:0}")
    private int database;

    /**
     * 配置 RedissonClient
     * Redisson 是 Redis 的 Java 驻留客户端，提供分布式锁等高级功能
     */
    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        
        // 构建 Redis 地址
        String address = "redis://" + redisHost + ":" + redisPort;
        
        // 使用单节点模式（生产环境可根据需求使用主从或集群模式）
        config.useSingleServer()
                .setAddress(address)
                .setDatabase(database)
                .setTimeout(3000) // 命令等待超时时间（毫秒）
                .setConnectTimeout(5000) // 连接建立超时时间（毫秒）
                .setRetryAttempts(3) // 失败重试次数
                .setRetryInterval(1500); // 重试间隔（毫秒）
        
        // 如果有密码则设置密码
        if (redisPassword != null && !redisPassword.isEmpty()) {
            config.useSingleServer().setPassword(redisPassword);
        }
        
        return Redisson.create(config);
    }
}
