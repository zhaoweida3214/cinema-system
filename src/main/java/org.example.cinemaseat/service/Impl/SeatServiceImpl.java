package org.example.cinemaseat.service.Impl;

import lombok.extern.slf4j.Slf4j;
import org.example.cinemaseat.common.cache.MultiLevelCacheManager;
import org.example.cinemaseat.mapper.HallMapper;
import org.example.cinemaseat.mapper.SeatStatusMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.example.cinemaseat.pojo.VO.SeatLayoutVO;
import org.example.cinemaseat.pojo.entity.Hall;
import org.example.cinemaseat.pojo.entity.SeatStatus;
import org.example.cinemaseat.service.SeatService;

import java.util.List;

@Slf4j
@Service
public class SeatServiceImpl implements SeatService {

    @Autowired
    private HallMapper hallMapper;
    
    @Autowired
    private SeatStatusMapper seatStatusMapper;
    
    @Autowired
    private MultiLevelCacheManager cacheManager;
    
    // 缓存配置常量
    private static final String SEAT_LAYOUT_CACHE = "seat_layout_cache";
    private static final long CACHE_TTL = 300; // 5 分钟（座位状态变化频繁，缓存时间较短）

    @Override
    public SeatLayoutVO getSeatLayout(Long scheduleId) {
        // 构建缓存 key：排期 ID
        String cacheKey = "schedule_" + scheduleId;
        
        log.info("【查询座位布局】scheduleId={}", scheduleId);
        
        // 使用多级缓存查询
        return cacheManager.get(SEAT_LAYOUT_CACHE, cacheKey, () -> {
            // 缓存未命中时，从数据库查询
            log.debug("【数据库查询】scheduleId={}", scheduleId);
            
            // 1. 获取影厅信息（行数、列数、名称）
            Hall hall = hallMapper.getByScheduleId(scheduleId);
            // 2. 获取该场次所有座位状态
            List<SeatStatus> statuses = seatStatusMapper.getByScheduleId(scheduleId);
            
            // 3. 封装返回
            return SeatLayoutVO.builder()
                    .hallName(hall.getName())
                    .rows(hall.getRows())
                    .cols(hall.getCols())
                    .seats(statuses)
                    .build();
        }, CACHE_TTL);
    }
}
