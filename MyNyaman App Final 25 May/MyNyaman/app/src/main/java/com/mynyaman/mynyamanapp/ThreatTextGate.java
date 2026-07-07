package com.mynyaman.mynyamanapp;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class ThreatTextGate {

    public enum Decision {
        DROP_UI_NOISE,
        DROP_STATUS_TEXT,
        DROP_PROFILE_METADATA,
        DROP_USERNAME_ONLY,
        DROP_SHORT_HARMLESS_FILLER,
        DROP_LOW_VALUE_NON_MESSAGE,
        SEND_TO_GROQ_MESSAGE,
        SEND_TO_GROQ_SUSPICIOUS_MESSAGE
    }

    private static final int MAX_MESSAGE_CHARS = 700;

    private static final Set<String> SHORT_HARMLESS_FILLER = new HashSet<>(Arrays.asList(
            "ok",
            "okay",
            "yes",
            "no",
            "haha",
            "hahaha",
            "lol",
            "thanks",
            "thank you",
            "hello",
            "hi",
            "hey",
            "bye",
            "confirm",
            "delete",
            "close",
            "cancel",
            "recents",
            "folders",
            "updates",
            "communities",
            "requests",
            "pending"
    ));
    private static final Set<String> HARMFUL_LANGUAGE_TERMS = new HashSet<>(Arrays.asList(
            "stupid",
            "dumb",
            "idiot",
            "bodoh",
            "babi",
            "worthless",
            "useless",
            "ugly",
            "loser",
            "pathetic",
            "freak",
            "trash",
            "slut",
            "whore"
    ));

    private static final Pattern USERNAME_HANDLE_PATTERN =
            Pattern.compile("^@?[a-z0-9][a-z0-9._]{2,29}$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PAGE_COUNTER_PATTERN =
            Pattern.compile("^page\\s+\\d+\\s+of\\s+\\d+\\s+pages?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PROFILE_METADATA_PATTERN =
            Pattern.compile(".*\\b(follower|followers|following|posts?|joined|follow each other)\\b.*", Pattern.CASE_INSENSITIVE);
    private static final Pattern TARGETED_INSULT_PATTERN =
            Pattern.compile(".*\\b(you|your|u|ur|kau|awak|kamu|dia)\\b.*\\b(stupid|dumb|idiot|bodoh|worthless|tak guna|ugly|loser|pathetic|freak|trash)\\b.*", Pattern.CASE_INSENSITIVE);

    private ThreatTextGate() {
    }

    public static GateResult evaluate(String rawText) {
        String displayText = ThreatTextNormalizer.displayText(rawText);
        String normalizedText = ThreatTextNormalizer.normalizedText(rawText);
        LocalThreatScreen.ScreenResult weakScreenResult = LocalThreatScreen.scan(normalizedText);

        if (normalizedText.isEmpty()) {
            return GateResult.blocked(
                    displayText,
                    normalizedText,
                    "MESSAGE_GATE_DROP_LOW_VALUE",
                    "Empty captured text.",
                    weakScreenResult,
                    Decision.DROP_LOW_VALUE_NON_MESSAGE
            );
        }

        UiNoiseFilter.NoiseResult noiseResult = UiNoiseFilter.evaluate(normalizedText);
        if (noiseResult.noise) {
            return GateResult.blocked(
                    displayText,
                    normalizedText,
                    "UI_GATE_DROP",
                    noiseResult.reason,
                    weakScreenResult,
                    mapNoiseDecision(noiseResult.type)
            );
        }

        if (isProfileMetadata(normalizedText)) {
            return GateResult.blocked(
                    displayText,
                    normalizedText,
                    "UI_GATE_DROP",
                    "Profile metadata, not chat message text.",
                    weakScreenResult,
                    Decision.DROP_PROFILE_METADATA
            );
        }

        if (isUsernameOnly(normalizedText)) {
            return GateResult.blocked(
                    displayText,
                    normalizedText,
                    "MESSAGE_GATE_DROP_LOW_VALUE",
                    "Username or handle only.",
                    weakScreenResult,
                    Decision.DROP_USERNAME_ONLY
            );
        }

        if (isShortHarmlessFiller(normalizedText)) {
            return GateResult.blocked(
                    displayText,
                    normalizedText,
                    "MESSAGE_GATE_DROP_LOW_VALUE",
                    "Short harmless chat filler.",
                    weakScreenResult,
                    Decision.DROP_SHORT_HARMLESS_FILLER
            );
        }

        if (!isMessageLike(normalizedText)) {
            return GateResult.blocked(
                    displayText,
                    normalizedText,
                    "MESSAGE_GATE_DROP_LOW_VALUE",
                    "Captured text is low-value non-message content.",
                    weakScreenResult,
                    Decision.DROP_LOW_VALUE_NON_MESSAGE
            );
        }

        if (!hasActionableReviewCue(normalizedText)) {
            return GateResult.blocked(
                    displayText,
                    normalizedText,
                    "MESSAGE_GATE_DROP_LOW_VALUE",
                    "Message has no actionable threat, bullying, coercion, self-harm, sexual, hate, or real-world danger cue.",
                    weakScreenResult,
                    Decision.DROP_LOW_VALUE_NON_MESSAGE
            );
        }

        return GateResult.accepted(
                displayText,
                normalizedText,
                "MESSAGE_GATE_SEND_TO_GROQ",
                "Actionable safety cue selected for Groq/Llama review: " + Decision.SEND_TO_GROQ_SUSPICIOUS_MESSAGE.name(),
                weakScreenResult,
                Decision.SEND_TO_GROQ_SUSPICIOUS_MESSAGE
        );
    }

    public static boolean isSendDecision(Decision decision) {
        return decision == Decision.SEND_TO_GROQ_MESSAGE
                || decision == Decision.SEND_TO_GROQ_SUSPICIOUS_MESSAGE;
    }

    private static Decision mapNoiseDecision(UiNoiseFilter.NoiseType type) {
        if (type == UiNoiseFilter.NoiseType.STATUS_TEXT) {
            return Decision.DROP_STATUS_TEXT;
        }
        if (type == UiNoiseFilter.NoiseType.PROFILE_METADATA) {
            return Decision.DROP_PROFILE_METADATA;
        }
        return Decision.DROP_UI_NOISE;
    }

    private static boolean isProfileMetadata(String normalizedText) {
        return PAGE_COUNTER_PATTERN.matcher(normalizedText).matches()
                || PROFILE_METADATA_PATTERN.matcher(normalizedText).matches();
    }

    private static boolean isUsernameOnly(String normalizedText) {
        if (isEmpty(normalizedText) || normalizedText.contains(" ")) {
            return false;
        }
        if (!USERNAME_HANDLE_PATTERN.matcher(normalizedText).matches()) {
            return false;
        }
        return normalizedText.startsWith("@")
                || normalizedText.contains("_")
                || normalizedText.contains(".")
                || normalizedText.matches(".*\\d.*");
    }

    private static boolean isShortHarmlessFiller(String normalizedText) {
        return SHORT_HARMLESS_FILLER.contains(normalizedText);
    }

    private static boolean isMessageLike(String normalizedText) {
        if (isEmpty(normalizedText) || normalizedText.length() > MAX_MESSAGE_CHARS) {
            return false;
        }
        if (!normalizedText.matches(".*[a-z].*")) {
            return false;
        }
        if (wordCount(normalizedText) >= 2) {
            return true;
        }
        return normalizedText.length() >= 3
                && normalizedText.matches("[a-z][a-z'!?*#@$-]*")
                && !isShortHarmlessFiller(normalizedText);
    }

    private static boolean hasActionableReviewCue(String normalizedText) {
        if (hasClearNonRealThreatContext(normalizedText) && !containsSevereNonGameCue(normalizedText)) {
            return false;
        }
        return containsDirectThreatCue(normalizedText)
                || containsSelfHarmCue(normalizedText)
                || containsSexualCoercionCue(normalizedText)
                || containsBlackmailOrPrivacyCue(normalizedText)
                || containsTargetedBullyingCue(normalizedText)
                || containsHarmfulLanguageCue(normalizedText)
                || containsHateOrDiscriminationCue(normalizedText)
                || containsSuspiciousRealWorldPressure(normalizedText);
    }

    private static boolean hasClearNonRealThreatContext(String normalizedText) {
        if (containsRealWorldCue(normalizedText)) {
            return false;
        }
        return containsGameContextCue(normalizedText)
                || containsCompetitionContextCue(normalizedText)
                || containsHyperboleCue(normalizedText);
    }

    private static boolean containsSevereNonGameCue(String normalizedText) {
        return containsSelfHarmCue(normalizedText)
                || containsSexualCoercionCue(normalizedText)
                || containsBlackmailOrPrivacyCue(normalizedText)
                || containsTargetedBullyingCue(normalizedText)
                || containsHarmfulLanguageCue(normalizedText)
                || containsHateOrDiscriminationCue(normalizedText);
    }

    private static boolean containsDirectThreatCue(String normalizedText) {
        return containsAnyPhrase(normalizedText,
                "kill you",
                "murder you",
                "hurt you",
                "stab you",
                "shoot you",
                "find you",
                "beat you",
                "i will kill you",
                "i'll kill you",
                "i will murder you",
                "i'll murder you",
                "im going to kill you",
                "i'm going to kill you",
                "i will hurt you",
                "i'll hurt you",
                "i will stab you",
                "i'll stab you",
                "i will shoot you",
                "i'll shoot you",
                "im going to hurt you",
                "i'm going to hurt you",
                "i will find you",
                "i'll find you",
                "i will beat you",
                "i'll beat you",
                "i know where you live",
                "you will regret this",
                "bunuh kau",
                "aku bunuh kau",
                "aku akan bunuh kau",
                "pukul kau",
                "aku pukul kau",
                "cari kau",
                "aku cari kau",
                "nanti aku cari kau",
                "kau tunggu",
                "tunggu kau");
    }

    private static boolean containsSelfHarmCue(String normalizedText) {
        return containsAnyPhrase(normalizedText,
                "kill yourself",
                "go die",
                "die already",
                "you should die",
                "you should disappear",
                "go kill yourself",
                "jump off",
                "jump off and die",
                "end yourself",
                "pergi mati",
                "jangan hidup",
                "tak payah hidup",
                "bunuh diri");
    }

    private static boolean containsSexualCoercionCue(String normalizedText) {
        return containsToken(normalizedText, "rape")
                || containsAnyPhrase(normalizedText,
                "send nudes",
                "send nude",
                "send naked",
                "send your body",
                "send pic",
                "send picture",
                "hantar gambar",
                "hantar nude",
                "hantar gambar bogel",
                "sex with me")
                || (containsAnyToken(normalizedText, "nude", "nudes", "porn", "horny", "dick", "pussy", "cum", "blowjob", "rape")
                && (containsPersonalCue(normalizedText) || containsPressureCue(normalizedText)));
    }

    private static boolean containsBlackmailOrPrivacyCue(String normalizedText) {
        return containsAnyPhrase(normalizedText,
                "i will expose you",
                "i'll expose you",
                "i will leak",
                "i'll leak",
                "or i expose you",
                "or i leak",
                "aku sebarkan",
                "aku akan sebarkan",
                "jangan bagitahu",
                "keep it secret")
                || (containsAnyToken(normalizedText, "expose", "leak", "dox", "blackmail", "sebarkan", "rahsia")
                && containsPersonalCue(normalizedText));
    }

    private static boolean containsTargetedBullyingCue(String normalizedText) {
        return TARGETED_INSULT_PATTERN.matcher(normalizedText).matches()
                || containsToken(normalizedText, "bully")
                || containsToken(normalizedText, "buli")
                || containsAnyPhrase(normalizedText,
                "nobody likes you",
                "no one likes you",
                "everyone hates you",
                "you are worthless",
                "you're worthless",
                "you are useless",
                "you're useless",
                "i hate you",
                "kau tak guna",
                "kau memang tak guna",
                "kau bodoh",
                "kamu bodoh",
                "awak bodoh",
                "jangan kawan dia",
                "jom buli dia");
    }

    private static boolean containsHarmfulLanguageCue(String normalizedText) {
        for (String term : HARMFUL_LANGUAGE_TERMS) {
            if (containsAnyPhrase(normalizedText, term)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsHateOrDiscriminationCue(String normalizedText) {
        return containsAnyPhrase(normalizedText,
                "go back to your country",
                "your race",
                "your religion")
                || (containsAnyToken(normalizedText, "race", "religion", "muslim", "christian", "hindu", "disabled", "autistic", "fat", "gay")
                && containsTargetedBullyingCue(normalizedText));
    }

    private static boolean containsSuspiciousRealWorldPressure(String normalizedText) {
        return (containsRealWorldCue(normalizedText) && (containsPressureCue(normalizedText) || containsDirectThreatCue(normalizedText)))
                || containsAnyPhrase(normalizedText,
                "wait outside school",
                "after school",
                "outside your house",
                "i know your address",
                "aku tunggu kau lepas sekolah",
                "tunggu luar sekolah",
                "jumpa luar sekolah");
    }

    private static boolean containsGameContextCue(String normalizedText) {
        return containsToken(normalizedText, "ml")
                || containsToken(normalizedText, "mlbb")
                || normalizedText.contains("mobile legends")
                || normalizedText.contains("call of duty")
                || containsToken(normalizedText, "cod")
                || containsToken(normalizedText, "valorant")
                || containsToken(normalizedText, "pubg")
                || containsToken(normalizedText, "roblox")
                || containsToken(normalizedText, "minecraft")
                || containsToken(normalizedText, "fortnite")
                || normalizedText.contains("free fire")
                || containsToken(normalizedText, "game")
                || containsToken(normalizedText, "match")
                || containsToken(normalizedText, "lobby")
                || containsToken(normalizedText, "rank")
                || containsToken(normalizedText, "enemy")
                || containsToken(normalizedText, "respawn");
    }

    private static boolean containsCompetitionContextCue(String normalizedText) {
        return containsAnyToken(normalizedText, "team", "match", "tournament", "score", "win", "rank", "competition");
    }

    private static boolean containsHyperboleCue(String normalizedText) {
        return containsAnyPhrase(normalizedText,
                "dying of laughter",
                "killing it",
                "this song is fire",
                "you are dead to me");
    }

    private static boolean containsPersonalCue(String normalizedText) {
        return containsToken(normalizedText, "you")
                || containsToken(normalizedText, "your")
                || containsToken(normalizedText, "u")
                || containsToken(normalizedText, "i")
                || containsToken(normalizedText, "im")
                || containsToken(normalizedText, "i'm")
                || containsToken(normalizedText, "aku")
                || containsToken(normalizedText, "kau")
                || containsToken(normalizedText, "awak")
                || containsToken(normalizedText, "dia");
    }

    private static boolean containsRealWorldCue(String normalizedText) {
        return containsToken(normalizedText, "school")
                || containsToken(normalizedText, "class")
                || containsToken(normalizedText, "outside")
                || containsToken(normalizedText, "tomorrow")
                || containsToken(normalizedText, "later")
                || containsToken(normalizedText, "sekolah")
                || containsToken(normalizedText, "kelas")
                || containsToken(normalizedText, "luar")
                || containsToken(normalizedText, "esok");
    }

    private static boolean containsPressureCue(String normalizedText) {
        return containsToken(normalizedText, "send")
                || containsToken(normalizedText, "hantar")
                || containsToken(normalizedText, "wait")
                || containsToken(normalizedText, "tunggu")
                || containsToken(normalizedText, "meet")
                || containsToken(normalizedText, "jumpa")
                || containsToken(normalizedText, "secret")
                || containsToken(normalizedText, "rahsia");
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

    private static String searchableText(String normalizedText) {
        return " " + normalizedText
                .toLowerCase(Locale.US)
                .replaceAll("[^a-z0-9']+", " ")
                .replaceAll("\\s+", " ")
                .trim() + " ";
    }

    private static boolean containsToken(String normalizedText, String token) {
        String[] parts = normalizedText.toLowerCase(Locale.US).split("[^a-z0-9']+");
        for (String part : parts) {
            if (token.equals(part)) {
                return true;
            }
        }
        return false;
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

    public static final class GateResult {
        public final boolean accepted;
        public final String displayText;
        public final String normalizedText;
        public final String logCode;
        public final String reason;
        public final LocalThreatScreen.ScreenResult screenResult;
        public final Decision decision;

        private GateResult(
                boolean accepted,
                String displayText,
                String normalizedText,
                String logCode,
                String reason,
                LocalThreatScreen.ScreenResult screenResult,
                Decision decision
        ) {
            this.accepted = accepted;
            this.displayText = displayText;
            this.normalizedText = normalizedText;
            this.logCode = logCode;
            this.reason = reason;
            this.screenResult = screenResult;
            this.decision = decision;
        }

        static GateResult accepted(
                String displayText,
                String normalizedText,
                String logCode,
                String reason,
                LocalThreatScreen.ScreenResult screenResult,
                Decision decision
        ) {
            return new GateResult(true, displayText, normalizedText, logCode, reason, screenResult, decision);
        }

        static GateResult blocked(
                String displayText,
                String normalizedText,
                String logCode,
                String reason,
                LocalThreatScreen.ScreenResult screenResult,
                Decision decision
        ) {
            return new GateResult(false, displayText, normalizedText, logCode, reason, screenResult, decision);
        }
    }
}
