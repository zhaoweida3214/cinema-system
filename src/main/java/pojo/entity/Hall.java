package pojo.entity;

import lombok.Data;

@Data
/**
 *  Hall实体类
 */
public class Hall {
    private Long id;
    private String name;
    private Integer rows;
    private Integer cols;
    private Long cinemaId;
}
