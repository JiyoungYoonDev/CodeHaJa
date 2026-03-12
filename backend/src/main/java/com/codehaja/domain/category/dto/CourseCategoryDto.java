package com.codehaja.domain.category.dto;


import lombok.Getter;
import lombok.Setter;

public class CourseCategoryDto {
    @Getter
    @Setter
    public static class Request {
        private String categoryName;
    }

    @Getter
    @Setter
    public static class Response {
        private Long id;
        private String categoryName;
        private Long courseCount;
    }
}
