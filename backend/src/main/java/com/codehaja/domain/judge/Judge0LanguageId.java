package com.codehaja.domain.judge;

import java.util.Map;

public class Judge0LanguageId {

    private static final Map<String, Integer> MAP = Map.of(
        "python",     71,
        "javascript", 63,
        "java",       62,
        "cpp",        54,
        "c",          50
    );

    public static int of(String language) {
        return MAP.getOrDefault(language == null ? "" : language.toLowerCase(), 71);
    }
}
