package com.mynyaman.mynyamanapp;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.cardview.widget.CardView;
import androidx.core.app.NotificationCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class AiThreatDetectionActivity extends BaseActivity {

    private static final String TAG = "AiThreatDetection";
    private static final int REQUEST_POST_NOTIFICATIONS = 901;
    private static final int PARENT_SCAN_STATUS_NOTIFICATION_ID = 2206;
    private static final String SCAN_STATUS_CHANNEL_ID = "mynyaman_scan_status";
    private static final String EXTRA_CHILD_UID = "childUid";
    private static final String EXTRA_SCROLL_TO = "scroll_to";
    private static final String SCROLL_TO_CYBERBULLY_CONTROLS = "cyberbully_controls";

    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;

    private NestedScrollView scrollThreatContent;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView tvSubtitle;
    private TextView tvThreatMonitoringStatus;
    private TextView tvThreatOverview;
    private TextView tvRoleHint;
    private TextView tvSupportedApps;
    private TextView tvSetupHint;
    private TextView tvAccessibilityStatus;
    private TextView tvNotificationStatus;
    private TextView tvImageStatus;
    private TextView tvUsageStatus;
    private TextView tvOverlayStatus;
    private TextView tvBatteryStatus;
    private TextView tvParentAlertStatus;
    private TextView tvThreatEmptyState;
    private View cardParentAlertSetup;
    private AppCompatButton btnEnableAccessibility;
    private AppCompatButton btnEnableNotifications;
    private AppCompatButton btnEnableImages;
    private AppCompatButton btnEnableUsage;
    private AppCompatButton btnEnableOverlay;
    private AppCompatButton btnEnableBattery;
    private AppCompatButton btnEnableParentAlerts;
    private AppCompatButton btnEnableParentBackground;
    private AppCompatButton btnOpenParentNotificationSettings;
    private AppCompatButton btnOpenParentAppSettings;
    private AppCompatButton btnConsultantSupport;
    private RecyclerView rvThreatIncidentList;

    private final List<ThreatIncidentRow> incidentRows = new ArrayList<>();
    private final Map<String, ThreatIncidentRow> primaryIncidentRowsById = new LinkedHashMap<>();
    private final Map<String, ThreatIncidentRow> childHistoryIncidentRowsById = new LinkedHashMap<>();
    private ThreatIncidentAdapter adapter;
    private ListenerRegistration threatListenerRegistration;
    private ListenerRegistration childThreatHistoryListenerRegistration;
    private ListenerRegistration selectedChildSummaryListenerRegistration;
    private String activeIncidentChildUid;
    private String activeIncidentParentUid;
    private String activeSummaryChildUid;
    private String activeSummaryParentUid;
    private ThreatIncidentRow latestParentSummaryIncidentRow;

    private View cardCyberbullyControls;
    private androidx.appcompat.widget.SwitchCompat switchThreatScanEnabled;
    private LinearLayout llLockedAppsContainer;
    private ListenerRegistration cyberbullyLockSummaryListenerRegistration;
    private ListenerRegistration parentCyberbullyLockListenerRegistration;
    private ListenerRegistration directCyberbullyLockListenerRegistration;
    private String activeCyberbullyLockChildUid;
    private String activeCyberbullyLockParentUid;
    private final Map<String, LockedAppRow> summaryLockedAppsByPackage = new LinkedHashMap<>();
    private final Map<String, LockedAppRow> parentLockedAppsByPackage = new LinkedHashMap<>();
    private final Map<String, LockedAppRow> directLockedAppsByPackage = new LinkedHashMap<>();
    private final Set<String> summaryUnlockedLockedPackages = new HashSet<>();
    private final Set<String> parentUnlockedLockedPackages = new HashSet<>();
    private final Set<String> directUnlockedLockedPackages = new HashSet<>();
    private boolean currentThreatScanEnabled = true;
    private View cardCyberbullyStatusChild;
    private TextView tvChildScanStatus;
    private LinearLayout llChildLockedAppsContainer;
    private ListenerRegistration childCyberbullyStatusListenerRegistration;

    private String currentRole = "";
    private String parentUidForChild;
    private String currentChildUid;
    private String currentChildName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_threat_detection);

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        ImageView btnBack = findViewById(R.id.btnBack);
        scrollThreatContent = findViewById(R.id.scrollThreatContent);
        tvSubtitle = findViewById(R.id.tvSubtitle);
        tvThreatMonitoringStatus = findViewById(R.id.tvThreatMonitoringStatus);
        tvThreatOverview = findViewById(R.id.tvThreatOverview);
        tvRoleHint = findViewById(R.id.tvRoleHint);
        tvSupportedApps = findViewById(R.id.tvSupportedApps);
        tvSetupHint = findViewById(R.id.tvSetupHint);
        tvAccessibilityStatus = findViewById(R.id.tvAccessibilityStatus);
        tvNotificationStatus = findViewById(R.id.tvNotificationStatus);
        tvImageStatus = findViewById(R.id.tvImageStatus);
        tvUsageStatus = findViewById(R.id.tvUsageStatus);
        tvOverlayStatus = findViewById(R.id.tvOverlayStatus);
        tvBatteryStatus = findViewById(R.id.tvBatteryStatus);
        tvParentAlertStatus = findViewById(R.id.tvParentAlertStatus);
        tvThreatEmptyState = findViewById(R.id.tvThreatEmptyState);
        cardParentAlertSetup = findViewById(R.id.cardParentAlertSetup);
        btnEnableAccessibility = findViewById(R.id.btnEnableAccessibility);
        btnEnableNotifications = findViewById(R.id.btnEnableNotifications);
        btnEnableImages = findViewById(R.id.btnEnableImages);
        btnEnableUsage = findViewById(R.id.btnEnableUsage);
        btnEnableOverlay = findViewById(R.id.btnEnableOverlay);
        btnEnableBattery = findViewById(R.id.btnEnableBattery);
        btnEnableParentAlerts = findViewById(R.id.btnEnableParentAlerts);
        btnEnableParentBackground = findViewById(R.id.btnEnableParentBackground);
        btnOpenParentNotificationSettings = findViewById(R.id.btnOpenParentNotificationSettings);
        btnOpenParentAppSettings = findViewById(R.id.btnOpenParentAppSettings);
        btnConsultantSupport = findViewById(R.id.btnConsultantSupport);
        cardCyberbullyControls = findViewById(R.id.cardCyberbullyControls);
        switchThreatScanEnabled = findViewById(R.id.switchThreatScanEnabled);
        llLockedAppsContainer = findViewById(R.id.llLockedAppsContainer);
        cardCyberbullyStatusChild = findViewById(R.id.cardCyberbullyStatusChild);
        tvChildScanStatus = findViewById(R.id.tvChildScanStatus);
        llChildLockedAppsContainer = findViewById(R.id.llChildLockedAppsStatus);
        rvThreatIncidentList = findViewById(R.id.rvThreatIncidentList);

        rvThreatIncidentList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ThreatIncidentAdapter(incidentRows);
        rvThreatIncidentList.setAdapter(adapter);
        setupSwipeRefresh();

        btnBack.setOnClickListener(v -> finish());
        btnEnableAccessibility.setOnClickListener(v -> openSettingsSafely(ThreatMonitoringPermissionHelper.newAccessibilityIntent()));
        btnEnableNotifications.setOnClickListener(v -> enableNotificationsForCurrentDevice());
        btnEnableImages.setVisibility(View.GONE);
        btnEnableUsage.setOnClickListener(v -> openSettingsSafely(ThreatMonitoringPermissionHelper.newUsageAccessIntent()));
        btnEnableOverlay.setOnClickListener(v -> openSettingsSafely(ThreatMonitoringPermissionHelper.newOverlayIntent(this)));
        btnEnableBattery.setOnClickListener(v -> openSettingsSafely(ThreatMonitoringPermissionHelper.newBatteryOptimizationIntent(this)));
        btnEnableParentAlerts.setOnClickListener(v -> enableNotificationsForCurrentDevice());
        btnEnableParentBackground.setOnClickListener(v -> openParentBatterySettings());
        btnOpenParentNotificationSettings.setOnClickListener(v -> openParentNotificationSettings());
        btnOpenParentAppSettings.setOnClickListener(v -> openParentAppSettings());
        btnConsultantSupport.setOnClickListener(v -> {
            String role = TextUtils.isEmpty(currentRole) ? RoleUtils.ROLE_PARENT : currentRole;
            startActivity(ConsultantHubActivity.createIntent(this, role));
        });

        loadRoleAndState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshVisibleState();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (RoleUtils.ROLE_PARENT.equals(currentRole)
                && intent != null
                && !TextUtils.isEmpty(intent.getStringExtra(EXTRA_CHILD_UID))) {
            refreshVisibleState();
            return;
        }
        handleScrollIntentIfNeeded();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        removeThreatListener();
        removeSelectedChildSummaryListener();
        removeCyberbullyLockedAppsListeners();
        removeChildCyberbullyStatusListener();
    }

    private void setupSwipeRefresh() {
        if (scrollThreatContent == null || scrollThreatContent.getParent() == null) {
            return;
        }

        ViewGroup parent = (ViewGroup) scrollThreatContent.getParent();
        if (parent instanceof SwipeRefreshLayout) {
            swipeRefreshLayout = (SwipeRefreshLayout) parent;
        } else {
            int index = parent.indexOfChild(scrollThreatContent);
            ViewGroup.LayoutParams layoutParams = scrollThreatContent.getLayoutParams();
            parent.removeView(scrollThreatContent);

            swipeRefreshLayout = new SwipeRefreshLayout(this);
            swipeRefreshLayout.setLayoutParams(layoutParams);
            swipeRefreshLayout.setColorSchemeColors(Color.parseColor("#2188FF"));
            swipeRefreshLayout.addView(scrollThreatContent, new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            ));
            parent.addView(swipeRefreshLayout, index);
        }

        swipeRefreshLayout.setOnRefreshListener(this::refreshThreatScreenFromSwipe);
    }

    private void refreshThreatScreenFromSwipe() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            finishThreatRefreshing();
            return;
        }

        removeThreatListener();
        if (TextUtils.isEmpty(currentRole)) {
            loadRoleAndState();
            return;
        }
        refreshVisibleState();
    }

    private void finishThreatRefreshing() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    private void loadRoleAndState() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            finishThreatRefreshing();
            startActivity(new Intent(this, Login.class));
            finish();
            return;
        }

        firestore.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    currentRole = RoleUtils.readNormalizedRole(documentSnapshot);
                    currentChildName = documentSnapshot.getString("name");
                    parentUidForChild = documentSnapshot.getString("parentUid");
                    refreshVisibleState();
                })
                .addOnFailureListener(e -> {
                    currentRole = "";
                    finishThreatRefreshing();
                    refreshVisibleState();
                });
    }

    private void refreshVisibleState() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            finishThreatRefreshing();
            return;
        }

        tvSupportedApps.setText("Supported apps: " + ThreatMonitoringPermissionHelper.supportedAppsText());
        if (TextUtils.isEmpty(currentRole)) {
            tvThreatMonitoringStatus.setText("Loading");
            tvThreatOverview.setText("Reading account role before loading threat monitoring setup.");
            finishThreatRefreshing();
            return;
        }
        if (RoleUtils.ROLE_PARENT.equals(currentRole)) {
            bindParentMode(currentUser.getUid());
        } else {
            bindChildMode(currentUser);
        }
    }

    private void bindChildMode(FirebaseUser currentUser) {
        currentChildUid = currentUser.getUid();
        latestParentSummaryIncidentRow = null;
        removeSelectedChildSummaryListener();
        cardParentAlertSetup.setVisibility(View.GONE);
        if (cardCyberbullyControls != null) {
            cardCyberbullyControls.setVisibility(View.GONE);
        }
        removeCyberbullyLockedAppsListeners();
        if (cardCyberbullyStatusChild != null) {
            cardCyberbullyStatusChild.setVisibility(View.VISIBLE);
        }
        loadChildCyberbullyStatus(currentUser.getUid());
        setSetupButtonsVisible(true);

        String childNameLabel = TextUtils.isEmpty(currentChildName) ? "child device" : currentChildName;
        tvSubtitle.setText("Live safety setup for the " + childNameLabel + ".");
        tvSetupHint.setText("Complete setup here instead of jumping straight into system settings without context.");

        applyStatus(tvAccessibilityStatus, btnEnableAccessibility, ThreatMonitoringPermissionHelper.hasAccessibilityPermission(this));
        applyStatus(tvNotificationStatus, btnEnableNotifications, ThreatMonitoringPermissionHelper.hasNotificationPermission(this));
        applyNeutralStatus(tvImageStatus, "Not required");
        applyStatus(tvUsageStatus, btnEnableUsage, ThreatMonitoringPermissionHelper.hasUsagePermission(this));
        applyStatus(tvOverlayStatus, btnEnableOverlay, ThreatMonitoringPermissionHelper.hasOverlayPermission(this));
        applyStatus(tvBatteryStatus, btnEnableBattery, ThreatMonitoringPermissionHelper.isIgnoringBatteryOptimizations(this));

        tvThreatMonitoringStatus.setText(ThreatMonitoringPermissionHelper.buildLocalStatusLabel(this));
        tvThreatOverview.setText(ThreatMonitoringPermissionHelper.isThreatMonitoringReady(this)
                ? "Text-based monitoring is active for supported communication apps where Android accessibility can capture visible text."
                : "Monitoring is limited until the required setup is completed on this child device.");
        tvRoleHint.setText("AI gallery scanning is off. Threat detection now reviews captured conversation text and keeps manual report evidence separate.");

        BlueprintSyncRepository.syncThreatMonitoringStatus(
                firestore,
                currentUser.getUid(),
                parentUidForChild,
                currentChildName,
                ThreatMonitoringPermissionHelper.buildStatusPayload(this)
        );
        listenForThreatIncidents(currentUser.getUid(), null);
    }

    private void bindParentMode(String parentUid) {
        cardParentAlertSetup.setVisibility(View.VISIBLE);
        if (cardCyberbullyControls != null) {
            cardCyberbullyControls.setVisibility(View.VISIBLE);
        }
        if (cardCyberbullyStatusChild != null) {
            cardCyberbullyStatusChild.setVisibility(View.GONE);
        }
        removeChildCyberbullyStatusListener();
        refreshParentAlertStatus();
        handleScrollIntentIfNeeded();

        String selectedChildUid = resolveParentSelectedChildUidFromIntent();
        if (!TextUtils.isEmpty(selectedChildUid)) {
            loadSelectedChildSummary(parentUid, selectedChildUid);
            return;
        }

        loadFirstLinkedChildForParent(parentUid);
    }

    private String resolveParentSelectedChildUidFromIntent() {
        Intent intent = getIntent();
        String requestedChildUid = intent == null ? "" : intent.getStringExtra(EXTRA_CHILD_UID);
        if (!TextUtils.isEmpty(requestedChildUid)) {
            SelectedChildStore.saveSelectedChildUid(this, requestedChildUid);
            intent.removeExtra(EXTRA_CHILD_UID);
            return requestedChildUid;
        }
        return SelectedChildStore.getSelectedChildUid(this);
    }

    private void loadFirstLinkedChildForParent(String parentUid) {
        firestore.collection("users")
                .document(parentUid)
                .collection("children")
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot == null || querySnapshot.isEmpty()) {
                        showNoSelectedChildState();
                        return;
                    }
                    DocumentSnapshot firstChild = querySnapshot.getDocuments().get(0);
                    String childUid = firstChild.getId();
                    if (TextUtils.isEmpty(childUid)) {
                        showNoSelectedChildState();
                        return;
                    }
                    SelectedChildStore.saveSelectedChildUid(this, childUid);
                    loadSelectedChildSummary(parentUid, childUid);
                })
                .addOnFailureListener(e -> showNoSelectedChildState());
    }

    private void showNoSelectedChildState() {
        currentChildUid = null;
        currentChildName = null;
        latestParentSummaryIncidentRow = null;
        removeSelectedChildSummaryListener();
        setSetupButtonsVisible(false);
        tvSubtitle.setText("Select a child on the parent dashboard to view monitoring status.");
        tvThreatMonitoringStatus.setText("Select Child");
        tvThreatOverview.setText("No child is selected for AI Threat Detection yet.");
        tvRoleHint.setText("Parent notifications are checked on this phone. Child monitoring permissions are read from the selected child's synced status.");
        setRemoteStatusUnknown();
        setIncidentEmpty("Select a child to view recent threat alerts.");
        removeCyberbullyLockedAppsListeners();
        renderLockedApps("");
    }

    private void loadSelectedChildSummary(String parentUid, String selectedChildUid) {
        if (!TextUtils.equals(selectedChildUid, activeIncidentChildUid)
                || !TextUtils.equals(parentUid, activeIncidentParentUid)) {
            removeThreatListener();
            setIncidentLoading("Loading recent threat alerts...");
        }

        removeSelectedChildSummaryListener();
        activeSummaryParentUid = parentUid;
        activeSummaryChildUid = selectedChildUid;
        selectedChildSummaryListenerRegistration = firestore.collection("users")
                .document(parentUid)
                .collection("children")
                .document(selectedChildUid)
                .addSnapshotListener((documentSnapshot, error) -> {
                    if (!TextUtils.equals(parentUid, activeSummaryParentUid)
                            || !TextUtils.equals(selectedChildUid, activeSummaryChildUid)) {
                        return;
                    }
                    if (error != null) {
                        latestParentSummaryIncidentRow = null;
                        tvSubtitle.setText("Unable to read the selected child status right now.");
                        setRemoteStatusUnknown();
                        setIncidentError("Unable to load recent threat alerts right now.");
                        removeCyberbullyLockedAppsListeners();
                        renderLockedApps("");
                        return;
                    }
                    if (documentSnapshot == null || !documentSnapshot.exists()) {
                        removeSelectedChildSummaryListener();
                        SelectedChildStore.saveSelectedChildUid(this, "");
                        loadFirstLinkedChildForParent(parentUid);
                        return;
                    }
                    bindSelectedChildSummary(parentUid, selectedChildUid, documentSnapshot);
                });
    }

    private void bindSelectedChildSummary(String parentUid, String selectedChildUid, DocumentSnapshot documentSnapshot) {
        currentChildUid = selectedChildUid;
        currentChildName = documentSnapshot.getString("name");
        setSetupButtonsVisible(false);
        String childLabel = TextUtils.isEmpty(currentChildName) ? "selected child" : currentChildName;
        tvSubtitle.setText("Reviewing " + childLabel + "'s monitoring status and recent alerts.");
        tvSetupHint.setText("These setup rows are synced from the child device. Accessibility, notifications, usage access, overlay, and battery reliability must be enabled there.");
        tvThreatMonitoringStatus.setText(stringOrFallback(
                documentSnapshot.getString("threatMonitoringStatusLabel"),
                "Needs Setup"
        ));
        applyStatus(tvAccessibilityStatus, null, Boolean.TRUE.equals(documentSnapshot.getBoolean("threatPermissionAccessibility")));
        applyStatus(tvNotificationStatus, null, Boolean.TRUE.equals(documentSnapshot.getBoolean("threatPermissionNotifications")));
        applyNeutralStatus(tvImageStatus, "Not required");
        applyStatus(tvUsageStatus, null, Boolean.TRUE.equals(documentSnapshot.getBoolean("threatPermissionUsageAccess")));
        applyStatus(tvOverlayStatus, null, Boolean.TRUE.equals(documentSnapshot.getBoolean("threatPermissionOverlay")));
        applyStatus(tvBatteryStatus, null, Boolean.TRUE.equals(documentSnapshot.getBoolean("threatBatteryOptimizationIgnored")));

        String lastLevel = documentSnapshot.getString("threatLastLevel");
        String lastApp = documentSnapshot.getString("threatLastAppName");
        latestParentSummaryIncidentRow = buildParentSummaryIncidentRow(documentSnapshot, selectedChildUid, parentUid);
        if (!TextUtils.isEmpty(lastLevel)) {
            tvThreatOverview.setText("Latest alert: " + lastLevel + (TextUtils.isEmpty(lastApp) ? "" : " in " + lastApp) + ".");
        } else {
            tvThreatOverview.setText("No recent alert has been synced for this child yet.");
        }

        tvRoleHint.setText("Parent notifications use masked previews on the lock screen. Full message details are shown only inside the app.");

        listenForThreatIncidents(selectedChildUid, parentUid);
        loadThreatScanToggle(selectedChildUid);
        listenForCyberbullyLockedApps(parentUid, selectedChildUid);
    }

    private void refreshParentAlertStatus() {
        boolean notificationsEnabled = ThreatMonitoringPermissionHelper.hasNotificationPermission(this);
        boolean batteryAllowed = ThreatMonitoringPermissionHelper.isIgnoringBatteryOptimizations(this);
        String listenerStatus = notificationsEnabled ? "Running" : "Not running";
        tvParentAlertStatus.setText(
                "Notifications: " + (notificationsEnabled ? "Enabled" : "Disabled")
                        + "\nBattery background access: " + (batteryAllowed ? "Allowed" : "Needs setup")
                        + "\nParent alert listener: " + listenerStatus
        );
        if (notificationsEnabled) {
            btnEnableParentAlerts.setVisibility(View.GONE);
            startParentAlertServiceIfAllowed();
        } else {
            btnEnableParentAlerts.setVisibility(View.VISIBLE);
        }
        btnEnableParentBackground.setVisibility(batteryAllowed ? View.GONE : View.VISIBLE);
    }

    private void startParentAlertServiceIfAllowed() {
        if (!RoleUtils.ROLE_PARENT.equals(currentRole) || !ThreatMonitoringPermissionHelper.hasNotificationPermission(this)) {
            return;
        }
        Intent serviceIntent = new Intent(this, ParentThreatAlertService.class);
        serviceIntent.setAction(ParentThreatAlertService.ACTION_START);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        } catch (RuntimeException ignored) {
            tvParentAlertStatus.setText("Parent alert monitor could not start on this device yet. Alerts remain visible in the app.");
        }
    }

    private void setRemoteStatusUnknown() {
        applyStatus(tvAccessibilityStatus, null, false);
        applyStatus(tvNotificationStatus, null, false);
        applyNeutralStatus(tvImageStatus, "Not required");
        applyStatus(tvUsageStatus, null, false);
        applyStatus(tvOverlayStatus, null, false);
        applyStatus(tvBatteryStatus, null, false);
    }

    private void setSetupButtonsVisible(boolean visible) {
        int visibility = visible ? View.VISIBLE : View.GONE;
        btnEnableAccessibility.setVisibility(visibility);
        btnEnableNotifications.setVisibility(visibility);
        btnEnableImages.setVisibility(View.GONE);
        btnEnableUsage.setVisibility(visibility);
        btnEnableOverlay.setVisibility(visibility);
        btnEnableBattery.setVisibility(visibility);
    }

    private void applyStatus(TextView textView, AppCompatButton button, boolean enabled) {
        textView.setText(ThreatMonitoringPermissionHelper.statusText(enabled));
        textView.setTextColor(Color.parseColor(enabled ? "#2E7D32" : "#C62828"));
        if (button != null) {
            button.setVisibility(enabled ? View.GONE : View.VISIBLE);
        }
    }

    private void applyNeutralStatus(TextView textView, String label) {
        textView.setText(label);
        textView.setTextColor(Color.parseColor("#2E7D32"));
    }

    private void enableNotificationsForCurrentDevice() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_POST_NOTIFICATIONS);
            return;
        }
        if (RoleUtils.ROLE_PARENT.equals(currentRole)) {
            openParentNotificationSettings();
        } else {
            openSettingsSafely(ThreatMonitoringPermissionHelper.newNotificationSettingsIntent(this));
        }
    }

    private void openParentNotificationSettings() {
        Log.d(TAG, "PARENT_PERMISSION_OPENED_NOTIFICATION_SETTINGS");
        openSettingsSafely(ThreatMonitoringPermissionHelper.newNotificationSettingsIntent(this));
    }

    private void openParentBatterySettings() {
        if (ThreatMonitoringPermissionHelper.isIgnoringBatteryOptimizations(this)) {
            Log.d(TAG, "PARENT_PERMISSION_BATTERY_OPTIMIZATION_IGNORED");
            refreshVisibleState();
            return;
        }
        Log.d(TAG, "PARENT_PERMISSION_OPENED_BATTERY_SETTINGS");
        openSettingsSafely(ThreatMonitoringPermissionHelper.newBatteryOptimizationIntent(this));
    }

    private void openParentAppSettings() {
        Log.d(TAG, "PARENT_PERMISSION_OPENED_APP_SETTINGS");
        openSettingsSafely(ThreatMonitoringPermissionHelper.newAppDetailsIntent(this));
    }

    private void openSettingsSafely(Intent intent) {
        try {
            startActivity(intent);
        } catch (Exception firstException) {
            try {
                if (RoleUtils.ROLE_PARENT.equals(currentRole)) {
                    Log.d(TAG, "PARENT_PERMISSION_OPENED_APP_SETTINGS");
                }
                startActivity(ThreatMonitoringPermissionHelper.newAppDetailsIntent(this));
            } catch (Exception secondException) {
                Log.w("ThreatPermissions", "Unable to open Android settings.", secondException);
            }
        }
    }

    private void listenForThreatIncidents(String childUid, String parentFilterUid) {
        if (TextUtils.isEmpty(childUid)) {
            setIncidentEmpty("Select a child to view recent threat alerts.");
            return;
        }

        if (threatListenerRegistration != null
                && TextUtils.equals(childUid, activeIncidentChildUid)
                && TextUtils.equals(parentFilterUid, activeIncidentParentUid)) {
            mergeLatestParentSummaryIncidentIfNeeded(parentFilterUid);
            renderIncidentRows();
            finishThreatRefreshing();
            return;
        }

        removeThreatListener();
        activeIncidentChildUid = childUid;
        activeIncidentParentUid = parentFilterUid;
        primaryIncidentRowsById.clear();
        childHistoryIncidentRowsById.clear();

        setIncidentLoading("Loading recent threat alerts...");
        Query query = buildThreatIncidentQuery(childUid, parentFilterUid);

        threatListenerRegistration = query.addSnapshotListener((queryDocumentSnapshots, error) -> {
                    if (error != null || queryDocumentSnapshots == null) {
                        Log.w("ThreatAlerts", "Unable to load recent threat alerts.", error);
                        if (!TextUtils.isEmpty(parentFilterUid)
                                && (!childHistoryIncidentRowsById.isEmpty() || latestParentSummaryIncidentRow != null)) {
                            renderCachedIncidentRows(parentFilterUid);
                            finishThreatRefreshing();
                            return;
                        }
                        setIncidentError("Unable to load recent threat alerts right now.");
                        return;
                    }

                    primaryIncidentRowsById.clear();
                    collectIncidentRows(queryDocumentSnapshots.getDocuments(), parentFilterUid, primaryIncidentRowsById);
                    renderCachedIncidentRows(parentFilterUid);
                    finishThreatRefreshing();
                });
        listenForDirectChildThreatHistory(childUid, parentFilterUid);
    }

    private void listenForDirectChildThreatHistory(String childUid, String parentFilterUid) {
        if (TextUtils.isEmpty(parentFilterUid) || TextUtils.isEmpty(childUid)) {
            return;
        }

        if (childThreatHistoryListenerRegistration != null) {
            childThreatHistoryListenerRegistration.remove();
            childThreatHistoryListenerRegistration = null;
        }

        childThreatHistoryListenerRegistration = firestore.collection("users")
                .document(childUid)
                .collection("threat_reports")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener((queryDocumentSnapshots, error) -> {
                    if (error != null || queryDocumentSnapshots == null) {
                        Log.w("ThreatAlerts", "Unable to load direct child threat history.", error);
                        return;
                    }

                    childHistoryIncidentRowsById.clear();
                    collectIncidentRows(queryDocumentSnapshots.getDocuments(), parentFilterUid, childHistoryIncidentRowsById);
                    renderCachedIncidentRows(parentFilterUid);
                    finishThreatRefreshing();
                });
    }

    private void collectIncidentRows(
            List<DocumentSnapshot> documents,
            String parentFilterUid,
            Map<String, ThreatIncidentRow> rowsById
    ) {
        if (documents == null || rowsById == null) {
            return;
        }
        for (DocumentSnapshot document : documents) {
            ThreatIncidentRow row = buildIncidentRow(document);
            if (row == null) {
                continue;
            }
            if (!TextUtils.isEmpty(parentFilterUid)
                    && !TextUtils.isEmpty(row.parentUid)
                    && !TextUtils.equals(parentFilterUid, row.parentUid)) {
                continue;
            }
            rowsById.put(row.id, row);
        }
    }

    private void renderCachedIncidentRows(String parentFilterUid) {
        Map<String, ThreatIncidentRow> rowsById = new LinkedHashMap<>();
        rowsById.putAll(primaryIncidentRowsById);
        rowsById.putAll(childHistoryIncidentRowsById);

        incidentRows.clear();
        incidentRows.addAll(rowsById.values());
        mergeLatestParentSummaryIncidentIfNeeded(parentFilterUid);
        renderIncidentRows();
    }

    private void mergeLatestParentSummaryIncidentIfNeeded(String parentFilterUid) {
        if (TextUtils.isEmpty(parentFilterUid) || latestParentSummaryIncidentRow == null) {
            return;
        }

        for (ThreatIncidentRow row : incidentRows) {
            if (TextUtils.equals(row.id, latestParentSummaryIncidentRow.id)) {
                return;
            }
        }

        String summaryIdPrefix = "summary_" + latestParentSummaryIncidentRow.childUid;
        for (int i = incidentRows.size() - 1; i >= 0; i--) {
            ThreatIncidentRow row = incidentRows.get(i);
            if (!TextUtils.isEmpty(row.id) && row.id.startsWith(summaryIdPrefix)) {
                incidentRows.remove(i);
            }
        }
        incidentRows.add(latestParentSummaryIncidentRow);
    }

    private void renderIncidentRows() {
        incidentRows.sort(Comparator.comparing(
                (ThreatIncidentRow row) -> row.sortDate == null ? new Date(0L) : row.sortDate
        ).reversed());
        if (incidentRows.size() > 50) {
            incidentRows.subList(50, incidentRows.size()).clear();
        }
        adapter.notifyDataSetChanged();
        rvThreatIncidentList.setVisibility(incidentRows.isEmpty() ? View.GONE : View.VISIBLE);
        tvThreatEmptyState.setText("No recent alerts have been logged yet.");
        tvThreatEmptyState.setTextColor(Color.parseColor("#757575"));
        tvThreatEmptyState.setVisibility(incidentRows.isEmpty() ? View.VISIBLE : View.GONE);
        bindIncidentSummary();
        if (RoleUtils.ROLE_PARENT.equals(currentRole) && !TextUtils.isEmpty(currentChildUid)) {
            renderLockedApps(currentChildUid);
        }
    }

    private Query buildThreatIncidentQuery(String childUid, String parentUid) {
        if (!TextUtils.isEmpty(parentUid)) {
            return firestore.collection("users")
                    .document(parentUid)
                    .collection("children")
                    .document(childUid)
                    .collection("threat_reports")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(50);
        }

        return firestore.collection("users")
                .document(childUid)
                .collection("threat_reports")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50);
    }

    private ThreatIncidentRow buildIncidentRow(DocumentSnapshot document) {
        if (document == null) {
            return null;
        }
        String incidentId = stringOrFallback(document.getString("incidentId"), document.getId());
        return new ThreatIncidentRow(
                incidentId,
                document.getString("childUid"),
                document.getString("parentUid"),
                document.getString("childName"),
                document.getString("appName"),
                document.getString("packageName"),
                document.getString("contentType"),
                document.getString("maskedPreview"),
                document.getString("messageText"),
                document.getString("threatLevel"),
                document.getString("direction"),
                document.getString("status"),
                document.getString("aiProvider"),
                Boolean.TRUE.equals(document.getBoolean("consultantSuggested")),
                readBestDate(document)
        );
    }

    private ThreatIncidentRow buildParentSummaryIncidentRow(DocumentSnapshot document, String childUid, String parentUid) {
        if (document == null || TextUtils.isEmpty(document.getString("threatLastLevel"))) {
            return null;
        }

        return new ThreatIncidentRow(
                stringOrFallback(document.getString("threatLastIncidentId"), "summary_" + childUid),
                childUid,
                parentUid,
                document.getString("name"),
                document.getString("threatLastAppName"),
                document.getString("threatLastPackageName"),
                document.getString("threatLastContentType"),
                document.getString("threatLastMaskedPreview"),
                document.getString("threatLastMessageText"),
                document.getString("threatLastLevel"),
                document.getString("threatLastDirection"),
                document.getString("threatLastStatus"),
                document.getString("threatLastAiProvider"),
                Boolean.TRUE.equals(document.getBoolean("threatConsultantSuggested")),
                readDateField(document, "threatLastDetectedAt")
        );
    }

    private Date readBestDate(DocumentSnapshot document) {
        Date timestamp = readDateField(document, "timestamp");
        if (timestamp != null) {
            return timestamp;
        }
        Date createdAt = readDateField(document, "createdAt");
        if (createdAt != null) {
            return createdAt;
        }
        return readDateField(document, "updatedAt");
    }

    private Date readDateField(DocumentSnapshot document, String fieldName) {
        try {
            Object value = document.get(fieldName);
            if (value instanceof Timestamp) {
                return ((Timestamp) value).toDate();
            }
            if (value instanceof Date) {
                return (Date) value;
            }
            if (value instanceof Number) {
                long millis = ((Number) value).longValue();
                if (millis > 0L && millis < 100000000000L) {
                    millis *= 1000L;
                }
                return millis > 0L ? new Date(millis) : null;
            }
            if (value instanceof String) {
                String text = ((String) value).trim();
                if (TextUtils.isEmpty(text)) {
                    return null;
                }
                try {
                    long millis = Long.parseLong(text);
                    if (millis > 0L && millis < 100000000000L) {
                        millis *= 1000L;
                    }
                    return millis > 0L ? new Date(millis) : null;
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        } catch (RuntimeException exception) {
            Log.w("ThreatAlerts", "Unable to parse threat alert date field " + fieldName + ".", exception);
        }
        return null;
    }

    private void setIncidentLoading(String message) {
        incidentRows.clear();
        adapter.notifyDataSetChanged();
        rvThreatIncidentList.setVisibility(View.GONE);
        tvThreatEmptyState.setText(message);
        tvThreatEmptyState.setTextColor(Color.parseColor("#757575"));
        tvThreatEmptyState.setVisibility(View.VISIBLE);
    }

    private void setIncidentEmpty(String message) {
        removeThreatListener();
        incidentRows.clear();
        adapter.notifyDataSetChanged();
        rvThreatIncidentList.setVisibility(View.GONE);
        tvThreatEmptyState.setText(message);
        tvThreatEmptyState.setTextColor(Color.parseColor("#757575"));
        tvThreatEmptyState.setVisibility(View.VISIBLE);
        finishThreatRefreshing();
    }

    private void setIncidentError(String message) {
        incidentRows.clear();
        adapter.notifyDataSetChanged();
        rvThreatIncidentList.setVisibility(View.GONE);
        tvThreatEmptyState.setText(message);
        tvThreatEmptyState.setTextColor(Color.parseColor("#C62828"));
        tvThreatEmptyState.setVisibility(View.VISIBLE);
        finishThreatRefreshing();
    }

    private void removeThreatListener() {
        if (threatListenerRegistration != null) {
            threatListenerRegistration.remove();
            threatListenerRegistration = null;
        }
        if (childThreatHistoryListenerRegistration != null) {
            childThreatHistoryListenerRegistration.remove();
            childThreatHistoryListenerRegistration = null;
        }
        primaryIncidentRowsById.clear();
        childHistoryIncidentRowsById.clear();
        activeIncidentChildUid = null;
        activeIncidentParentUid = null;
    }

    private void removeSelectedChildSummaryListener() {
        if (selectedChildSummaryListenerRegistration != null) {
            selectedChildSummaryListenerRegistration.remove();
            selectedChildSummaryListenerRegistration = null;
        }
        activeSummaryChildUid = null;
        activeSummaryParentUid = null;
    }

    private void bindIncidentSummary() {
        if (incidentRows.isEmpty()) {
            return;
        }

        int yellowCount = 0;
        int redCount = 0;
        for (ThreatIncidentRow row : incidentRows) {
            if ("RED".equalsIgnoreCase(row.level)) {
                redCount++;
            } else if ("YELLOW".equalsIgnoreCase(row.level)) {
                yellowCount++;
            }
        }

        if (RoleUtils.ROLE_PARENT.equals(currentRole)) {
            tvThreatOverview.setText("Recent alerts for " + stringOrFallback(currentChildName, "the selected child")
                    + ": " + redCount + " red, " + yellowCount + " yellow.");
        } else {
            tvThreatOverview.setText("Recent alerts on this child device: "
                    + redCount + " red, " + yellowCount + " yellow.");
        }
    }

    private String stringOrFallback(String value, String fallback) {
        return TextUtils.isEmpty(value) ? fallback : value;
    }

    private void loadThreatScanToggle(String childUid) {
        if (TextUtils.isEmpty(childUid) || switchThreatScanEnabled == null) {
            return;
        }
        firestore.collection("users").document(childUid)
                .get()
                .addOnSuccessListener(doc -> {
                    Boolean enabled = doc.getBoolean("threatScanEnabled");
                    currentThreatScanEnabled = enabled == null || Boolean.TRUE.equals(enabled);
                    switchThreatScanEnabled.setOnCheckedChangeListener(null);
                    switchThreatScanEnabled.setChecked(currentThreatScanEnabled);
                    switchThreatScanEnabled.setOnCheckedChangeListener((btn, isChecked) ->
                            writeThreatScanToggle(childUid, isChecked));
                })
                .addOnFailureListener(e -> Log.w(TAG, "Failed to load threatScanEnabled for " + childUid, e));
    }

    private void writeThreatScanToggle(String childUid, boolean enabled) {
        if (TextUtils.isEmpty(childUid)) {
            return;
        }
        Map<String, Object> update = new HashMap<>();
        update.put("threatScanEnabled", enabled);
        firestore.collection("users").document(childUid)
                .set(update, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    currentThreatScanEnabled = enabled;
                    showParentScanStatusNotification(enabled);
                })
                .addOnFailureListener(e -> Log.w(TAG, "Failed to write threatScanEnabled", e));
    }

    private void showParentScanStatusNotification(boolean enabled) {
        createScanStatusNotificationChannel();
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            return;
        }

        String childLabel = TextUtils.isEmpty(currentChildName) ? "your child" : currentChildName;
        String title = enabled ? "AI Cyberbully Scan Enabled" : "AI Cyberbully Scan Disabled";
        String body = enabled
                ? childLabel + "'s messages are now being monitored for serious threats."
                : childLabel + "'s threat monitoring has been paused.";
        Intent intent = new Intent(this, AiThreatDetectionActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(EXTRA_SCROLL_TO, SCROLL_TO_CYBERBULLY_CONTROLS);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                PARENT_SCAN_STATUS_NOTIFICATION_ID,
                intent,
                flags
        );

        try {
            notificationManager.notify(
                    PARENT_SCAN_STATUS_NOTIFICATION_ID,
                    new NotificationCompat.Builder(this, SCAN_STATUS_CHANNEL_ID)
                            .setSmallIcon(R.mipmap.ic_launcher)
                            .setContentTitle(title)
                            .setContentText(body)
                            .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                            .setOngoing(false)
                            .setAutoCancel(true)
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                            .setContentIntent(pendingIntent)
                            .build()
            );
        } catch (RuntimeException exception) {
            Log.w(TAG, "Unable to show parent scan status notification.", exception);
        }
    }

    private void createScanStatusNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if (notificationManager == null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                SCAN_STATUS_CHANNEL_ID,
                "MyNyaman Scan Status",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        channel.setDescription("Shows AI cyberbully scan changes for the selected child.");
        notificationManager.createNotificationChannel(channel);
    }

    private void handleScrollIntentIfNeeded() {
        Intent intent = getIntent();
        if (intent == null || !SCROLL_TO_CYBERBULLY_CONTROLS.equals(intent.getStringExtra(EXTRA_SCROLL_TO))) {
            return;
        }
        if (scrollThreatContent == null || cardCyberbullyControls == null || cardCyberbullyControls.getVisibility() != View.VISIBLE) {
            return;
        }
        scrollThreatContent.post(() -> scrollThreatContent.smoothScrollTo(0, cardCyberbullyControls.getTop()));
        intent.removeExtra(EXTRA_SCROLL_TO);
    }

    private void loadChildCyberbullyStatus(String childUid) {
        if (TextUtils.isEmpty(childUid)) {
            return;
        }
        firestore.collection("users").document(childUid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Boolean enabled = documentSnapshot.getBoolean("threatScanEnabled");
                    bindChildScanStatus(enabled == null || Boolean.TRUE.equals(enabled));
                })
                .addOnFailureListener(e -> bindChildScanStatus(false));

        removeChildCyberbullyStatusListener();
        if (llChildLockedAppsContainer == null) {
            return;
        }
        childCyberbullyStatusListenerRegistration = firestore
                .collection("users")
                .document(childUid)
                .collection("cyberbullyLockedApps")
                .addSnapshotListener((querySnapshot, error) -> {
                    if (llChildLockedAppsContainer == null) {
                        return;
                    }
                    llChildLockedAppsContainer.removeAllViews();
                    if (error != null || querySnapshot == null) {
                        addChildLockedAppsEmptyRow("Unable to load locked apps right now.", "#C62828");
                        return;
                    }

                    boolean hasActiveLocks = false;
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        if (Boolean.TRUE.equals(doc.getBoolean("unlockedByParent"))) {
                            continue;
                        }
                        hasActiveLocks = true;
                        String packageName = doc.getId();
                        String appName = stringOrFallback(doc.getString("appName"), packageName);
                        addChildLockedAppStatusRow(appName);
                    }

                    if (!hasActiveLocks) {
                        addChildLockedAppsEmptyRow("No apps are currently locked.", "#757575");
                    }
                });
    }

    private void bindChildScanStatus(boolean enabled) {
        if (tvChildScanStatus == null) {
            return;
        }
        tvChildScanStatus.setText(enabled ? "Active" : "Paused");
        tvChildScanStatus.setTextColor(Color.parseColor(enabled ? "#2E7D32" : "#757575"));
    }

    private void addChildLockedAppStatusRow(String appName) {
        if (llChildLockedAppsContainer == null) {
            return;
        }
        TextView textView = new TextView(this);
        textView.setText("⚠ " + stringOrFallback(appName, "Supported app") + " — Locked by parent");
        textView.setTextColor(Color.parseColor("#C62828"));
        textView.setTextSize(14f);
        textView.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 10, 0, 0);
        textView.setLayoutParams(params);
        llChildLockedAppsContainer.addView(textView);
    }

    private void addChildLockedAppsEmptyRow(String message, String color) {
        if (llChildLockedAppsContainer == null) {
            return;
        }
        TextView textView = new TextView(this);
        textView.setText(message);
        textView.setTextColor(Color.parseColor(color));
        textView.setTextSize(13f);
        llChildLockedAppsContainer.addView(textView);
    }

    private void removeChildCyberbullyStatusListener() {
        if (childCyberbullyStatusListenerRegistration != null) {
            childCyberbullyStatusListenerRegistration.remove();
            childCyberbullyStatusListenerRegistration = null;
        }
    }

    private void listenForCyberbullyLockedApps(String parentUid, String childUid) {
        if (TextUtils.equals(parentUid, activeCyberbullyLockParentUid)
                && TextUtils.equals(childUid, activeCyberbullyLockChildUid)
                && (cyberbullyLockSummaryListenerRegistration != null
                || parentCyberbullyLockListenerRegistration != null
                || directCyberbullyLockListenerRegistration != null)) {
            renderLockedApps(childUid);
            return;
        }
        removeCyberbullyLockedAppsListeners();
        if (TextUtils.isEmpty(childUid) || llLockedAppsContainer == null) {
            return;
        }

        activeCyberbullyLockParentUid = parentUid;
        activeCyberbullyLockChildUid = childUid;
        renderLockedApps(childUid);

        if (!TextUtils.isEmpty(parentUid)) {
            cyberbullyLockSummaryListenerRegistration = firestore.collection("users")
                    .document(parentUid)
                    .collection("children")
                    .document(childUid)
                    .addSnapshotListener((documentSnapshot, error) -> {
                        summaryLockedAppsByPackage.clear();
                        summaryUnlockedLockedPackages.clear();
                        if (error != null || documentSnapshot == null || !documentSnapshot.exists()) {
                            android.util.Log.w("AiThreatDetection", "Unable to read cyberbully lock summary", error);
                            renderLockedApps(childUid);
                            return;
                        }
                        collectSummaryLockedAppRow(documentSnapshot, summaryLockedAppsByPackage, summaryUnlockedLockedPackages);
                        renderLockedApps(childUid);
                    });

            parentCyberbullyLockListenerRegistration = firestore.collection("users")
                    .document(parentUid)
                    .collection("children")
                    .document(childUid)
                    .collection("cyberbullyLockedApps")
                    .addSnapshotListener((querySnapshot, error) -> {
                        parentLockedAppsByPackage.clear();
                        parentUnlockedLockedPackages.clear();
                        if (error != null || querySnapshot == null) {
                            android.util.Log.w("AiThreatDetection", "Unable to read parent cyberbully locks", error);
                            renderLockedApps(childUid);
                            return;
                        }
                        collectLockedAppRows(querySnapshot.getDocuments(), parentLockedAppsByPackage, parentUnlockedLockedPackages);
                        renderLockedApps(childUid);
                    });
        }

        directCyberbullyLockListenerRegistration = firestore.collection("users")
                .document(childUid)
                .collection("cyberbullyLockedApps")
                .addSnapshotListener((querySnapshot, error) -> {
                    directLockedAppsByPackage.clear();
                    directUnlockedLockedPackages.clear();
                    if (error != null || querySnapshot == null) {
                        android.util.Log.w("AiThreatDetection", "Unable to read child cyberbully locks", error);
                        renderLockedApps(childUid);
                        return;
                    }
                    collectLockedAppRows(querySnapshot.getDocuments(), directLockedAppsByPackage, directUnlockedLockedPackages);
                    renderLockedApps(childUid);
                });
    }

    private void removeCyberbullyLockedAppsListeners() {
        if (cyberbullyLockSummaryListenerRegistration != null) {
            cyberbullyLockSummaryListenerRegistration.remove();
            cyberbullyLockSummaryListenerRegistration = null;
        }
        if (parentCyberbullyLockListenerRegistration != null) {
            parentCyberbullyLockListenerRegistration.remove();
            parentCyberbullyLockListenerRegistration = null;
        }
        if (directCyberbullyLockListenerRegistration != null) {
            directCyberbullyLockListenerRegistration.remove();
            directCyberbullyLockListenerRegistration = null;
        }
        summaryLockedAppsByPackage.clear();
        parentLockedAppsByPackage.clear();
        directLockedAppsByPackage.clear();
        summaryUnlockedLockedPackages.clear();
        parentUnlockedLockedPackages.clear();
        directUnlockedLockedPackages.clear();
        activeCyberbullyLockParentUid = null;
        activeCyberbullyLockChildUid = null;
    }

    private void collectSummaryLockedAppRow(
            DocumentSnapshot document,
            Map<String, LockedAppRow> targetRowsByPackage,
            Set<String> unlockedPackages
    ) {
        Map<?, ?> activeLockedAppNames = asMap(document.get("cyberbullyActiveLockedAppNames"));
        Map<?, ?> activeLockedApps = asMap(document.get("cyberbullyActiveLockedApps"));

        Object rawActivePackages = document.get("cyberbullyActiveLockedPackages");
        if (rawActivePackages instanceof List) {
            boolean addedFromActivePackages = false;
            for (Object rawPackage : (List<?>) rawActivePackages) {
                String packageName = rawPackage == null ? "" : String.valueOf(rawPackage);
                if (TextUtils.isEmpty(packageName)) {
                    continue;
                }
                String appName = summaryAppNameForPackage(packageName, activeLockedAppNames, activeLockedApps);
                targetRowsByPackage.put(packageName, new LockedAppRow(packageName, appName));
                addedFromActivePackages = true;
            }
            if (addedFromActivePackages) {
                return;
            }
        }

        if (activeLockedApps != null) {
            boolean addedFromActiveApps = false;
            for (Map.Entry<?, ?> entry : activeLockedApps.entrySet()) {
                String packageName = entry.getKey() == null ? "" : String.valueOf(entry.getKey());
                String appName = packageName;
                boolean unlockedByParent = false;
                Object rawRow = entry.getValue();
                if (rawRow instanceof Map) {
                    Map<?, ?> row = (Map<?, ?>) rawRow;
                    unlockedByParent = Boolean.TRUE.equals(row.get("unlockedByParent"));
                    packageName = objectStringOrFallback(row.get("packageName"), packageName);
                    appName = objectStringOrFallback(row.get("appName"), packageName);
                }
                if (unlockedByParent && !TextUtils.isEmpty(packageName) && unlockedPackages != null) {
                    unlockedPackages.add(packageName);
                }
                if (!unlockedByParent && !TextUtils.isEmpty(packageName)) {
                    targetRowsByPackage.put(packageName, new LockedAppRow(packageName, appName));
                    addedFromActiveApps = true;
                }
            }
            if (addedFromActiveApps) {
                return;
            }
        }

        String packageName = document.getString("cyberbullyLatestLockedPackage");
        if (!TextUtils.isEmpty(packageName)) {
            if (unlockedPackages != null && unlockedPackages.contains(packageName)) {
                return;
            }
            if (!Boolean.TRUE.equals(document.getBoolean("cyberbullyHasActiveLock"))) {
                return;
            }
            String appName = stringOrFallback(document.getString("cyberbullyLatestLockedAppName"), packageName);
            targetRowsByPackage.put(packageName, new LockedAppRow(packageName, appName));
        }
    }

    private Map<?, ?> asMap(Object value) {
        return value instanceof Map ? (Map<?, ?>) value : null;
    }

    private String summaryAppNameForPackage(String packageName, Map<?, ?> activeLockedAppNames, Map<?, ?> activeLockedApps) {
        if (activeLockedAppNames != null) {
            String appName = objectStringOrFallback(activeLockedAppNames.get(packageName), "");
            if (!TextUtils.isEmpty(appName)) {
                return appName;
            }
        }
        if (activeLockedApps != null) {
            Object rawRow = activeLockedApps.get(packageName);
            if (rawRow instanceof Map) {
                String appName = objectStringOrFallback(((Map<?, ?>) rawRow).get("appName"), "");
                if (!TextUtils.isEmpty(appName)) {
                    return appName;
                }
            }
        }
        return packageName;
    }

    private String objectStringOrFallback(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        return stringOrFallback(String.valueOf(value), fallback);
    }

    private void collectLockedAppRows(
            List<DocumentSnapshot> documents,
            Map<String, LockedAppRow> targetRowsByPackage,
            Set<String> unlockedPackages
    ) {
        for (DocumentSnapshot doc : documents) {
            String packageName = doc.getId();
            if (TextUtils.isEmpty(packageName)) {
                continue;
            }
            Boolean unlocked = doc.getBoolean("unlockedByParent");
            if (Boolean.TRUE.equals(unlocked)) {
                if (unlockedPackages != null) {
                    unlockedPackages.add(packageName);
                }
                continue;
            }
            String appName = stringOrFallback(doc.getString("appName"), packageName);
            targetRowsByPackage.put(packageName, new LockedAppRow(packageName, appName));
        }
    }

    private void renderLockedApps(String childUid) {
        if (llLockedAppsContainer == null) {
            return;
        }

        Map<String, LockedAppRow> rowsByPackage = new LinkedHashMap<>();
        rowsByPackage.putAll(summaryLockedAppsByPackage);
        rowsByPackage.putAll(parentLockedAppsByPackage);
        rowsByPackage.putAll(directLockedAppsByPackage);
        rowsByPackage.keySet().removeAll(summaryUnlockedLockedPackages);
        rowsByPackage.keySet().removeAll(parentUnlockedLockedPackages);
        rowsByPackage.keySet().removeAll(directUnlockedLockedPackages);

        llLockedAppsContainer.removeAllViews();
        if (rowsByPackage.isEmpty()) {
            TextView tvNoLocks = new TextView(this);
            tvNoLocks.setText("No apps are currently locked due to cyberbullying.");
            tvNoLocks.setTextColor(Color.parseColor("#666666"));
            tvNoLocks.setTextSize(13f);
            llLockedAppsContainer.addView(tvNoLocks);
            return;
        }

        for (LockedAppRow row : rowsByPackage.values()) {
            addLockedAppRow(childUid, row.packageName, row.appName);
        }
    }

    private void addLockedAppRow(String childUid, String packageName, String appName) {
        if (llLockedAppsContainer == null) {
            return;
        }

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.setMargins(0, 12, 0, 0);
        row.setLayoutParams(rowParams);

        TextView tvAppLabel = new TextView(this);
        tvAppLabel.setText("⚠ " + appName + " — Locked");
        tvAppLabel.setTextColor(Color.parseColor("#C62828"));
        tvAppLabel.setTextSize(14f);
        tvAppLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tvAppLabel.setLayoutParams(labelParams);

        AppCompatButton btnUnlock = new AppCompatButton(this);
        btnUnlock.setText("Unlock");
        btnUnlock.setTextColor(Color.WHITE);
        btnUnlock.setTextSize(13f);
        btnUnlock.setAllCaps(false);
        btnUnlock.setBackgroundResource(R.drawable.button_blue_rounded);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                (int) (42 * getResources().getDisplayMetrics().density));
        btnUnlock.setLayoutParams(btnParams);
        btnUnlock.setPadding(
                (int) (18 * getResources().getDisplayMetrics().density), 0,
                (int) (18 * getResources().getDisplayMetrics().density), 0);

        btnUnlock.setOnClickListener(v -> {
            btnUnlock.setEnabled(false);
            btnUnlock.setText("Unlocking...");
            FirebaseUser user = mAuth.getCurrentUser();
            String parentUid = user != null ? user.getUid() : "";
            BlueprintSyncRepository.clearCyberbullyLock(firestore, childUid, parentUid, packageName)
                    .addOnSuccessListener(unused -> btnUnlock.setText("Unlocked"))
                    .addOnFailureListener(e -> {
                        btnUnlock.setEnabled(true);
                        btnUnlock.setText("Unlock");
                        Toast.makeText(
                                this,
                                "Unable to unlock this app right now. Please try again.",
                                Toast.LENGTH_SHORT
                        ).show();
                    });
        });

        row.addView(tvAppLabel);
        row.addView(btnUnlock);
        llLockedAppsContainer.addView(row);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_POST_NOTIFICATIONS) {
            if (RoleUtils.ROLE_PARENT.equals(currentRole)) {
                if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "PARENT_PERMISSION_NOTIFICATION_GRANTED");
                } else {
                    Log.d(TAG, "PARENT_PERMISSION_NOTIFICATION_DENIED");
                }
            }
            refreshVisibleState();
        }
    }

    private static class LockedAppRow {
        final String packageName;
        final String appName;

        LockedAppRow(String packageName, String appName) {
            this.packageName = packageName;
            this.appName = appName;
        }
    }

    private static class ThreatIncidentRow {
        final String id;
        final String childUid;
        final String parentUid;
        final String childName;
        final String appName;
        final String packageName;
        final String contentType;
        final String maskedPreview;
        final String messageText;
        final String level;
        final String direction;
        final String status;
        final String aiProvider;
        final boolean consultantSuggested;
        final Date sortDate;

        ThreatIncidentRow(
                String id,
                String childUid,
                String parentUid,
                String childName,
                String appName,
                String packageName,
                String contentType,
                String maskedPreview,
                String messageText,
                String level,
                String direction,
                String status,
                String aiProvider,
                boolean consultantSuggested,
                Date sortDate
        ) {
            this.id = id;
            this.childUid = childUid;
            this.parentUid = parentUid;
            this.childName = childName;
            this.appName = appName;
            this.packageName = packageName;
            this.contentType = contentType;
            this.maskedPreview = maskedPreview;
            this.messageText = messageText;
            this.level = level;
            this.direction = direction;
            this.status = status;
            this.aiProvider = aiProvider;
            this.consultantSuggested = consultantSuggested;
            this.sortDate = sortDate;
        }
    }

    private static class ThreatIncidentAdapter extends RecyclerView.Adapter<ThreatIncidentAdapter.ViewHolder> {
        private final List<ThreatIncidentRow> rows;

        ThreatIncidentAdapter(List<ThreatIncidentRow> rows) {
            this.rows = rows;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflaterFrom(parent).inflate(R.layout.item_threat_incident, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ThreatIncidentRow row = rows.get(position);
            holder.cardThreatItem.setCardBackgroundColor(ThreatNotificationHelper.resolvePastelColor(row.level));
            holder.tvThreatItemApp.setText(TextUtils.isEmpty(row.appName) ? "Supported App" : row.appName);
            holder.tvThreatItemLevel.setText(TextUtils.isEmpty(row.level) ? "YELLOW" : row.level);
            holder.tvThreatItemLevel.setTextColor(resolveLevelColor(row.level));
            holder.tvThreatItemTimestamp.setText(formatTimestamp(row.sortDate));
            holder.tvThreatItemMaskedPreview.setText("Masked preview: " + stringOrFallback(row.maskedPreview, "Not available"));
            holder.tvThreatItemMessage.setText("Full in-app message: " + stringOrFallback(row.messageText, "Not available"));

            String severityLabel = "RED".equalsIgnoreCase(row.level) ? "major" : "minor";
            String sourceLabel = isManualReport(row) ? "Manual Report" : "Automatic AI Detection";
            String consultantText = row.consultantSuggested
                    ? "Consultant support suggested"
                    : "Consultant support optional";
            holder.tvThreatItemMeta.setText(
                    "Source: " + sourceLabel
                            + " - Direction: " + stringOrFallback(row.direction, "visible")
                            + " - Severity: " + severityLabel
                            + " - " + consultantText
            );
        }

        @Override
        public int getItemCount() {
            return rows.size();
        }

        private int resolveLevelColor(String level) {
            if ("RED".equalsIgnoreCase(level)) {
                return Color.parseColor("#C62828");
            }
            if ("YELLOW".equalsIgnoreCase(level)) {
                return Color.parseColor("#B26A00");
            }
            return Color.parseColor("#2E7D32");
        }

        private String formatTimestamp(Date date) {
            if (date == null) {
                return "Time unavailable";
            }
            return new SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(date);
        }

        private boolean isManualReport(ThreatIncidentRow row) {
            return row != null
                    && ("manual_report".equalsIgnoreCase(row.direction)
                    || "manual_report".equalsIgnoreCase(row.contentType)
                    || "manual".equalsIgnoreCase(row.aiProvider));
        }

        private String stringOrFallback(String value, String fallback) {
            return TextUtils.isEmpty(value) ? fallback : value;
        }

        private android.view.LayoutInflater getLayoutInflaterFrom(ViewGroup parent) {
            return android.view.LayoutInflater.from(parent.getContext());
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            final CardView cardThreatItem;
            final TextView tvThreatItemApp;
            final TextView tvThreatItemLevel;
            final TextView tvThreatItemTimestamp;
            final TextView tvThreatItemMeta;
            final TextView tvThreatItemMaskedPreview;
            final TextView tvThreatItemMessage;

            ViewHolder(View itemView) {
                super(itemView);
                cardThreatItem = (CardView) itemView;
                tvThreatItemApp = itemView.findViewById(R.id.tvThreatItemApp);
                tvThreatItemLevel = itemView.findViewById(R.id.tvThreatItemLevel);
                tvThreatItemTimestamp = itemView.findViewById(R.id.tvThreatItemTimestamp);
                tvThreatItemMeta = itemView.findViewById(R.id.tvThreatItemMeta);
                tvThreatItemMaskedPreview = itemView.findViewById(R.id.tvThreatItemMaskedPreview);
                tvThreatItemMessage = itemView.findViewById(R.id.tvThreatItemMessage);
            }
        }
    }
}
