package com.codehaja.common.security;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.springframework.boot.jackson.JsonComponent;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Globally sanitizes all incoming JSON String fields to prevent XSS.
 * Targets only dangerous patterns (<script>, javascript:, inline event handlers).
 * Does NOT strip all HTML to avoid breaking code content with < > characters.
 */
@JsonComponent
public class XssStringDeserializer extends StdDeserializer<String> {

    private static final Pattern SCRIPT_TAG =
            Pattern.compile("<\\s*script[\\s\\S]*?>[\\s\\S]*?<\\s*/\\s*script\\s*>",
                    Pattern.CASE_INSENSITIVE);

    private static final Pattern SCRIPT_OPEN =
            Pattern.compile("<\\s*script[^>]*>", Pattern.CASE_INSENSITIVE);

    private static final Pattern JAVASCRIPT_URL =
            Pattern.compile("javascript\\s*:", Pattern.CASE_INSENSITIVE);

    private static final Pattern EVENT_HANDLERS =
            Pattern.compile("\\bon\\w+\\s*=\\s*(\"[^\"]*\"|'[^']*'|[^\\s>]*)",
                    Pattern.CASE_INSENSITIVE);

    public XssStringDeserializer() {
        super(String.class);
    }

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
        String value = p.getValueAsString();
        if (value == null || value.isBlank()) return value;
        return sanitize(value);
    }

    private String sanitize(String value) {
        value = SCRIPT_TAG.matcher(value).replaceAll("");
        value = SCRIPT_OPEN.matcher(value).replaceAll("");
        value = JAVASCRIPT_URL.matcher(value).replaceAll("blocked:");
        value = EVENT_HANDLERS.matcher(value).replaceAll("");
        return value;
    }
}
