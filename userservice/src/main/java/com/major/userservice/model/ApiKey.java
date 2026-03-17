package com.major.userservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "api_keys")
@Getter
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String apiKey;

    @ManyToOne
    @JoinColumn(name = "user_id")
    @Setter
    private User user;

    @Setter
    private boolean active;

    public ApiKey() {
        this.apiKey = UUID.randomUUID().toString();
        this.active = true;
    }

}
