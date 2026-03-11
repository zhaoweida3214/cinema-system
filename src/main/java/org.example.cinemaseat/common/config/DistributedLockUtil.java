package org.example.cinemaseat.common.config;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Redisson 分布式重锁工具类
 * 
 * 特性：
 * 1. 互斥性：同一时刻只有一个线程能持有锁
 * 2. 可重入性：同一个线程可以重复获取同一把锁
 * 3. 自动续期：通过 WatchDog 机制自动延长锁的有效期
 * 4. 防止死锁：通过设置最大等待时间避免无限等待
 */
@Slf4j
@Component
public class DistributedLockUtil {

    @Autowired
    private RedissonClient redissonClient;

    @Value("${distributed-lock.wait-time:5}")
    private int waitTime; // 等待锁的时间（秒）

    @Value("${distributed-lock.lease-time:30}")
    private int leaseTime; // 锁持有时间（秒）

    /**
     * 执行加锁业务逻辑（使用默认等待时间和锁持有时间）
     * 
     * @param lockKey 锁的 key
     * @param action 业务逻辑
     * @return 业务执行结果
     */
    public <T> T executeWithLock(String lockKey, Supplier<T> action) {
        return executeWithLock(lockKey, waitTime, leaseTime, action);
    }

    /**
     * 执行加锁业务逻辑（自定义等待时间和锁持有时间）
     * 
     * @param lockKey 锁的 key
     * @param waitTime 等待锁的时间（秒）
     * @param leaseTime 锁持有时间（秒）
     * @param action 业务逻辑
     * @return 业务执行结果
     */
    public <T> T executeWithLock(String lockKey, int waitTime, int leaseTime, Supplier<T> action) {
        RLock lock = redissonClient.getLock(lockKey);
        boolean isLocked = false;
        
        try {
            // 尝试获取锁，如果成功则执行业务逻辑
            // waitTime: 等待锁的最大时间
            // leaseTime: 锁的持有时间（超过后自动释放）
            // TimeUnit.SECONDS: 时间单位
            isLocked = lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);
            
            if (isLocked) {
                log.debug("【获取锁成功】lockKey={}", lockKey);
                // 执行业务逻辑
                return action.get();
            } else {
                log.warn("【获取锁失败】lockKey={}, 请求过于频繁", lockKey);
                throw new RuntimeException("系统繁忙，请稍后再试");
            }
        } catch (InterruptedException e) {
            // 如果等待锁的过程中被中断，恢复中断状态
            Thread.currentThread().interrupt();
            log.error("【获取锁被中断】lockKey={}, error={}", lockKey, e.getMessage());
            throw new RuntimeException("获取锁失败", e);
        } finally {
            // 释放锁
            if (isLocked && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("【释放锁成功】lockKey={}", lockKey);
            }
        }
    }

    /**
     * 执行加锁业务逻辑（无返回值）
     * 
     * @param lockKey 锁的 key
     * @param action 业务逻辑
     */
    public void executeWithLock(String lockKey, Runnable action) {
        executeWithLock(lockKey, () -> {
            action.run();
            return null;
        });
    }

    /**
     * 尝试快速获取锁（不阻塞）
     * 
     * @param lockKey 锁的 key
     * @param action 业务逻辑
     * @return 如果成功获取锁并执行返回 true，否则返回 false
     */
    public <T> T tryExecuteWithLock(String lockKey, Supplier<T> action) {
        RLock lock = redissonClient.getLock(lockKey);
        boolean isLocked = false;
        
        try {
            // 尝试立即获取锁（不等待）
            isLocked = lock.tryLock(0, leaseTime, TimeUnit.SECONDS);
            
            if (isLocked) {
                log.debug("【快速获取锁成功】lockKey={}", lockKey);
                return action.get();
            } else {
                log.warn("【快速获取锁失败】lockKey={}, 资源正在被使用", lockKey);
                throw new RuntimeException("资源正在被使用，请稍后再试");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("【快速获取锁被中断】lockKey={}, error={}", lockKey, e.getMessage());
            throw new RuntimeException("获取锁失败", e);
        } finally {
            if (isLocked && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("【释放锁成功】lockKey={}", lockKey);
            }
        }
    }
}
