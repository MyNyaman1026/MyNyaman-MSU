package com.mynyaman.mynyamanapp;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class UiNoiseFilter {

    public enum NoiseType {
        NONE("MESSAGE_GATE_SEND_TO_GROQ"),
        UI_TEXT("UI_GATE_DROP"),
        STATUS_TEXT("UI_GATE_DROP"),
        PROFILE_METADATA("UI_GATE_DROP"),
        MEDIA_TEXT("UI_GATE_DROP"),
        TIMESTAMP_TEXT("UI_GATE_DROP"),
        COUNTER_TEXT("UI_GATE_DROP");

        public final String logCode;

        NoiseType(String logCode) {
            this.logCode = logCode;
        }
    }

    private static final Set<String> UI_EXACT = new HashSet<>(Arrays.asList(
            "more options",
            "more",
            "instagram",
            "whatsapp",
            "messenger",
            "telegram",
            "gmail",
            "tiktok",
            "tik tok",
            "facebook",
            "message",
            "messages",
            "message info",
            "search",
            "menu",
            "attach",
            "attachment",
            "camera",
            "gallery",
            "reply",
            "send",
            "call",
            "voice call",
            "video call",
            "back",
            "open navigation drawer",
            "emoji",
            "stickers",
            "new chat",
            "tap to add",
            "double tap to like",
            "new message",
            "new messages",
            "tap to chat",
            "tap to view",
            "view once",
            "view status",
            "profile picture",
            "mentioned you",
            "missed call",
            "incoming call",
            "select messages",
            "select message",
            "mark as read",
            "mark as unread",
            "chats",
            "status",
            "calls",
            "stories",
            "reels",
            "home",
            "profile",
            "settings",
            "application icon",
            "search and explore",
            "search or ask meta ai",
            "your story",
            "add to story",
            "draw",
            "try it",
            "not now",
            "go to most recent message",
            "instagram home feed",
            "my status, tap to add new status",
            "recent updates",
            "new community",
            "add note",
            "map",
            "see all",
            "accounts to follow",
            "follow requests"
    ));

    private static final Set<String> STATUS_EXACT = new HashSet<>(Arrays.asList(
            "sent",
            "delivered",
            "seen",
            "read",
            "online",
            "typing",
            "typing.",
            "typing...",
            "is typing",
            "is typing...",
            "recording",
            "recording audio",
            "recording voice",
            "last seen",
            "unread",
            "active now",
            "active today",
            "loading",
            "loading.",
            "loading..."
    ));

    private static final Set<String> MEDIA_EXACT = new HashSet<>(Arrays.asList(
            "photo",
            "video",
            "image",
            "sticker",
            "gif",
            "audio",
            "voice note",
            "voice note recorder",
            "forward image",
            "forward video",
            "forward photo",
            "document",
            "enlarge photo",
            "enlarge",
            "view photo",
            "view image",
            "video note",
            "media preview",
            "whatsapp camera"
    ));

    private static final Pattern[] STATUS_PATTERNS = {
            Pattern.compile("^typing[.\\s]*$"),
            Pattern.compile("^last seen\\b.*"),
            Pattern.compile("^\\d+ unread messages?$"),
            Pattern.compile("^\\d+ messages?$"),
            Pattern.compile("^\\d+ message$"),
            Pattern.compile("^\\d+ new messages?$"),
            Pattern.compile("^active\\s+(now|today|\\d+\\s*(s|m|h|d|w|sec|secs|min|mins|hr|hrs|hour|hours|minute|minutes|day|days|week|weeks)\\s+ago)$"),
            Pattern.compile(".*\\d+\\s+unread\\s+status(es)?.*", Pattern.CASE_INSENSITIVE)
    };

    private static final Pattern[] TIMESTAMP_PATTERNS = {
            Pattern.compile("^[0-9:./\\-\\s]+$"),
            Pattern.compile("^\\d{1,2}:\\d{2}(\\s?[ap]m)?$"),
            Pattern.compile("^\\d+\\s?(sec|secs|min|mins|hr|hrs|hour|hours)\\b.*"),
            Pattern.compile("^(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)\\b.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^(today|yesterday)$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^(today|yesterday)\\s+\\d{1,2}:\\d{2}(\\s?(am|pm))?$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^(monday|tuesday|wednesday|thursday|friday|saturday|sunday)$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^(monday|tuesday|wednesday|thursday|friday|saturday|sunday)\\s+\\d{1,2}:\\d{2}(\\s?(am|pm))?$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]*\\s+\\d{1,2}(,\\s*)?(\\d{1,2}:\\d{2})?$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^(photo\\s+)?\\d+(\\.\\d+)?\\s*(kb|mb)$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^photo,\\s+date\\s+.*\\s+\\d{4}.*", Pattern.CASE_INSENSITIVE)
    };

    private static final Pattern[] PROFILE_METADATA_PATTERNS = {
            Pattern.compile("^joined\\s+\\w+\\s+\\d{4}$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^\\d+\\s+followers?\\s*[\\u00b7.]\\s*\\d+\\s+posts?$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^you\\s+follow\\s+each\\s+other\\s+on\\s+instagram$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^page\\s+\\d+\\s+of\\s+\\d+\\s+pages?$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*'s\\s+story,\\s+\\d+\\s+of\\s+\\d+,\\s+unseen\\.?.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\bposted\\b.*\\bin\\b.*\\d+\\s+(day|days|hour|hours|minute|minutes)\\s+ago.*", Pattern.CASE_INSENSITIVE)
    };

    private static final String[] UI_CONTAINS = {
            "voice note recorder",
            "group profile icon",
            "swipe down to reveal",
            "video call menu",
            "tap to add status",
            "message reactions",
            "forward image",
            "forward video",
            "started following you",
            "reacted to your message",
            "reacted to your story",
            "liked your photo",
            "liked your post",
            "liked your story",
            "sent you a friend request",
            "open navigation drawer",
            "close navigation drawer",
            "press and hold",
            "to record",
            "voice message, press and hold to record",
            "reels tray container",
            "media upload quality",
            "shutter, button. double tap",
            "ignore limit for today",
            "remind me in",
            "header that appears on a screen",
            "screenshot ui"
    };

    private static final String[] SYSTEM_NOTICE_CONTAINS = {
            "you're in sleep mode",
            "you are in sleep mode",
            "sleep mode",
            "consider closing",
            "consider closing instagram",
            "we'll notify you later",
            "we will notify you later",
            "about any messages you get",
            "message sent at",
            "messages will be deleted",
            "delete messages",
            "deleted for you",
            "delete for you",
            "tap and hold to super react",
            "see all emojis",
            "reaction",
            "instagram calling dropdown",
            "meta ai side chat",
            "search or ask meta ai",
            "you've been on",
            "you've reached your daily limit"
    };

    private UiNoiseFilter() {
    }

    public static NoiseResult evaluate(String normalizedText) {
        String normalized = normalizedText == null ? "" : normalizedText.trim().toLowerCase(Locale.US);
        if (normalized.isEmpty()) {
            return NoiseResult.notNoise();
        }

        if (STATUS_EXACT.contains(normalized)) {
            return NoiseResult.noise(NoiseType.STATUS_TEXT, "Known delivery/read/chat status text.");
        }
        for (Pattern pattern : STATUS_PATTERNS) {
            if (pattern.matcher(normalized).matches()) {
                return NoiseResult.noise(NoiseType.STATUS_TEXT, "Matched chat status pattern.");
            }
        }

        if (MEDIA_EXACT.contains(normalized)) {
            return NoiseResult.noise(NoiseType.MEDIA_TEXT, "Known media or attachment label.");
        }

        if (isKnownAppName(normalized)) {
            return NoiseResult.noise(NoiseType.UI_TEXT, "Known app name.");
        }

        if (UI_EXACT.contains(normalized)) {
            return NoiseResult.noise(NoiseType.UI_TEXT, "Known app chrome/navigation label.");
        }

        for (Pattern pattern : PROFILE_METADATA_PATTERNS) {
            if (pattern.matcher(normalized).matches()) {
                return NoiseResult.noise(NoiseType.PROFILE_METADATA, "Known profile metadata.");
            }
        }

        for (Pattern pattern : TIMESTAMP_PATTERNS) {
            if (pattern.matcher(normalized).matches()) {
                return NoiseResult.noise(NoiseType.TIMESTAMP_TEXT, "Timestamp/date metadata.");
            }
        }

        for (String fragment : UI_CONTAINS) {
            if (normalized.contains(fragment)) {
                return NoiseResult.noise(NoiseType.UI_TEXT, "Known UI description fragment.");
            }
        }

        for (String fragment : SYSTEM_NOTICE_CONTAINS) {
            if (normalized.contains(fragment)) {
                return NoiseResult.noise(NoiseType.UI_TEXT, "Known app/system notice.");
            }
        }

        return NoiseResult.notNoise();
    }

    private static boolean isKnownAppName(String normalized) {
        return "instagram".equals(normalized)
                || "whatsapp".equals(normalized)
                || "messenger".equals(normalized)
                || "telegram".equals(normalized)
                || "gmail".equals(normalized)
                || "tiktok".equals(normalized)
                || "tik tok".equals(normalized)
                || "facebook".equals(normalized);
    }

    public static final class NoiseResult {
        public final boolean noise;
        public final NoiseType type;
        public final String reason;

        private NoiseResult(boolean noise, NoiseType type, String reason) {
            this.noise = noise;
            this.type = type;
            this.reason = reason;
        }

        static NoiseResult noise(NoiseType type, String reason) {
            return new NoiseResult(true, type, reason);
        }

        static NoiseResult notNoise() {
            return new NoiseResult(false, NoiseType.NONE, "");
        }
    }
}
