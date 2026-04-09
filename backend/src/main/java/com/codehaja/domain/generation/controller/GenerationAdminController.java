package com.codehaja.domain.generation.controller;

import com.codehaja.common.api.ApiResponse;
import com.codehaja.domain.generation.entity.*;
import com.codehaja.domain.generation.service.GenerationMetricsService;
import com.codehaja.domain.generation.service.PromptTemplateService;
import com.codehaja.domain.generation.service.ReviewService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Unified admin controller for generation management:
 * - Phase 2: Prompt templates, validation
 * - Phase 3: Reviews, diffs
 * - Phase 4: Eval dashboard, metrics
 */
@RestController
@RequestMapping("/api/generation/admin")
@RequiredArgsConstructor
public class GenerationAdminController {

    private static final Logger log = LoggerFactory.getLogger(GenerationAdminController.class);

    private final PromptTemplateService promptTemplateService;
    private final ReviewService reviewService;
    private final GenerationMetricsService metricsService;

    // ── Phase 2: Prompt Template Management ──

    @GetMapping("/templates")
    public ResponseEntity<ApiResponse<?>> listTemplates() {
        List<PromptTemplate> templates = promptTemplateService.listTemplates();
        var result = templates.stream().map(t -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", t.getId());
            m.put("name", t.getName());
            m.put("taskType", t.getTaskType().name());
            m.put("description", t.getDescription());
            m.put("status", t.getStatus().name());
            return m;
        }).toList();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/templates/{templateId}/versions")
    public ResponseEntity<ApiResponse<?>> listVersions(@PathVariable Long templateId) {
        List<PromptTemplateVersion> versions = promptTemplateService.listVersions(templateId);
        var result = versions.stream().map(v -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", v.getId());
            m.put("versionNumber", v.getVersionNumber());
            m.put("isActive", v.getIsActive());
            m.put("changeNotes", v.getChangeNotes());
            m.put("createdBy", v.getCreatedBy());
            m.put("contentLength", v.getContent() != null ? v.getContent().length() : 0);
            m.put("createdAt", v.getCreatedAt());
            return m;
        }).toList();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/templates/{templateId}/versions/active")
    public ResponseEntity<ApiResponse<?>> getActiveVersion(@PathVariable Long templateId) {
        PromptTemplateVersion version = promptTemplateService.getActiveVersion(templateId);
        if (version == null) {
            return ResponseEntity.ok(ApiResponse.ok("No active version.", null));
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", version.getId());
        m.put("versionNumber", version.getVersionNumber());
        m.put("content", version.getContent());
        m.put("variables", version.getVariables());
        m.put("changeNotes", version.getChangeNotes());
        m.put("createdBy", version.getCreatedBy());
        m.put("isActive", version.getIsActive());
        return ResponseEntity.ok(ApiResponse.ok(m));
    }

    @PostMapping("/templates/{templateId}/versions")
    public ResponseEntity<ApiResponse<?>> createVersion(
            @PathVariable Long templateId,
            @RequestBody Map<String, String> body) {
        String content = body.get("content");
        String changeNotes = body.get("changeNotes");
        String createdBy = body.getOrDefault("createdBy", "admin");
        PromptTemplateVersion version = promptTemplateService.createVersion(templateId, content, changeNotes, createdBy);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", version.getId());
        m.put("versionNumber", version.getVersionNumber());
        m.put("isActive", version.getIsActive());
        return ResponseEntity.ok(ApiResponse.ok("Version created.", m));
    }

    @GetMapping("/templates/versions/{versionId}")
    public ResponseEntity<ApiResponse<?>> getVersionDetail(@PathVariable Long versionId) {
        PromptTemplateVersion version = promptTemplateService.getVersionDetail(versionId);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", version.getId());
        m.put("versionNumber", version.getVersionNumber());
        m.put("content", version.getContent());
        m.put("variables", version.getVariables());
        m.put("changeNotes", version.getChangeNotes());
        m.put("createdBy", version.getCreatedBy());
        m.put("isActive", version.getIsActive());
        m.put("contentLength", version.getContent() != null ? version.getContent().length() : 0);
        m.put("createdAt", version.getCreatedAt());
        return ResponseEntity.ok(ApiResponse.ok(m));
    }

    @PostMapping("/templates/versions/{versionId}/activate")
    public ResponseEntity<ApiResponse<?>> activateVersion(@PathVariable Long versionId) {
        PromptTemplateVersion version = promptTemplateService.activateVersion(versionId);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", version.getId());
        m.put("versionNumber", version.getVersionNumber());
        m.put("isActive", version.getIsActive());
        return ResponseEntity.ok(ApiResponse.ok("Version activated.", m));
    }

    @GetMapping("/templates/versions/{versionId}/metrics")
    public ResponseEntity<ApiResponse<?>> getVersionMetrics(@PathVariable Long versionId) {
        return ResponseEntity.ok(ApiResponse.ok(promptTemplateService.getVersionMetrics(versionId)));
    }

    @GetMapping("/templates/versions/compare")
    public ResponseEntity<ApiResponse<?>> compareVersions(
            @RequestParam Long versionA, @RequestParam Long versionB) {
        return ResponseEntity.ok(ApiResponse.ok(promptTemplateService.compareVersions(versionA, versionB)));
    }

    // ── Phase 2: Validation Results ──

    @GetMapping("/validations/output/{outputId}")
    public ResponseEntity<ApiResponse<?>> getOutputValidations(@PathVariable Long outputId) {
        return ResponseEntity.ok(ApiResponse.ok(metricsService.getOutputValidations(outputId)));
    }

    @GetMapping("/validations/overview")
    public ResponseEntity<ApiResponse<?>> getValidationOverview() {
        return ResponseEntity.ok(ApiResponse.ok(metricsService.getValidationOverview()));
    }

    // ── Phase 3: Review Workflow ──

    @PostMapping("/reviews")
    public ResponseEntity<ApiResponse<?>> createReview(@RequestBody Map<String, Object> body) {
        Long jobId = ((Number) body.get("jobId")).longValue();
        Long reviewerId = body.get("reviewerId") != null ? ((Number) body.get("reviewerId")).longValue() : null;
        ReviewAction action = ReviewAction.valueOf((String) body.get("action"));
        String comments = (String) body.get("comments");

        ReviewSession session = reviewService.createReview(jobId, reviewerId, action, comments);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", session.getId());
        m.put("jobId", jobId);
        m.put("action", session.getAction().name());
        m.put("reviewedAt", session.getReviewedAt());
        return ResponseEntity.ok(ApiResponse.ok("Review created.", m));
    }

    @GetMapping("/reviews/job/{jobId}")
    public ResponseEntity<ApiResponse<?>> getJobReviews(@PathVariable Long jobId) {
        List<ReviewSession> reviews = reviewService.getReviewsForJob(jobId);
        var result = reviews.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("reviewerId", r.getReviewerId());
            m.put("action", r.getAction().name());
            m.put("comments", r.getComments());
            m.put("reviewedAt", r.getReviewedAt());
            return m;
        }).toList();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // ── Phase 3: Content Diffs ──

    @PostMapping("/diffs/record")
    public ResponseEntity<ApiResponse<?>> recordContentEdit(@RequestBody Map<String, Object> body) {
        try {
            Long itemId = ((Number) body.get("itemId")).longValue();
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            JsonNode newContent = mapper.valueToTree(body.get("newContent"));

            GenerationDiff diff = reviewService.recordContentEdit(itemId, newContent);
            if (diff == null) {
                return ResponseEntity.ok(ApiResponse.ok("No diff needed (content unchanged or no linked job).", null));
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("diffId", diff.getId());
            m.put("diffType", diff.getDiffType().name());
            return ResponseEntity.ok(ApiResponse.ok("Diff recorded.", m));
        } catch (Exception e) {
            log.error("Failed to record content diff", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("DIFF_FAILED", e.getMessage()));
        }
    }

    @GetMapping("/diffs/job/{jobId}")
    public ResponseEntity<ApiResponse<?>> getJobDiffs(@PathVariable Long jobId) {
        List<GenerationDiff> diffs = reviewService.getDiffsForJob(jobId);
        var result = diffs.stream().map(d -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", d.getId());
            m.put("diffType", d.getDiffType().name());
            m.put("diffJson", d.getDiffJson());
            m.put("createdAt", d.getCreatedAt());
            return m;
        }).toList();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // ── Phase 4: Eval Dashboard & Metrics ──

    @GetMapping("/dashboard/overview")
    public ResponseEntity<ApiResponse<?>> getDashboardOverview() {
        return ResponseEntity.ok(ApiResponse.ok(metricsService.getDashboardOverview()));
    }

    @GetMapping("/dashboard/jobs")
    public ResponseEntity<ApiResponse<?>> getJobsSummary() {
        return ResponseEntity.ok(ApiResponse.ok(metricsService.getJobsSummary()));
    }

    @GetMapping("/dashboard/jobs/{jobId}")
    public ResponseEntity<ApiResponse<?>> getJobDetail(@PathVariable Long jobId) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(metricsService.getJobDetail(jobId)));
        } catch (Exception e) {
            return ResponseEntity.status(404)
                    .body(ApiResponse.error("NOT_FOUND", e.getMessage()));
        }
    }

    @PatchMapping("/batches/{batchId}/status")
    public ResponseEntity<ApiResponse<?>> updateBatchStatus(
            @PathVariable Long batchId,
            @RequestBody Map<String, String> body) {
        String status = body.get("status");
        if (status == null || status.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("BAD_REQUEST", "status is required"));
        }
        return ResponseEntity.ok(ApiResponse.ok("Batch status updated.", metricsService.updateBatchStatus(batchId, status)));
    }

    @PatchMapping("/tasks/{taskId}/status")
    public ResponseEntity<ApiResponse<?>> updateTaskStatus(
            @PathVariable Long taskId,
            @RequestBody Map<String, String> body) {
        String status = body.get("status");
        if (status == null || status.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("BAD_REQUEST", "status is required"));
        }
        return ResponseEntity.ok(ApiResponse.ok("Task status updated.", metricsService.updateTaskStatus(taskId, status)));
    }
}
