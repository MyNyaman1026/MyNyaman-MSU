package com.mynyaman.mynyamanapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.text.TextUtils;

import androidx.core.app.NotificationCompat;

public final class ThreatNotificationHelper {

    private static final String CHANNEL_CHILD = "mynyaman_threat_child";
    private static final String CHANNEL_PARENT = "mynyaman_threat_parent";
    private static final String CHANNEL_PARENT_MONITOR = "mynyaman_threat_parent_monitor";
    private static final String CHANNEL_THREAT_ALERTS = "threat_alerts_channel";

    private static final int PASTEL_GREEN = Color.rgb(230, 244, 234);
    private static final int PASTEL_YELLOW = Color.rgb(255, 244, 204);
    private static final int PASTEL_RED = Color.rgb(255, 225, 225);

    private ThreatNotificationHelper() {
    }

    public static void showChildAlert(
            Context context,
            String appName,
            ThreatResult result
    ) {
        if (context == null || result == null || !ThreatMonitoringPermissionHelper.hasNotificationPermission(context)) {
            return;
        }

        createChannels(context);
        String title = "Harmful text detected";
        String content = "A harmful message was detected. Please stay safe online.";

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) {
            return;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_THREAT_ALERTS)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setColor(resolvePastelColor(result.finalLevel))
                .setColorized(true)
                .setAutoCancel(true)
                .setContentIntent(buildThreatActivityIntent(context))
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE);
        manager.notify((int) (System.currentTimeMillis() % Integer.MAX_VALUE), builder.build());
    }

    public static boolean showParentAlert(
            Context context,
            String requestId,
            String childUid,
            String childName,
            String appName,
            String packageName,
            String maskedPreview,
            ThreatResult.Level level,
            String detectedAtLabel
    ) {
        if (context == null || !ThreatMonitoringPermissionHelper.hasNotificationPermission(context)) {
            return false;
        }

        createChannels(context);
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) {
            return false;
        }

        String title;
        String contentText;
        if (level == ThreatResult.Level.RED) {
            String childLabel = TextUtils.isEmpty(childName) ? "your child" : childName;
            String appLabel = TextUtils.isEmpty(appName) ? "the app" : appName;
            title = "🚨 Cyberbullying Alert — App Locked";
            contentText = childLabel + "'s " + appLabel
                    + " has been locked. A serious threat was detected. Open MyNyaman to review and unlock.";
        } else {
            title = TextUtils.isEmpty(childName)
                    ? "Your child might be in danger"
                    : "Your " + childName + " might be in danger";
            String sourceLabel = TextUtils.isEmpty(appName) ? "a supported app" : appName;
            contentText = "Harmful text was detected from " + sourceLabel + ". Tap to review the alert.";
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_PARENT)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(title)
                .setContentText(contentText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(contentText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setColor(resolvePastelColor(level))
                .setColorized(true)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setContentIntent(buildThreatActivityIntent(context, requestId, childUid, appName, packageName));

        manager.notify(stableNotificationId(requestId), builder.build());
        return true;
    }

    public static boolean showParentReportAlert(
            Context context,
            String reportId,
            String childUid,
            String childName,
            String reporterDisplayName
    ) {
        if (context == null || !ThreatMonitoringPermissionHelper.hasNotificationPermission(context)) {
            return false;
        }

        createChannels(context);
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) {
            return false;
        }

        String childLabel = TextUtils.isEmpty(childName)
                ? (TextUtils.isEmpty(reporterDisplayName) ? "your child" : reporterDisplayName)
                : childName;
        String title = "New report from " + childLabel;
        String contentText = childLabel + " lodged a safety report. Tap to review it in MyNyaman.";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_PARENT)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(contentText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(contentText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setColor(PASTEL_YELLOW)
                .setColorized(true)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setContentIntent(buildReportListIntent(context, reportId, childUid, childName));

        manager.notify(stableNotificationId("report_" + reportId), builder.build());
        return true;
    }

    public static NotificationCompat.Builder buildParentMonitorNotification(Context context, String contentText) {
        createChannels(context);
        return new NotificationCompat.Builder(context, CHANNEL_PARENT_MONITOR)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle("MyNyaman threat alerts active")
                .setContentText(contentText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(contentText))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setColor(PASTEL_GREEN)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setContentIntent(buildThreatActivityIntent(context));
    }

    public static int resolvePastelColor(ThreatResult.Level level) {
        if (level == ThreatResult.Level.RED) {
            return PASTEL_RED;
        }
        if (level == ThreatResult.Level.YELLOW) {
            return PASTEL_YELLOW;
        }
        return PASTEL_GREEN;
    }

    public static int resolvePastelColor(String level) {
        if ("RED".equalsIgnoreCase(level) || "Red".equalsIgnoreCase(level)) {
            return PASTEL_RED;
        }
        if ("YELLOW".equalsIgnoreCase(level) || "Yellow".equalsIgnoreCase(level)) {
            return PASTEL_YELLOW;
        }
        return PASTEL_GREEN;
    }

    private static PendingIntent buildThreatActivityIntent(Context context) {
        return buildThreatActivityIntent(context, "", "", "", "");
    }

    private static PendingIntent buildThreatActivityIntent(
            Context context,
            String requestId,
            String childUid,
            String appName,
            String packageName
    ) {
        Intent intent = new Intent(context, AiThreatDetectionActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (!TextUtils.isEmpty(requestId)) {
            intent.putExtra("requestId", requestId);
        }
        if (!TextUtils.isEmpty(childUid)) {
            intent.putExtra("childUid", childUid);
        }
        if (!TextUtils.isEmpty(appName)) {
            intent.putExtra("appName", appName);
        }
        if (!TextUtils.isEmpty(packageName)) {
            intent.putExtra("packageName", packageName);
        }
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getActivity(context, stableNotificationId(requestId), intent, flags);
    }

    private static PendingIntent buildReportListIntent(
            Context context,
            String reportId,
            String childUid,
            String childName
    ) {
        Intent intent = ThreatReportListActivity.createIntent(context, RoleUtils.ROLE_PARENT, childUid, childName);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (!TextUtils.isEmpty(reportId)) {
            intent.putExtra("reportId", reportId);
        }
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getActivity(context, stableNotificationId("report_" + reportId), intent, flags);
    }

    private static int stableNotificationId(String requestId) {
        if (TextUtils.isEmpty(requestId)) {
            return (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
        }
        long hash = requestId.hashCode();
        return 100000 + (int) (Math.abs(hash) % 1000000000L);
    }

    private static void createChannels(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) {
            return;
        }

        NotificationChannel childChannel = new NotificationChannel(
                CHANNEL_CHILD,
                "Child Threat Alerts",
                NotificationManager.IMPORTANCE_HIGH
        );
        childChannel.setDescription("Warns the child when harmful content is detected.");

        NotificationChannel parentChannel = new NotificationChannel(
                CHANNEL_PARENT,
                "Parent Threat Alerts",
                NotificationManager.IMPORTANCE_HIGH
        );
        parentChannel.setDescription("Masked alerts for parent threat detection events.");

        NotificationChannel monitorChannel = new NotificationChannel(
                CHANNEL_PARENT_MONITOR,
                "Parent Alert Monitor",
                NotificationManager.IMPORTANCE_LOW
        );
        monitorChannel.setDescription("Keeps parent threat alert monitoring active in the background.");

        NotificationChannel threatAlertsChannel = new NotificationChannel(
                CHANNEL_THREAT_ALERTS,
                "Threat Alerts",
                NotificationManager.IMPORTANCE_HIGH
        );
        threatAlertsChannel.setDescription("High-priority child and parent threat alerts.");

        manager.createNotificationChannel(childChannel);
        manager.createNotificationChannel(parentChannel);
        manager.createNotificationChannel(monitorChannel);
        manager.createNotificationChannel(threatAlertsChannel);
    }
}
