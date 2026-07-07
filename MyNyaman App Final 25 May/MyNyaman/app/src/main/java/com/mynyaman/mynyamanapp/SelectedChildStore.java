package com.mynyaman.mynyamanapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.util.List;

public final class SelectedChildStore {

    private static final String PREFS_NAME = "parent_dash_prefs";
    private static final String PREF_SELECTED_CHILD_UID = "selected_child_uid";

    private SelectedChildStore() {
    }

    public static String getSelectedChildUid(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(PREF_SELECTED_CHILD_UID, null);
    }

    public static void saveSelectedChildUid(Context context, String childUid) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(PREF_SELECTED_CHILD_UID, childUid)
                .apply();
    }

    public static String resolveSelectedChildUid(Context context, List<String> availableChildUids) {
        if (availableChildUids == null || availableChildUids.isEmpty()) {
            return null;
        }

        String storedChildUid = getSelectedChildUid(context);
        if (!TextUtils.isEmpty(storedChildUid) && availableChildUids.contains(storedChildUid)) {
            return storedChildUid;
        }

        String fallbackChildUid = availableChildUids.get(0);
        saveSelectedChildUid(context, fallbackChildUid);
        return fallbackChildUid;
    }
}
