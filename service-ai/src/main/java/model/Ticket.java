package com.mindx.service_ai.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "tickets")
@Data
public class Ticket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;   // which user raised this ticket
    private String query;
    private String status; // OPEN / RESOLVED / NEEDS_HUMAN
    private LocalDateTime createdAt;
}