package com.mynyaman.mynyamanapp;

import java.util.HashMap;
import java.util.Map;

public final class DuplicateSuppressionManager {

    private static final long HARMFUL_DUPLICATE_COOLDOWN_MS = 30000L;

    private final Map<String, Long> lastSubmittedAtByText = new HashMap<>();
    private final Map<String, Integer> repeatedCountByText = new HashMap<>();

    public DuplicateDecision shouldSubmit(String normalizedText, long nowMs) {
        String normalized = normalizedText == null ? "" : normalizedText;
        Long lastSubmittedAt = lastSubmittedAtByText.get(normalized);
        if (lastSubmittedAt != null && nowMs - lastSubmittedAt < HARMFUL_DUPLICATE_COOLDOWN_MS) {
            int count = repeatedCountByText.containsKey(normalized)
                    ? repeatedCountByText.get(normalized) + 1
                    : 2;
            repeatedCountByText.put(normalized, count);
            return new DuplicateDecision(false, count, "SKIPPED_DUPLICATE", "Repeated candidate inside cooldown.");
        }

        lastSubmittedAtByText.put(normalized, nowMs);
        repeatedCountByText.put(normalized, 1);
        return new DuplicateDecision(true, 1, "LOCAL_GATE_PASSED_TO_GROQ", "Candidate changed or cooldown passed.");
    }

    public static final class DuplicateDecision {
        public final boolean shouldSubmit;
        public final int repeatedCount;
        public final String logCode;
        public final String reason;

        DuplicateDecision(boolean shouldSubmit, int repeatedCount, String logCode, String reason) {
            this.shouldSubmit = shouldSubmit;
            this.repeatedCount = repeatedCount;
            this.logCode = logCode;
            this.reason = reason;
        }
    }
}
