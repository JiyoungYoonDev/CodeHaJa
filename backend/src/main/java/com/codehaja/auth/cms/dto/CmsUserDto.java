package com.codehaja.auth.cms.dto;

import com.codehaja.auth.entity.UserRole;
import com.codehaja.auth.entity.UserStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

public class CmsUserDto {

    @Getter
    @Builder
    public static class UserListItem {
        private Long id;
        private String name;
        private String email;
        private String role;
        private String status;
        private String provider;
        private LocalDateTime createdAt;
        private long enrollmentCount;
        // subscription
        private boolean subscribed;
        private String subscriptionPlan;
        private String subscriptionStatus;
        private LocalDateTime subscriptionExpiresAt;
    }

    @Getter
    @Setter
    public static class RoleUpdateRequest {
        private UserRole role;
    }

    @Getter
    @Setter
    public static class StatusUpdateRequest {
        private UserStatus status;
    }
}
