package com.mynyaman.mynyamanapp;

import android.text.TextUtils;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public final class UsageWindowHelper {

    private static final String DAY_PATTERN = "yyyyMMdd";

    private UsageWindowHelper() {
    }

    public static String currentDayKey() {
        return formatDayKey(new Date());
    }

    public static String currentWeekStartKey() {
        return formatDayKey(startOfCurrentWeek());
    }

    public static Date startOfToday() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    public static Date startOfCurrentWeek() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    public static boolean isCurrentDay(DocumentSnapshot documentSnapshot) {
        if (documentSnapshot == null) {
            return false;
        }
        String usageDayKey = documentSnapshot.getString("usageDayKey");
        if (!TextUtils.isEmpty(usageDayKey)) {
            return TextUtils.equals(usageDayKey, currentDayKey());
        }
        Timestamp lastSynced = documentSnapshot.getTimestamp("usageLastSyncedAt");
        return isToday(lastSynced);
    }

    public static boolean isToday(Timestamp timestamp) {
        if (timestamp == null) {
            return false;
        }
        return formatDayKey(timestamp.toDate()).equals(currentDayKey());
    }

    public static long extractCurrentDayUsageSeconds(DocumentSnapshot documentSnapshot) {
        if (documentSnapshot == null || !isCurrentDay(documentSnapshot)) {
            return 0L;
        }

        long totalMillis = sumTopAppsMillis(documentSnapshot);
        if (totalMillis > 0L) {
            return totalMillis / 1000L;
        }

        Long usageSeconds = documentSnapshot.getLong("usageTodaySeconds");
        if (usageSeconds != null) {
            return usageSeconds;
        }

        Long usageMinutes = documentSnapshot.getLong("usageTodayMinutes");
        return usageMinutes == null ? 0L : usageMinutes * 60L;
    }

    @SuppressWarnings("unchecked")
    public static long sumTopAppsMillis(DocumentSnapshot documentSnapshot) {
        Object rawTopApps = documentSnapshot == null ? null : documentSnapshot.get("topApps");
        if (!(rawTopApps instanceof Iterable)) {
            return 0L;
        }

        long totalMillis = 0L;
        for (Object entry : (Iterable<Object>) rawTopApps) {
            if (!(entry instanceof Map)) {
                continue;
            }
            Object value = ((Map<?, ?>) entry).get("timeInForeground");
            if (value instanceof Number) {
                totalMillis += ((Number) value).longValue();
            }
        }
        return totalMillis;
    }

    public static String formatDayKey(Date date) {
        return new SimpleDateFormat(DAY_PATTERN, Locale.US).format(date);
    }
}
