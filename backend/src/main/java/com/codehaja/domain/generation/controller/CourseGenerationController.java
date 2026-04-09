package com.codehaja.domain.generation.controller;

import com.codehaja.common.api.ApiResponse;
import com.codehaja.domain.generation.dto.CourseGenerationDto;
import com.codehaja.domain.generation.entity.GenerationOutput;
import com.codehaja.domain.generation.entity.GenerationTaskStatus;
import com.codehaja.domain.generation.entity.LectureContentBatch;
import com.codehaja.domain.generation.entity.LectureContentTask;
import com.codehaja.domain.generation.repository.GenerationOutputRepository;
import com.codehaja.domain.generation.repository.LectureContentBatchRepository;
import com.codehaja.domain.generation.repository.LectureContentTaskRepository;
import com.codehaja.domain.generation.service.CourseContentGenerationService;
import com.codehaja.domain.generation.service.CourseGenerationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseGenerationController {

    private static final Logger log = LoggerFactory.getLogger(CourseGenerationController.class);
    private final CourseGenerationService courseGenerationService;
    private final CourseContentGenerationService contentGenerationService;
    private final LectureContentTaskRepository taskRepository;
    private final LectureContentBatchRepository batchRepository;
    private final GenerationOutputRepository generationOutputRepository;

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<?>> generateCourse(
            @RequestBody CourseGenerationDto.GenerateRequest request
    ) {
        try {
            CourseGenerationDto.GenerateResponse response = courseGenerationService.generateCourse(request);
            return ResponseEntity.ok(ApiResponse.ok("Course draft generated successfully.", response));
        } catch (Exception e) {
            log.error("Course generation failed", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("AI_500", "Generation failed: " + e.getMessage()));
        }
    }

    /**
     * Generate a new section by AI and add it to an existing course.
     */
    @PostMapping("/generate/section")
    public ResponseEntity<ApiResponse<?>> addSection(
            @RequestBody CourseGenerationDto.AddSectionRequest request) {
        try {
            var response = courseGenerationService.addSection(request);
            return ResponseEntity.ok(ApiResponse.ok("Section generated successfully.", response));
        } catch (Exception e) {
            log.error("Section generation failed", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("AI_500", "Section generation failed: " + e.getMessage()));
        }
    }

    /**
     * Generate a new lecture by AI and add it to an existing section.
     */
    @PostMapping("/generate/lecture")
    public ResponseEntity<ApiResponse<?>> addLecture(
            @RequestBody CourseGenerationDto.AddLectureRequest request) {
        try {
            var response = courseGenerationService.addLecture(request);
            return ResponseEntity.ok(ApiResponse.ok("Lecture generated successfully.", response));
        } catch (Exception e) {
            log.error("Lecture generation failed", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("AI_500", "Lecture generation failed: " + e.getMessage()));
        }
    }

    /**
     * Generate a new lecture item by AI and add it to an existing lecture.
     */
    @PostMapping("/generate/item")
    public ResponseEntity<ApiResponse<?>> addItem(
            @RequestBody CourseGenerationDto.AddItemRequest request) {
        try {
            var response = courseGenerationService.addItem(request);
            return ResponseEntity.ok(ApiResponse.ok("Item generated successfully.", response));
        } catch (Exception e) {
            log.error("Item generation failed", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("AI_500", "Item generation failed: " + e.getMessage()));
        }
    }

    /**
     * Retry all FAILED and PARTIALLY_COMPLETED tasks for a job (async).
     */
    @PostMapping("/generate/retry/job/{jobId}")
    public ResponseEntity<ApiResponse<?>> retryFailedTasks(@PathVariable Long jobId) {
        List<LectureContentTask> retryableTasks = taskRepository.findByJobIdAndStatusIn(
                jobId, List.of(GenerationTaskStatus.FAILED, GenerationTaskStatus.PARTIALLY_COMPLETED));
        if (retryableTasks.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.ok("No failed/partial tasks to retry.", Map.of("retryCount", 0)));
        }
        contentGenerationService.retryFailedTasksAsync(jobId);
        return ResponseEntity.ok(ApiResponse.ok(
                "Retrying " + retryableTasks.size() + " tasks.",
                Map.of("retryCount", retryableTasks.size(), "jobId", jobId)
        ));
    }

    /**
     * Retry a single FAILED task.
     */
    @PostMapping("/generate/retry/task/{taskId}")
    public ResponseEntity<ApiResponse<?>> retrySingleTask(@PathVariable Long taskId) {
        try {
            contentGenerationService.retrySingleTask(taskId);
            return ResponseEntity.ok(ApiResponse.ok("Task retried successfully.", Map.of("taskId", taskId)));
        } catch (Exception e) {
            log.error("Task retry failed for taskId={}", taskId, e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("RETRY_FAILED", e.getMessage()));
        }
    }

    /**
     * Regenerate content for a single lecture by lectureId.
     * Works standalone — no jobId needed.
     */
    @PostMapping("/generate/lecture/{lectureId}")
    public ResponseEntity<ApiResponse<?>> regenerateLecture(@PathVariable Long lectureId) {
        try {
            contentGenerationService.regenerateLecture(lectureId);
            return ResponseEntity.ok(ApiResponse.ok("Lecture content regenerated.", Map.of("lectureId", lectureId)));
        } catch (Exception e) {
            log.error("Lecture regeneration failed for lectureId={}", lectureId, e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("REGEN_FAILED", e.getMessage()));
        }
    }

    /**
     * Re-convert stored raw AI output through the fixed ContentConverter without calling AI again.
     */
    @PostMapping("/generate/reconvert/lecture/{lectureId}")
    public ResponseEntity<ApiResponse<?>> reconvertLecture(@PathVariable Long lectureId) {
        try {
            int count = contentGenerationService.reconvertLecture(lectureId);
            var result = new LinkedHashMap<String, Object>();
            result.put("lectureId", lectureId);
            result.put("reconvertedItems", count);
            return ResponseEntity.ok(ApiResponse.ok("Lecture content re-converted.", result));
        } catch (Exception e) {
            log.error("Reconvert failed for lectureId={}", lectureId, e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("RECONVERT_FAILED", e.getMessage()));
        }
    }

    /**
     * Regenerate content for a single lecture item.
     * Returns the generation output (prompt, raw AI output, parse strategy) for inspection.
     */
    @PostMapping("/generate/item/{itemId}")
    public ResponseEntity<ApiResponse<?>> regenerateItem(@PathVariable Long itemId) {
        try {
            var output = contentGenerationService.regenerateItem(itemId);
            var result = new LinkedHashMap<String, Object>();
            result.put("itemId", itemId);
            result.put("success", output != null && Boolean.TRUE.equals(output.getSuccess()));
            if (output != null) {
                result.put("outputId", output.getId());
                result.put("parseStrategy", output.getParseStrategy() != null ? output.getParseStrategy().name() : null);
                result.put("finishReason", output.getFinishReason());
                result.put("promptTokens", output.getPromptTokens());
                result.put("completionTokens", output.getCandidatesTokens());
                result.put("latencyMs", output.getLatencyMs());
            }
            return ResponseEntity.ok(ApiResponse.ok("Item content regenerated.", result));
        } catch (Exception e) {
            log.error("Item regeneration failed for itemId={}", itemId, e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("ITEM_REGEN_FAILED", e.getMessage()));
        }
    }

    /**
     * Get generation output details for inspection/debugging.
     * Shows the system prompt, user prompt, raw AI output, and parse strategy.
     */
    @GetMapping("/generate/output/{outputId}")
    public ResponseEntity<ApiResponse<?>> getGenerationOutput(@PathVariable Long outputId) {
        GenerationOutput output = generationOutputRepository.findById(outputId).orElse(null);
        if (output == null) {
            return ResponseEntity.status(404)
                    .body(ApiResponse.error("NOT_FOUND", "Generation output not found: " + outputId));
        }
        var result = new LinkedHashMap<String, Object>();
        result.put("outputId", output.getId());
        result.put("taskType", output.getTaskType() != null ? output.getTaskType().name() : null);
        result.put("modelName", output.getModelName());
        result.put("systemPrompt", output.getSystemPrompt());
        result.put("userPrompt", output.getUserPrompt());
        result.put("rawOutput", output.getRawOutput());
        result.put("parsedOutput", output.getParsedOutput());
        result.put("parseStrategy", output.getParseStrategy() != null ? output.getParseStrategy().name() : null);
        result.put("finishReason", output.getFinishReason());
        result.put("promptTokens", output.getPromptTokens());
        result.put("candidatesTokens", output.getCandidatesTokens());
        result.put("thinkingTokens", output.getThinkingTokens());
        result.put("latencyMs", output.getLatencyMs());
        result.put("estimatedCostUsd", output.getEstimatedCostUsd());
        result.put("success", output.getSuccess());
        result.put("errorMessage", output.getErrorMessage());
        result.put("createdAt", output.getCreatedAt());
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * Retry a single FAILED batch.
     */
    @PostMapping("/generate/retry/batch/{batchId}")
    public ResponseEntity<ApiResponse<?>> retrySingleBatch(@PathVariable Long batchId) {
        try {
            contentGenerationService.retrySingleBatch(batchId);
            return ResponseEntity.ok(ApiResponse.ok("Batch retried.", Map.of("batchId", batchId)));
        } catch (Exception e) {
            log.error("Batch retry failed for batchId={}", batchId, e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("BATCH_RETRY_FAILED", e.getMessage()));
        }
    }

    /**
     * Get all tasks for a job (to see which failed / succeeded).
     */
    @GetMapping("/generate/job/{jobId}/tasks")
    public ResponseEntity<ApiResponse<?>> getJobTasks(@PathVariable Long jobId) {
        List<LectureContentTask> tasks = taskRepository.findByJobIdOrderBySectionIdAscLectureIdAsc(jobId);
        var summary = tasks.stream().map(t -> {
            var m = new LinkedHashMap<String, Object>();
            m.put("taskId", t.getId());
            m.put("lectureId", t.getLectureId());
            m.put("lectureTitle", t.getLectureTitle() != null ? t.getLectureTitle() : "");
            m.put("sectionTitle", t.getSectionTitle() != null ? t.getSectionTitle() : "");
            m.put("status", t.getStatus().name());
            m.put("itemsTotal", t.getItemsTotal() != null ? t.getItemsTotal() : 0);
            m.put("itemsMatched", t.getItemsMatched() != null ? t.getItemsMatched() : 0);
            m.put("retryCount", t.getRetryCount() != null ? t.getRetryCount() : 0);
            m.put("errorMessage", t.getErrorMessage() != null ? t.getErrorMessage() : "");
            m.put("generationMode", t.getGenerationMode() != null ? t.getGenerationMode() : "SINGLE");
            m.put("totalBatches", t.getTotalBatches() != null ? t.getTotalBatches() : 0);
            m.put("completedBatches", t.getCompletedBatches() != null ? t.getCompletedBatches() : 0);
            m.put("failedBatches", t.getFailedBatches() != null ? t.getFailedBatches() : 0);
            return m;
        }).toList();
        return ResponseEntity.ok(ApiResponse.ok(summary));
    }

    /**
     * Get all batches for a task.
     */
    @GetMapping("/generate/task/{taskId}/batches")
    public ResponseEntity<ApiResponse<?>> getTaskBatches(@PathVariable Long taskId) {
        List<LectureContentBatch> batches = batchRepository.findByTaskIdOrderByBatchIndexAsc(taskId);
        var summary = batches.stream().map(b -> {
            var m = new LinkedHashMap<String, Object>();
            m.put("batchId", b.getId());
            m.put("batchIndex", b.getBatchIndex());
            m.put("itemsInBatch", b.getItemsInBatch());
            m.put("itemTypes", b.getItemTypes());
            m.put("itemTitles", b.getItemTitles());
            m.put("itemsMatched", b.getItemsMatched() != null ? b.getItemsMatched() : 0);
            m.put("status", b.getStatus().name());
            m.put("maxOutputTokens", b.getMaxOutputTokens());
            m.put("retryCount", b.getRetryCount() != null ? b.getRetryCount() : 0);
            m.put("errorMessage", b.getErrorMessage() != null ? b.getErrorMessage() : "");
            m.put("latencyMs", b.getLatencyMs());
            m.put("promptTokens", b.getPromptTokens());
            m.put("completionTokens", b.getCompletionTokens());
            return m;
        }).toList();
        return ResponseEntity.ok(ApiResponse.ok(summary));
    }
}
