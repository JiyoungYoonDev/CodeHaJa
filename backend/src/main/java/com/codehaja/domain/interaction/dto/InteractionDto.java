package com.codehaja.domain.interaction.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

public class InteractionDto {

    @Getter
    public static class HintRequest {
        private Integer hintIndex;
    }

    @Getter
    @AllArgsConstructor
    public static class HintStatusResponse {
        private Long lectureItemId;
        private List<Integer> revealedIndexes;
        private int usedCount;
        private int xpDeducted;
    }

    @Getter
    public static class ReactionRequest {
        private String reactionType; // "LIKE" or "DISLIKE"
    }

    @Getter
    @AllArgsConstructor
    public static class ReactionStatusResponse {
        private Long lectureItemId;
        private String userReaction; // "LIKE", "DISLIKE", or null
        private long likeCount;
        private long dislikeCount;
    }
}
