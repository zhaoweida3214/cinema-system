package org.example.cinemaseat.service.Impl;

import org.example.cinemaseat.mapper.HallMapper;
import org.example.cinemaseat.mapper.SeatStatusMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.example.cinemaseat.pojo.VO.SeatLayoutVO;
import org.example.cinemaseat.pojo.entity.Hall;
import org.example.cinemaseat.pojo.entity.SeatStatus;
import org.example.cinemaseat.service.SeatService;

import java.util.List;

@Service
public class SeatServiceImpl implements SeatService {

    @Autowired
    private HallMapper hallMapper;
    @Autowired
    private SeatStatusMapper seatStatusMapper;

    @Override
    public SeatLayoutVO getSeatLayout(Long scheduleId) {
        // 1. 获取影厅信息（行数、列数、名称）
        Hall hall = hallMapper.getByScheduleId(scheduleId);
        // 2. 获取该场次所有座位状态
        List<SeatStatus> statuses =
                seatStatusMapper.getByScheduleId(scheduleId);
        // 3. 封装返回
        return SeatLayoutVO.builder()
                .hallName(hall.getName())
                .rows(hall.getRows())
                .cols(hall.getCols())
                .seats(statuses)
                .build();
    }
}
