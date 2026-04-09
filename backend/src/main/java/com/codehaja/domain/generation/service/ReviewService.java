package com.codehaja.domain.generation.service;

import com.codehaja.domain.generation.entity.*;
import com.codehaja.domain.generation.repository.CourseGenerationJobRepository;
import com.codehaja.domain.generation.repository.GenerationDiffRepository;
import com.codehaja.domain.generation.repository.ReviewSessionRepository;
import com.codehaja.domain.lectureitem.entity.LectureItem;
import com.codehaja.domain.lectureitem.repository.LectureItemRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Phase 3: Review workflow for generated content.
 * Admins can approve, reject, or request changes on generation jobs.
 * Diffs are stored when content is manually edited after generation.
 */
@Service
@RequiredArgsConstructor
public class ReviewService {

    private static final Logger log = LoggerFactory.getLogger(ReviewService.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final ReviewSessionRepository reviewSessionRepository;
    private final GenerationDiffRepository diffRepository;
    private final CourseGenerationJobRepository jobRepository;
    private final LectureItemRepository lectureItemRepository;

    /**
     * Create a review session (approve, reject, request changes).
     */
    @Transactional
    public ReviewSession createReview(Long jobId, Long reviewerId, ReviewAction action, String comments) {
        CourseGenerationJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));

        ReviewSession session = new ReviewSession();
        session.setJob(job);
        session.setReviewerId(reviewerId);
        session.setAction(action);
        session.setComments(comments);
        session.setReviewedAt(LocalDateTime.now());

        session = reviewSessionRepository.save(session);
        log.info("Review created: job={}, action={}, reviewer={}", jobId, action, reviewerId);
        return session;
    }

    /**
     * Get all reviews for a job.
     */
    public List<ReviewSession> getReviewsForJob(Long jobId) {
        return reviewSessionRepository.findByJobIdOrderByReviewedAtDesc(jobId);
    }

    /**
     * Record a content diff when an admin edits a lecture item's content.
     * Call this BEFORE saving the new content.
     */
    @Transactional
    public GenerationDiff recordContentEdit(Long itemId, JsonNode newContent) {
        LectureItem item = lectureItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found: " + itemId));

        JsonNode oldContent = item.getContentJson();
        if (oldContent == null || oldContent.equals(newContent)) {
            return null; // No diff needed
        }

        // Find the job that generated this content (via lecture → task → job)
        CourseGenerationJob job = findJobForItem(item);
        if (job == null) return null;

        // Compute simple diff summary
        String diffJson = computeDiffSummary(oldContent, newContent);

        GenerationDiff diff = new GenerationDiff();
        diff.setJob(job);
        diff.setDiffType(DiffType.CONTENT);
        diff.setDiffJson(diffJson);

        diff = diffRepository.save(diff);
        log.info("Content diff recorded: item={}, job={}", itemId, job.getId());
        return diff;
    }

    /**
     * Get all diffs for a job.
     */
    public List<GenerationDiff> getDiffsForJob(Long jobId) {
        return diffRepository.findByJobIdOrderByCreatedAtDesc(jobId);
    }

    private CourseGenerationJob findJobForItem(LectureItem item) {
        // Items don't directly link to jobs — would need LectureContentTask → Job query
        // For now, return null if not directly linkable
        return null;
    }

    private String computeDiffSummary(JsonNode before, JsonNode after) {
        try {
            ObjectNode diff = mapper.createObjectNode();
            diff.put("beforeLength", before.toString().length());
            diff.put("afterLength", after.toString().length());
            diff.put("sizeChange", after.toString().length() - before.toString().length());

            // Count structural differences
            int beforeNodes = countNodes(before);
            int afterNodes = countNodes(after);
            diff.put("beforeNodes", beforeNodes);
            diff.put("afterNodes", afterNodes);
            diff.put("nodeChange", afterNodes - beforeNodes);

            return mapper.writeValueAsString(diff);
        } catch (Exception e) {
            return "{\"error\": \"diff computation failed\"}";
        }
    }

    private int countNodes(JsonNode node) {
        if (node == null) return 0;
        if (node.isArray()) {
            int count = 0;
            for (JsonNode child : node) count += countNodes(child);
            return count;
        }
        if (node.isObject()) {
            int count = 1;
            var it = node.properties();
            for (var entry : it) count += countNodes(entry.getValue());
            return count;
        }
        return 1;
    }
}
