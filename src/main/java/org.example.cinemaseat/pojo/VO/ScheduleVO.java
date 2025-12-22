package org.example.cinemaseat.pojo.VO;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ScheduleVO {
    private Long id;
    private Long movieId;
    private String movieTitle;
    private Long hallId;
    private String hallName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
