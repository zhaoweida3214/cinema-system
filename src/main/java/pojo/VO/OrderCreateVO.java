package pojo.VO;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrderCreateVO {
    private Long orderId;
    private LocalDateTime expiresAt;
    private List<Long> lockedSeats;
}
