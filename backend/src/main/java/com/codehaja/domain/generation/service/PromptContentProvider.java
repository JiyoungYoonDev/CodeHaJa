package com.codehaja.domain.generation.service;

import com.codehaja.domain.generation.config.PromptDefaults;
import com.codehaja.domain.generation.entity.PromptTemplateVersion;
import com.codehaja.domain.generation.repository.PromptTemplateVersionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Provides prompt content by template name.
 * Primary source: active PromptTemplateVersion from DB.
 * Fallback: hardcoded defaults from PromptDefaults (migration path).
 */
@Service
@RequiredArgsConstructor
public class PromptContentProvider {

    private static final Logger log = LoggerFactory.getLogger(PromptContentProvider.class);

    private final PromptTemplateVersionRepository versionRepository;

    /**
     * Get the content string for a template by name.
     * Reads from DB (active version), falls back to hardcoded default.
     */
    public String getActiveContent(String templateName) {
        return versionRepository.findActiveByTemplateName(templateName)
                .map(PromptTemplateVersion::getContent)
                .orElseGet(() -> {
                    log.debug("No active DB version for template '{}', using hardcoded default", templateName);
                    return PromptDefaults.getDefault(templateName);
                });
    }

    /**
     * Get the active PromptTemplateVersion entity for a template name.
     * Used for linking to GenerationOutput for traceability.
     */
    public Optional<PromptTemplateVersion> getActiveVersion(String templateName) {
        return versionRepository.findActiveByTemplateName(templateName);
    }
}
