package com.codehaja.domain.coding.service;

import com.codehaja.common.exception.BusinessException;
import com.codehaja.common.exception.ErrorCode;
import com.codehaja.domain.judge.PistonClient;
import com.codehaja.domain.lectureitem.entity.LectureItem;
import com.codehaja.domain.lectureitem.entity.LectureItemType;
import com.codehaja.domain.lectureitem.repository.LectureItemRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Recalibrates test cases for CODING_SET items by actually executing
 * the starter code through Piston with each test input, capturing the
 * real stdout as expectedOutput. This guarantees test cases match the code.
 */
@Service
@RequiredArgsConstructor
public class TestCaseRecalibrationService {

    private static final Logger log = LoggerFactory.getLogger(TestCaseRecalibrationService.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final LectureItemRepository lectureItemRepository;
    private final PistonClient pistonClient;

    public record RecalibrationResult(
            Long itemId,
            int totalTestCases,
            int updated,
            int failed,
            List<String> details
    ) {}

    /**
     * Recalibrate test cases for a single CODING_SET item.
     * Runs the starter code with each test input, replaces expectedOutput with real stdout.
     */
    @Transactional
    public RecalibrationResult recalibrate(Long itemId) {
        LectureItem item = lectureItemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LECTURE_ITEM_NOT_FOUND));

        if (item.getItemType() != LectureItemType.CODING_SET) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Item is not a CODING_SET");
        }

        JsonNode contentJson = item.getContentJson();
        if (contentJson == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Item has no contentJson");
        }

        // Extract starter code and language
        String starterCode = extractStarterCode(contentJson);
        String language = extractLanguage(contentJson);

        if (starterCode == null || starterCode.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "No starter code found");
        }

        // Auto-fix common AI code generation bugs
        String fixedCode = fixBrokenCode(starterCode);
        boolean codeFixed = !fixedCode.equals(starterCode);
        starterCode = fixedCode;

        // Find test cases and recalibrate
        List<String> details = new ArrayList<>();
        if (codeFixed) {
            details.add("CODE_FIXED: repaired broken string literals (newlines inside quotes)");
        }
        int updated = 0;
        int failed = 0;

        ObjectNode rootNode = (ObjectNode) mapper.valueToTree(contentJson);
        JsonNode problems = rootNode.get("problems");

        if (problems != null && problems.isArray() && !problems.isEmpty()) {
            ObjectNode problem = (ObjectNode) problems.get(0);
            JsonNode testCasesNode = problem.get("testCases");

            if (testCasesNode != null && testCasesNode.isArray()) {
                ArrayNode newTestCases = mapper.createArrayNode();

                for (int i = 0; i < testCasesNode.size(); i++) {
                    JsonNode tc = testCasesNode.get(i);
                    String input = tc.has("input") ? tc.get("input").asText("") : "";
                    String oldExpected = tc.has("expectedOutput") ? tc.get("expectedOutput").asText("") : "";

                    try {
                        PistonClient.ExecutionResult result = input.isEmpty()
                                ? pistonClient.execute(starterCode, language)
                                : pistonClient.execute(starterCode, language, input);

                        if (result.isCompileError() || result.isRuntimeError()) {
                            String error = result.getStderr();
                            details.add("Test " + (i + 1) + ": ERROR — " + truncate(error, 100));
                            failed++;
                            // Keep old test case
                            newTestCases.add(tc);
                        } else {
                            String realOutput = result.getStdout().stripTrailing();
                            ObjectNode newTc = mapper.createObjectNode();
                            newTc.put("input", input);
                            newTc.put("expectedOutput", realOutput);
                            newTestCases.add(newTc);

                            if (!realOutput.equals(oldExpected.stripTrailing())) {
                                details.add("Test " + (i + 1) + ": UPDATED"
                                        + " | old=" + truncate(oldExpected, 60)
                                        + " | new=" + truncate(realOutput, 60));
                                updated++;
                            } else {
                                details.add("Test " + (i + 1) + ": OK (unchanged)");
                            }
                        }
                    } catch (Exception e) {
                        details.add("Test " + (i + 1) + ": EXEC_ERROR — " + e.getMessage());
                        failed++;
                        newTestCases.add(tc);
                    }
                }

                problem.set("testCases", newTestCases);
            }

            // Persist fixed starter code back into contentJson
            if (codeFixed) {
                JsonNode files = problem.get("files");
                if (files != null && files.isArray() && !files.isEmpty()) {
                    ((ObjectNode) files.get(0)).put("content", starterCode);
                }
                if (problem.has("starterCode")) {
                    problem.put("starterCode", starterCode);
                }
            }

            // Also update legacy expectedOutput at problem level if present
            if (problem.has("expectedOutput") && !problem.get("expectedOutput").isNull()) {
                try {
                    PistonClient.ExecutionResult result = pistonClient.execute(starterCode, language);
                    if (!result.isCompileError() && !result.isRuntimeError()) {
                        problem.put("expectedOutput", result.getStdout().stripTrailing());
                    }
                } catch (Exception ignored) {}
            }
        }

        // Also handle legacy top-level expectedOutput
        if (rootNode.has("expectedOutput") && !rootNode.get("expectedOutput").isNull()) {
            try {
                PistonClient.ExecutionResult result = pistonClient.execute(starterCode, language);
                if (!result.isCompileError() && !result.isRuntimeError()) {
                    rootNode.put("expectedOutput", result.getStdout().stripTrailing());
                    details.add("Legacy expectedOutput: UPDATED");
                    updated++;
                }
            } catch (Exception ignored) {}
        }

        // Save back
        item.setContentJson(rootNode);
        lectureItemRepository.save(item);

        int total = details.size();
        log.info("Recalibrated item {}: {}/{} updated, {} failed", itemId, updated, total, failed);

        return new RecalibrationResult(itemId, total, updated, failed, details);
    }

    /**
     * Recalibrate all CODING_SET items in a lecture.
     */
    @Transactional
    public List<RecalibrationResult> recalibrateLecture(Long lectureId) {
        List<LectureItem> items = lectureItemRepository.findAllByLectureIdOrderBySortOrderAsc(lectureId);
        List<RecalibrationResult> results = new ArrayList<>();

        for (LectureItem item : items) {
            if (item.getItemType() != LectureItemType.CODING_SET) continue;
            if (item.getContentJson() == null) continue;
            try {
                results.add(recalibrate(item.getId()));
            } catch (Exception e) {
                results.add(new RecalibrationResult(
                        item.getId(), 0, 0, 1,
                        List.of("ERROR: " + e.getMessage())
                ));
            }
        }
        return results;
    }

    /**
     * Recalibrate all CODING_SET items in an entire course.
     */
    @Transactional
    public List<RecalibrationResult> recalibrateCourse(Long courseId) {
        List<LectureItem> items = lectureItemRepository
                .findByCourseIdAndItemType(courseId, LectureItemType.CODING_SET);

        List<RecalibrationResult> results = new ArrayList<>();
        for (LectureItem item : items) {
            if (item.getContentJson() == null) continue;
            try {
                results.add(recalibrate(item.getId()));
            } catch (Exception e) {
                results.add(new RecalibrationResult(
                        item.getId(), 0, 0, 1,
                        List.of("ERROR: " + e.getMessage())
                ));
            }
        }
        return results;
    }

    // ── Helpers ──

    private String extractStarterCode(JsonNode contentJson) {
        JsonNode problems = contentJson.get("problems");
        if (problems != null && problems.isArray() && !problems.isEmpty()) {
            JsonNode problem = problems.get(0);
            // Check files array first
            JsonNode files = problem.get("files");
            if (files != null && files.isArray() && !files.isEmpty()) {
                JsonNode content = files.get(0).get("content");
                if (content != null) return content.asText("");
            }
            // Fallback: starterCode field
            JsonNode starter = problem.get("starterCode");
            if (starter != null) return starter.asText("");
        }
        // Legacy: top-level starterCode
        JsonNode starter = contentJson.get("starterCode");
        return starter != null ? starter.asText("") : null;
    }

    private String extractLanguage(JsonNode contentJson) {
        JsonNode problems = contentJson.get("problems");
        if (problems != null && problems.isArray() && !problems.isEmpty()) {
            JsonNode lang = problems.get(0).get("language");
            if (lang != null) return lang.asText("python");
        }
        JsonNode lang = contentJson.get("language");
        return lang != null ? lang.asText("python") : "python";
    }

    /**
     * Fix common AI code generation bugs:
     * 1. Real newlines inside single-quoted string literals (f"...\n..." → f"...\n...")
     *    The AI outputs \n as an actual newline instead of the escape sequence.
     */
    private String fixBrokenCode(String code) {
        if (code == null) return code;

        String[] lines = code.split("\n", -1);
        StringBuilder fixed = new StringBuilder();
        boolean insideBrokenString = false;
        String pendingLine = null;

        for (String line : lines) {
            if (insideBrokenString) {
                // This line is a continuation of a broken string — collapse with \n
                pendingLine += "\\n" + line;
                // Check if this line closes the string (has a matching quote + closing paren/end)
                if (lineClosesString(pendingLine)) {
                    insideBrokenString = false;
                    if (!fixed.isEmpty()) fixed.append("\n");
                    fixed.append(pendingLine);
                    pendingLine = null;
                }
            } else {
                if (pendingLine != null) {
                    if (!fixed.isEmpty()) fixed.append("\n");
                    fixed.append(pendingLine);
                }
                pendingLine = line;
                // Check if this line opens a string that doesn't close on the same line
                // Pattern: line has f" or (" but no matching closing quote
                if (hasUnclosedString(line)) {
                    insideBrokenString = true;
                }
            }
        }
        if (pendingLine != null) {
            if (!fixed.isEmpty()) fixed.append("\n");
            fixed.append(pendingLine);
        }

        return fixed.toString();
    }

    private boolean hasUnclosedString(String line) {
        String trimmed = line.stripTrailing();
        // Count unescaped double quotes
        int quoteCount = 0;
        for (int i = 0; i < trimmed.length(); i++) {
            if (trimmed.charAt(i) == '"' && (i == 0 || trimmed.charAt(i - 1) != '\\')) {
                quoteCount++;
            }
        }
        // Odd number of quotes = unclosed string
        // Also verify the line looks like it has a string opening (not a comment or triple-quote)
        if (quoteCount % 2 != 0 && !trimmed.contains("\"\"\"")) {
            // Make sure the last quote is an opening quote (line ends with just the quote or f")
            return trimmed.endsWith("\"") || trimmed.endsWith("f\"");
        }
        return false;
    }

    private boolean lineClosesString(String line) {
        int quoteCount = 0;
        for (int i = 0; i < line.length(); i++) {
            if (line.charAt(i) == '"' && (i == 0 || line.charAt(i - 1) != '\\')) {
                quoteCount++;
            }
        }
        return quoteCount % 2 == 0;
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        String clean = s.replace("\n", "\\n").replace("\r", "");
        return clean.length() > max ? clean.substring(0, max) + "..." : clean;
    }
}
