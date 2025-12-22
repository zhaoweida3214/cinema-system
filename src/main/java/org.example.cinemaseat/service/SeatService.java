package org.example.cinemaseat.service;

import org.example.cinemaseat.pojo.VO.SeatLayoutVO;

public interface SeatService {
    SeatLayoutVO getSeatLayout(Long scheduleId);
}
