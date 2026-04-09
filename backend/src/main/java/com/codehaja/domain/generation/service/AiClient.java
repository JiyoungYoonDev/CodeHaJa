package com.codehaja.domain.generation.service;

import com.codehaja.domain.generation.dto.AiGenerationResult;

public interface AiClient {

    // Legacy methods — kept for backward compatibility
    String generate(String systemPrompt, String userPrompt);

    String generate(String systemPrompt, String userPrompt, long maxTokens);

    /**
     * Generate with structured output support and full metadata.
     *
     * @param systemPrompt      the system prompt
     * @param userPrompt        the user prompt
     * @param maxTokens         max output tokens
     * @param responseJsonSchema JSON string of the response schema (null = no schema enforcement)
     * @return result with content, token usage, latency, and metadata
     */
    default AiGenerationResult generateStructured(
            String systemPrompt, String userPrompt,
            long maxTokens, String responseJsonSchema) {
        // Default: wraps legacy generate() with timing. ClaudeApiClient inherits this.
        long start = System.currentTimeMillis();
        String content = generate(systemPrompt, userPrompt, maxTokens);
        long elapsed = System.currentTimeMillis() - start;
        return new AiGenerationResult(
                null, content, "STOP",
                null, null, null, null,
                elapsed, false, 200
        );
    }
}
