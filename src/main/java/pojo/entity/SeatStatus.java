package pojo.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SeatStatus {
    private Long id;
    private Long scheduleId;
    private Integer row;
    private Integer col;
    private String status; // AVAILABLE / LOCKED / SOLD
    private String type;   // NORMAL / VIP
    private Long orderId;  // 关联订单ID（可为空）
    private LocalDateTime lockedUntil; // 锁定过期时间
}
