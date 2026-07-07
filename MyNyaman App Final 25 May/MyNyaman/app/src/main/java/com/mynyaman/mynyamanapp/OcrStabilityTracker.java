package com.mynyaman.mynyamanapp;

public final class OcrStabilityTracker {

    private static final long STABLE_WINDOW_MS = 700L;

    private String latestNormalizedText;
    private long firstObservedAtMs;

    public void observe(String normalizedText, long nowMs) {
        String normalized = normalizedText == null ? "" : normalizedText;
        if (!normalized.equals(latestNormalizedText)) {
            latestNormalizedText = normalized;
            firstObservedAtMs = nowMs;
        }
    }

    public boolean isStable(String normalizedText, long nowMs) {
        if (normalizedText == null || !normalizedText.equals(latestNormalizedText)) {
            return false;
        }
        return nowMs - firstObservedAtMs >= STABLE_WINDOW_MS
                || LocalThreatScreen.isKnownCompleteHarmfulTerm(normalizedText);
    }
}
