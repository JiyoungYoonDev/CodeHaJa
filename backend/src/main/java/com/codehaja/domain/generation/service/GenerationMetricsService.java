package com.codehaja.domain.generation.service;

import com.codehaja.domain.generation.entity.*;
import com.codehaja.domain.generation.repository.CourseGenerationJobRepository;
import com.codehaja.domain.generation.repository.GenerationDiffRepository;
import com.codehaja.domain.generation.repository.GenerationOutputRepository;
import com.codehaja.domain.generation.repository.LectureContentBatchRepository;
import com.codehaja.domain.generation.repository.LectureContentTaskRepository;
import com.codehaja.domain.generation.repository.ReviewSessionRepository;
import com.codehaja.domain.generation.repository.ValidationResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Phase 4: Generation metrics and eval dashboard data.
 * Aggregates stats across jobs, tasks, batches, outputs, and validations.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GenerationMetricsService {

    private final CourseGenerationJobRepository jobRepository;
    private final LectureContentTaskRepository taskRepository;
    private final LectureContentBatchRepository batchRepository;
    private final GenerationOutputRepository outputRepository;
    private final ValidationResultRepository validationResultRepository;
    private final ReviewSessionRepository reviewSessionRepository;
    private final GenerationDiffRepository diffRepository;

    /**
     * Overview metrics for the eval dashboard.
     * Separates running/pending from finished jobs for accurate success rate.
     * Includes failure analysis, batch quality, and 7-day trend.
     */
    public Map<String, Object> getDashboardOverview() {
        List<CourseGenerationJob> allJobs = jobRepository.findAllByOrderByCreatedAtDesc();

        long totalJobs = allJobs.size();
        long completedJobs = allJobs.stream().filter(j -> j.getStatus() == GenerationJobStatus.COMPLETED).count();
        long failedJobs = allJobs.stream().filter(j -> j.getStatus() == GenerationJobStatus.FAILED).count();
        long partialJobs = allJobs.stream().filter(j -> j.getStatus() == GenerationJobStatus.PARTIALLY_COMPLETED).count();
        long runningJobs = allJobs.stream().filter(j -> j.getStatus() == GenerationJobStatus.IN_PROGRESS).count();
        long pendingJobs = allJobs.stream().filter(j -> j.getStatus() == GenerationJobStatus.PENDING).count();
        long finishedJobs = completedJobs + failedJobs + partialJobs;

        // Token usage
        int totalPromptTokens = allJobs.stream()
                .mapToInt(j -> j.getTotalPromptTokens() != null ? j.getTotalPromptTokens() : 0).sum();
        int totalCompletionTokens = allJobs.stream()
                .mapToInt(j -> j.getTotalCompletionTokens() != null ? j.getTotalCompletionTokens() : 0).sum();
        BigDecimal totalCost = allJobs.stream()
                .map(j -> j.getTotalCostUsd() != null ? j.getTotalCostUsd() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Average latency (finished only)
        long totalLatency = allJobs.stream()
                .filter(j -> j.getStatus() != GenerationJobStatus.IN_PROGRESS && j.getStatus() != GenerationJobStatus.PENDING)
                .mapToLong(j -> j.getTotalLatencyMs() != null ? j.getTotalLatencyMs() : 0).sum();
        long avgLatency = finishedJobs > 0 ? totalLatency / finishedJobs : 0;

        // Batch-level deep stats
        List<LectureContentTask> allTasks = new ArrayList<>();
        for (CourseGenerationJob job : allJobs) {
            allTasks.addAll(taskRepository.findByJobIdOrderBySectionIdAscLectureIdAsc(job.getId()));
        }

        // Pre-collect all leaf batches for aggregations (excludes SPLIT parents)
        List<LectureContentBatch> allBatchesFlat = new ArrayList<>();
        for (LectureContentTask task : allTasks) {
            allBatchesFlat.addAll(batchRepository.findByTaskIdAndIsLeafTrueOrderByBatchIndexAsc(task.getId()));
        }

        int totalBatches = 0;
        int truncationCount = 0;
        int parseRepairCount = 0;
        int totalRetries = 0;
        int totalItemsAcrossBatches = 0;
        int totalMatchedAcrossBatches = 0;
        int partialBatches = 0;

        // Drill-down detail lists
        List<Map<String, Object>> truncationDetails = new ArrayList<>();
        List<Map<String, Object>> partialDetails = new ArrayList<>();
        List<Map<String, Object>> parseRepairDetails = new ArrayList<>();
        List<Map<String, Object>> retryDetails = new ArrayList<>();
        List<Map<String, Object>> unmatchedDetails = new ArrayList<>();

        // Failure reason aggregation
        Map<String, Integer> failureReasons = new LinkedHashMap<>();

        for (LectureContentTask task : allTasks) {
            if (task.getStatus() == GenerationTaskStatus.FAILED && task.getErrorMessage() != null) {
                String reason = normalizeErrorReason(task.getErrorMessage());
                failureReasons.merge(reason, 1, Integer::sum);
            }

            List<LectureContentBatch> batches = batchRepository.findByTaskIdAndIsLeafTrueOrderByBatchIndexAsc(task.getId());
            for (LectureContentBatch batch : batches) {
                totalBatches++;

                int inBatch = batch.getItemsInBatch() != null ? batch.getItemsInBatch() : 0;
                int matched = batch.getItemsMatched() != null ? batch.getItemsMatched() : 0;
                totalItemsAcrossBatches += inBatch;
                totalMatchedAcrossBatches += matched;

                // Shared context for drill-down entries
                Map<String, Object> ctx = batchContext(batch, task);

                // Shared enrichment fields
                GenerationOutput output = batch.getOutput();
                String promptVersionLabel = null;
                if (output != null && output.getPromptTemplateVersion() != null) {
                    PromptTemplateVersion pv = output.getPromptTemplateVersion();
                    promptVersionLabel = pv.getTemplate().getName() + " v" + pv.getVersionNumber();
                }
                boolean hasRetry = batch.getRetryCount() != null && batch.getRetryCount() > 0;
                boolean isTruncated = Boolean.TRUE.equals(batch.getTruncated());
                boolean isRepaired = batch.getParseStrategy() != null
                        && batch.getParseStrategy() != OutputParseStrategy.STRUCTURED_SCHEMA;

                // Partial: matched some but not all
                if (matched > 0 && matched < inBatch) {
                    partialBatches++;
                    Map<String, Object> d = new LinkedHashMap<>(ctx);
                    d.put("itemsInBatch", inBatch);
                    d.put("itemsMatched", matched);
                    d.put("missing", inBatch - matched);
                    d.put("status", batch.getStatus().name());
                    d.put("truncated", isTruncated);
                    d.put("repaired", isRepaired);
                    d.put("parseStrategy", batch.getParseStrategy() != null ? batch.getParseStrategy().name() : null);
                    d.put("promptVersion", promptVersionLabel);
                    d.put("errorMessage", batch.getErrorMessage());
                    d.put("latencyMs", batch.getLatencyMs());
                    // Classify partial cause
                    String partialCause = "Unknown";
                    if (isTruncated) partialCause = "Truncation (MAX_TOKENS)";
                    else if (isRepaired) partialCause = "Parse repair (fallback)";
                    else if (batch.getErrorMessage() != null && batch.getErrorMessage().toLowerCase().contains("parse"))
                        partialCause = "Parse failure";
                    else partialCause = "Title mismatch";
                    d.put("cause", partialCause);
                    partialDetails.add(d);
                }

                // Unmatched: matched zero
                if (matched == 0 && inBatch > 0 && batch.getStatus() != GenerationTaskStatus.PENDING) {
                    Map<String, Object> d = new LinkedHashMap<>(ctx);
                    d.put("itemsInBatch", inBatch);
                    d.put("status", batch.getStatus().name());
                    d.put("errorMessage", batch.getErrorMessage());
                    d.put("truncated", isTruncated);
                    d.put("repaired", isRepaired);
                    d.put("promptVersion", promptVersionLabel);
                    unmatchedDetails.add(d);
                }

                // Retries
                if (hasRetry) {
                    totalRetries += batch.getRetryCount();
                    Map<String, Object> d = new LinkedHashMap<>(ctx);
                    d.put("retryCount", batch.getRetryCount());
                    d.put("status", batch.getStatus().name());
                    d.put("succeeded", batch.getStatus() == GenerationTaskStatus.COMPLETED);
                    d.put("itemsMatched", matched);
                    d.put("itemsInBatch", inBatch);
                    d.put("errorMessage", batch.getErrorMessage());
                    d.put("promptVersion", promptVersionLabel);
                    d.put("latencyMs", batch.getLatencyMs());
                    // Retry reason from error or truncation
                    String retryReason = "Unknown";
                    if (isTruncated) retryReason = "MAX_TOKENS truncation";
                    else if (batch.getErrorMessage() != null) retryReason = normalizeErrorReason(batch.getErrorMessage());
                    d.put("retryReason", retryReason);
                    retryDetails.add(d);
                }

                // Parse repair
                if (isRepaired) {
                    parseRepairCount++;
                    Map<String, Object> d = new LinkedHashMap<>(ctx);
                    d.put("parseStrategy", batch.getParseStrategy().name());
                    d.put("itemsMatched", matched);
                    d.put("itemsInBatch", inBatch);
                    d.put("isPartial", matched > 0 && matched < inBatch);
                    d.put("isFullMatch", matched == inBatch);
                    d.put("status", batch.getStatus().name());
                    d.put("errorMessage", batch.getErrorMessage());
                    d.put("promptVersion", promptVersionLabel);
                    d.put("latencyMs", batch.getLatencyMs());
                    parseRepairDetails.add(d);
                }

                // Truncation
                if (isTruncated) {
                    truncationCount++;
                    failureReasons.merge("MAX_TOKENS truncation", 1, Integer::sum);
                    Map<String, Object> d = new LinkedHashMap<>(ctx);
                    d.put("maxOutputTokens", batch.getMaxOutputTokens());
                    d.put("completionTokens", batch.getCompletionTokens());
                    d.put("promptTokens", batch.getPromptTokens());
                    d.put("finishReason", batch.getFinishReason());
                    d.put("itemsMatched", matched);
                    d.put("itemsInBatch", inBatch);
                    d.put("unmatched", inBatch - matched);
                    d.put("status", batch.getStatus().name());
                    d.put("repaired", isRepaired);
                    d.put("parseStrategy", batch.getParseStrategy() != null ? batch.getParseStrategy().name() : null);
                    d.put("retried", hasRetry);
                    d.put("retryCount", batch.getRetryCount());
                    d.put("promptVersion", promptVersionLabel);
                    d.put("latencyMs", batch.getLatencyMs());
                    d.put("courseTitle", task.getJob().getCourseTitle());
                    d.put("tokenUsagePct", batch.getMaxOutputTokens() != null && batch.getMaxOutputTokens() > 0
                            ? BigDecimal.valueOf((batch.getCompletionTokens() != null ? batch.getCompletionTokens() : 0) * 100.0
                                / batch.getMaxOutputTokens()).setScale(0, RoundingMode.HALF_UP).intValue()
                            : null);
                    truncationDetails.add(d);
                }

                // Batch-level errors
                if (batch.getStatus() == GenerationTaskStatus.FAILED && batch.getErrorMessage() != null) {
                    String reason = normalizeErrorReason(batch.getErrorMessage());
                    failureReasons.merge(reason, 1, Integer::sum);
                }
            }
        }

        BigDecimal avgMatchedRatio = totalItemsAcrossBatches > 0
                ? BigDecimal.valueOf(totalMatchedAcrossBatches * 100.0 / totalItemsAcrossBatches)
                    .setScale(1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal partialRate = totalBatches > 0
                ? BigDecimal.valueOf(partialBatches * 100.0 / totalBatches)
                    .setScale(1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // ── Match rate aggregates ──

        // Match rate by lecture (all tasks, sorted worst-first)
        List<Map<String, Object>> matchRateByLecture = allTasks.stream()
                .filter(t -> t.getItemsTotal() != null && t.getItemsTotal() > 0)
                .map(t -> {
                    int total = t.getItemsTotal();
                    int mch = t.getItemsMatched() != null ? t.getItemsMatched() : 0;
                    BigDecimal rate = BigDecimal.valueOf(mch * 100.0 / total).setScale(1, RoundingMode.HALF_UP);
                    Map<String, Object> e = new LinkedHashMap<>();
                    e.put("taskId", t.getId());
                    e.put("jobId", t.getJob().getId());
                    e.put("lectureTitle", t.getLectureTitle());
                    e.put("sectionTitle", t.getSectionTitle());
                    e.put("itemsTotal", total);
                    e.put("itemsMatched", mch);
                    e.put("matchRate", rate);
                    e.put("status", t.getStatus().name());
                    return e;
                })
                .sorted(Comparator.<Map<String, Object>, BigDecimal>comparing(m -> (BigDecimal) m.get("matchRate")))
                .toList();

        // Match rate by item type (from batches)
        Map<String, int[]> matchByType = new LinkedHashMap<>(); // type -> [total, matched]
        for (LectureContentBatch b : allBatchesFlat) {
            if (b.getItemTypes() == null) continue;
            for (String type : b.getItemTypes().split(",")) {
                type = type.trim();
                if (type.isEmpty()) continue;
                matchByType.computeIfAbsent(type, k -> new int[2]);
                // Approximate: distribute evenly across types in batch
                int inB = b.getItemsInBatch() != null ? b.getItemsInBatch() : 0;
                int mB = b.getItemsMatched() != null ? b.getItemsMatched() : 0;
                if (inB > 0) {
                    int typeCount = b.getItemTypes().split(",").length;
                    int perType = Math.max(1, inB / typeCount);
                    int perTypeMatched = Math.max(0, mB / typeCount);
                    matchByType.get(type)[0] += perType;
                    matchByType.get(type)[1] += Math.min(perTypeMatched, perType);
                }
            }
        }
        List<Map<String, Object>> matchRateByItemType = matchByType.entrySet().stream()
                .map(entry -> {
                    int[] counts = entry.getValue();
                    BigDecimal rate = counts[0] > 0
                            ? BigDecimal.valueOf(counts[1] * 100.0 / counts[0]).setScale(1, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;
                    Map<String, Object> e = new LinkedHashMap<>();
                    e.put("itemType", entry.getKey());
                    e.put("totalItems", counts[0]);
                    e.put("matchedItems", counts[1]);
                    e.put("matchRate", rate);
                    return e;
                })
                .sorted(Comparator.<Map<String, Object>, BigDecimal>comparing(m -> (BigDecimal) m.get("matchRate")))
                .toList();

        // Partial cause distribution
        Map<String, Integer> partialCauses = new LinkedHashMap<>();
        for (Map<String, Object> pd : partialDetails) {
            String cause = (String) pd.get("cause");
            partialCauses.merge(cause, 1, Integer::sum);
        }
        List<Map<String, Object>> partialCauseBreakdown = partialCauses.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .map(e -> Map.<String, Object>of("cause", e.getKey(), "count", e.getValue()))
                .toList();

        // Retry summary stats
        long retrySuccessCount = retryDetails.stream().filter(d -> Boolean.TRUE.equals(d.get("succeeded"))).count();
        long retryFailCount = retryDetails.size() - retrySuccessCount;
        double avgRetryCount = retryDetails.isEmpty() ? 0
                : retryDetails.stream().mapToInt(d -> (int) d.get("retryCount")).average().orElse(0);

        // Parse repair summary
        long repairFullMatch = parseRepairDetails.stream().filter(d -> Boolean.TRUE.equals(d.get("isFullMatch"))).count();
        long repairPartial = parseRepairDetails.stream().filter(d -> Boolean.TRUE.equals(d.get("isPartial"))).count();

        // Top failure reasons (sorted by count desc, top 10)
        List<Map<String, Object>> topFailureReasons = failureReasons.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("reason", e.getKey());
                    m.put("count", e.getValue());
                    return m;
                }).toList();

        // Failed/partial job summaries
        List<Map<String, Object>> problemJobs = allJobs.stream()
                .filter(j -> j.getStatus() == GenerationJobStatus.FAILED
                          || j.getStatus() == GenerationJobStatus.PARTIALLY_COMPLETED)
                .map(j -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("jobId", j.getId());
                    m.put("courseTitle", j.getCourseTitle());
                    m.put("status", j.getStatus().name());
                    m.put("errorMessage", j.getErrorMessage());
                    m.put("completedLectures", j.getCompletedLectures());
                    m.put("failedLectures", j.getFailedLectures());
                    m.put("totalLectures", j.getTotalLectures());
                    m.put("createdAt", j.getCreatedAt());
                    return m;
                }).toList();

        // ── Token breakdown for cost analysis ──

        // 1. Job ranking by total tokens (top 10)
        List<Map<String, Object>> tokenByJob = allJobs.stream()
                .filter(j -> (j.getTotalPromptTokens() != null && j.getTotalPromptTokens() > 0)
                           || (j.getTotalCompletionTokens() != null && j.getTotalCompletionTokens() > 0))
                .sorted(Comparator.<CourseGenerationJob, Integer>comparing(
                        j -> (j.getTotalPromptTokens() != null ? j.getTotalPromptTokens() : 0)
                           + (j.getTotalCompletionTokens() != null ? j.getTotalCompletionTokens() : 0))
                        .reversed())
                .limit(10)
                .map(j -> {
                    int prompt = j.getTotalPromptTokens() != null ? j.getTotalPromptTokens() : 0;
                    int completion = j.getTotalCompletionTokens() != null ? j.getTotalCompletionTokens() : 0;
                    Map<String, Object> e = new LinkedHashMap<>();
                    e.put("jobId", j.getId());
                    e.put("courseTitle", j.getCourseTitle());
                    e.put("status", j.getStatus().name());
                    e.put("promptTokens", prompt);
                    e.put("completionTokens", completion);
                    e.put("totalTokens", prompt + completion);
                    e.put("costUsd", j.getTotalCostUsd());
                    e.put("totalLectures", j.getTotalLectures());
                    return e;
                }).toList();

        // 2. Lecture hot spots — top 15 lectures by token usage
        List<Map<String, Object>> tokenByLecture = allTasks.stream()
                .filter(t -> (t.getPromptTokens() != null && t.getPromptTokens() > 0)
                           || (t.getCompletionTokens() != null && t.getCompletionTokens() > 0))
                .sorted(Comparator.<LectureContentTask, Integer>comparing(
                        t -> (t.getPromptTokens() != null ? t.getPromptTokens() : 0)
                           + (t.getCompletionTokens() != null ? t.getCompletionTokens() : 0))
                        .reversed())
                .limit(15)
                .map(t -> {
                    int prompt = t.getPromptTokens() != null ? t.getPromptTokens() : 0;
                    int completion = t.getCompletionTokens() != null ? t.getCompletionTokens() : 0;
                    Map<String, Object> e = new LinkedHashMap<>();
                    e.put("taskId", t.getId());
                    e.put("jobId", t.getJob().getId());
                    e.put("lectureTitle", t.getLectureTitle());
                    e.put("sectionTitle", t.getSectionTitle());
                    e.put("promptTokens", prompt);
                    e.put("completionTokens", completion);
                    e.put("totalTokens", prompt + completion);
                    e.put("itemsTotal", t.getItemsTotal());
                    e.put("status", t.getStatus().name());
                    return e;
                }).toList();

        // 3. Batch hot spots — top 15 batches by token usage
        List<Map<String, Object>> tokenByBatch = allBatchesFlat.stream()
                .filter(b -> (b.getPromptTokens() != null && b.getPromptTokens() > 0)
                           || (b.getCompletionTokens() != null && b.getCompletionTokens() > 0))
                .sorted(Comparator.<LectureContentBatch, Integer>comparing(
                        b -> (b.getPromptTokens() != null ? b.getPromptTokens() : 0)
                           + (b.getCompletionTokens() != null ? b.getCompletionTokens() : 0))
                        .reversed())
                .limit(15)
                .map(b -> {
                    int prompt = b.getPromptTokens() != null ? b.getPromptTokens() : 0;
                    int completion = b.getCompletionTokens() != null ? b.getCompletionTokens() : 0;
                    Map<String, Object> e = new LinkedHashMap<>();
                    e.put("batchId", b.getId());
                    e.put("batchIndex", b.getBatchIndex());
                    e.put("taskId", b.getTask().getId());
                    e.put("jobId", b.getTask().getJob().getId());
                    e.put("lectureTitle", b.getTask().getLectureTitle());
                    e.put("sectionTitle", b.getTask().getSectionTitle());
                    e.put("itemTypes", b.getItemTypes());
                    e.put("itemTitles", b.getItemTitles());
                    e.put("promptTokens", prompt);
                    e.put("completionTokens", completion);
                    e.put("totalTokens", prompt + completion);
                    e.put("maxOutputTokens", b.getMaxOutputTokens());
                    return e;
                }).toList();

        // 4. Prompt version avg tokens
        List<GenerationOutput> allOutputs = outputRepository.findAll();
        Map<Long, List<GenerationOutput>> byVersion = allOutputs.stream()
                .filter(o -> o.getPromptTemplateVersion() != null)
                .collect(Collectors.groupingBy(o -> o.getPromptTemplateVersion().getId()));
        List<Map<String, Object>> tokenByPromptVersion = byVersion.entrySet().stream()
                .map(entry -> {
                    List<GenerationOutput> outputs = entry.getValue();
                    PromptTemplateVersion ver = outputs.get(0).getPromptTemplateVersion();
                    int avgPrompt = (int) outputs.stream()
                            .mapToInt(o -> o.getPromptTokens() != null ? o.getPromptTokens() : 0).average().orElse(0);
                    int avgCompletion = (int) outputs.stream()
                            .mapToInt(o -> o.getCandidatesTokens() != null ? o.getCandidatesTokens() : 0).average().orElse(0);
                    Map<String, Object> e = new LinkedHashMap<>();
                    e.put("versionId", ver.getId());
                    e.put("templateName", ver.getTemplate().getName());
                    e.put("versionNumber", ver.getVersionNumber());
                    e.put("isActive", ver.getIsActive());
                    e.put("generationCount", outputs.size());
                    e.put("avgPromptTokens", avgPrompt);
                    e.put("avgCompletionTokens", avgCompletion);
                    e.put("avgTotalTokens", avgPrompt + avgCompletion);
                    return e;
                })
                .sorted(Comparator.<Map<String, Object>, Integer>comparing(m -> (int) m.get("avgTotalTokens")).reversed())
                .toList();

        // ── Cost breakdown ──

        // 1. Cost by job (top 10)
        List<Map<String, Object>> costByJob = allJobs.stream()
                .filter(j -> j.getTotalCostUsd() != null && j.getTotalCostUsd().compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.<CourseGenerationJob, BigDecimal>comparing(CourseGenerationJob::getTotalCostUsd).reversed())
                .limit(10)
                .map(j -> {
                    Map<String, Object> e = new LinkedHashMap<>();
                    e.put("jobId", j.getId());
                    e.put("courseTitle", j.getCourseTitle());
                    e.put("status", j.getStatus().name());
                    e.put("costUsd", j.getTotalCostUsd());
                    e.put("totalLectures", j.getTotalLectures());
                    int tokens = (j.getTotalPromptTokens() != null ? j.getTotalPromptTokens() : 0)
                               + (j.getTotalCompletionTokens() != null ? j.getTotalCompletionTokens() : 0);
                    e.put("totalTokens", tokens);
                    e.put("modelName", j.getModelName());
                    return e;
                }).toList();

        // 2. Cost by lecture (from outputs, top 15)
        List<Map<String, Object>> costByLecture = allOutputs.stream()
                .filter(o -> o.getTask() != null && o.getEstimatedCostUsd() != null)
                .collect(Collectors.groupingBy(o -> o.getTask().getId()))
                .entrySet().stream()
                .map(entry -> {
                    List<GenerationOutput> outputs = entry.getValue();
                    LectureContentTask task = outputs.get(0).getTask();
                    BigDecimal lectureCost = outputs.stream()
                            .map(o -> o.getEstimatedCostUsd() != null ? o.getEstimatedCostUsd() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    int lectureTokens = outputs.stream()
                            .mapToInt(o -> o.getTotalTokens() != null ? o.getTotalTokens() : 0).sum();
                    Map<String, Object> e = new LinkedHashMap<>();
                    e.put("taskId", task.getId());
                    e.put("jobId", task.getJob().getId());
                    e.put("lectureTitle", task.getLectureTitle());
                    e.put("sectionTitle", task.getSectionTitle());
                    e.put("costUsd", lectureCost);
                    e.put("totalTokens", lectureTokens);
                    e.put("itemsTotal", task.getItemsTotal());
                    e.put("batchCount", outputs.size());
                    return e;
                })
                .sorted(Comparator.<Map<String, Object>, BigDecimal>comparing(m -> (BigDecimal) m.get("costUsd")).reversed())
                .limit(15)
                .toList();

        // 3. Cost by prompt version
        List<Map<String, Object>> costByPromptVersion = byVersion.entrySet().stream()
                .map(entry -> {
                    List<GenerationOutput> outputs = entry.getValue();
                    PromptTemplateVersion ver = outputs.get(0).getPromptTemplateVersion();
                    BigDecimal versionTotalCost = outputs.stream()
                            .map(o -> o.getEstimatedCostUsd() != null ? o.getEstimatedCostUsd() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal versionAvgCost = outputs.isEmpty() ? BigDecimal.ZERO
                            : versionTotalCost.divide(BigDecimal.valueOf(outputs.size()), 6, RoundingMode.HALF_UP);
                    Map<String, Object> e = new LinkedHashMap<>();
                    e.put("versionId", ver.getId());
                    e.put("templateName", ver.getTemplate().getName());
                    e.put("versionNumber", ver.getVersionNumber());
                    e.put("isActive", ver.getIsActive());
                    e.put("generationCount", outputs.size());
                    e.put("totalCostUsd", versionTotalCost);
                    e.put("avgCostUsd", versionAvgCost);
                    return e;
                })
                .sorted(Comparator.<Map<String, Object>, BigDecimal>comparing(m -> (BigDecimal) m.get("totalCostUsd")).reversed())
                .toList();

        // 4. Cost by model
        List<Map<String, Object>> costByModel = allJobs.stream()
                .filter(j -> j.getModelName() != null)
                .collect(Collectors.groupingBy(CourseGenerationJob::getModelName))
                .entrySet().stream()
                .map(entry -> {
                    List<CourseGenerationJob> modelJobs = entry.getValue();
                    BigDecimal modelCost = modelJobs.stream()
                            .map(j -> j.getTotalCostUsd() != null ? j.getTotalCostUsd() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    int modelTokens = modelJobs.stream()
                            .mapToInt(j -> (j.getTotalPromptTokens() != null ? j.getTotalPromptTokens() : 0)
                                         + (j.getTotalCompletionTokens() != null ? j.getTotalCompletionTokens() : 0)).sum();
                    Map<String, Object> e = new LinkedHashMap<>();
                    e.put("modelName", entry.getKey());
                    e.put("jobCount", modelJobs.size());
                    e.put("totalCostUsd", modelCost);
                    e.put("totalTokens", modelTokens);
                    e.put("avgCostPerJob", modelJobs.isEmpty() ? BigDecimal.ZERO
                            : modelCost.divide(BigDecimal.valueOf(modelJobs.size()), 6, RoundingMode.HALF_UP));
                    return e;
                })
                .sorted(Comparator.<Map<String, Object>, BigDecimal>comparing(m -> (BigDecimal) m.get("totalCostUsd")).reversed())
                .toList();

        // ── Latency breakdown ──

        // Collect all batch latencies for percentile computation
        List<Long> allBatchLatencies = allBatchesFlat.stream()
                .map(LectureContentBatch::getLatencyMs)
                .filter(Objects::nonNull)
                .sorted()
                .toList();

        long p50Latency = percentile(allBatchLatencies, 50);
        long p95Latency = percentile(allBatchLatencies, 95);

        // 1. Slowest jobs (top 10)
        List<Map<String, Object>> latencyByJob = allJobs.stream()
                .filter(j -> j.getTotalLatencyMs() != null && j.getTotalLatencyMs() > 0)
                .sorted(Comparator.<CourseGenerationJob, Long>comparing(CourseGenerationJob::getTotalLatencyMs).reversed())
                .limit(10)
                .map(j -> {
                    Map<String, Object> e = new LinkedHashMap<>();
                    e.put("jobId", j.getId());
                    e.put("courseTitle", j.getCourseTitle());
                    e.put("status", j.getStatus().name());
                    e.put("latencyMs", j.getTotalLatencyMs());
                    e.put("totalLectures", j.getTotalLectures());
                    e.put("modelName", j.getModelName());
                    return e;
                }).toList();

        // 2. Slowest lectures (top 15)
        List<Map<String, Object>> latencyByLecture = allTasks.stream()
                .filter(t -> t.getLatencyMs() != null && t.getLatencyMs() > 0)
                .sorted(Comparator.<LectureContentTask, Long>comparing(LectureContentTask::getLatencyMs).reversed())
                .limit(15)
                .map(t -> {
                    Map<String, Object> e = new LinkedHashMap<>();
                    e.put("taskId", t.getId());
                    e.put("jobId", t.getJob().getId());
                    e.put("lectureTitle", t.getLectureTitle());
                    e.put("sectionTitle", t.getSectionTitle());
                    e.put("latencyMs", t.getLatencyMs());
                    e.put("itemsTotal", t.getItemsTotal());
                    e.put("status", t.getStatus().name());
                    return e;
                }).toList();

        // 3. Slowest batches (top 15)
        List<Map<String, Object>> latencyByBatch = allBatchesFlat.stream()
                .filter(b -> b.getLatencyMs() != null && b.getLatencyMs() > 0)
                .sorted(Comparator.<LectureContentBatch, Long>comparing(LectureContentBatch::getLatencyMs).reversed())
                .limit(15)
                .map(b -> {
                    Map<String, Object> e = new LinkedHashMap<>();
                    e.put("batchId", b.getId());
                    e.put("batchIndex", b.getBatchIndex());
                    e.put("jobId", b.getTask().getJob().getId());
                    e.put("lectureTitle", b.getTask().getLectureTitle());
                    e.put("itemTypes", b.getItemTypes());
                    e.put("latencyMs", b.getLatencyMs());
                    boolean truncated = Boolean.TRUE.equals(b.getTruncated());
                    boolean repaired = b.getParseStrategy() != null
                            && b.getParseStrategy() != OutputParseStrategy.STRUCTURED_SCHEMA;
                    e.put("truncated", truncated);
                    e.put("repaired", repaired);
                    e.put("status", b.getStatus().name());
                    return e;
                }).toList();

        // 4. Latency by condition (normal vs truncated vs repaired)
        long normalCount = 0, normalSum = 0;
        long truncatedCount = 0, truncatedSum = 0;
        long repairedCount = 0, repairedSum = 0;
        for (LectureContentBatch b : allBatchesFlat) {
            if (b.getLatencyMs() == null || b.getLatencyMs() <= 0) continue;
            long lat = b.getLatencyMs();
            boolean isTruncated = Boolean.TRUE.equals(b.getTruncated());
            boolean isRepaired = b.getParseStrategy() != null
                    && b.getParseStrategy() != OutputParseStrategy.STRUCTURED_SCHEMA;
            if (isTruncated) { truncatedCount++; truncatedSum += lat; }
            else if (isRepaired) { repairedCount++; repairedSum += lat; }
            else { normalCount++; normalSum += lat; }
        }
        List<Map<String, Object>> latencyByCondition = new ArrayList<>();
        latencyByCondition.add(Map.of("condition", "Normal", "count", normalCount,
                "avgLatencyMs", normalCount > 0 ? normalSum / normalCount : 0));
        latencyByCondition.add(Map.of("condition", "Truncated", "count", truncatedCount,
                "avgLatencyMs", truncatedCount > 0 ? truncatedSum / truncatedCount : 0));
        latencyByCondition.add(Map.of("condition", "Parse Repaired", "count", repairedCount,
                "avgLatencyMs", repairedCount > 0 ? repairedSum / repairedCount : 0));

        // 5. Latency by model
        List<Map<String, Object>> latencyByModel = allJobs.stream()
                .filter(j -> j.getModelName() != null && j.getTotalLatencyMs() != null && j.getTotalLatencyMs() > 0)
                .collect(Collectors.groupingBy(CourseGenerationJob::getModelName))
                .entrySet().stream()
                .map(entry -> {
                    List<CourseGenerationJob> modelJobs = entry.getValue();
                    long avgLat = (long) modelJobs.stream()
                            .mapToLong(CourseGenerationJob::getTotalLatencyMs).average().orElse(0);
                    List<Long> sorted = modelJobs.stream()
                            .map(CourseGenerationJob::getTotalLatencyMs).sorted().toList();
                    Map<String, Object> e = new LinkedHashMap<>();
                    e.put("modelName", entry.getKey());
                    e.put("jobCount", modelJobs.size());
                    e.put("avgLatencyMs", avgLat);
                    e.put("p50LatencyMs", percentile(sorted, 50));
                    e.put("p95LatencyMs", percentile(sorted, 95));
                    return e;
                })
                .sorted(Comparator.<Map<String, Object>, Long>comparing(m -> (long) m.get("avgLatencyMs")).reversed())
                .toList();

        // 7-day trend (with cost)
        List<Map<String, Object>> dailyTrend = new ArrayList<>();
        for (int d = 6; d >= 0; d--) {
            LocalDate date = LocalDate.now().minusDays(d);
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

            long dayCompleted = 0, dayFailed = 0, dayPartial = 0;
            BigDecimal dayCost = BigDecimal.ZERO;
            for (CourseGenerationJob job : allJobs) {
                if (job.getCreatedAt() != null && !job.getCreatedAt().isBefore(dayStart) && job.getCreatedAt().isBefore(dayEnd)) {
                    if (job.getTotalCostUsd() != null) dayCost = dayCost.add(job.getTotalCostUsd());
                    switch (job.getStatus()) {
                        case COMPLETED -> dayCompleted++;
                        case FAILED -> dayFailed++;
                        case PARTIALLY_COMPLETED -> dayPartial++;
                        default -> {}
                    }
                }
            }
            Map<String, Object> day = new LinkedHashMap<>();
            day.put("date", date.toString());
            day.put("completed", dayCompleted);
            day.put("failed", dayFailed);
            day.put("partial", dayPartial);
            day.put("costUsd", dayCost);
            dailyTrend.add(day);
        }

        // Build result
        Map<String, Object> result = new LinkedHashMap<>();

        // Job status breakdown
        result.put("totalJobs", totalJobs);
        result.put("completedJobs", completedJobs);
        result.put("failedJobs", failedJobs);
        result.put("partialJobs", partialJobs);
        result.put("runningJobs", runningJobs);
        result.put("pendingJobs", pendingJobs);
        result.put("finishedJobs", finishedJobs);

        // Success rate — finished jobs only (excludes running/pending)
        result.put("successRate", finishedJobs > 0
                ? BigDecimal.valueOf(completedJobs * 100.0 / finishedJobs).setScale(1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);

        // Token/cost/latency
        result.put("totalPromptTokens", totalPromptTokens);
        result.put("totalCompletionTokens", totalCompletionTokens);
        result.put("totalTokens", totalPromptTokens + totalCompletionTokens);
        result.put("totalCostUsd", totalCost);
        result.put("avgLatencyMs", avgLatency);

        // Batch quality
        result.put("totalBatches", totalBatches);
        result.put("partialRate", partialRate);
        result.put("truncationCount", truncationCount);
        result.put("parseRepairCount", parseRepairCount);
        result.put("avgMatchedRatio", avgMatchedRatio);
        result.put("totalRetries", totalRetries);

        // Failure analysis
        result.put("topFailureReasons", topFailureReasons);
        result.put("problemJobs", problemJobs);

        // Drill-down detail lists
        result.put("truncationDetails", truncationDetails);
        result.put("partialDetails", partialDetails);
        result.put("partialCauseBreakdown", partialCauseBreakdown);
        result.put("parseRepairDetails", parseRepairDetails);
        result.put("parseRepairFullMatch", repairFullMatch);
        result.put("parseRepairPartial", repairPartial);
        result.put("retryDetails", retryDetails);
        result.put("retrySuccessCount", retrySuccessCount);
        result.put("retryFailCount", retryFailCount);
        result.put("avgRetryCount", BigDecimal.valueOf(avgRetryCount).setScale(1, RoundingMode.HALF_UP));
        result.put("unmatchedDetails", unmatchedDetails);
        result.put("matchRateByLecture", matchRateByLecture);
        result.put("matchRateByItemType", matchRateByItemType);

        // Token breakdown
        result.put("tokenByJob", tokenByJob);
        result.put("tokenByLecture", tokenByLecture);
        result.put("tokenByBatch", tokenByBatch);
        result.put("tokenByPromptVersion", tokenByPromptVersion);

        // Cost breakdown
        result.put("costByJob", costByJob);
        result.put("costByLecture", costByLecture);
        result.put("costByPromptVersion", costByPromptVersion);
        result.put("costByModel", costByModel);

        // Latency breakdown
        result.put("p50LatencyMs", p50Latency);
        result.put("p95LatencyMs", p95Latency);
        result.put("latencyByJob", latencyByJob);
        result.put("latencyByLecture", latencyByLecture);
        result.put("latencyByBatch", latencyByBatch);
        result.put("latencyByCondition", latencyByCondition);
        result.put("latencyByModel", latencyByModel);

        // 7-day trend
        result.put("dailyTrend", dailyTrend);

        return result;
    }

    /**
     * Build shared context map for a batch drill-down entry.
     */
    private Map<String, Object> batchContext(LectureContentBatch batch, LectureContentTask task) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("batchId", batch.getId());
        m.put("batchIndex", batch.getBatchIndex());
        m.put("taskId", task.getId());
        m.put("jobId", task.getJob().getId());
        m.put("lectureId", task.getLectureId());
        m.put("lectureTitle", task.getLectureTitle());
        m.put("sectionTitle", task.getSectionTitle());
        m.put("itemTitles", batch.getItemTitles());
        m.put("itemTypes", batch.getItemTypes());
        return m;
    }

    /**
     * Normalize error messages into shorter bucket labels.
     */
    private String normalizeErrorReason(String error) {
        if (error == null) return "Unknown";
        String lower = error.toLowerCase();
        if (lower.contains("max_tokens") || lower.contains("truncat")) return "MAX_TOKENS truncation";
        if (lower.contains("timeout") || lower.contains("timed out")) return "Request timeout";
        if (lower.contains("rate limit") || lower.contains("429")) return "Rate limit (429)";
        if (lower.contains("500") || lower.contains("internal server")) return "Server error (500)";
        if (lower.contains("parse") || lower.contains("json")) return "JSON parse failure";
        if (lower.contains("empty")) return "Empty AI response";
        if (lower.contains("connect") || lower.contains("network")) return "Network error";
        // Truncate long messages
        return error.length() > 60 ? error.substring(0, 57) + "..." : error;
    }

    /**
     * Compute percentile from a pre-sorted list.
     */
    private long percentile(List<Long> sorted, int p) {
        if (sorted == null || sorted.isEmpty()) return 0;
        int idx = (int) Math.ceil(p / 100.0 * sorted.size()) - 1;
        return sorted.get(Math.max(0, Math.min(idx, sorted.size() - 1)));
    }

    /**
     * Per-job metrics summary (list of all jobs with key stats).
     */
    public List<Map<String, Object>> getJobsSummary() {
        List<CourseGenerationJob> jobs = jobRepository.findAllByOrderByCreatedAtDesc();
        return jobs.stream().map(job -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("jobId", job.getId());
            m.put("courseId", job.getCourseId());
            m.put("courseTitle", job.getCourseTitle());
            m.put("topic", job.getTopic());
            m.put("modelName", job.getModelName());
            m.put("status", job.getStatus().name());
            m.put("totalLectures", job.getTotalLectures());
            m.put("completedLectures", job.getCompletedLectures());
            m.put("failedLectures", job.getFailedLectures());
            m.put("totalPromptTokens", job.getTotalPromptTokens());
            m.put("totalCompletionTokens", job.getTotalCompletionTokens());
            m.put("totalCostUsd", job.getTotalCostUsd());
            m.put("totalLatencyMs", job.getTotalLatencyMs());
            m.put("createdAt", job.getCreatedAt());
            m.put("startedAt", job.getStartedAt());
            m.put("completedAt", job.getCompletedAt());
            // Wall clock time (total elapsed)
            if (job.getStartedAt() != null && job.getCompletedAt() != null) {
                long wallMs = java.time.Duration.between(job.getStartedAt(), job.getCompletedAt()).toMillis();
                m.put("wallTimeMs", wallMs);
            }

            // Review summary
            long approveCount = reviewSessionRepository.countByJobIdAndAction(job.getId(), ReviewAction.APPROVE);
            long rejectCount = reviewSessionRepository.countByJobIdAndAction(job.getId(), ReviewAction.REJECT);
            long diffCount = diffRepository.countByJobId(job.getId());
            m.put("approveCount", approveCount);
            m.put("rejectCount", rejectCount);
            m.put("diffCount", diffCount);

            // Batch quality metrics per job
            List<LectureContentTask> tasks = taskRepository.findByJobIdOrderBySectionIdAscLectureIdAsc(job.getId());
            int jobTotalItems = 0, jobMatchedItems = 0, jobTruncations = 0, jobPartialBatches = 0, jobTotalBatches = 0, jobSplits = 0;
            for (LectureContentTask task : tasks) {
                // Count splits separately (non-leaf)
                jobSplits += (int) batchRepository.countByTaskIdAndStatus(task.getId(), GenerationTaskStatus.SPLIT);
                List<LectureContentBatch> batches = batchRepository.findByTaskIdAndIsLeafTrueOrderByBatchIndexAsc(task.getId());
                for (LectureContentBatch batch : batches) {
                    jobTotalBatches++;
                    int inBatch = batch.getItemsInBatch() != null ? batch.getItemsInBatch() : 0;
                    int matched = batch.getItemsMatched() != null ? batch.getItemsMatched() : 0;
                    jobTotalItems += inBatch;
                    jobMatchedItems += matched;

                    if (matched > 0 && matched < inBatch) jobPartialBatches++;

                    if (Boolean.TRUE.equals(batch.getTruncated())) {
                        jobTruncations++;
                    }
                }
            }
            m.put("totalBatches", jobTotalBatches);
            m.put("matchRate", jobTotalItems > 0
                    ? BigDecimal.valueOf(jobMatchedItems * 100.0 / jobTotalItems).setScale(1, RoundingMode.HALF_UP)
                    : null);
            m.put("partialBatches", jobPartialBatches);
            m.put("truncations", jobTruncations);
            m.put("splits", jobSplits);

            return m;
        }).toList();
    }

    /**
     * Detailed metrics for a single job: tasks with nested batches, validation summary, review info.
     */
    public Map<String, Object> getJobDetail(Long jobId) {
        CourseGenerationJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("jobId", job.getId());
        result.put("courseId", job.getCourseId());
        result.put("courseTitle", job.getCourseTitle());
        result.put("topic", job.getTopic());
        result.put("status", job.getStatus().name());
        result.put("modelName", job.getModelName());
        result.put("totalLectures", job.getTotalLectures());
        result.put("completedLectures", job.getCompletedLectures());
        result.put("failedLectures", job.getFailedLectures());
        result.put("totalPromptTokens", job.getTotalPromptTokens());
        result.put("totalCompletionTokens", job.getTotalCompletionTokens());
        result.put("totalCostUsd", job.getTotalCostUsd());
        result.put("totalLatencyMs", job.getTotalLatencyMs());
        result.put("createdAt", job.getCreatedAt());
        result.put("startedAt", job.getStartedAt());
        result.put("completedAt", job.getCompletedAt());
        if (job.getStartedAt() != null && job.getCompletedAt() != null) {
            long wallMs = java.time.Duration.between(job.getStartedAt(), job.getCompletedAt()).toMillis();
            result.put("wallTimeMs", wallMs);
        }
        result.put("errorMessage", job.getErrorMessage());

        // Prompt versions used (from outputs)
        List<GenerationOutput> allJobOutputs = outputRepository.findByJobIdOrderByCreatedAtAsc(jobId);
        Set<String> promptVersionsUsed = new LinkedHashSet<>();
        for (GenerationOutput o : allJobOutputs) {
            if (o.getPromptTemplateVersion() != null) {
                PromptTemplateVersion pv = o.getPromptTemplateVersion();
                promptVersionsUsed.add(pv.getTemplate().getName() + " v" + pv.getVersionNumber());
            }
        }
        result.put("promptVersionsUsed", promptVersionsUsed);

        // Tasks with nested batch details
        List<LectureContentTask> tasks = taskRepository.findByJobIdOrderBySectionIdAscLectureIdAsc(jobId);
        int totalTruncations = 0;
        int totalBatchCount = 0;
        int totalRepairs = 0;
        int totalSplits = 0;

        List<Map<String, Object>> taskList = new ArrayList<>();
        for (LectureContentTask task : tasks) {
            Map<String, Object> tm = new LinkedHashMap<>();
            tm.put("taskId", task.getId());
            tm.put("lectureId", task.getLectureId());
            tm.put("lectureTitle", task.getLectureTitle());
            tm.put("sectionTitle", task.getSectionTitle());
            tm.put("status", task.getStatus().name());
            tm.put("itemsTotal", task.getItemsTotal());
            tm.put("itemsMatched", task.getItemsMatched());
            tm.put("latencyMs", task.getLatencyMs());
            tm.put("promptTokens", task.getPromptTokens());
            tm.put("completionTokens", task.getCompletionTokens());
            tm.put("generationMode", task.getGenerationMode() != null ? task.getGenerationMode() : "SINGLE");

            // Nested batches
            List<LectureContentBatch> batches = batchRepository.findByTaskIdOrderByBatchIndexAsc(task.getId());
            List<Map<String, Object>> batchList = new ArrayList<>();
            int taskTruncations = 0, taskRepairs = 0;
            for (LectureContentBatch batch : batches) {
                Map<String, Object> bm = new LinkedHashMap<>();
                bm.put("batchId", batch.getId());
                bm.put("batchIndex", batch.getBatchIndex());
                bm.put("status", batch.getStatus().name());
                bm.put("itemsInBatch", batch.getItemsInBatch());
                bm.put("itemsMatched", batch.getItemsMatched());
                bm.put("itemTitles", batch.getItemTitles());
                bm.put("matchedItemTitles", batch.getMatchedItemTitles());
                bm.put("itemTypes", batch.getItemTypes());
                bm.put("latencyMs", batch.getLatencyMs());
                bm.put("promptTokens", batch.getPromptTokens());
                bm.put("completionTokens", batch.getCompletionTokens());
                bm.put("maxOutputTokens", batch.getMaxOutputTokens());
                bm.put("parseStrategy", batch.getParseStrategy() != null ? batch.getParseStrategy().name() : null);
                bm.put("retryCount", batch.getRetryCount());
                bm.put("errorMessage", batch.getErrorMessage());
                bm.put("isLeaf", batch.getIsLeaf());
                bm.put("parentBatchId", batch.getParentBatch() != null ? batch.getParentBatch().getId() : null);
                bm.put("splitReason", batch.getSplitReason());

                // Read finishReason + truncation from batch fields (stored directly to avoid lazy-load issues)
                String finishReason = batch.getFinishReason();
                boolean truncated = Boolean.TRUE.equals(batch.getTruncated());
                boolean repaired = batch.getParseStrategy() != null
                        && batch.getParseStrategy() != OutputParseStrategy.STRUCTURED_SCHEMA;

                // Pull promptVersion + outputId from linked GenerationOutput
                String promptVersion = null;
                Long outputId = null;
                GenerationOutput output = batch.getOutput();
                if (output != null) {
                    outputId = output.getId();
                    if (output.getPromptTemplateVersion() != null) {
                        PromptTemplateVersion pv = output.getPromptTemplateVersion();
                        promptVersion = pv.getTemplate().getName() + " v" + pv.getVersionNumber();
                    }
                }
                bm.put("finishReason", finishReason);
                bm.put("truncated", truncated);
                bm.put("repaired", repaired);
                bm.put("promptVersion", promptVersion);
                bm.put("outputId", outputId);
                if (truncated) { totalTruncations++; taskTruncations++; }
                if (repaired) { totalRepairs++; taskRepairs++; }
                if (batch.getStatus() == GenerationTaskStatus.SPLIT) { totalSplits++; }

                batchList.add(bm);
            }
            tm.put("batches", batchList);
            tm.put("totalBatches", batches.size());
            tm.put("truncations", taskTruncations);
            tm.put("repairs", taskRepairs);
            totalBatchCount += batches.size();
            taskList.add(tm);
        }
        result.put("tasks", taskList);
        result.put("totalTasks", tasks.size());
        result.put("totalRepairs", totalRepairs);

        // Aggregated stats
        Map<String, Long> tasksByStatus = tasks.stream()
                .collect(Collectors.groupingBy(t -> t.getStatus().name(), Collectors.counting()));
        result.put("tasksByStatus", tasksByStatus);

        int totalItems = tasks.stream().mapToInt(t -> t.getItemsTotal() != null ? t.getItemsTotal() : 0).sum();
        int matchedItems = tasks.stream().mapToInt(t -> t.getItemsMatched() != null ? t.getItemsMatched() : 0).sum();
        result.put("totalItems", totalItems);
        result.put("matchedItems", matchedItems);
        result.put("itemMatchRate", totalItems > 0
                ? BigDecimal.valueOf(matchedItems * 100.0 / totalItems).setScale(1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
        result.put("totalBatches", totalBatchCount);
        result.put("totalTruncations", totalTruncations);
        result.put("totalSplits", totalSplits);

        // Validation summary across all outputs for this job
        List<GenerationOutput> outputs = outputRepository.findByJobIdOrderByCreatedAtAsc(jobId);
        long totalValidations = 0;
        long failedValidations = 0;
        Map<String, Long> failsByRule = new LinkedHashMap<>();
        for (GenerationOutput output : outputs) {
            List<ValidationResult> validations = validationResultRepository.findByOutputIdOrderByIdAsc(output.getId());
            totalValidations += validations.size();
            for (ValidationResult vr : validations) {
                if (!Boolean.TRUE.equals(vr.getPassed())) {
                    failedValidations++;
                    failsByRule.merge(vr.getRuleName(), 1L, Long::sum);
                }
            }
        }
        result.put("totalValidations", totalValidations);
        result.put("failedValidations", failedValidations);
        result.put("validationFailsByRule", failsByRule);

        // Review summary
        long approves = reviewSessionRepository.countByJobIdAndAction(jobId, ReviewAction.APPROVE);
        long rejects = reviewSessionRepository.countByJobIdAndAction(jobId, ReviewAction.REJECT);
        long changes = reviewSessionRepository.countByJobIdAndAction(jobId, ReviewAction.REQUEST_CHANGES);
        result.put("approveCount", approves);
        result.put("rejectCount", rejects);
        result.put("requestChangesCount", changes);
        result.put("diffCount", diffRepository.countByJobId(jobId));

        return result;
    }

    /**
     * Validation results for a specific output.
     */
    public List<Map<String, Object>> getOutputValidations(Long outputId) {
        List<ValidationResult> results = validationResultRepository.findByOutputIdOrderByIdAsc(outputId);
        return results.stream().map(vr -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", vr.getId());
            m.put("ruleName", vr.getRuleName());
            m.put("severity", vr.getSeverity().name());
            m.put("passed", vr.getPassed());
            m.put("message", vr.getMessage());
            m.put("details", vr.getDetails());
            return m;
        }).toList();
    }

    /**
     * Aggregated validation stats across all outputs (for dashboard charts).
     */
    public Map<String, Object> getValidationOverview() {
        // Get all outputs
        List<GenerationOutput> allOutputs = outputRepository.findByTaskTypeAndSuccessOrderByCreatedAtDesc(
                GenerationTaskType.LECTURE_CONTENT_BATCH, true);

        long totalOutputs = allOutputs.size();
        long totalChecks = 0;
        long passedChecks = 0;
        Map<String, long[]> ruleStats = new LinkedHashMap<>(); // ruleName -> [passed, failed]

        for (GenerationOutput output : allOutputs) {
            List<ValidationResult> results = validationResultRepository.findByOutputIdOrderByIdAsc(output.getId());
            for (ValidationResult vr : results) {
                totalChecks++;
                if (Boolean.TRUE.equals(vr.getPassed())) passedChecks++;

                ruleStats.computeIfAbsent(vr.getRuleName(), k -> new long[2]);
                if (Boolean.TRUE.equals(vr.getPassed())) {
                    ruleStats.get(vr.getRuleName())[0]++;
                } else {
                    ruleStats.get(vr.getRuleName())[1]++;
                }
            }
        }

        Map<String, Object> ruleBreakdown = new LinkedHashMap<>();
        ruleStats.forEach((rule, counts) -> {
            Map<String, Object> rs = new LinkedHashMap<>();
            rs.put("passed", counts[0]);
            rs.put("failed", counts[1]);
            rs.put("total", counts[0] + counts[1]);
            rs.put("passRate", (counts[0] + counts[1]) > 0
                    ? BigDecimal.valueOf(counts[0] * 100.0 / (counts[0] + counts[1])).setScale(1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO);
            ruleBreakdown.put(rule, rs);
        });

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalOutputs", totalOutputs);
        result.put("totalChecks", totalChecks);
        result.put("passedChecks", passedChecks);
        result.put("overallPassRate", totalChecks > 0
                ? BigDecimal.valueOf(passedChecks * 100.0 / totalChecks).setScale(1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
        result.put("ruleBreakdown", ruleBreakdown);
        return result;
    }

    // ── Batch / Task status override ──

    @Transactional
    public Map<String, Object> updateBatchStatus(Long batchId, String newStatus) {
        GenerationTaskStatus status;
        try {
            status = GenerationTaskStatus.valueOf(newStatus);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid status: " + newStatus);
        }

        LectureContentBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new RuntimeException("Batch not found: " + batchId));

        GenerationTaskStatus oldStatus = batch.getStatus();
        batch.setStatus(status);
        batchRepository.save(batch);

        // Re-aggregate task from all leaf batches
        LectureContentTask task = batch.getTask();
        if (task != null) {
            reAggregateTask(task);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("batchId", batchId);
        result.put("oldStatus", oldStatus.name());
        result.put("newStatus", status.name());
        result.put("taskId", task != null ? task.getId() : null);
        result.put("taskStatus", task != null ? task.getStatus().name() : null);
        return result;
    }

    @Transactional
    public Map<String, Object> updateTaskStatus(Long taskId, String newStatus) {
        GenerationTaskStatus status;
        try {
            status = GenerationTaskStatus.valueOf(newStatus);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid status: " + newStatus);
        }

        LectureContentTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found: " + taskId));

        GenerationTaskStatus oldStatus = task.getStatus();
        task.setStatus(status);
        taskRepository.save(task);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("taskId", taskId);
        result.put("oldStatus", oldStatus.name());
        result.put("newStatus", status.name());
        return result;
    }

    private void reAggregateTask(LectureContentTask task) {
        List<LectureContentBatch> batches = batchRepository.findByTaskIdOrderByBatchIndexAsc(task.getId());
        List<LectureContentBatch> leafBatches = batches.stream()
                .filter(b -> b.getIsLeaf() == null || b.getIsLeaf())
                .toList();

        int totalMatched = 0, completed = 0, failed = 0;
        for (LectureContentBatch b : leafBatches) {
            totalMatched += (b.getItemsMatched() != null ? b.getItemsMatched() : 0);
            if (b.getStatus() == GenerationTaskStatus.COMPLETED) completed++;
            else if (b.getStatus() == GenerationTaskStatus.FAILED) failed++;
        }

        task.setItemsMatched(totalMatched);
        task.setCompletedBatches(completed);
        task.setFailedBatches(failed);

        if (failed == leafBatches.size()) {
            task.setStatus(GenerationTaskStatus.FAILED);
        } else if (completed == leafBatches.size()) {
            task.setStatus(GenerationTaskStatus.COMPLETED);
        } else if (totalMatched > 0) {
            task.setStatus(GenerationTaskStatus.PARTIALLY_COMPLETED);
        }
        taskRepository.save(task);
    }
}
