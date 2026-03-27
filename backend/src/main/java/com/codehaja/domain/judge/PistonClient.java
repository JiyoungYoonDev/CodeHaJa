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

    public ExecutionResult execute(String sourceCode, String language) {
        String lang = language == null ? "" : language.toLowerCase();
        String pistonLang = LANGUAGE_MAP.getOrDefault(lang, "python");
        String filename = FILENAME_MAP.getOrDefault(lang, "main.py");

        try {
            // objectMapper.writeValueAsString handles all escaping for source code
            String encodedSource = objectMapper.writeValueAsString(sourceCode);
            String json = "{\"language\":\"" + pistonLang + "\",\"version\":\"*\","
                + "\"files\":[{\"name\":\"" + filename + "\",\"content\":" + encodedSource + "}]}";

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
