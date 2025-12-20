package pojo.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Schedule {
    private Long id;
    private Long movieId;
    private Long hallId;
    private Long cinemaId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
