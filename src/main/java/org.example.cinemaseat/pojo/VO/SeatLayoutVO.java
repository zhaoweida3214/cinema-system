package org.example.cinemaseat.pojo.VO;

import lombok.Builder;
import lombok.Data;
import org.example.cinemaseat.pojo.entity.SeatStatus;

import java.util.List;

@Data
@Builder
public class SeatLayoutVO {
    private String hallName;
    private Integer rows;
    private Integer cols;
    private List<SeatStatus> seats; // 注意：这里复用 SeatStatus 作为 VO，也可单独建SeatVO
}
