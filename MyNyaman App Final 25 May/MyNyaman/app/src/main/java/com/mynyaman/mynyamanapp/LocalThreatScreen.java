package com.mynyaman.mynyamanapp;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Weak local fallback metadata only.
 *
 * Groq/Llama is the real cyberbullying and threat classifier. This class must
 * not be used as the normal Groq gate and must not make final safety decisions
 * while Groq returns a valid result. It intentionally avoids cyberbullying,
 * threat, and bad-word dictionaries so the app does not behave like local
 * keyword detection.
 */
public final class LocalThreatScreen {

    public enum Category {
        SAFE,
        LOW_CONCERN,
        MODERATE_CONCERN,
        HIGH_CONCERN
    }

    private LocalThreatScreen() {
    }

    public static ScreenResult scan(String normalizedText) {
        String normalized = normalizedText == null ? "" : normalizedText.toLowerCase(Locale.US).trim();
        int wordCount = wordCount(normalized);
        boolean targeted = containsSecondPersonCue(normalized);
        boolean directIntent = containsFirstPersonIntentCue(normalized);
        boolean realWorldContext = containsRealWorldContextCue(normalized);
        boolean gameContext = containsGameContextCue(normalized);
        boolean pressureContext = containsPressureShapeCue(normalized);
        boolean highIntensity = hasHighIntensityStyle(normalized);

        Category category = Category.SAFE;
        if (wordCount > 0) {
            category = Category.LOW_CONCERN;
        }
        if ((targeted && (directIntent || pressureContext))
                || realWorldContext
                || (highIntensity && targeted)) {
            category = Category.MODERATE_CONCERN;
        }

        return new ScreenResult(
                category,
                new ArrayList<>(),
                0,
                0,
                0,
                0,
                targeted,
                false,
                0,
                0,
                0,
                0,
                realWorldContext ? 1 : 0,
                gameContext ? 1 : 0
        );
    }

    public static boolean isKnownCompleteHarmfulTerm(String normalizedText) {
        return false;
    }

    public static boolean isIncompleteHarmfulPrefix(String normalizedText) {
        return false;
    }

    private static boolean containsSecondPersonCue(String normalizedText) {
        return containsToken(normalizedText, "you")
                || containsToken(normalizedText, "your")
                || containsToken(normalizedText, "u")
                || containsToken(normalizedText, "kau")
                || containsToken(normalizedText, "awak")
                || containsToken(normalizedText, "dia");
    }

    private static boolean containsFirstPersonIntentCue(String normalizedText) {
        return containsToken(normalizedText, "i")
                || containsToken(normalizedText, "im")
                || containsToken(normalizedText, "i'm")
                || containsToken(normalizedText, "aku")
                || containsToken(normalizedText, "kita")
                || containsToken(normalizedText, "jom")
                || containsToken(normalizedText, "lets")
                || containsToken(normalizedText, "let's");
    }

    private static boolean containsRealWorldContextCue(String normalizedText) {
        return containsToken(normalizedText, "school")
                || containsToken(normalizedText, "class")
                || containsToken(normalizedText, "home")
                || containsToken(normalizedText, "tomorrow")
                || containsToken(normalizedText, "later")
                || containsToken(normalizedText, "outside")
                || containsToken(normalizedText, "address")
                || containsToken(normalizedText, "family")
                || containsToken(normalizedText, "sekolah")
                || containsToken(normalizedText, "kelas")
                || containsToken(normalizedText, "rumah")
                || containsToken(normalizedText, "esok")
                || containsToken(normalizedText, "nanti")
                || containsToken(normalizedText, "luar");
    }

    private static boolean containsGameContextCue(String normalizedText) {
        return containsToken(normalizedText, "ml")
                || normalizedText.contains("mobile legends")
                || containsToken(normalizedText, "valorant")
                || containsToken(normalizedText, "pubg")
                || containsToken(normalizedText, "roblox")
                || containsToken(normalizedText, "minecraft")
                || containsToken(normalizedText, "fortnite")
                || normalizedText.contains("free fire")
                || containsToken(normalizedText, "game")
                || containsToken(normalizedText, "match")
                || containsToken(normalizedText, "lobby")
                || containsToken(normalizedText, "rank");
    }

    private static boolean containsPressureShapeCue(String normalizedText) {
        return containsToken(normalizedText, "send")
                || containsToken(normalizedText, "hantar")
                || containsToken(normalizedText, "give")
                || containsToken(normalizedText, "meet")
                || containsToken(normalizedText, "jumpa")
                || containsToken(normalizedText, "secret")
                || containsToken(normalizedText, "rahsia");
    }

    private static boolean hasHighIntensityStyle(String normalizedText) {
        return normalizedText.contains("!!")
                || normalizedText.contains("???")
                || normalizedText.contains("!?");
    }

    private static boolean containsToken(String normalizedText, String token) {
        if (isEmpty(normalizedText) || isEmpty(token)) {
            return false;
        }
        String padded = " " + normalizedText + " ";
        return padded.contains(" " + token + " ");
    }

    private static int wordCount(String normalizedText) {
        if (isEmpty(normalizedText)) {
            return 0;
        }
        return normalizedText.trim().split("\\s+").length;
    }

    private static boolean isEmpty(String value) {
        return value == null || value.isEmpty();
    }

    public static final class ScreenResult {
        public final Category category;
        public final List<String> matchedTerms;
        public final int mildMatches;
        public final int severeMatches;
        public final int explicitMatches;
        public final int suspiciousMatches;
        public final boolean targeted;
        public final boolean suspectedEscalate;
        public final int violenceMatches;
        public final int bullyingTriggerMatches;
        public final int selfHarmMatches;
        public final int sexualCoercionMatches;
        public final int realWorldContextMatches;
        public final int gameContextMatches;

        ScreenResult(
                Category category,
                List<String> matchedTerms,
                int mildMatches,
                int severeMatches,
                int explicitMatches,
                int suspiciousMatches,
                boolean targeted,
                boolean suspectedEscalate,
                int violenceMatches,
                int bullyingTriggerMatches,
                int selfHarmMatches,
                int sexualCoercionMatches,
                int realWorldContextMatches,
                int gameContextMatches
        ) {
            this.category = category;
            this.matchedTerms = matchedTerms;
            this.mildMatches = mildMatches;
            this.severeMatches = severeMatches;
            this.explicitMatches = explicitMatches;
            this.suspiciousMatches = suspiciousMatches;
            this.targeted = targeted;
            this.suspectedEscalate = suspectedEscalate;
            this.violenceMatches = violenceMatches;
            this.bullyingTriggerMatches = bullyingTriggerMatches;
            this.selfHarmMatches = selfHarmMatches;
            this.sexualCoercionMatches = sexualCoercionMatches;
            this.realWorldContextMatches = realWorldContextMatches;
            this.gameContextMatches = gameContextMatches;
        }

        public boolean shouldAnalyzeRemotely() {
            return category != Category.SAFE;
        }

        public boolean hasHighRiskTrigger() {
            return false;
        }

        public boolean needsContextReview() {
            return category == Category.MODERATE_CONCERN;
        }

        public int priorityScore() {
            switch (category) {
                case MODERATE_CONCERN:
                    return 60;
                case LOW_CONCERN:
                    return 25;
                case HIGH_CONCERN:
                    return 80;
                case SAFE:
                default:
                    return 0;
            }
        }
    }
}
