package org.example.cinemaseat.common.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * 多级缓存管理器
 * 架构：Caffeine(本地缓存) -> Redis(分布式缓存) -> Database(数据库)
 * 
 * 读策略：
 * 1. 先查 Caffeine 本地缓存，命中则返回
 * 2. 本地未命中，查 Redis 分布式缓存，命中则回写本地缓存后返回
 * 3. Redis 未命中，查询数据库，写入 Redis 和 Caffeine 后返回
 * 
 * 写策略：
 * 1. 更新数据库
 * 2. 删除 Redis 缓存（延迟双删策略）
 * 3. 删除 Caffeine 本地缓存
 */
@Slf4j
@Component
public class MultiLevelCacheManager {

    @Autowired
    private CacheManager localCacheManager;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 从多级缓存获取数据
     * 
     * @param cacheName 缓存名称
     * @param key 缓存键
     * @param loader 数据加载器（数据库查询）
     * @param ttl 过期时间（秒）
     * @return 缓存的数据
     */
    public <T> T get(String cacheName, String key, Callable<T> loader, long ttl) {
        // 1. 尝试从 Caffeine 本地缓存获取
        Cache localCache = localCacheManager.getCache(cacheName);
        if (localCache != null) {
            Cache.ValueWrapper wrapper = localCache.get(key);
            if (wrapper != null && wrapper.get() != null) {
                log.debug("【本地缓存命中】key={}", key);
                @SuppressWarnings("unchecked")
                T value = (T) wrapper.get();
                return value;
            }
        }

        // 2. 尝试从 Redis 分布式缓存获取
        String redisKey = buildRedisKey(cacheName, key);
        @SuppressWarnings("unchecked")
        T redisValue = (T) redisTemplate.opsForValue().get(redisKey);
        if (redisValue != null) {
            log.debug("【Redis 缓存命中】key={}", key);
            // 回写到本地缓存
            if (localCache != null) {
                localCache.put(key, redisValue);
            }
            return redisValue;
        }

        // 3. 从数据库加载
        log.debug("【缓存未命中，查询数据库】key={}", key);
        try {
            T value = loader.call();
            if (value != null) {
                // 写入 Redis
                redisTemplate.opsForValue().set(redisKey, value, ttl, TimeUnit.SECONDS);
                // 写入本地缓存
                if (localCache != null) {
                    localCache.put(key, value);
                }
                log.debug("【缓存已更新】key={}", key);
            }
            return value;
        } catch (Exception e) {
            log.error("【缓存加载失败】key={}, error={}", key, e.getMessage());
            throw new RuntimeException("缓存加载失败", e);
        }
    }

    /**
     * 从多级缓存获取数据（使用默认过期时间）
     */
    public <T> T get(String cacheName, String key, Callable<T> loader) {
        return get(cacheName, key, loader, 300); // 默认 5 分钟
    }

    /**
     * 更新缓存
     * 
     * @param cacheName 缓存名称
     * @param key 缓存键
     * @param value 缓存值
     * @param ttl 过期时间（秒）
     */
    public void put(String cacheName, String key, Object value, long ttl) {
        // 写入本地缓存
        Cache localCache = localCacheManager.getCache(cacheName);
        if (localCache != null) {
            localCache.put(key, value);
        }

        // 写入 Redis 缓存
        String redisKey = buildRedisKey(cacheName, key);
        redisTemplate.opsForValue().set(redisKey, value, ttl, TimeUnit.SECONDS);
        
        log.debug("【缓存已更新】cacheName={}, key={}", cacheName, key);
    }

    /**
     * 从缓存中删除
     * 
     * @param cacheName 缓存名称
     * @param key 缓存键
     */
    public void evict(String cacheName, String key) {
        // 删除本地缓存
        Cache localCache = localCacheManager.getCache(cacheName);
        if (localCache != null) {
            localCache.evict(key);
        }

        // 删除 Redis 缓存
        String redisKey = buildRedisKey(cacheName, key);
        redisTemplate.delete(redisKey);
        
        log.debug("【缓存已删除】cacheName={}, key={}", cacheName, key);
    }

    /**
     * 延迟双删 - 用于保证数据库和缓存的一致性
     * 
     * @param cacheName 缓存名称
     * @param key 缓存键
     * @param delay 延迟时间（毫秒）
     */
    public void delayedEvict(String cacheName, String key, long delay) {
        // 立即删除一次
        evict(cacheName, key);
        
        // 延迟删除第二次（防止删除操作发生在数据库更新之前）
        new Thread(() -> {
            try {
                Thread.sleep(delay);
                evict(cacheName, key);
                log.debug("【延迟双删完成】cacheName={}, key={}", cacheName, key);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("【延迟双删中断】cacheName={}, key={}", cacheName, key);
            }
        }).start();
    }

    /**
     * 构建 Redis 的完整 key
     */
    private String buildRedisKey(String cacheName, String key) {
        return cacheName + "::" + key;
    }
}
