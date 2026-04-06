package com.mindx.service_ai.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password; // plain text for now (no BCrypt to keep it simple)

    private String name;
    private String phone;
    private String address;

    @Column(updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}