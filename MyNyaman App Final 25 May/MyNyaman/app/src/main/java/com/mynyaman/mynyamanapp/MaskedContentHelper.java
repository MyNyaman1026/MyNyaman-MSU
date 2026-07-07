package com.mynyaman.mynyamanapp;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MaskedContentHelper {

    private static final int PREVIEW_LIMIT = 140;

    private MaskedContentHelper() {
    }

    public static String maskPreview(String originalText, List<String> matchedTerms) {
        if (TextUtils.isEmpty(originalText)) {
            return "";
        }

        String masked = originalText;
        List<String> safeTerms = new ArrayList<>();
        if (matchedTerms != null) {
            for (String term : matchedTerms) {
                if (!TextUtils.isEmpty(term)) {
                    safeTerms.add(term);
                }
            }
        }
        safeTerms.sort(Comparator.comparingInt(String::length).reversed());

        for (String term : safeTerms) {
            String maskedTerm = maskPhrase(term);
            Pattern pattern = Pattern.compile(Pattern.quote(term), Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(masked);
            masked = matcher.replaceAll(Matcher.quoteReplacement(maskedTerm));
        }

        if (TextUtils.equals(masked, originalText) && !safeTerms.isEmpty()) {
            masked = maskWordWise(originalText, safeTerms);
        }

        if (masked.length() > PREVIEW_LIMIT) {
            masked = masked.substring(0, PREVIEW_LIMIT - 1).trim() + "…";
        }
        return masked;
    }

    public static String maskPhrase(String phrase) {
        if (TextUtils.isEmpty(phrase)) {
            return "";
        }

        String[] parts = phrase.split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < parts.length; index++) {
            if (index > 0) {
                builder.append(' ');
            }
            builder.append(maskSingleToken(parts[index]));
        }
        return builder.toString();
    }

    private static String maskWordWise(String originalText, List<String> matchedTerms) {
        String masked = originalText;
        for (String term : matchedTerms) {
            String[] pieces = term.toLowerCase(Locale.US).split("\\s+");
            for (String piece : pieces) {
                if (TextUtils.isEmpty(piece)) {
                    continue;
                }
                Pattern pattern = Pattern.compile("\\b" + Pattern.quote(piece) + "\\b", Pattern.CASE_INSENSITIVE);
                Matcher matcher = pattern.matcher(masked);
                masked = matcher.replaceAll(Matcher.quoteReplacement(maskSingleToken(piece)));
            }
        }
        return masked;
    }

    private static String maskSingleToken(String token) {
        if (TextUtils.isEmpty(token)) {
            return "";
        }

        int visibleCount = 0;
        for (int index = 0; index < token.length(); index++) {
            if (Character.isLetterOrDigit(token.charAt(index))) {
                visibleCount++;
            }
        }

        if (visibleCount <= 1) {
            return "*";
        }

        StringBuilder builder = new StringBuilder();
        boolean firstVisibleWritten = false;
        for (int index = 0; index < token.length(); index++) {
            char value = token.charAt(index);
            if (!Character.isLetterOrDigit(value)) {
                builder.append(value);
                continue;
            }
            if (!firstVisibleWritten) {
                builder.append(value);
                firstVisibleWritten = true;
            } else {
                builder.append('*');
            }
        }
        return builder.toString();
    }
}
