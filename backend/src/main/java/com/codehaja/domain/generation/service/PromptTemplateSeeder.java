package com.codehaja.domain.generation.service;

import com.codehaja.domain.generation.config.PromptDefaults;
import com.codehaja.domain.generation.config.PromptTemplateNames;
import com.codehaja.domain.generation.entity.*;
import com.codehaja.domain.generation.repository.PromptTemplateRepository;
import com.codehaja.domain.generation.repository.PromptTemplateVersionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds prompt templates on startup and auto-upgrades when Java defaults change.
 *
 * On each startup:
 * - New templates are created with v1.
 * - Existing templates whose active version content differs from the Java default
 *   get a new version auto-created and activated.
 */
@Component
@RequiredArgsConstructor
public class PromptTemplateSeeder {

    private static final Logger log = LoggerFactory.getLogger(PromptTemplateSeeder.class);

    private final PromptTemplateRepository templateRepository;
    private final PromptTemplateVersionRepository versionRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        int created = 0;
        int upgraded = 0;

        int[] r;

        r = seedOrUpdate(
                PromptTemplateNames.COURSE_STRUCTURE_SYSTEM,
                GenerationTaskType.COURSE_OUTLINE,
                "System prompt for course structure generation (all topic types)");
        created += r[0]; upgraded += r[1];

        r = seedOrUpdate(
                PromptTemplateNames.LECTURE_CONTENT_SYSTEM_BASE,
                GenerationTaskType.LECTURE_CONTENT,
                "Base system prompt for lecture content (role, RICH_TEXT, CHECKPOINT, engagement rules)");
        created += r[0]; upgraded += r[1];

        r = seedOrUpdate(
                PromptTemplateNames.LECTURE_CONTENT_OVERLAY_MATH,
                GenerationTaskType.LECTURE_CONTENT,
                "Math/science content overlay (LaTeX, aligned equations, graphs)");
        created += r[0]; upgraded += r[1];

        r = seedOrUpdate(
                PromptTemplateNames.LECTURE_CONTENT_OVERLAY_ALGO,
                GenerationTaskType.LECTURE_CONTENT,
                "Algorithm content overlay (thinking process, trace tables, LeetCode)");
        created += r[0]; upgraded += r[1];

        r = seedOrUpdate(
                PromptTemplateNames.LECTURE_CONTENT_OVERLAY_GENERAL,
                GenerationTaskType.LECTURE_CONTENT,
                "General programming content overlay (code examples, input/output)");
        created += r[0]; upgraded += r[1];

        r = seedOrUpdate(
                PromptTemplateNames.LECTURE_CONTENT_OVERLAY_INTERVIEW,
                GenerationTaskType.LECTURE_CONTENT,
                "Technical interview prep overlay (debug, optimize, follow-up drills, mock interview)");
        created += r[0]; upgraded += r[1];

        r = seedOrUpdate(
                PromptTemplateNames.LECTURE_CONTENT_QUIZ_RULES,
                GenerationTaskType.LECTURE_CONTENT,
                "Quiz format rules (MC questions, difficulty progression, Korean workbook style)");
        created += r[0]; upgraded += r[1];

        r = seedOrUpdate(
                PromptTemplateNames.LECTURE_CONTENT_QUIZ_MATH_OVERLAY,
                GenerationTaskType.LECTURE_CONTENT,
                "Math quiz overlay (LaTeX in questions, computation problems)");
        created += r[0]; upgraded += r[1];

        r = seedOrUpdate(
                PromptTemplateNames.LECTURE_CONTENT_CODING_RULES,
                GenerationTaskType.LECTURE_CONTENT,
                "Coding set format rules (TITLE, LANG, DESCRIPTION, STARTER, TEST_CASE)");
        created += r[0]; upgraded += r[1];

        r = seedOrUpdate(
                PromptTemplateNames.LECTURE_CONTENT_USER_REQUIREMENTS,
                GenerationTaskType.LECTURE_CONTENT,
                "Shared requirements block appended to lecture content user prompts");
        created += r[0]; upgraded += r[1];

        if (created > 0 || upgraded > 0) {
            log.info("PromptTemplateSeeder: created={}, upgraded={}", created, upgraded);
        } else {
            log.debug("PromptTemplateSeeder: all templates up-to-date");
        }
    }

    /**
     * @return int[]{created, upgraded} — each 0 or 1
     */
    private int[] seedOrUpdate(String name, GenerationTaskType taskType, String description) {
        String defaultContent = PromptDefaults.getDefault(name);
        PromptTemplate existing = templateRepository.findByName(name).orElse(null);

        if (existing == null) {
            // Brand new template — create with v1
            PromptTemplate template = new PromptTemplate();
            template.setName(name);
            template.setTaskType(taskType);
            template.setDescription(description);
            template.setStatus(PromptTemplateStatus.ACTIVE);
            template = templateRepository.save(template);

            createVersion(template, 1, defaultContent, "Initial version (auto-seeded)");
            log.info("  Seeded template '{}' v1", name);
            return new int[]{1, 0};
        }

        // Template exists — check if active version content matches Java default
        PromptTemplateVersion activeVersion = versionRepository
                .findByTemplateIdAndIsActiveTrue(existing.getId())
                .orElse(null);

        if (activeVersion != null && activeVersion.getContent().strip().equals(defaultContent.strip())) {
            return new int[]{0, 0}; // Already up-to-date
        }

        // Content differs — create new version
        int nextVersion = versionRepository
                .findByTemplateIdOrderByVersionNumberDesc(existing.getId())
                .stream().findFirst()
                .map(v -> v.getVersionNumber() + 1)
                .orElse(1);

        // Deactivate current active version
        if (activeVersion != null) {
            activeVersion.setIsActive(false);
            versionRepository.save(activeVersion);
        }

        createVersion(existing, nextVersion, defaultContent, "Auto-upgraded from Java defaults");
        log.info("  Upgraded template '{}' to v{}", name, nextVersion);
        return new int[]{0, 1};
    }

    private void createVersion(PromptTemplate template, int versionNumber, String content, String notes) {
        PromptTemplateVersion version = new PromptTemplateVersion();
        version.setTemplate(template);
        version.setVersionNumber(versionNumber);
        version.setContent(content);
        version.setChangeNotes(notes);
        version.setIsActive(true);
        version.setCreatedBy("system");
        versionRepository.save(version);
    }
}
