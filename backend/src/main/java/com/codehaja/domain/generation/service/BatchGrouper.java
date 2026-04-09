package com.codehaja.domain.generation.service;

import com.codehaja.domain.lectureitem.entity.LectureItem;
import com.codehaja.domain.lectureitem.entity.LectureItemType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Groups lecture items into generation batches.
 * <p>
 * Rules (type-based):
 * - QUIZ_SET, CHECKPOINT, CODING_SET → always solo (1 item per batch)
 * - RICH_TEXT → max 2 consecutive items per batch
 */
@Component
public class BatchGrouper {

    public record Batch(
            int batchIndex,
            List<LectureItem> items,
            int estimatedTokens,
            int maxOutputTokens
    ) {}

    /**
     * Group lecture items (already sorted by sortOrder) into generation batches.
     */
    public List<Batch> groupIntoBatches(List<LectureItem> items) {
        List<Batch> batches = new ArrayList<>();
        List<LectureItem> current = new ArrayList<>();
        int currentEstimate = 0;
        int batchIdx = 0;

        for (LectureItem item : items) {
            int itemEstimate = estimateTokenBudget(item);

            if (mustBeSolo(item.getItemType())) {
                // Flush current RICH_TEXT accumulator first
                if (!current.isEmpty()) {
                    batches.add(new Batch(batchIdx++, List.copyOf(current),
                            currentEstimate, computeMaxOutputTokens(currentEstimate)));
                    current.clear();
                    currentEstimate = 0;
                }
                // Solo batch
                batches.add(new Batch(batchIdx++, List.of(item),
                        itemEstimate, computeMaxOutputTokens(itemEstimate)));
            } else {
                // RICH_TEXT — max 2 per batch
                if (current.size() >= 2) {
                    batches.add(new Batch(batchIdx++, List.copyOf(current),
                            currentEstimate, computeMaxOutputTokens(currentEstimate)));
                    current.clear();
                    currentEstimate = 0;
                }
                current.add(item);
                currentEstimate += itemEstimate;
            }
        }

        if (!current.isEmpty()) {
            batches.add(new Batch(batchIdx, List.copyOf(current),
                    currentEstimate, computeMaxOutputTokens(currentEstimate)));
        }
        return batches;
    }

    /**
     * QUIZ_SET, CHECKPOINT, CODING_SET must always be solo.
     */
    private boolean mustBeSolo(LectureItemType type) {
        return type == LectureItemType.QUIZ_SET
                || type == LectureItemType.CHECKPOINT
                || type == LectureItemType.CODING_SET;
    }

    static int estimateTokenBudget(LectureItem item) {
        return switch (item.getItemType()) {
            case RICH_TEXT -> 15_000;
            case QUIZ_SET -> 13_000;
            case CHECKPOINT -> 7_000;
            case CODING_SET -> 16_000;
            default -> 8_000;
        };
    }

    /**
     * Compute the maxOutputTokens for the AI call.
     * Adds 25% headroom, rounds up to nearest 1024, caps at 40960.
     */
    static int computeMaxOutputTokens(int estimatedTokens) {
        int withHeadroom = (int) (estimatedTokens * 1.25);
        int rounded = ((withHeadroom + 1023) / 1024) * 1024;
        return Math.min(rounded, 40_960);
    }
}
