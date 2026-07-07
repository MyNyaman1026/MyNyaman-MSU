package com.mynyaman.mynyamanapp;

import android.util.Log;

import java.util.ArrayDeque;
import java.util.Deque;

public final class GroqRateLimiter {

    private static final int MAX_CALLS_PER_WINDOW = 25;
    private static final long WINDOW_MS = 60_000L;

    private static final Deque<Long> CALL_TIMESTAMPS = new ArrayDeque<>();
    private static final Object LOCK = new Object();

    private GroqRateLimiter() {
    }

    public static boolean tryAcquire() {
        long now = System.currentTimeMillis();
        synchronized (LOCK) {
            while (!CALL_TIMESTAMPS.isEmpty() && now - CALL_TIMESTAMPS.peekFirst() >= WINDOW_MS) {
                CALL_TIMESTAMPS.pollFirst();
            }
            if (CALL_TIMESTAMPS.size() >= MAX_CALLS_PER_WINDOW) {
                Log.w("GROQ_SKIPPED_RATE_LIMIT", "Groq rate limit hit: "
                        + CALL_TIMESTAMPS.size() + "/" + MAX_CALLS_PER_WINDOW
                        + " in the last " + WINDOW_MS + "ms. Local fallback will be used.");
                return false;
            }
            CALL_TIMESTAMPS.addLast(now);
            return true;
        }
    }
}
