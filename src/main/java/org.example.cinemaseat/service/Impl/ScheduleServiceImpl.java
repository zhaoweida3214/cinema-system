package org.example.cinemaseat.service.Impl;

import lombok.extern.slf4j.Slf4j;
import org.example.cinemaseat.common.cache.MultiLevelCacheManager;
import org.example.cinemaseat.mapper.ScheduleMapper;
import org.example.cinemaseat.pojo.VO.ScheduleVO;
import org.example.cinemaseat.service.ScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class ScheduleServiceImpl implements ScheduleService {
    @Autowired
    private ScheduleMapper scheduleMapper;
    
    @Autowired
    private MultiLevelCacheManager cacheManager;
    
    // 缓存配置常量
    private static final String SCHEDULE_CACHE = "schedule_cache";
    private static final long CACHE_TTL = 600; // 10 分钟
    
    @Override
    public List<ScheduleVO> getSchedulesByCinemaAndDate(Long cinemaId, String date) {
        // 构建缓存 key：影院 ID+ 日期
        String cacheKey = "cinema_" + cinemaId + "_date_" + date;
        
        log.info("【查询排期列表】cinemaId={}, date={}", cinemaId, date);
        
        // 使用多级缓存查询
        return cacheManager.get(SCHEDULE_CACHE, cacheKey, () -> {
            // 缓存未命中时，从数据库查询
            log.debug("【数据库查询】cinemaId={}, date={}", cinemaId, date);
            return scheduleMapper.selectByCinemaAndDate(cinemaId, date);
        }, CACHE_TTL);
    }
}
