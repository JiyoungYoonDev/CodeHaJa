package com.codehaja.domain.judge;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import static java.net.http.HttpClient.Version.HTTP_1_1;

@Component
public class PistonClient {

    private final String baseUrl;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HTTP_1_1)
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Map<String, String> LANGUAGE_MAP = Map.of(
        "python",     "python",
        "javascript", "javascript",
        "java",       "java",
        "cpp",        "c++",
        "c",          "c"
    );

    private static final Map<String, String> FILENAME_MAP = Map.of(
        "python",     "main.py",
        "javascript", "main.js",
        "java",       "Main.java",
        "cpp",        "main.cpp",
        "c",          "main.c"
    );

    public PistonClient(@Value("${piston.base-url:http://localhost:2000}") String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public record SourceFile(String name, String content) {}

    public ExecutionResult execute(String sourceCode, String language) {
        return execute(sourceCode, language, null);
    }

    /**
     * Execute multiple files (e.g. Java: Solution.java + Main.java test runner).
     * The first file in the list is treated as the main entry point by Piston.
     */
    public ExecutionResult execute(List<SourceFile> files, String language) {
        String lang = language == null ? "" : language.toLowerCase();
        String pistonLang = LANGUAGE_MAP.getOrDefault(lang, "python");

        try {
            StringBuilder filesJson = new StringBuilder("[");
            for (int i = 0; i < files.size(); i++) {
                if (i > 0) filesJson.append(",");
                filesJson.append("{\"name\":")
                        .append(objectMapper.writeValueAsString(files.get(i).name()))
                        .append(",\"content\":")
                        .append(objectMapper.writeValueAsString(files.get(i).content()))
                        .append("}");
            }
            filesJson.append("]");

            String json = "{\"language\":\"" + pistonLang + "\",\"version\":\"*\","
                + "\"files\":" + filesJson + "}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/v2/execute"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                throw new RuntimeException("Piston error " + response.statusCode()
                    + " body=" + response.body()
                    + " sent=" + json);
            }

            return objectMapper.readValue(response.body(), ExecutionResult.class);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to call Piston: " + e.getMessage(), e);
        }
    }

    public ExecutionResult execute(String sourceCode, String language, String stdin) {
        String lang = language == null ? "" : language.toLowerCase();
        String pistonLang = LANGUAGE_MAP.getOrDefault(lang, "python");
        String filename = FILENAME_MAP.getOrDefault(lang, "main.py");

        // Java: extract public class name for correct filename
        if ("java".equals(lang) && sourceCode != null) {
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("public\\s+class\\s+(\\w+)")
                    .matcher(sourceCode);
            if (m.find()) {
                filename = m.group(1) + ".java";
            }
        }

        try {
            // objectMapper.writeValueAsString handles all escaping for source code
            String encodedSource = objectMapper.writeValueAsString(sourceCode);
            String stdinPart = "";
            if (stdin != null && !stdin.isEmpty()) {
                stdinPart = ",\"stdin\":" + objectMapper.writeValueAsString(stdin);
            }
            String json = "{\"language\":\"" + pistonLang + "\",\"version\":\"*\","
                + "\"files\":[{\"name\":\"" + filename + "\",\"content\":" + encodedSource + "}]"
                + stdinPart + "}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/v2/execute"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                throw new RuntimeException("Piston error " + response.statusCode()
                    + " body=" + response.body()
                    + " sent=" + json);
            }

            return objectMapper.readValue(response.body(), ExecutionResult.class);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to call Piston: " + e.getMessage(), e);
        }
    }

    @Getter @Setter
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public static class ExecutionResult {
        private String language;
        private String version;
        private RunResult run;
        private RunResult compile;

        @Getter @Setter
        @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
        public static class RunResult {
            private String stdout;
            private String stderr;
            private String output;
            private Integer code;
            private String signal;
        }

        public String getStdout() {
            return run != null && run.getStdout() != null ? run.getStdout() : "";
        }

        public String getStderr() {
            if (compile != null && compile.getStderr() != null && !compile.getStderr().isBlank()) {
                return compile.getStderr();
            }
            return run != null && run.getStderr() != null ? run.getStderr() : "";
        }

        public boolean isCompileError() {
            return compile != null && compile.getCode() != null && compile.getCode() != 0;
        }

        public boolean isRuntimeError() {
            return run != null && run.getCode() != null && run.getCode() != 0 && !isCompileError();
        }

        public boolean isAccepted(String expectedOutput) {
            if (isCompileError() || isRuntimeError()) return false;
            if (expectedOutput == null || expectedOutput.isBlank()) return false;
            String actual = getStdout().stripTrailing();
            String expected = expectedOutput.stripTrailing();
            return actual.equals(expected);
        }
    }
}
