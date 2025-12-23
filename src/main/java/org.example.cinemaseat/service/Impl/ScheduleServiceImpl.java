package org.example.cinemaseat.service.Impl;

import org.example.cinemaseat.mapper.ScheduleMapper;
import org.example.cinemaseat.pojo.VO.ScheduleVO;
import org.example.cinemaseat.service.ScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ScheduleServiceImpl implements ScheduleService {
    @Autowired
    private ScheduleMapper scheduleMapper;
    @Override
    public List<ScheduleVO> getSchedulesByCinemaAndDate(Long cinemaId, String date) {
        return scheduleMapper.selectByCinemaAndDate(cinemaId, date);
    }
}
