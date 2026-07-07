package com.mynyaman.mynyamanapp;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ThreatAnalyzer {

    private ThreatAnalyzer() {
    }

    /**
     * Backward-compatible helper. This no longer means local keyword threat
     * detection; it only means the text is a candidate for Groq/Llama review.
     */
    public static boolean hasLocalTextTrigger(Context context, String messageText) {
        ThreatTextGate.GateResult gateResult = ThreatTextGate.evaluate(messageText);
        return gateResult.accepted;
    }

    public static ThreatResult analyze(Context context, String messageText) {
        return analyze(context, messageText, null, null, null, null, null);
    }

    public static ThreatResult analyze(
            Context context,
            String messageText,
            List<String> previousMessages,
            String appName,
            String packageName,
            String direction,
            String sender
    ) {
        String safeText = messageText == null ? "" : messageText.trim();
        String normalizedText = normalize(safeText);
        if (TextUtils.isEmpty(normalizedText)) {
            return buildGreenResult(safeText, normalizedText, "No analyzable text was captured.");
        }

        ThreatTextGate.GateResult gateResult = ThreatTextGate.evaluate(safeText);
        if (!gateResult.accepted) {
            Log.d("ThreatPipeline", gateResult.logCode + ": \"" + safeText + "\" (" + gateResult.reason + ")");
            return buildGreenResult(safeText, normalizedText, gateResult.reason);
        }
        Log.d("ThreatPipeline", gateResult.logCode + ": \"" + safeText + "\" (" + gateResult.reason + ")");
        Log.d("LOCAL_GATE_PASSED_TO_GROQ", "Local code selected message-like text for Groq/Llama review.");

        LocalScanResult localFallback = scanLocalFallback(gateResult.screenResult);
        List<String> matchedTerms = new ArrayList<>();

        GroqThreatApi.ThreatReport groqReport = null;
        boolean groqConfigured = GroqThreatApi.isConfigured();
        boolean groqAttempted = false;
        boolean groqRateLimited = false;
        if (groqConfigured && GroqRateLimiter.tryAcquire()) {
            groqAttempted = true;
            groqReport = GroqThreatApi.analyzeText(new GroqThreatApi.AnalysisRequest(
                    safeText,
                    previousMessages,
                    appName,
                    packageName,
                    buildLocalSignals(localFallback, gateResult),
                    matchedTerms,
                    direction,
                    sender,
                    false
            ));
        } else if (groqConfigured) {
            groqRateLimited = true;
            Log.w("GROQ_FAILED_FALLBACK_LOCAL", "Groq rate limit reached. Local fallback will be used.");
        }

        ThreatResult.Level remoteLevel = groqReport == null ? null : groqReport.level;
        boolean remoteReviewUsed = groqReport != null && remoteLevel != null;
        HighConfidenceLocalResult highConfidenceLocalResult = scanHighConfidenceLocalFallback(normalizedText);
        ThreatResult.Level localDecisionLevel = ThreatResult.max(localFallback.level, highConfidenceLocalResult.level);
        ThreatResult.Level finalLevel = remoteReviewUsed
                ? ThreatResult.max(remoteLevel, highConfidenceLocalResult.level)
                : highConfidenceLocalResult.level;
        boolean highConfidenceLocalAlert = highConfidenceLocalResult.level != ThreatResult.Level.GREEN;
        boolean highConfidenceLocalOverride = highConfidenceLocalAlert
                && (!remoteReviewUsed
                || !groqReport.shouldAlertParent
                || highConfidenceLocalResult.level.isHigherThan(remoteLevel));

        if (remoteReviewUsed) {
            Log.d("GROQ_FINAL_RESULT", "Groq/Llama final safety decision: level="
                    + ThreatResult.displayName(finalLevel)
                    + ", label=" + groqReport.label
                    + ", alert=" + groqReport.shouldAlertParent
                    + ", highConfidenceLocalOverride=" + highConfidenceLocalOverride);
        } else {
            String fallbackReason = groqRateLimited
                    ? "Groq rate limit reached."
                    : groqConfigured && groqAttempted
                    ? "Groq failed or returned invalid JSON."
                    : "Groq is not configured.";
            Log.w("LOCAL_FALLBACK_USED", fallbackReason + " High-confidence local fallback level="
                    + ThreatResult.displayName(highConfidenceLocalResult.level) + ".");
        }

        String maskedPreview = groqReport != null && !TextUtils.isEmpty(groqReport.maskedText)
                ? groqReport.maskedText
                : safeText;
        String reason = highConfidenceLocalOverride
                ? highConfidenceLocalResult.reason
                : groqReport != null && !TextUtils.isEmpty(groqReport.reason)
                ? groqReport.reason
                : buildLocalFallbackReason(highConfidenceLocalResult);
        String reportJson = !highConfidenceLocalOverride && groqReport != null && !TextUtils.isEmpty(groqReport.rawJson)
                ? groqReport.rawJson
                : buildReportJson(
                        finalLevel,
                        reason,
                        maskedPreview,
                        highConfidenceLocalResult.label,
                        highConfidenceLocalResult.contextType
                );
        boolean consultantSuggested = remoteReviewUsed
                ? (groqReport.shouldAlertParent || highConfidenceLocalAlert)
                && finalLevel != ThreatResult.Level.GREEN
                : highConfidenceLocalAlert;

        return new ThreatResult(
                safeText,
                normalizedText,
                localDecisionLevel,
                localDecisionLevel,
                remoteLevel,
                finalLevel,
                matchedTerms,
                maskedPreview,
                reason,
                reportJson,
                "text",
                consultantSuggested,
                remoteReviewUsed,
                buildSummary(
                        localFallback,
                        highConfidenceLocalResult,
                        remoteLevel,
                        finalLevel,
                        remoteReviewUsed,
                        gateResult.decision
                )
        );
    }

    private static ThreatResult buildGreenResult(String originalText, String normalizedText, String reason) {
        String maskedText = TextUtils.isEmpty(originalText) ? "" : originalText;
        return new ThreatResult(
                originalText,
                normalizedText,
                ThreatResult.Level.GREEN,
                ThreatResult.Level.GREEN,
                null,
                ThreatResult.Level.GREEN,
                new ArrayList<>(),
                maskedText,
                reason,
                buildReportJson(ThreatResult.Level.GREEN, reason, maskedText),
                "text",
                false,
                false,
                reason
        );
    }

    private static LocalScanResult scanLocalFallback(LocalThreatScreen.ScreenResult screenResult) {
        ThreatResult.Level level = ThreatResult.Level.GREEN;
        if (screenResult != null && screenResult.category == LocalThreatScreen.Category.MODERATE_CONCERN) {
            level = ThreatResult.Level.YELLOW;
        } else if (screenResult != null && screenResult.category == LocalThreatScreen.Category.HIGH_CONCERN) {
            level = ThreatResult.Level.RED;
        }
        return new LocalScanResult(level, screenResult);
    }

    private static HighConfidenceLocalResult scanHighConfidenceLocalFallback(String normalizedText) {
        if (TextUtils.isEmpty(normalizedText)) {
            return HighConfidenceLocalResult.none();
        }

        boolean gameOrCompetitionContext = containsGameOrCompetitionContext(normalizedText);
        boolean realWorldContext = containsRealWorldCue(normalizedText);

        if (containsSelfHarmEmergency(normalizedText)) {
            return HighConfidenceLocalResult.red(
                    "SELF_HARM_RISK",
                    "High-confidence local safety check detected explicit self-harm encouragement."
            );
        }

        if (containsSexualCoercionEmergency(normalizedText)) {
            return HighConfidenceLocalResult.red(
                    "SEXUAL_HARASSMENT",
                    "High-confidence local safety check detected explicit sexual coercion."
            );
        }

        if (containsBlackmailEmergency(normalizedText)) {
            return HighConfidenceLocalResult.red(
                    "BLACKMAIL_OR_PRIVACY_THREAT",
                    "High-confidence local safety check detected explicit blackmail or exposure pressure."
            );
        }

        if (containsDirectViolenceEmergency(normalizedText)
                && (!gameOrCompetitionContext || realWorldContext)) {
            return HighConfidenceLocalResult.red(
                    "REAL_WORLD_THREAT",
                    "High-confidence local safety check detected an explicit real-world violence threat."
            );
        }

        return HighConfidenceLocalResult.none();
    }

    private static boolean containsDirectViolenceEmergency(String normalizedText) {
        return containsAnyPhrase(normalizedText,
                "kill you",
                "murder you",
                "hurt you",
                "beat you",
                "stab you",
                "shoot you",
                "find you",
                "i will kill you",
                "i'll kill you",
                "i will murder you",
                "i'll murder you",
                "im going to kill you",
                "i'm going to kill you",
                "i will hurt you",
                "i'll hurt you",
                "i will beat you",
                "i'll beat you",
                "i will stab you",
                "i'll stab you",
                "i will shoot you",
                "i'll shoot you",
                "i know where you live",
                "i know your address",
                "wait outside school",
                "outside your house",
                "bunuh kau",
                "aku bunuh kau",
                "aku akan bunuh kau",
                "pukul kau",
                "aku pukul kau",
                "cari kau",
                "aku cari kau",
                "nanti aku cari kau",
                "aku tunggu kau lepas sekolah",
                "tunggu luar sekolah",
                "kau tunggu");
    }

    private static boolean containsSelfHarmEmergency(String normalizedText) {
        return containsAnyPhrase(normalizedText,
                "kill yourself",
                "go kill yourself",
                "go die",
                "die already",
                "you should die",
                "jump off",
                "jump off and die",
                "end yourself",
                "pergi mati",
                "jangan hidup",
                "tak payah hidup",
                "bunuh diri");
    }

    private static boolean containsSexualCoercionEmergency(String normalizedText) {
        return containsAnyPhrase(normalizedText,
                "send nudes",
                "send nude",
                "send naked",
                "send your body",
                "hantar nude",
                "hantar gambar bogel",
                "sex with me")
                || (containsAnyToken(normalizedText, "nude", "nudes", "bogel", "porn", "dick", "pussy", "cum", "blowjob")
                && containsPersonalCue(normalizedText)
                && containsPressureCue(normalizedText))
                || (containsToken(normalizedText, "rape")
                && containsPersonalCue(normalizedText)
                && !containsGameOrCompetitionContext(normalizedText));
    }

    private static boolean containsBlackmailEmergency(String normalizedText) {
        return containsAnyPhrase(normalizedText,
                "i will expose you",
                "i'll expose you",
                "i will leak",
                "i'll leak",
                "or i expose you",
                "or i leak",
                "hantar gambar atau aku sebarkan",
                "aku sebarkan",
                "aku akan sebarkan")
                || containsToken(normalizedText, "blackmail")
                || (containsAnyToken(normalizedText, "expose", "leak", "dox", "sebarkan")
                && containsPersonalCue(normalizedText));
    }

    private static boolean containsGameOrCompetitionContext(String normalizedText) {
        return containsAnyPhrase(normalizedText, "mobile legends", "call of duty", "free fire", "counter strike")
                || containsAnyToken(
                        normalizedText,
                        "ml",
                        "mlbb",
                        "cod",
                        "valorant",
                        "pubg",
                        "roblox",
                        "minecraft",
                        "fortnite",
                        "game",
                        "match",
                        "lobby",
                        "rank",
                        "enemy",
                        "respawn",
                        "team",
                        "tournament",
                        "score",
                        "win",
                        "competition"
                );
    }

    private static boolean containsRealWorldCue(String normalizedText) {
        return containsAnyToken(
                normalizedText,
                "school",
                "class",
                "outside",
                "tomorrow",
                "later",
                "home",
                "house",
                "address",
                "family",
                "sekolah",
                "kelas",
                "luar",
                "esok",
                "nanti",
                "rumah",
                "alamat"
        );
    }

    private static boolean containsPersonalCue(String normalizedText) {
        return containsAnyToken(normalizedText, "you", "your", "u", "i", "im", "i'm", "aku", "kau", "awak", "kamu", "dia");
    }

    private static boolean containsPressureCue(String normalizedText) {
        return containsAnyToken(normalizedText, "send", "hantar", "give", "meet", "jumpa", "secret", "rahsia");
    }

    private static boolean containsAnyPhrase(String normalizedText, String... phrases) {
        String searchable = searchableText(normalizedText);
        for (String phrase : phrases) {
            if (searchable.contains(" " + phrase + " ")) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsAnyToken(String normalizedText, String... tokens) {
        for (String token : tokens) {
            if (containsToken(normalizedText, token)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsToken(String normalizedText, String token) {
        if (TextUtils.isEmpty(normalizedText) || TextUtils.isEmpty(token)) {
            return false;
        }
        String[] parts = normalizedText.toLowerCase(Locale.US).split("[^a-z0-9']+");
        for (String part : parts) {
            if (token.equals(part)) {
                return true;
            }
        }
        return false;
    }

    private static String searchableText(String normalizedText) {
        if (TextUtils.isEmpty(normalizedText)) {
            return " ";
        }
        return " " + normalizedText
                .toLowerCase(Locale.US)
                .replaceAll("[^a-z0-9']+", " ")
                .replaceAll("\\s+", " ")
                .trim() + " ";
    }

    private static String buildLocalFallbackReason(HighConfidenceLocalResult highConfidenceLocalResult) {
        if (highConfidenceLocalResult.level == ThreatResult.Level.GREEN) {
            return "AI review was unavailable, so no threat alert was created from weak local fallback metadata.";
        }
        return highConfidenceLocalResult.reason;
    }

    private static String buildSummary(
            LocalScanResult localFallback,
            HighConfidenceLocalResult highConfidenceLocalResult,
            ThreatResult.Level remoteLevel,
            ThreatResult.Level finalLevel,
            boolean remoteReviewUsed,
            ThreatTextGate.Decision gateDecision
    ) {
        StringBuilder builder = new StringBuilder();
        builder.append("gate=").append(gateDecision == null ? "unknown" : gateDecision.name());
        builder.append(", localFallback=").append(localFallback.level.name());
        builder.append(", highConfidenceLocalFallback=").append(highConfidenceLocalResult.level.name());
        if (highConfidenceLocalResult.level != ThreatResult.Level.GREEN
                && !TextUtils.isEmpty(highConfidenceLocalResult.label)) {
            builder.append(", highConfidenceLabel=").append(highConfidenceLocalResult.label);
        }
        if (remoteLevel != null) {
            builder.append(", groq=").append(remoteLevel.name());
        }
        builder.append(", final=").append(finalLevel.name());
        builder.append(", groqUsed=").append(remoteReviewUsed);
        return builder.toString();
    }

    private static JSONObject buildLocalSignals(
            LocalScanResult localFallback,
            ThreatTextGate.GateResult gateResult
    ) {
        JSONObject object = new JSONObject();
        try {
            LocalThreatScreen.ScreenResult screenResult = localFallback.screenResult;
            object.put("localSignalsAreWeakOnly", true);
            object.put("notFinalClassification", true);
            object.put("messageCandidateGate", gateResult == null || gateResult.decision == null ? "" : gateResult.decision.name());
            object.put("gateReason", gateResult == null ? "" : gateResult.reason);
            object.put("weakFallbackLevel", ThreatResult.displayName(localFallback.level));
            if (screenResult != null) {
                object.put("weakFallbackCategory", screenResult.category.name());
                object.put("weakTargetedCue", screenResult.targeted);
                object.put("weakRealWorldContextCue", screenResult.realWorldContextMatches > 0);
                object.put("weakGameContextCue", screenResult.gameContextMatches > 0);
            }
        } catch (Exception ignored) {
        }
        return object;
    }

    private static String buildReportJson(ThreatResult.Level level, String reason, String maskedText) {
        return buildReportJson(
                level,
                reason,
                maskedText,
                level == ThreatResult.Level.GREEN ? "SAFE" : "UNKNOWN_NEEDS_REVIEW",
                "local_fallback"
        );
    }

    private static String buildReportJson(
            ThreatResult.Level level,
            String reason,
            String maskedText,
            String label,
            String contextType
    ) {
        try {
            JSONObject object = new JSONObject();
            object.put("label", TextUtils.isEmpty(label) ? "UNKNOWN_NEEDS_REVIEW" : label);
            object.put("risk_score", level == ThreatResult.Level.RED ? 0.9d : level == ThreatResult.Level.YELLOW ? 0.65d : 0.1d);
            object.put("level", ThreatResult.displayName(level));
            object.put("should_alert_parent", level != ThreatResult.Level.GREEN);
            object.put("reason", reason == null ? "" : reason);
            object.put("detectedHarmfulPhrase", "");
            object.put("contextType", TextUtils.isEmpty(contextType) ? "local_fallback" : contextType);
            object.put("maskedText", maskedText == null ? "" : maskedText);
            return object.toString();
        } catch (Exception ignored) {
            return "{}";
        }
    }

    private static String normalize(String text) {
        return text == null
                ? ""
                : text.trim().replaceAll("\\s+", " ").toLowerCase(Locale.US);
    }

    private static final class LocalScanResult {
        final ThreatResult.Level level;
        final LocalThreatScreen.ScreenResult screenResult;

        LocalScanResult(ThreatResult.Level level, LocalThreatScreen.ScreenResult screenResult) {
            this.level = level;
            this.screenResult = screenResult;
        }
    }

    private static final class HighConfidenceLocalResult {
        final ThreatResult.Level level;
        final String label;
        final String reason;
        final String contextType;

        private HighConfidenceLocalResult(
                ThreatResult.Level level,
                String label,
                String reason,
                String contextType
        ) {
            this.level = level;
            this.label = label;
            this.reason = reason;
            this.contextType = contextType;
        }

        static HighConfidenceLocalResult none() {
            return new HighConfidenceLocalResult(
                    ThreatResult.Level.GREEN,
                    "SAFE",
                    "AI review was unavailable, and no high-confidence local danger phrase was detected.",
                    "local_fallback"
            );
        }

        static HighConfidenceLocalResult red(String label, String reason) {
            return new HighConfidenceLocalResult(
                    ThreatResult.Level.RED,
                    label,
                    reason,
                    "local_high_confidence_fallback"
            );
        }
    }
}
