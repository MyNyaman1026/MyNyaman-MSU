package com.mynyaman.mynyamanapp;

public final class ThreatTextPipeline {

    private static final long IGNORED_DUPLICATE_COOLDOWN_MS = 10000L;

    private final OcrStabilityTracker stabilityTracker = new OcrStabilityTracker();
    private final DuplicateSuppressionManager duplicateSuppressionManager = new DuplicateSuppressionManager();
    private final java.util.Map<String, Long> lastIgnoredAtByText = new java.util.HashMap<>();

    public ThreatTextGate.GateResult preflight(String rawText, long nowMs) {
        ThreatTextGate.GateResult result = ThreatTextGate.evaluate(rawText);
        if (!result.accepted && !result.normalizedText.isEmpty()) {
            Long lastIgnoredAt = lastIgnoredAtByText.get(result.normalizedText);
            lastIgnoredAtByText.put(result.normalizedText, nowMs);
            if (lastIgnoredAt != null && nowMs - lastIgnoredAt < IGNORED_DUPLICATE_COOLDOWN_MS) {
                return ThreatTextGate.GateResult.blocked(
                        result.displayText,
                        result.normalizedText,
                        "SKIPPED_DUPLICATE",
                        "Repeated ignored OCR/UI text.",
                        result.screenResult,
                        result.decision
                );
            }
        }
        if (result.accepted) {
            stabilityTracker.observe(result.normalizedText, nowMs);
        }
        return result;
    }

    public SubmissionResult beforeSubmit(String rawText, long nowMs) {
        ThreatTextGate.GateResult gateResult = ThreatTextGate.evaluate(rawText);
        if (!gateResult.accepted) {
            return SubmissionResult.blocked(gateResult, gateResult.logCode, gateResult.reason, 0);
        }

        if (!canBypassStability(gateResult) && !stabilityTracker.isStable(gateResult.normalizedText, nowMs)) {
            return SubmissionResult.blocked(
                    gateResult,
                    "MESSAGE_GATE_DROP_LOW_VALUE",
                    "Candidate did not remain stable long enough.",
                    0
            );
        }

        DuplicateSuppressionManager.DuplicateDecision duplicateDecision =
                duplicateSuppressionManager.shouldSubmit(gateResult.normalizedText, nowMs);
        if (!duplicateDecision.shouldSubmit) {
            return SubmissionResult.blocked(
                    gateResult,
                    duplicateDecision.logCode,
                    duplicateDecision.reason,
                    duplicateDecision.repeatedCount
            );
        }

        return SubmissionResult.accepted(gateResult, duplicateDecision.logCode, duplicateDecision.repeatedCount);
    }

    private static boolean canBypassStability(ThreatTextGate.GateResult gateResult) {
        return gateResult != null
                && gateResult.decision == ThreatTextGate.Decision.SEND_TO_GROQ_SUSPICIOUS_MESSAGE;
    }

    public static final class SubmissionResult {
        public final boolean shouldSubmit;
        public final ThreatTextGate.GateResult gateResult;
        public final String logCode;
        public final String reason;
        public final int repeatedCount;

        private SubmissionResult(
                boolean shouldSubmit,
                ThreatTextGate.GateResult gateResult,
                String logCode,
                String reason,
                int repeatedCount
        ) {
            this.shouldSubmit = shouldSubmit;
            this.gateResult = gateResult;
            this.logCode = logCode;
            this.reason = reason;
            this.repeatedCount = repeatedCount;
        }

        static SubmissionResult accepted(ThreatTextGate.GateResult gateResult, String logCode, int repeatedCount) {
            return new SubmissionResult(true, gateResult, logCode, "Ready for Groq/Llama analysis.", repeatedCount);
        }

        static SubmissionResult blocked(
                ThreatTextGate.GateResult gateResult,
                String logCode,
                String reason,
                int repeatedCount
        ) {
            return new SubmissionResult(false, gateResult, logCode, reason, repeatedCount);
        }
    }
}
