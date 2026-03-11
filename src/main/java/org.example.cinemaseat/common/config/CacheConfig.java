package org.example.cinemaseat.common.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 缓存配置类 - 配置 Caffeine 本地缓存和 Redis 分布式缓存
 */
@Configuration
public class CacheConfig {

    @Value("${cache.caffeine.max-size:10000}")
    private int maxSize;

    @Value("${cache.caffeine.expire-after-write:300}")
    private int expireAfterWrite;

    @Value("${cache.caffeine.refresh-after-write:180}")
    private int refreshAfterWrite;

    /**
     * Caffeine 本地缓存管理器
     * 用于缓存热点数据，减少 Redis 网络开销
     */
    @Bean("localCacheManager")
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                // 设置最后一次写入或访问后经过固定时间过期
                .expireAfterWrite(Duration.ofSeconds(expireAfterWrite))
                // 设置缓存刷新时间，当缓存即将过期时自动刷新
                .refreshAfterWrite(Duration.ofSeconds(refreshAfterWrite))
                // 设置缓存最大数量
                .maximumSize(maxSize));
        return cacheManager;
    }

    /**
     * Redis Template 配置
     * 用于操作 Redis 分布式缓存
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);
        
        // 设置 key 的序列化器
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        
        // 设置 value 的序列化器（使用 JSON）
        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        redisTemplate.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }
}
