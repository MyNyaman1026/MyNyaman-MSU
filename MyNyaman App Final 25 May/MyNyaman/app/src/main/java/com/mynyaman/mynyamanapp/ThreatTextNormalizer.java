package com.mynyaman.mynyamanapp;

import java.util.Locale;

public final class ThreatTextNormalizer {

    private ThreatTextNormalizer() {
    }

    public static String displayText(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace('\n', ' ')
                .replace('\r', ' ')
                .replace('\u2026', '.')
                .replaceAll("\\s+", " ")
                .trim();
    }

    public static String normalizedText(String value) {
        return displayText(value)
                .replace('\u2018', '\'')
                .replace('\u2019', '\'')
                .replace('\u201c', '"')
                .replace('\u201d', '"')
                .replaceAll("[.]{2,}", "...")
                .toLowerCase(Locale.US)
                .trim();
    }
}
