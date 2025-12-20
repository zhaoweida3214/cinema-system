package pojo.DTO;

import lombok.Data;
import java.util.List;

@Data
public class OrderCreateDTO {
    private Long scheduleId;
    private List<Long> seatIds;
}
