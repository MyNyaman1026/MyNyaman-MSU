package com.mynyaman.mynyamanapp;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class AppWatchAccessibilityService extends AccessibilityService {

    private static final String TAG = "AppWatch";

    // Debounce: wait 2.5 seconds of inactivity before sending text to remote AI review.
    private static final long DEBOUNCE_DELAY_MS = 2500L;
    private static final int MAX_CAPTURED_TEXT_COUNT = 16;
    private static final int MAX_TRAVERSED_NODES = 30;
    private static final int MAX_FORWARD_EVENTS_PER_ACCESSIBILITY_EVENT = 4;

    private String lastReportedPackage;
    private long foregroundSessionId = 0L;

    // Debounce state: accumulate text events and forward after a typing pause.
    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private final ThreatTextPipeline threatTextPipeline = new ThreatTextPipeline();
    private ForwardedTextEvent pendingTextEvent;

    private final Runnable debouncedForwardRunnable = () -> {
        ForwardedTextEvent event = pendingTextEvent;
        pendingTextEvent = null;
        if (event != null) {
            ThreatTextPipeline.SubmissionResult submissionResult =
                    threatTextPipeline.beforeSubmit(event.text, SystemClock.elapsedRealtime());
            if (!submissionResult.shouldSubmit) {
                Log.d("ThreatPipeline", submissionResult.logCode + ": \"" + event.text
                        + "\" (" + submissionResult.reason + ")");
                return;
            }

            Log.d("ThreatPipeline", submissionResult.logCode + ": \"" + event.text + "\"");
            Log.d(TAG, "Debounce fired - forwarding text from " + event.appName
                    + " (" + event.text.length() + " chars)");
            forwardThreatText(event);
        }
    };

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) {
            return;
        }

        CharSequence packageNameValue = event.getPackageName();
        if (packageNameValue == null) {
            return;
        }

        String packageName = packageNameValue.toString();
        if (TextUtils.isEmpty(packageName)) {
            return;
        }

        int eventType = event.getEventType();
        if (isForegroundWindowEvent(eventType)) {
            reportForegroundPackage(packageName, eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
        }

        if (!SupportedThreatAppRegistry.isSupportedPackage(packageName) || !isThreatTextEvent(eventType)) {
            return;
        }

        List<ForwardedTextEvent> forwardedTextEvents = extractRelevantTextEvents(event, packageName);
        if (forwardedTextEvents.isEmpty()) {
            return;
        }

        for (ForwardedTextEvent forwardedTextEvent : forwardedTextEvents) {
            enqueueForwardedText(forwardedTextEvent);
        }
    }

    private void enqueueForwardedText(ForwardedTextEvent forwardedTextEvent) {
        if (shouldReplacePendingText(forwardedTextEvent)) {
            Log.d("ThreatPipeline", "1. Text extracted: " + forwardedTextEvent.text);
            if (pendingTextEvent != null) {
                Log.d("ThreatPipeline", "PENDING_REPLACED_BY_HIGHER_PRIORITY_MESSAGE: oldRequestId="
                        + pendingTextEvent.requestId
                        + ", newRequestId=" + forwardedTextEvent.requestId);
            }
            flushDifferentPendingText(forwardedTextEvent);
            pendingTextEvent = forwardedTextEvent;
            debounceHandler.removeCallbacks(debouncedForwardRunnable);
            debounceHandler.postDelayed(debouncedForwardRunnable, DEBOUNCE_DELAY_MS);
        } else {
            Log.d("ThreatPipeline", "PENDING_IGNORED_LOWER_PRIORITY_CANDIDATE: \"" + forwardedTextEvent.text
                    + "\" (Keeping pending \"" + pendingTextEvent.text + "\")");
        }
    }

    private void flushDifferentPendingText(ForwardedTextEvent newEvent) {
        if (pendingTextEvent == null || newEvent == null || TextUtils.equals(pendingTextEvent.text, newEvent.text)) {
            return;
        }
        debounceHandler.removeCallbacks(debouncedForwardRunnable);
        Log.d("ThreatPipeline", "PENDING_SUBMITTED_WITH_ORIGINAL_CONTEXT: requestId="
                + pendingTextEvent.requestId
                + ", app=" + pendingTextEvent.appName
                + ", before=\"" + newEvent.text + "\"");
        boolean submitted = submitForwardedText(pendingTextEvent);
        if (!submitted) {
            Log.d("ThreatPipeline", "PENDING_DROPPED_ON_APP_SWITCH: requestId="
                    + pendingTextEvent.requestId
                    + ", app=" + pendingTextEvent.appName);
        }
        pendingTextEvent = null;
    }

    @Override
    public void onInterrupt() {
    }

    private boolean isForegroundWindowEvent(int eventType) {
        return eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                || eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED;
    }

    private boolean isThreatTextEvent(int eventType) {
        return eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
                || eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                || eventType == AccessibilityEvent.TYPE_VIEW_CLICKED
                || eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED
                || eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                || eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED
                || eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED;
    }

    private void reportForegroundPackage(String packageName, boolean forceNewSession) {
        if (TextUtils.equals(packageName, lastReportedPackage) && !forceNewSession) {
            return;
        }

        foregroundSessionId++;
        Log.d("ThreatPipeline", "APP_CONTEXT_SWITCHED: from="
                + (TextUtils.isEmpty(lastReportedPackage) ? "none" : lastReportedPackage)
                + ", to=" + packageName
                + ", sessionId=" + foregroundSessionId);

        // Flush any pending debounced text from the previous app immediately.
        flushPendingText();

        lastReportedPackage = packageName;
        long enterUptimeMs = SystemClock.uptimeMillis();
        long baselineUsageMs = UsageStatsHelper.getTodayPackageUsageMillis(this, packageName);
        ForegroundAppStateStore.saveSession(this, packageName, enterUptimeMs, baselineUsageMs);

        Intent serviceIntent = new Intent(this, LimitService.class);
        serviceIntent.setAction(LimitService.ACTION_FOREGROUND_APP_CHANGED);
        serviceIntent.putExtra(LimitService.EXTRA_PACKAGE_NAME, packageName);
        serviceIntent.putExtra(LimitService.EXTRA_ENTER_UPTIME_MS, enterUptimeMs);
        serviceIntent.putExtra(LimitService.EXTRA_BASELINE_USAGE_MS, baselineUsageMs);
        startLimitService(serviceIntent);
    }

    /**
     * Flush any debounced text event immediately (e.g. when the user switches apps).
     */
    private void flushPendingText() {
        if (pendingTextEvent != null) {
            debounceHandler.removeCallbacks(debouncedForwardRunnable);
            Log.d("ThreatPipeline", "PENDING_SUBMITTED_WITH_ORIGINAL_CONTEXT: requestId="
                    + pendingTextEvent.requestId
                    + ", app=" + pendingTextEvent.appName
                    + ", sessionId=" + pendingTextEvent.appSessionId);
            boolean submitted = submitForwardedText(pendingTextEvent);
            if (!submitted) {
                Log.d("ThreatPipeline", "PENDING_DROPPED_ON_APP_SWITCH: requestId="
                        + pendingTextEvent.requestId
                        + ", app=" + pendingTextEvent.appName);
            }
            pendingTextEvent = null;
        }
    }

    private List<ForwardedTextEvent> extractRelevantTextEvents(AccessibilityEvent event, String packageName) {
        // Gather text candidates from the accessibility event node tree.
        Set<String> uniqueCandidates = new LinkedHashSet<>();
        for (CharSequence value : event.getText()) {
            addCandidate(uniqueCandidates, value);
        }

        AccessibilityNodeInfo source = event.getSource();
        boolean editableSource = false;
        if (source != null) {
            editableSource = source.isEditable();
            addCandidate(uniqueCandidates, source.getText());
            addCandidate(uniqueCandidates, source.getContentDescription());
            addNodeTreeCandidates(source, uniqueCandidates);
        } else if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root != null) {
                addNodeTreeCandidates(root, uniqueCandidates);
            }
        }

        CharSequence contentDescription = event.getContentDescription();
        addCandidate(uniqueCandidates, contentDescription);

        List<ThreatTextGate.GateResult> acceptedCandidates = collectAcceptedCandidates(uniqueCandidates);
        if (acceptedCandidates.isEmpty()) {
            return new ArrayList<>();
        }

        boolean notificationEvent = event.getEventType() == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED;
        String direction = notificationEvent
                ? "incoming"
                : ((editableSource || event.getEventType() == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED)
                        ? "outgoing"
                        : "visible");
        String sender = "outgoing".equals(direction) ? "child" : "unknown";
        String sourceType = notificationEvent ? "accessibility_notification" : "accessibility_text";
        String appName = SupportedThreatAppRegistry.resolveAppName(this, packageName);
        long capturedAt = System.currentTimeMillis();
        List<ForwardedTextEvent> events = new ArrayList<>();
        for (ThreatTextGate.GateResult candidate : acceptedCandidates) {
            events.add(new ForwardedTextEvent(
                    UUID.randomUUID().toString(),
                    packageName,
                    appName,
                    candidate.displayText,
                    direction,
                    sender,
                    event.getEventType(),
                    capturedAt,
                    foregroundSessionId,
                    sourceType,
                    scoreCandidate(candidate)
            ));
        }
        return events;
    }

    private void addNodeTreeCandidates(AccessibilityNodeInfo root, Set<String> candidates) {
        ArrayDeque<AccessibilityNodeInfo> queue = new ArrayDeque<>();
        queue.add(root);
        int visitedCount = 0;

        while (!queue.isEmpty() && visitedCount < MAX_TRAVERSED_NODES && candidates.size() < MAX_CAPTURED_TEXT_COUNT) {
            AccessibilityNodeInfo node = queue.removeFirst();
            visitedCount++;
            addCandidate(candidates, node.getText());
            addCandidate(candidates, node.getContentDescription());

            for (int index = node.getChildCount() - 1; index >= 0; index--) {
                AccessibilityNodeInfo child = node.getChild(index);
                if (child != null) {
                    queue.addLast(child);
                }
            }
            node.recycle();
        }

        List<AccessibilityNodeInfo> toRecycle = new ArrayList<>(queue);
        for (AccessibilityNodeInfo node : toRecycle) {
            node.recycle();
        }
    }

    private void addCandidate(Set<String> candidates, CharSequence value) {
        if (value == null || candidates.size() >= MAX_CAPTURED_TEXT_COUNT) {
            return;
        }
        String clean = cleanup(value.toString());
        if (hasBasicTextValue(clean)) {
            candidates.add(clean);
        }
    }

    private List<ThreatTextGate.GateResult> collectAcceptedCandidates(Set<String> candidates) {
        List<ThreatTextGate.GateResult> accepted = new ArrayList<>();
        long now = SystemClock.elapsedRealtime();
        for (String candidate : candidates) {
            if (isSameAsPending(candidate)) {
                continue;
            }
            ThreatTextGate.GateResult result = threatTextPipeline.preflight(candidate, now);
            if (!"SKIPPED_DUPLICATE".equals(result.logCode)) {
                Log.d("ThreatPipeline", result.logCode + ": \"" + candidate + "\" (" + result.reason + ")");
            }
            if (!result.accepted) {
                if (pendingTextEvent != null && isMetadataDrop(result.decision)) {
                    Log.d("ThreatPipeline", "PENDING_KEEP_REAL_MESSAGE_DROP_METADATA: \""
                            + candidate + "\" (Keeping pending \"" + pendingTextEvent.text + "\")");
                }
                continue;
            }
            accepted.add(result);
        }
        accepted.sort((first, second) -> Integer.compare(scoreCandidate(second), scoreCandidate(first)));
        List<ThreatTextGate.GateResult> selected = new ArrayList<>();
        for (ThreatTextGate.GateResult result : accepted) {
            if (isContainerDuplicate(result, selected)) {
                continue;
            }
            selected.add(result);
            if (selected.size() >= MAX_FORWARD_EVENTS_PER_ACCESSIBILITY_EVENT) {
                break;
            }
        }
        return selected;
    }

    private boolean isContainerDuplicate(ThreatTextGate.GateResult candidate, List<ThreatTextGate.GateResult> selected) {
        if (candidate == null || TextUtils.isEmpty(candidate.normalizedText)) {
            return true;
        }
        for (ThreatTextGate.GateResult existing : selected) {
            if (existing == null || TextUtils.isEmpty(existing.normalizedText)) {
                continue;
            }
            if (candidate.normalizedText.contains(existing.normalizedText)
                    || existing.normalizedText.contains(candidate.normalizedText)) {
                return true;
            }
        }
        return false;
    }

    private boolean isMetadataDrop(ThreatTextGate.Decision decision) {
        return decision == ThreatTextGate.Decision.DROP_UI_NOISE
                || decision == ThreatTextGate.Decision.DROP_STATUS_TEXT
                || decision == ThreatTextGate.Decision.DROP_PROFILE_METADATA
                || decision == ThreatTextGate.Decision.DROP_LOW_VALUE_NON_MESSAGE;
    }

    private int scoreCandidate(ThreatTextGate.GateResult result) {
        int screenScore = result.screenResult == null ? 0 : result.screenResult.priorityScore();
        return screenScore + Math.min(40, result.displayText.length());
    }

    private boolean shouldReplacePendingText(ForwardedTextEvent newEvent) {
        if (newEvent == null) {
            return false;
        }
        if (pendingTextEvent == null) {
            return true;
        }
        if (TextUtils.equals(
                ThreatTextNormalizer.normalizedText(newEvent.text),
                ThreatTextNormalizer.normalizedText(pendingTextEvent.text))) {
            return false;
        }
        if (newEvent.appSessionId != pendingTextEvent.appSessionId) {
            return true;
        }
        return newEvent.priorityScore >= pendingTextEvent.priorityScore;
    }

    private boolean isSameAsPending(String candidate) {
        if (pendingTextEvent == null || TextUtils.isEmpty(candidate)) {
            return false;
        }
        String normalizedCandidate = ThreatTextNormalizer.normalizedText(candidate);
        String normalizedPending = ThreatTextNormalizer.normalizedText(pendingTextEvent.text);
        return TextUtils.equals(normalizedCandidate, normalizedPending);
    }

    private boolean hasBasicTextValue(String text) {
        if (TextUtils.isEmpty(text) || text.length() < 3) {
            return false;
        }

        String normalized = ThreatTextNormalizer.normalizedText(text);
        if (normalized.matches("^[0-9:./\\-\\s]+$")) {
            return false;
        }
        if (!normalized.matches(".*[a-z].*")) {
            return false;
        }
        return true;
    }

    private String cleanup(String text) {
        if (text == null) {
            return "";
        }
        return text.replace('\n', ' ').replaceAll("\\s+", " ").trim();
    }

    private boolean submitForwardedText(ForwardedTextEvent event) {
        ThreatTextPipeline.SubmissionResult submissionResult =
                threatTextPipeline.beforeSubmit(event.text, SystemClock.elapsedRealtime());
        if (!submissionResult.shouldSubmit) {
            Log.d("ThreatPipeline", submissionResult.logCode + ": \"" + event.text
                    + "\" (" + submissionResult.reason + ")");
            return false;
        }

        Log.d("ThreatPipeline", submissionResult.logCode + ": \"" + event.text + "\"");
        forwardThreatText(event);
        return true;
    }

    private void forwardThreatText(ForwardedTextEvent forwardedTextEvent) {
        Intent serviceIntent = new Intent(this, LimitService.class);
        serviceIntent.setAction(LimitService.ACTION_PROCESS_TEXT_EVENT);
        serviceIntent.putExtra(LimitService.EXTRA_PACKAGE_NAME, forwardedTextEvent.packageName);
        serviceIntent.putExtra(LimitService.EXTRA_APP_NAME, forwardedTextEvent.appName);
        serviceIntent.putExtra(LimitService.EXTRA_TEXT_CONTENT, forwardedTextEvent.text);
        serviceIntent.putExtra(LimitService.EXTRA_TEXT_DIRECTION, forwardedTextEvent.direction);
        serviceIntent.putExtra(LimitService.EXTRA_TEXT_SENDER, forwardedTextEvent.sender);
        serviceIntent.putExtra(LimitService.EXTRA_TEXT_EVENT_TYPE, forwardedTextEvent.eventType);
        serviceIntent.putExtra(LimitService.EXTRA_CAPTURED_AT, forwardedTextEvent.capturedAt);
        serviceIntent.putExtra(LimitService.EXTRA_TEXT_REQUEST_ID, forwardedTextEvent.requestId);
        serviceIntent.putExtra(LimitService.EXTRA_TEXT_APP_SESSION_ID, forwardedTextEvent.appSessionId);
        serviceIntent.putExtra(LimitService.EXTRA_TEXT_SOURCE_TYPE, forwardedTextEvent.sourceType);
        startLimitService(serviceIntent);
    }

    private void startLimitService(Intent serviceIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    private static final class ForwardedTextEvent {
        final String requestId;
        final String packageName;
        final String appName;
        final String text;
        final String direction;
        final String sender;
        final int eventType;
        final long capturedAt;
        final long appSessionId;
        final String sourceType;
        final int priorityScore;

        ForwardedTextEvent(
                String requestId,
                String packageName,
                String appName,
                String text,
                String direction,
                String sender,
                int eventType,
                long capturedAt,
                long appSessionId,
                String sourceType,
                int priorityScore
        ) {
            this.requestId = requestId;
            this.packageName = packageName;
            this.appName = appName;
            this.text = text;
            this.direction = direction;
            this.sender = sender;
            this.eventType = eventType;
            this.capturedAt = capturedAt;
            this.appSessionId = appSessionId;
            this.sourceType = sourceType;
            this.priorityScore = priorityScore;
        }
    }
}
