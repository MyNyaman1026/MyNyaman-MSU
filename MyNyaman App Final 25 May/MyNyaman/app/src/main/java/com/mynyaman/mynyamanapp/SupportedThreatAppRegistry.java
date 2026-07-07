package com.mynyaman.mynyamanapp;

import android.content.Context;
import android.content.pm.PackageManager;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class SupportedThreatAppRegistry {

    private static final Map<String, String> EXACT_SUPPORTED_PACKAGES = new LinkedHashMap<>();
    private static final Set<String> PREFIX_SUPPORTED_PACKAGES = new LinkedHashSet<>();

    static {
        EXACT_SUPPORTED_PACKAGES.put("com.instagram.android", "Instagram");
        EXACT_SUPPORTED_PACKAGES.put("com.whatsapp", "WhatsApp");
        EXACT_SUPPORTED_PACKAGES.put("com.whatsapp.w4b", "WhatsApp Business");
        EXACT_SUPPORTED_PACKAGES.put("com.facebook.katana", "Facebook");
        EXACT_SUPPORTED_PACKAGES.put("com.facebook.orca", "Messenger");
        EXACT_SUPPORTED_PACKAGES.put("com.snapchat.android", "Snapchat");
        EXACT_SUPPORTED_PACKAGES.put("org.telegram.messenger", "Telegram");
        EXACT_SUPPORTED_PACKAGES.put("org.telegram.messenger.web", "Telegram");
        EXACT_SUPPORTED_PACKAGES.put("org.thunderdog.challegram", "Telegram X");
        EXACT_SUPPORTED_PACKAGES.put("com.zhiliaoapp.musically", "TikTok");
        EXACT_SUPPORTED_PACKAGES.put("com.zhiliaoapp.musically.go", "TikTok Lite");
        EXACT_SUPPORTED_PACKAGES.put("com.ss.android.ugc.trill", "TikTok");
        EXACT_SUPPORTED_PACKAGES.put("com.google.android.gm", "Gmail");
        EXACT_SUPPORTED_PACKAGES.put("com.microsoft.office.outlook", "Outlook");
        EXACT_SUPPORTED_PACKAGES.put("com.yahoo.mobile.client.android.mail", "Yahoo Mail");
        EXACT_SUPPORTED_PACKAGES.put("com.android.email", "Email");
        EXACT_SUPPORTED_PACKAGES.put("com.google.android.apps.messaging", "Messages");
        EXACT_SUPPORTED_PACKAGES.put("com.samsung.android.messaging", "Samsung Messages");
        EXACT_SUPPORTED_PACKAGES.put("com.android.mms", "Messaging");
        EXACT_SUPPORTED_PACKAGES.put("com.verizon.messaging.vzmsgs", "Message+");

        PREFIX_SUPPORTED_PACKAGES.add("com.instagram.");
        PREFIX_SUPPORTED_PACKAGES.add("com.whatsapp");
        PREFIX_SUPPORTED_PACKAGES.add("com.facebook.");
        PREFIX_SUPPORTED_PACKAGES.add("com.snapchat.");
        PREFIX_SUPPORTED_PACKAGES.add("org.telegram.");
        PREFIX_SUPPORTED_PACKAGES.add("com.zhiliaoapp.musically");
        PREFIX_SUPPORTED_PACKAGES.add("com.ss.android.ugc.trill");
        PREFIX_SUPPORTED_PACKAGES.add("com.google.android.gm");
        PREFIX_SUPPORTED_PACKAGES.add("com.google.android.apps.messaging");
        PREFIX_SUPPORTED_PACKAGES.add("com.microsoft.office.outlook");
        PREFIX_SUPPORTED_PACKAGES.add("com.yahoo.mobile.client.android.mail");
    }

    private SupportedThreatAppRegistry() {
    }

    public static boolean isSupportedPackage(String packageName) {
        String safePackage = normalize(packageName);
        if (TextUtils.isEmpty(safePackage)) {
            return false;
        }
        if (EXACT_SUPPORTED_PACKAGES.containsKey(safePackage)) {
            return true;
        }
        for (String prefix : PREFIX_SUPPORTED_PACKAGES) {
            if (safePackage.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    public static String resolveAppName(Context context, String packageName) {
        String safePackage = normalize(packageName);
        if (EXACT_SUPPORTED_PACKAGES.containsKey(safePackage)) {
            return EXACT_SUPPORTED_PACKAGES.get(safePackage);
        }

        try {
            PackageManager packageManager = context.getPackageManager();
            CharSequence label = packageManager.getApplicationLabel(
                    packageManager.getApplicationInfo(packageName, 0)
            );
            if (!TextUtils.isEmpty(label)) {
                return label.toString();
            }
        } catch (Exception ignored) {
        }

        return DistractionAppFilter.defaultDisplayName(null, packageName);
    }

    public static List<String> supportedAppLabels() {
        return new ArrayList<>(new LinkedHashSet<>(EXACT_SUPPORTED_PACKAGES.values()));
    }

    public static int supportedAppCount() {
        return supportedAppLabels().size();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.US);
    }
}
