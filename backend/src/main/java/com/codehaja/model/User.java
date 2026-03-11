package com.codehaja.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    
    @Column(name = "email", nullable = false, length = 50)
    private String userEmail;

    @Column(name = "user_name", nullable = false, length = 50)
    private String userName;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "user_password", nullable = false, length = 50)
    private String password;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name= "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "user_token", nullable = false, length = 255)
    private String userToken;
    
    @Column(name="provider", nullable = false, length = 50)
    private String provider;
}
