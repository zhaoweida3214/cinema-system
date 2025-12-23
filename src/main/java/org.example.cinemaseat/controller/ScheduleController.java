package org.example.cinemaseat.controller;

import org.example.cinemaseat.common.Result;
import org.example.cinemaseat.pojo.VO.ScheduleVO;
import org.example.cinemaseat.service.ScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/schedules")
public class ScheduleController {
    @Autowired
    private ScheduleService scheduleService;
    @GetMapping
    public Result<List<ScheduleVO>> getSchedules(
            @RequestParam Long cinemaId,
            @RequestParam(required = false) String date) {
        if (date == null) date = LocalDate.now().toString();
        List<ScheduleVO> list =
                scheduleService.getSchedulesByCinemaAndDate(cinemaId, date);
        return Result.success(list);
    }
}

