package com.codehaja.domain.generation.service;

import com.codehaja.domain.generation.entity.CourseGenerationJob;
import com.codehaja.domain.generation.entity.GenerationOutput;
import com.codehaja.domain.generation.entity.PromptTemplate;
import com.codehaja.domain.generation.entity.PromptTemplateVersion;
import com.codehaja.domain.generation.repository.GenerationOutputRepository;
import com.codehaja.domain.generation.repository.PromptTemplateRepository;
import com.codehaja.domain.generation.repository.PromptTemplateVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Manages prompt templates and versions.
 * Allows admins to create new versions and activate them without code deployment.
 */
@Service
@RequiredArgsConstructor
public class PromptTemplateService {

    private final PromptTemplateRepository templateRepository;
    private final PromptTemplateVersionRepository versionRepository;
    private final GenerationOutputRepository outputRepository;

    public List<PromptTemplate> listTemplates() {
        return templateRepository.findAll();
    }

    public List<PromptTemplateVersion> listVersions(Long templateId) {
        return versionRepository.findByTemplateIdOrderByVersionNumberDesc(templateId);
    }

    public PromptTemplateVersion getActiveVersion(Long templateId) {
        return versionRepository.findByTemplateIdAndIsActiveTrue(templateId).orElse(null);
    }

    public PromptTemplateVersion getVersionDetail(Long versionId) {
        return versionRepository.findById(versionId)
                .orElseThrow(() -> new RuntimeException("Version not found: " + versionId));
    }

    /**
     * Create a new version of a template. Does NOT activate it automatically.
     */
    @Transactional
    public PromptTemplateVersion createVersion(Long templateId, String content, String changeNotes, String createdBy) {
        PromptTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found: " + templateId));

        List<PromptTemplateVersion> existing = versionRepository.findByTemplateIdOrderByVersionNumberDesc(templateId);
        int nextVersion = existing.isEmpty() ? 1 : existing.get(0).getVersionNumber() + 1;

        PromptTemplateVersion version = new PromptTemplateVersion();
        version.setTemplate(template);
        version.setVersionNumber(nextVersion);
        version.setContent(content);
        version.setIsActive(false);
        version.setChangeNotes(changeNotes);
        version.setCreatedBy(createdBy);

        return versionRepository.save(version);
    }

    /**
     * Activate a specific version, deactivating the current active one.
     */
    @Transactional
    public PromptTemplateVersion activateVersion(Long versionId) {
        PromptTemplateVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new RuntimeException("Version not found: " + versionId));

        // Deactivate current active
        versionRepository.findByTemplateIdAndIsActiveTrue(version.getTemplate().getId())
                .ifPresent(current -> {
                    current.setIsActive(false);
                    versionRepository.save(current);
                });

        // Activate new
        version.setIsActive(true);
        return versionRepository.save(version);
    }

    /**
     * Performance metrics for a prompt template version.
     * Shows generation count, success/partial/fail breakdown, and recent output stats.
     */
    public Map<String, Object> getVersionMetrics(Long versionId) {
        PromptTemplateVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new RuntimeException("Version not found: " + versionId));

        long totalGenerations = outputRepository.countByPromptTemplateVersionId(versionId);
        long successCount = outputRepository.countByPromptTemplateVersionIdAndSuccess(versionId, true);
        long failCount = outputRepository.countByPromptTemplateVersionIdAndSuccess(versionId, false);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("versionId", versionId);
        result.put("versionNumber", version.getVersionNumber());
        result.put("isActive", version.getIsActive());
        result.put("totalGenerations", totalGenerations);
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        result.put("successRate", totalGenerations > 0
                ? BigDecimal.valueOf(successCount * 100.0 / totalGenerations).setScale(1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);

        // Recent 10 outputs performance
        List<GenerationOutput> recentOutputs = outputRepository
                .findByPromptTemplateVersionIdOrderByCreatedAtDesc(versionId);
        List<Map<String, Object>> recent = recentOutputs.stream().limit(10).map(o -> {
            Map<String, Object> om = new LinkedHashMap<>();
            om.put("outputId", o.getId());
            om.put("success", o.getSuccess());
            om.put("finishReason", o.getFinishReason());
            om.put("parseStrategy", o.getParseStrategy() != null ? o.getParseStrategy().name() : null);
            om.put("latencyMs", o.getLatencyMs());
            om.put("promptTokens", o.getPromptTokens());
            om.put("completionTokens", o.getCandidatesTokens());
            om.put("createdAt", o.getCreatedAt());
            return om;
        }).toList();
        result.put("recentOutputs", recent);

        // Avg latency / tokens for successful runs
        long avgLatency = 0;
        int avgPromptTokens = 0;
        int avgCompletionTokens = 0;
        long successWithLatency = 0;
        for (GenerationOutput o : recentOutputs) {
            if (Boolean.TRUE.equals(o.getSuccess()) && o.getLatencyMs() != null) {
                avgLatency += o.getLatencyMs();
                avgPromptTokens += o.getPromptTokens() != null ? o.getPromptTokens() : 0;
                avgCompletionTokens += o.getCandidatesTokens() != null ? o.getCandidatesTokens() : 0;
                successWithLatency++;
            }
        }
        if (successWithLatency > 0) {
            result.put("avgLatencyMs", avgLatency / successWithLatency);
            result.put("avgPromptTokens", avgPromptTokens / (int) successWithLatency);
            result.put("avgCompletionTokens", avgCompletionTokens / (int) successWithLatency);
        }

        // Cost aggregation
        BigDecimal totalCost = recentOutputs.stream()
                .map(o -> o.getEstimatedCostUsd() != null ? o.getEstimatedCostUsd() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        result.put("totalCostUsd", totalCost);
        result.put("avgCostUsd", totalGenerations > 0
                ? totalCost.divide(BigDecimal.valueOf(totalGenerations), 6, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);

        // Recent jobs that used this version
        Set<Long> seenJobIds = new LinkedHashSet<>();
        List<Map<String, Object>> recentJobs = new ArrayList<>();
        for (GenerationOutput o : recentOutputs) {
            if (o.getJob() != null && seenJobIds.add(o.getJob().getId()) && recentJobs.size() < 5) {
                CourseGenerationJob job = o.getJob();
                Map<String, Object> jm = new LinkedHashMap<>();
                jm.put("jobId", job.getId());
                jm.put("courseTitle", job.getCourseTitle());
                jm.put("status", job.getStatus().name());
                jm.put("createdAt", job.getCreatedAt());
                recentJobs.add(jm);
            }
        }
        result.put("recentJobs", recentJobs);

        return result;
    }

    /**
     * Compare two versions' content (simple line-based diff summary).
     */
    public Map<String, Object> compareVersions(Long versionIdA, Long versionIdB) {
        PromptTemplateVersion a = versionRepository.findById(versionIdA)
                .orElseThrow(() -> new RuntimeException("Version not found: " + versionIdA));
        PromptTemplateVersion b = versionRepository.findById(versionIdB)
                .orElseThrow(() -> new RuntimeException("Version not found: " + versionIdB));

        String contentA = a.getContent() != null ? a.getContent() : "";
        String contentB = b.getContent() != null ? b.getContent() : "";

        String[] linesA = contentA.split("\n");
        String[] linesB = contentB.split("\n");

        // Simple diff: count added, removed, changed lines
        int maxLines = Math.max(linesA.length, linesB.length);
        int added = 0, removed = 0, changed = 0, unchanged = 0;
        for (int i = 0; i < maxLines; i++) {
            String lineA = i < linesA.length ? linesA[i] : null;
            String lineB = i < linesB.length ? linesB[i] : null;
            if (lineA == null) added++;
            else if (lineB == null) removed++;
            else if (lineA.equals(lineB)) unchanged++;
            else changed++;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("versionA", Map.of("id", a.getId(), "versionNumber", a.getVersionNumber(),
                "contentLength", contentA.length(), "lineCount", linesA.length));
        result.put("versionB", Map.of("id", b.getId(), "versionNumber", b.getVersionNumber(),
                "contentLength", contentB.length(), "lineCount", linesB.length));
        result.put("added", added);
        result.put("removed", removed);
        result.put("changed", changed);
        result.put("unchanged", unchanged);
        result.put("contentA", contentA);
        result.put("contentB", contentB);
        return result;
    }
}
