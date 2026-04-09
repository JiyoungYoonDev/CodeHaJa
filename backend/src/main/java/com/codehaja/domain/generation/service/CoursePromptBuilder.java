package com.codehaja.domain.generation.service;

import com.codehaja.domain.generation.config.PromptTemplateNames;
import com.codehaja.domain.generation.dto.CourseGenerationDto;
import com.codehaja.domain.generation.entity.TopicCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Thin assembler — composes final prompts from:
 *   1. Active prompt template versions (DB-backed via PromptContentProvider)
 *   2. Request context (topic, level, audience, etc.)
 *   3. Topic classification result (TopicCategory, resolved by caller)
 *
 * No hardcoded prompt bodies. All prompt text comes from PromptContentProvider.
 */
@Component
@RequiredArgsConstructor
public class CoursePromptBuilder {

    private final PromptContentProvider promptContent;

    // ── Phase 1: Structure generation ──

    public String buildStructureSystemPrompt() {
        return promptContent.getActiveContent(PromptTemplateNames.COURSE_STRUCTURE_SYSTEM);
    }

    public String buildStructureUserPrompt(CourseGenerationDto.GenerateRequest request,
                                            TopicCategory topicCategory) {
        var sb = new StringBuilder();
        sb.append("Design a comprehensive course structure for:\n\n");
        sb.append("Topic: ").append(request.getTopic()).append("\n");

        appendRequestContext(sb, request);

        if (request.getNumberOfSections() != null && request.getNumberOfSections() > 0) {
            sb.append("Number of Sections: ").append(request.getNumberOfSections()).append("\n");
        } else {
            sb.append("Number of Sections: use your judgment (enough to cover the topic thoroughly)\n");
        }

        appendTopicHints(sb, topicCategory);

        sb.append("\n\nCreate a structure that covers the ENTIRE topic comprehensively.");
        sb.append("\nEach section = one major chapter. Each lecture = one specific lesson.");

        return sb.toString();
    }

    public String buildContinuationUserPrompt(CourseGenerationDto.GenerateRequest request,
                                               List<String> existingSectionTitles,
                                               TopicCategory topicCategory) {
        var sb = new StringBuilder();
        sb.append("Continue generating the course structure for:\n\n");
        sb.append("Topic: ").append(request.getTopic()).append("\n");

        if (request.getLevel() != null && !request.getLevel().isBlank()) {
            sb.append("Level: ").append(request.getLevel()).append("\n");
        }

        sb.append("\nThe following sections have ALREADY been generated. Do NOT repeat them:\n");
        for (int i = 0; i < existingSectionTitles.size(); i++) {
            sb.append("  ").append(i + 1).append(". ").append(existingSectionTitles.get(i)).append("\n");
        }

        Integer targetSections = request.getNumberOfSections();
        if (targetSections != null && targetSections > 0) {
            int remaining = targetSections - existingSectionTitles.size();
            sb.append("\nGenerate the NEXT ").append(remaining).append(" sections to complete the course.\n");
        } else {
            sb.append("\nGenerate the REMAINING sections to complete the course thoroughly.\n");
        }

        appendTopicHints(sb, topicCategory);

        sb.append("\n\nReturn the SAME JSON format as before — a complete course object with ONLY the new sections.");
        sb.append("\nThe title and description should match the original course.");

        return sb.toString();
    }

    // ── Phase 2: Content generation ──

    /**
     * Compose the content system prompt from DB-backed fragments.
     * Assembly order: base → topic overlay → quiz rules → quiz math overlay → coding rules → closing.
     */
    public String buildContentSystemPrompt(TopicCategory topicCategory) {
        var sb = new StringBuilder();

        // Base instructions (role, RICH_TEXT, CHECKPOINT, engagement rules)
        sb.append(promptContent.getActiveContent(PromptTemplateNames.LECTURE_CONTENT_SYSTEM_BASE));

        // Topic-specific overlay
        switch (topicCategory) {
            case MATH_SCIENCE ->
                    sb.append(promptContent.getActiveContent(PromptTemplateNames.LECTURE_CONTENT_OVERLAY_MATH));
            case ALGORITHM ->
                    sb.append(promptContent.getActiveContent(PromptTemplateNames.LECTURE_CONTENT_OVERLAY_ALGO));
            case TECHNICAL_INTERVIEW ->
                    sb.append(promptContent.getActiveContent(PromptTemplateNames.LECTURE_CONTENT_OVERLAY_INTERVIEW));
            case GENERAL_PROGRAMMING ->
                    sb.append(promptContent.getActiveContent(PromptTemplateNames.LECTURE_CONTENT_OVERLAY_GENERAL));
        }

        // Quiz rules (shared)
        sb.append(promptContent.getActiveContent(PromptTemplateNames.LECTURE_CONTENT_QUIZ_RULES));

        // Math quiz overlay (LaTeX in questions)
        if (topicCategory == TopicCategory.MATH_SCIENCE) {
            sb.append(promptContent.getActiveContent(PromptTemplateNames.LECTURE_CONTENT_QUIZ_MATH_OVERLAY));
        }

        // Coding set rules (not for math topics)
        if (topicCategory != TopicCategory.MATH_SCIENCE) {
            sb.append(promptContent.getActiveContent(PromptTemplateNames.LECTURE_CONTENT_CODING_RULES));
        }

        sb.append("""

                RESPONSE FORMAT — You MUST follow this structured output schema:
                Each item in the JSON array must have "itemTitle", "itemType", and the correct content field:

                - RICH_TEXT / CHECKPOINT items → put content in the "content" field (markdown/text string).
                - CODING_SET items → put content in the "codingContent" field (structured object).
                  Do NOT put coding data in "content". Use the "codingContent" object with fields:
                  title, language, description, evaluationStyle, functionName, starterCode, hint, testCases.
                  Choose evaluationStyle: "FUNCTION" (algorithm) or "CONSOLE" (beginner I/O).
                  ALL testCases use: {"input": "<string>", "expectedOutput": "<string>"}.
                  FUNCTION: input = JSON args array string, expectedOutput = JSON return value string.
                  CONSOLE: input = stdin text, expectedOutput = expected stdout text.
                  starterCode must NOT be the complete solution — the student writes the implementation.
                - QUIZ_SET items → put content in the "quizContent" field (array of quiz question objects).
                  Do NOT put quiz data in "content". Use the "quizContent" array with objects:
                  {question, options: [{letter, text}], answer (the correct letter), explanation}.

                Return ONLY the JSON array. No markdown fences.
                """);

        return sb.toString();
    }

    public String buildLectureContentUserPrompt(String sectionTitle, String lectureTitle,
                                                 List<ItemOutline> items,
                                                 CourseGenerationDto.GenerateRequest request) {
        var sb = new StringBuilder();
        sb.append("Generate complete educational content for this SINGLE lecture:\n\n");
        sb.append("Course Topic: ").append(request.getTopic()).append("\n");
        sb.append("Section: ").append(sectionTitle).append("\n");
        sb.append("Lecture: ").append(lectureTitle).append("\n");

        appendRequestContext(sb, request);

        sb.append("\nItems to generate content for:\n");
        for (var item : items) {
            sb.append("  - ").append(item.title())
                    .append(" [").append(item.itemType()).append("]");
            if (item.description() != null && !item.description().isBlank()) {
                sb.append("\n    → ").append(item.description());
            }
            sb.append("\n");
        }

        // Append DB-backed requirements
        sb.append(promptContent.getActiveContent(PromptTemplateNames.LECTURE_CONTENT_USER_REQUIREMENTS));

        return sb.toString();
    }

    /**
     * Build a batch-aware user prompt that generates content for only a subset of lecture items.
     * The full lecture outline is included for context so the AI understands item flow.
     */
    public String buildBatchContentUserPrompt(
            String sectionTitle, String lectureTitle,
            List<ItemOutline> allLectureItems,
            List<ItemOutline> batchItems,
            int batchIndex, int totalBatches,
            CourseGenerationDto.GenerateRequest request) {

        var sb = new StringBuilder();
        sb.append("Generate educational content for a SUBSET of items from this lecture.\n\n");
        sb.append("Course Topic: ").append(request.getTopic()).append("\n");
        sb.append("Section: ").append(sectionTitle).append("\n");
        sb.append("Lecture: ").append(lectureTitle).append("\n");

        appendRequestContext(sb, request);

        // Full lecture outline for context — includes descriptions so AI understands flow
        sb.append("\n=== FULL LECTURE OUTLINE (for context — do NOT generate items outside the batch) ===\n");
        for (int i = 0; i < allLectureItems.size(); i++) {
            var item = allLectureItems.get(i);
            sb.append("  ").append(i + 1).append(". ")
                    .append(item.title()).append(" [").append(item.itemType()).append("]");
            if (item.description() != null && !item.description().isBlank()) {
                sb.append("\n     → ").append(item.description());
            }
            sb.append("\n");
        }

        // The actual batch to generate
        sb.append("\n=== GENERATE CONTENT FOR THESE ITEMS ONLY (batch ")
                .append(batchIndex + 1).append("/").append(totalBatches).append(") ===\n");
        for (var item : batchItems) {
            sb.append("  - ").append(item.title())
                    .append(" [").append(item.itemType()).append("]");
            if (item.description() != null && !item.description().isBlank()) {
                sb.append("\n    → ").append(item.description());
            }
            sb.append("\n");
        }

        sb.append("\n=== STRICT BATCH SCOPE ===\n");
        sb.append("Generate content ONLY for the ").append(batchItems.size())
                .append(" item(s) listed in the batch section above.\n");
        sb.append("Do NOT generate content for any other lecture items.\n");
        sb.append("Return exactly one entry per requested itemTitle.\n");
        sb.append("If this batch contains one item, return exactly one item.\n");
        sb.append("Do not continue beyond the listed item titles.\n");
        sb.append("The 'itemTitle' must EXACTLY match the titles listed in the batch section.\n");
        sb.append("Return the same JSON array format: [{\"itemTitle\": \"exact title\", \"content\": \"full content\"}]\n");
        sb.append("=== END BATCH SCOPE ===\n");

        // Append DB-backed requirements
        sb.append(promptContent.getActiveContent(PromptTemplateNames.LECTURE_CONTENT_USER_REQUIREMENTS));

        return sb.toString();
    }

    // ── Granular generation: Add Section / Add Lecture ──

    public String buildAddSectionUserPrompt(
            String courseTitle, String courseDescription, String courseDifficulty,
            List<String> existingSectionTitles,
            String sectionTopic, Integer numberOfLectures,
            String extraInstructions, TopicCategory topicCategory) {

        var sb = new StringBuilder();
        sb.append("Generate a NEW section to add to an existing course.\n\n");
        sb.append("=== EXISTING COURSE CONTEXT ===\n");
        sb.append("Course Title: ").append(courseTitle).append("\n");
        if (courseDescription != null && !courseDescription.isBlank()) {
            sb.append("Course Description: ").append(courseDescription).append("\n");
        }
        sb.append("Difficulty: ").append(courseDifficulty != null ? courseDifficulty : "BEGINNER").append("\n\n");

        sb.append("Existing sections (do NOT duplicate these):\n");
        for (int i = 0; i < existingSectionTitles.size(); i++) {
            sb.append("  ").append(i + 1).append(". ").append(existingSectionTitles.get(i)).append("\n");
        }

        sb.append("\n=== NEW SECTION TO GENERATE ===\n");
        sb.append("Section Topic: ").append(sectionTopic).append("\n");
        if (numberOfLectures != null && numberOfLectures > 0) {
            sb.append("Number of Lectures: ").append(numberOfLectures).append("\n");
        } else {
            sb.append("Number of Lectures: use your judgment (3-5 is typical)\n");
        }

        if (extraInstructions != null && !extraInstructions.isBlank()) {
            sb.append("\n=== IMPORTANT: Admin Instructions ===\n");
            sb.append(extraInstructions).append("\n=== END ===\n");
        }

        appendTopicHints(sb, topicCategory);

        sb.append("\n\nGenerate EXACTLY ONE new section that covers this topic.");
        sb.append("\nReturn the response in the same course structure JSON format,");
        sb.append(" with the 'sections' array containing ONLY the single new section.");
        sb.append("\nThe new section should complement the existing sections, not duplicate content.");

        return sb.toString();
    }

    public String buildAddLectureUserPrompt(
            String courseTitle, String sectionTitle,
            List<String> existingLectureTitles,
            String lectureTopic, String extraInstructions,
            TopicCategory topicCategory) {

        var sb = new StringBuilder();
        sb.append("Generate a NEW lecture to add to an existing section.\n\n");
        sb.append("=== EXISTING CONTEXT ===\n");
        sb.append("Course: ").append(courseTitle).append("\n");
        sb.append("Section: ").append(sectionTitle).append("\n\n");

        sb.append("Existing lectures in this section (do NOT duplicate):\n");
        for (int i = 0; i < existingLectureTitles.size(); i++) {
            sb.append("  ").append(i + 1).append(". ").append(existingLectureTitles.get(i)).append("\n");
        }

        sb.append("\n=== NEW LECTURE TO GENERATE ===\n");
        sb.append("Lecture Topic: ").append(lectureTopic).append("\n");

        if (extraInstructions != null && !extraInstructions.isBlank()) {
            sb.append("\n=== IMPORTANT: Admin Instructions ===\n");
            sb.append(extraInstructions).append("\n=== END ===\n");
        }

        appendTopicHints(sb, topicCategory);

        sb.append("\n\nGenerate EXACTLY ONE new lecture with its lecture items.");
        sb.append("\nReturn in the course structure JSON format, with the 'sections' array");
        sb.append(" containing ONE section that has ONE lecture.");
        sb.append("\nThe lecture should complement the existing lectures, not duplicate content.");

        return sb.toString();
    }

    public String buildAddItemUserPrompt(
            String courseTitle, String sectionTitle, String lectureTitle,
            List<String> existingItemTitles,
            String itemTopic, String itemType,
            String extraInstructions, TopicCategory topicCategory) {

        var sb = new StringBuilder();
        sb.append("Generate a NEW lecture item to add to an existing lecture.\n\n");
        sb.append("=== EXISTING CONTEXT ===\n");
        sb.append("Course: ").append(courseTitle).append("\n");
        sb.append("Section: ").append(sectionTitle).append("\n");
        sb.append("Lecture: ").append(lectureTitle).append("\n\n");

        sb.append("Existing items in this lecture (do NOT duplicate):\n");
        for (int i = 0; i < existingItemTitles.size(); i++) {
            sb.append("  ").append(i + 1).append(". ").append(existingItemTitles.get(i)).append("\n");
        }

        sb.append("\n=== NEW ITEM TO GENERATE ===\n");
        sb.append("Item Topic: ").append(itemTopic).append("\n");
        if (itemType != null && !itemType.isBlank()) {
            sb.append("Item Type: ").append(itemType).append("\n");
        } else {
            sb.append("Item Type: choose the most appropriate type (RICH_TEXT, QUIZ_SET, CODING_SET, or CHECKPOINT)\n");
        }

        if (extraInstructions != null && !extraInstructions.isBlank()) {
            sb.append("\n=== IMPORTANT: Admin Instructions ===\n");
            sb.append(extraInstructions).append("\n=== END ===\n");
        }

        appendTopicHints(sb, topicCategory);

        sb.append("\n\nGenerate EXACTLY ONE new lecture item.");
        sb.append("\nReturn in the course structure JSON format, with 'sections' containing ONE section");
        sb.append(" that has ONE lecture with ONE lecture item.");
        sb.append("\nThe item should complement the existing items, not duplicate content.");

        return sb.toString();
    }

    // ── Helper records ──

    public record LectureOutline(String title, List<ItemOutline> items) {}
    public record ItemOutline(String title, String itemType, String description) {
        /** Backward-compatible constructor without description */
        public ItemOutline(String title, String itemType) {
            this(title, itemType, null);
        }
    }

    // ── Private assembly helpers ──

    private void appendRequestContext(StringBuilder sb, CourseGenerationDto.GenerateRequest request) {
        if (request.getLevel() != null && !request.getLevel().isBlank()) {
            sb.append("Level: ").append(request.getLevel()).append("\n");
        }
        if (request.getTargetAudience() != null && !request.getTargetAudience().isBlank()) {
            sb.append("Target Audience: ").append(request.getTargetAudience()).append("\n");
        }
        if (request.getTone() != null && !request.getTone().isBlank()) {
            sb.append("Tone: ").append(request.getTone()).append("\n");
        }
        if (request.getExtraInstructions() != null && !request.getExtraInstructions().isBlank()) {
            sb.append("\n=== IMPORTANT: Admin Instructions (MUST follow these requirements) ===\n");
            sb.append(request.getExtraInstructions());
            sb.append("\n=== END Admin Instructions ===\n");
        }
    }

    private void appendTopicHints(StringBuilder sb, TopicCategory topicCategory) {
        switch (topicCategory) {
            case MATH_SCIENCE -> {
                sb.append("\nThis is a MATH/SCIENCE topic.");
                sb.append("\n- Use only RICH_TEXT and QUIZ_SET items (no CODING_SET).");
                sb.append("\n- Cover all major subtopics thoroughly.");
            }
            case ALGORITHM -> {
                sb.append("\nThis is an ALGORITHM/DATA STRUCTURE topic.");
                sb.append("\n- Use the ALGORITHM lecture item structure (7-8 items per lecture).");
                sb.append("\n- MUST include 'Thinking Process' RICH_TEXT showing step-by-step thought process.");
                sb.append("\n- MUST include Easy/Medium/Hard CODING_SET items with progressive difficulty.");
                sb.append("\n- MUST include 'LeetCode Practice' RICH_TEXT with real LeetCode problem links.");
                sb.append("\n- Set externalLinks on LeetCode Practice items (comma-separated URLs).");
            }
            case TECHNICAL_INTERVIEW -> {
                sb.append("\nThis is a TECHNICAL INTERVIEW PREP topic.");
                sb.append("\n- Use the INTERVIEW lecture item structure (7-8 items per lecture).");
                sb.append("\n- MUST include 'Debug Challenge' CODING_SET with bug-finding problems.");
                sb.append("\n- MUST include 'Coding Challenge' CODING_SET with implement/optimize problems.");
                sb.append("\n- MUST include 'Follow-up Drill' QUIZ_SET with 꼬리물기 style questions.");
                sb.append("\n- Focus on WHY, not just WHAT — interviewers always ask 'why?'.");
                sb.append("\n- Include CODING_SET items even for conceptual topics (debug, optimize, design).");
            }
            case GENERAL_PROGRAMMING -> {
                sb.append("\nThis is a PROGRAMMING topic.");
                sb.append("\n- Include CODING_SET items for hands-on practice.");
            }
        }
    }
}
