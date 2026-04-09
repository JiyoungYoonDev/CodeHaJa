package com.codehaja.domain.generation.config;

/**
 * Constants for prompt template names.
 * Each name maps 1:1 to a PromptTemplate row in DB.
 * The builder composes these fragments into final prompts.
 */
public final class PromptTemplateNames {

    private PromptTemplateNames() {}

    // ── Structure generation (Phase 1) ──
    public static final String COURSE_STRUCTURE_SYSTEM = "COURSE_STRUCTURE_SYSTEM";

    // ── Content generation system prompt fragments ──
    public static final String LECTURE_CONTENT_SYSTEM_BASE = "LECTURE_CONTENT_SYSTEM_BASE";
    public static final String LECTURE_CONTENT_OVERLAY_MATH = "LECTURE_CONTENT_OVERLAY_MATH";
    public static final String LECTURE_CONTENT_OVERLAY_ALGO = "LECTURE_CONTENT_OVERLAY_ALGO";
    public static final String LECTURE_CONTENT_OVERLAY_GENERAL = "LECTURE_CONTENT_OVERLAY_GENERAL";
    public static final String LECTURE_CONTENT_OVERLAY_INTERVIEW = "LECTURE_CONTENT_OVERLAY_INTERVIEW";
    public static final String LECTURE_CONTENT_QUIZ_RULES = "LECTURE_CONTENT_QUIZ_RULES";
    public static final String LECTURE_CONTENT_QUIZ_MATH_OVERLAY = "LECTURE_CONTENT_QUIZ_MATH_OVERLAY";
    public static final String LECTURE_CONTENT_CODING_RULES = "LECTURE_CONTENT_CODING_RULES";

    // ── User prompt fragments ──
    public static final String LECTURE_CONTENT_USER_REQUIREMENTS = "LECTURE_CONTENT_USER_REQUIREMENTS";
}
