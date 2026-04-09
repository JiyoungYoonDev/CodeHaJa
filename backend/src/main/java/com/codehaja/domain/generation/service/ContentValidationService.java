package com.codehaja.domain.generation.service;

import com.codehaja.domain.generation.entity.*;
import com.codehaja.domain.generation.repository.ValidationResultRepository;
import com.codehaja.domain.lectureitem.entity.LectureItem;
import com.codehaja.domain.lectureitem.entity.LectureItemType;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Validates generated content quality after AI generation.
 * 10 rules covering schema integrity, content quality, and type-specific validation.
 */
@Service
@RequiredArgsConstructor
public class ContentValidationService {

    private final ValidationResultRepository validationResultRepository;

    /**
     * Run all validation rules on a batch's generated content.
     */
    @Transactional
    public List<ValidationResult> validateBatch(GenerationOutput output,
                                                 LectureContentBatch batch,
                                                 List<LectureItem> batchItems,
                                                 int matchedCount,
                                                 String finishReason,
                                                 TopicCategory topicCategory) {
        List<ValidationResult> results = new ArrayList<>();

        // 1. STRUCTURED_SCHEMA_VALID — did the AI return valid structured schema output?
        results.add(checkStructuredSchema(output));

        // 2. NO_TRUNCATION — MAX_TOKENS check
        results.add(checkNoTruncation(output, finishReason));

        // 3. PARSE_SUCCESS — was parsing successful without fallback?
        results.add(checkParseSuccess(output, batch));

        // 4. ITEM_COUNT_MATCH — all expected items present?
        results.add(checkItemCountMatch(output, batchItems.size(), matchedCount));

        // 5. ITEM_TITLE_MATCH_RATE — how well did titles match?
        results.add(checkTitleMatchRate(output, batchItems.size(), matchedCount));

        // 6. REQUIRED_ITEMS_PRESENT — are required item types covered?
        results.add(checkRequiredItemsPresent(output, batchItems));

        // Per-item type-specific validation
        for (LectureItem item : batchItems) {
            JsonNode json = item.getContentJson();
            if (json == null || json.isMissingNode()) continue;

            // 7. CONTENT_NOT_EMPTY
            results.add(checkContentNotEmpty(output, item));

            // 8-10. Type-specific schema validation
            switch (item.getItemType()) {
                case CODING_SET -> results.add(checkCodingSchema(output, item));
                case QUIZ_SET -> results.add(checkQuizSchema(output, item));
                case CHECKPOINT -> results.add(checkCheckpointSchema(output, item));
                case RICH_TEXT -> {
                    results.add(checkMinContentLength(output, item));
                    if (topicCategory == TopicCategory.MATH_SCIENCE) {
                        results.add(checkGraphPresent(output, item));
                    }
                }
                default -> results.add(checkMinContentLength(output, item));
            }
        }

        validationResultRepository.saveAll(results);
        return results;
    }

    // ── 1. STRUCTURED_SCHEMA_VALID ──

    private ValidationResult checkStructuredSchema(GenerationOutput output) {
        boolean used = Boolean.TRUE.equals(output.getStructuredSchemaUsed());
        boolean success = used && Boolean.TRUE.equals(output.getSuccess());
        return build(output, "STRUCTURED_SCHEMA_VALID", success,
                success ? ValidationSeverity.INFO : ValidationSeverity.WARNING,
                used ? (success ? "Structured schema output parsed successfully"
                               : "Structured schema used but output failed")
                     : "Structured schema not used (text fallback)");
    }

    // ── 2. NO_TRUNCATION ──

    private ValidationResult checkNoTruncation(GenerationOutput output, String finishReason) {
        boolean truncated = "MAX_TOKENS".equalsIgnoreCase(finishReason);
        return build(output, "NO_TRUNCATION", !truncated,
                truncated ? ValidationSeverity.ERROR : ValidationSeverity.INFO,
                truncated ? "Output truncated (MAX_TOKENS) — batch may need smaller grouping"
                          : "Output complete");
    }

    // ── 3. PARSE_SUCCESS ──

    private ValidationResult checkParseSuccess(GenerationOutput output, LectureContentBatch batch) {
        OutputParseStrategy strategy = batch != null ? batch.getParseStrategy() : output.getParseStrategy();
        boolean clean = strategy == OutputParseStrategy.STRUCTURED_SCHEMA
                     || strategy == OutputParseStrategy.DIRECT_JSON;
        String strategyName = strategy != null ? strategy.name() : "NONE";
        return build(output, "PARSE_SUCCESS", clean,
                clean ? ValidationSeverity.INFO : ValidationSeverity.WARNING,
                clean ? "Clean parse via " + strategyName
                      : "Fallback parse used: " + strategyName + " — content may have lost structure");
    }

    // ── 4. ITEM_COUNT_MATCH ──

    private ValidationResult checkItemCountMatch(GenerationOutput output, int expected, int matched) {
        boolean perfect = matched == expected;
        ValidationSeverity severity;
        if (perfect) severity = ValidationSeverity.INFO;
        else if (matched == 0) severity = ValidationSeverity.ERROR;
        else severity = ValidationSeverity.WARNING;
        return build(output, "ITEM_COUNT_MATCH", perfect, severity,
                matched + "/" + expected + " items matched" +
                (perfect ? "" : " — " + (expected - matched) + " items missing"));
    }

    // ── 5. ITEM_TITLE_MATCH_RATE ──

    private ValidationResult checkTitleMatchRate(GenerationOutput output, int expected, int matched) {
        double rate = expected > 0 ? (matched * 100.0 / expected) : 0;
        boolean good = rate >= 80.0;
        return build(output, "ITEM_TITLE_MATCH_RATE", good,
                good ? ValidationSeverity.INFO : ValidationSeverity.WARNING,
                String.format("Title match rate: %.0f%% (%d/%d)", rate, matched, expected) +
                (good ? "" : " — check for title mismatches between prompt and AI output"));
    }

    // ── 6. REQUIRED_ITEMS_PRESENT ──

    private ValidationResult checkRequiredItemsPresent(GenerationOutput output, List<LectureItem> items) {
        Set<LectureItemType> expectedTypes = new java.util.HashSet<>();
        Set<LectureItemType> presentTypes = new java.util.HashSet<>();

        for (LectureItem item : items) {
            expectedTypes.add(item.getItemType());
            if (item.getContentJson() != null && !item.getContentJson().isEmpty()) {
                presentTypes.add(item.getItemType());
            }
        }

        Set<LectureItemType> missing = new java.util.HashSet<>(expectedTypes);
        missing.removeAll(presentTypes);

        boolean allPresent = missing.isEmpty();
        return build(output, "REQUIRED_ITEMS_PRESENT", allPresent,
                allPresent ? ValidationSeverity.INFO : ValidationSeverity.ERROR,
                allPresent ? "All required item types present: " + presentTypes
                           : "Missing item types: " + missing);
    }

    // ── 7. CONTENT_NOT_EMPTY ──

    private ValidationResult checkContentNotEmpty(GenerationOutput output, LectureItem item) {
        JsonNode json = item.getContentJson();
        boolean empty = json == null || json.isMissingNode() || json.isEmpty();
        return build(output, "CONTENT_NOT_EMPTY", !empty,
                empty ? ValidationSeverity.ERROR : ValidationSeverity.INFO,
                item.getTitle() + " [" + item.getItemType() + "]: " + (empty ? "EMPTY" : "has content"));
    }

    // ── 8. CODING_SCHEMA_VALID ──

    private ValidationResult checkCodingSchema(GenerationOutput output, LectureItem item) {
        JsonNode json = item.getContentJson();
        List<String> errors = new ArrayList<>();

        // Expected: { problems: [{ id, title, description, language, files: [...], testCases: [...] }] }
        JsonNode problems = json.path("problems");
        if (!problems.isArray() || problems.isEmpty()) {
            errors.add("missing 'problems' array");
        } else {
            JsonNode p = problems.get(0);
            if (p.path("title").asText("").isBlank()) errors.add("problem missing 'title'");
            if (p.path("language").asText("").isBlank()) errors.add("problem missing 'language'");

            JsonNode desc = p.path("description");
            if (desc.isMissingNode() || (desc.isObject() && desc.path("content").isEmpty())) {
                errors.add("problem missing 'description'");
            }

            JsonNode files = p.path("files");
            if (!files.isArray() || files.isEmpty()) {
                errors.add("problem missing 'files' (starter code)");
            } else {
                String code = files.get(0).path("content").asText("");
                if (code.isBlank()) errors.add("starter code is empty");
            }

            JsonNode testCases = p.path("testCases");
            if (!testCases.isArray() || testCases.isEmpty()) {
                errors.add("problem missing 'testCases'");
            } else {
                int validTc = 0;
                for (JsonNode tc : testCases) {
                    // Function-based: args + expected; Console-based: input + expectedOutput
                    boolean hasArgs = tc.has("args") && tc.has("expected");
                    boolean hasIO = !tc.path("input").asText("").isBlank()
                                 || !tc.path("expectedOutput").asText("").isBlank();
                    if (hasArgs || hasIO) validTc++;
                }
                if (validTc == 0) errors.add("all test cases are empty/invalid");
                else if (validTc < testCases.size()) errors.add(validTc + "/" + testCases.size() + " test cases valid");
            }
        }

        boolean passed = errors.isEmpty();
        return build(output, "CODING_SCHEMA_VALID", passed,
                passed ? ValidationSeverity.INFO : ValidationSeverity.ERROR,
                item.getTitle() + ": " + (passed ? "valid coding schema" : String.join("; ", errors)));
    }

    // ── 9. QUIZ_SCHEMA_VALID ──

    private ValidationResult checkQuizSchema(GenerationOutput output, LectureItem item) {
        JsonNode json = item.getContentJson();
        List<String> errors = new ArrayList<>();

        // Expected: { blocks: [{ type: "quiz", question, options: [{ text, isCorrect }], explanation }] }
        JsonNode blocks = json.path("blocks");
        if (!blocks.isArray() || blocks.isEmpty()) {
            errors.add("missing 'blocks' array");
        } else {
            int quizCount = 0;
            int noOptionsCount = 0;
            int noCorrectCount = 0;
            int noExplanationCount = 0;

            for (JsonNode block : blocks) {
                if (!"quiz".equals(block.path("type").asText(""))) continue;
                quizCount++;

                if (block.path("question").asText("").isBlank()) {
                    errors.add("quiz #" + quizCount + " missing question");
                }

                JsonNode options = block.path("options");
                if (!options.isArray() || options.size() < 2) {
                    noOptionsCount++;
                } else {
                    boolean hasCorrect = false;
                    for (JsonNode opt : options) {
                        if (Boolean.TRUE.equals(opt.path("isCorrect").asBoolean(false))) {
                            hasCorrect = true;
                            break;
                        }
                    }
                    if (!hasCorrect) noCorrectCount++;
                }

                if (block.path("explanation").asText("").isBlank()) {
                    noExplanationCount++;
                }
            }

            if (quizCount == 0) errors.add("no quiz blocks found");
            if (noOptionsCount > 0) errors.add(noOptionsCount + " quizzes have <2 options");
            if (noCorrectCount > 0) errors.add(noCorrectCount + " quizzes have no correct answer marked");
            if (noExplanationCount > 0) errors.add(noExplanationCount + "/" + quizCount + " quizzes missing explanation");
        }

        boolean passed = errors.isEmpty();
        return build(output, "QUIZ_SCHEMA_VALID", passed,
                passed ? ValidationSeverity.INFO : (errors.stream().anyMatch(e -> e.contains("no quiz") || e.contains("no correct")) ? ValidationSeverity.ERROR : ValidationSeverity.WARNING),
                item.getTitle() + ": " + (passed ? quizSummary(json) : String.join("; ", errors)));
    }

    // ── 10. CHECKPOINT_SCHEMA_VALID ──

    private ValidationResult checkCheckpointSchema(GenerationOutput output, LectureItem item) {
        JsonNode json = item.getContentJson();
        List<String> errors = new ArrayList<>();

        // Expected: { blocks: [{ type: "checkpoint"|"text", question, answer, ... }] }
        JsonNode blocks = json.path("blocks");
        if (!blocks.isArray() || blocks.isEmpty()) {
            errors.add("missing 'blocks' array");
        } else {
            int cpCount = 0;
            int noAnswerCount = 0;
            int noQuestionCount = 0;

            for (JsonNode block : blocks) {
                if (!"checkpoint".equals(block.path("type").asText(""))) continue;
                cpCount++;

                if (block.path("question").asText("").isBlank()) noQuestionCount++;
                if (block.path("answer").asText("").isBlank()) noAnswerCount++;
            }

            if (cpCount == 0) errors.add("no checkpoint blocks found");
            if (noQuestionCount > 0) errors.add(noQuestionCount + " checkpoints missing question");
            if (noAnswerCount > 0) errors.add(noAnswerCount + "/" + cpCount + " checkpoints missing answer");
        }

        boolean passed = errors.isEmpty();
        return build(output, "CHECKPOINT_SCHEMA_VALID", passed,
                passed ? ValidationSeverity.INFO : (errors.stream().anyMatch(e -> e.contains("no checkpoint")) ? ValidationSeverity.ERROR : ValidationSeverity.WARNING),
                item.getTitle() + ": " + (passed ? checkpointSummary(json) : String.join("; ", errors)));
    }

    // ── Helpers ──

    private ValidationResult checkMinContentLength(GenerationOutput output, LectureItem item) {
        JsonNode json = item.getContentJson();
        String text = json != null ? json.toString() : "";
        int minLength = switch (item.getItemType()) {
            case RICH_TEXT -> 500;
            case QUIZ_SET -> 300;
            case CHECKPOINT -> 200;
            case CODING_SET -> 200;
            default -> 100;
        };
        boolean passed = text.length() >= minLength;
        return build(output, "MIN_CONTENT_LENGTH", passed,
                passed ? ValidationSeverity.INFO : ValidationSeverity.WARNING,
                item.getTitle() + ": " + text.length() + " chars (min " + minLength + ")");
    }

    private ValidationResult checkGraphPresent(GenerationOutput output, LectureItem item) {
        JsonNode json = item.getContentJson();
        boolean hasGraph = json != null && json.toString().contains("graphBlock");
        return build(output, "GRAPH_PRESENT_FOR_MATH", hasGraph,
                hasGraph ? ValidationSeverity.INFO : ValidationSeverity.WARNING,
                item.getTitle() + ": " + (hasGraph ? "has graph" : "no graph in math RICH_TEXT"));
    }

    private ValidationResult build(GenerationOutput output, String ruleName,
                                    boolean passed, ValidationSeverity severity, String message) {
        ValidationResult vr = new ValidationResult();
        vr.setOutput(output);
        vr.setRuleName(ruleName);
        vr.setPassed(passed);
        vr.setSeverity(severity);
        vr.setMessage(message);
        return vr;
    }

    private String quizSummary(JsonNode json) {
        JsonNode blocks = json.path("blocks");
        int count = 0;
        for (JsonNode b : blocks) {
            if ("quiz".equals(b.path("type").asText(""))) count++;
        }
        return count + " quiz questions, all valid";
    }

    private String checkpointSummary(JsonNode json) {
        JsonNode blocks = json.path("blocks");
        int cpCount = 0;
        int textCount = 0;
        for (JsonNode b : blocks) {
            String type = b.path("type").asText("");
            if ("checkpoint".equals(type)) cpCount++;
            else if ("text".equals(type)) textCount++;
        }
        return cpCount + " checkpoints + " + textCount + " text blocks, all valid";
    }
}
