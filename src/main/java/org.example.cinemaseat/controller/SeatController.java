package org.example.cinemaseat.controller;

import org.example.cinemaseat.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.example.cinemaseat.pojo.VO.SeatLayoutVO;
import org.example.cinemaseat.service.SeatService;

@RestController
@RequestMapping("/schedules/{scheduleId}/seats")
public class SeatController {
    @Autowired
    private SeatService seatService;
    @GetMapping
    public Result<SeatLayoutVO> getSeatLayout(@PathVariable Long scheduleId) {
        SeatLayoutVO layout = seatService.getSeatLayout(scheduleId);
        return Result.success(layout);
    }
}