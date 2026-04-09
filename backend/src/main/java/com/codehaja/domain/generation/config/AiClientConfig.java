package com.codehaja.domain.generation.config;

import com.codehaja.domain.generation.service.AiClient;
import com.codehaja.domain.generation.service.ClaudeApiClient;
import com.codehaja.domain.generation.service.GeminiApiClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class AiClientConfig {

    private static final Logger log = LoggerFactory.getLogger(AiClientConfig.class);

    @Value("${app.ai.provider:gemini}")
    private String provider;

    @Bean
    @Primary
    public AiClient aiClient(ClaudeApiClient claudeApiClient, GeminiApiClient geminiApiClient) {
        return switch (provider.toLowerCase()) {
            case "claude", "anthropic" -> {
                log.info("AI provider: Claude");
                yield claudeApiClient;
            }
            case "gemini", "google" -> {
                log.info("AI provider: Gemini");
                yield geminiApiClient;
            }
            default -> {
                log.warn("Unknown AI provider '{}', defaulting to Gemini", provider);
                yield geminiApiClient;
            }
        };
    }
}
