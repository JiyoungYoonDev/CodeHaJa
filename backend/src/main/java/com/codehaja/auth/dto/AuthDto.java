package com.codehaja.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

public class AuthDto {

    @Getter
    @Setter
    public static class SignupRequest {
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        private String email;

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, max = 72, message = "비밀번호는 8자 이상 72자 이하여야 합니다.")
        private String password;

        @NotBlank(message = "이름은 필수입니다.")
        private String name;
    }

    @Getter
    @Setter
    public static class LoginRequest {
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        private String email;

        @NotBlank(message = "비밀번호는 필수입니다.")
        private String password;
    }

    @Getter
    @Setter
    public static class GoogleLoginRequest {
        @NotBlank(message = "Google ID 토큰은 필수입니다.")
        private String idToken;
    }

    @Getter
    @Setter
    public static class ForgotPasswordRequest {
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        private String email;
    }

    @Getter
    @Setter
    public static class ResetPasswordRequest {
        @NotBlank(message = "토큰은 필수입니다.")
        private String token;

        @NotBlank(message = "새 비밀번호는 필수입니다.")
        @Size(min = 8, max = 72, message = "비밀번호는 8자 이상 72자 이하여야 합니다.")
        private String newPassword;
    }

    @Getter
    @Setter
    @Builder
    public static class MeResponse {
        private Long id;
        private String email;
        private String name;
        private String profileImage;
        private String role;
        private String provider;
        // Gamification
        private int totalXp;
        private String tier;
        private int hearts;
        private LocalDateTime heartsRefillAt;
        private int streakDays;
    }
}
