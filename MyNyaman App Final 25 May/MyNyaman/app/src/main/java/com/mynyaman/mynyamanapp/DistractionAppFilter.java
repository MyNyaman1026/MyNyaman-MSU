package com.mynyaman.mynyamanapp;

import android.text.TextUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class DistractionAppFilter {

    private static final Set<String> PACKAGE_KEYWORDS = new HashSet<>(Arrays.asList(
            "instagram",
            "whatsapp",
            "facebook",
            "messenger",
            "tiktok",
            "musically",
            "snapchat",
            "telegram",
            "discord",
            "spotify",
            "youtube",
            "netflix",
            "chatgpt",
            "openai",
            "roblox",
            "pubg",
            "freefire",
            "mobilelegends",
            "mlbb",
            "genshin",
            "brawlstars",
            "candycrush",
            "twitter",
            "x.com",
            "twitch"
    ));

    private static final Set<String> NAME_KEYWORDS = new HashSet<>(Arrays.asList(
            "instagram",
            "whatsapp",
            "facebook",
            "messenger",
            "tiktok",
            "snapchat",
            "telegram",
            "discord",
            "spotify",
            "chatgpt",
            "youtube",
            "netflix",
            "roblox",
            "pubg",
            "free fire",
            "mobile legends",
            "genshin",
            "game",
            "games"
    ));

    private DistractionAppFilter() {
    }

    public static boolean isHighDistractionApp(String appName, String packageName) {
        String safeName = normalize(appName);
        String safePackage = normalize(packageName);

        for (String keyword : NAME_KEYWORDS) {
            if (safeName.contains(keyword)) {
                return true;
            }
        }

        for (String keyword : PACKAGE_KEYWORDS) {
            if (safePackage.contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    public static String defaultDisplayName(String appName, String packageName) {
        if (!TextUtils.isEmpty(appName)) {
            return appName;
        }
        if (TextUtils.isEmpty(packageName)) {
            return "App";
        }

        String[] parts = packageName.split("\\.");
        String lastPart = parts.length == 0 ? packageName : parts[parts.length - 1];
        String withSpaces = lastPart.replace('_', ' ').replace('-', ' ');
        if (withSpaces.isEmpty()) {
            return packageName;
        }
        return Character.toUpperCase(withSpaces.charAt(0)) + withSpaces.substring(1);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.US).trim();
    }
}
