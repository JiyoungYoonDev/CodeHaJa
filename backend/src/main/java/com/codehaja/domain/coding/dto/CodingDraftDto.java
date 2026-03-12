package com.codehaja.domain.coding.dto;

import lombok.Getter;
import lombok.Setter;

public class CodingDraftDto {

    @Getter
    @Setter
    public static class SaveRequest {
        private String anonymousUserKey;
        private String sourceCode;
        private String language;
    }

    @Getter
    @Setter
    public static class Response {
        private Long id;
        private Long lectureItemEntryId;
        private String sourceCode;
        private String language;
    }
}