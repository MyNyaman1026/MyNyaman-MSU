package com.mynyaman.mynyamanapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

public final class ForegroundAppStateStore {

    private static final String PREFS_NAME = "foreground_app_state";
    private static final String KEY_PACKAGE_NAME = "package_name";
    private static final String KEY_ENTER_UPTIME_MS = "enter_uptime_ms";
    private static final String KEY_BASELINE_USAGE_MS = "baseline_usage_ms";

    private ForegroundAppStateStore() {
    }

    public static void saveSession(Context context, String packageName, long enterUptimeMs, long baselineUsageMs) {
        if (TextUtils.isEmpty(packageName)) {
            clear(context);
            return;
        }

        preferences(context)
                .edit()
                .putString(KEY_PACKAGE_NAME, packageName)
                .putLong(KEY_ENTER_UPTIME_MS, enterUptimeMs)
                .putLong(KEY_BASELINE_USAGE_MS, baselineUsageMs)
                .apply();
    }

    public static ForegroundSession readSession(Context context) {
        SharedPreferences prefs = preferences(context);
        String packageName = prefs.getString(KEY_PACKAGE_NAME, null);
        long enterUptimeMs = prefs.getLong(KEY_ENTER_UPTIME_MS, -1L);
        long baselineUsageMs = prefs.getLong(KEY_BASELINE_USAGE_MS, 0L);
        if (TextUtils.isEmpty(packageName) || enterUptimeMs < 0L) {
            return null;
        }
        return new ForegroundSession(packageName, enterUptimeMs, baselineUsageMs);
    }

    public static void clear(Context context) {
        preferences(context)
                .edit()
                .remove(KEY_PACKAGE_NAME)
                .remove(KEY_ENTER_UPTIME_MS)
                .remove(KEY_BASELINE_USAGE_MS)
                .apply();
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static final class ForegroundSession {
        public final String packageName;
        public final long enterUptimeMs;
        public final long baselineUsageMs;

        ForegroundSession(String packageName, long enterUptimeMs, long baselineUsageMs) {
            this.packageName = packageName;
            this.enterUptimeMs = enterUptimeMs;
            this.baselineUsageMs = baselineUsageMs;
        }
    }
}
