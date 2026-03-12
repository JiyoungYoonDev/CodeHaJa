package com.codehaja.domain.anonymous.entity;

import com.codehaja.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "anonymous_users")
@Getter
@Setter
public class AnonymousUser extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "anonymous_user_key", nullable = false, unique = true, length = 100)
    private String anonymousUserKey;

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;
}