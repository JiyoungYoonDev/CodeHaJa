package com.codehaja.domain.anonymous.dto;

import lombok.Getter;
import lombok.Setter;

public class AnonymousUserDto {

    @Getter
    @Setter
    public static class InitRequest {
        private String anonymousUserKey;
    }

    @Getter
    @Setter
    public static class Response {
        private Long id;
        private String anonymousUserKey;
    }
}