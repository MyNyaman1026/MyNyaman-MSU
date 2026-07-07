package com.mynyaman.mynyamanapp;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class AiConsultantApi {

    private static final String TAG = "AiConsultantApi";

    public interface Callback {
        void onSuccess(String response);
        void onFailure(String message);
    }

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final String DEFAULT_BASE_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String DEFAULT_CONSULTANT_MODEL = "llama-3.3-70b-versatile";
    private static final String SYSTEM_PROMPT = "You are MyNyaman's AI consultant for digital wellbeing and family online safety. You support parents and children with screen time management, cyberbullying concerns, online safety, emotional wellbeing related to device use, and parent-child communication. Give calm, practical, age-appropriate, family-safe advice. Keep replies concise, supportive, and easy to understand. If the user asks unrelated questions, politely redirect them back to screen time, cyberbullying, online safety, digital wellbeing, or family guidance. You are not a doctor, therapist, lawyer, police officer, or emergency service. If the user mentions immediate danger, self-harm, abuse, grooming, sexual exploitation, or real-world violence, advise them to contact a trusted adult, local emergency services, school counselor, parent, guardian, or relevant authority immediately.";

    private AiConsultantApi() {
    }

    public static void sendMessage(List<BlueprintSyncRepository.ConsultantMessageRecord> messages, Callback callback) {
        EXECUTOR.execute(() -> {
            try {
                ApiConfig apiConfig = resolveApiConfig();
                if (apiConfig == null) {
                    callback.onFailure("AI consultant is not configured yet. Add GROQ_API_KEY to local.properties.");
                    return;
                }

                JSONObject payload = new JSONObject();
                payload.put("model", apiConfig.model);
                payload.put("temperature", 0.5d);
                payload.put("max_tokens", 600);

                JSONArray messageArray = new JSONArray();
                JSONObject systemMessage = new JSONObject();
                systemMessage.put("role", "system");
                systemMessage.put("content", SYSTEM_PROMPT);
                messageArray.put(systemMessage);

                for (BlueprintSyncRepository.ConsultantMessageRecord message : messages) {
                    JSONObject item = new JSONObject();
                    item.put("role", message.role);
                    item.put("content", message.content);
                    messageArray.put(item);
                }
                payload.put("messages", messageArray);

                HttpURLConnection connection = (HttpURLConnection) new URL(apiConfig.endpoint).openConnection();
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(20000);
                connection.setReadTimeout(30000);
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Authorization", "Bearer " + apiConfig.apiKey);

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
                    Log.w(TAG, "Groq consultant request failed with HTTP " + statusCode + ".");
                    callback.onFailure("AI consultant is unavailable right now. Please try again shortly.");
                    return;
                }

                JSONObject responseJson = new JSONObject(responseBuilder.toString());
                JSONArray choices = responseJson.optJSONArray("choices");
                if (choices == null || choices.length() == 0) {
                    callback.onFailure("AI consultant returned an empty response. Please try again.");
                    return;
                }

                JSONObject firstChoice = choices.optJSONObject(0);
                JSONObject message = firstChoice == null ? null : firstChoice.optJSONObject("message");
                if (message == null) {
                    callback.onFailure("AI consultant returned an empty response. Please try again.");
                    return;
                }

                Object content = message.opt("content");
                String assistantText = extractContentText(content);
                if (TextUtils.isEmpty(assistantText)) {
                    callback.onFailure("AI consultant returned an empty response. Please try again.");
                    return;
                }

                callback.onSuccess(assistantText.trim());
            } catch (Exception exception) {
                Log.w(TAG, "Groq consultant request failed: " + exception.getClass().getSimpleName()
                        + ": " + safe(exception.getMessage()));
                callback.onFailure("AI consultant is unavailable right now. Please try again shortly.");
            }
        });
    }

    private static ApiConfig resolveApiConfig() {
        String apiKey = safe(BuildConfig.GROQ_API_KEY);
        if (TextUtils.isEmpty(apiKey)) {
            return null;
        }

        String endpoint = safe(BuildConfig.GROQ_BASE_URL);
        if (TextUtils.isEmpty(endpoint)) {
            endpoint = DEFAULT_BASE_URL;
        }

        String model = safe(BuildConfig.GROQ_CONSULTANT_MODEL);
        if (TextUtils.isEmpty(model)) {
            model = DEFAULT_CONSULTANT_MODEL;
        }
        return new ApiConfig(endpoint, apiKey, model);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String extractContentText(Object content) {
        if (content instanceof String) {
            return (String) content;
        }
        if (content instanceof JSONArray) {
            JSONArray contentArray = (JSONArray) content;
            StringBuilder builder = new StringBuilder();
            for (int index = 0; index < contentArray.length(); index++) {
                JSONObject item = contentArray.optJSONObject(index);
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

    private static final class ApiConfig {
        final String endpoint;
        final String apiKey;
        final String model;

        ApiConfig(String endpoint, String apiKey, String model) {
            this.endpoint = endpoint;
            this.apiKey = apiKey;
            this.model = model;
        }
    }
}
