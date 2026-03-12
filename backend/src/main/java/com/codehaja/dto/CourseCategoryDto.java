package com.codehaja.dto;

import lombok.Getter;
import lombok.Setter;

public class CourseCategoryDto {
    @Getter
    @Setter
    public static class Request{
        private String categoryName;
    }

    @Getter
    @Setter
    public static class Response{
        private String categoryName;
        private Long id;
        private Long courseCount;
    }
}