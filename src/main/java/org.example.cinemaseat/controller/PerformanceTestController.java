package org.example.cinemaseat.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.cinemaseat.common.Result;
import org.example.cinemaseat.common.cache.MultiLevelCacheManager;
import org.example.cinemaseat.common.config.DistributedLockUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 性能测试控制器
 * 用于测试多级缓存和分布式锁的性能
 */
@Slf4j
@RestController
@RequestMapping("/test")
public class PerformanceTestController {

    @Autowired
    private MultiLevelCacheManager cacheManager;
    
    @Autowired
    private DistributedLockUtil distributedLockUtil;
    
    /**
     * 测试多级缓存性能
     * 
     * @param iterations 迭代次数
     * @return 测试结果
     */
    @GetMapping("/cache/performance")
    public Result<String> testCachePerformance(@RequestParam(defaultValue = "1000") int iterations) {
        long startTime = System.currentTimeMillis();
        
        // 模拟缓存读写
        for (int i = 0; i < iterations; i++) {
            String key = "test_key_" + (i % 100);
            final int currentIndex = i; // 创建 effectively final 变量
            
            // 写缓存
            cacheManager.put("test_cache", key, "value_" + currentIndex, 300);
            
            // 读缓存
            cacheManager.get("test_cache", key, () -> "value_" + currentIndex, 300);
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        log.info("【缓存性能测试】iterations={}, duration={}ms", iterations, duration);
        
        return Result.success(String.format("缓存性能测试完成：执行%d次操作，耗时%dms", iterations, duration));
    }
    
    /**
     * 测试分布式锁并发控制
     * 
     * @param threads 线程数
     * @param iterations 每个线程的迭代次数
     * @return 测试结果
     */
    @GetMapping("/lock/concurrency")
    public Result<String> testDistributedLock(
            @RequestParam(defaultValue = "10") int threads,
            @RequestParam(defaultValue = "100") int iterations) {
        
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        AtomicInteger counter = new AtomicInteger(0);
        
        long startTime = System.currentTimeMillis();
        
        // 启动多个线程同时操作同一个资源
        for (int i = 0; i < threads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < iterations; j++) {
                        // 使用分布式锁保证线程安全
                        distributedLockUtil.executeWithLock("test_lock", () -> {
                            counter.incrementAndGet();
                            // 模拟业务操作
                            try {
                                Thread.sleep(1);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                            return null;
                        });
                    }
                } finally {
                    latch.countDown();
                    log.info("【线程{}完成】threadId={}", threadId, threadId);
                }
            });
        }
        
        try {
            latch.await();
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            int expectedCount = threads * iterations;
            int actualCount = counter.get();
            
            log.info("【分布式锁并发测试】threads={}, iterations={}, expected={}, actual={}, duration={}ms",
                    threads, iterations, expectedCount, actualCount, duration);
            
            if (expectedCount == actualCount) {
                return Result.success(String.format("分布式锁并发测试成功：%d个线程，每个线程执行%d次，总计%d次操作全部完成，耗时%dms",
                        threads, iterations, actualCount, duration));
            } else {
                return Result.error(String.format("测试失败：预期%d次，实际%d次", expectedCount, actualCount));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("【分布式锁并发测试中断】error={}", e.getMessage());
            return Result.error("测试中断");
        } finally {
            executor.shutdown();
        }
    }
    
    /**
     * 测试缓存命中率
     * 
     * @param warmup 预热次数
     * @param testIterations 测试次数
     * @return 测试结果
     */
    @GetMapping("/cache/hit-rate")
    public Result<String> testCacheHitRate(
            @RequestParam(defaultValue = "100") int warmup,
            @RequestParam(defaultValue = "1000") int testIterations) {
        
        // 预热缓存
        for (int i = 0; i < warmup; i++) {
            String key = "hot_key_" + (i % 10); // 只有 10 个不同的 key
            cacheManager.put("hit_rate_test", key, "value_" + i, 300);
        }
        
        long startTime = System.currentTimeMillis();
        int hitCount = 0;
        
        // 测试缓存命中
        for (int i = 0; i < testIterations; i++) {
            String key = "hot_key_" + (i % 10);
            final int currentIndex = i; // 创建 effectively final 变量
            
            Object value = cacheManager.get("hit_rate_test", key, () -> {
                // 这个 lambda 只有在缓存未命中时才会执行
                return "new_value_" + currentIndex;
            }, 300);
            
            if (value != null && ((String) value).startsWith("value_")) {
                hitCount++;
            }
        }
        
        long endTime = System.currentTimeMillis();
        double hitRate = (double) hitCount / testIterations * 100;
        
        log.info("【缓存命中率测试】warmup={}, testIterations={}, hitCount={}, hitRate={}%",
                warmup, testIterations, hitCount, hitRate);
        
        return Result.success(String.format("缓存命中率测试完成：预热%d次，测试%d次，命中%d次，命中率%.2f%%，耗时%dms",
                warmup, testIterations, hitCount, hitRate, (endTime - startTime)));
    }
}
