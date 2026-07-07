package com.mynyaman.mynyamanapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.core.app.NotificationCompat;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LimitService extends Service {

    public static final String ACTION_START_OR_REFRESH = "com.mynyaman.mynyamanapp.action.START_OR_REFRESH";
    public static final String ACTION_FORCE_SYNC = "com.mynyaman.mynyamanapp.action.FORCE_SYNC";
    public static final String ACTION_FOREGROUND_APP_CHANGED = "com.mynyaman.mynyamanapp.action.FOREGROUND_APP_CHANGED";
    public static final String ACTION_PROCESS_TEXT_EVENT = "com.mynyaman.mynyamanapp.action.PROCESS_TEXT_EVENT";

    public static final String EXTRA_PACKAGE_NAME = "package_name";
    public static final String EXTRA_APP_NAME = "app_name";
    public static final String EXTRA_ENTER_UPTIME_MS = "enter_uptime_ms";
    public static final String EXTRA_BASELINE_USAGE_MS = "baseline_usage_ms";
    public static final String EXTRA_TEXT_CONTENT = "text_content";
    public static final String EXTRA_TEXT_DIRECTION = "text_direction";
    public static final String EXTRA_TEXT_SENDER = "text_sender";
    public static final String EXTRA_TEXT_EVENT_TYPE = "text_event_type";
    public static final String EXTRA_CAPTURED_AT = "captured_at";
    public static final String EXTRA_TEXT_REQUEST_ID = "text_request_id";
    public static final String EXTRA_TEXT_APP_SESSION_ID = "text_app_session_id";
    public static final String EXTRA_TEXT_SOURCE_TYPE = "text_source_type";

    private static final int TICK_INTERVAL_MS = 1000;
    private static final int SYNC_INTERVAL_MS = 5000;
    private static final int THREAT_STATUS_SYNC_INTERVAL_MS = 30000;
    private static final int LOCK_BUFFER_MS = 150;
    private static final int NOTIFICATION_ID = 2201;
    private static final int SCAN_STATUS_NOTIFICATION_ID = 2205;
    private static final String CHANNEL_ID = "mynyaman_limit_monitor";
    private static final String SCAN_STATUS_CHANNEL_ID = "mynyaman_scan_status";
    private static final String THREAT_LOCKED_CHANNEL_ID = "mynyaman_threat_locked";
    private static final String GRACE_PERIOD_CHANNEL_ID = "mynyaman_grace_period";
    private static final long THREAT_DUPLICATE_WINDOW_MS = 4000L;
    private static final long POST_UNLOCK_GRACE_MS = 60L * 1000L;
    private static final int MAX_RECENT_THREAT_MESSAGES = 5;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private final ExecutorService threatExecutor = Executors.newSingleThreadExecutor();

    private WindowManager windowManager;
    private View overlayView;
    private boolean isOverlayShowing = false;
    private boolean isDeletingAccount = false;
    private long lastUsageSyncAt = 0L;
    private long lastThreatStatusSyncAt = 0L;
    private boolean foregroundReady = false;

    private ListenerRegistration userListenerRegistration;
    private String monitoredUserUid;
    private String parentUid;
    private String childName;
    private boolean pendingDeletion;
    private String currentUsageDayKey;

    private final Map<String, Long> cachedAppLimitSeconds = new HashMap<>();
    private String activePackageName;
    private long activePackageEnterUptimeMs = -1L;
    private long activePackageBaselineUsageMs = 0L;
    private String scheduledPackageName;
    private long scheduledLockAtUptimeMs = 0L;
    private String lastThreatSignature;
    private long lastThreatCapturedAt = 0L;
    private final List<String> recentThreatMessages = new ArrayList<>();
    private final Set<String> activeThreatRequestIds = new HashSet<>();

    // Cyberbully lock — separate from screen time lock
    private final Map<String, Long> postUnlockGraceEndByPackage = new HashMap<>();
    private final Set<String> activeCyberbullyLockedPackages = new HashSet<>();
    private final Set<String> directCyberbullyLockedPackages = new HashSet<>();
    private final Set<String> parentCyberbullyLockedPackages = new HashSet<>();
    private final Set<String> parentSummaryCyberbullyLockedPackages = new HashSet<>();
    private final Set<String> directCyberbullyUnlockedPackages = new HashSet<>();
    private final Set<String> parentCyberbullyUnlockedPackages = new HashSet<>();
    private final Set<String> parentSummaryCyberbullyUnlockedPackages = new HashSet<>();
    private final Map<String, Long> directCyberbullyLockedAtByPackage = new HashMap<>();
    private final Map<String, Long> parentCyberbullyLockedAtByPackage = new HashMap<>();
    private final Map<String, Long> parentSummaryCyberbullyLockedAtByPackage = new HashMap<>();
    private final Map<String, Long> directCyberbullyUnlockCreatedByPackage = new HashMap<>();
    private final Map<String, Long> parentCyberbullyUnlockCreatedByPackage = new HashMap<>();
    private final Map<String, Long> parentSummaryCyberbullyUnlockCreatedByPackage = new HashMap<>();
    private final Set<String> graceNotificationShownPackages = new HashSet<>();
    private final Map<String, String> processedUnlockRequestByPackage = new HashMap<>();
    private View threatOverlayView;
    private boolean isThreatOverlayShowing = false;
    private ListenerRegistration cyberbullyLockListenerRegistration;
    private ListenerRegistration parentCyberbullyLockListenerRegistration;
    private ListenerRegistration parentCyberbullySummaryListenerRegistration;
    private String monitoredCyberbullyLockChildUid;
    private String monitoredCyberbullyLockParentUid;
    private boolean parentUidLookupInProgress;
    private Runnable cyberbullyCountdownRunnable;
    private boolean threatScanEnabled = true;

    private final Runnable tickRunnable = new Runnable() {
        @Override
        public void run() {
            heartbeat(false);
            handler.postDelayed(this, TICK_INTERVAL_MS);
        }
    };

    private final Runnable scheduledLockRunnable = new Runnable() {
        @Override
        public void run() {
            scheduledPackageName = null;
            scheduledLockAtUptimeMs = 0L;
            evaluateLockState(true);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        createNotificationChannel();
        currentUsageDayKey = UsageWindowHelper.currentDayKey();
        try {
            startForeground(NOTIFICATION_ID, buildNotification("Protection is running in the background."));
            foregroundReady = true;
        } catch (RuntimeException ignored) {
            stopSelf();
            return;
        }
        handler.post(tickRunnable);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!foregroundReady) {
            stopSelf();
            return START_NOT_STICKY;
        }

        ensureUserListener();

        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_FOREGROUND_APP_CHANGED.equals(action)) {
                handleForegroundAppChanged(intent);
            } else if (ACTION_PROCESS_TEXT_EVENT.equals(action)) {
                handleThreatTextEvent(intent);
            }
        }

        boolean forceSync = intent != null && ACTION_FORCE_SYNC.equals(intent.getAction());
        heartbeat(forceSync);
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(tickRunnable);
        cancelScheduledLock();
        removeUserListener();
        removeCyberbullyLockListeners();
        hideOverlay();
        threatExecutor.shutdownNow();
    }

    private void heartbeat(boolean forceSync) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            clearSessionState();
            hideOverlay();
            updateNotification("Waiting for child sign in.");
            return;
        }

        ensureUserListener();

        if (pendingDeletion) {
            handlePendingDeletion(user);
            return;
        }

        handleDayBoundaryIfNeeded();
        restoreForegroundSessionIfNeeded();
        if (!AccessibilityStateHelper.isAppWatchServiceEnabled(this)) {
            refreshForegroundFromUsageEvents();
        }

        evaluateLockState(false);

        if (forceSync || System.currentTimeMillis() - lastUsageSyncAt >= SYNC_INTERVAL_MS) {
            syncUsageTotals(user);
        }
        if (forceSync || System.currentTimeMillis() - lastThreatStatusSyncAt >= THREAT_STATUS_SYNC_INTERVAL_MS) {
            syncThreatMonitoringStatus(user);
        }
    }

    private void ensureUserListener() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            removeUserListener();
            removeCyberbullyLockListeners();
            return;
        }

        if (TextUtils.equals(monitoredUserUid, user.getUid()) && userListenerRegistration != null) {
            return;
        }

        removeUserListener();
        removeCyberbullyLockListeners();
        monitoredUserUid = user.getUid();
        userListenerRegistration = firestore.collection("users")
                .document(monitoredUserUid)
                .addSnapshotListener((documentSnapshot, error) -> {
                    if (error != null || documentSnapshot == null || !documentSnapshot.exists()) {
                        cachedAppLimitSeconds.clear();
                        pendingDeletion = false;
                        parentUid = null;
                        childName = null;
                        evaluateLockState(true);
                        return;
                    }

                    pendingDeletion = Boolean.TRUE.equals(documentSnapshot.getBoolean("pendingDeletion"));
                    parentUid = documentSnapshot.getString("parentUid");
                    childName = documentSnapshot.getString("name");

                    Boolean scanEnabled = documentSnapshot.getBoolean("threatScanEnabled");
                    threatScanEnabled = scanEnabled == null || Boolean.TRUE.equals(scanEnabled);
                    updateScanStatusNotification(threatScanEnabled);

                    cachedAppLimitSeconds.clear();
                    cachedAppLimitSeconds.putAll(readLimitSeconds(documentSnapshot));

                    if (pendingDeletion) {
                        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                        if (currentUser != null) {
                            handlePendingDeletion(currentUser);
                        }
                        return;
                    }

                    evaluateLockState(true);
                    ensureCyberbullyLockListeners(monitoredUserUid, parentUid);
                    resolveParentUidForCyberbullyLocksIfNeeded(monitoredUserUid);
                });
        ensureCyberbullyLockListeners(user.getUid(), parentUid);
        resolveParentUidForCyberbullyLocksIfNeeded(user.getUid());
    }

    private void removeUserListener() {
        if (userListenerRegistration != null) {
            userListenerRegistration.remove();
            userListenerRegistration = null;
        }
        monitoredUserUid = null;
    }

    private void resolveParentUidForCyberbullyLocksIfNeeded(String childUid) {
        if (TextUtils.isEmpty(childUid) || !TextUtils.isEmpty(parentUid) || parentUidLookupInProgress) {
            return;
        }
        parentUidLookupInProgress = true;
        firestore.collection("parent_child_relationships")
                .whereEqualTo("childUid", childUid)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    parentUidLookupInProgress = false;
                    if (querySnapshot == null || querySnapshot.isEmpty()) {
                        return;
                    }
                    String resolvedParentUid = querySnapshot.getDocuments().get(0).getString("parentUid");
                    if (TextUtils.isEmpty(resolvedParentUid)) {
                        return;
                    }
                    parentUid = resolvedParentUid;
                    ensureCyberbullyLockListeners(childUid, resolvedParentUid);
                })
                .addOnFailureListener(e -> {
                    parentUidLookupInProgress = false;
                    Log.w("CyberbullyLock", "Unable to resolve parentUid for child unlock listener. childUid=" + childUid, e);
                });
    }

    private void handleForegroundAppChanged(Intent intent) {
        String packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME);
        long enterUptimeMs = intent.getLongExtra(EXTRA_ENTER_UPTIME_MS, SystemClock.uptimeMillis());
        long baselineUsageMs = intent.getLongExtra(EXTRA_BASELINE_USAGE_MS, 0L);
        setActiveForegroundPackage(packageName, enterUptimeMs, baselineUsageMs);
    }

    private void handleThreatTextEvent(Intent intent) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }

        if (!threatScanEnabled) {
            String pkgForLog = intent.getStringExtra(EXTRA_PACKAGE_NAME);
            Log.d("ThreatPipeline", "THREAT_SCAN_DISABLED: skipping text event for " + pkgForLog);
            return;
        }

        String packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME);
        String appName = intent.getStringExtra(EXTRA_APP_NAME);
        String textContent = intent.getStringExtra(EXTRA_TEXT_CONTENT);
        String direction = intent.getStringExtra(EXTRA_TEXT_DIRECTION);
        String sender = intent.getStringExtra(EXTRA_TEXT_SENDER);
        long capturedAt = intent.getLongExtra(EXTRA_CAPTURED_AT, System.currentTimeMillis());
        String requestId = intent.getStringExtra(EXTRA_TEXT_REQUEST_ID);
        long appSessionId = intent.getLongExtra(EXTRA_TEXT_APP_SESSION_ID, 0L);
        String sourceType = intent.getStringExtra(EXTRA_TEXT_SOURCE_TYPE);

        if (TextUtils.isEmpty(packageName)
                || TextUtils.isEmpty(textContent)
                || !SupportedThreatAppRegistry.isSupportedPackage(packageName)) {
            return;
        }

        if (isPostUnlockGraceActive(packageName)) {
            Long graceEnd = postUnlockGraceEndByPackage.get(packageName);
            Log.d("ThreatPipeline", "SKIPPED_POST_UNLOCK_GRACE: package=" + packageName
                    + " graceRemainingMs=" + (graceEnd == null ? 0L : graceEnd - System.currentTimeMillis()));
            return;
        }

        String safeDirection = TextUtils.isEmpty(direction) ? "visible" : direction;
        String signature = packageName + "|" + safeDirection + "|" + textContent.trim().toLowerCase(Locale.US);
        if (TextUtils.equals(signature, lastThreatSignature)
                && Math.abs(capturedAt - lastThreatCapturedAt) < THREAT_DUPLICATE_WINDOW_MS) {
            return;
        }

        lastThreatSignature = signature;
        lastThreatCapturedAt = capturedAt;
        String resolvedAppName = TextUtils.isEmpty(appName)
                ? SupportedThreatAppRegistry.resolveAppName(this, packageName)
                : appName;
        String safeSender = TextUtils.isEmpty(sender) ? "unknown" : sender;
        String safeChildName = childName;
        String safeParentUid = parentUid;
        String safeRequestId = TextUtils.isEmpty(requestId) ? UUID.randomUUID().toString() : requestId;
        String cleanedText = ThreatTextNormalizer.displayText(textContent);
        List<String> previousMessages = recentThreatMessagesSnapshot();
        ThreatTextAnalysisRequest request = new ThreatTextAnalysisRequest(
                safeRequestId,
                textContent,
                cleanedText,
                packageName,
                resolvedAppName,
                safeDirection,
                safeSender,
                user.getUid(),
                safeParentUid,
                safeChildName,
                capturedAt,
                appSessionId,
                TextUtils.isEmpty(sourceType) ? "accessibility_text" : sourceType
        );

        synchronized (activeThreatRequestIds) {
            activeThreatRequestIds.add(request.requestId);
        }

        threatExecutor.execute(() -> processThreatTextEvent(
                request,
                previousMessages
        ));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Long> readLimitSeconds(DocumentSnapshot documentSnapshot) {
        Map<String, Long> result = new HashMap<>();
        Map<String, Object> appLimitSeconds = (Map<String, Object>) documentSnapshot.get("appLimitSeconds");
        if (appLimitSeconds != null) {
            for (Map.Entry<String, Object> entry : appLimitSeconds.entrySet()) {
                Object value = entry.getValue();
                result.put(entry.getKey(), value instanceof Number ? ((Number) value).longValue() : 0L);
            }
            return result;
        }

        Map<String, Object> appLimitsMinutes = (Map<String, Object>) documentSnapshot.get("appLimits");
        if (appLimitsMinutes != null) {
            for (Map.Entry<String, Object> entry : appLimitsMinutes.entrySet()) {
                Object value = entry.getValue();
                long minutes = value instanceof Number ? ((Number) value).longValue() : 0L;
                result.put(entry.getKey(), minutes * 60L);
            }
        }
        return result;
    }

    private void handleDayBoundaryIfNeeded() {
        String newDayKey = UsageWindowHelper.currentDayKey();
        if (TextUtils.equals(currentUsageDayKey, newDayKey)) {
            return;
        }

        currentUsageDayKey = newDayKey;
        hideOverlay();
        cancelScheduledLock();
        lastUsageSyncAt = 0L;

        if (!TextUtils.isEmpty(activePackageName) && !getPackageName().equals(activePackageName)) {
            long baselineUsageMs = UsageStatsHelper.getTodayPackageUsageMillis(this, activePackageName);
            long enterUptimeMs = SystemClock.uptimeMillis();
            ForegroundAppStateStore.saveSession(this, activePackageName, enterUptimeMs, baselineUsageMs);
            activePackageEnterUptimeMs = enterUptimeMs;
            activePackageBaselineUsageMs = baselineUsageMs;
        }
    }
    private void restoreForegroundSessionIfNeeded() {
        ForegroundAppStateStore.ForegroundSession session = ForegroundAppStateStore.readSession(this);
        if (session == null) {
            return;
        }
        if (session.enterUptimeMs > SystemClock.uptimeMillis()) {
            ForegroundAppStateStore.clear(this);
            return;
        }
        if (!TextUtils.equals(activePackageName, session.packageName)
                || activePackageEnterUptimeMs != session.enterUptimeMs
                || activePackageBaselineUsageMs != session.baselineUsageMs) {
            setActiveForegroundPackage(session.packageName, session.enterUptimeMs, session.baselineUsageMs);
        }
    }

    private void refreshForegroundFromUsageEvents() {
        UsageStatsManager usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        if (usageStatsManager == null) {
            return;
        }

        String currentForegroundApp = findCurrentForegroundApp(usageStatsManager);
        if (TextUtils.isEmpty(currentForegroundApp)) {
            return;
        }

        if (!TextUtils.equals(activePackageName, currentForegroundApp)) {
            long baselineUsageMs = UsageStatsHelper.getTodayPackageUsageMillis(this, currentForegroundApp);
            long enterUptimeMs = SystemClock.uptimeMillis();
            ForegroundAppStateStore.saveSession(this, currentForegroundApp, enterUptimeMs, baselineUsageMs);
            setActiveForegroundPackage(currentForegroundApp, enterUptimeMs, baselineUsageMs);
        }
    }

    private void setActiveForegroundPackage(String packageName, long enterUptimeMs, long baselineUsageMs) {
        long now = SystemClock.uptimeMillis();
        if (TextUtils.isEmpty(packageName)) {
            activePackageName = null;
            activePackageEnterUptimeMs = -1L;
            activePackageBaselineUsageMs = 0L;
            cancelScheduledLock();
            hideOverlay();
            return;
        }

        activePackageName = packageName;
        activePackageEnterUptimeMs = Math.min(enterUptimeMs, now);
        activePackageBaselineUsageMs = Math.max(0L, baselineUsageMs);
        evaluateLockState(true);
        applyThreatLockIfNeeded(packageName);
    }

    private void evaluateLockState(boolean immediate) {
        if (!UsageStatsHelper.hasUsageStatsPermission(this)) {
            cancelScheduledLock();
            hideOverlay();
            updateNotification("Usage access is needed to monitor limits.");
            return;
        }

        if (TextUtils.isEmpty(activePackageName) || getPackageName().equals(activePackageName)) {
            cancelScheduledLock();
            hideOverlay();
            updateNotification(buildIdleNotificationText());
            return;
        }

        long limitSeconds = cachedAppLimitSeconds.containsKey(activePackageName)
                ? cachedAppLimitSeconds.get(activePackageName)
                : 0L;
        if (limitSeconds <= 0L) {
            cancelScheduledLock();
            hideOverlay();
            updateNotification(buildIdleNotificationText());
            return;
        }

        long limitMillis = limitSeconds * 1000L;
        long elapsedInCurrentSession = activePackageEnterUptimeMs > 0L
                ? Math.max(0L, SystemClock.uptimeMillis() - activePackageEnterUptimeMs)
                : 0L;
        long usedMillis = activePackageBaselineUsageMs + elapsedInCurrentSession;
        long remainingMillis = limitMillis - usedMillis;

        if (remainingMillis <= 0L || immediate) {
            if (remainingMillis <= 0L) {
                showOverlay(activePackageName);
                return;
            }
        }

        scheduleExactLock(activePackageName, remainingMillis);
        hideOverlay();
        updateNotification("Watching " + resolveAppName(activePackageName) + ".");
    }

    private String buildIdleNotificationText() {
        if (!AccessibilityStateHelper.isAppWatchServiceEnabled(this)) {
            return "Enable MyNyaman accessibility tracking for instant app locks.";
        }
        return cachedAppLimitSeconds.isEmpty() ? "No active app limits yet." : "Protection is active.";
    }

    private void scheduleExactLock(String packageName, long remainingMillis) {
        long targetTime = SystemClock.uptimeMillis() + Math.max(100L, remainingMillis + LOCK_BUFFER_MS);
        if (TextUtils.equals(packageName, scheduledPackageName)
                && Math.abs(targetTime - scheduledLockAtUptimeMs) < 400L) {
            return;
        }

        cancelScheduledLock();
        scheduledPackageName = packageName;
        scheduledLockAtUptimeMs = targetTime;
        handler.postAtTime(scheduledLockRunnable, targetTime);
    }

    private void cancelScheduledLock() {
        scheduledPackageName = null;
        scheduledLockAtUptimeMs = 0L;
        handler.removeCallbacks(scheduledLockRunnable);
    }

    private void syncUsageTotals(FirebaseUser user) {
        if (!UsageStatsHelper.hasUsageStatsPermission(this)) {
            return;
        }

        List<UsageStatsHelper.AppUsageRecord> usageRecords = UsageStatsHelper.getTodayUsageRecords(this);
        long usageMillis = 0L;
        for (UsageStatsHelper.AppUsageRecord record : usageRecords) {
            usageMillis += record.timeInForegroundMillis;
        }

        if (!TextUtils.isEmpty(activePackageName) && !getPackageName().equals(activePackageName)) {
            long elapsedInCurrentSession = activePackageEnterUptimeMs > 0L
                    ? Math.max(0L, SystemClock.uptimeMillis() - activePackageEnterUptimeMs)
                    : 0L;
            long internalUsageMillis = activePackageBaselineUsageMs + elapsedInCurrentSession;
            mergeActiveSessionIntoUsageRecords(usageRecords, internalUsageMillis);
        }

        usageMillis = recalculateUsageTotals(usageRecords);
        long usageSeconds = usageMillis / 1000L;
        long usageMinutes = usageSeconds / 60L;
        Timestamp now = Timestamp.now();

        List<Map<String, Object>> topAppsPayload = UsageStatsHelper.toTopAppsPayload(usageRecords);
        Map<String, Object> usageData = new HashMap<>();
        usageData.put("usageDayKey", UsageWindowHelper.currentDayKey());
        usageData.put("usageWeekKey", UsageWindowHelper.currentWeekStartKey());
        usageData.put("usageTodayMinutes", usageMinutes);
        usageData.put("usageTodaySeconds", usageSeconds);
        usageData.put("usageLastSyncedAt", now);
        usageData.put("topApps", topAppsPayload);
        usageData.put("usagePermissionGranted", true);

        firestore.collection("users")
                .document(user.getUid())
                .set(usageData, SetOptions.merge());

        if (!TextUtils.isEmpty(parentUid)) {
            Map<String, Object> parentChildUsage = new HashMap<>(usageData);
            parentChildUsage.put("name", childName);
            firestore.collection("users")
                    .document(parentUid)
                    .collection("children")
                    .document(user.getUid())
                    .set(parentChildUsage, SetOptions.merge());
        }

        BlueprintSyncRepository.syncUsageReport(
                firestore,
                user.getUid(),
                parentUid,
                childName,
                usageMillis,
                topAppsPayload
        );
        lastUsageSyncAt = System.currentTimeMillis();
    }

    private void syncThreatMonitoringStatus(FirebaseUser user) {
        if (user == null) {
            return;
        }
        BlueprintSyncRepository.syncThreatMonitoringStatus(
                firestore,
                user.getUid(),
                parentUid,
                childName,
                ThreatMonitoringPermissionHelper.buildStatusPayload(this)
        );
        lastThreatStatusSyncAt = System.currentTimeMillis();
    }

    private void processThreatTextEvent(
            ThreatTextAnalysisRequest request,
            List<String> previousMessages
    ) {
        try {
            if (!isActiveThreatRequest(request.requestId)) {
                Log.w("STALE_GROQ_RESULT_DROPPED", "Threat request is no longer active before analysis: " + request.requestId);
                return;
            }

            // Threat analysis runs off the UI thread so the foreground protection loop stays responsive.
            Log.d("ThreatPipeline", "processThreatTextEvent: analyzing requestId=" + request.requestId
                    + " from " + request.appName + " (" + request.cleanedText.length() + " chars)");
            ThreatResult result = ThreatAnalyzer.analyze(
                    getApplicationContext(),
                    request.cleanedText,
                    previousMessages,
                    request.appName,
                    request.packageName,
                    request.direction,
                    request.sender
            );
            rememberThreatMessage(request.cleanedText);
            Log.d("ThreatPipeline", "Text analysis result: requestId=" + request.requestId
                    + ", level=" + result.finalLevel.name()
                    + ", groqUsed=" + result.remoteReviewUsed);
            if (result.finalLevel == ThreatResult.Level.GREEN) {
                return;
            }
            if (!shouldCreateThreatAlert(result)) {
                Log.d("ThreatPipeline", "NON_ACTIONABLE_THREAT_RESULT_DROPPED: requestId=" + request.requestId
                        + ", level=" + result.finalLevel.name()
                        + ", groqUsed=" + result.remoteReviewUsed);
                return;
            }
            if (isPostUnlockGraceActive(request.packageName)) {
                Log.d("ThreatPipeline", "POST_UNLOCK_GRACE_DROPPED_ANALYSIS_RESULT: requestId=" + request.requestId
                        + ", package=" + request.packageName);
                return;
            }

            if (result.finalLevel == ThreatResult.Level.RED) {
                String lockedPackage = request.packageName;
                String lockedAppName = request.appName;
                String triggerSignature = request.originalText == null
                        ? ""
                        : request.originalText.trim().toLowerCase(Locale.US);
                BlueprintSyncRepository.writeCyberbullyLock(
                        firestore,
                        request.childUid,
                        request.parentUid,
                        lockedPackage,
                        lockedAppName,
                        triggerSignature
                );
                showCyberbullyLockedNotification(lockedPackage, lockedAppName);
                activeCyberbullyLockedPackages.add(lockedPackage);
                showCyberbullyWarningOverlayIfActive(lockedPackage, lockedAppName);
            }

            if (!isActiveThreatRequest(request.requestId)) {
                Log.w("STALE_GROQ_RESULT_DROPPED", "Dropping stale Groq result for requestId=" + request.requestId);
                return;
            }

            Timestamp detectedAt = new Timestamp(
                    new java.util.Date(request.capturedAt > 0L ? request.capturedAt : System.currentTimeMillis())
            );

            if (TextUtils.isEmpty(request.parentUid)) {
                Log.w("ThreatFirestore", "Threat incident has no parentUid yet; repository will resolve the linked parent before mirroring.");
            }

            Log.d("INCIDENT_BUILT_FROM_REQUEST_ID", "Building incident from requestId=" + request.requestId
                    + ", app=" + request.appName
                    + ", package=" + request.packageName);
            BlueprintSyncRepository.ThreatIncidentPayload payload =
                    new BlueprintSyncRepository.ThreatIncidentPayload(
                            null,
                            request.requestId,
                            request.childUid,
                            request.parentUid,
                            request.childName,
                            request.appName,
                            request.packageName,
                            request.cleanedText,
                            result.maskedPreview,
                            ThreatResult.displayName(result.finalLevel),
                            detectedAt,
                            request.sender,
                            request.direction,
                            "unresolved",
                            result.consultantSuggested,
                            result.summary,
                            result.contentType,
                            null,
                            result.remoteReviewUsed ? "groq" : "local",
                            result.remoteReviewUsed
                                    ? (TextUtils.isEmpty(BuildConfig.GROQ_MODEL) ? GroqThreatApi.DEFAULT_MODEL : BuildConfig.GROQ_MODEL)
                                    : "",
                            result.reason,
                            result.reportJson
                    );

            BlueprintSyncRepository.logThreatIncident(
                    firestore,
                    payload
            );

            handler.post(() -> {
                ThreatNotificationHelper.showChildAlert(this, request.appName, result);
                updateNotification("Threat monitoring flagged " + request.appName + ".");
            });
        } catch (Exception e) {
            Log.w("ThreatPipeline", "Threat analysis failed for requestId=" + request.requestId, e);
        } finally {
            synchronized (activeThreatRequestIds) {
                activeThreatRequestIds.remove(request.requestId);
            }
        }
    }

    private boolean shouldCreateThreatAlert(ThreatResult result) {
        return result != null
                && result.finalLevel != ThreatResult.Level.GREEN
                && result.consultantSuggested;
    }

    private boolean isActiveThreatRequest(String requestId) {
        synchronized (activeThreatRequestIds) {
            return activeThreatRequestIds.contains(requestId);
        }
    }

    private List<String> recentThreatMessagesSnapshot() {
        synchronized (recentThreatMessages) {
            return new ArrayList<>(recentThreatMessages);
        }
    }

    private void rememberThreatMessage(String textContent) {
        if (TextUtils.isEmpty(textContent)) {
            return;
        }
        synchronized (recentThreatMessages) {
            recentThreatMessages.add(textContent.trim());
            while (recentThreatMessages.size() > MAX_RECENT_THREAT_MESSAGES) {
                recentThreatMessages.remove(0);
            }
        }
    }

    private void mergeActiveSessionIntoUsageRecords(List<UsageStatsHelper.AppUsageRecord> usageRecords, long internalUsageMillis) {
        for (int i = 0; i < usageRecords.size(); i++) {
            UsageStatsHelper.AppUsageRecord record = usageRecords.get(i);
            if (TextUtils.equals(activePackageName, record.packageName)) {
                if (record.timeInForegroundMillis >= internalUsageMillis) {
                    return;
                }
                usageRecords.set(i, new UsageStatsHelper.AppUsageRecord(record.appName, record.packageName, internalUsageMillis));
                return;
            }
        }

        usageRecords.add(new UsageStatsHelper.AppUsageRecord(resolveAppName(activePackageName), activePackageName, internalUsageMillis));
    }

    private long recalculateUsageTotals(List<UsageStatsHelper.AppUsageRecord> usageRecords) {
        long totalUsageMillis = 0L;
        for (UsageStatsHelper.AppUsageRecord record : usageRecords) {
            totalUsageMillis += record.timeInForegroundMillis;
        }

        for (UsageStatsHelper.AppUsageRecord record : usageRecords) {
            record.percentage = totalUsageMillis > 0L
                    ? (int) ((record.timeInForegroundMillis * 100L) / totalUsageMillis)
                    : 0;
        }
        usageRecords.sort((first, second) -> Long.compare(second.timeInForegroundMillis, first.timeInForegroundMillis));
        return totalUsageMillis;
    }

    private void handlePendingDeletion(FirebaseUser user) {
        if (isDeletingAccount) {
            return;
        }
        isDeletingAccount = true;

        user.delete()
                .addOnSuccessListener(unused -> {
                    firestore.collection("users").document(user.getUid()).delete();
                    FirebaseAuth.getInstance().signOut();
                    hideOverlay();
                    stopSelf();
                    Intent loginIntent = new Intent(this, Login.class);
                    loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(loginIntent);
                })
                .addOnFailureListener(e -> {
                    FirebaseAuth.getInstance().signOut();
                    hideOverlay();
                    stopSelf();
                    Intent loginIntent = new Intent(this, Login.class);
                    loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(loginIntent);
                });
    }

    private String findCurrentForegroundApp(UsageStatsManager usageStatsManager) {
        long timeNow = System.currentTimeMillis();
        UsageEvents usageEvents = usageStatsManager.queryEvents(timeNow - 5000L, timeNow);
        UsageEvents.Event event = new UsageEvents.Event();
        String currentForegroundApp = null;

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event);
            if (event.getEventType() == UsageEvents.Event.ACTIVITY_RESUMED) {
                currentForegroundApp = event.getPackageName();
            }
        }
        return currentForegroundApp;
    }

    private void showOverlay(String packageName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            sendUserHomeRepeatedly();
            return;
        }

        String appName = resolveAppName(packageName);
        updateNotification(appName + " has reached its limit.");

        if (isOverlayShowing && overlayView != null) {
            TextView tvMessage = overlayView.findViewById(R.id.tvLockMessage);
            tvMessage.setText(appName + " is Locked");
            sendUserHomeRepeatedly();
            return;
        }

        int layoutFlag = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );

        overlayView = LayoutInflater.from(this).inflate(R.layout.layout_app_lock, null);
        TextView tvMessage = overlayView.findViewById(R.id.tvLockMessage);
        tvMessage.setText(appName + " is Locked");
        overlayView.findViewById(R.id.btnGoHome).setOnClickListener(v -> sendUserHomeRepeatedly());

        try {
            windowManager.addView(overlayView, params);
            isOverlayShowing = true;
        } catch (Exception ignored) {
        }
        sendUserHomeRepeatedly();
    }

    private void sendUserHomeRepeatedly() {
        sendUserHome();
        handler.postDelayed(this::sendUserHome, 150L);
        handler.postDelayed(this::sendUserHome, 300L);
    }

    private void sendUserHome() {
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(homeIntent);
    }

    private void hideOverlay() {
        if (isOverlayShowing && overlayView != null) {
            try {
                windowManager.removeViewImmediate(overlayView);
            } catch (Exception ignored) {
            }
            overlayView = null;
            isOverlayShowing = false;
        }
    }

    private void clearSessionState() {
        cachedAppLimitSeconds.clear();
        pendingDeletion = false;
        parentUid = null;
        parentUidLookupInProgress = false;
        childName = null;
        activePackageName = null;
        activePackageEnterUptimeMs = -1L;
        activePackageBaselineUsageMs = 0L;
        currentUsageDayKey = UsageWindowHelper.currentDayKey();
        lastThreatSignature = null;
        lastThreatCapturedAt = 0L;
        lastThreatStatusSyncAt = 0L;
        synchronized (recentThreatMessages) {
            recentThreatMessages.clear();
        }
        ForegroundAppStateStore.clear(this);
        cancelScheduledLock();
        removeUserListener();
        postUnlockGraceEndByPackage.clear();
        activeCyberbullyLockedPackages.clear();
        directCyberbullyLockedPackages.clear();
        parentCyberbullyLockedPackages.clear();
        parentSummaryCyberbullyLockedPackages.clear();
        directCyberbullyUnlockedPackages.clear();
        parentCyberbullyUnlockedPackages.clear();
        parentSummaryCyberbullyUnlockedPackages.clear();
        directCyberbullyLockedAtByPackage.clear();
        parentCyberbullyLockedAtByPackage.clear();
        parentSummaryCyberbullyLockedAtByPackage.clear();
        directCyberbullyUnlockCreatedByPackage.clear();
        parentCyberbullyUnlockCreatedByPackage.clear();
        parentSummaryCyberbullyUnlockCreatedByPackage.clear();
        graceNotificationShownPackages.clear();
        processedUnlockRequestByPackage.clear();
        hideCyberbullyWarningOverlay();
        removeCyberbullyLockListeners();
        threatScanEnabled = true;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "MyNyaman Protection",
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription("Keeps app limits active in the background.");
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification(String contentText) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
                .setContentTitle("MyNyaman protection active")
                .setContentText(contentText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(contentText))
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void updateNotification(String contentText) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, buildNotification(contentText));
        }
    }

    private void updateScanStatusNotification(boolean enabled) {
        ensureNotificationChannel(
                SCAN_STATUS_CHANNEL_ID,
                "MyNyaman Scan Status",
                "Shows whether AI cyberbully scanning is active on this child device.",
                NotificationManager.IMPORTANCE_LOW
        );
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            return;
        }

        String title = enabled
                ? "MyNyaman AI Cyberbully Scan: ON"
                : "MyNyaman AI Cyberbully Scan: OFF";
        String body = enabled
                ? "Your conversation activity is being monitored for serious threats."
                : "Threat monitoring has been paused by your parent.";

        try {
            notificationManager.notify(
                    SCAN_STATUS_NOTIFICATION_ID,
                    new NotificationCompat.Builder(this, SCAN_STATUS_CHANNEL_ID)
                            .setSmallIcon(R.mipmap.ic_launcher)
                            .setContentTitle(title)
                            .setContentText(body)
                            .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                            .setOngoing(true)
                            .setAutoCancel(false)
                            .setOnlyAlertOnce(true)
                            .setPriority(NotificationCompat.PRIORITY_LOW)
                            .setContentIntent(buildThreatDetectionPendingIntent(SCAN_STATUS_NOTIFICATION_ID))
                            .build()
            );
        } catch (RuntimeException exception) {
            Log.w("ScanStatusNotification", "Unable to show scan status notification.", exception);
        }
    }

    private void showCyberbullyLockedNotification(String packageName, String appName) {
        if (TextUtils.isEmpty(packageName)) {
            return;
        }
        ensureNotificationChannel(
                THREAT_LOCKED_CHANNEL_ID,
                "MyNyaman Threat Locks",
                "Persistent alerts for apps locked after serious cyberbullying threats.",
                NotificationManager.IMPORTANCE_HIGH
        );
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            return;
        }

        String appLabel = TextUtils.isEmpty(appName) ? resolveAppName(packageName) : appName;
        String body = appLabel + " has been locked. A serious threat was detected in your conversation. Contact your parent to unblock.";
        try {
            notificationManager.notify(
                    cyberbullyLockNotificationId(packageName),
                    new NotificationCompat.Builder(this, THREAT_LOCKED_CHANNEL_ID)
                            .setSmallIcon(R.mipmap.ic_launcher)
                            .setContentTitle("🚨 MyNyaman — Cyberbullying Detected")
                            .setContentText(body)
                            .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                            .setOngoing(true)
                            .setAutoCancel(false)
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                            .build()
            );
        } catch (RuntimeException exception) {
            Log.w("ThreatLockNotification", "Unable to show cyberbully lock notification for " + packageName, exception);
        }
    }

    private void showGracePeriodNotification(String packageName, String appName) {
        if (TextUtils.isEmpty(packageName)) {
            return;
        }
        ensureNotificationChannel(
                GRACE_PERIOD_CHANNEL_ID,
                "MyNyaman Grace Period",
                "Temporary alerts after a parent unlocks an app.",
                NotificationManager.IMPORTANCE_HIGH
        );
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            return;
        }

        int notificationId = gracePeriodNotificationId(packageName);
        String appLabel = TextUtils.isEmpty(appName) ? resolveAppName(packageName) : appName;
        String body = "You have 1 minute to clear any threatening messages. Stay safe online! 💙";
        try {
            notificationManager.notify(
                    notificationId,
                    new NotificationCompat.Builder(this, GRACE_PERIOD_CHANNEL_ID)
                            .setSmallIcon(R.mipmap.ic_launcher)
                            .setContentTitle(appLabel + " has been unlocked by your parent")
                            .setContentText(body)
                            .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                            .setOngoing(true)
                            .setAutoCancel(false)
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                            .build()
            );
            handler.postDelayed(() -> notificationManager.cancel(notificationId), 60L * 1000L);
        } catch (RuntimeException exception) {
            Log.w("GracePeriodNotification", "Unable to show grace period notification for " + packageName, exception);
        }
    }

    private void cancelCyberbullyLockedNotification(String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            return;
        }
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel(cyberbullyLockNotificationId(packageName));
        }
    }

    private int cyberbullyLockNotificationId(String packageName) {
        return Math.abs(packageName.hashCode()) % 100000 + 3000;
    }

    private int gracePeriodNotificationId(String packageName) {
        return Math.abs(packageName.hashCode()) % 100000 + 4000;
    }

    private PendingIntent buildThreatDetectionPendingIntent(int requestCode) {
        Intent intent = new Intent(this, AiThreatDetectionActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getActivity(this, requestCode, intent, flags);
    }

    private void ensureNotificationChannel(String channelId, String name, String description, int importance) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if (notificationManager == null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(channelId, name, importance);
        channel.setDescription(description);
        notificationManager.createNotificationChannel(channel);
    }

    private String resolveAppName(String packageName) {
        try {
            return getPackageManager().getApplicationLabel(
                    getPackageManager().getApplicationInfo(packageName, 0)
            ).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return packageName;
        }
    }

    private void ensureCyberbullyLockListeners(String childUid, String linkedParentUid) {
        if (TextUtils.isEmpty(childUid)) {
            return;
        }

        String safeParentUid = linkedParentUid == null ? "" : linkedParentUid;
        boolean parentListenerReady = TextUtils.isEmpty(safeParentUid)
                || (parentCyberbullyLockListenerRegistration != null
                && parentCyberbullySummaryListenerRegistration != null);
        if (TextUtils.equals(childUid, monitoredCyberbullyLockChildUid)
                && TextUtils.equals(safeParentUid, monitoredCyberbullyLockParentUid)
                && cyberbullyLockListenerRegistration != null
                && parentListenerReady) {
            return;
        }

        removeCyberbullyLockListeners();
        monitoredCyberbullyLockChildUid = childUid;
        monitoredCyberbullyLockParentUid = safeParentUid;

        cyberbullyLockListenerRegistration = firestore
                .collection("users")
                .document(childUid)
                .collection("cyberbullyLockedApps")
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null || querySnapshot == null) {
                        return;
                    }
                    updateCyberbullyLockSource(
                            querySnapshot.getDocuments(),
                            directCyberbullyLockedPackages,
                            directCyberbullyUnlockedPackages,
                            directCyberbullyLockedAtByPackage,
                            directCyberbullyUnlockCreatedByPackage
                    );
                });

        if (!TextUtils.isEmpty(safeParentUid)) {
            parentCyberbullyLockListenerRegistration = firestore
                    .collection("users")
                    .document(safeParentUid)
                    .collection("children")
                    .document(childUid)
                    .collection("cyberbullyLockedApps")
                    .addSnapshotListener((querySnapshot, error) -> {
                        if (error != null || querySnapshot == null) {
                            return;
                        }
                        updateCyberbullyLockSource(
                                querySnapshot.getDocuments(),
                                parentCyberbullyLockedPackages,
                                parentCyberbullyUnlockedPackages,
                                parentCyberbullyLockedAtByPackage,
                                parentCyberbullyUnlockCreatedByPackage
                        );
                    });
            parentCyberbullySummaryListenerRegistration = firestore
                    .collection("users")
                    .document(safeParentUid)
                    .collection("children")
                    .document(childUid)
                    .addSnapshotListener((documentSnapshot, error) -> {
                        if (error != null || documentSnapshot == null) {
                            return;
                        }
                        updateCyberbullySummaryLockSource(documentSnapshot);
                    });
        }
    }

    private void updateCyberbullyLockSource(
            List<DocumentSnapshot> documents,
            Set<String> lockedPackages,
            Set<String> unlockedPackages,
            Map<String, Long> lockedAtByPackage,
            Map<String, Long> unlockCreatedByPackage
    ) {
        lockedPackages.clear();
        unlockedPackages.clear();
        lockedAtByPackage.clear();
        unlockCreatedByPackage.clear();
        for (DocumentSnapshot doc : documents) {
            String packageName = doc.getId();
            if (TextUtils.isEmpty(packageName)) {
                continue;
            }
            Boolean unlocked = doc.getBoolean("unlockedByParent");
            if (Boolean.TRUE.equals(unlocked)) {
                unlockedPackages.add(packageName);
                unlockCreatedByPackage.put(packageName, readLongField(doc, "unlockCreatedMillis"));
                applyCyberbullyUnlock(
                        packageName,
                        doc.getString("appName"),
                        readLongField(doc, "unlockGraceUntilMillis"),
                        doc.getString("unlockRequestId")
                );
            } else {
                lockedPackages.add(packageName);
                lockedAtByPackage.put(packageName, readLongField(doc, "lockedAtMillis"));
            }
        }
        rebuildActiveCyberbullyLocks();
    }

    private void updateCyberbullySummaryLockSource(DocumentSnapshot document) {
        parentSummaryCyberbullyLockedPackages.clear();
        parentSummaryCyberbullyUnlockedPackages.clear();
        parentSummaryCyberbullyLockedAtByPackage.clear();
        parentSummaryCyberbullyUnlockCreatedByPackage.clear();

        if (document == null || !document.exists()) {
            rebuildActiveCyberbullyLocks();
            return;
        }

        Object activePackagesValue = document.get("cyberbullyActiveLockedPackages");
        if (activePackagesValue instanceof List<?>) {
            for (Object value : (List<?>) activePackagesValue) {
                String packageName = value == null ? "" : value.toString();
                if (!TextUtils.isEmpty(packageName)) {
                    parentSummaryCyberbullyLockedPackages.add(packageName);
                    parentSummaryCyberbullyLockedAtByPackage.put(packageName, 0L);
                }
            }
        }

        Object activeAppsValue = document.get("cyberbullyActiveLockedApps");
        if (activeAppsValue instanceof Map<?, ?>) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) activeAppsValue).entrySet()) {
                String packageName = entry.getKey() == null ? "" : entry.getKey().toString();
                Map<?, ?> appData = asMap(entry.getValue());
                if (appData != null) {
                    String mappedPackageName = readStringFromMap(appData, "packageName");
                    if (!TextUtils.isEmpty(mappedPackageName)) {
                        packageName = mappedPackageName;
                    }
                }
                if (TextUtils.isEmpty(packageName)) {
                    continue;
                }

                boolean unlocked = appData != null && readBooleanFromMap(appData, "unlockedByParent");
                if (unlocked) {
                    parentSummaryCyberbullyUnlockedPackages.add(packageName);
                    parentSummaryCyberbullyLockedPackages.remove(packageName);
                    long unlockCreatedMillis = readLongFromMap(appData, "unlockCreatedMillis");
                    parentSummaryCyberbullyUnlockCreatedByPackage.put(packageName, unlockCreatedMillis);
                    applyCyberbullyUnlock(
                            packageName,
                            readStringFromMap(appData, "appName"),
                            readLongFromMap(appData, "unlockGraceUntilMillis"),
                            readStringFromMap(appData, "unlockRequestId")
                    );
                } else {
                    parentSummaryCyberbullyLockedPackages.add(packageName);
                    parentSummaryCyberbullyLockedAtByPackage.put(
                            packageName,
                            appData == null ? 0L : readLongFromMap(appData, "lockedAtMillis")
                    );
                }
            }
        }

        rebuildActiveCyberbullyLocks();
    }

    private void rebuildActiveCyberbullyLocks() {
        Set<String> lockedCandidates = new HashSet<>();
        lockedCandidates.addAll(directCyberbullyLockedPackages);
        lockedCandidates.addAll(parentCyberbullyLockedPackages);
        lockedCandidates.addAll(parentSummaryCyberbullyLockedPackages);
        activeCyberbullyLockedPackages.clear();

        for (String packageName : lockedCandidates) {
            if (isPostUnlockGraceActive(packageName)) {
                continue;
            }

            long latestLockMillis = Math.max(
                    Math.max(
                            valueOrZero(directCyberbullyLockedAtByPackage.get(packageName)),
                            valueOrZero(parentCyberbullyLockedAtByPackage.get(packageName))
                    ),
                    valueOrZero(parentSummaryCyberbullyLockedAtByPackage.get(packageName))
            );
            long latestUnlockMillis = Math.max(
                    Math.max(
                            valueOrZero(directCyberbullyUnlockCreatedByPackage.get(packageName)),
                            valueOrZero(parentCyberbullyUnlockCreatedByPackage.get(packageName))
                    ),
                    valueOrZero(parentSummaryCyberbullyUnlockCreatedByPackage.get(packageName))
            );
            boolean markedUnlocked = directCyberbullyUnlockedPackages.contains(packageName)
                    || parentCyberbullyUnlockedPackages.contains(packageName)
                    || parentSummaryCyberbullyUnlockedPackages.contains(packageName);
            if (markedUnlocked
                    && ((latestUnlockMillis > 0L && latestLockMillis <= latestUnlockMillis)
                    || (latestUnlockMillis == 0L && latestLockMillis == 0L))) {
                continue;
            }

            activeCyberbullyLockedPackages.add(packageName);
        }

        for (String packageName : activeCyberbullyLockedPackages) {
            postUnlockGraceEndByPackage.remove(packageName);
            graceNotificationShownPackages.remove(packageName);
            processedUnlockRequestByPackage.remove(packageName);
        }

        if (!TextUtils.isEmpty(activePackageName)) {
            if (activeCyberbullyLockedPackages.contains(activePackageName)) {
                applyThreatLockIfNeeded(activePackageName);
            } else {
                hideCyberbullyWarningOverlay();
            }
        }
    }

    private void applyCyberbullyUnlock(
            String packageName,
            String appName,
            long unlockGraceUntilMillis,
            String unlockRequestId
    ) {
        if (TextUtils.isEmpty(packageName)) {
            return;
        }
        String safeUnlockRequestId = TextUtils.isEmpty(unlockRequestId)
                ? "legacy_" + packageName
                : unlockRequestId;
        boolean newUnlockRequest = !TextUtils.equals(
                safeUnlockRequestId,
                processedUnlockRequestByPackage.get(packageName)
        );
        processedUnlockRequestByPackage.put(packageName, safeUnlockRequestId);

        activeCyberbullyLockedPackages.remove(packageName);
        cancelCyberbullyLockedNotification(packageName);
        if (TextUtils.equals(activePackageName, packageName)) {
            hideCyberbullyWarningOverlay();
        }
        if (newUnlockRequest) {
            graceNotificationShownPackages.remove(packageName);
        }
        if (newUnlockRequest && graceNotificationShownPackages.add(packageName)) {
            showGracePeriodNotification(packageName, appName);
        }
        long now = System.currentTimeMillis();
        if (unlockGraceUntilMillis > 0L) {
            if (unlockGraceUntilMillis > now) {
                postUnlockGraceEndByPackage.put(packageName, unlockGraceUntilMillis);
            } else {
                postUnlockGraceEndByPackage.remove(packageName);
            }
            return;
        }
        if (newUnlockRequest) {
            postUnlockGraceEndByPackage.put(packageName, now + POST_UNLOCK_GRACE_MS);
        }
    }

    private boolean isPostUnlockGraceActive(String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            return false;
        }
        Long graceEnd = postUnlockGraceEndByPackage.get(packageName);
        if (graceEnd == null) {
            return false;
        }
        if (System.currentTimeMillis() < graceEnd) {
            return true;
        }
        postUnlockGraceEndByPackage.remove(packageName);
        graceNotificationShownPackages.remove(packageName);
        return false;
    }

    private Map<?, ?> asMap(Object value) {
        if (value instanceof Map<?, ?>) {
            return (Map<?, ?>) value;
        }
        return null;
    }

    private String readStringFromMap(Map<?, ?> map, String fieldName) {
        if (map == null || TextUtils.isEmpty(fieldName)) {
            return "";
        }
        Object value = map.get(fieldName);
        return value == null ? "" : value.toString();
    }

    private boolean readBooleanFromMap(Map<?, ?> map, String fieldName) {
        if (map == null || TextUtils.isEmpty(fieldName)) {
            return false;
        }
        Object value = map.get(fieldName);
        if (value instanceof Boolean) {
            return Boolean.TRUE.equals(value);
        }
        return value != null && Boolean.parseBoolean(value.toString());
    }

    private long readLongFromMap(Map<?, ?> map, String fieldName) {
        if (map == null || TextUtils.isEmpty(fieldName)) {
            return 0L;
        }
        Object value = map.get(fieldName);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value == null) {
            return 0L;
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private long readLongField(DocumentSnapshot document, String fieldName) {
        if (document == null || TextUtils.isEmpty(fieldName)) {
            return 0L;
        }
        Long value = document.getLong(fieldName);
        return value == null ? 0L : value;
    }

    private long valueOrZero(Long value) {
        return value == null ? 0L : value;
    }

    private void removeCyberbullyLockListeners() {
        if (cyberbullyLockListenerRegistration != null) {
            cyberbullyLockListenerRegistration.remove();
            cyberbullyLockListenerRegistration = null;
        }
        if (parentCyberbullyLockListenerRegistration != null) {
            parentCyberbullyLockListenerRegistration.remove();
            parentCyberbullyLockListenerRegistration = null;
        }
        if (parentCyberbullySummaryListenerRegistration != null) {
            parentCyberbullySummaryListenerRegistration.remove();
            parentCyberbullySummaryListenerRegistration = null;
        }
        directCyberbullyLockedPackages.clear();
        parentCyberbullyLockedPackages.clear();
        parentSummaryCyberbullyLockedPackages.clear();
        directCyberbullyUnlockedPackages.clear();
        parentCyberbullyUnlockedPackages.clear();
        parentSummaryCyberbullyUnlockedPackages.clear();
        directCyberbullyLockedAtByPackage.clear();
        parentCyberbullyLockedAtByPackage.clear();
        parentSummaryCyberbullyLockedAtByPackage.clear();
        directCyberbullyUnlockCreatedByPackage.clear();
        parentCyberbullyUnlockCreatedByPackage.clear();
        parentSummaryCyberbullyUnlockCreatedByPackage.clear();
        monitoredCyberbullyLockChildUid = null;
        monitoredCyberbullyLockParentUid = null;
    }

    private void applyThreatLockIfNeeded(String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            return;
        }
        if (!activeCyberbullyLockedPackages.contains(packageName)) {
            return;
        }
        showCyberbullyWarningOverlayIfActive(packageName, resolveAppName(packageName));
    }

    private void showCyberbullyWarningOverlayIfActive(String packageName, String appName) {
        handler.post(() -> {
            if (!TextUtils.equals(activePackageName, packageName)) {
                Log.d("ThreatOverlay", "Threat lock saved without overlay because app is not foreground: " + packageName);
                return;
            }
            showCyberbullyWarningOverlay(packageName, appName);
        });
    }

    private void showCyberbullyWarningOverlay(String packageName, String appName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Log.d("ThreatOverlay", "Overlay permission missing; pushing user home for " + packageName);
            sendUserHomeRepeatedly();
            return;
        }

        if (isThreatOverlayShowing && threatOverlayView != null) {
            Log.d("ThreatOverlay", "Threat overlay already showing for " + packageName);
            sendUserHomeRepeatedly();
            return;
        }

        int layoutFlag = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );

        threatOverlayView = LayoutInflater.from(this).inflate(R.layout.layout_threat_warning_overlay, null);
        TextView tvCountdown = threatOverlayView.findViewById(R.id.tvCyberbullyCountdown);

        try {
            windowManager.addView(threatOverlayView, params);
            isThreatOverlayShowing = true;
            Log.d("ThreatOverlay", "Threat overlay shown for " + packageName);
        } catch (Exception e) {
            Log.w("ThreatOverlay", "Failed to add threat overlay", e);
            threatOverlayView = null;
            isThreatOverlayShowing = false;
            return;
        }

        sendUserHomeRepeatedly();

        final int[] countdown = {10};
        if (tvCountdown != null) {
            tvCountdown.setText("Returning to home in " + countdown[0] + "...");
        }
        if (cyberbullyCountdownRunnable != null) {
            handler.removeCallbacks(cyberbullyCountdownRunnable);
        }
        cyberbullyCountdownRunnable = new Runnable() {
            @Override
            public void run() {
                countdown[0]--;
                if (countdown[0] <= 0) {
                    hideCyberbullyWarningOverlay();
                    sendUserHomeRepeatedly();
                    return;
                }
                if (isThreatOverlayShowing && threatOverlayView != null) {
                    TextView tv = threatOverlayView.findViewById(R.id.tvCyberbullyCountdown);
                    if (tv != null) {
                        tv.setText("Returning to home in " + countdown[0] + "...");
                    }
                }
                sendUserHome();
                handler.postDelayed(this, 1000L);
            }
        };
        handler.postDelayed(cyberbullyCountdownRunnable, 1000L);
    }

    private void hideCyberbullyWarningOverlay() {
        if (cyberbullyCountdownRunnable != null) {
            handler.removeCallbacks(cyberbullyCountdownRunnable);
            cyberbullyCountdownRunnable = null;
        }
        if (isThreatOverlayShowing && threatOverlayView != null) {
            try {
                windowManager.removeViewImmediate(threatOverlayView);
                Log.d("ThreatOverlay", "Threat overlay removed");
            } catch (Exception ignored) {
            }
            threatOverlayView = null;
            isThreatOverlayShowing = false;
        }
    }
}
