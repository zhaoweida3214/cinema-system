package org.example.cinemaseat.pojo.VO;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TokenVO {
    private Long id;
    private String username;
    private String name;
    private String token;
}
