package com.codehaja.domain.generation.dto;

/**
 * Result from an AI generation call, including content and metadata for logging.
 */
public record AiGenerationResult(
        String rawResponseBody,
        String content,
        String finishReason,
        Integer promptTokens,
        Integer candidatesTokens,
        Integer totalTokens,
        Integer thinkingTokens,
        long latencyMs,
        boolean structuredSchemaUsed,
        int httpStatusCode
) {}
