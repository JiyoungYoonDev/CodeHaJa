package com.codehaja.domain.generation.service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.StopReason;
import com.codehaja.common.exception.BusinessException;
import com.codehaja.common.exception.ErrorCode;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ClaudeApiClient implements AiClient {

    private static final Logger log = LoggerFactory.getLogger(ClaudeApiClient.class);

    @Value("${app.ai.api-key:}")
    private String apiKey;

    @Value("${app.ai.model:claude-sonnet-4-20250514}")
    private String model;

    @Value("${app.ai.max-tokens:8192}")
    private long maxTokens;

    private AnthropicClient client;

    @PostConstruct
    public void init() {
        if (apiKey != null && !apiKey.isBlank()) {
            this.client = AnthropicOkHttpClient.builder()
                    .apiKey(apiKey)
                    .build();
            log.info("Claude API client initialized with model: {}", model);
        } else {
            log.warn("ANTHROPIC_API_KEY is not set. AI generation will be unavailable.");
        }
    }

    @Override
    public String generate(String systemPrompt, String userPrompt) {
        return generate(systemPrompt, userPrompt, maxTokens);
    }

    @Override
    public String generate(String systemPrompt, String userPrompt, long tokenLimit) {
        if (client == null) {
            throw new BusinessException(ErrorCode.AI_NOT_CONFIGURED,
                    "AI service is not configured. Set the ANTHROPIC_API_KEY environment variable.");
        }

        try {
            MessageCreateParams params = MessageCreateParams.builder()
                    .model(model)
                    .maxTokens(tokenLimit)
                    .system(systemPrompt)
                    .addUserMessage(userPrompt)
                    .build();

            log.info("Sending AI generation request. Model: {}, MaxTokens: {}", model, maxTokens);

            Message message = client.messages().create(params);

            String result = message.content().stream()
                    .filter(ContentBlock::isText)
                    .map(block -> block.asText().text())
                    .findFirst()
                    .orElse(null);

            if (result == null || result.isBlank()) {
                log.error("AI returned empty response");
                throw new BusinessException(ErrorCode.AI_GENERATION_FAILED, "AI returned empty content.");
            }

            boolean truncated = message.stopReason()
                    .map(r -> r.equals(StopReason.MAX_TOKENS))
                    .orElse(false);

            if (truncated) {
                log.error("AI response was truncated (hit max_tokens={})", maxTokens);
                throw new BusinessException(ErrorCode.AI_GENERATION_FAILED,
                        "AI output was truncated. Try fewer sections or a simpler topic.");
            }

            log.info("AI generation completed. Response length: {} chars", result.length());
            return result;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI generation failed: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.AI_GENERATION_FAILED,
                    "AI generation failed: " + e.getMessage());
        }
    }
}
