package com.mynyaman.mynyamanapp;

import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class BlueprintSyncRepository {

    private static final String CONSULTANT_REPORTS_COLLECTION = "consultant_reports";
    private static final String REPORT_STATUS_PENDING = "pending";
    private static final String REPORT_STATUS_SOLVED = "solved";
    private static final String REPORT_WORKFLOW_NEW = "new";
    private static final String REPORT_WORKFLOW_CLAIMED = "claimed";
    private static final String REPORT_WORKFLOW_FOLLOW_UP = "follow_up_requested";
    private static final String REPORT_WORKFLOW_SOLVED = "solved";
    private static final String REPORT_CLAIM_UNCLAIMED = "unclaimed";
    private static final String REPORT_CLAIM_CLAIMED = "claimed";

    public interface WeeklyUsageCallback {
        void onSuccess(WeeklyUsageSummary summary);
        void onFailure(Exception exception);
    }

    public interface ConsultantReportClaimCallback {
        void onSuccess(ConsultantReportClaimResult result);
        void onFailure(Exception exception);
    }

    public interface ConsultantReportWriteCallback {
        void onSuccess();
        void onFailure(Exception exception);
    }

    public static final class WeeklyUsageSummary {
        public final long totalSeconds;
        public final String mostUsedApp;
        public final long mostUsedAppMillis;

        WeeklyUsageSummary(long totalSeconds, String mostUsedApp, long mostUsedAppMillis) {
            this.totalSeconds = totalSeconds;
            this.mostUsedApp = mostUsedApp;
            this.mostUsedAppMillis = mostUsedAppMillis;
        }
    }

    public static final class ConsultantReportClaimResult {
        public final boolean availableToCurrentConsultant;
        public final boolean newlyClaimed;
        public final boolean alreadyClaimedByCurrentConsultant;
        public final boolean solved;
        public final String claimedByConsultantUid;
        public final String claimedByConsultantName;

        ConsultantReportClaimResult(
                boolean availableToCurrentConsultant,
                boolean newlyClaimed,
                boolean alreadyClaimedByCurrentConsultant,
                boolean solved,
                String claimedByConsultantUid,
                String claimedByConsultantName
        ) {
            this.availableToCurrentConsultant = availableToCurrentConsultant;
            this.newlyClaimed = newlyClaimed;
            this.alreadyClaimedByCurrentConsultant = alreadyClaimedByCurrentConsultant;
            this.solved = solved;
            this.claimedByConsultantUid = claimedByConsultantUid;
            this.claimedByConsultantName = claimedByConsultantName;
        }
    }

    private BlueprintSyncRepository() {
    }

    private static String stringOrFallback(String value, String fallback) {
        return TextUtils.isEmpty(value) ? fallback : value;
    }

    public static void syncSignedInUser(FirebaseFirestore firestore, FirebaseUser user, String role, String fallbackName) {
        if (firestore == null || user == null) {
            return;
        }

        Map<String, Object> userData = new HashMap<>();
        String normalizedRole = RoleUtils.normalizeRole(role);
        userData.put("openId", user.getUid());
        userData.put("firebaseUid", user.getUid());
        userData.put("email", user.getEmail());
        userData.put("name", !TextUtils.isEmpty(fallbackName) ? fallbackName : user.getDisplayName());
        if (!TextUtils.isEmpty(normalizedRole)) {
            userData.put("role", normalizedRole);
            userData.put("roleKey", normalizedRole);
            userData.put("roleLabel", RoleUtils.toLegacyRoleLabel(normalizedRole));
        }
        userData.put("emailVerified", user.isEmailVerified());
        userData.put("loginMethod", "email");
        userData.put("lastSignedIn", FieldValue.serverTimestamp());
        userData.put("updatedAt", FieldValue.serverTimestamp());
        firestore.collection("users")
                .document(user.getUid())
                .set(userData, SetOptions.merge());
    }

    public static void syncParentChildRelationship(
            FirebaseFirestore firestore,
            String parentUid,
            String childUid,
            String nickname,
            String parentEmail,
            String childEmail
    ) {
        if (firestore == null || TextUtils.isEmpty(parentUid) || TextUtils.isEmpty(childUid)) {
            return;
        }

        Map<String, Object> relationship = new HashMap<>();
        relationship.put("parentUid", parentUid);
        relationship.put("childUid", childUid);
        relationship.put("parentId", parentUid);
        relationship.put("childId", childUid);
        relationship.put("nickname", nickname);
        relationship.put("parentEmail", parentEmail);
        relationship.put("childEmail", childEmail);
        relationship.put("createdAt", FieldValue.serverTimestamp());

        firestore.collection("parent_child_relationships")
                .document(parentUid + "_" + childUid)
                .set(relationship, SetOptions.merge());
    }

    public static void removeParentChildRelationship(FirebaseFirestore firestore, String parentUid, String childUid) {
        if (firestore == null || TextUtils.isEmpty(parentUid) || TextUtils.isEmpty(childUid)) {
            return;
        }
        firestore.collection("parent_child_relationships")
                .document(parentUid + "_" + childUid)
                .delete();
    }

    public static void syncAppTimeLimits(
            FirebaseFirestore firestore,
            String parentUid,
            String childUid,
            String childName,
            Map<String, AppLimitPayload> limitsByPackage
    ) {
        if (firestore == null || TextUtils.isEmpty(childUid) || limitsByPackage == null) {
            return;
        }

        WriteBatch batch = firestore.batch();
        for (Map.Entry<String, AppLimitPayload> entry : limitsByPackage.entrySet()) {
            String packageName = entry.getKey();
            AppLimitPayload payload = entry.getValue();
            Map<String, Object> data = new HashMap<>();
            data.put("childId", childUid);
            data.put("childUid", childUid);
            data.put("parentUid", parentUid);
            data.put("childName", childName);
            data.put("appName", payload.appName);
            data.put("packageName", packageName);
            data.put("limitMinutes", payload.limitSeconds / 60L);
            data.put("limitSeconds", payload.limitSeconds);
            data.put("isEnabled", payload.isEnabled);
            data.put("updatedAt", FieldValue.serverTimestamp());
            if (payload.isNewRecord) {
                data.put("createdAt", FieldValue.serverTimestamp());
            }
            batch.set(
                    firestore.collection("app_time_limits").document(childUid + "_" + packageName),
                    data,
                    SetOptions.merge()
            );
        }
        batch.commit();
    }

    public static void syncUsageReport(
            FirebaseFirestore firestore,
            String childUid,
            String parentUid,
            String childName,
            long totalUsageMillis,
            List<Map<String, Object>> topAppsData
    ) {
        if (firestore == null || TextUtils.isEmpty(childUid)) {
            return;
        }

        long totalSeconds = totalUsageMillis / 1000L;
        Timestamp now = Timestamp.now();
        String dayKey = UsageWindowHelper.currentDayKey();
        String weekStartKey = UsageWindowHelper.currentWeekStartKey();

        Map<String, Object> usageReport = new HashMap<>();
        usageReport.put("childId", childUid);
        usageReport.put("childUid", childUid);
        usageReport.put("parentUid", parentUid);
        usageReport.put("childName", childName);
        usageReport.put("reportDate", now);
        usageReport.put("reportDateKey", dayKey);
        usageReport.put("reportWeekStartKey", weekStartKey);
        usageReport.put("totalScreenTime", totalSeconds / 60L);
        usageReport.put("totalScreenTimeSeconds", totalSeconds);
        usageReport.put("appUsageData", topAppsData == null ? Collections.emptyList() : topAppsData);
        usageReport.put("updatedAt", now);
        usageReport.put("createdAt", now);

        firestore.collection("usage_reports")
                .document(childUid + "_" + dayKey)
                .set(usageReport, SetOptions.merge());
    }

    public static void fetchWeeklyUsageSummary(FirebaseFirestore firestore, String childUid, WeeklyUsageCallback callback) {
        fetchWeeklyUsageSummary(firestore, childUid, -1L, null, callback);
    }

    public static void fetchWeeklyUsageSummary(
            FirebaseFirestore firestore,
            String childUid,
            long currentDayUsageSeconds,
            Object currentDayTopAppsData,
            WeeklyUsageCallback callback
    ) {
        if (firestore == null || TextUtils.isEmpty(childUid)) {
            callback.onSuccess(new WeeklyUsageSummary(0L, "None", 0L));
            return;
        }

        Timestamp startTimestamp = new Timestamp(UsageWindowHelper.startOfCurrentWeek());
        String currentWeekStartKey = UsageWindowHelper.currentWeekStartKey();
        String currentDayKey = UsageWindowHelper.currentDayKey();

        firestore.collection("usage_reports")
                .whereEqualTo("childUid", childUid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    long weeklySeconds = 0L;
                    long currentDayReportSeconds = 0L;
                    Map<String, Long> appTotals = new HashMap<>();
                    Map<String, Long> currentDayReportAppTotals = new HashMap<>();

                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        if (!isCurrentWeekUsageReport(document, currentWeekStartKey, startTimestamp)) {
                            continue;
                        }

                        long reportSeconds = readUsageReportSeconds(document);
                        Object rawAppUsage = document.get("appUsageData");
                        if (TextUtils.equals(document.getString("reportDateKey"), currentDayKey)) {
                            currentDayReportSeconds = Math.max(currentDayReportSeconds, reportSeconds);
                            addAppUsageTotals(currentDayReportAppTotals, rawAppUsage);
                        } else {
                            weeklySeconds += reportSeconds;
                            addAppUsageTotals(appTotals, rawAppUsage);
                        }
                    }

                    if (currentDayUsageSeconds >= 0L) {
                        long liveCurrentDaySeconds = Math.max(0L, currentDayUsageSeconds);
                        if (liveCurrentDaySeconds >= currentDayReportSeconds) {
                            weeklySeconds += liveCurrentDaySeconds;
                            if (hasAppUsageData(currentDayTopAppsData)) {
                                addAppUsageTotals(appTotals, currentDayTopAppsData);
                            } else {
                                mergeAppTotals(appTotals, currentDayReportAppTotals);
                            }
                        } else {
                            weeklySeconds += currentDayReportSeconds;
                            mergeAppTotals(appTotals, currentDayReportAppTotals);
                        }
                    } else {
                        weeklySeconds += currentDayReportSeconds;
                        mergeAppTotals(appTotals, currentDayReportAppTotals);
                    }

                    callback.onSuccess(buildWeeklyUsageSummary(weeklySeconds, appTotals));
                })
                .addOnFailureListener(callback::onFailure);
    }

    private static boolean isCurrentWeekUsageReport(DocumentSnapshot document, String currentWeekStartKey, Timestamp startTimestamp) {
        String reportWeekStartKey = document.getString("reportWeekStartKey");
        if (!TextUtils.isEmpty(reportWeekStartKey)) {
            return TextUtils.equals(reportWeekStartKey, currentWeekStartKey);
        }

        Timestamp reportDate = document.getTimestamp("reportDate");
        return reportDate != null && !reportDate.toDate().before(startTimestamp.toDate());
    }

    private static long readUsageReportSeconds(DocumentSnapshot document) {
        Long seconds = document.getLong("totalScreenTimeSeconds");
        if (seconds != null) {
            return Math.max(0L, seconds);
        }

        Long minutes = document.getLong("totalScreenTime");
        return minutes == null ? 0L : Math.max(0L, minutes * 60L);
    }

    private static void addAppUsageTotals(Map<String, Long> appTotals, Object rawAppUsage) {
        if (!(rawAppUsage instanceof List)) {
            return;
        }

        for (Object item : (List<?>) rawAppUsage) {
            if (!(item instanceof Map)) {
                continue;
            }
            Map<?, ?> map = (Map<?, ?>) item;
            String appName = map.get("appName") == null ? "App" : String.valueOf(map.get("appName"));
            Object millisValue = map.get("timeInForeground");
            long millis = millisValue instanceof Number ? ((Number) millisValue).longValue() : 0L;
            if (millis > 0L) {
                appTotals.put(appName, appTotals.getOrDefault(appName, 0L) + millis);
            }
        }
    }

    private static boolean hasAppUsageData(Object rawAppUsage) {
        return rawAppUsage instanceof List && !((List<?>) rawAppUsage).isEmpty();
    }

    private static void mergeAppTotals(Map<String, Long> target, Map<String, Long> source) {
        for (Map.Entry<String, Long> entry : source.entrySet()) {
            target.put(entry.getKey(), target.getOrDefault(entry.getKey(), 0L) + entry.getValue());
        }
    }

    private static WeeklyUsageSummary buildWeeklyUsageSummary(long weeklySeconds, Map<String, Long> appTotals) {
        String mostUsedApp = "None";
        long mostUsedMillis = 0L;
        for (Map.Entry<String, Long> entry : appTotals.entrySet()) {
            if (entry.getValue() > mostUsedMillis) {
                mostUsedMillis = entry.getValue();
                mostUsedApp = entry.getKey();
            }
        }

        return new WeeklyUsageSummary(weeklySeconds, mostUsedApp, mostUsedMillis);
    }

    public static void saveAiConsultantSession(
            FirebaseFirestore firestore,
            String ownerUid,
            String ownerRole,
            String sessionId,
            String title,
            List<ConsultantMessageRecord> messages
    ) {
        if (firestore == null || TextUtils.isEmpty(ownerUid) || TextUtils.isEmpty(sessionId) || messages == null) {
            return;
        }

        Timestamp now = Timestamp.now();
        String conversationType = RoleUtils.ROLE_CHILD.equals(RoleUtils.normalizeRole(ownerRole))
                ? "child_consultant"
                : "parent_consultant";

        Map<String, Object> conversation = new HashMap<>();
        conversation.put("id", sessionId);
        conversation.put("conversationId", sessionId);
        conversation.put("ownerUid", ownerUid);
        conversation.put("ownerRole", RoleUtils.normalizeRole(ownerRole));
        conversation.put("participantIds", ownerUid + ",ai_consultant");
        conversation.put("conversationType", conversationType);
        conversation.put("consultantType", "ai");
        conversation.put("title", title);
        conversation.put("status", "closed");
        conversation.put("messageCount", messages.size());
        conversation.put("lastMessage", messages.isEmpty() ? "" : messages.get(messages.size() - 1).content);
        conversation.put("lastMessageAt", now);
        conversation.put("createdAt", now);
        conversation.put("updatedAt", now);

        Map<String, Object> session = new HashMap<>();
        session.put("id", sessionId);
        session.put("ownerUid", ownerUid);
        session.put("participantRole", RoleUtils.normalizeRole(ownerRole));
        session.put("consultantId", "ai_consultant");
        session.put("consultantType", "ai");
        session.put("topic", title);
        session.put("status", "closed");
        session.put("createdAt", now);
        session.put("updatedAt", now);

        WriteBatch batch = firestore.batch();
        batch.set(firestore.collection("conversations").document(sessionId), conversation, SetOptions.merge());
        batch.set(firestore.collection("consultant_sessions").document(sessionId), session, SetOptions.merge());
        for (int i = 0; i < messages.size(); i++) {
            ConsultantMessageRecord message = messages.get(i);
            Map<String, Object> messageData = new HashMap<>();
            messageData.put("conversationId", sessionId);
            messageData.put("ownerUid", ownerUid);
            messageData.put("senderRole", message.role);
            messageData.put("senderId", ownerUid);
            messageData.put("recipientId", "ai_consultant");
            messageData.put("message", message.content);
            messageData.put("messageType", "text");
            messageData.put("isRead", true);
            messageData.put("createdAt", message.createdAt != null ? message.createdAt : now);
            batch.set(
                    firestore.collection("chat_messages").document(sessionId + "_" + i),
                    messageData,
                    SetOptions.merge()
            );
        }
        batch.commit();
    }

    public static void syncThreatMonitoringStatus(
            FirebaseFirestore firestore,
            String childUid,
            String parentUid,
            String childName,
            Map<String, Object> statusData
    ) {
        if (firestore == null || TextUtils.isEmpty(childUid) || statusData == null) {
            return;
        }

        Map<String, Object> childData = new HashMap<>(statusData);
        childData.put("threatStatusUpdatedAt", FieldValue.serverTimestamp());
        firestore.collection("users")
                .document(childUid)
                .set(childData, SetOptions.merge());

        if (!TextUtils.isEmpty(parentUid)) {
            mirrorThreatMonitoringStatusToParent(firestore, parentUid, childUid, childName, childData);
        } else {
            resolveParentAndMirrorThreatMonitoringStatus(firestore, childUid, childName, childData);
        }
    }

    private static void resolveParentAndMirrorThreatMonitoringStatus(
            FirebaseFirestore firestore,
            String childUid,
            String childName,
            Map<String, Object> childData
    ) {
        firestore.collection("users")
                .document(childUid)
                .get()
                .addOnSuccessListener(childDocument -> {
                    String resolvedParentUid = childDocument.getString("parentUid");
                    String resolvedChildName = stringOrFallback(childName, childDocument.getString("name"));
                    if (TextUtils.isEmpty(resolvedParentUid)) {
                        resolveParentFromRelationshipAndMirrorThreatMonitoringStatus(
                                firestore,
                                childUid,
                                resolvedChildName,
                                childData
                        );
                        return;
                    }
                    mirrorThreatMonitoringStatusToParent(
                            firestore,
                            resolvedParentUid,
                            childUid,
                            resolvedChildName,
                            childData
                    );
                })
                .addOnFailureListener(e -> {
                    Log.w("ThreatFirestore", "Unable to resolve parentUid for threat monitoring status. Trying relationship mirror. childUid=" + childUid, e);
                    resolveParentFromRelationshipAndMirrorThreatMonitoringStatus(
                            firestore,
                            childUid,
                            childName,
                            childData
                    );
                });
    }

    private static void resolveParentFromRelationshipAndMirrorThreatMonitoringStatus(
            FirebaseFirestore firestore,
            String childUid,
            String childName,
            Map<String, Object> childData
    ) {
        firestore.collection("parent_child_relationships")
                .whereEqualTo("childUid", childUid)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot == null || querySnapshot.isEmpty()) {
                        Log.w("ThreatFirestore", "Threat monitoring status still has no linked parentUid. childUid=" + childUid);
                        return;
                    }
                    DocumentSnapshot relationship = querySnapshot.getDocuments().get(0);
                    String resolvedParentUid = relationship.getString("parentUid");
                    String resolvedChildName = stringOrFallback(childName, relationship.getString("nickname"));
                    resolvedChildName = stringOrFallback(resolvedChildName, relationship.getString("childName"));
                    if (TextUtils.isEmpty(resolvedParentUid)) {
                        Log.w("ThreatFirestore", "Relationship document had no parentUid for threat monitoring status. childUid=" + childUid);
                        return;
                    }
                    mirrorThreatMonitoringStatusToParent(
                            firestore,
                            resolvedParentUid,
                            childUid,
                            resolvedChildName,
                            childData
                    );
                })
                .addOnFailureListener(e -> Log.w(
                        "ThreatFirestore",
                        "Unable to resolve parentUid from parent_child_relationships for threat monitoring status. childUid=" + childUid,
                        e
                ));
    }

    private static void mirrorThreatMonitoringStatusToParent(
            FirebaseFirestore firestore,
            String parentUid,
            String childUid,
            String childName,
            Map<String, Object> childData
    ) {
        Map<String, Object> parentChildData = new HashMap<>(childData);
        parentChildData.put("name", childName);
        firestore.collection("users")
                .document(parentUid)
                .collection("children")
                .document(childUid)
                .set(parentChildData, SetOptions.merge())
                .addOnFailureListener(e -> Log.w(
                        "ThreatFirestore",
                        "Failed to mirror threat monitoring status for parent. childUid=" + childUid,
                        e
                ));
    }

    public static void logThreatIncident(FirebaseFirestore firestore, ThreatIncidentPayload payload) {
        if (firestore == null || payload == null || TextUtils.isEmpty(payload.childUid)) {
            return;
        }

        Timestamp detectedAt = payload.detectedAt != null ? payload.detectedAt : Timestamp.now();
        String incidentId = !TextUtils.isEmpty(payload.incidentId)
                ? payload.incidentId
                : !TextUtils.isEmpty(payload.requestId)
                        ? payload.childUid + "_" + payload.requestId
                        : payload.childUid + "_" + UUID.randomUUID();
        String status = TextUtils.isEmpty(payload.status) ? "unresolved" : payload.status;
        String contentType = TextUtils.isEmpty(payload.contentType) ? "text" : payload.contentType;
        String aiProvider = TextUtils.isEmpty(payload.aiProvider) ? "local" : payload.aiProvider;
        String aiModel = TextUtils.isEmpty(payload.aiModel) ? "" : payload.aiModel;
        String aiReason = TextUtils.isEmpty(payload.aiReason) ? payload.geminiReason : payload.aiReason;
        String aiRawJson = TextUtils.isEmpty(payload.aiRawJson) ? payload.geminiReportJson : payload.aiRawJson;
        boolean parentNotificationPending = "RED".equalsIgnoreCase(payload.threatLevel)
                || "Red".equalsIgnoreCase(payload.threatLevel)
                || "YELLOW".equalsIgnoreCase(payload.threatLevel)
                || "Yellow".equalsIgnoreCase(payload.threatLevel);

        Map<String, Object> threatReport = new HashMap<>();
        threatReport.put("level", payload.threatLevel);
        threatReport.put("reason", aiReason);
        threatReport.put("maskedText", payload.maskedPreview);
        threatReport.put("aiProvider", aiProvider);
        threatReport.put("aiModel", aiModel);

        Map<String, Object> incidentData = new HashMap<>();
        incidentData.put("incidentId", incidentId);
        incidentData.put("requestId", payload.requestId);
        incidentData.put("childId", payload.childUid);
        incidentData.put("childUid", payload.childUid);
        incidentData.put("parentUid", payload.parentUid);
        incidentData.put("childName", payload.childName);
        incidentData.put("appName", payload.appName);
        incidentData.put("packageName", payload.packageName);
        incidentData.put("contentType", contentType);
        incidentData.put("sourceType", "automatic_ai_detection");
        incidentData.put("sourceUri", payload.sourceUri);
        incidentData.put("detectedText", payload.messageText);
        incidentData.put("messageText", payload.messageText);
        incidentData.put("fullMessage", payload.messageText);
        incidentData.put("maskedPreview", payload.maskedPreview);
        incidentData.put("level", payload.threatLevel);
        incidentData.put("threatLevel", payload.threatLevel);
        incidentData.put("severityLabel", payload.threatLevel);
        incidentData.put("timestamp", detectedAt);
        incidentData.put("sender", payload.sender);
        incidentData.put("direction", payload.direction);
        incidentData.put("status", status);
        incidentData.put("consultantSuggested", payload.consultantSuggested);
        incidentData.put("analysisSummary", payload.analysisSummary);
        incidentData.put("aiProvider", aiProvider);
        incidentData.put("aiModel", aiModel);
        incidentData.put("aiReason", aiReason);
        incidentData.put("aiRawJson", aiRawJson);
        incidentData.put("groqReason", "groq".equalsIgnoreCase(aiProvider) ? aiReason : "");
        incidentData.put("geminiReason", aiReason);
        incidentData.put("threatReport", threatReport);
        incidentData.put("threatReportJson", aiRawJson);
        incidentData.put("parentNotificationPending", parentNotificationPending);
        incidentData.put("parentNotifiedAt", null);
        incidentData.put("createdAt", detectedAt);
        incidentData.put("updatedAt", detectedAt);

        DocumentReference incidentReference = firestore.collection("users")
                .document(payload.childUid)
                .collection("threat_reports")
                .document(incidentId);

        Map<String, Object> summaryData = new HashMap<>();
        summaryData.put("threatLastIncidentId", incidentId);
        summaryData.put("threatLastLevel", payload.threatLevel);
        summaryData.put("threatLastAppName", payload.appName);
        summaryData.put("threatLastPackageName", payload.packageName);
        summaryData.put("threatLastContentType", contentType);
        summaryData.put("threatLastMaskedPreview", payload.maskedPreview);
        summaryData.put("threatLastMessageText", payload.messageText);
        summaryData.put("threatLastDirection", payload.direction);
        summaryData.put("threatLastSender", payload.sender);
        summaryData.put("threatLastDetectedAt", detectedAt);
        summaryData.put("threatLastStatus", status);
        summaryData.put("threatConsultantSuggested", payload.consultantSuggested);
        summaryData.put("threatLastAnalysisSummary", payload.analysisSummary);
        summaryData.put("threatLastAiProvider", aiProvider);
        summaryData.put("threatLastAiModel", aiModel);
        summaryData.put("threatLastAiReason", aiReason);
        summaryData.put("threatLastGroqReason", "groq".equalsIgnoreCase(aiProvider) ? aiReason : "");
        summaryData.put("threatLastGeminiReason", aiReason);
        summaryData.put("threatLastDayKey", UsageWindowHelper.currentDayKey());
        summaryData.put("threatTotalAlertCount", FieldValue.increment(1));
        if ("RED".equalsIgnoreCase(payload.threatLevel) || "Red".equalsIgnoreCase(payload.threatLevel)) {
            summaryData.put("threatTotalRedCount", FieldValue.increment(1));
        } else if ("YELLOW".equalsIgnoreCase(payload.threatLevel) || "Yellow".equalsIgnoreCase(payload.threatLevel)) {
            summaryData.put("threatTotalYellowCount", FieldValue.increment(1));
        }

        incidentReference.set(incidentData, SetOptions.merge());

        firestore.collection("users")
                .document(payload.childUid)
                .set(summaryData, SetOptions.merge());

        if (!TextUtils.isEmpty(payload.parentUid)) {
            mirrorThreatIncidentToParent(
                    firestore,
                    incidentReference,
                    payload.parentUid,
                    payload.childUid,
                    payload.childName,
                    incidentId,
                    incidentData,
                    summaryData
            );
        } else {
            resolveParentAndMirrorThreatIncident(
                    firestore,
                    incidentReference,
                    payload.childUid,
                    payload.childName,
                    incidentId,
                    incidentData,
                    summaryData
            );
        }
    }

    private static void resolveParentAndMirrorThreatIncident(
            FirebaseFirestore firestore,
            DocumentReference incidentReference,
            String childUid,
            String childName,
            String incidentId,
            Map<String, Object> incidentData,
            Map<String, Object> summaryData
    ) {
        firestore.collection("users")
                .document(childUid)
                .get()
                .addOnSuccessListener(childDocument -> {
                    String resolvedParentUid = childDocument.getString("parentUid");
                    String resolvedChildName = stringOrFallback(childName, childDocument.getString("name"));
                    if (TextUtils.isEmpty(resolvedParentUid)) {
                        resolveParentFromRelationshipAndMirrorThreatIncident(
                                firestore,
                                incidentReference,
                                childUid,
                                resolvedChildName,
                                incidentId,
                                incidentData,
                                summaryData
                        );
                        return;
                    }
                    mirrorThreatIncidentToParent(
                            firestore,
                            incidentReference,
                            resolvedParentUid,
                            childUid,
                            resolvedChildName,
                            incidentId,
                            incidentData,
                            summaryData
                    );
                })
                .addOnFailureListener(e -> {
                    Log.w("ThreatFirestore", "Unable to resolve parentUid from child document. Trying relationship mirror. childUid=" + childUid, e);
                    resolveParentFromRelationshipAndMirrorThreatIncident(
                            firestore,
                            incidentReference,
                            childUid,
                            childName,
                            incidentId,
                            incidentData,
                            summaryData
                    );
                });
    }

    private static void resolveParentFromRelationshipAndMirrorThreatIncident(
            FirebaseFirestore firestore,
            DocumentReference incidentReference,
            String childUid,
            String childName,
            String incidentId,
            Map<String, Object> incidentData,
            Map<String, Object> summaryData
    ) {
        firestore.collection("parent_child_relationships")
                .whereEqualTo("childUid", childUid)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot == null || querySnapshot.isEmpty()) {
                        Log.w("ThreatFirestore", "Threat incident still has no linked parentUid. childUid=" + childUid);
                        return;
                    }
                    DocumentSnapshot relationship = querySnapshot.getDocuments().get(0);
                    String resolvedParentUid = relationship.getString("parentUid");
                    String resolvedChildName = stringOrFallback(childName, relationship.getString("nickname"));
                    if (TextUtils.isEmpty(resolvedParentUid)) {
                        Log.w("ThreatFirestore", "Relationship document had no parentUid. childUid=" + childUid);
                        return;
                    }
                    mirrorThreatIncidentToParent(
                            firestore,
                            incidentReference,
                            resolvedParentUid,
                            childUid,
                            resolvedChildName,
                            incidentId,
                            incidentData,
                            summaryData
                    );
                })
                .addOnFailureListener(e -> Log.w(
                        "ThreatFirestore",
                        "Unable to resolve parentUid from parent_child_relationships. childUid=" + childUid,
                        e
                ));
    }

    private static void mirrorThreatIncidentToParent(
            FirebaseFirestore firestore,
            DocumentReference incidentReference,
            String parentUid,
            String childUid,
            String childName,
            String incidentId,
            Map<String, Object> incidentData,
            Map<String, Object> summaryData
    ) {
        Map<String, Object> parentIncidentData = new HashMap<>(incidentData);
        parentIncidentData.put("parentUid", parentUid);
        parentIncidentData.put("childName", childName);

        Map<String, Object> parentChildSummary = new HashMap<>(summaryData);
        parentChildSummary.put("name", childName);

        incidentReference.set(parentIncidentData, SetOptions.merge());

        firestore.collection("users")
                .document(parentUid)
                .collection("children")
                .document(childUid)
                .set(parentChildSummary, SetOptions.merge());

        firestore.collection("users")
                .document(parentUid)
                .collection("children")
                .document(childUid)
                .collection("threat_reports")
                .document(incidentId)
                .set(parentIncidentData, SetOptions.merge())
                .addOnFailureListener(e -> Log.w(
                        "ThreatFirestore",
                        "Failed to mirror threat incident for parent. childUid=" + childUid,
                        e
                ));
    }

    public static Task<Void> markThreatIncidentParentNotified(DocumentReference incidentReference) {
        if (incidentReference == null) {
            return null;
        }
        Map<String, Object> update = new HashMap<>();
        update.put("parentNotificationPending", false);
        update.put("parentNotifiedAt", FieldValue.serverTimestamp());
        update.put("updatedAt", FieldValue.serverTimestamp());
        return incidentReference.set(update, SetOptions.merge());
    }

    public static String newConsultantReportId(FirebaseFirestore firestore) {
        if (firestore == null) {
            return UUID.randomUUID().toString();
        }
        return firestore.collection(CONSULTANT_REPORTS_COLLECTION).document().getId();
    }

    public static String buildConsultantReportPreview(String reportText) {
        if (TextUtils.isEmpty(reportText)) {
            return "";
        }

        String normalized = reportText.trim().replaceAll("\\s+", " ");
        if (normalized.length() <= 80) {
            return normalized;
        }
        return normalized.substring(0, 80).trim() + "...";
    }

    public static void createConsultantReport(
            FirebaseFirestore firestore,
            String reportId,
            String reporterUid,
            String reporterRole,
            String reporterDisplayName,
            String childUid,
            String childName,
            String parentUid,
            String parentName,
            String reportText,
            List<Map<String, Object>> evidenceImages,
            ConsultantReportWriteCallback callback
    ) {
        if (firestore == null
                || TextUtils.isEmpty(reportId)
                || TextUtils.isEmpty(reporterUid)
                || TextUtils.isEmpty(childUid)
                || TextUtils.isEmpty(reportText)) {
            if (callback != null) {
                callback.onFailure(new IllegalArgumentException("Missing consultant report data."));
            }
            return;
        }

        Timestamp now = Timestamp.now();
        String normalizedReporterRole = RoleUtils.normalizeRole(reporterRole);
        boolean shouldNotifyParent = RoleUtils.ROLE_CHILD.equals(normalizedReporterRole)
                && !TextUtils.isEmpty(parentUid);
        Map<String, Object> reportData = new HashMap<>();
        reportData.put("reportId", reportId);
        reportData.put("reporterUid", reporterUid);
        reportData.put("reporterRole", normalizedReporterRole);
        reportData.put("reporterDisplayName", reporterDisplayName);
        reportData.put("childUid", childUid);
        reportData.put("childName", childName);
        reportData.put("parentUid", parentUid);
        reportData.put("parentName", parentName);
        reportData.put("reportText", reportText.trim());
        reportData.put("reportPreview", buildConsultantReportPreview(reportText));
        reportData.put("visibleStatus", REPORT_STATUS_PENDING);
        reportData.put("workflowState", REPORT_WORKFLOW_NEW);
        reportData.put("claimState", REPORT_CLAIM_UNCLAIMED);
        reportData.put("followUpRequested", false);
        reportData.put("followUpTarget", "");
        reportData.put("followUpMessage", "");
        reportData.put("evidenceImages", evidenceImages == null ? Collections.emptyList() : evidenceImages);
        reportData.put("parentReportNotificationPending", shouldNotifyParent);
        reportData.put("parentReportNotifiedAt", null);
        reportData.put("createdAt", now);
        reportData.put("updatedAt", now);

        firestore.collection(CONSULTANT_REPORTS_COLLECTION)
                .document(reportId)
                .set(reportData, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    if (callback != null) {
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(exception -> {
                    if (callback != null) {
                        callback.onFailure(exception);
                    }
                });
    }

    public static Task<Void> markConsultantReportParentNotified(DocumentReference reportReference) {
        if (reportReference == null) {
            return null;
        }
        Map<String, Object> update = new HashMap<>();
        update.put("parentReportNotificationPending", false);
        update.put("parentReportNotifiedAt", FieldValue.serverTimestamp());
        update.put("updatedAt", FieldValue.serverTimestamp());
        return reportReference.set(update, SetOptions.merge());
    }

    public static void claimConsultantReport(
            FirebaseFirestore firestore,
            String reportId,
            String consultantUid,
            String consultantName,
            ConsultantReportClaimCallback callback
    ) {
        if (firestore == null || TextUtils.isEmpty(reportId) || TextUtils.isEmpty(consultantUid)) {
            if (callback != null) {
                callback.onFailure(new IllegalArgumentException("Missing consultant report claim data."));
            }
            return;
        }

        DocumentReference reportReference = firestore.collection(CONSULTANT_REPORTS_COLLECTION).document(reportId);
        firestore.runTransaction(transaction -> {
                    DocumentSnapshot snapshot = transaction.get(reportReference);
                    if (!snapshot.exists()) {
                        throw new IllegalStateException("Report not found.");
                    }

                    String visibleStatus = snapshot.getString("visibleStatus");
                    String workflowState = snapshot.getString("workflowState");
                    if (REPORT_STATUS_SOLVED.equalsIgnoreCase(visibleStatus)
                            || REPORT_WORKFLOW_SOLVED.equalsIgnoreCase(workflowState)) {
                        return new ConsultantReportClaimResult(
                                false,
                                false,
                                false,
                                true,
                                snapshot.getString("claimedByConsultantUid"),
                                snapshot.getString("claimedByConsultantName")
                        );
                    }

                    String claimedByConsultantUid = snapshot.getString("claimedByConsultantUid");
                    String claimedByConsultantName = snapshot.getString("claimedByConsultantName");
                    String claimState = snapshot.getString("claimState");
                    if (REPORT_CLAIM_CLAIMED.equalsIgnoreCase(claimState) && !TextUtils.isEmpty(claimedByConsultantUid)) {
                        boolean ownedByCurrentConsultant = consultantUid.equals(claimedByConsultantUid);
                        return new ConsultantReportClaimResult(
                                ownedByCurrentConsultant,
                                false,
                                ownedByCurrentConsultant,
                                false,
                                claimedByConsultantUid,
                                claimedByConsultantName
                        );
                    }

                    Map<String, Object> update = new HashMap<>();
                    update.put("claimState", REPORT_CLAIM_CLAIMED);
                    update.put("workflowState", REPORT_WORKFLOW_CLAIMED);
                    update.put("claimedByConsultantUid", consultantUid);
                    update.put("claimedByConsultantName", consultantName);
                    update.put("claimedAt", FieldValue.serverTimestamp());
                    update.put("updatedAt", FieldValue.serverTimestamp());
                    transaction.set(reportReference, update, SetOptions.merge());

                    return new ConsultantReportClaimResult(
                            true,
                            true,
                            false,
                            false,
                            consultantUid,
                            consultantName
                    );
                })
                .addOnSuccessListener(result -> {
                    if (callback != null) {
                        callback.onSuccess(result);
                    }
                })
                .addOnFailureListener(exception -> {
                    claimConsultantReportWithReadFallback(
                            firestore,
                            reportId,
                            consultantUid,
                            consultantName,
                            callback
                    );
                });
    }

    private static void claimConsultantReportWithReadFallback(
            FirebaseFirestore firestore,
            String reportId,
            String consultantUid,
            String consultantName,
            ConsultantReportClaimCallback callback
    ) {
        DocumentReference reportReference = firestore.collection(CONSULTANT_REPORTS_COLLECTION).document(reportId);
        reportReference.get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        if (callback != null) {
                            callback.onFailure(new IllegalStateException("Report not found."));
                        }
                        return;
                    }

                    String visibleStatus = snapshot.getString("visibleStatus");
                    String workflowState = snapshot.getString("workflowState");
                    if (REPORT_STATUS_SOLVED.equalsIgnoreCase(visibleStatus)
                            || REPORT_WORKFLOW_SOLVED.equalsIgnoreCase(workflowState)) {
                        if (callback != null) {
                            callback.onSuccess(new ConsultantReportClaimResult(
                                    false,
                                    false,
                                    false,
                                    true,
                                    snapshot.getString("claimedByConsultantUid"),
                                    snapshot.getString("claimedByConsultantName")
                            ));
                        }
                        return;
                    }

                    String claimedByConsultantUid = snapshot.getString("claimedByConsultantUid");
                    String claimedByConsultantName = snapshot.getString("claimedByConsultantName");
                    String claimState = snapshot.getString("claimState");
                    if (REPORT_CLAIM_CLAIMED.equalsIgnoreCase(claimState) && !TextUtils.isEmpty(claimedByConsultantUid)) {
                        boolean ownedByCurrentConsultant = consultantUid.equals(claimedByConsultantUid);
                        if (callback != null) {
                            callback.onSuccess(new ConsultantReportClaimResult(
                                    ownedByCurrentConsultant,
                                    false,
                                    ownedByCurrentConsultant,
                                    false,
                                    claimedByConsultantUid,
                                    claimedByConsultantName
                            ));
                        }
                        return;
                    }

                    Map<String, Object> update = new HashMap<>();
                    update.put("claimState", REPORT_CLAIM_CLAIMED);
                    update.put("workflowState", REPORT_WORKFLOW_CLAIMED);
                    update.put("claimedByConsultantUid", consultantUid);
                    update.put("claimedByConsultantName", consultantName);
                    update.put("claimedAt", FieldValue.serverTimestamp());
                    update.put("updatedAt", FieldValue.serverTimestamp());

                    reportReference.set(update, SetOptions.merge())
                            .addOnSuccessListener(unused -> {
                                if (callback != null) {
                                    callback.onSuccess(new ConsultantReportClaimResult(
                                            true,
                                            true,
                                            false,
                                            false,
                                            consultantUid,
                                            consultantName
                                    ));
                                }
                            })
                            .addOnFailureListener(exception -> {
                                if (callback != null) {
                                    callback.onFailure(exception);
                                }
                            });
                })
                .addOnFailureListener(exception -> {
                    if (callback != null) {
                        callback.onFailure(exception);
                    }
                });
    }

    public static void markConsultantReportSolved(
            FirebaseFirestore firestore,
            String reportId,
            String consultantUid,
            String consultantName,
            ConsultantReportWriteCallback callback
    ) {
        if (firestore == null || TextUtils.isEmpty(reportId)) {
            if (callback != null) {
                callback.onFailure(new IllegalArgumentException("Missing consultant report id."));
            }
            return;
        }

        Map<String, Object> update = new HashMap<>();
        update.put("visibleStatus", REPORT_STATUS_SOLVED);
        update.put("workflowState", REPORT_WORKFLOW_SOLVED);
        update.put("claimState", REPORT_CLAIM_CLAIMED);
        update.put("claimedByConsultantUid", consultantUid);
        update.put("claimedByConsultantName", consultantName);
        update.put("solvedByConsultantUid", consultantUid);
        update.put("solvedByConsultantName", consultantName);
        update.put("solvedAt", FieldValue.serverTimestamp());
        update.put("updatedAt", FieldValue.serverTimestamp());

        firestore.collection(CONSULTANT_REPORTS_COLLECTION)
                .document(reportId)
                .set(update, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    if (callback != null) {
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(exception -> {
                    if (callback != null) {
                        callback.onFailure(exception);
                    }
                });
    }

    public static void requestConsultantReportFollowUp(
            FirebaseFirestore firestore,
            String reportId,
            String consultantUid,
            String consultantName,
            String followUpTarget,
            String followUpMessage,
            ConsultantReportWriteCallback callback
    ) {
        if (firestore == null || TextUtils.isEmpty(reportId)) {
            if (callback != null) {
                callback.onFailure(new IllegalArgumentException("Missing consultant report id."));
            }
            return;
        }

        Map<String, Object> update = new HashMap<>();
        update.put("visibleStatus", REPORT_STATUS_PENDING);
        update.put("workflowState", REPORT_WORKFLOW_FOLLOW_UP);
        update.put("claimState", REPORT_CLAIM_CLAIMED);
        update.put("claimedByConsultantUid", consultantUid);
        update.put("claimedByConsultantName", consultantName);
        update.put("followUpRequested", true);
        update.put("followUpTarget", TextUtils.isEmpty(followUpTarget) ? "" : followUpTarget);
        update.put("followUpMessage", TextUtils.isEmpty(followUpMessage)
                ? "Consultant requested a follow-up conversation."
                : followUpMessage.trim());
        update.put("followUpRequestedAt", FieldValue.serverTimestamp());
        update.put("updatedAt", FieldValue.serverTimestamp());

        firestore.collection(CONSULTANT_REPORTS_COLLECTION)
                .document(reportId)
                .set(update, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    if (callback != null) {
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(exception -> {
                    if (callback != null) {
                        callback.onFailure(exception);
                    }
                });
    }

    public static final class AppLimitPayload {
        public final String appName;
        public final long limitSeconds;
        public final boolean isEnabled;
        public final boolean isNewRecord;

        public AppLimitPayload(String appName, long limitSeconds, boolean isEnabled, boolean isNewRecord) {
            this.appName = appName;
            this.limitSeconds = limitSeconds;
            this.isEnabled = isEnabled;
            this.isNewRecord = isNewRecord;
        }
    }

    public static final class ConsultantMessageRecord {
        public final String role;
        public final String content;
        public final Timestamp createdAt;

        public ConsultantMessageRecord(String role, String content, Timestamp createdAt) {
            this.role = role;
            this.content = content;
            this.createdAt = createdAt;
        }
    }

    public static void writeCyberbullyLock(
            FirebaseFirestore firestore,
            String childUid,
            String parentUid,
            String packageName,
            String appName,
            String triggerSignature
    ) {
        if (firestore == null || TextUtils.isEmpty(childUid) || TextUtils.isEmpty(packageName)) {
            return;
        }

        Map<String, Object> lockData = new HashMap<>();
        lockData.put("lockedAt", FieldValue.serverTimestamp());
        lockData.put("lockedAtMillis", System.currentTimeMillis());
        lockData.put("appName", appName == null ? "" : appName);
        lockData.put("unlockedByParent", false);
        lockData.put("triggerSignature", triggerSignature == null ? "" : triggerSignature);
        lockData.put("lockedBy", "AI_DETECTION");

        firestore.collection("users")
                .document(childUid)
                .collection("cyberbullyLockedApps")
                .document(packageName)
                .set(lockData)
                .addOnFailureListener(e -> Log.w("CyberbullyLock", "Failed to write cyberbully lock for " + packageName, e));

        if (!TextUtils.isEmpty(parentUid)) {
            mirrorCyberbullyLockToParent(firestore, parentUid, childUid, packageName, appName, lockData);
        } else {
            resolveParentAndMirrorCyberbullyLock(firestore, childUid, packageName, appName, lockData);
        }
    }

    private static void resolveParentAndMirrorCyberbullyLock(
            FirebaseFirestore firestore,
            String childUid,
            String packageName,
            String appName,
            Map<String, Object> lockData
    ) {
        firestore.collection("users")
                .document(childUid)
                .get()
                .addOnSuccessListener(childDocument -> {
                    String resolvedParentUid = childDocument.getString("parentUid");
                    if (TextUtils.isEmpty(resolvedParentUid)) {
                        resolveParentFromRelationshipAndMirrorCyberbullyLock(
                                firestore,
                                childUid,
                                packageName,
                                appName,
                                lockData
                        );
                        return;
                    }
                    mirrorCyberbullyLockToParent(firestore, resolvedParentUid, childUid, packageName, appName, lockData);
                })
                .addOnFailureListener(e -> {
                    Log.w("CyberbullyLock", "Unable to resolve parentUid from child document. Trying relationship mirror. childUid=" + childUid, e);
                    resolveParentFromRelationshipAndMirrorCyberbullyLock(
                            firestore,
                            childUid,
                            packageName,
                            appName,
                            lockData
                    );
                });
    }

    private static void resolveParentFromRelationshipAndMirrorCyberbullyLock(
            FirebaseFirestore firestore,
            String childUid,
            String packageName,
            String appName,
            Map<String, Object> lockData
    ) {
        firestore.collection("parent_child_relationships")
                .whereEqualTo("childUid", childUid)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot == null || querySnapshot.isEmpty()) {
                        Log.w("CyberbullyLock", "Cyberbully lock still has no linked parentUid. childUid=" + childUid);
                        return;
                    }
                    String resolvedParentUid = querySnapshot.getDocuments().get(0).getString("parentUid");
                    if (TextUtils.isEmpty(resolvedParentUid)) {
                        Log.w("CyberbullyLock", "Relationship document had no parentUid. childUid=" + childUid);
                        return;
                    }
                    mirrorCyberbullyLockToParent(firestore, resolvedParentUid, childUid, packageName, appName, lockData);
                })
                .addOnFailureListener(e -> Log.w(
                        "CyberbullyLock",
                        "Unable to resolve parentUid from parent_child_relationships for cyberbully lock. childUid=" + childUid,
                        e
                ));
    }

    private static void mirrorCyberbullyLockToParent(
            FirebaseFirestore firestore,
            String parentUid,
            String childUid,
            String packageName,
            String appName,
            Map<String, Object> lockData
    ) {
        firestore.collection("users")
                .document(parentUid)
                .collection("children")
                .document(childUid)
                .collection("cyberbullyLockedApps")
                .document(packageName)
                .set(lockData, SetOptions.merge())
                .addOnFailureListener(e -> Log.w("CyberbullyLock", "Failed to mirror cyberbully lock for " + packageName, e));

        Map<String, Object> parentSummary = new HashMap<>();
        parentSummary.put("cyberbullyLatestLockedPackage", packageName);
        parentSummary.put("cyberbullyLatestLockedAppName", appName == null ? "" : appName);
        parentSummary.put("cyberbullyHasActiveLock", true);
        parentSummary.put("cyberbullyActiveLockedPackages", FieldValue.arrayUnion(packageName));
        Map<String, Object> activeLockedAppNames = new HashMap<>();
        activeLockedAppNames.put(packageName, appName == null ? "" : appName);
        parentSummary.put("cyberbullyActiveLockedAppNames", activeLockedAppNames);
        Map<String, Object> activeLockedApps = new HashMap<>();
        activeLockedApps.put(packageName, buildCyberbullyLockSummary(packageName, appName, false, lockData));
        parentSummary.put("cyberbullyActiveLockedApps", activeLockedApps);
        firestore.collection("users")
                .document(parentUid)
                .collection("children")
                .document(childUid)
                .set(parentSummary, SetOptions.merge())
                .addOnFailureListener(e -> Log.w("CyberbullyLock", "Failed to mirror cyberbully lock summary for " + packageName, e));
    }

    public static Task<Void> clearCyberbullyLock(
            FirebaseFirestore firestore,
            String childUid,
            String parentUid,
            String packageName
    ) {
        if (firestore == null || TextUtils.isEmpty(childUid) || TextUtils.isEmpty(packageName)) {
            return Tasks.forResult(null);
        }

        Map<String, Object> update = new HashMap<>();
        String unlockRequestId = UUID.randomUUID().toString();
        long unlockCreatedMillis = System.currentTimeMillis();
        update.put("unlockedByParent", true);
        update.put("unlockedAt", FieldValue.serverTimestamp());
        update.put("unlockCreatedMillis", unlockCreatedMillis);
        update.put("unlockRequestId", unlockRequestId);
        update.put("unlockGraceUntilMillis", unlockCreatedMillis + 60L * 1000L);
        update.put("updatedAt", FieldValue.serverTimestamp());

        if (!TextUtils.isEmpty(parentUid)) {
            Task<Void> parentSummaryTask = writeCyberbullyUnlockSummary(firestore, parentUid, childUid, packageName, update);
            parentSummaryTask.addOnSuccessListener(unused -> {
                firestore.collection("users")
                        .document(parentUid)
                        .collection("children")
                        .document(childUid)
                        .collection("cyberbullyLockedApps")
                        .document(packageName)
                        .set(update, SetOptions.merge())
                        .addOnFailureListener(e -> Log.w("CyberbullyLock", "Optional parent unlock mirror failed for " + packageName, e));
                firestore.collection("users")
                        .document(childUid)
                        .collection("cyberbullyLockedApps")
                        .document(packageName)
                        .set(update, SetOptions.merge())
                        .addOnFailureListener(e -> Log.w("CyberbullyLock", "Optional direct child unlock mirror failed for " + packageName, e));
            });
            return parentSummaryTask;
        }

        return firestore.collection("users")
                .document(childUid)
                .collection("cyberbullyLockedApps")
                .document(packageName)
                .set(update, SetOptions.merge())
                .addOnFailureListener(e -> Log.w("CyberbullyLock", "Failed to clear cyberbully lock for " + packageName, e));
    }

    private static Task<Void> writeCyberbullyUnlockSummary(
            FirebaseFirestore firestore,
            String parentUid,
            String childUid,
            String packageName,
            Map<String, Object> unlockData
    ) {
        Map<String, Object> parentSummary = new HashMap<>();
        parentSummary.put("cyberbullyLatestLockedPackage", "");
        parentSummary.put("cyberbullyLatestLockedAppName", "");
        parentSummary.put("cyberbullyActiveLockedPackages", FieldValue.arrayRemove(packageName));
        Map<String, Object> activeLockedApps = new HashMap<>();
        activeLockedApps.put(packageName, buildCyberbullyLockSummary(packageName, "", true, unlockData));
        parentSummary.put("cyberbullyActiveLockedApps", activeLockedApps);
        return firestore.collection("users")
                .document(parentUid)
                .collection("children")
                .document(childUid)
                .set(parentSummary, SetOptions.merge())
                .addOnFailureListener(e -> Log.w("CyberbullyLock", "Failed to clear parent cyberbully summary for " + packageName, e));
    }

    private static Map<String, Object> buildCyberbullyLockSummary(String packageName, String appName, boolean unlockedByParent) {
        return buildCyberbullyLockSummary(packageName, appName, unlockedByParent, null);
    }

    private static Map<String, Object> buildCyberbullyLockSummary(
            String packageName,
            String appName,
            boolean unlockedByParent,
            Map<String, Object> extraData
    ) {
        Map<String, Object> lockSummary = new HashMap<>();
        lockSummary.put("packageName", packageName);
        lockSummary.put("appName", appName == null ? "" : appName);
        lockSummary.put("unlockedByParent", unlockedByParent);
        if (extraData != null) {
            lockSummary.putAll(extraData);
            lockSummary.put("packageName", packageName);
            Object summaryAppName = lockSummary.get("appName");
            if (summaryAppName == null || TextUtils.isEmpty(summaryAppName.toString())) {
                lockSummary.put("appName", appName == null ? "" : appName);
            }
            lockSummary.put("unlockedByParent", unlockedByParent);
        }
        lockSummary.put("updatedAt", FieldValue.serverTimestamp());
        return lockSummary;
    }

    public static final class ThreatIncidentPayload {
        public final String incidentId;
        public final String requestId;
        public final String childUid;
        public final String parentUid;
        public final String childName;
        public final String appName;
        public final String packageName;
        public final String messageText;
        public final String maskedPreview;
        public final String threatLevel;
        public final Timestamp detectedAt;
        public final String sender;
        public final String direction;
        public final String status;
        public final boolean consultantSuggested;
        public final String analysisSummary;
        public final String contentType;
        public final String sourceUri;
        public final String aiProvider;
        public final String aiModel;
        public final String aiReason;
        public final String aiRawJson;
        public final String geminiReason;
        public final String geminiReportJson;

        public ThreatIncidentPayload(
                String incidentId,
                String childUid,
                String parentUid,
                String childName,
                String appName,
                String packageName,
                String messageText,
                String maskedPreview,
                String threatLevel,
                Timestamp detectedAt,
                String sender,
                String direction,
                String status,
                boolean consultantSuggested,
                String analysisSummary,
                String contentType,
                String sourceUri,
                String geminiReason,
                String geminiReportJson
        ) {
            this(
                    incidentId,
                    "",
                    childUid,
                    parentUid,
                    childName,
                    appName,
                    packageName,
                    messageText,
                    maskedPreview,
                    threatLevel,
                    detectedAt,
                    sender,
                    direction,
                    status,
                    consultantSuggested,
                    analysisSummary,
                    contentType,
                    sourceUri,
                    "",
                    "",
                    geminiReason,
                    geminiReportJson
            );
        }

        public ThreatIncidentPayload(
                String incidentId,
                String requestId,
                String childUid,
                String parentUid,
                String childName,
                String appName,
                String packageName,
                String messageText,
                String maskedPreview,
                String threatLevel,
                Timestamp detectedAt,
                String sender,
                String direction,
                String status,
                boolean consultantSuggested,
                String analysisSummary,
                String contentType,
                String sourceUri,
                String aiProvider,
                String aiModel,
                String aiReason,
                String aiRawJson
        ) {
            this.incidentId = incidentId;
            this.requestId = requestId;
            this.childUid = childUid;
            this.parentUid = parentUid;
            this.childName = childName;
            this.appName = appName;
            this.packageName = packageName;
            this.messageText = messageText;
            this.maskedPreview = maskedPreview;
            this.threatLevel = threatLevel;
            this.detectedAt = detectedAt;
            this.sender = sender;
            this.direction = direction;
            this.status = status;
            this.consultantSuggested = consultantSuggested;
            this.analysisSummary = analysisSummary;
            this.contentType = contentType;
            this.sourceUri = sourceUri;
            this.aiProvider = aiProvider;
            this.aiModel = aiModel;
            this.aiReason = aiReason;
            this.aiRawJson = aiRawJson;
            this.geminiReason = aiReason;
            this.geminiReportJson = aiRawJson;
        }
    }
}
