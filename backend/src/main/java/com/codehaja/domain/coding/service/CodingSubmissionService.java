package com.codehaja.domain.coding.service;

import com.codehaja.auth.entity.User;
import com.codehaja.auth.repository.UserRepository;
import com.codehaja.common.exception.BusinessException;
import com.codehaja.common.exception.ErrorCode;
import com.codehaja.domain.coding.dto.CodingSubmissionDto;
import com.codehaja.domain.coding.entity.CodingSubmission;
import com.codehaja.domain.coding.entity.SubmissionStatus;
import com.codehaja.domain.coding.repository.CodingSubmissionRepository;
import com.codehaja.domain.gamification.service.HeartService;
import com.codehaja.domain.gamification.service.XpService;
import com.codehaja.domain.judge.PistonClient;
import com.codehaja.domain.lectureitem.entity.LectureItem;
import com.codehaja.domain.lectureitem.repository.LectureItemRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CodingSubmissionService {

    private static final Logger log = LoggerFactory.getLogger(CodingSubmissionService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final UserRepository userRepository;
    private final LectureItemRepository lectureItemRepository;
    private final CodingSubmissionRepository codingSubmissionRepository;
    private final PistonClient pistonClient;
    private final XpService xpService;
    private final HeartService heartService;

    @Transactional
    public CodingSubmissionDto.Response submit(Long lectureItemId, CodingSubmissionDto.SubmitRequest request, String userEmail) {
        if (request == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Request body is required.");
        }
        if (request.getSourceCode() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "sourceCode is required.");
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_UNAUTHORIZED));

        if (request.isGrade()) {
            heartService.requireHeart(user);
        }

        LectureItem lectureItem = lectureItemRepository.findById(lectureItemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LECTURE_ITEM_NOT_FOUND));

        boolean noPreviousPass = request.isGrade() &&
                !codingSubmissionRepository.existsByUserIdAndLectureItemIdAndSubmissionStatus(
                        user.getId(), lectureItemId, SubmissionStatus.PASSED);

        CodingSubmission submission = new CodingSubmission();
        submission.setUser(user);
        submission.setLectureItem(lectureItem);
        submission.setSourceCode(request.getSourceCode());
        submission.setLanguage(request.getLanguage());
        submission.setSubmissionStatus(SubmissionStatus.RUNNING);
        submission.setPassedCount(0);
        submission.setTotalCount(0);
        submission.setExecutionTimeMs(0L);
        submission.setStdout("");
        submission.setStderr("");
        submission.setResultJson(null);

        CodingSubmission saved = codingSubmissionRepository.save(submission);

        int xpGained = 0;
        List<CodingSubmissionDto.TestCaseResult> testCaseResults = new ArrayList<>();

        try {
            if (!request.isGrade()) {
                // ── Run mode: just execute with optional stdin ──
                String stdin = request.getStdin();
                PistonClient.ExecutionResult result = (stdin != null && !stdin.isEmpty())
                        ? pistonClient.execute(request.getSourceCode(), request.getLanguage(), stdin)
                        : pistonClient.execute(request.getSourceCode(), request.getLanguage());
                saved.setStdout(result.getStdout() != null ? result.getStdout() : "");
                saved.setStderr(result.getStderr() != null ? result.getStderr() : "");
                saved.setTotalCount(1);
                if (result.isCompileError() || result.isRuntimeError()) {
                    saved.setSubmissionStatus(SubmissionStatus.ERROR);
                } else {
                    saved.setSubmissionStatus(SubmissionStatus.PASSED);
                    saved.setPassedCount(1);
                }
            } else {
                // ── Grade mode ──
                FunctionTestInfo fnTest = extractFunctionTestInfo(lectureItem);

                if (fnTest != null) {
                    // ── Function-based grading (LeetCode style) ──
                    testCaseResults = executeFunctionTests(request.getSourceCode(), request.getLanguage(), fnTest);
                } else {
                    // ── Legacy stdin/stdout grading ──
                    List<TestCase> testCases = extractTestCases(lectureItem);
                    testCaseResults = executeStdinTests(request.getSourceCode(), request.getLanguage(), testCases);
                }

                int passed = (int) testCaseResults.stream().filter(CodingSubmissionDto.TestCaseResult::isPassed).count();
                int total = testCaseResults.size();
                saved.setPassedCount(passed);
                saved.setTotalCount(total);

                if (passed == total && total > 0) {
                    saved.setSubmissionStatus(SubmissionStatus.PASSED);
                    if (noPreviousPass) xpGained = xpService.award(user, XpService.XP_CODING_PASS);
                } else {
                    saved.setSubmissionStatus(SubmissionStatus.FAILED);
                    heartService.deductHeart(user);
                }

                try {
                    saved.setResultJson(objectMapper.readTree(objectMapper.writeValueAsString(testCaseResults)));
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            saved.setSubmissionStatus(SubmissionStatus.ERROR);
            saved.setStderr("Execution failed: " + e.getMessage());
            if (request.isGrade()) heartService.deductHeart(user);
        }

        CodingSubmissionDto.Response response = toResponse(saved, xpGained, user);
        if (!testCaseResults.isEmpty()) {
            response.setTestCaseResults(testCaseResults);
        }
        return response;
    }

    // ══════════════════════════════════════════════════════════════════════
    // Function-based testing (LeetCode style)
    // ══════════════════════════════════════════════════════════════════════

    private record FunctionTestInfo(String functionName, String testCasesJson) {}

    /**
     * Extract function test info from contentJson.
     * Returns null if the item doesn't use function-based testing.
     */
    private FunctionTestInfo extractFunctionTestInfo(LectureItem item) {
        JsonNode content = item.getContentJson();
        if (content == null) return null;

        JsonNode problems = content.get("problems");
        if (problems == null || !problems.isArray() || problems.isEmpty()) return null;

        JsonNode problem = problems.get(0);
        JsonNode fnNameNode = problem.get("functionName");
        if (fnNameNode == null || fnNameNode.asText("").isBlank()) return null;

        JsonNode testCasesNode = problem.get("testCases");
        if (testCasesNode == null || !testCasesNode.isArray() || testCasesNode.isEmpty()) return null;

        // Check if test cases have "args" field (function-based) vs "input" field (stdin-based)
        JsonNode firstTc = testCasesNode.get(0);
        if (!firstTc.has("args")) return null;

        try {
            return new FunctionTestInfo(
                    fnNameNode.asText(),
                    objectMapper.writeValueAsString(testCasesNode)
            );
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Execute function-based tests. Language-aware:
     * - Java: multi-file (Solution.java + Main.java test runner)
     * - Python/JS: single-file (append test runner after student code)
     */
    private List<CodingSubmissionDto.TestCaseResult> executeFunctionTests(
            String sourceCode, String language, FunctionTestInfo fnTest) {

        String lang = language == null ? "" : language.toLowerCase();
        PistonClient.ExecutionResult result;

        if ("java".equals(lang)) {
            // Java: extract class name, send two files
            String className = "Solution";
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("public\\s+class\\s+(\\w+)").matcher(sourceCode);
            if (m.find()) className = m.group(1);

            String mainJava = buildJavaTestRunner(className, fnTest.functionName, fnTest.testCasesJson);
            result = pistonClient.execute(java.util.List.of(
                    new PistonClient.SourceFile("Main.java", mainJava),
                    new PistonClient.SourceFile(className + ".java", sourceCode)
            ), language);
        } else if ("javascript".equals(lang) || "js".equals(lang)) {
            String testRunner = sourceCode + "\n\n" + buildJsTestRunner(fnTest.functionName, fnTest.testCasesJson);
            result = pistonClient.execute(testRunner, language);
        } else {
            // Python (default)
            String testRunner = sourceCode + "\n\n" + buildPythonTestRunner(fnTest.functionName, fnTest.testCasesJson);
            result = pistonClient.execute(testRunner, language);
        }

        return parseTestRunnerResult(result);
    }

    private List<CodingSubmissionDto.TestCaseResult> parseTestRunnerResult(PistonClient.ExecutionResult result) {
        List<CodingSubmissionDto.TestCaseResult> results = new ArrayList<>();

        if (result.isCompileError() || result.isRuntimeError()) {
            CodingSubmissionDto.TestCaseResult tcr = new CodingSubmissionDto.TestCaseResult();
            tcr.setIndex(1);
            tcr.setPassed(false);
            tcr.setStatus("ERROR");
            tcr.setActualOutput(result.getStderr());
            results.add(tcr);
            return results;
        }

        String stdout = result.getStdout().strip();
        try {
            JsonNode parsed = objectMapper.readTree(stdout);
            if (parsed.isArray()) {
                for (JsonNode r : parsed) {
                    CodingSubmissionDto.TestCaseResult tcr = new CodingSubmissionDto.TestCaseResult();
                    tcr.setIndex(r.get("index").asInt());
                    tcr.setPassed(r.get("passed").asBoolean());
                    tcr.setInput(r.has("input") ? r.get("input").asText() : "");
                    tcr.setExpectedOutput(r.has("expected") ? r.get("expected").asText() : "");
                    tcr.setActualOutput(r.has("got") ? r.get("got").asText() : "");
                    tcr.setStatus(tcr.isPassed() ? "PASSED" : "FAILED");
                    results.add(tcr);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse test runner output: {}", stdout);
            CodingSubmissionDto.TestCaseResult tcr = new CodingSubmissionDto.TestCaseResult();
            tcr.setIndex(1);
            tcr.setPassed(false);
            tcr.setStatus("ERROR");
            tcr.setActualOutput("Test runner parse error: " + stdout);
            results.add(tcr);
        }

        return results;
    }

    // ── Python test runner ──

    private String buildPythonTestRunner(String functionName, String testCasesJson) {
        return """
                # ═══ CodeHaja Test Runner ═══
                import json as _json
                _test_cases = _json.loads('''%s''')
                _fn = %s
                _results = []
                for _i, _tc in enumerate(_test_cases):
                    try:
                        _actual = _fn(*_tc["args"])
                        _expected = _tc["expected"]
                        _passed = _actual == _expected
                        _results.append({
                            "index": _i + 1,
                            "passed": _passed,
                            "input": _json.dumps(_tc["args"]),
                            "expected": _json.dumps(_expected),
                            "got": _json.dumps(_actual)
                        })
                    except Exception as _e:
                        _results.append({
                            "index": _i + 1,
                            "passed": False,
                            "input": _json.dumps(_tc.get("args", [])),
                            "expected": _json.dumps(_tc.get("expected", "")),
                            "got": f"ERROR: {_e}"
                        })
                print(_json.dumps(_results))
                """.formatted(testCasesJson.replace("\\", "\\\\").replace("'", "\\'"), functionName);
    }

    // ── JavaScript test runner ──

    private String buildJsTestRunner(String functionName, String testCasesJson) {
        return """
                // ═══ CodeHaja Test Runner ═══
                const _testCases = %s;
                const _results = [];
                for (let _i = 0; _i < _testCases.length; _i++) {
                    const _tc = _testCases[_i];
                    try {
                        const _actual = %s(..._tc.args);
                        const _expected = _tc.expected;
                        const _passed = JSON.stringify(_actual) === JSON.stringify(_expected);
                        _results.push({
                            index: _i + 1,
                            passed: _passed,
                            input: JSON.stringify(_tc.args),
                            expected: JSON.stringify(_expected),
                            got: JSON.stringify(_actual)
                        });
                    } catch (_e) {
                        _results.push({
                            index: _i + 1,
                            passed: false,
                            input: JSON.stringify(_tc.args || []),
                            expected: JSON.stringify(_tc.expected || ""),
                            got: "ERROR: " + _e.message
                        });
                    }
                }
                console.log(JSON.stringify(_results));
                """.formatted(testCasesJson, functionName);
    }

    // ── Java test runner ──

    private String buildJavaTestRunner(String className, String functionName, String testCasesJson) {
        try {
            JsonNode testCases = objectMapper.readTree(testCasesJson);
            StringBuilder sb = new StringBuilder();
            sb.append("import java.util.*;\n\n");
            sb.append("public class Main {\n");
            sb.append("    public static void main(String[] a) {\n");
            sb.append("        ").append(className).append(" sol = new ").append(className).append("();\n");
            sb.append("        StringBuilder j = new StringBuilder(\"[\");\n");

            for (int i = 0; i < testCases.size(); i++) {
                JsonNode tc = testCases.get(i);
                JsonNode argsNode = tc.get("args");
                JsonNode expectedNode = tc.get("expected");

                if (i > 0) sb.append("        j.append(\",\");\n");
                sb.append("        try {\n");

                // Build arg list
                StringBuilder argsList = new StringBuilder();
                for (int k = 0; k < argsNode.size(); k++) {
                    if (k > 0) argsList.append(", ");
                    argsList.append(jsonToJavaLiteral(argsNode.get(k)));
                }

                sb.append("            Object r = sol.").append(functionName)
                  .append("(").append(argsList).append(");\n");
                sb.append("            Object e = ").append(jsonToJavaLiteral(expectedNode)).append(";\n");
                sb.append("            boolean p = eq(r, e);\n");
                sb.append("            j.append(\"{\\\"index\\\":").append(i + 1)
                  .append(",\\\"passed\\\":\").append(p)")
                  .append(".append(\",\\\"expected\\\":\\\"\").append(s(e).replace(\"\\\"\",\"'\"))")
                  .append(".append(\"\\\",\\\"got\\\":\\\"\").append(s(r).replace(\"\\\"\",\"'\"))")
                  .append(".append(\"\\\"}\");\n");
                sb.append("        } catch (Exception ex) {\n");
                sb.append("            j.append(\"{\\\"index\\\":").append(i + 1)
                  .append(",\\\"passed\\\":false,\\\"expected\\\":\\\"\\\",\\\"got\\\":\\\"ERROR: \")")
                  .append(".append(ex.getMessage()!=null?ex.getMessage().replace(\"\\\"\",\"'\"):\"null\")")
                  .append(".append(\"\\\"}\");\n");
                sb.append("        }\n");
            }

            sb.append("        j.append(\"]\");\n");
            sb.append("        System.out.println(j);\n");
            sb.append("    }\n\n");

            // Helper: deep equals
            sb.append("    static boolean eq(Object a, Object b) {\n");
            sb.append("        if (a instanceof int[] && b instanceof int[]) return Arrays.equals((int[])a,(int[])b);\n");
            sb.append("        if (a instanceof long[] && b instanceof long[]) return Arrays.equals((long[])a,(long[])b);\n");
            sb.append("        if (a instanceof double[] && b instanceof double[]) return Arrays.equals((double[])a,(double[])b);\n");
            sb.append("        if (a instanceof String[] && b instanceof String[]) return Arrays.equals((String[])a,(String[])b);\n");
            sb.append("        if (a instanceof Object[] && b instanceof Object[]) return Arrays.deepEquals((Object[])a,(Object[])b);\n");
            sb.append("        return Objects.equals(a, b);\n");
            sb.append("    }\n\n");

            // Helper: stringify
            sb.append("    static String s(Object o) {\n");
            sb.append("        if (o instanceof int[]) return Arrays.toString((int[])o);\n");
            sb.append("        if (o instanceof long[]) return Arrays.toString((long[])o);\n");
            sb.append("        if (o instanceof double[]) return Arrays.toString((double[])o);\n");
            sb.append("        if (o instanceof Object[]) return Arrays.deepToString((Object[])o);\n");
            sb.append("        return String.valueOf(o);\n");
            sb.append("    }\n");
            sb.append("}\n");

            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to build Java test runner: " + e.getMessage(), e);
        }
    }

    /**
     * Convert a JSON value to a Java literal for use in generated test runner code.
     * Handles: int, long, double, boolean, String, int[], String[], int[][], null.
     */
    private static String jsonToJavaLiteral(JsonNode node) {
        if (node == null || node.isNull()) return "null";
        if (node.isBoolean()) return String.valueOf(node.booleanValue());
        if (node.isInt()) return String.valueOf(node.intValue());
        if (node.isLong()) return node.longValue() + "L";
        if (node.isDouble() || node.isFloat()) return node.doubleValue() + "d";
        if (node.isTextual()) return "\"" + node.textValue()
                .replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r") + "\"";
        if (node.isArray()) {
            if (node.isEmpty()) return "new int[]{}";
            JsonNode first = node.get(0);
            if (first.isArray()) {
                // 2D array (e.g. int[][])
                StringBuilder sb = new StringBuilder("new int[][]{");
                for (int i = 0; i < node.size(); i++) {
                    if (i > 0) sb.append(",");
                    sb.append(jsonToJavaLiteral(node.get(i)));
                }
                return sb.append("}").toString();
            }
            if (first.isTextual()) {
                StringBuilder sb = new StringBuilder("new String[]{");
                for (int i = 0; i < node.size(); i++) {
                    if (i > 0) sb.append(",");
                    sb.append(jsonToJavaLiteral(node.get(i)));
                }
                return sb.append("}").toString();
            }
            // Default: int array
            StringBuilder sb = new StringBuilder("new int[]{");
            for (int i = 0; i < node.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(node.get(i).asInt());
            }
            return sb.append("}").toString();
        }
        return node.toString();
    }

    // ══════════════════════════════════════════════════════════════════════
    // Legacy stdin/stdout testing
    // ══════════════════════════════════════════════════════════════════════

    private List<CodingSubmissionDto.TestCaseResult> executeStdinTests(
            String sourceCode, String language, List<TestCase> testCases) {

        List<CodingSubmissionDto.TestCaseResult> results = new ArrayList<>();

        for (int i = 0; i < testCases.size(); i++) {
            TestCase tc = testCases.get(i);
            CodingSubmissionDto.TestCaseResult tcr = new CodingSubmissionDto.TestCaseResult();
            tcr.setIndex(i + 1);
            tcr.setInput(tc.input);
            tcr.setExpectedOutput(tc.expectedOutput);

            try {
                PistonClient.ExecutionResult result = pistonClient.execute(sourceCode, language, tc.input);
                String stdout = result.getStdout() != null ? result.getStdout() : "";
                tcr.setActualOutput(stdout.stripTrailing());

                if (result.isCompileError() || result.isRuntimeError()) {
                    tcr.setPassed(false);
                    tcr.setStatus("ERROR");
                } else if (stdout.stripTrailing().equals(tc.expectedOutput.stripTrailing())) {
                    tcr.setPassed(true);
                    tcr.setStatus("PASSED");
                } else {
                    tcr.setPassed(false);
                    tcr.setStatus("FAILED");
                }
            } catch (Exception e) {
                tcr.setPassed(false);
                tcr.setStatus("ERROR");
                tcr.setActualOutput(e.getMessage());
            }
            results.add(tcr);
        }

        return results;
    }

    // ── Test case extraction (legacy stdin/stdout) ──

    private record TestCase(String input, String expectedOutput) {}

    private List<TestCase> extractTestCases(LectureItem lectureItem) {
        List<TestCase> cases = new ArrayList<>();
        if (lectureItem.getContentJson() == null) return cases;

        JsonNode content = lectureItem.getContentJson();

        JsonNode problems = content.get("problems");
        if (problems != null && problems.isArray() && !problems.isEmpty()) {
            JsonNode testCasesNode = problems.get(0).get("testCases");
            if (testCasesNode != null && testCasesNode.isArray()) {
                for (JsonNode tc : testCasesNode) {
                    String input = tc.has("input") ? tc.get("input").asText("") : "";
                    String expected = tc.has("expectedOutput") ? tc.get("expectedOutput").asText("") : "";
                    if (!expected.isEmpty()) {
                        cases.add(new TestCase(input, expected));
                    }
                }
            }
            if (cases.isEmpty()) {
                JsonNode expected = problems.get(0).get("expectedOutput");
                if (expected != null && !expected.isNull() && !expected.asText("").isEmpty()) {
                    cases.add(new TestCase("", expected.asText()));
                }
            }
        }

        if (cases.isEmpty()) {
            JsonNode expected = content.get("expectedOutput");
            if (expected != null && !expected.isNull() && !expected.asText("").isEmpty()) {
                cases.add(new TestCase("", expected.asText()));
            }
        }

        return cases;
    }

    // ── Read-only endpoints ──

    public CodingSubmissionDto.Response getSubmission(Long submissionId) {
        CodingSubmission submission = codingSubmissionRepository.findById(submissionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CODING_SUBMISSION_NOT_FOUND));
        return toResponse(submission);
    }

    public CodingSubmissionDto.Response getLatestSubmission(Long lectureItemId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_UNAUTHORIZED));
        return codingSubmissionRepository
                .findAllByUserIdAndLectureItemIdOrderByCreatedAtDesc(user.getId(), lectureItemId)
                .stream().findFirst().map(this::toResponse).orElse(null);
    }

    public List<Long> getPassedItemIds(Long courseId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_UNAUTHORIZED));
        return codingSubmissionRepository.findDistinctLectureItemIdsByUserIdAndStatusAndCourseId(
                user.getId(), SubmissionStatus.PASSED, courseId);
    }

    public List<CodingSubmissionDto.Response> getSubmissions(Long lectureItemId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_UNAUTHORIZED));

        return codingSubmissionRepository
                .findAllByUserIdAndLectureItemIdOrderByCreatedAtDesc(user.getId(), lectureItemId)
                .stream().map(this::toResponse).toList();
    }

    private CodingSubmissionDto.Response toResponse(CodingSubmission submission) {
        return toResponse(submission, 0, null);
    }

    private CodingSubmissionDto.Response toResponse(CodingSubmission submission, int xpGained, User user) {
        CodingSubmissionDto.Response response = new CodingSubmissionDto.Response();
        response.setId(submission.getId());
        response.setLectureItemId(submission.getLectureItem().getId());
        response.setSourceCode(submission.getSourceCode());
        response.setLanguage(submission.getLanguage());
        response.setSubmissionStatus(submission.getSubmissionStatus().name());
        response.setPassedCount(submission.getPassedCount());
        response.setTotalCount(submission.getTotalCount());
        response.setExecutionTimeMs(submission.getExecutionTimeMs());
        response.setStdout(submission.getStdout());
        response.setStderr(submission.getStderr());
        response.setResultJson(submission.getResultJson() != null ? submission.getResultJson().toString() : null);
        response.setXpGained(xpGained);
        if (user != null) {
            response.setCurrentHearts(user.getHearts());
            response.setHeartsRefillAt(user.getHeartsRefillAt());
        }
        return response;
    }
}
