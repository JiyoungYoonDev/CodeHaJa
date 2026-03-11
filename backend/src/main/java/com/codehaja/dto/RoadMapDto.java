package com.codehaja.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

public class RoadMapDto {
    @Getter
    @Setter
    public static class Response {
        private long id;
        private String name;
        private int orderIndex;
        private List<LectureDto> lectures;
    }
}
