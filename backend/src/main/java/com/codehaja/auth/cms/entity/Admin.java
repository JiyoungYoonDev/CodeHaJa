package com.codehaja.auth.cms.entity;

import com.codehaja.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "admins")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Admin extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    private String name;

    @Builder.Default
    @Column(nullable = false)
    private int failedAttempts = 0;

    private LocalDateTime lockedUntil;

    private String refreshTokenHash;
    private LocalDateTime refreshTokenExpiresAt;
}
