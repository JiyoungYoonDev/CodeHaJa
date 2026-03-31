package com.codehaja.auth.entity;

import com.codehaja.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AuthProvider provider = AuthProvider.LOCAL;

    private String providerId;

    private String name;

    private String profileImage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UserRole role = UserRole.USER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    private String refreshTokenHash;
    private LocalDateTime refreshTokenExpiresAt;

    private String passwordResetToken;
    private LocalDateTime passwordResetTokenExpiresAt;

    // Gamification
    @Builder.Default
    private int totalXp = 0;

    @Builder.Default
    private int hearts = 5;

    private LocalDateTime heartsRefillAt;

    @Builder.Default
    private int streakDays = 0;

    private LocalDate lastActivityDate;
}
