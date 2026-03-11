package com.codehaja.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

public class LectureDto {
    @Getter
    @Setter
    public static class Response {
        private long id;
        private String title;
        private List<ProblemDto.Summary> problems;
        private List<ProjectDto.Summary> projects;
    }
}
