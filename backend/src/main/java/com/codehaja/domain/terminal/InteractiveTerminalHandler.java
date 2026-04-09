package com.codehaja.domain.terminal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class InteractiveTerminalHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(InteractiveTerminalHandler.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final long SESSION_TIMEOUT_SECONDS = 30;

    private static final Map<String, String> DOCKER_IMAGE_MAP = Map.of(
            "python", "python:3.11-slim",
            "javascript", "node:20-slim",
            "java", "eclipse-temurin:21-jdk",
            "c", "gcc:13",
            "cpp", "gcc:13"
    );

    private static final Map<String, String> RUN_CMD_MAP = Map.of(
            "python", "python -u /tmp/_code.py",
            "javascript", "node /tmp/_code.js",
            // Java command is built dynamically in handleStart() to match class name
            "java", "",
            "c", "sh -c 'gcc -o /tmp/a.out /tmp/_code.py && /tmp/a.out'",
            "cpp", "sh -c 'g++ -o /tmp/a.out /tmp/_code.py && /tmp/a.out'"
    );

    private static final java.util.regex.Pattern JAVA_CLASS_PATTERN =
            java.util.regex.Pattern.compile("public\\s+class\\s+(\\w+)");

    private final Map<String, SessionContext> sessions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("Terminal WebSocket connected: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonNode node = mapper.readTree(message.getPayload());
        String type = node.has("type") ? node.get("type").asText() : "";

        switch (type) {
            case "start" -> handleStart(session, node);
            case "stdin" -> handleStdin(session, node);
            case "kill" -> handleKill(session);
            default -> sendJson(session, "error", "Unknown message type: " + type);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("Terminal WebSocket closed: {} ({})", session.getId(), status);
        cleanupSession(session.getId());
    }

    private void handleStart(WebSocketSession session, JsonNode node) throws IOException {
        // Kill any existing process for this session
        cleanupSession(session.getId());

        String code = node.has("code") ? node.get("code").asText() : "";
        String language = node.has("language") ? node.get("language").asText("python") : "python";
        String lang = language.toLowerCase();

        String image = DOCKER_IMAGE_MAP.getOrDefault(lang, "python:3.11-slim");
        String runCmd = RUN_CMD_MAP.getOrDefault(lang, "python -u /tmp/_code.py");

        // Java: extract public class name for correct filename
        if ("java".equals(lang)) {
            String className = "Main";
            java.util.regex.Matcher m = JAVA_CLASS_PATTERN.matcher(code);
            if (m.find()) className = m.group(1);
            runCmd = "sh -c 'cp /tmp/_code.py /tmp/" + className + ".java && javac /tmp/" + className + ".java && java -cp /tmp " + className + "'";
        }

        // Base64 encode the source code to avoid shell escaping issues
        String encoded = Base64.getEncoder().encodeToString(code.getBytes(StandardCharsets.UTF_8));

        // Decode inside container, write to file, then exec the runner
        // `exec` replaces shell so stdin/stdout go directly to the program
        String shellCmd = "echo " + encoded + " | base64 -d > /tmp/_code.py && exec " + runCmd;

        String containerName = "terminal-" + session.getId().replace("-", "").substring(0, 12);

        try {
            log.info("Starting Docker container: {} | image={} | cmd={}", containerName, image, runCmd);
            ProcessBuilder pb = new ProcessBuilder(
                    "docker", "run", "-i", "--rm",
                    "--network", "none",
                    "--memory", "128m",
                    "--cpus", "0.5",
                    "--name", containerName,
                    image,
                    "sh", "-c", shellCmd
            );
            pb.redirectErrorStream(false);
            Process process = pb.start();

            SessionContext ctx = new SessionContext(process, containerName);
            sessions.put(session.getId(), ctx);

            // Schedule timeout kill
            ctx.timeoutFuture = scheduler.schedule(
                    () -> {
                        sendJsonSafe(session, "exit", "Timeout: process killed after " + SESSION_TIMEOUT_SECONDS + "s");
                        cleanupSession(session.getId());
                        closeSession(session);
                    },
                    SESSION_TIMEOUT_SECONDS, TimeUnit.SECONDS
            );

            // Stream stdout → WebSocket
            Thread.startVirtualThread(() -> streamOutput(session, process.getInputStream(), "stdout"));
            // Stream stderr → WebSocket
            Thread.startVirtualThread(() -> streamOutput(session, process.getErrorStream(), "stderr"));
            // Wait for process exit → send exit message
            Thread.startVirtualThread(() -> waitForExit(session, process));

        } catch (Exception e) {
            log.error("Failed to start Docker process", e);
            sendJson(session, "error", "Failed to start: " + e.getMessage());
        }
    }

    private void handleStdin(WebSocketSession session, JsonNode node) {
        SessionContext ctx = sessions.get(session.getId());
        if (ctx == null || !ctx.process.isAlive()) return;

        String data = node.has("data") ? node.get("data").asText() : "";
        try {
            OutputStream os = ctx.process.getOutputStream();
            os.write(data.getBytes(StandardCharsets.UTF_8));
            os.flush();
        } catch (IOException e) {
            log.debug("Failed to write stdin: {}", e.getMessage());
        }
    }

    private void handleKill(WebSocketSession session) {
        cleanupSession(session.getId());
        sendJsonSafe(session, "exit", "Process killed by user");
    }

    private void streamOutput(WebSocketSession session, InputStream is, String streamType) {
        try {
            byte[] buf = new byte[1024];
            int n;
            while ((n = is.read(buf)) != -1) {
                String text = new String(buf, 0, n, StandardCharsets.UTF_8);
                sendJsonSafe(session, streamType, text);
            }
        } catch (IOException e) {
            // Stream closed — process exited
        }
    }

    private void waitForExit(WebSocketSession session, Process process) {
        try {
            int exitCode = process.waitFor();
            sendJsonSafe(session, "exit", String.valueOf(exitCode));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            cleanupSession(session.getId());
        }
    }

    private void cleanupSession(String sessionId) {
        SessionContext ctx = sessions.remove(sessionId);
        if (ctx == null) return;

        if (ctx.timeoutFuture != null) ctx.timeoutFuture.cancel(false);

        if (ctx.process.isAlive()) {
            ctx.process.destroyForcibly();
        }

        // Force-remove the container (may already be gone due to --rm)
        Thread.startVirtualThread(() -> {
            try {
                new ProcessBuilder("docker", "rm", "-f", ctx.containerName)
                        .redirectErrorStream(true)
                        .start()
                        .waitFor(5, TimeUnit.SECONDS);
            } catch (Exception ignored) {}
        });
    }

    private void sendJson(WebSocketSession session, String type, String data) throws IOException {
        if (!session.isOpen()) return;
        String json = mapper.writeValueAsString(Map.of("type", type, "data", data));
        session.sendMessage(new TextMessage(json));
    }

    private void sendJsonSafe(WebSocketSession session, String type, String data) {
        try {
            sendJson(session, type, data);
        } catch (IOException e) {
            log.debug("Failed to send WS message: {}", e.getMessage());
        }
    }

    private void closeSession(WebSocketSession session) {
        try {
            if (session.isOpen()) session.close();
        } catch (IOException ignored) {}
    }

    private static class SessionContext {
        final Process process;
        final String containerName;
        java.util.concurrent.ScheduledFuture<?> timeoutFuture;

        SessionContext(Process process, String containerName) {
            this.process = process;
            this.containerName = containerName;
        }
    }
}
