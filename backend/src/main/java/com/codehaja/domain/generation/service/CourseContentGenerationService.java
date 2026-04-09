package com.codehaja.domain.generation.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.codehaja.domain.generation.config.GenerationSchemas;
import com.codehaja.domain.generation.config.PromptTemplateNames;
import com.codehaja.domain.generation.dto.AiGenerationResult;
import com.codehaja.domain.generation.dto.CourseGenerationDto;
import com.codehaja.domain.generation.entity.*;
import com.codehaja.domain.generation.repository.CourseGenerationJobRepository;
import com.codehaja.domain.generation.repository.GenerationOutputRepository;
import com.codehaja.domain.generation.repository.LectureContentBatchRepository;
import com.codehaja.domain.generation.repository.LectureContentTaskRepository;
import com.codehaja.domain.lecture.entity.Lecture;
import com.codehaja.domain.lecture.repository.LectureRepository;
import com.codehaja.domain.lectureitem.entity.LectureItem;
import com.codehaja.domain.lectureitem.repository.LectureItemRepository;
import com.codehaja.domain.section.entity.CourseSection;
import com.codehaja.domain.section.repository.CourseSectionRepository;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CourseContentGenerationService {

    private static final Logger log = LoggerFactory.getLogger(CourseContentGenerationService.class);
    private static final int MAX_PARTIAL_RETRIES = 3;
    private static final ObjectMapper objectMapper;
    static {
        com.fasterxml.jackson.core.StreamReadConstraints constraints =
                com.fasterxml.jackson.core.StreamReadConstraints.builder()
                        .maxNumberLength(Integer.MAX_VALUE)
                        .maxStringLength(Integer.MAX_VALUE)
                        .build();
        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.getFactory().setStreamReadConstraints(constraints);
    }

    private final AiClient aiClient;
    private final CoursePromptBuilder promptBuilder;
    private final PromptContentProvider promptContentProvider;
    private final TopicClassifier topicClassifier;
    private final ContentConverter contentConverter;
    private final GenerationOutputLogger outputLogger;
    private final BatchGrouper batchGrouper;
    private final CourseSectionRepository courseSectionRepository;
    private final LectureRepository lectureRepository;
    private final LectureItemRepository lectureItemRepository;
    private final CourseGenerationJobRepository generationJobRepository;
    private final LectureContentTaskRepository lectureContentTaskRepository;
    private final LectureContentBatchRepository lectureContentBatchRepository;
    private final GenerationOutputRepository generationOutputRepository;

    // ── Retry failed tasks ──

    /**
     * Retry all FAILED/PARTIALLY_COMPLETED tasks for a given job. Runs async.
     * For BATCHED tasks, only failed batches are retried.
     */
    @Async
    public void retryFailedTasksAsync(Long jobId) {
        CourseGenerationJob job = generationJobRepository.findById(jobId).orElse(null);
        if (job == null) {
            log.warn("Job {} not found for retry", jobId);
            return;
        }

        List<LectureContentTask> failedTasks = lectureContentTaskRepository
                .findByJobIdAndStatusIn(jobId, List.of(GenerationTaskStatus.FAILED, GenerationTaskStatus.PARTIALLY_COMPLETED));
        if (failedTasks.isEmpty()) {
            log.info("No failed/partial tasks to retry for job {}", jobId);
            return;
        }

        log.info("Retrying {} failed/partial tasks for job {}", failedTasks.size(), jobId);
        job.setStatus(GenerationJobStatus.IN_PROGRESS);
        generationJobRepository.save(job);

        TopicCategory topicCategory = topicClassifier.classify(job.getTopic());
        CourseGenerationDto.GenerateRequest request = buildRetryRequest(job);

        int retried = 0, succeeded = 0, failed = 0;
        for (LectureContentTask task : failedTasks) {
            retried++;
            try {
                retryTask(task, topicCategory, request, job);
                succeeded++;
            } catch (Exception e) {
                failed++;
                log.error("Retry failed for task {} (lecture '{}'): {}",
                        task.getId(), task.getLectureTitle(), e.getMessage());
            }
        }

        // Update job status
        long totalIncomplete = lectureContentTaskRepository.countByJobIdAndStatusIn(
                jobId, List.of(GenerationTaskStatus.FAILED, GenerationTaskStatus.PARTIALLY_COMPLETED));
        job.setFailedLectures((int) totalIncomplete);
        job.setStatus(totalIncomplete == 0 ? GenerationJobStatus.COMPLETED : GenerationJobStatus.PARTIALLY_COMPLETED);
        generationJobRepository.save(job);

        log.info("Retry finished for job {}. Retried: {}, succeeded: {}, still failed: {}",
                jobId, retried, succeeded, failed);
    }

    /**
     * Retry a single failed task by ID.
     * For BATCHED tasks, only re-runs failed batches.
     */
    @Transactional
    public void retrySingleTask(Long taskId) {
        LectureContentTask task = lectureContentTaskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found: " + taskId));

        if (task.getStatus() != GenerationTaskStatus.FAILED && task.getStatus() != GenerationTaskStatus.PARTIALLY_COMPLETED) {
            throw new RuntimeException("Task " + taskId + " is not retryable (current: " + task.getStatus() + ")");
        }

        CourseGenerationJob job = task.getJob();
        TopicCategory topicCategory = topicClassifier.classify(job.getTopic());
        CourseGenerationDto.GenerateRequest request = buildRetryRequest(job);

        retryTask(task, topicCategory, request, job);

        // Update job counters
        long totalIncomplete = lectureContentTaskRepository.countByJobIdAndStatusIn(
                job.getId(), List.of(GenerationTaskStatus.FAILED, GenerationTaskStatus.PARTIALLY_COMPLETED));
        job.setFailedLectures((int) totalIncomplete);
        if (totalIncomplete == 0 && job.getStatus() == GenerationJobStatus.PARTIALLY_COMPLETED) {
            job.setStatus(GenerationJobStatus.COMPLETED);
        }
        generationJobRepository.save(job);
    }

    /**
     * Retry a single failed batch by ID.
     */
    @Transactional
    public void retrySingleBatch(Long batchId) {
        LectureContentBatch batch = lectureContentBatchRepository.findById(batchId)
                .orElseThrow(() -> new RuntimeException("Batch not found: " + batchId));

        if (batch.getStatus() != GenerationTaskStatus.FAILED
                && batch.getStatus() != GenerationTaskStatus.PARTIALLY_COMPLETED) {
            throw new RuntimeException("Batch " + batchId + " is not retryable (current: " + batch.getStatus() + ")");
        }

        LectureContentTask task = batch.getTask();
        CourseGenerationJob job = task.getJob();
        Lecture lecture = lectureRepository.findById(task.getLectureId())
                .orElseThrow(() -> new RuntimeException("Lecture not found: " + task.getLectureId()));
        CourseSection section = courseSectionRepository.findById(task.getSectionId())
                .orElseThrow(() -> new RuntimeException("Section not found: " + task.getSectionId()));

        TopicCategory topicCategory = topicClassifier.classify(job.getTopic());
        CourseGenerationDto.GenerateRequest request = buildRetryRequest(job);

        // Resolve which items are in this batch
        List<LectureItem> allItems = lectureItemRepository.findAllByLectureIdOrderBySortOrderAsc(lecture.getId());
        List<String> batchTitles = parseBatchTitles(batch.getItemTitles());
        List<LectureItem> batchItems = allItems.stream()
                .filter(i -> batchTitles.contains(i.getTitle()))
                .toList();

        if (batchItems.isEmpty()) {
            throw new RuntimeException("No matching items found for batch " + batchId);
        }

        // Build full outline for context
        var allOutlines = new ArrayList<CoursePromptBuilder.ItemOutline>();
        for (LectureItem item : allItems) {
            allOutlines.add(new CoursePromptBuilder.ItemOutline(item.getTitle(), item.getItemType().name(), item.getDescription()));
        }

        String systemPrompt = promptBuilder.buildContentSystemPrompt(topicCategory);
        String modelName = getModelName();
        PromptTemplateVersion promptVersion = promptContentProvider.getActiveVersion(PromptTemplateNames.LECTURE_CONTENT_SYSTEM_BASE).orElse(null);

        int prevRetryCount = batch.getRetryCount() != null ? batch.getRetryCount() : 0;

        // Preserve original batch as non-leaf history, create child for retry
        batch.setIsLeaf(false);
        batch.setSplitReason("MANUAL_RETRY");
        lectureContentBatchRepository.save(batch);

        int nextIndex = getNextBatchIndex(task);
        int tokenEstimate = batchItems.stream()
                .mapToInt(BatchGrouper::estimateTokenBudget)
                .sum();

        LectureContentBatch retryBatch = new LectureContentBatch();
        retryBatch.setTask(task);
        retryBatch.setBatchIndex(nextIndex);
        retryBatch.setItemTitles(batch.getItemTitles());
        retryBatch.setItemTypes(batch.getItemTypes());
        retryBatch.setItemsInBatch(batchItems.size());
        retryBatch.setItemsMatched(0);
        retryBatch.setStatus(GenerationTaskStatus.PENDING);
        retryBatch.setRetryCount(prevRetryCount + 1);
        retryBatch.setMaxOutputTokens(BatchGrouper.computeMaxOutputTokens(tokenEstimate));
        retryBatch.setParentBatch(batch);
        retryBatch.setIsLeaf(true);
        retryBatch = lectureContentBatchRepository.save(retryBatch);

        if (task.getTotalBatches() != null) {
            task.setTotalBatches(task.getTotalBatches() + 1);
            lectureContentTaskRepository.save(task);
        }

        generateSingleBatch(
                retryBatch, batchItems, systemPrompt, allOutlines,
                task.getTotalBatches() != null ? task.getTotalBatches() : 1,
                task, job, request, modelName,
                section.getTitle(), lecture.getTitle(), promptVersion);

        // Re-aggregate task from all batches
        List<LectureContentBatch> allBatches = lectureContentBatchRepository.findByTaskIdOrderByBatchIndexAsc(task.getId());
        aggregateTaskFromBatches(task, allBatches, job);

        // Update job counters
        long totalIncomplete = lectureContentTaskRepository.countByJobIdAndStatusIn(
                job.getId(), List.of(GenerationTaskStatus.FAILED, GenerationTaskStatus.PARTIALLY_COMPLETED));
        job.setFailedLectures((int) totalIncomplete);
        if (totalIncomplete == 0 && job.getStatus() == GenerationJobStatus.PARTIALLY_COMPLETED) {
            job.setStatus(GenerationJobStatus.COMPLETED);
        }
        generationJobRepository.save(job);
    }

    private void retryTask(LectureContentTask task, TopicCategory topicCategory,
                           CourseGenerationDto.GenerateRequest request, CourseGenerationJob job) {
        Lecture lecture = lectureRepository.findById(task.getLectureId()).orElse(null);
        CourseSection section = courseSectionRepository.findById(task.getSectionId()).orElse(null);
        if (lecture == null || section == null) {
            throw new RuntimeException("Lecture or section not found for task " + task.getId());
        }

        // For BATCHED tasks, only retry failed batches
        if ("BATCHED".equals(task.getGenerationMode())) {
            retryTaskBatches(task, lecture, section, topicCategory, request, job);
            return;
        }

        // Legacy SINGLE mode — full regeneration using batch mode
        task.setStatus(GenerationTaskStatus.IN_PROGRESS);
        task.setRetryCount((task.getRetryCount() != null ? task.getRetryCount() : 0) + 1);
        task.setErrorMessage(null);
        lectureContentTaskRepository.save(task);

        List<LectureItem> items = lectureItemRepository.findAllByLectureIdOrderBySortOrderAsc(lecture.getId());
        if (items.isEmpty()) {
            task.setStatus(GenerationTaskStatus.SKIPPED);
            lectureContentTaskRepository.save(task);
            return;
        }

        // Upgrade to BATCHED mode on retry
        List<BatchGrouper.Batch> batches = batchGrouper.groupIntoBatches(items);
        task.setGenerationMode("BATCHED");
        task.setItemsTotal(items.size());
        task.setTotalBatches(batches.size());
        task.setCompletedBatches(0);
        task.setFailedBatches(0);
        lectureContentTaskRepository.save(task);

        var allOutlines = new ArrayList<CoursePromptBuilder.ItemOutline>();
        for (LectureItem item : items) {
            allOutlines.add(new CoursePromptBuilder.ItemOutline(item.getTitle(), item.getItemType().name(), item.getDescription()));
        }

        String systemPrompt = promptBuilder.buildContentSystemPrompt(topicCategory);
        String modelName = getModelName();
        PromptTemplateVersion promptVersion = promptContentProvider.getActiveVersion(PromptTemplateNames.LECTURE_CONTENT_SYSTEM_BASE).orElse(null);

        log.info("  Retrying lecture '{}' in BATCHED mode ({} batches), attempt #{}",
                lecture.getTitle(), batches.size(), task.getRetryCount());

        List<LectureContentBatch> batchEntities = new ArrayList<>();
        for (BatchGrouper.Batch batch : batches) {
            LectureContentBatch batchEntity = new LectureContentBatch();
            batchEntity.setTask(task);
            batchEntity.setBatchIndex(batch.batchIndex());
            batchEntity.setItemTitles(serializeTitles(batch.items()));
            batchEntity.setItemTypes(serializeTypes(batch.items()));
            batchEntity.setItemsInBatch(batch.items().size());
            batchEntity.setItemsMatched(0);
            batchEntity.setStatus(GenerationTaskStatus.PENDING);
            batchEntity.setRetryCount(0);
            batchEntity.setMaxOutputTokens(batch.maxOutputTokens());
            batchEntity = lectureContentBatchRepository.save(batchEntity);
            batchEntities.add(batchEntity);
        }

        for (int i = 0; i < batches.size(); i++) {
            try {
                generateSingleBatch(
                        batchEntities.get(i), batches.get(i).items(),
                        systemPrompt, allOutlines, batches.size(),
                        task, job, request, modelName,
                        section.getTitle(), lecture.getTitle(), promptVersion);
            } catch (Exception e) {
                log.error("  Retry batch {}/{} failed: {}", i + 1, batches.size(), e.getMessage());
            }
        }

        aggregateTaskFromBatches(task, batchEntities, job);
    }

    /**
     * Retry only the failed batches within a BATCHED task.
     */
    private void retryTaskBatches(LectureContentTask task, Lecture lecture, CourseSection section,
                                   TopicCategory topicCategory,
                                   CourseGenerationDto.GenerateRequest request,
                                   CourseGenerationJob job) {
        task.setStatus(GenerationTaskStatus.IN_PROGRESS);
        task.setRetryCount((task.getRetryCount() != null ? task.getRetryCount() : 0) + 1);
        task.setErrorMessage(null);
        lectureContentTaskRepository.save(task);

        List<LectureContentBatch> allBatches = lectureContentBatchRepository.findByTaskIdOrderByBatchIndexAsc(task.getId());
        List<LectureContentBatch> failedBatches = allBatches.stream()
                .filter(b -> b.getStatus() == GenerationTaskStatus.FAILED
                        || b.getStatus() == GenerationTaskStatus.PARTIALLY_COMPLETED)
                .toList();

        if (failedBatches.isEmpty()) {
            log.info("  No failed batches to retry for task {}", task.getId());
            aggregateTaskFromBatches(task, allBatches, job);
            return;
        }

        List<LectureItem> allItems = lectureItemRepository.findAllByLectureIdOrderBySortOrderAsc(lecture.getId());
        var allOutlines = new ArrayList<CoursePromptBuilder.ItemOutline>();
        for (LectureItem item : allItems) {
            allOutlines.add(new CoursePromptBuilder.ItemOutline(item.getTitle(), item.getItemType().name(), item.getDescription()));
        }

        String systemPrompt = promptBuilder.buildContentSystemPrompt(topicCategory);
        String modelName = getModelName();
        PromptTemplateVersion promptVersion = promptContentProvider.getActiveVersion(PromptTemplateNames.LECTURE_CONTENT_SYSTEM_BASE).orElse(null);
        int totalBatches = task.getTotalBatches() != null ? task.getTotalBatches() : allBatches.size();

        log.info("  Retrying {} failed batches for lecture '{}'", failedBatches.size(), lecture.getTitle());

        for (LectureContentBatch batch : failedBatches) {
            List<String> batchTitles = parseBatchTitles(batch.getItemTitles());
            List<LectureItem> batchItems = allItems.stream()
                    .filter(i -> batchTitles.contains(i.getTitle()))
                    .toList();

            if (batchItems.isEmpty()) {
                log.warn("  Batch {} has no matching items, skipping", batch.getBatchIndex());
                continue;
            }

            int prevRetry = batch.getRetryCount() != null ? batch.getRetryCount() : 0;

            // Preserve original batch as non-leaf history
            batch.setIsLeaf(false);
            batch.setSplitReason("TASK_RETRY");
            lectureContentBatchRepository.save(batch);

            int nextIdx = getNextBatchIndex(task);
            int tokenEst = batchItems.stream()
                    .mapToInt(BatchGrouper::estimateTokenBudget)
                    .sum();

            LectureContentBatch retryBatch = new LectureContentBatch();
            retryBatch.setTask(task);
            retryBatch.setBatchIndex(nextIdx);
            retryBatch.setItemTitles(batch.getItemTitles());
            retryBatch.setItemTypes(batch.getItemTypes());
            retryBatch.setItemsInBatch(batchItems.size());
            retryBatch.setItemsMatched(0);
            retryBatch.setStatus(GenerationTaskStatus.PENDING);
            retryBatch.setRetryCount(prevRetry + 1);
            retryBatch.setMaxOutputTokens(BatchGrouper.computeMaxOutputTokens(tokenEst));
            retryBatch.setParentBatch(batch);
            retryBatch.setIsLeaf(true);
            retryBatch = lectureContentBatchRepository.save(retryBatch);

            if (task.getTotalBatches() != null) {
                task.setTotalBatches(task.getTotalBatches() + 1);
                lectureContentTaskRepository.save(task);
            }

            try {
                generateSingleBatch(
                        retryBatch, batchItems, systemPrompt, allOutlines, totalBatches,
                        task, job, request, modelName,
                        section.getTitle(), lecture.getTitle(), promptVersion);
            } catch (Exception e) {
                log.error("  Retry batch {}/{} failed: {}", retryBatch.getBatchIndex() + 1, totalBatches, e.getMessage());
            }
        }

        // Re-fetch all batches after retries created new ones
        allBatches = lectureContentBatchRepository.findByTaskIdOrderByBatchIndexAsc(task.getId());
        aggregateTaskFromBatches(task, allBatches, job);
    }

    private CourseGenerationDto.GenerateRequest buildRetryRequest(CourseGenerationJob job) {
        CourseGenerationDto.GenerateRequest req = new CourseGenerationDto.GenerateRequest();
        req.setTopic(job.getTopic());
        return req;
    }

    /**
     * Extract the AI-generated text from the full Gemini API raw response body.
     * Structure: { "candidates": [{ "content": { "parts": [{ "text": "..." }] } }] }
     * Falls back to the raw string if it's already plain content (not a Gemini envelope).
     */
    private String extractTextFromRawOutput(String raw) {
        if (raw == null || raw.isBlank()) return raw;
        String trimmed = raw.strip();
        // If it starts with '[' it's already the content array, not a Gemini envelope
        if (trimmed.startsWith("[")) return raw;
        try {
            JsonNode root = objectMapper.readTree(raw);
            JsonNode text = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
            if (!text.isMissingNode() && text.isTextual()) {
                return text.asText();
            }
        } catch (Exception ignored) {}
        return raw;
    }

    private List<String> parseBatchTitles(String itemTitlesJson) {
        try {
            return objectMapper.readValue(itemTitlesJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * Regenerate content for a single lecture by lectureId.
     * Uses batch mode. Creates a new task with fresh batches.
     */
    @Transactional
    public void regenerateLecture(Long lectureId) {
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new RuntimeException("Lecture not found: " + lectureId));
        CourseSection section = lecture.getCourseSection();
        String courseTopic = section.getCourse().getTitle();

        TopicCategory topicCategory = topicClassifier.classify(courseTopic);
        CourseGenerationDto.GenerateRequest request = new CourseGenerationDto.GenerateRequest();
        request.setTopic(courseTopic);

        // Find existing task or create one
        LectureContentTask task = lectureContentTaskRepository.findTopByLectureIdOrderByIdDesc(lectureId)
                .orElseGet(() -> {
                    LectureContentTask t = new LectureContentTask();
                    t.setLectureId(lectureId);
                    t.setLectureTitle(lecture.getTitle());
                    t.setSectionId(section.getId());
                    t.setSectionTitle(section.getTitle());
                    return t;
                });

        // Link to existing job or create a lightweight one (validate job still exists in DB)
        CourseGenerationJob job = null;
        if (task.getJob() != null) {
            job = generationJobRepository.findById(task.getJob().getId()).orElse(null);
        }
        if (job == null) {
            job = new CourseGenerationJob();
            job.setTopic(courseTopic);
            job.setModelName(getModelName());
            job.setStatus(GenerationJobStatus.IN_PROGRESS);
            job.setCourseId(section.getCourse().getId());
            job.setCourseTitle(courseTopic);
            job = generationJobRepository.save(job);
            task.setJob(job);
        }

        task.setStatus(GenerationTaskStatus.IN_PROGRESS);
        task.setRetryCount((task.getRetryCount() != null ? task.getRetryCount() : 0) + 1);
        task.setErrorMessage(null);
        task = lectureContentTaskRepository.save(task);

        retryTask(task, topicCategory, request, job);
    }

    /**
     * Regenerate content for a SINGLE lecture item by itemId.
     * Creates a 1-item batch, generates, and returns the generation output for inspection.
     */
    @Transactional
    public GenerationOutput regenerateItem(Long itemId) {
        LectureItem item = lectureItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("LectureItem not found: " + itemId));
        Lecture lecture = item.getLecture();
        CourseSection section = lecture.getCourseSection();
        String courseTopic = section.getCourse().getTitle();

        TopicCategory topicCategory = topicClassifier.classify(courseTopic);
        CourseGenerationDto.GenerateRequest request = new CourseGenerationDto.GenerateRequest();
        request.setTopic(courseTopic);

        // Find or create task
        LectureContentTask task = lectureContentTaskRepository.findTopByLectureIdOrderByIdDesc(lecture.getId())
                .orElseGet(() -> {
                    LectureContentTask t = new LectureContentTask();
                    t.setLectureId(lecture.getId());
                    t.setLectureTitle(lecture.getTitle());
                    t.setSectionId(section.getId());
                    t.setSectionTitle(section.getTitle());
                    return t;
                });

        // Validate job exists in DB (may have been deleted)
        CourseGenerationJob job = null;
        if (task.getJob() != null) {
            job = generationJobRepository.findById(task.getJob().getId()).orElse(null);
        }
        if (job == null) {
            job = new CourseGenerationJob();
            job.setTopic(courseTopic);
            job.setModelName(getModelName());
            job.setStatus(GenerationJobStatus.IN_PROGRESS);
            job.setCourseId(section.getCourse().getId());
            job.setCourseTitle(courseTopic);
            job = generationJobRepository.save(job);
            task.setJob(job);
        }
        task.setStatus(GenerationTaskStatus.IN_PROGRESS);
        task.setRetryCount((task.getRetryCount() != null ? task.getRetryCount() : 0) + 1);
        task = lectureContentTaskRepository.save(task);

        // Build full lecture outline for context
        List<LectureItem> allItems = lectureItemRepository.findAllByLectureIdOrderBySortOrderAsc(lecture.getId());
        var allOutlines = new ArrayList<CoursePromptBuilder.ItemOutline>();
        for (LectureItem li : allItems) {
            allOutlines.add(new CoursePromptBuilder.ItemOutline(li.getTitle(), li.getItemType().name(), li.getDescription()));
        }
        // Find old batch for this item to inherit its batchIndex (preserve order)
        int regenBatchIndex = item.getSortOrder() != null ? item.getSortOrder() : 0;
        List<LectureContentBatch> existingBatches = lectureContentBatchRepository.findByTaskIdOrderByBatchIndexAsc(task.getId());
        for (LectureContentBatch old : existingBatches) {
            if (old.getItemTitles() != null && old.getItemTitles().contains(item.getTitle())
                    && old.getIsLeaf() != null && old.getIsLeaf()) {
                regenBatchIndex = old.getBatchIndex();
                break;
            }
        }

        // Create a 1-item batch entity for tracking
        LectureContentBatch batchEntity = new LectureContentBatch();
        batchEntity.setTask(task);
        batchEntity.setBatchIndex(regenBatchIndex);
        batchEntity.setItemTitles(serializeTitles(List.of(item)));
        batchEntity.setItemTypes(item.getItemType().name());
        batchEntity.setItemsInBatch(1);
        batchEntity.setItemsMatched(0);
        batchEntity.setStatus(GenerationTaskStatus.PENDING);
        batchEntity.setRetryCount(0);
        batchEntity.setMaxOutputTokens(computeItemTokenLimit(item));
        batchEntity = lectureContentBatchRepository.save(batchEntity);

        // Generate
        String systemPrompt = promptBuilder.buildContentSystemPrompt(topicCategory);
        String modelName = getModelName();
        PromptTemplateVersion promptVersion = promptContentProvider.getActiveVersion(PromptTemplateNames.LECTURE_CONTENT_SYSTEM_BASE).orElse(null);

        generateSingleBatch(
                batchEntity, List.of(item), systemPrompt, allOutlines,
                1, task, job, request, modelName,
                section.getTitle(), lecture.getTitle(), promptVersion);

        // Link new batch to old failed batch: old becomes non-leaf (hidden from aggregate),
        // new batch becomes the leaf with parentBatch set (so UI shows correct attempt count)
        {
            List<LectureContentBatch> oldBatches = lectureContentBatchRepository.findByTaskIdOrderByBatchIndexAsc(task.getId());
            for (LectureContentBatch old : oldBatches) {
                if (old.getId().equals(batchEntity.getId())) continue;
                if (old.getItemTitles() != null && old.getItemTitles().contains(item.getTitle())
                        && old.getIsLeaf() != null && old.getIsLeaf()) {
                    // Old batch becomes non-leaf (excluded from aggregate + main table)
                    old.setIsLeaf(false);
                    lectureContentBatchRepository.save(old);
                    // New batch links to old as parent (ancestor chain → Attempts count)
                    batchEntity.setParentBatch(old);
                    lectureContentBatchRepository.save(batchEntity);
                    break; // only link to the most recent old batch
                }
            }
        }

        // Re-aggregate task status from all batches
        List<LectureContentBatch> allBatches = lectureContentBatchRepository.findByTaskIdOrderByBatchIndexAsc(task.getId());
        aggregateTaskFromBatches(task, allBatches, job);

        // Recalculate job status
        if (job != null) {
            long totalIncomplete = lectureContentTaskRepository.countByJobIdAndStatusIn(
                    job.getId(), List.of(GenerationTaskStatus.FAILED, GenerationTaskStatus.PARTIALLY_COMPLETED));
            job.setFailedLectures((int) totalIncomplete);
            if (totalIncomplete == 0) {
                job.setStatus(GenerationJobStatus.COMPLETED);
            } else {
                job.setStatus(GenerationJobStatus.PARTIALLY_COMPLETED);
            }
            generationJobRepository.save(job);
        }

        log.info("Item {} '{}' regenerated. Status: {}", itemId, item.getTitle(), batchEntity.getStatus());
        return batchEntity.getOutput();
    }

    /**
     * Re-convert stored raw AI output through the (fixed) ContentConverter without calling the AI again.
     * Useful when ContentConverter had a bug and the content needs re-parsing.
     */
    @Transactional
    public int reconvertLecture(Long lectureId) {
        LectureContentTask task = lectureContentTaskRepository.findTopByLectureIdOrderByIdDesc(lectureId)
                .orElseThrow(() -> new RuntimeException("No task found for lecture " + lectureId));
        List<LectureContentBatch> batches = lectureContentBatchRepository
                .findByTaskIdOrderByBatchIndexAsc(task.getId());
        if (batches.isEmpty()) {
            throw new RuntimeException("No batches found for task " + task.getId());
        }

        List<LectureItem> allItems = lectureItemRepository.findAllByLectureIdOrderBySortOrderAsc(lectureId);
        int totalReconverted = 0;

        for (LectureContentBatch batch : batches) {
            GenerationOutput output = batch.getOutput();
            if (output == null || output.getRawOutput() == null || output.getRawOutput().isBlank()) {
                log.warn("  Batch {} has no stored output, skipping", batch.getBatchIndex());
                continue;
            }

            String rawText = extractTextFromRawOutput(output.getRawOutput());
            List<ContentEntry> entries = parseContentEntries(rawText);
            if (entries.isEmpty()) {
                log.warn("  Batch {} raw output could not be parsed, skipping", batch.getBatchIndex());
                continue;
            }

            List<String> batchTitles = parseBatchTitles(batch.getItemTitles());
            List<LectureItem> batchItems = allItems.stream()
                    .filter(i -> batchTitles.contains(i.getTitle()))
                    .toList();

            int matched = 0;
            List<String> matchedTitles = new ArrayList<>();
            for (LectureItem item : batchItems) {
                ContentEntry entry = findMatchingEntry(entries, item.getTitle());
                if (entry != null) {
                    String itemType = item.getItemType().name();
                    JsonNode converted;
                    if ("CODING_SET".equals(itemType) && entry.hasCodingContent()) {
                        converted = contentConverter.convertCodingStructured(entry.codingContent());
                    } else if ("QUIZ_SET".equals(itemType) && entry.hasQuizContent()) {
                        converted = contentConverter.convertQuizStructured(entry.quizContent());
                    } else {
                        converted = contentConverter.convert(itemType, entry.content());
                    }
                    item.setContentJson(converted);
                    lectureItemRepository.save(item);
                    matched++;
                    matchedTitles.add(item.getTitle());
                }
            }
            batch.setItemsMatched(matched);
            batch.setMatchedItemTitles(serializeTitlesList(matchedTitles));
            lectureContentBatchRepository.save(batch);
            totalReconverted += matched;
            log.info("  Reconvert batch {}: {}/{} items re-converted",
                    batch.getBatchIndex() + 1, matched, batchItems.size());
        }

        log.info("Reconvert lecture {}: {} items total re-converted", lectureId, totalReconverted);
        return totalReconverted;
    }

    private int computeItemTokenLimit(LectureItem item) {
        String title = item.getTitle() != null ? item.getTitle().toLowerCase() : "";
        int estimate = switch (item.getItemType()) {
            case RICH_TEXT -> 14_000;
            case QUIZ_SET -> 12_000;
            case CHECKPOINT -> title.contains("worked examples") ? 25_000 : 6_000;
            case CODING_SET -> 10_000;
            default -> 8_000;
        };
        int withHeadroom = (int) (estimate * 1.3);
        return Math.min(((withHeadroom + 1023) / 1024) * 1024, 40_960);
    }

    // ── Granular generation: single new lecture ──

    @Async
    public void generateContentForNewLectureAsync(Long lectureId, TopicCategory topicCategory, Long jobId) {
        log.info("Background content generation for new lectureId={}, jobId={}", lectureId, jobId);

        Lecture lecture = lectureRepository.findById(lectureId).orElse(null);
        if (lecture == null) {
            log.warn("Lecture {} not found, skipping content generation", lectureId);
            return;
        }
        CourseSection section = lecture.getCourseSection();
        CourseGenerationJob job = generationJobRepository.findById(jobId).orElse(null);

        CourseGenerationDto.GenerateRequest request = new CourseGenerationDto.GenerateRequest();
        request.setTopic(section.getCourse().getTitle());

        try {
            generateContentForLecture(lecture, section, topicCategory, request, job);
            if (job != null) {
                job.setCompletedLectures(1);
                job.setStatus(GenerationJobStatus.COMPLETED);
                generationJobRepository.save(job);
            }
        } catch (Exception e) {
            log.error("Content generation failed for new lectureId={}: {}", lectureId, e.getMessage(), e);
            if (job != null) {
                job.setFailedLectures(1);
                job.setStatus(GenerationJobStatus.FAILED);
                job.setErrorMessage(e.getMessage());
                generationJobRepository.save(job);
            }
        }
    }

    // ── Initial generation ──

    @Async
    public void generateContentAsync(Long courseId, List<Long> sectionIds,
                                     TopicCategory topicCategory,
                                     CourseGenerationDto.GenerateRequest request,
                                     Long jobId) {
        log.info("Background content generation started for courseId={}, sections={}, jobId={}",
                courseId, sectionIds.size(), jobId);

        CourseGenerationJob job = generationJobRepository.findById(jobId).orElse(null);

        int done = 0;
        int failed = 0;
        for (int i = 0; i < sectionIds.size(); i++) {
            Long sectionId = sectionIds.get(i);
            try {
                generateContentForSection(sectionId, topicCategory, request, i + 1, sectionIds.size(), job);
                done++;
            } catch (Exception e) {
                failed++;
                log.error("Content generation failed for sectionId={}: {}", sectionId, e.getMessage(), e);
            }
        }

        // Update job status
        if (job != null) {
            job.setCompletedLectures((job.getCompletedLectures() != null ? job.getCompletedLectures() : 0));
            job.setFailedLectures((job.getFailedLectures() != null ? job.getFailedLectures() : 0));
            job.setStatus(failed == 0 ? GenerationJobStatus.COMPLETED
                    : done == 0 ? GenerationJobStatus.FAILED
                    : GenerationJobStatus.PARTIALLY_COMPLETED);
            job.setCompletedAt(java.time.LocalDateTime.now());
            generationJobRepository.save(job);
        }

        log.info("Background content generation finished for courseId={}. {}/{} sections completed, {} failed.",
                courseId, done, sectionIds.size(), failed);
    }

    @Transactional
    public void generateContentForSection(Long sectionId, TopicCategory topicCategory,
                                          CourseGenerationDto.GenerateRequest request,
                                          int current, int total, CourseGenerationJob job) {
        CourseSection section = courseSectionRepository.findById(sectionId).orElse(null);
        if (section == null) {
            log.warn("Section {} not found, skipping", sectionId);
            return;
        }

        log.info("Generating content for section {}/{}: '{}'", current, total, section.getTitle());

        List<Lecture> lectures = lectureRepository.findAllByCourseSectionIdOrderBySortOrderAsc(sectionId);
        log.info("  Found {} lectures for section '{}'", lectures.size(), section.getTitle());

        if (lectures.isEmpty()) return;

        int lectureDone = 0;
        int lectureFailed = 0;
        for (Lecture lecture : lectures) {
            try {
                generateContentForLecture(lecture, section, topicCategory, request, job);
                lectureDone++;
                if (job != null) {
                    job.setCompletedLectures(
                            (job.getCompletedLectures() != null ? job.getCompletedLectures() : 0) + 1);
                    generationJobRepository.save(job);
                }
            } catch (Exception e) {
                lectureFailed++;
                if (job != null) {
                    job.setFailedLectures(
                            (job.getFailedLectures() != null ? job.getFailedLectures() : 0) + 1);
                    generationJobRepository.save(job);
                }
                log.error("  Content generation failed for lecture '{}': {}", lecture.getTitle(), e.getMessage(), e);
            }
        }

        log.info("Section '{}' content done: {}/{} lectures succeeded, {} failed",
                section.getTitle(), lectureDone, lectures.size(), lectureFailed);
    }

    private void generateContentForLecture(Lecture lecture, CourseSection section,
                                            TopicCategory topicCategory,
                                            CourseGenerationDto.GenerateRequest request,
                                            CourseGenerationJob job) {
        List<LectureItem> items = lectureItemRepository.findAllByLectureIdOrderBySortOrderAsc(lecture.getId());
        if (items.isEmpty()) {
            log.info("  Lecture '{}': no items, skipping", lecture.getTitle());
            return;
        }

        // Group items into batches
        List<BatchGrouper.Batch> batches = batchGrouper.groupIntoBatches(items);

        // Create task record
        LectureContentTask task = new LectureContentTask();
        task.setJob(job);
        task.setLectureId(lecture.getId());
        task.setLectureTitle(lecture.getTitle());
        task.setSectionId(section.getId());
        task.setSectionTitle(section.getTitle());
        task.setStatus(GenerationTaskStatus.IN_PROGRESS);
        task.setRetryCount(0);
        task.setItemsTotal(items.size());
        task.setItemsMatched(0);
        task.setGenerationMode("BATCHED");
        task.setTotalBatches(batches.size());
        task.setCompletedBatches(0);
        task.setFailedBatches(0);
        task = lectureContentTaskRepository.save(task);

        // Build full lecture outline for context in each batch prompt
        var allItemOutlines = new ArrayList<CoursePromptBuilder.ItemOutline>();
        for (LectureItem item : items) {
            allItemOutlines.add(new CoursePromptBuilder.ItemOutline(item.getTitle(), item.getItemType().name(), item.getDescription()));
        }

        String systemPrompt = promptBuilder.buildContentSystemPrompt(topicCategory);
        String modelName = getModelName();
        PromptTemplateVersion promptVersion = promptContentProvider.getActiveVersion(PromptTemplateNames.LECTURE_CONTENT_SYSTEM_BASE).orElse(null);

        log.info("  Generating content for lecture '{}' ({} items, {} batches)",
                lecture.getTitle(), items.size(), batches.size());

        // Create batch entities
        List<LectureContentBatch> batchEntities = new ArrayList<>();
        for (BatchGrouper.Batch batch : batches) {
            LectureContentBatch batchEntity = new LectureContentBatch();
            batchEntity.setTask(task);
            batchEntity.setBatchIndex(batch.batchIndex());
            batchEntity.setItemTitles(serializeTitles(batch.items()));
            batchEntity.setItemTypes(serializeTypes(batch.items()));
            batchEntity.setItemsInBatch(batch.items().size());
            batchEntity.setItemsMatched(0);
            batchEntity.setStatus(GenerationTaskStatus.PENDING);
            batchEntity.setRetryCount(0);
            batchEntity.setMaxOutputTokens(batch.maxOutputTokens());
            batchEntity = lectureContentBatchRepository.save(batchEntity);
            batchEntities.add(batchEntity);
        }

        // Generate each batch
        for (int i = 0; i < batches.size(); i++) {
            BatchGrouper.Batch batch = batches.get(i);
            LectureContentBatch batchEntity = batchEntities.get(i);

            try {
                generateSingleBatch(
                        batchEntity, batch.items(), systemPrompt, allItemOutlines,
                        batches.size(), task, job, request, modelName,
                        section.getTitle(), lecture.getTitle(), promptVersion);
            } catch (Exception e) {
                log.error("  Batch {}/{} failed for lecture '{}': {}",
                        i + 1, batches.size(), lecture.getTitle(), e.getMessage());
                // Batch entity already marked FAILED inside generateSingleBatch
            }
        }

        // Aggregate task status from batches
        aggregateTaskFromBatches(task, batchEntities, job);
    }

    /**
     * Generate content for a single batch of lecture items.
     */
    private void generateSingleBatch(
            LectureContentBatch batchEntity,
            List<LectureItem> batchItems,
            String systemPrompt,
            List<CoursePromptBuilder.ItemOutline> allItemOutlines,
            int totalBatches,
            LectureContentTask task,
            CourseGenerationJob job,
            CourseGenerationDto.GenerateRequest request,
            String modelName,
            String sectionTitle,
            String lectureTitle,
            PromptTemplateVersion promptVersion) {

        batchEntity.setStatus(GenerationTaskStatus.IN_PROGRESS);
        lectureContentBatchRepository.save(batchEntity);

        // Build batch-specific outlines
        var batchOutlines = new ArrayList<CoursePromptBuilder.ItemOutline>();
        for (LectureItem item : batchItems) {
            batchOutlines.add(new CoursePromptBuilder.ItemOutline(item.getTitle(), item.getItemType().name(), item.getDescription()));
        }

        String userPrompt = promptBuilder.buildBatchContentUserPrompt(
                sectionTitle, lectureTitle,
                allItemOutlines, batchOutlines,
                batchEntity.getBatchIndex(), totalBatches, request);

        log.info("    Batch {}/{}: {} items, maxTokens={}",
                batchEntity.getBatchIndex() + 1, totalBatches,
                batchItems.size(), batchEntity.getMaxOutputTokens());

        AiGenerationResult result;
        try {
            result = aiClient.generateStructured(
                    systemPrompt, userPrompt,
                    batchEntity.getMaxOutputTokens(),
                    GenerationSchemas.LECTURE_CONTENT);
        } catch (Exception e) {
            batchEntity.setStatus(GenerationTaskStatus.FAILED);
            batchEntity.setErrorMessage(e.getMessage());
            lectureContentBatchRepository.save(batchEntity);
            GenerationOutput failOutput = outputLogger.logBatchFailure(
                    modelName, systemPrompt, userPrompt, job, task, 0, e.getMessage(), promptVersion);
            failOutput.setBatch(batchEntity);
            generationOutputRepository.save(failOutput);
            throw e;
        }

        // Detect HTTP errors (429 rate limit, 500 server error, etc.)
        if (result.httpStatusCode() != 200) {
            String errMsg = "AI API error: HTTP " + result.httpStatusCode();
            batchEntity.setStatus(GenerationTaskStatus.FAILED);
            batchEntity.setErrorMessage(errMsg);
            batchEntity.setLatencyMs(result.latencyMs());
            lectureContentBatchRepository.save(batchEntity);
            log.error("    Batch {}/{} failed: {}", batchEntity.getBatchIndex() + 1, totalBatches, errMsg);
            return;
        }

        // Detect MAX_TOKENS truncation
        if ("MAX_TOKENS".equals(result.finishReason())) {
            String errMsg = "Truncated: MAX_TOKENS. Token limit was "
                    + batchEntity.getMaxOutputTokens()
                    + ". Response: " + (result.content() != null ? result.content().length() : 0) + " chars.";

            // Log the truncated output
            GenerationOutput truncOutput = outputLogger.logBatchSuccess(
                    result, modelName, systemPrompt, userPrompt, job, task,
                    OutputParseStrategy.MANUAL_EXTRACTION, promptVersion);
            truncOutput.setBatch(batchEntity);
            generationOutputRepository.save(truncOutput);

            batchEntity.setOutput(truncOutput);
            batchEntity.setLatencyMs(result.latencyMs());
            batchEntity.setPromptTokens(result.promptTokens());
            batchEntity.setCompletionTokens(result.candidatesTokens());
            batchEntity.setFinishReason(result.finishReason());
            batchEntity.setTruncated(true);

            if (job != null) {
                accumulateJobTokens(job, result);
                generationJobRepository.save(job);
            }

            // AUTO-SPLIT: multi-item batch → split into single-item child batches and retry
            if (batchItems.size() > 1) {
                batchEntity.setStatus(GenerationTaskStatus.SPLIT);
                batchEntity.setSplitReason("MAX_TOKENS");
                batchEntity.setIsLeaf(false);
                batchEntity.setErrorMessage(errMsg);
                lectureContentBatchRepository.save(batchEntity);

                log.warn("    Batch {}/{} truncated (MAX_TOKENS), auto-splitting {} items into individual batches",
                        batchEntity.getBatchIndex() + 1, totalBatches, batchItems.size());

                int nextBatchIndex = getNextBatchIndex(task);
                for (LectureItem item : batchItems) {
                    int itemEstimate = BatchGrouper.estimateTokenBudget(item);
                    LectureContentBatch childBatch = new LectureContentBatch();
                    childBatch.setTask(task);
                    childBatch.setBatchIndex(nextBatchIndex++);
                    childBatch.setItemTitles(serializeTitles(List.of(item)));
                    childBatch.setItemTypes(item.getItemType().name());
                    childBatch.setItemsInBatch(1);
                    childBatch.setItemsMatched(0);
                    childBatch.setStatus(GenerationTaskStatus.PENDING);
                    childBatch.setRetryCount(0);
                    childBatch.setMaxOutputTokens(BatchGrouper.computeMaxOutputTokens(itemEstimate));
                    childBatch.setParentBatch(batchEntity);
                    childBatch.setIsLeaf(true);
                    childBatch = lectureContentBatchRepository.save(childBatch);

                    try {
                        generateSingleBatch(
                                childBatch, List.of(item), systemPrompt, allItemOutlines,
                                totalBatches, task, job, request, modelName,
                                sectionTitle, lectureTitle, promptVersion);
                    } catch (Exception splitEx) {
                        log.error("    Child batch {} (item '{}') failed: {}",
                                childBatch.getBatchIndex(), item.getTitle(), splitEx.getMessage());
                    }
                }

                // Update task batch counts
                if (task.getTotalBatches() != null) {
                    task.setTotalBatches(task.getTotalBatches() + batchItems.size() - 1);
                    lectureContentTaskRepository.save(task);
                }
            } else {
                // Single-item batch — terminal failure, can't split further
                batchEntity.setStatus(GenerationTaskStatus.FAILED);
                batchEntity.setErrorMessage(errMsg);
                lectureContentBatchRepository.save(batchEntity);
                log.warn("    Batch {}/{} truncated (MAX_TOKENS) — single item, terminal failure",
                        batchEntity.getBatchIndex() + 1, totalBatches);
            }
            return;
        }

        String content = result.content();
        if (content == null || content.isBlank()) {
            batchEntity.setStatus(GenerationTaskStatus.FAILED);
            batchEntity.setErrorMessage("Empty response from AI");
            lectureContentBatchRepository.save(batchEntity);
            return;
        }

        log.info("    Batch {}/{} response: {} chars",
                batchEntity.getBatchIndex() + 1, totalBatches, content.length());

        // Parse content entries
        OutputParseStrategy strategy;
        List<ContentEntry> entries = parseContentEntries(result.content());

        log.info("    Batch {}/{}: parsed {} content entries", batchEntity.getBatchIndex() + 1, totalBatches, entries.size());
        for (ContentEntry e : entries) {
            log.info("      entry: title='{}', hasContent={}, hasCoding={}, hasQuiz={}",
                    e.itemTitle(), e.content() != null, e.hasCodingContent(), e.hasQuizContent());
        }

        if (result.structuredSchemaUsed() && !entries.isEmpty()) {
            strategy = OutputParseStrategy.STRUCTURED_SCHEMA;
        } else {
            strategy = detectParseStrategy(result.content(), entries);
        }

        // Log output
        GenerationOutput genOutput = outputLogger.logBatchSuccess(
                result, modelName, systemPrompt, userPrompt, job, task, strategy, promptVersion);
        genOutput.setBatch(batchEntity);
        generationOutputRepository.save(genOutput);

        // Match and convert content for this batch's items only
        int matched = 0;
        List<String> matchedTitles = new ArrayList<>();
        for (LectureItem item : batchItems) {
            ContentEntry entry = findMatchingEntry(entries, item.getTitle());
            if (entry != null) {
                String itemType = item.getItemType().name();
                JsonNode converted;
                if ("CODING_SET".equals(itemType) && entry.hasCodingContent()) {
                    converted = contentConverter.convertCodingStructured(entry.codingContent());
                } else if ("QUIZ_SET".equals(itemType) && entry.hasQuizContent()) {
                    converted = contentConverter.convertQuizStructured(entry.quizContent());
                } else {
                    converted = contentConverter.convert(itemType, entry.content());
                }
                if (!isContentMeaningful(converted, itemType)) {
                    log.warn("    Batch {}: item '{}' [{}] matched but content is empty/insufficient — skipping. " +
                                    "hasQuiz={}, hasCoding={}, convertedSize={}, converted={}",
                            batchEntity.getBatchIndex() + 1, item.getTitle(), itemType,
                            entry.hasQuizContent(), entry.hasCodingContent(),
                            converted != null ? Math.max(converted.path("blocks").size(), converted.path("content").size()) : -1,
                            converted != null ? converted.toString().substring(0, Math.min(converted.toString().length(), 500)) : "null");
                    continue;
                }
                item.setContentJson(converted);
                lectureItemRepository.save(item);
                matched++;
                matchedTitles.add(item.getTitle());
            } else {
                log.warn("    Batch {}: no match for item '{}'",
                        batchEntity.getBatchIndex() + 1, item.getTitle());
            }
        }

        // Update batch record
        batchEntity.setOutput(genOutput);
        batchEntity.setParseStrategy(strategy);
        batchEntity.setLatencyMs(result.latencyMs());
        batchEntity.setPromptTokens(result.promptTokens());
        batchEntity.setCompletionTokens(result.candidatesTokens());
        batchEntity.setFinishReason(result.finishReason());
        batchEntity.setTruncated(false);
        batchEntity.setItemsMatched(matched);
        batchEntity.setMatchedItemTitles(serializeTitlesList(matchedTitles));
        if (matched == 0) {
            batchEntity.setStatus(GenerationTaskStatus.FAILED);
            batchEntity.setErrorMessage("No items matched (" + batchItems.size() + " expected)");
        } else if (matched < batchItems.size()) {
            batchEntity.setStatus(GenerationTaskStatus.PARTIALLY_COMPLETED);
            batchEntity.setErrorMessage(matched + "/" + batchItems.size() + " items matched");
        } else {
            batchEntity.setStatus(GenerationTaskStatus.COMPLETED);
        }
        lectureContentBatchRepository.save(batchEntity);

        if (job != null) {
            accumulateJobTokens(job, result);
            generationJobRepository.save(job);
        }

        log.info("    Batch {}/{}: {}/{} items matched",
                batchEntity.getBatchIndex() + 1, totalBatches, matched, batchItems.size());

        // ── Auto-retry unmatched items (up to MAX_PARTIAL_RETRIES) ──
        if (batchEntity.getStatus() == GenerationTaskStatus.PARTIALLY_COMPLETED) {
            Set<String> matchedSet = new HashSet<>(matchedTitles);
            List<LectureItem> unmatchedItems = batchItems.stream()
                    .filter(item -> !matchedSet.contains(item.getTitle()))
                    .toList();

            int retryCount = batchEntity.getRetryCount() != null ? batchEntity.getRetryCount() : 0;
            if (!unmatchedItems.isEmpty() && retryCount < MAX_PARTIAL_RETRIES) {
                log.info("    Auto-retrying {} unmatched items (attempt {}/{})",
                        unmatchedItems.size(), retryCount + 1, MAX_PARTIAL_RETRIES);

                int nextIndex = getNextBatchIndex(task);
                int itemEstimate = unmatchedItems.stream()
                        .mapToInt(BatchGrouper::estimateTokenBudget)
                        .sum();

                LectureContentBatch retryBatch = new LectureContentBatch();
                retryBatch.setTask(task);
                retryBatch.setBatchIndex(nextIndex);
                retryBatch.setItemTitles(serializeTitles(unmatchedItems));
                retryBatch.setItemTypes(unmatchedItems.stream()
                        .map(i -> i.getItemType().name())
                        .collect(Collectors.joining(",")));
                retryBatch.setItemsInBatch(unmatchedItems.size());
                retryBatch.setItemsMatched(0);
                retryBatch.setStatus(GenerationTaskStatus.PENDING);
                retryBatch.setRetryCount(retryCount + 1);
                retryBatch.setMaxOutputTokens(BatchGrouper.computeMaxOutputTokens(itemEstimate));
                retryBatch.setParentBatch(batchEntity);
                retryBatch.setIsLeaf(true);
                retryBatch = lectureContentBatchRepository.save(retryBatch);

                // Mark current batch as non-leaf since child takes over unmatched items
                batchEntity.setIsLeaf(false);
                batchEntity.setSplitReason("PARTIAL_RETRY");
                lectureContentBatchRepository.save(batchEntity);

                if (task.getTotalBatches() != null) {
                    task.setTotalBatches(task.getTotalBatches() + 1);
                    lectureContentTaskRepository.save(task);
                }

                try {
                    generateSingleBatch(
                            retryBatch, unmatchedItems, systemPrompt, allItemOutlines,
                            totalBatches, task, job, request, modelName,
                            sectionTitle, lectureTitle, promptVersion);
                } catch (Exception retryEx) {
                    log.error("    Auto-retry batch {} failed: {}",
                            retryBatch.getBatchIndex(), retryEx.getMessage());
                }
            }
        }
    }

    /**
     * Aggregate task status from its batch results.
     */
    /**
     * Aggregate task status from batch results.
     * Only leaf batches count — SPLIT parents are excluded from aggregation.
     */
    private void aggregateTaskFromBatches(LectureContentTask task,
                                           List<LectureContentBatch> batchEntities,
                                           CourseGenerationJob job) {
        // Use leaf batches for aggregation (fresh query to include any auto-split children)
        List<LectureContentBatch> leafBatches =
                lectureContentBatchRepository.findByTaskIdAndIsLeafTrueOrderByBatchIndexAsc(task.getId());

        // ── Cleanup: if multiple leaf batches cover the same item, keep only the latest ──
        // This handles old data from before the isLeaf fix was deployed
        Map<String, LectureContentBatch> latestByItem = new LinkedHashMap<>();
        for (LectureContentBatch b : leafBatches) {
            if (b.getItemTitles() == null) continue;
            for (String title : b.getItemTitles().split("\\|")) {
                String trimmed = title.trim();
                if (trimmed.isEmpty()) continue;
                LectureContentBatch existing = latestByItem.get(trimmed);
                if (existing == null || b.getBatchIndex() > existing.getBatchIndex()) {
                    // Newer batch supersedes — mark old as non-leaf
                    if (existing != null && !existing.getId().equals(b.getId())) {
                        existing.setIsLeaf(false);
                        lectureContentBatchRepository.save(existing);
                    }
                    latestByItem.put(trimmed, b);
                } else if (b.getBatchIndex() < existing.getBatchIndex()) {
                    // This batch is older — mark it as non-leaf
                    b.setIsLeaf(false);
                    lectureContentBatchRepository.save(b);
                }
            }
        }

        // Re-fetch after cleanup
        leafBatches = lectureContentBatchRepository.findByTaskIdAndIsLeafTrueOrderByBatchIndexAsc(task.getId());

        int completed = 0, failed = 0;
        long totalLatency = 0;
        int totalPrompt = 0, totalCompletion = 0;
        GenerationOutput lastOutput = null;
        OutputParseStrategy lastStrategy = null;

        // Deduplicate matched items by title to avoid double-counting
        Set<String> uniqueMatchedItems = new HashSet<>();

        for (LectureContentBatch b : leafBatches) {
            if (b.getStatus() == GenerationTaskStatus.COMPLETED) completed++;
            else if (b.getStatus() == GenerationTaskStatus.FAILED
                    || b.getStatus() == GenerationTaskStatus.PARTIALLY_COMPLETED) failed++;
            // Collect unique matched item titles
            if (b.getMatchedItemTitles() != null) {
                for (String title : b.getMatchedItemTitles().split("\\|")) {
                    if (!title.isBlank()) uniqueMatchedItems.add(title.trim());
                }
            }
            totalLatency += (b.getLatencyMs() != null ? b.getLatencyMs() : 0);
            totalPrompt += (b.getPromptTokens() != null ? b.getPromptTokens() : 0);
            totalCompletion += (b.getCompletionTokens() != null ? b.getCompletionTokens() : 0);
            if (b.getOutput() != null) lastOutput = b.getOutput();
            if (b.getParseStrategy() != null) lastStrategy = b.getParseStrategy();
        }

        task.setTotalBatches(leafBatches.size());
        task.setCompletedBatches(completed);
        task.setFailedBatches(failed);
        task.setItemsMatched(uniqueMatchedItems.size());
        task.setLatencyMs(totalLatency);
        task.setPromptTokens(totalPrompt);
        task.setCompletionTokens(totalCompletion);
        task.setOutput(lastOutput);
        task.setParseStrategy(lastStrategy);

        if (failed == 0 && completed == leafBatches.size()) {
            task.setStatus(GenerationTaskStatus.COMPLETED);
            task.setErrorMessage(null);
        } else if (completed > 0) {
            task.setStatus(GenerationTaskStatus.PARTIALLY_COMPLETED);
            task.setErrorMessage(completed + "/" + leafBatches.size() + " leaf batches completed, "
                    + uniqueMatchedItems.size() + "/" + task.getItemsTotal() + " items matched");
        } else {
            task.setStatus(GenerationTaskStatus.FAILED);
            task.setErrorMessage("All " + leafBatches.size() + " leaf batches failed");
        }
        lectureContentTaskRepository.save(task);

        log.info("  Lecture '{}': {}/{} leaf batches completed, {}/{} items matched",
                task.getLectureTitle(), completed, leafBatches.size(),
                uniqueMatchedItems.size(), task.getItemsTotal());
    }

    /** Get the next available batch index for a task (max existing + 1). */
    private int getNextBatchIndex(LectureContentTask task) {
        List<LectureContentBatch> existing = lectureContentBatchRepository.findByTaskIdOrderByBatchIndexAsc(task.getId());
        return existing.stream().mapToInt(LectureContentBatch::getBatchIndex).max().orElse(-1) + 1;
    }

    private String serializeTitles(List<LectureItem> items) {
        try {
            return objectMapper.writeValueAsString(items.stream().map(LectureItem::getTitle).toList());
        } catch (Exception e) {
            return "[]";
        }
    }

    private String serializeTitlesList(List<String> titles) {
        try {
            return objectMapper.writeValueAsString(titles);
        } catch (Exception e) {
            return "[]";
        }
    }

    private String serializeTypes(List<LectureItem> items) {
        return items.stream().map(i -> i.getItemType().name()).reduce((a, b) -> a + "," + b).orElse("");
    }

    private void accumulateJobTokens(CourseGenerationJob job, AiGenerationResult result) {
        if (result.promptTokens() != null) {
            job.setTotalPromptTokens(
                    (job.getTotalPromptTokens() != null ? job.getTotalPromptTokens() : 0) + result.promptTokens());
        }
        if (result.candidatesTokens() != null) {
            job.setTotalCompletionTokens(
                    (job.getTotalCompletionTokens() != null ? job.getTotalCompletionTokens() : 0) + result.candidatesTokens());
        }
        job.setTotalLatencyMs(
                (job.getTotalLatencyMs() != null ? job.getTotalLatencyMs() : 0L) + result.latencyMs());

        var cost = GenerationOutputLogger.estimateCost(
                getModelName(), result.promptTokens(), result.candidatesTokens(), result.thinkingTokens());
        if (cost != null) {
            job.setTotalCostUsd(
                    job.getTotalCostUsd() != null ? job.getTotalCostUsd().add(cost) : cost);
        }
    }

    /**
     * Check if converted tiptap JSON has meaningful content for the item type.
     * Rejects empty/stub responses the AI sometimes returns.
     */
    private boolean isContentMeaningful(JsonNode converted, String itemType) {
        if (converted == null) return false;

        // CODING_SET uses "problems" array, not "blocks"/"content"
        if ("CODING_SET".equals(itemType)) {
            JsonNode problems = converted.path("problems");
            return problems.isArray() && !problems.isEmpty();
        }

        // Other types use "blocks" (converter output) or "content" (legacy)
        JsonNode contentArray = converted.path("blocks");
        if (!contentArray.isArray() || contentArray.isEmpty()) {
            contentArray = converted.path("content");
            if (!contentArray.isArray() || contentArray.isEmpty()) return false;
        }

        if ("CHECKPOINT".equals(itemType)) {
            for (JsonNode node : contentArray) {
                String type = node.path("type").asText();
                if ("checkpointBlock".equals(type) || "checkpoint".equals(type)) return true;
            }
            return contentArray.size() >= 1;
        }

        if ("QUIZ_SET".equals(itemType)) {
            for (JsonNode node : contentArray) {
                String type = node.path("type").asText();
                if ("quizBlock".equals(type) || "quiz".equals(type)) return true;
            }
            return contentArray.size() >= 1;
        }

        // For RICH_TEXT and others, require at least 2 content nodes (not just an empty paragraph)
        return contentArray.size() >= 2;
    }

    private String getModelName() {
        if (aiClient instanceof GeminiApiClient gemini) {
            return gemini.getModel();
        }
        return "unknown";
    }

    // ── Title matching ──

    private static String normalizeTitle(String title) {
        if (title == null) return "";
        return title
                .toLowerCase()
                .strip()
                .replaceAll("[^a-z0-9가-힣\\s\\-]", "")  // remove punctuation except hyphens
                .replaceAll("\\s+", " ")                   // collapse whitespace
                .strip();
    }

    private ContentEntry findMatchingEntry(List<ContentEntry> entries, String itemTitle) {
        if (itemTitle == null) return null;
        String normalized = normalizeTitle(itemTitle);

        // Pass 1: exact normalized match
        for (ContentEntry entry : entries) {
            if (normalizeTitle(entry.itemTitle()).equals(normalized)) {
                return entry;
            }
        }

        // Pass 2: substring containment (bidirectional)
        for (ContentEntry entry : entries) {
            if (entry.itemTitle() == null) continue;
            String entryNorm = normalizeTitle(entry.itemTitle());
            if (entryNorm.contains(normalized) || normalized.contains(entryNorm)) {
                return entry;
            }
        }

        // Pass 3: word overlap scoring (Jaccard-like, threshold >= 0.5)
        ContentEntry best = null;
        double bestScore = 0;
        java.util.Set<String> targetWords = new java.util.HashSet<>(java.util.Arrays.asList(normalized.split("\\s+")));

        for (ContentEntry entry : entries) {
            if (entry.itemTitle() == null) continue;
            java.util.Set<String> entryWords = new java.util.HashSet<>(
                    java.util.Arrays.asList(normalizeTitle(entry.itemTitle()).split("\\s+")));
            long intersection = targetWords.stream().filter(entryWords::contains).count();
            double union = targetWords.size() + entryWords.size() - intersection;
            double score = union > 0 ? intersection / union : 0;
            if (score > bestScore && score >= 0.5) {
                bestScore = score;
                best = entry;
            }
        }

        return best;
    }

    // ── Parse strategy detection ──

    private OutputParseStrategy detectParseStrategy(String rawContent, List<ContentEntry> entries) {
        if (entries.isEmpty()) return OutputParseStrategy.MANUAL_EXTRACTION;

        String cleaned = cleanJson(rawContent);
        // Try direct parse
        try {
            objectMapper.readValue(cleaned, new TypeReference<List<ContentEntry>>() {});
            return OutputParseStrategy.DIRECT_JSON;
        } catch (Exception ignored) {}

        // Try repair
        try {
            String repaired = repairJsonIterative(cleaned);
            objectMapper.readValue(repaired, new TypeReference<List<ContentEntry>>() {});
            return OutputParseStrategy.REPAIRED_JSON;
        } catch (Exception ignored) {}

        return OutputParseStrategy.MANUAL_EXTRACTION;
    }

    // ── JSON parsing (3-tier) ──

    private List<ContentEntry> parseContentEntries(String output) {
        String cleaned = cleanJson(output);

        try {
            return objectMapper.readValue(cleaned, new TypeReference<List<ContentEntry>>() {});
        } catch (Exception e) {
            log.warn("First content parse failed: {}", e.getMessage());
        }

        String repaired = repairJsonIterative(cleaned);
        try {
            return objectMapper.readValue(repaired, new TypeReference<List<ContentEntry>>() {});
        } catch (Exception e) {
            log.warn("Iterative repair failed: {}", e.getMessage());
        }

        List<ContentEntry> extracted = extractEntriesManually(cleaned);
        if (!extracted.isEmpty()) {
            log.info("Manual extraction recovered {} content entries", extracted.size());
            return extracted;
        }

        log.error("Could not parse content entries by any method");
        log.error("First 500 chars of AI output: {}", cleaned.substring(0, Math.min(500, cleaned.length())));
        return List.of();
    }

    private String cleanJson(String output) {
        String cleaned = output.strip();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceFirst("^```[a-zA-Z]*\\n?", "");
            cleaned = cleaned.replaceFirst("\\n?```$", "");
            cleaned = cleaned.strip();
        }
        cleaned = fixLatexJsonEscapes(cleaned);
        return cleaned;
    }

    private String fixLatexJsonEscapes(String json) {
        json = json.replaceAll(
                "(?<!\\\\)\\\\n(eq|abla|eg|u|ot(?:in)?|i|less|geq|leq|mid|ewline|parallel|vdash|subset(?:eq)?|supset(?:eq)?|cong|sim|prec|succ)(?![a-zA-Z])",
                "\\\\\\\\n$1"
        );
        json = json.replaceAll(
                "(?<!\\\\)\\\\t(ext|heta|imes|au|o|ilde|riangle|op)(?![a-zA-Z])",
                "\\\\\\\\t$1"
        );
        json = json.replaceAll(
                "(?<!\\\\)\\\\b(eta|egin|ecause|inom|igcup|igcap|ullet|ar|ot)(?![a-zA-Z])",
                "\\\\\\\\b$1"
        );
        json = json.replaceAll(
                "(?<!\\\\)\\\\f(rac|orall|lat)(?![a-zA-Z])",
                "\\\\\\\\f$1"
        );
        json = json.replaceAll(
                "(?<!\\\\)\\\\r(ho|ight(?:arrow)?|angle|ceil|floor)(?![a-zA-Z])",
                "\\\\\\\\r$1"
        );
        return json;
    }

    private String repairJsonIterative(String json) {
        String attempt = json;
        for (int i = 0; i < 30; i++) {
            int lastBrace = attempt.lastIndexOf('}');
            int lastBracket = attempt.lastIndexOf(']');
            int cutPoint = Math.max(lastBrace, lastBracket);
            if (cutPoint <= 0) break;

            String cut = attempt.substring(0, cutPoint + 1);
            cut = cut.replaceAll(",\\s*}", "}");
            cut = cut.replaceAll(",\\s*]", "]");

            String trimmed = cut.strip();
            if (trimmed.startsWith("[") && !trimmed.endsWith("]")) {
                trimmed = trimmed.replaceAll(",\\s*$", "");
                trimmed = trimmed + "]";
            }

            try {
                List<ContentEntry> result = objectMapper.readValue(trimmed, new TypeReference<List<ContentEntry>>() {});
                if (result != null && !result.isEmpty()) {
                    log.info("JSON repaired by cutting {} chars (attempt {}), recovered {} entries",
                            json.length() - cutPoint, i + 1, result.size());
                    return trimmed;
                }
            } catch (Exception e) {
                attempt = attempt.substring(0, cutPoint);
            }
        }
        return json;
    }

    private List<ContentEntry> extractEntriesManually(String json) {
        List<ContentEntry> results = new ArrayList<>();
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "\\{\\s*\"(?:itemTitle|item_title|title)\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"content\"\\s*:\\s*\"");
        java.util.regex.Matcher matcher = pattern.matcher(json);

        List<Integer> starts = new ArrayList<>();
        List<String> titles = new ArrayList<>();
        while (matcher.find()) {
            starts.add(matcher.start());
            titles.add(matcher.group(1));
        }

        for (int i = 0; i < starts.size(); i++) {
            int start = starts.get(i);
            int end = (i + 1 < starts.size()) ? starts.get(i + 1) : json.length();
            String segment = json.substring(start, end).strip();

            if (segment.endsWith(",")) {
                segment = segment.substring(0, segment.length() - 1).strip();
            }

            try {
                String fixed = segment;
                if (!fixed.endsWith("}")) {
                    int lastClose = fixed.lastIndexOf("\"}");
                    if (lastClose > 0) {
                        fixed = fixed.substring(0, lastClose + 2);
                    } else {
                        fixed = fixed + "\"}";
                    }
                }
                ContentEntry entry = objectMapper.readValue(fixed, ContentEntry.class);
                if (entry.itemTitle() != null && entry.content() != null) {
                    results.add(entry);
                }
            } catch (Exception e) {
                String title = titles.get(i);
                int contentStart = segment.indexOf("\"content\"");
                if (contentStart >= 0) {
                    int valStart = segment.indexOf('"', segment.indexOf(':', contentStart) + 1) + 1;
                    if (valStart > 0 && valStart < segment.length()) {
                        String rawContent = segment.substring(valStart);
                        if (rawContent.endsWith("\"}")) {
                            rawContent = rawContent.substring(0, rawContent.length() - 2);
                        } else if (rawContent.endsWith("\"")) {
                            rawContent = rawContent.substring(0, rawContent.length() - 1);
                        }
                        rawContent = rawContent.replace("\\n", "\n")
                                .replace("\\\"", "\"")
                                .replace("\\\\", "\\")
                                .replace("\\t", "\t");
                        if (!rawContent.isBlank()) {
                            results.add(new ContentEntry(title, null, rawContent, null, null));
                        }
                    }
                }
            }
        }

        return results;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ContentEntry(
            @JsonAlias({"item_title", "itemtitle", "title"}) String itemTitle,
            String itemType,
            String content,
            CodingContent codingContent,
            List<QuizQuestion> quizContent
    ) {
        /** Has structured coding data from schema? */
        boolean hasCodingContent() { return codingContent != null && codingContent.title() != null; }
        /** Has structured quiz data from schema? */
        boolean hasQuizContent() { return quizContent != null && !quizContent.isEmpty(); }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CodingContent(
            String title, String language, String description,
            String functionName, String starterCode, String hint,
            String evaluationStyle,
            List<CodingTestCase> testCases
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CodingTestCase(String input, String expectedOutput) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record QuizQuestion(
            String question,
            List<QuizOption> options,
            String answer,
            String explanation
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record QuizOption(String letter, String text) {}
}
