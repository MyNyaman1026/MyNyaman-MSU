package com.mynyaman.mynyamanapp;

import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Process;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class UsageStatsHelper {

    public static final class AppUsageRecord {
        public final String appName;
        public final String packageName;
        public final long timeInForegroundMillis;
        public int percentage;

        AppUsageRecord(String appName, String packageName, long timeInForegroundMillis) {
            this.appName = appName;
            this.packageName = packageName;
            this.timeInForegroundMillis = timeInForegroundMillis;
        }
    }

    private UsageStatsHelper() {
    }

    public static boolean hasUsageStatsPermission(Context context) {
        AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        if (appOps == null) {
            return false;
        }

        int mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.getPackageName()
        );
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    public static long getTodayUsageMillis(Context context) {
        long totalTimeMs = 0L;
        for (AppUsageRecord record : getTodayUsageRecords(context)) {
            totalTimeMs += record.timeInForegroundMillis;
        }
        return totalTimeMs;
    }

    public static long getTodayPackageUsageMillis(Context context, String packageName) {
        if (!hasUsageStatsPermission(context) || TextUtils.isEmpty(packageName)) {
            return 0L;
        }

        UsageStatsManager usageStatsManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        if (usageStatsManager == null) {
            return 0L;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        Map<String, UsageStats> usageStatsMap = usageStatsManager.queryAndAggregateUsageStats(
                calendar.getTimeInMillis(),
                System.currentTimeMillis()
        );
        if (usageStatsMap == null || !usageStatsMap.containsKey(packageName)) {
            return 0L;
        }
        return usageStatsMap.get(packageName).getTotalTimeInForeground();
    }

    public static long getTodayUsageMinutes(Context context) {
        return TimeUnit.MILLISECONDS.toMinutes(getTodayUsageMillis(context));
    }

    public static long getTodayUsageSeconds(Context context) {
        return TimeUnit.MILLISECONDS.toSeconds(getTodayUsageMillis(context));
    }

    public static List<AppUsageRecord> getTodayUsageRecords(Context context) {
        UsageStatsManager usageStatsManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        if (usageStatsManager == null) {
            return Collections.emptyList();
        }

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        long startTime = calendar.getTimeInMillis();
        long endTime = System.currentTimeMillis();
        Map<String, UsageStats> usageStatsMap = usageStatsManager.queryAndAggregateUsageStats(startTime, endTime);
        if (usageStatsMap == null || usageStatsMap.isEmpty()) {
            return Collections.emptyList();
        }

        PackageManager packageManager = context.getPackageManager();
        List<AppUsageRecord> records = new ArrayList<>();
        long totalTimeMs = 0L;

        for (Map.Entry<String, UsageStats> entry : usageStatsMap.entrySet()) {
            UsageStats stat = entry.getValue();
            long timeInForeground = stat.getTotalTimeInForeground();
            if (timeInForeground <= 1000L) {
                continue;
            }

            String packageName = stat.getPackageName();
            String appName = resolveAppName(packageManager, packageName);
            records.add(new AppUsageRecord(appName, packageName, timeInForeground));
            totalTimeMs += timeInForeground;
        }

        records.sort(Comparator.comparingLong((AppUsageRecord record) -> record.timeInForegroundMillis).reversed());
        for (AppUsageRecord record : records) {
            record.percentage = totalTimeMs > 0L ? (int) ((record.timeInForegroundMillis * 100L) / totalTimeMs) : 0;
        }
        return records;
    }

    public static List<Map<String, Object>> toTopAppsPayload(List<AppUsageRecord> records) {
        List<Map<String, Object>> payload = new ArrayList<>();
        for (AppUsageRecord record : records) {
            Map<String, Object> item = new HashMap<>();
            item.put("appName", record.appName);
            item.put("packageName", record.packageName);
            item.put("timeInForeground", record.timeInForegroundMillis);
            item.put("percentage", record.percentage);
            payload.add(item);
        }
        return payload;
    }

    private static String resolveAppName(PackageManager packageManager, String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            return "Unknown App";
        }

        try {
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
            CharSequence label = packageManager.getApplicationLabel(applicationInfo);
            if (!TextUtils.isEmpty(label)) {
                return label.toString();
            }
        } catch (PackageManager.NameNotFoundException ignored) {
        }

        return packageName;
    }

    public static String formatMinutes(long minutes) {
        return formatDurationFromSeconds(TimeUnit.MINUTES.toSeconds(minutes));
    }

    public static String formatDurationFromMillis(long millis) {
        return formatDurationFromSeconds(TimeUnit.MILLISECONDS.toSeconds(millis));
    }

    public static String formatDurationFromSeconds(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long remainderSeconds = seconds % 60;
        return String.format(
                Locale.getDefault(),
                "%02d hrs %02d mins %02d secs",
                hours,
                minutes,
                remainderSeconds
        );
    }
}
