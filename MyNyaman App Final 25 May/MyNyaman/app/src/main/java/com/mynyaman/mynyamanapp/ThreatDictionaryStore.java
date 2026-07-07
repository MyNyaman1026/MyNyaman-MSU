package com.mynyaman.mynyamanapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class ThreatDictionaryStore {

    private static final String PREFS_NAME = "threat_dictionary_store";
    private static final String KEY_EXTRA_MILD = "extra_mild_terms";
    private static final String KEY_EXTRA_SEVERE = "extra_severe_terms";
    private static final String KEY_EXTRA_EXPLICIT = "extra_explicit_terms";

    private ThreatDictionaryStore() {
    }

    public static Set<String> getMergedMildTerms(Context context, Set<String> defaults) {
        return merge(defaults, readTerms(context, KEY_EXTRA_MILD));
    }

    public static Set<String> getMergedSevereTerms(Context context, Set<String> defaults) {
        return merge(defaults, readTerms(context, KEY_EXTRA_SEVERE));
    }

    public static Set<String> getMergedExplicitTerms(Context context, Set<String> defaults) {
        return merge(defaults, readTerms(context, KEY_EXTRA_EXPLICIT));
    }

    public static void addMildTerm(Context context, String term) {
        addTerm(context, KEY_EXTRA_MILD, term);
    }

    public static void addSevereTerm(Context context, String term) {
        addTerm(context, KEY_EXTRA_SEVERE, term);
    }

    public static void addExplicitTerm(Context context, String term) {
        addTerm(context, KEY_EXTRA_EXPLICIT, term);
    }

    private static void addTerm(Context context, String key, String term) {
        String clean = sanitize(term);
        if (TextUtils.isEmpty(clean)) {
            return;
        }

        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> current = new LinkedHashSet<>(preferences.getStringSet(key, Collections.emptySet()));
        current.add(clean);
        preferences.edit().putStringSet(key, current).apply();
    }

    private static Set<String> readTerms(Context context, String key) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return new LinkedHashSet<>(preferences.getStringSet(key, Collections.emptySet()));
    }

    private static Set<String> merge(Set<String> defaults, Set<String> extras) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        if (defaults != null) {
            for (String item : defaults) {
                String clean = sanitize(item);
                if (!TextUtils.isEmpty(clean)) {
                    merged.add(clean);
                }
            }
        }
        if (extras != null) {
            for (String item : extras) {
                String clean = sanitize(item);
                if (!TextUtils.isEmpty(clean)) {
                    merged.add(clean);
                }
            }
        }
        return merged;
    }

    private static String sanitize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}
