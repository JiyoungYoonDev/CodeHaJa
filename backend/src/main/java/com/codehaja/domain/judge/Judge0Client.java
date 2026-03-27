package com.codehaja.domain.judge;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class Judge0Client {

    private final RestClient restClient;

    public Judge0Client(@Value("${judge0.base-url:http://localhost:2358}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public SubmissionResult submit(String sourceCode, String language, String expectedOutput) {
        int languageId = Judge0LanguageId.of(language);

        String escapedSource = sourceCode.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
        String json;
        if (expectedOutput != null && !expectedOutput.isBlank()) {
            String escapedExpected = expectedOutput.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
            json = "{\"source_code\":\"" + escapedSource + "\",\"language_id\":" + languageId + ",\"expected_output\":\"" + escapedExpected + "\"}";
        } else {
            json = "{\"source_code\":\"" + escapedSource + "\",\"language_id\":" + languageId + "}";
        }

        return restClient.post()
                .uri("/submissions?wait=true&base64_encoded=false")
                .contentType(MediaType.APPLICATION_JSON)
                .body(json)
                .retrieve()
                .onStatus(status -> status.isError(), (req, res) -> {
                    String errorBody = new String(res.getBody().readAllBytes());
                    throw new RuntimeException("Judge0 error " + res.getStatusCode() + ": " + errorBody);
                })
                .body(SubmissionResult.class);
    }

    @Getter @Setter
    public static class SubmissionResult {
        private String stdout;
        private String stderr;
        private String compile_output;
        private String time;
        private Integer memory;
        private Status status;

        @Getter @Setter
        public static class Status {
            private Integer id;
            private String description;
        }

        // Status IDs: 3=Accepted, 4=Wrong Answer, 5=Time Limit, 6=Compile Error, 11-14=Runtime Error
        public boolean isAccepted() {
            System.out.println("Judge0 status: " + (status != null ? status.getId() : "null"));
            return status != null && status.getId() != null && status.getId() == 3;
        }

        public String getErrorMessage() {
            if (compile_output != null && !compile_output.isBlank()) return compile_output;
            if (stderr != null && !stderr.isBlank()) return stderr;
            return null;
        }
    }
}
