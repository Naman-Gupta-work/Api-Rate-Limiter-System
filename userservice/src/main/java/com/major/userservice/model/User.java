package com.major.userservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name="users")
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    private String role;

    private String resetOtp;

    private LocalDateTime resetOtpExpiry;

    @Enumerated(EnumType.STRING)
    private Plan plan;

    @Column(unique = true, nullable = false)
    private String apiKey;

    // Automatically generate defaults before saving to the database
    @PrePersist
    public void prePersist() {
        if (this.apiKey == null) {
            this.apiKey = UUID.randomUUID().toString();
        }
        if (this.plan == null) {
            this.plan = Plan.FREE;
        }
        if (this.role == null) {
            this.role = "USER";
        }
    }
}