package com.mynyaman.mynyamanapp;

import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

public final class GroqThreatApi {

    public static final String DEFAULT_MODEL = "llama-3.1-8b-instant";
    private static final String DEFAULT_BASE_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final int API_TIMEOUT_MS = 30000;
    private static final int MAX_COMPLETION_TOKENS = 400;
    private static final double TEMPERATURE = 0.1d;

    private static final String SYSTEM_PROMPT =
            "You are a child-safety cyberbullying and threat classifier for the MyNyaman Android app.\n\n" +
                    "You classify real chat/message text from children and teenagers aged 6 to 16.\n\n" +
                    "You must understand:\n" +
                    "- English\n" +
                    "- Bahasa Melayu\n" +
                    "- Bahasa Rojak\n" +
                    "- Manglish\n" +
                    "- Malay slang\n" +
                    "- internet slang\n" +
                    "- gaming slang\n" +
                    "- school bullying slang\n" +
                    "- abbreviations\n" +
                    "- intentional misspellings\n" +
                    "- censored spellings\n" +
                    "- mixed-language messages\n\n" +
                    "Your task is to analyze the meaning and intent of the message, not just keywords.\n\n" +
                    "Return only valid JSON. Do not use markdown. Do not add extra fields.\n\n" +
                    "Allowed labels:\n" +
                    "SAFE\n" +
                    "JOKE_OR_GAME_CONTEXT\n" +
                    "INSULT\n" +
                    "HARASSMENT\n" +
                    "REAL_WORLD_THREAT\n" +
                    "SELF_HARM_RISK\n" +
                    "SEXUAL_HARASSMENT\n" +
                    "HATE_OR_DISCRIMINATION\n" +
                    "UNKNOWN_NEEDS_REVIEW\n\n" +
                    "Allowed levels:\n" +
                    "Green\n" +
                    "Yellow\n" +
                    "Red\n\n" +
                    "General rules:\n" +
                    "1. Use Green for harmless messages, normal conversation, friendly jokes, friendly teasing, gaming trash talk, and in-game combat language.\n" +
                    "2. Use Yellow for insults, humiliation, bullying, harassment, repeated targeting, social exclusion, degrading language, suspicious pressure, or emotional harm.\n" +
                    "3. Use Red for real-world threats, self-harm encouragement, sexual coercion, grooming pressure, blackmail, doxxing, stalking, hate-based threats, weapons, serious danger, or threats involving real places/people/times.\n\n" +
                    "Multilingual and slang rules:\n" +
                    "1. Understand Bahasa Melayu and mixed Malay-English messages.\n" +
                    "2. Understand Bahasa Rojak and Manglish even when grammar is informal.\n" +
                    "3. Understand teen slang, short forms, misspellings, and censored words.\n" +
                    "4. Do not require perfect grammar.\n" +
                    "5. Do not classify a message as safe just because it uses slang, abbreviations, mixed language, or misspellings.\n" +
                    "6. Infer harmful intent from context, targeting, tone, threat meaning, coercion, humiliation, and repeated abuse.\n\n" +
                    "Gaming context rule:\n" +
                    "If the message clearly refers to a video game, match, hero, enemy, respawn, rank, lobby, Mobile Legends, ML, Valorant, PUBG, Roblox, Minecraft, Fortnite, Free Fire, or similar gaming context, classify it as JOKE_OR_GAME_CONTEXT unless real-world danger is also present.\n" +
                    "For clear gaming context:\n" +
                    "level = Green\n" +
                    "should_alert_parent = false\n" +
                    "risk_score between 0.05 and 0.30\n\n" +
                    "Real-world threat rule:\n" +
                    "If the message contains a direct violent threat without gaming context, classify it as REAL_WORLD_THREAT.\n" +
                    "If the message mentions school, class, home, tomorrow, later, after school, outside, address, family, weapon, knife, gun, stalking, waiting, or meeting in real life, classify it as Red.\n\n" +
                    "Self-harm rule:\n" +
                    "If the message encourages someone to die, disappear, hurt themselves, kill themselves, jump off, end themselves, or says they should not exist, classify it as SELF_HARM_RISK and Red.\n\n" +
                    "Harassment rule:\n" +
                    "If the message insults, humiliates, degrades, excludes, shames, repeatedly targets, or damages someone's self-worth, classify it as INSULT or HARASSMENT.\n" +
                    "Use INSULT for a single mild insult.\n" +
                    "Use HARASSMENT for stronger bullying, repeated targeting, humiliation, or self-worth attacks.\n" +
                    "Yellow harassment should usually notify the parent as a caution alert.\n\n" +
                    "Sexual coercion rule:\n" +
                    "If the message asks for nude images, sexual pictures, sexual favors, threatens exposure, threatens to leak photos, asks for secrecy, or pressures a child sexually, classify it as SEXUAL_HARASSMENT.\n" +
                    "Use Red for coercion, grooming, blackmail, threats, or explicit image requests.\n\n" +
                    "Hate/discrimination rule:\n" +
                    "If the message attacks a person or group based on race, religion, nationality, gender, sexuality, disability, body, or similar personal identity, classify it as HATE_OR_DISCRIMINATION.\n" +
                    "Use Red for violent hate or targeted threats.\n\n" +
                    "Alert rule:\n" +
                    "For Green, should_alert_parent must be false.\n" +
                    "For Yellow, should_alert_parent should be true when the message contains bullying, insults, harassment, self-worth attacks, repeated targeting, coercive language, suspicious pressure, or harmful social exclusion.\n" +
                    "For Red, should_alert_parent must be true.\n\n" +
                    "Context rule:\n" +
                    "Use message text, previous messages, app name, package name, direction, sender, and local metadata if provided.\n" +
                    "If the text is unclear but suspicious, use UNKNOWN_NEEDS_REVIEW with Yellow.\n" +
                    "If the text is harmless or clearly joking/gaming, use Green.\n\n" +
                    "Masked text rule:\n" +
                    "Mask only harmful words or sensitive phrases using asterisks.\n" +
                    "Do not rewrite, shorten, misspell, or remove safe words.\n" +
                    "Keep non-harmful words exactly the same as the original message.\n\n" +
                    "CONTEXT AWARENESS RULES - READ CAREFULLY BEFORE CLASSIFYING:\n\n" +
                    "1. GAMING CONTEXT - Always classify GREEN:\n" +
                    "Any message that mentions a video game title (Call of Duty, Mobile Legends, PUBG, Valorant, Roblox, Minecraft, Free Fire, Fortnite, Genshin Impact, ML, COD, MLBB, or any recognizable game name) together with violent-sounding words like kill, die, dead, shoot, destroy, attack, enemy, or similar. The presence of a game name or game character name is a strong signal of gaming context. Examples that must be GREEN: \"come lets kill the enemy in call of duty\", \"i want zilong to be dead in Mobile Legends\", \"lets go kill them in PUBG\", \"destroy the enemy base in Minecraft\".\n\n" +
                    "2. SPORTS AND COMPETITION CONTEXT - Always classify GREEN:\n" +
                    "Messages about winning, beating, destroying, or killing in the context of sports, matches, tournaments, or competitions. Examples: \"we killed them in the match\", \"lets destroy the other team\", \"I want to beat them badly\".\n\n" +
                    "3. SLANG AND HYPERBOLE - Be careful:\n" +
                    "Casual slang like \"killing it\", \"this song is fire\", \"you are dead to me\", \"I am dying of laughter\" are not threats. Hyperbolic expressions of emotion without a specific real target are not threats. Only classify as YELLOW or RED if the language is directed at a real person with intent to harm, humiliate, coerce, or cause fear.\n\n" +
                    "4. GENUINE THREATS - Must classify YELLOW or RED:\n" +
                    "Direct threats to a specific real person: \"I will hurt you\", \"you will regret this\", \"I know where you live\". Bullying with intent: \"you are worthless\", \"nobody likes you\", \"go kill yourself\". Sexual coercion, blackmail, or harassment. Self-harm encouragement. Threats tied to real-world context: location, time, event, personal information.\n\n" +
                    "5. WHEN IN DOUBT ABOUT CONTEXT:\n" +
                    "If the message contains a game character name (Zilong, Garen, Naruto, etc.) treat it as gaming/fictional context and classify GREEN. If the message contains real personal information (name, school, address, time, meeting place) treat it as a potential real threat and classify YELLOW or RED. A message about killing in a game is not a death threat.\n\n" +
                    "DO NOT penalize messages just because they contain the words kill, die, dead, shoot, or destroy if those words appear in a clearly fictional, gaming, sports, or hyperbolic context.\n\n" +
                    "MULTILINGUAL AND MALAYSIAN CONTEXT RULES:\n\n" +
                    "This system monitors messages from Malaysian children. Messages may be written in Bahasa Malaysia, Bahasa Melayu, Manglish (Malaysian English), Bahasa Rojak, or a mixture of English, Malay, Chinese, and Tamil slang.\n\n" +
                    "You must be able to classify threats and non-threats written in any of these languages or combinations. Do not treat a message as suspicious simply because it is not in standard English.\n\n" +
                    "For Bahasa Malaysia and Manglish classification:\n" +
                    "- Apply the same threat vs non-threat logic you use for English.\n" +
                    "- Recognize that some Malay or Manglish words may sound similar to English threat words but carry different meanings in context.\n" +
                    "- Distinguish between casual rude slang used between friends (which may be GREEN or at most YELLOW) versus genuine targeted harassment, bullying, or threats (which are YELLOW or RED).\n" +
                    "- Recognize gaming terms, sports terms, and hyperbole in Malay and Manglish the same way you do in English.\n" +
                    "- Recognize self-harm encouragement, sexual coercion, racial or religious hatred, and direct personal threats in Malay and Manglish and classify them YELLOW or RED accordingly.\n" +
                    "- When a message mixes languages, analyze the full combined meaning of the message, not individual words in isolation.\n\n" +
                    "You are expected to use your multilingual language understanding to correctly interpret Malaysian slang, Manglish expressions, and Bahasa Malaysia threat patterns without needing explicit word lists.\n\n" +
                    "Return JSON using exactly this format:\n" +
                    "{\n" +
                    "  \"label\": \"\",\n" +
                    "  \"risk_score\": 0.0,\n" +
                    "  \"level\": \"\",\n" +
                    "  \"should_alert_parent\": false,\n" +
                    "  \"reason\": \"\",\n" +
                    "  \"detectedHarmfulPhrase\": \"\",\n" +
                    "  \"contextType\": \"\",\n" +
                    "  \"maskedText\": \"\"\n" +
                    "}";

    private GroqThreatApi() {
    }

    public static boolean isConfigured() {
        boolean configured = !TextUtils.isEmpty(safe(BuildConfig.GROQ_API_KEY));
        if (!configured) {
            Log.w("GROQ_NOT_CONFIGURED", "GROQ_API_KEY is not configured in local.properties.");
        }
        return configured;
    }

    public static ThreatReport analyzeText(AnalysisRequest request) {
        if (request == null || TextUtils.isEmpty(request.messageText) || request.messageText.trim().isEmpty()) {
            return null;
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Log.w("GROQ_FAILED_FALLBACK_LOCAL", "Groq analysis was requested on the main thread. Local fallback will be used.");
            return null;
        }
        if (!isConfigured()) {
            return null;
        }

        try {
            JSONObject payload = new JSONObject();
            payload.put("model", configuredModel());
            payload.put("temperature", TEMPERATURE);
            payload.put("max_completion_tokens", MAX_COMPLETION_TOKENS);
            payload.put("response_format", new JSONObject().put("type", "json_object"));

            JSONArray messages = new JSONArray();
            messages.put(new JSONObject()
                    .put("role", "system")
                    .put("content", SYSTEM_PROMPT));
            messages.put(new JSONObject()
                    .put("role", "user")
                    .put("content", buildUserPayload(request).toString()));
            payload.put("messages", messages);

            Log.d("GROQ_ANALYSIS_STARTED", "Starting Groq text safety analysis.");
            HttpURLConnection connection = (HttpURLConnection) new URL(configuredBaseUrl()).openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(API_TIMEOUT_MS);
            connection.setReadTimeout(API_TIMEOUT_MS);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + safe(BuildConfig.GROQ_API_KEY));

            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(payload.toString().getBytes(StandardCharsets.UTF_8));
            }

            int statusCode = connection.getResponseCode();
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    statusCode >= 200 && statusCode < 300
                            ? connection.getInputStream()
                            : connection.getErrorStream(),
                    StandardCharsets.UTF_8
            ));
            StringBuilder responseBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                responseBuilder.append(line);
            }
            reader.close();

            if (statusCode < 200 || statusCode >= 300) {
                Log.w("GROQ_FAILED_FALLBACK_LOCAL", "Groq HTTP " + statusCode + ". Local fallback will be used.");
                return null;
            }

            JSONObject responseJson = new JSONObject(responseBuilder.toString());
            JSONArray choices = responseJson.optJSONArray("choices");
            if (choices == null || choices.length() == 0) {
                Log.w("GROQ_FAILED_FALLBACK_LOCAL", "Groq response did not include choices. Local fallback will be used.");
                return null;
            }

            JSONObject message = choices.getJSONObject(0).optJSONObject("message");
            if (message == null) {
                Log.w("GROQ_FAILED_FALLBACK_LOCAL", "Groq response did not include a message object. Local fallback will be used.");
                return null;
            }

            String content = extractContentText(message.opt("content"));
            ThreatReport report = parseReport(content, request.messageText, request.localMatchedTerms);
            if (report != null) {
                Log.d("GROQ_FINAL_RESULT", "Groq result: level=" + ThreatResult.displayName(report.level)
                        + ", label=" + report.label
                        + ", alert=" + report.shouldAlertParent);
            }
            return report;
        } catch (Exception exception) {
            Log.w("GROQ_FAILED_FALLBACK_LOCAL", "Groq analysis failed. Local fallback will be used.", exception);
            return null;
        }
    }

    private static JSONObject buildUserPayload(AnalysisRequest request) throws Exception {
        JSONObject object = new JSONObject();
        object.put("message", safe(request.messageText));

        JSONArray previousMessages = new JSONArray();
        if (request.previousMessages != null) {
            int start = Math.max(0, request.previousMessages.size() - 5);
            for (int index = start; index < request.previousMessages.size(); index++) {
                String previous = request.previousMessages.get(index);
                if (!TextUtils.isEmpty(previous)) {
                    previousMessages.put(previous);
                }
            }
        }
        object.put("previousMessages", previousMessages);
        object.put("foregroundAppName", safe(request.foregroundAppName));
        object.put("packageName", safe(request.packageName));
        object.put("direction", safe(request.direction));
        object.put("sender", safe(request.sender));
        object.put("extractedFromCompositeAccessibilityString", request.extractedFromCompositeAccessibilityString);
        object.put("localSignals", request.localSignals == null ? new JSONObject() : request.localSignals);
        object.put("localMetadata", new JSONObject()
                .put("localSignalsAreWeakOnly", true)
                .put("localVerdictIsFinalTruth", false)
                .put("captureSource", "android_accessibility_text"));
        return object;
    }

    private static ThreatReport parseReport(String rawContent, String originalText, List<String> localMatchedTerms) {
        String jsonText = extractJson(rawContent);
        if (TextUtils.isEmpty(jsonText)) {
            Log.w("GROQ_FAILED_FALLBACK_LOCAL", "Groq returned empty content. Local fallback will be used.");
            return null;
        }

        try {
            JSONObject object = new JSONObject(jsonText);
            ThreatResult.Level level = parseLevel(object.optString("level"));
            if (level == null) {
                Log.w("GROQ_FAILED_FALLBACK_LOCAL", "Groq returned an unrecognized level. Local fallback will be used.");
                return null;
            }

            String label = normalizeLabel(object.optString("label"));
            double riskScore = clampRiskScore(object.optDouble("risk_score", defaultRiskScore(level)));
            boolean actionableLabel = isActionableAlertLabel(label);
            boolean shouldAlertParent = level == ThreatResult.Level.GREEN
                    ? false
                    : actionableLabel;
            String reason = safe(object.optString("reason"));
            if (TextUtils.isEmpty(reason)) {
                reason = "Groq completed contextual safety review.";
            }

            String maskedText = safe(object.optString("maskedText"));
            if (TextUtils.isEmpty(maskedText)) {
                maskedText = MaskedContentHelper.maskPreview(originalText, localMatchedTerms);
            }

            JSONObject normalizedJson = new JSONObject();
            normalizedJson.put("label", label);
            normalizedJson.put("risk_score", riskScore);
            normalizedJson.put("level", ThreatResult.displayName(level));
            normalizedJson.put("should_alert_parent", shouldAlertParent);
            normalizedJson.put("reason", reason);
            normalizedJson.put("detectedHarmfulPhrase", safe(object.optString("detectedHarmfulPhrase")));
            normalizedJson.put("contextType", safe(object.optString("contextType")));
            normalizedJson.put("maskedText", maskedText);

            return new ThreatReport(
                    level,
                    label,
                    riskScore,
                    shouldAlertParent,
                    reason,
                    safe(object.optString("detectedHarmfulPhrase")),
                    safe(object.optString("contextType")),
                    maskedText,
                    normalizedJson.toString()
            );
        } catch (Exception exception) {
            Log.w("GROQ_FAILED_FALLBACK_LOCAL", "Groq returned invalid JSON. Local fallback will be used.", exception);
            return null;
        }
    }

    private static boolean isActionableAlertLabel(String label) {
        String normalized = safe(label).toUpperCase(Locale.US);
        return "INSULT".equals(normalized)
                || "HARASSMENT".equals(normalized)
                || "REAL_WORLD_THREAT".equals(normalized)
                || "SELF_HARM_RISK".equals(normalized)
                || "SEXUAL_HARASSMENT".equals(normalized)
                || "HATE_OR_DISCRIMINATION".equals(normalized);
    }

    private static String extractJson(String rawContent) {
        String clean = safe(rawContent);
        if (TextUtils.isEmpty(clean)) {
            return "";
        }
        if (clean.startsWith("```")) {
            clean = clean.replace("```json", "").replace("```", "").trim();
        }
        int start = clean.indexOf('{');
        int end = clean.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return clean.substring(start, end + 1);
        }
        return clean;
    }

    private static String extractContentText(Object content) {
        if (content instanceof String) {
            return (String) content;
        }
        if (content instanceof JSONArray) {
            JSONArray array = (JSONArray) content;
            StringBuilder builder = new StringBuilder();
            for (int index = 0; index < array.length(); index++) {
                JSONObject item = array.optJSONObject(index);
                if (item == null) {
                    continue;
                }
                if ("text".equals(item.optString("type"))) {
                    if (builder.length() > 0) {
                        builder.append('\n');
                    }
                    builder.append(item.optString("text"));
                }
            }
            return builder.toString();
        }
        return "";
    }

    private static ThreatResult.Level parseLevel(String value) {
        if (TextUtils.isEmpty(value)) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.US);
        if ("RED".equals(normalized)) {
            return ThreatResult.Level.RED;
        }
        if ("YELLOW".equals(normalized)) {
            return ThreatResult.Level.YELLOW;
        }
        if ("GREEN".equals(normalized)) {
            return ThreatResult.Level.GREEN;
        }
        return null;
    }

    private static String normalizeLabel(String value) {
        String normalized = safe(value).toUpperCase(Locale.US);
        switch (normalized) {
            case "SAFE":
            case "JOKE_OR_GAME_CONTEXT":
            case "INSULT":
            case "HARASSMENT":
            case "REAL_WORLD_THREAT":
            case "SELF_HARM_RISK":
            case "SEXUAL_HARASSMENT":
            case "HATE_OR_DISCRIMINATION":
            case "UNKNOWN_NEEDS_REVIEW":
                return normalized;
            default:
                return "UNKNOWN_NEEDS_REVIEW";
        }
    }

    private static double clampRiskScore(double value) {
        if (Double.isNaN(value)) {
            return 0.0d;
        }
        return Math.max(0.0d, Math.min(1.0d, value));
    }

    private static double defaultRiskScore(ThreatResult.Level level) {
        if (level == ThreatResult.Level.RED) {
            return 0.9d;
        }
        if (level == ThreatResult.Level.YELLOW) {
            return 0.65d;
        }
        return 0.1d;
    }

    private static String configuredModel() {
        String model = safe(BuildConfig.GROQ_MODEL);
        return TextUtils.isEmpty(model) ? DEFAULT_MODEL : model;
    }

    private static String configuredBaseUrl() {
        String baseUrl = safe(BuildConfig.GROQ_BASE_URL);
        return TextUtils.isEmpty(baseUrl) ? DEFAULT_BASE_URL : baseUrl;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public static final class AnalysisRequest {
        public final String messageText;
        public final List<String> previousMessages;
        public final String foregroundAppName;
        public final String packageName;
        public final JSONObject localSignals;
        public final List<String> localMatchedTerms;
        public final String direction;
        public final String sender;
        public final boolean extractedFromCompositeAccessibilityString;

        public AnalysisRequest(
                String messageText,
                List<String> previousMessages,
                String foregroundAppName,
                String packageName,
                JSONObject localSignals,
                List<String> localMatchedTerms,
                String direction,
                String sender,
                boolean extractedFromCompositeAccessibilityString
        ) {
            this.messageText = messageText;
            this.previousMessages = previousMessages;
            this.foregroundAppName = foregroundAppName;
            this.packageName = packageName;
            this.localSignals = localSignals;
            this.localMatchedTerms = localMatchedTerms;
            this.direction = direction;
            this.sender = sender;
            this.extractedFromCompositeAccessibilityString = extractedFromCompositeAccessibilityString;
        }
    }

    public static final class ThreatReport {
        public final ThreatResult.Level level;
        public final String label;
        public final double riskScore;
        public final boolean shouldAlertParent;
        public final String reason;
        public final String detectedHarmfulPhrase;
        public final String contextType;
        public final String maskedText;
        public final String rawJson;

        ThreatReport(
                ThreatResult.Level level,
                String label,
                double riskScore,
                boolean shouldAlertParent,
                String reason,
                String detectedHarmfulPhrase,
                String contextType,
                String maskedText,
                String rawJson
        ) {
            this.level = level;
            this.label = label;
            this.riskScore = riskScore;
            this.shouldAlertParent = shouldAlertParent;
            this.reason = reason;
            this.detectedHarmfulPhrase = detectedHarmfulPhrase;
            this.contextType = contextType;
            this.maskedText = maskedText;
            this.rawJson = rawJson;
        }
    }
}
