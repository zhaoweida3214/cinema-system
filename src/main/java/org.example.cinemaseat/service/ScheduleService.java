package org.example.cinemaseat.service;

import org.example.cinemaseat.pojo.VO.ScheduleVO;

import java.util.List;

public interface ScheduleService {
    List<ScheduleVO> getSchedulesByCinemaAndDate(Long cinemaId, String date);
}
