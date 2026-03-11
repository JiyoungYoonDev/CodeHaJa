package com.codehaja.dto;

import lombok.Getter;
import lombok.Setter;
import tools.jackson.databind.JsonNode;

public class ProjectDto {
    @Getter
    @Setter
    public static class CreateRequest {
        private String title;
        private JsonNode description;
        private String answer;
        private String hint;
        private String skeletonCode;
        private int difficulty;
    }

    @Getter
    @Setter
    public static class Response {
        private long id;
        private String title;
        private JsonNode description;
        private String skeletonCode;
        private boolean isSolved;
        private String githubTemplateUrl;
        private int difficulty;
    }

    @Getter
    @Setter
    public static class Summary {
        private long id;
        private String title;
        private boolean isSolved;
        private String type;
    }
}
