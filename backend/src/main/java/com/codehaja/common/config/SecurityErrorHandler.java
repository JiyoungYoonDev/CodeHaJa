package com.codehaja.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
public class SecurityErrorHandler implements AccessDeniedHandler, AuthenticationEntryPoint {

    private static final Logger log = LoggerFactory.getLogger(SecurityErrorHandler.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException ex) throws IOException {
        log.warn("Access denied: {} {} — {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        writeJson(response, 403, "AUTH_403", "Access denied: " + ex.getMessage());
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException ex) throws IOException {
        log.warn("Unauthenticated: {} {} — {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        writeJson(response, 401, "AUTH_401", "Authentication required: " + ex.getMessage());
    }

    private void writeJson(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(response.getOutputStream(),
                Map.of("success", false, "code", code, "message", message));
    }
}
