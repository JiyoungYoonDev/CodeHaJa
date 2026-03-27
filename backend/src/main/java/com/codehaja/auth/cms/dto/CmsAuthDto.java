package com.codehaja.auth.cms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

public class CmsAuthDto {

    @Getter
    @Setter
    public static class LoginRequest {
        @NotBlank
        @Email
        private String email;

        @NotBlank
        private String password;
    }

    @Getter
    @Setter
    @Builder
    public static class MeResponse {
        private Long id;
        private String email;
        private String name;
    }
}
