package com.codehaja.domain.generation.service;

import com.codehaja.domain.generation.dto.AiGenerationResult;
import com.codehaja.domain.generation.entity.*;
import com.codehaja.domain.generation.repository.GenerationOutputRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
@RequiredArgsConstructor
public class GenerationOutputLogger {

    private static final Logger log = LoggerFactory.getLogger(GenerationOutputLogger.class);

    private final GenerationOutputRepository repository;

    /**
     * Log a successful AI generation call.
     * Uses REQUIRES_NEW so the log persists even if the outer transaction rolls back.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public GenerationOutput logSuccess(
            AiGenerationResult result,
            GenerationTaskType taskType,
            String modelName,
            String systemPrompt,
            String userPrompt,
            CourseGenerationJob job,
            LectureContentTask task,
            OutputParseStrategy parseStrategy) {
        return logSuccess(result, taskType, modelName, systemPrompt, userPrompt, job, task, parseStrategy, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public GenerationOutput logSuccess(
            AiGenerationResult result,
            GenerationTaskType taskType,
            String modelName,
            String systemPrompt,
            String userPrompt,
            CourseGenerationJob job,
            LectureContentTask task,
            OutputParseStrategy parseStrategy,
            PromptTemplateVersion promptVersion) {

        GenerationOutput output = new GenerationOutput();
        output.setJob(job);
        output.setTask(task);
        output.setTaskType(taskType);
        output.setModelName(modelName);
        output.setSystemPromptHash(sha256(systemPrompt));
        output.setSystemPrompt(systemPrompt);
        output.setUserPrompt(userPrompt);
        output.setRawOutput(result.rawResponseBody());
        output.setParsedOutput(result.content());
        output.setParseStrategy(parseStrategy);
        output.setStructuredSchemaUsed(result.structuredSchemaUsed());
        output.setPromptTokens(result.promptTokens());
        output.setCandidatesTokens(result.candidatesTokens());
        output.setTotalTokens(result.totalTokens());
        output.setThinkingTokens(result.thinkingTokens());
        output.setLatencyMs(result.latencyMs());
        output.setEstimatedCostUsd(estimateCost(modelName, result.promptTokens(), result.candidatesTokens(), result.thinkingTokens()));
        output.setFinishReason(result.finishReason());
        output.setHttpStatusCode(result.httpStatusCode());
        output.setPromptTemplateVersion(promptVersion);
        output.setSuccess(true);

        return repository.save(output);
    }

    /**
     * Log a failed AI generation call.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public GenerationOutput logFailure(
            GenerationTaskType taskType,
            String modelName,
            String systemPrompt,
            String userPrompt,
            CourseGenerationJob job,
            LectureContentTask task,
            long latencyMs,
            String errorMessage) {

        GenerationOutput output = new GenerationOutput();
        output.setJob(job);
        output.setTask(task);
        output.setTaskType(taskType);
        output.setModelName(modelName);
        output.setSystemPromptHash(sha256(systemPrompt));
        output.setSystemPrompt(systemPrompt);
        output.setUserPrompt(userPrompt);
        output.setLatencyMs(latencyMs);
        output.setErrorMessage(errorMessage);
        output.setSuccess(false);

        return repository.save(output);
    }

    // ── Batch-aware overloads ──

    /**
     * Log a batch success. Joins the caller's transaction so it can see
     * entities (job, task, batch) created in the same transaction.
     */
    @Transactional
    public GenerationOutput logBatchSuccess(
            AiGenerationResult result,
            String modelName,
            String systemPrompt,
            String userPrompt,
            CourseGenerationJob job,
            LectureContentTask task,
            OutputParseStrategy parseStrategy,
            PromptTemplateVersion promptVersion) {

        return logSuccess(
                result, GenerationTaskType.LECTURE_CONTENT,
                modelName, systemPrompt, userPrompt, job, task, parseStrategy, promptVersion);
    }

    /**
     * Log a batch failure. Joins the caller's transaction so it can see
     * entities (job, task, batch) created in the same transaction.
     */
    @Transactional
    public GenerationOutput logBatchFailure(
            String modelName,
            String systemPrompt,
            String userPrompt,
            CourseGenerationJob job,
            LectureContentTask task,
            long latencyMs,
            String errorMessage,
            PromptTemplateVersion promptVersion) {

        GenerationOutput output = logFailure(
                GenerationTaskType.LECTURE_CONTENT,
                modelName, systemPrompt, userPrompt, job, task, latencyMs, errorMessage);
        output.setPromptTemplateVersion(promptVersion);
        return repository.save(output);
    }

    // ── Cost estimation ──

    /**
     * Estimate cost in USD based on Gemini 2.5 Flash pricing (per 1M tokens):
     * Input: $0.15, Output: $0.60, Thinking: $0.70
     */
    static BigDecimal estimateCost(String model, Integer promptTokens, Integer candidatesTokens, Integer thinkingTokens) {
        if (promptTokens == null && candidatesTokens == null) return null;

        // Default to Gemini 2.5 Flash pricing
        BigDecimal inputRate = new BigDecimal("0.15");   // per 1M tokens
        BigDecimal outputRate = new BigDecimal("0.60");  // per 1M tokens
        BigDecimal thinkingRate = new BigDecimal("0.70"); // per 1M tokens

        BigDecimal million = new BigDecimal("1000000");
        BigDecimal cost = BigDecimal.ZERO;

        if (promptTokens != null) {
            cost = cost.add(new BigDecimal(promptTokens).multiply(inputRate).divide(million, 6, RoundingMode.HALF_UP));
        }
        if (candidatesTokens != null) {
            cost = cost.add(new BigDecimal(candidatesTokens).multiply(outputRate).divide(million, 6, RoundingMode.HALF_UP));
        }
        if (thinkingTokens != null) {
            cost = cost.add(new BigDecimal(thinkingTokens).multiply(thinkingRate).divide(million, 6, RoundingMode.HALF_UP));
        }

        return cost;
    }

    // ── Utilities ──

    private static String sha256(String input) {
        if (input == null) return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            return null;
        }
    }
}
