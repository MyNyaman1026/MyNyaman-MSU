package com.mynyaman.mynyamanapp;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ParentThreatAlertService extends Service {

    public static final String ACTION_START = "com.mynyaman.mynyamanapp.action.PARENT_ALERT_START";
    private static final String TAG = "ParentThreatAlert";
    private static final int NOTIFICATION_ID = 4402;

    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private final Set<String> deliveredIncidentIds = new HashSet<>();
    private final Set<String> deliveredReportIds = new HashSet<>();
    private final List<ListenerRegistration> childThreatListeners = new ArrayList<>();

    private ListenerRegistration childrenListenerRegistration;
    private ListenerRegistration consultantReportListenerRegistration;
    private String monitoredParentUid;
    private boolean foregroundReady = false;
    private boolean fcmFallbackLogged = false;

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            startForeground(
                    NOTIFICATION_ID,
                    ThreatNotificationHelper.buildParentMonitorNotification(
                            this,
                            "Masked threat alerts will appear here when linked child incidents are detected."
                    ).build()
            );
            foregroundReady = true;
        } catch (RuntimeException ignored) {
            stopSelf();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!foregroundReady) {
            stopSelf();
            return START_NOT_STICKY;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        firestore.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!RoleUtils.ROLE_PARENT.equals(RoleUtils.readNormalizedRole(documentSnapshot))) {
                        Log.d(TAG, "Stopping parent alert service for non-parent account.");
                        stopSelf();
                        return;
                    }
                    Log.d(TAG, "PARENT_ALERT_SERVICE_STARTED: parentUid=" + user.getUid());
                    startListening(user.getUid());
                })
                .addOnFailureListener(exception -> {
                    Log.w(TAG, "Unable to verify parent role for alert service.", exception);
                    stopSelf();
                });
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopListening();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startListening(String parentUid) {
        if (TextUtils.equals(monitoredParentUid, parentUid) && childrenListenerRegistration != null) {
            return;
        }

        stopListening();
        monitoredParentUid = parentUid;
        if (!fcmFallbackLogged) {
            Log.w(TAG, "PARENT_FCM_NOT_CONFIGURED_USING_FIRESTORE_LISTENER_FALLBACK");
            fcmFallbackLogged = true;
        }

        childrenListenerRegistration = firestore.collection("users")
                .document(parentUid)
                .collection("children")
                .addSnapshotListener((childSnapshots, childError) -> {
                    if (childError != null || childSnapshots == null) {
                        Log.w(TAG, "Unable to read children for parent alert listener.", childError);
                        return;
                    }
                    if (!TextUtils.equals(monitoredParentUid, parentUid)) {
                        return;
                    }

                    Set<String> currentChildUids = new HashSet<>();
                    for (DocumentSnapshot child : childSnapshots.getDocuments()) {
                        currentChildUids.add(child.getId());
                    }

                    removeChildThreatListeners();
                    for (String childUid : currentChildUids) {
                        attachChildThreatListener(parentUid, childUid);
                    }
                    Log.d(TAG, "PARENT_ALERT_LISTENERS_ATTACHED: parentUid=" + parentUid
                            + ", childCount=" + currentChildUids.size());
                });
        attachConsultantReportListener(parentUid);
    }

    private void attachConsultantReportListener(String parentUid) {
        if (consultantReportListenerRegistration != null) {
            consultantReportListenerRegistration.remove();
            consultantReportListenerRegistration = null;
        }

        consultantReportListenerRegistration = firestore.collection("consultant_reports")
                .whereEqualTo("parentUid", parentUid)
                .addSnapshotListener((queryDocumentSnapshots, error) -> {
                    if (error != null || queryDocumentSnapshots == null) {
                        Log.w(TAG, "Unable to listen for child-lodged consultant reports. parentUid=" + parentUid, error);
                        return;
                    }
                    FirebaseUser activeUser = FirebaseAuth.getInstance().getCurrentUser();
                    if (activeUser == null || !TextUtils.equals(activeUser.getUid(), monitoredParentUid)) {
                        stopSelf();
                        return;
                    }

                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        if (!Boolean.TRUE.equals(document.getBoolean("parentReportNotificationPending"))) {
                            continue;
                        }
                        String reporterRole = RoleUtils.normalizeRole(document.getString("reporterRole"));
                        if (!RoleUtils.ROLE_CHILD.equals(reporterRole)) {
                            continue;
                        }

                        String reportId = stringOrFallback(document.getString("reportId"), document.getId());
                        String deliveryKey = "consultant_report_" + reportId;
                        if (!deliveredReportIds.add(deliveryKey)) {
                            Log.d(TAG, "PARENT_REPORT_ALERT_SKIPPED_ALREADY_DELIVERED: reportId=" + reportId);
                            markConsultantReportNotified(document, reportId);
                            continue;
                        }

                        Log.d(TAG, "PARENT_REPORT_ALERT_NOTIFICATION_ATTEMPT: reportId=" + reportId);
                        boolean notificationShown = ThreatNotificationHelper.showParentReportAlert(
                                this,
                                reportId,
                                document.getString("childUid"),
                                document.getString("childName"),
                                document.getString("reporterDisplayName")
                        );
                        if (notificationShown) {
                            Log.d(TAG, "PARENT_REPORT_ALERT_NOTIFICATION_SHOWN: reportId=" + reportId);
                            markConsultantReportNotified(document, reportId);
                        } else {
                            Log.w(TAG, "PARENT_REPORT_ALERT_NOTIFICATION_FAILED: reason=notification_permission_or_manager_unavailable, reportId=" + reportId);
                            deliveredReportIds.remove(deliveryKey);
                        }
                    }
                });
    }

    private void attachChildThreatListener(String parentUid, String childUid) {
        Query parentMirrorQuery = firestore.collection("users")
                .document(parentUid)
                .collection("children")
                .document(childUid)
                .collection("threat_reports")
                .whereEqualTo("parentNotificationPending", true);
        attachPendingThreatListener(parentUid, childUid, parentMirrorQuery, "parent_mirror");

        Query directChildQuery = firestore.collection("users")
                .document(childUid)
                .collection("threat_reports")
                .whereEqualTo("parentNotificationPending", true);
        attachPendingThreatListener(parentUid, childUid, directChildQuery, "direct_child");
    }

    private void attachPendingThreatListener(String parentUid, String childUid, Query query, String sourceLabel) {
        ListenerRegistration registration = query
                .addSnapshotListener((queryDocumentSnapshots, error) -> {
                    if (error != null || queryDocumentSnapshots == null) {
                        Log.w(TAG, "Unable to listen for child threat alerts. source=" + sourceLabel
                                + ", childUid=" + childUid, error);
                        return;
                    }
                    FirebaseUser activeUser = FirebaseAuth.getInstance().getCurrentUser();
                    if (activeUser == null || !TextUtils.equals(activeUser.getUid(), monitoredParentUid)) {
                        stopSelf();
                        return;
                    }

                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        String requestId = stringOrFallback(document.getString("requestId"), document.getId());
                        String documentChildUid = stringOrFallback(document.getString("childUid"), childUid);
                        String levelText = stringOrFallback(document.getString("threatLevel"), document.getString("level"));
                        Log.d(TAG, "PARENT_ALERT_DOC_RECEIVED: requestId=" + requestId
                                + ", childUid=" + documentChildUid
                                + ", parentUid=" + parentUid
                                + ", level=" + levelText
                                + ", source=" + sourceLabel);

                        ThreatResult.Level level = parseLevel(levelText);
                        if (level == ThreatResult.Level.GREEN) {
                            Log.d(TAG, "PARENT_ALERT_SKIPPED_GREEN: requestId=" + requestId);
                            continue;
                        }

                        String deliveryKey = childUid + "_" + stringOrFallback(requestId, document.getId());
                        if (!deliveredIncidentIds.add(deliveryKey)) {
                            Log.d(TAG, "PARENT_ALERT_SKIPPED_ALREADY_DELIVERED: requestId=" + requestId);
                            markAlertNotified(parentUid, documentChildUid, document, requestId);
                            continue;
                        }

                        Log.d(TAG, "PARENT_ALERT_NOTIFICATION_ATTEMPT: requestId=" + requestId);
                        boolean notificationShown = ThreatNotificationHelper.showParentAlert(
                                this,
                                requestId,
                                documentChildUid,
                                document.getString("childName"),
                                document.getString("appName"),
                                document.getString("packageName"),
                                document.getString("maskedPreview"),
                                level,
                                formatTimestamp(document.getTimestamp("timestamp"))
                        );
                        if (notificationShown) {
                            Log.d(TAG, "PARENT_ALERT_NOTIFICATION_SHOWN: requestId=" + requestId);
                            markAlertNotified(parentUid, documentChildUid, document, requestId);
                        } else {
                            Log.w(TAG, "PARENT_ALERT_NOTIFICATION_FAILED: reason=notification_permission_or_manager_unavailable, requestId=" + requestId);
                            deliveredIncidentIds.remove(deliveryKey);
                        }
                    }
                });
        childThreatListeners.add(registration);
    }

    private void markAlertNotified(String parentUid, String childUid, DocumentSnapshot document, String requestId) {
        if (document == null || TextUtils.isEmpty(childUid)) {
            return;
        }

        Set<String> markedPaths = new HashSet<>();
        markAlertReference(document.getReference(), markedPaths, requestId);
        markAlertReference(
                firestore.collection("users")
                        .document(childUid)
                        .collection("threat_reports")
                        .document(document.getId()),
                markedPaths,
                requestId
        );
        if (!TextUtils.isEmpty(parentUid)) {
            markAlertReference(
                    firestore.collection("users")
                            .document(parentUid)
                            .collection("children")
                            .document(childUid)
                            .collection("threat_reports")
                            .document(document.getId()),
                    markedPaths,
                    requestId
            );
        }
    }

    private void markAlertReference(DocumentReference reference, Set<String> markedPaths, String requestId) {
        if (reference == null || markedPaths == null || !markedPaths.add(reference.getPath())) {
            return;
        }

        Task<Void> markTask = BlueprintSyncRepository.markThreatIncidentParentNotified(reference);
        if (markTask != null) {
            markTask.addOnSuccessListener(unused -> Log.d(TAG, "PARENT_ALERT_MARKED_NOTIFIED: requestId=" + requestId
                            + ", path=" + reference.getPath()))
                    .addOnFailureListener(exception -> Log.w(TAG,
                            "PARENT_ALERT_NOTIFICATION_FAILED: reason=mark_notified_failed, requestId=" + requestId
                                    + ", path=" + reference.getPath(),
                            exception));
        }
    }

    private void removeChildThreatListeners() {
        for (ListenerRegistration registration : childThreatListeners) {
            registration.remove();
        }
        childThreatListeners.clear();
    }

    private void stopListening() {
        removeChildThreatListeners();
        if (consultantReportListenerRegistration != null) {
            consultantReportListenerRegistration.remove();
            consultantReportListenerRegistration = null;
        }
        if (childrenListenerRegistration != null) {
            childrenListenerRegistration.remove();
            childrenListenerRegistration = null;
        }
        monitoredParentUid = null;
    }

    private ThreatResult.Level parseLevel(String value) {
        if ("RED".equalsIgnoreCase(value)) {
            return ThreatResult.Level.RED;
        }
        if ("YELLOW".equalsIgnoreCase(value)) {
            return ThreatResult.Level.YELLOW;
        }
        return ThreatResult.Level.GREEN;
    }

    private String formatTimestamp(Timestamp timestamp) {
        if (timestamp == null) {
            return "";
        }
        return new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(timestamp.toDate());
    }

    private String stringOrFallback(String value, String fallback) {
        return TextUtils.isEmpty(value) ? fallback : value;
    }

    private void markConsultantReportNotified(DocumentSnapshot document, String reportId) {
        if (document == null) {
            return;
        }
        Task<Void> markTask = BlueprintSyncRepository.markConsultantReportParentNotified(document.getReference());
        if (markTask != null) {
            markTask.addOnSuccessListener(unused -> Log.d(TAG, "PARENT_REPORT_ALERT_MARKED_NOTIFIED: reportId=" + reportId))
                    .addOnFailureListener(exception -> Log.w(TAG,
                            "PARENT_REPORT_ALERT_NOTIFICATION_FAILED: reason=mark_notified_failed, reportId=" + reportId,
                            exception));
        }
    }
}
