package org.example.cinemaseat.pojo.VO;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserOrderVO {
    private Long id;
    private Long scheduleId;
    private String movieTitle;
    private String hallName;
    private LocalDateTime startTime;
    private List<String> seatNumbers; // 如 ["1排1座", "1排3座"]
    private String status;
    private LocalDateTime createdAt;
}
