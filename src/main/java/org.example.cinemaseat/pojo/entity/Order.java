package org.example.cinemaseat.pojo.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Order {
    private Long id;
    private Long userId;
    private Long scheduleId;
    private String status; // PENDING / PAID / CANCELLED
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}
