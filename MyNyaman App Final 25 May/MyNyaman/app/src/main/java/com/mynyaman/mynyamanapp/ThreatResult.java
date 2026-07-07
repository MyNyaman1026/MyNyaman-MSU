package com.mynyaman.mynyamanapp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ThreatResult {

    public enum Level {
        GREEN(0),
        YELLOW(1),
        RED(2);

        private final int severity;

        Level(int severity) {
            this.severity = severity;
        }

        public int getSeverity() {
            return severity;
        }

        public boolean isHigherThan(Level other) {
            return other == null || severity > other.severity;
        }
    }

    public final String originalText;
    public final String normalizedText;
    public final Level ruleLevel;
    public final Level contextualLevel;
    public final Level remoteLevel;
    public final Level finalLevel;
    public final List<String> matchedTerms;
    public final String maskedPreview;
    public final String reason;
    public final String reportJson;
    public final String contentType;
    public final boolean consultantSuggested;
    public final boolean remoteReviewUsed;
    public final String summary;

    public ThreatResult(
            String originalText,
            String normalizedText,
            Level ruleLevel,
            Level contextualLevel,
            Level remoteLevel,
            Level finalLevel,
            List<String> matchedTerms,
            String maskedPreview,
            String reason,
            String reportJson,
            String contentType,
            boolean consultantSuggested,
            boolean remoteReviewUsed,
            String summary
    ) {
        this.originalText = originalText;
        this.normalizedText = normalizedText;
        this.ruleLevel = ruleLevel;
        this.contextualLevel = contextualLevel;
        this.remoteLevel = remoteLevel;
        this.finalLevel = finalLevel;
        this.matchedTerms = matchedTerms == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(matchedTerms));
        this.maskedPreview = maskedPreview;
        this.reason = reason;
        this.reportJson = reportJson;
        this.contentType = contentType;
        this.consultantSuggested = consultantSuggested;
        this.remoteReviewUsed = remoteReviewUsed;
        this.summary = summary;
    }

    public boolean shouldNotifyParent() {
        return finalLevel != Level.GREEN && consultantSuggested;
    }

    public boolean shouldNotifyChild() {
        return finalLevel != Level.GREEN && consultantSuggested;
    }

    public boolean isHighPriority() {
        return shouldNotifyParent() && finalLevel == Level.RED;
    }

    public static Level max(Level first, Level second) {
        if (first == null) {
            return second == null ? Level.GREEN : second;
        }
        if (second == null) {
            return first;
        }
        return first.severity >= second.severity ? first : second;
    }

    public static String displayName(Level level) {
        if (level == Level.RED) {
            return "Red";
        }
        if (level == Level.YELLOW) {
            return "Yellow";
        }
        return "Green";
    }
}
