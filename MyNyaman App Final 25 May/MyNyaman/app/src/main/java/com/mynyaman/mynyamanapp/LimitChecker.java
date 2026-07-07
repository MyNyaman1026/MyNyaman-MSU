package com.mynyaman.mynyamanapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.Map;

public class LimitChecker {

    public static void checkLimits(Context context) {
        // Check for Notification Permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return; // Missing permission, silently fail
            }
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        // Fetch limits from Firebase
        FirebaseFirestore.getInstance().collection("users").document(user.getUid())
                .get().addOnSuccessListener(documentSnapshot -> {
                    Map<String, Long> appLimits = (Map<String, Long>) documentSnapshot.get("appLimits");
                    if (appLimits == null || appLimits.isEmpty()) return;

                    // Get today's usage from Android System
                    UsageStatsManager mUsageStatsManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
                    Calendar cal = Calendar.getInstance();
                    cal.set(Calendar.HOUR_OF_DAY, 0);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);

                    Map<String, UsageStats> statsMap = mUsageStatsManager.queryAndAggregateUsageStats(cal.getTimeInMillis(), System.currentTimeMillis());

                    // Compare limits vs usage
                    for (Map.Entry<String, Long> limitEntry : appLimits.entrySet()) {
                        String packageName = limitEntry.getKey();
                        long limitInMinutes = limitEntry.getValue();

                        if (statsMap.containsKey(packageName)) {
                            long timeInForegroundMs = statsMap.get(packageName).getTotalTimeInForeground();
                            long usedMinutes = timeInForegroundMs / (1000 * 60);

                            if (usedMinutes >= limitInMinutes) {
                                sendNotification(context, packageName, limitInMinutes);
                            }
                        }
                    }
                });
    }

    private static void sendNotification(Context context, String packageName, long limitMinutes) {
        String channelId = "AppLimitsChannel";
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "App Limits", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        String appName = packageName; // Fallback
        try {
            PackageManager pm = context.getPackageManager();
            appName = pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString();
        } catch (Exception ignored) {}

        Intent intent = new Intent(context, Childdash.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("Screen Time Limit Reached!")
                .setContentText("You have used up your " + limitMinutes + " mins for " + appName + ".")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        // Use a unique ID based on package name hash so multiple apps can send notifications
        notificationManager.notify(packageName.hashCode(), builder.build());
    }
}