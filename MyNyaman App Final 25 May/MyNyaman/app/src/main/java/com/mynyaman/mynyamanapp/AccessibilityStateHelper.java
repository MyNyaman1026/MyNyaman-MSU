package com.mynyaman.mynyamanapp;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityManager;

import java.util.List;

public final class AccessibilityStateHelper {

    private AccessibilityStateHelper() {
    }

    public static boolean isAppWatchServiceEnabled(Context context) {
        AccessibilityManager manager = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (manager != null) {
            List<AccessibilityServiceInfo> enabledServices = manager.getEnabledAccessibilityServiceList(
                    AccessibilityServiceInfo.FEEDBACK_ALL_MASK
            );
            String expectedId = new ComponentName(context, AppWatchAccessibilityService.class).flattenToString();
            for (AccessibilityServiceInfo info : enabledServices) {
                if (info != null && TextUtils.equals(expectedId, info.getId())) {
                    return true;
                }
            }
        }

        String enabledServicesSetting = Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        );
        String expectedId = new ComponentName(context, AppWatchAccessibilityService.class).flattenToString();
        return enabledServicesSetting != null && enabledServicesSetting.contains(expectedId);
    }
}
