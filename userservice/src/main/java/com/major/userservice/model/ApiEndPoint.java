package com.major.userservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "path"})
        }
)
@Getter
@Setter
public class ApiEndPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String path;

    private String targetUrl;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;


}
