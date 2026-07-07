package com.mynyaman.mynyamanapp;

import android.text.TextUtils;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.Locale;

public final class RoleUtils {

    public static final String ROLE_PARENT = "parent";
    public static final String ROLE_CHILD = "child";
    public static final String ROLE_CONSULTANT = "consultant";

    private RoleUtils() {
    }

    public static String normalizeRole(String role) {
        if (TextUtils.isEmpty(role)) {
            return "";
        }
        String normalized = role.trim().toLowerCase(Locale.US);
        if (normalized.equals("user")) {
            return ROLE_PARENT;
        }
        return normalized;
    }

    public static String toLegacyRoleLabel(String role) {
        String normalized = normalizeRole(role);
        switch (normalized) {
            case ROLE_PARENT:
                return "Parent";
            case ROLE_CHILD:
                return "Child";
            case ROLE_CONSULTANT:
                return "Consultant";
            default:
                return role == null ? "" : role;
        }
    }

    public static String readNormalizedRole(DocumentSnapshot documentSnapshot) {
        if (documentSnapshot == null) {
            return "";
        }
        String role = documentSnapshot.getString("role");
        if (TextUtils.isEmpty(role)) {
            role = documentSnapshot.getString("roleKey");
        }
        return normalizeRole(role);
    }
}
