package com.mynyaman.mynyamanapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;

import androidx.core.content.ContextCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ThreatMonitoringPermissionHelper {

    private ThreatMonitoringPermissionHelper() {
    }

    public static boolean hasNotificationPermission(Context context) {
        if (context == null) {
            return false;
        }
        boolean runtimeGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
        return runtimeGranted && NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    public static boolean hasUsagePermission(Context context) {
        return UsageStatsHelper.hasUsageStatsPermission(context);
    }

    public static boolean hasAccessibilityPermission(Context context) {
        return AccessibilityStateHelper.isAppWatchServiceEnabled(context);
    }

    public static boolean hasOverlayPermission(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context);
    }

    public static boolean isIgnoringBatteryOptimizations(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        return powerManager != null && powerManager.isIgnoringBatteryOptimizations(context.getPackageName());
    }

    public static boolean isThreatMonitoringReady(Context context) {
        return hasAccessibilityPermission(context)
                && hasNotificationPermission(context)
                && hasUsagePermission(context)
                && hasOverlayPermission(context)
                && isIgnoringBatteryOptimizations(context);
    }

    public static boolean hasAnyRequiredSetupMissing(Context context) {
        return !hasAccessibilityPermission(context)
                || !hasNotificationPermission(context)
                || !hasUsagePermission(context)
                || !hasOverlayPermission(context)
                || !isIgnoringBatteryOptimizations(context);
    }

    public static String buildLocalStatusLabel(Context context) {
        if (isThreatMonitoringReady(context)) {
            return "Monitoring Active";
        }
        if (!hasAccessibilityPermission(context)) {
            return "Needs Accessibility Setup";
        }
        if (!hasNotificationPermission(context)) {
            return "Needs Alert Setup";
        }
        if (!hasUsagePermission(context)) {
            return "Needs Usage Access";
        }
        if (!hasOverlayPermission(context)) {
            return "Needs Overlay Setup";
        }
        if (!isIgnoringBatteryOptimizations(context)) {
            return "Needs Battery Setup";
        }
        return "Needs Setup";
    }

    public static Map<String, Object> buildStatusPayload(Context context) {
        Map<String, Object> data = new HashMap<>();
        data.put("threatMonitoringEnabled", isThreatMonitoringReady(context));
        data.put("threatPermissionAccessibility", hasAccessibilityPermission(context));
        data.put("threatPermissionNotifications", hasNotificationPermission(context));
        data.put("threatPermissionImages", false);
        data.put("threatGalleryImageScanningEnabled", false);
        data.put("threatEvidenceImagePickerAvailable", true);
        data.put("threatPermissionUsageAccess", hasUsagePermission(context));
        data.put("threatPermissionOverlay", hasOverlayPermission(context));
        data.put("threatBatteryOptimizationIgnored", isIgnoringBatteryOptimizations(context));
        data.put("threatSupportedApps", SupportedThreatAppRegistry.supportedAppLabels());
        data.put("threatSupportedAppCount", SupportedThreatAppRegistry.supportedAppCount());
        data.put("threatMonitoringStatusLabel", buildLocalStatusLabel(context));
        return data;
    }

    public static Intent newAccessibilityIntent() {
        return new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
    }

    public static Intent newUsageAccessIntent() {
        return new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
    }

    public static Intent newOverlayIntent(Context context) {
        Intent intent = new Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + context.getPackageName())
        );
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    public static Intent newBatteryOptimizationIntent(Context context) {
        Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
        intent.setData(Uri.parse("package:" + context.getPackageName()));
        return intent;
    }

    public static Intent newNotificationSettingsIntent(Context context) {
        Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
        intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName());
        intent.putExtra("android.provider.extra.APP_PACKAGE", context.getPackageName());
        return intent;
    }

    public static Intent newAppDetailsIntent(Context context) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + context.getPackageName()));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    public static Intent newGeneralSettingsIntent() {
        return new Intent(Settings.ACTION_SETTINGS);
    }

    public static String statusText(boolean enabled) {
        return enabled ? "Enabled" : "Disabled";
    }

    public static String supportedAppsText() {
        List<String> labels = SupportedThreatAppRegistry.supportedAppLabels();
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < labels.size(); index++) {
            if (index > 0) {
                builder.append(", ");
            }
            builder.append(labels.get(index));
        }
        return builder.toString();
    }
}
