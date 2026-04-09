package com.codehaja.domain.generation.service;

import com.codehaja.common.exception.BusinessException;
import com.codehaja.common.exception.ErrorCode;
import com.codehaja.domain.generation.dto.AiGenerationResult;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class GeminiApiClient implements AiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiApiClient.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.ai.gemini.api-key:}")
    private String apiKey;

    @Value("${app.ai.gemini.model:gemini-2.5-flash}")
    private String model;

    @Value("${app.ai.gemini.max-tokens:16384}")
    private long maxTokens;

    private HttpClient httpClient;

    @PostConstruct
    public void init() {
        if (apiKey != null && !apiKey.isBlank()) {
            this.httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(30))
                    .build();
            log.info("Gemini API client initialized with model: {}", model);
        } else {
            log.warn("GEMINI_API_KEY is not set. Gemini AI generation will be unavailable.");
        }
    }

    public String getModel() {
        return model;
    }

    // ── Legacy interface ──

    @Override
    public String generate(String systemPrompt, String userPrompt) {
        return generate(systemPrompt, userPrompt, maxTokens);
    }

    @Override
    public String generate(String systemPrompt, String userPrompt, long tokenLimit) {
        AiGenerationResult result = generateStructured(systemPrompt, userPrompt, tokenLimit, null);
        return result.content();
    }

    // ── Structured output with full metadata ──

    @Override
    public AiGenerationResult generateStructured(
            String systemPrompt, String userPrompt,
            long tokenLimit, String responseJsonSchema) {

        if (httpClient == null) {
            throw new BusinessException(ErrorCode.AI_NOT_CONFIGURED,
                    "Gemini AI service is not configured. Set the GEMINI_API_KEY environment variable.");
        }

        long startMs = System.currentTimeMillis();
        int httpStatus = 0;

        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                    + model + ":generateContent?key=" + apiKey;

            // Build request with optional schema
            JsonNode schemaNode = null;
            boolean schemaUsed = false;
            if (responseJsonSchema != null && !responseJsonSchema.isBlank()) {
                schemaNode = objectMapper.readTree(responseJsonSchema);
                schemaUsed = true;
            }

            Object requestObj = new GeminiRequest(systemPrompt, userPrompt, tokenLimit, schemaNode);
            String requestBody = objectMapper.writeValueAsString(requestObj);

            log.info("Sending Gemini generation request. Model: {}, MaxTokens: {}, Schema: {}",
                    model, tokenLimit, schemaUsed);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .timeout(Duration.ofMinutes(5))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long latencyMs = System.currentTimeMillis() - startMs;
            httpStatus = response.statusCode();

            if (response.statusCode() != 200) {
                log.error("Gemini API error: status={}, body={}", response.statusCode(), response.body());
                return new AiGenerationResult(
                        response.body(), null, null,
                        null, null, null, null,
                        latencyMs, schemaUsed, httpStatus
                );
            }

            String rawBody = response.body();
            String content = extractText(rawBody);
            String finishReason = extractFinishReason(rawBody);
            UsageMetadata usage = extractUsageMetadata(rawBody);

            if (content == null || content.isBlank()) {
                log.error("Gemini returned empty response");
                throw new BusinessException(ErrorCode.AI_GENERATION_FAILED, "Gemini returned empty content.");
            }

            if ("MAX_TOKENS".equals(finishReason)) {
                log.warn("Gemini response was truncated (MAX_TOKENS)");
            }

            log.info("Gemini generation completed. Response: {} chars, Tokens: prompt={} candidates={} total={}, Latency: {}ms",
                    content.length(),
                    usage.promptTokenCount, usage.candidatesTokenCount, usage.totalTokenCount,
                    latencyMs);

            return new AiGenerationResult(
                    rawBody, content, finishReason,
                    usage.promptTokenCount, usage.candidatesTokenCount,
                    usage.totalTokenCount, usage.thoughtsTokenCount,
                    latencyMs, schemaUsed, httpStatus
            );

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            long latencyMs = System.currentTimeMillis() - startMs;
            log.error("Gemini generation failed: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.AI_GENERATION_FAILED,
                    "Gemini generation failed: " + e.getMessage());
        }
    }

    // ── Response parsing ──

    private String extractText(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode candidates = root.path("candidates");
        if (candidates.isMissingNode() || candidates.isEmpty()) {
            return null;
        }
        JsonNode parts = candidates.get(0).path("content").path("parts");
        if (parts.isMissingNode() || parts.isEmpty()) {
            return null;
        }
        return parts.get(0).path("text").asText(null);
    }

    private String extractFinishReason(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            return root.path("candidates").get(0).path("finishReason").asText("");
        } catch (Exception e) {
            return "";
        }
    }

    private UsageMetadata extractUsageMetadata(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode usage = root.path("usageMetadata");
            if (usage.isMissingNode()) return new UsageMetadata(null, null, null, null);
            return new UsageMetadata(
                    usage.has("promptTokenCount") ? usage.get("promptTokenCount").asInt() : null,
                    usage.has("candidatesTokenCount") ? usage.get("candidatesTokenCount").asInt() : null,
                    usage.has("totalTokenCount") ? usage.get("totalTokenCount").asInt() : null,
                    usage.has("thoughtsTokenCount") ? usage.get("thoughtsTokenCount").asInt() : null
            );
        } catch (Exception e) {
            return new UsageMetadata(null, null, null, null);
        }
    }

    private record UsageMetadata(Integer promptTokenCount, Integer candidatesTokenCount,
                                 Integer totalTokenCount, Integer thoughtsTokenCount) {}

    // ── Request structures ──

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record GeminiRequest(
            SystemInstruction system_instruction,
            java.util.List<Content> contents,
            GenerationConfig generationConfig
    ) {
        GeminiRequest(String systemPrompt, String userPrompt, long maxTokens, JsonNode responseSchema) {
            this(
                    new SystemInstruction(java.util.List.of(new Part(systemPrompt))),
                    java.util.List.of(new Content("user", java.util.List.of(new Part(userPrompt)))),
                    new GenerationConfig(
                            maxTokens,
                            new ThinkingConfig(8192),
                            responseSchema != null ? "application/json" : null,
                            responseSchema
                    )
            );
        }
    }

    private record SystemInstruction(java.util.List<Part> parts) {}
    private record Content(String role, java.util.List<Part> parts) {}
    private record Part(String text) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record GenerationConfig(
            long maxOutputTokens,
            ThinkingConfig thinkingConfig,
            String responseMimeType,
            JsonNode responseSchema
    ) {}

    private record ThinkingConfig(long thinkingBudget) {}
}
