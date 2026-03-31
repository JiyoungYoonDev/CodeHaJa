package com.codehaja.domain.subscription.dto;

import com.codehaja.domain.subscription.entity.SubscriptionPlan;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

public class SubscriptionDto {

    @Getter
    @Builder
    public static class SubscriptionInfo {
        private Long id;
        private String plan;
        private String status;
        private LocalDateTime startedAt;
        private LocalDateTime expiresAt;
    }

    @Getter
    @Builder
    public static class SubscriptionListItem {
        private Long id;
        private Long userId;
        private String userName;
        private String userEmail;
        private String plan;
        private String status;
        private LocalDateTime startedAt;
        private LocalDateTime expiresAt;
        private LocalDateTime createdAt;
    }

    @Getter
    @Setter
    public static class GrantRequest {
        private Long userId;
        private SubscriptionPlan plan;
        private LocalDateTime expiresAt;
    }
}
