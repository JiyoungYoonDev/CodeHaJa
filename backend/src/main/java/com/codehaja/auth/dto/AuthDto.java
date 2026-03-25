package com.codehaja.auth.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

public class AuthDto {

    @Getter
    @Setter
    public static class SignupRequest {
        private String email;
        private String password;
        private String name;
    }

    @Getter
    @Setter
    public static class LoginRequest {
        private String email;
        private String password;
    }

    @Getter
    @Setter
    public static class GoogleLoginRequest {
        private String idToken;
    }

    @Getter
    @Setter
    public static class ForgotPasswordRequest {
        private String email;
    }

    @Getter
    @Setter
    public static class ResetPasswordRequest {
        private String token;
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
    }
}
