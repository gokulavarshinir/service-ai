package com.mindx.service_ai.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Data
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String orderId; // e.g. ORD-1001

    @Column(nullable = false)
    private Long userId;

    private String productName;
    private String status;       // PLACED, DISPATCHED, SHIPPED, DELIVERED, CANCELLED
    private String trackingId;
    private String courierName;
    private Double amount;
    private LocalDateTime orderDate;
    private LocalDateTime estimatedDelivery;
}