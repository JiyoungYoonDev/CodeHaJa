package com.codehaja.dto;
import lombok.Getter;
import lombok.Setter;
import tools.jackson.databind.JsonNode;

@Getter
@Setter
public class ProblemDto {
    @Getter
    @Setter
    public static class CreateRequest {
        private long bookId;
        private String title;
        private JsonNode content;
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
        private JsonNode content;
        private String skeletonCode;
        private boolean isSolved;
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
