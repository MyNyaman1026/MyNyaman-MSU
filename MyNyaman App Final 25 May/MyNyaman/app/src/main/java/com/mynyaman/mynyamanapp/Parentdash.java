package com.mynyaman.mynyamanapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatButton;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Parentdash extends BaseActivity {

    private static final String TAG = "Parentdash";
    private static final int REQUEST_POST_NOTIFICATIONS_PARENT = 201;

    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private TextView tvWelcome;
    private SwipeRefreshLayout swipeRefreshLayout;
    private LinearLayout childChipContainer;
    private TextView tvNoChildren;
    private TextView tvThreatDetectionSubtitle;
    private TextView tvThreatDetectionStatus;
    private TextView tvParentAlertPermissionStatus;
    private CardView cardThreatDetection;
    private CardView cardParentAlertPermissions;
    private AppCompatButton btnParentEnableNotifications;
    private AppCompatButton btnParentAllowBackground;
    private AppCompatButton btnParentOpenNotificationSettings;
    private AppCompatButton btnParentOpenAppSettings;
    private final List<ChildAccountRow> childAccounts = new ArrayList<>();

    // Live Firestore listener for real-time threat card updates.
    private ListenerRegistration liveThreatListenerRegistration;
    private String liveThreatListenerChildUid;
    private boolean notificationPermissionRequestedThisSession = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.parentdash);

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, Login.class));
            finish();
            return;
        }

        tvWelcome = findViewById(R.id.tvWelcomeParent);
        CardView cardMonitoring = findViewById(R.id.cardMonitoring);
        ImageView icNotification = findViewById(R.id.icNotification);
        AppCompatButton btnMyFamily = findViewById(R.id.btnMyFamily);
        LinearLayout btnChangeChild = findViewById(R.id.btnAddMember);
        AppCompatButton btnHeroDailyLimit = findViewById(R.id.btnHeroDailyLimit);
        CardView cardDailyLimit = findViewById(R.id.cardDailyLimit);
        CardView cardUsageReport = findViewById(R.id.cardUsageReport);
        cardThreatDetection = findViewById(R.id.cardThreatDetection);
        cardParentAlertPermissions = findViewById(R.id.cardParentAlertPermissions);
        CardView cardChat = findViewById(R.id.cardChatConsultants);
        CardView cardReportThreats = findViewById(R.id.cardReportThreatsParent);
        CardView cardBlockSites = findViewById(R.id.cardBlockSites);
        CardView cardTutorial = findViewById(R.id.cardTutorial);
        LinearLayout navKnowledge = findViewById(R.id.navKnowledge);
        LinearLayout navProfile = findViewById(R.id.navProfile);
        LinearLayout navMenu = findViewById(R.id.navMenu);
        AppCompatButton menuSettings = findViewById(R.id.menuSettings);
        tvThreatDetectionSubtitle = findViewById(R.id.tvThreatDetectionSubtitle);
        tvThreatDetectionStatus = findViewById(R.id.tvThreatDetectionStatus);
        tvParentAlertPermissionStatus = findViewById(R.id.tvParentAlertPermissionStatus);
        btnParentEnableNotifications = findViewById(R.id.btnParentEnableNotifications);
        btnParentAllowBackground = findViewById(R.id.btnParentAllowBackground);
        btnParentOpenNotificationSettings = findViewById(R.id.btnParentOpenNotificationSettings);
        btnParentOpenAppSettings = findViewById(R.id.btnParentOpenAppSettings);

        if (cardMonitoring != null) {
            cardMonitoring.setVisibility(View.VISIBLE);
        }

        setupResponsiveHeaderSpacing(btnMyFamily, icNotification);
        setupSwipeRefresh();
        setupChildTabs();

        icNotification.setOnClickListener(v -> Toast.makeText(this, "Notifications empty", Toast.LENGTH_SHORT).show());
        btnMyFamily.setOnClickListener(v -> {
            ChildAccountRow activeChild = findSelectedChild();
            if (activeChild == null) {
                startActivity(new Intent(this, Addchild.class));
                return;
            }
            openChildActions(activeChild);
        });
        btnChangeChild.setOnClickListener(v -> startActivity(new Intent(this, Addchild.class)));

        btnHeroDailyLimit.setOnClickListener(v -> openDailyLimit());
        cardDailyLimit.setOnClickListener(v -> openDailyLimit());
        cardUsageReport.setOnClickListener(v -> startActivity(new Intent(this, Usagereport.class)));
        if (cardThreatDetection != null) {
            cardThreatDetection.setOnClickListener(v -> {
                ChildAccountRow activeChild = findSelectedChild();
                if (activeChild == null) {
                    Toast.makeText(this, "Add a child account first.", Toast.LENGTH_SHORT).show();
                    return;
                }
                requestNotificationPermissionIfNeeded();
                Intent intent = new Intent(this, AiThreatDetectionActivity.class);
                intent.putExtra("childUid", activeChild.uid);
                startActivity(intent);
            });
        }
        if (btnParentEnableNotifications != null) {
            btnParentEnableNotifications.setOnClickListener(v -> enableParentNotificationsFromDashboard());
        }
        if (btnParentAllowBackground != null) {
            btnParentAllowBackground.setOnClickListener(v -> openParentBatterySettings());
        }
        if (btnParentOpenNotificationSettings != null) {
            btnParentOpenNotificationSettings.setOnClickListener(v -> openParentNotificationSettings());
        }
        if (btnParentOpenAppSettings != null) {
            btnParentOpenAppSettings.setOnClickListener(v -> openParentAppSettings());
        }
        cardChat.setOnClickListener(v -> startActivity(ConsultantHubActivity.createIntent(this, RoleUtils.ROLE_PARENT)));
        if (cardReportThreats != null) {
            cardReportThreats.setOnClickListener(v -> {
                ChildAccountRow activeChild = findSelectedChild();
                if (activeChild == null) {
                    Toast.makeText(this, "Add a child account first.", Toast.LENGTH_SHORT).show();
                    return;
                }
                startActivity(ThreatReportListActivity.createIntent(this, RoleUtils.ROLE_PARENT, activeChild.uid, activeChild.name));
            });
        }
        if (cardBlockSites != null) {
            cardBlockSites.setOnClickListener(v -> startActivity(new Intent(this, Blocksite.class)));
        }
        if (cardTutorial != null) {
            cardTutorial.setOnClickListener(v -> startActivity(new Intent(this, Tutorial.class)));
        }

        if (navKnowledge != null) {
            navKnowledge.setOnClickListener(v -> startActivity(new Intent(this, Tutorial.class)));
        }
        navProfile.setOnClickListener(v -> startActivity(new Intent(this, Parentprofile.class)));
        navMenu.setOnClickListener(v -> startActivity(new Intent(this, FullMenu.class)));

        if (menuSettings != null) {
            menuSettings.setOnClickListener(v -> Toast.makeText(this, "Settings coming soon!", Toast.LENGTH_SHORT).show());
        }

        // Request notification permission right away on Android 13+.
        requestNotificationPermissionIfNeeded();
        startParentAlertServiceIfAllowed();
        refreshParentAlertPermissionCard();

        loadSummary();
        loadChildren();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSummary();
        loadChildren();
        loadThreatSummary();
        startParentAlertServiceIfAllowed();
        refreshParentAlertPermissionCard();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopLiveThreatListener();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_POST_NOTIFICATIONS_PARENT) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "PARENT_PERMISSION_NOTIFICATION_GRANTED");
                startParentAlertServiceIfAllowed();
            } else {
                Log.d(TAG, "PARENT_PERMISSION_NOTIFICATION_DENIED");
                Toast.makeText(this,
                        "Notification permission is required to receive threat alerts on this device.",
                        Toast.LENGTH_LONG).show();
            }
            refreshParentAlertPermissionCard();
        }
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED
                && !notificationPermissionRequestedThisSession) {
            notificationPermissionRequestedThisSession = true;
            Log.d(TAG, "Requesting POST_NOTIFICATIONS permission");
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_POST_NOTIFICATIONS_PARENT);
        }
    }

    private void enableParentNotificationsFromDashboard() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            if (!notificationPermissionRequestedThisSession) {
                requestNotificationPermissionIfNeeded();
            } else {
                openParentNotificationSettings();
            }
            return;
        }
        openParentNotificationSettings();
    }

    private void openParentNotificationSettings() {
        Log.d(TAG, "PARENT_PERMISSION_OPENED_NOTIFICATION_SETTINGS");
        openSettingsSafely(ThreatMonitoringPermissionHelper.newNotificationSettingsIntent(this));
    }

    private void openParentBatterySettings() {
        if (ThreatMonitoringPermissionHelper.isIgnoringBatteryOptimizations(this)) {
            Log.d(TAG, "PARENT_PERMISSION_BATTERY_OPTIMIZATION_IGNORED");
            refreshParentAlertPermissionCard();
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
                Log.d(TAG, "PARENT_PERMISSION_OPENED_APP_SETTINGS");
                startActivity(ThreatMonitoringPermissionHelper.newAppDetailsIntent(this));
            } catch (Exception secondException) {
                Log.w(TAG, "Unable to open parent permission settings.", secondException);
            }
        }
    }

    private void refreshParentAlertPermissionCard() {
        if (cardParentAlertPermissions == null || tvParentAlertPermissionStatus == null) {
            return;
        }
        boolean notificationsEnabled = ThreatMonitoringPermissionHelper.hasNotificationPermission(this);
        boolean batteryAllowed = ThreatMonitoringPermissionHelper.isIgnoringBatteryOptimizations(this);
        tvParentAlertPermissionStatus.setText(
                "Notifications: " + (notificationsEnabled ? "Enabled" : "Disabled")
                        + "\nBattery background access: " + (batteryAllowed ? "Allowed" : "Needs setup")
                        + "\nParent alert listener: " + (notificationsEnabled ? "Running" : "Not running")
        );
        if (btnParentEnableNotifications != null) {
            btnParentEnableNotifications.setVisibility(notificationsEnabled ? View.GONE : View.VISIBLE);
        }
        if (btnParentAllowBackground != null) {
            btnParentAllowBackground.setVisibility(batteryAllowed ? View.GONE : View.VISIBLE);
        }
    }

    private void setupResponsiveHeaderSpacing(AppCompatButton btnMyFamily, ImageView icNotification) {
        ViewGroup.MarginLayoutParams buttonParams = (ViewGroup.MarginLayoutParams) btnMyFamily.getLayoutParams();
        buttonParams.setMarginEnd(dpToPx(14));
        btnMyFamily.setLayoutParams(buttonParams);

        ViewGroup.MarginLayoutParams notificationParams = (ViewGroup.MarginLayoutParams) icNotification.getLayoutParams();
        notificationParams.setMarginStart(dpToPx(6));
        icNotification.setLayoutParams(notificationParams);
    }

    private void setupSwipeRefresh() {
        ScrollView dashboardScroll = findViewById(R.id.dashboardScroll);
        ViewGroup parent = (ViewGroup) dashboardScroll.getParent();
        if (parent instanceof SwipeRefreshLayout) {
            swipeRefreshLayout = (SwipeRefreshLayout) parent;
            swipeRefreshLayout.setOnRefreshListener(() -> {
                loadSummary();
                loadChildren();
            });
            return;
        }

        int index = parent.indexOfChild(dashboardScroll);
        ViewGroup.LayoutParams layoutParams = dashboardScroll.getLayoutParams();
        parent.removeView(dashboardScroll);

        swipeRefreshLayout = new SwipeRefreshLayout(this);
        swipeRefreshLayout.setLayoutParams(layoutParams);
        swipeRefreshLayout.addView(dashboardScroll, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        parent.addView(swipeRefreshLayout, index);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadSummary();
            loadChildren();
        });
    }

    private void setupChildTabs() {
        LinearLayout llChildTabs = findViewById(R.id.llChildTabs);
        AppCompatButton btnChild1 = findViewById(R.id.btnChild1);
        AppCompatButton btnChild2 = findViewById(R.id.btnChild2);

        llChildTabs.removeView(btnChild1);
        llChildTabs.removeView(btnChild2);

        HorizontalScrollView horizontalScrollView = new HorizontalScrollView(this);
        horizontalScrollView.setHorizontalScrollBarEnabled(false);
        horizontalScrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        horizontalScrollView.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        childChipContainer = new LinearLayout(this);
        childChipContainer.setOrientation(LinearLayout.HORIZONTAL);
        childChipContainer.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        horizontalScrollView.addView(childChipContainer);
        llChildTabs.addView(horizontalScrollView, 0);

        tvNoChildren = new TextView(this);
        tvNoChildren.setText("No child accounts linked yet.");
        tvNoChildren.setTextColor(0xFF757575);
        tvNoChildren.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tvNoChildren.setVisibility(View.GONE);
        LinearLayout headerContainer = findViewById(R.id.headerContainer);
        headerContainer.addView(tvNoChildren, headerContainer.indexOfChild(llChildTabs) + 1);
    }

    private void loadSummary() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        firestore.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String savedName = documentSnapshot.getString("name");
                    if (!TextUtils.isEmpty(savedName)) {
                        tvWelcome.setText("Welcome, " + savedName);
                    }
                });
    }

    private void loadChildren() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        firestore.collection("users")
                .document(currentUser.getUid())
                .collection("children")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    childAccounts.clear();
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        childAccounts.add(new ChildAccountRow(
                                document.getId(),
                                document.getString("name"),
                                document.getString("email")
                        ));
                    }
                    Collections.sort(childAccounts, Comparator.comparing(child -> safeName(child.name)));
                    ensureSelectedChild();
                    renderChildTabs();
                    loadThreatSummary();
                    startLiveThreatListener();
                    finishRefreshing();
                })
                .addOnFailureListener(e -> {
                    resetThreatSummary();
                    finishRefreshing();
                    Toast.makeText(this, "Unable to load child accounts.", Toast.LENGTH_SHORT).show();
                });
    }

    private void renderChildTabs() {
        childChipContainer.removeAllViews();
        boolean hasChildren = !childAccounts.isEmpty();
        tvNoChildren.setVisibility(hasChildren ? View.GONE : View.VISIBLE);
        String selectedChildUid = ensureSelectedChild();

        for (ChildAccountRow child : childAccounts) {
            AppCompatButton chip = new AppCompatButton(this);
            chip.setBackgroundResource(R.drawable.child_chip_background);
            chip.setTextColor(0xFFFFFFFF);
            chip.setText(TextUtils.isEmpty(child.name) ? "Child" : child.name);
            chip.setAllCaps(false);
            chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            chip.setTypeface(chip.getTypeface(), Typeface.BOLD);
            chip.setMinWidth(dpToPx(132));
            chip.setPadding(dpToPx(22), 0, dpToPx(22), 0);
            chip.setSelected(child.uid.equals(selectedChildUid));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    dpToPx(40)
            );
            params.setMarginEnd(dpToPx(10));
            chip.setLayoutParams(params);
            chip.setOnClickListener(v -> onChildChipClicked(child));
            childChipContainer.addView(chip);
        }
    }

    private void onChildChipClicked(ChildAccountRow child) {
        String currentSelected = SelectedChildStore.getSelectedChildUid(this);
        SelectedChildStore.saveSelectedChildUid(this, child.uid);
        renderChildTabs();
        loadThreatSummary();
        startLiveThreatListener();

        if (child.uid.equals(currentSelected)) {
            openChildActions(child);
        }
    }

    private void openChildActions(ChildAccountRow child) {
        Intent intent = new Intent(this, ChildAccountActionsActivity.class);
        intent.putExtra(ChildAccountActionsActivity.EXTRA_CHILD_UID, child.uid);
        intent.putExtra(ChildAccountActionsActivity.EXTRA_CHILD_NAME, child.name);
        startActivity(intent);
    }

    private void openDailyLimit() {
        ChildAccountRow activeChild = findSelectedChild();
        if (activeChild == null) {
            Toast.makeText(this, "Add a child account first.", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, Dailylimit.class);
        intent.putExtra("CHILD_UID", activeChild.uid);
        intent.putExtra("CHILD_NAME", activeChild.name);
        startActivity(intent);
    }

    private ChildAccountRow findSelectedChild() {
        String selectedChildUid = ensureSelectedChild();
        for (ChildAccountRow child : childAccounts) {
            if (child.uid.equals(selectedChildUid)) {
                return child;
            }
        }
        return childAccounts.isEmpty() ? null : childAccounts.get(0);
    }

    private String ensureSelectedChild() {
        List<String> childUids = new ArrayList<>();
        for (ChildAccountRow child : childAccounts) {
            childUids.add(child.uid);
        }
        return SelectedChildStore.resolveSelectedChildUid(this, childUids);
    }

    private void finishRefreshing() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    // Threat Summary: one-shot read

    private void loadThreatSummary() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        String selectedChildUid = SelectedChildStore.getSelectedChildUid(this);
        if (currentUser == null || TextUtils.isEmpty(selectedChildUid)) {
            resetThreatSummary();
            return;
        }

        firestore.collection("users")
                .document(currentUser.getUid())
                .collection("children")
                .document(selectedChildUid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot == null || !documentSnapshot.exists()) {
                        resetThreatSummary();
                        return;
                    }
                    updateThreatCardFromSnapshot(documentSnapshot);
                })
                .addOnFailureListener(e -> resetThreatSummary());
    }

    // Live Threat Listener: real-time Firestore updates

    /**
     * Attach a SnapshotListener to the selected child's user document under
     * users/{parentUid}/children/{childUid}. When LimitService writes a new
     * threat summary (threatLastLevel, threatLastAppName, etc.), the parent dashboard
     * card updates in real time without needing a manual refresh.
     */
    private void startLiveThreatListener() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        String selectedChildUid = SelectedChildStore.getSelectedChildUid(this);
        if (currentUser == null || TextUtils.isEmpty(selectedChildUid)) {
            stopLiveThreatListener();
            return;
        }

        // Already listening for this child? Skip.
        if (TextUtils.equals(selectedChildUid, liveThreatListenerChildUid)
                && liveThreatListenerRegistration != null) {
            return;
        }

        // Stop any previous listener (e.g. child switched).
        stopLiveThreatListener();

        liveThreatListenerChildUid = selectedChildUid;
        Log.d(TAG, "Starting live threat listener for child " + selectedChildUid);

        liveThreatListenerRegistration = firestore.collection("users")
                .document(currentUser.getUid())
                .collection("children")
                .document(selectedChildUid)
                .addSnapshotListener((documentSnapshot, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Live threat listener error", error);
                        return;
                    }
                    if (documentSnapshot == null || !documentSnapshot.exists()) {
                        return;
                    }
                    Log.d(TAG, "Live threat update received for child " + selectedChildUid);
                    updateThreatCardFromSnapshot(documentSnapshot);
                });
    }

    private void stopLiveThreatListener() {
        if (liveThreatListenerRegistration != null) {
            liveThreatListenerRegistration.remove();
            liveThreatListenerRegistration = null;
        }
        liveThreatListenerChildUid = null;
    }

    /**
     * Shared logic: update the threat detection card from a Firestore document snapshot.
     * Called both by the one-shot loadThreatSummary() and the live SnapshotListener.
     */
    private void updateThreatCardFromSnapshot(DocumentSnapshot documentSnapshot) {
        String childName = documentSnapshot.getString("name");
        String threatLevel = documentSnapshot.getString("threatLastLevel");
        String appName = documentSnapshot.getString("threatLastAppName");
        String maskedPreview = documentSnapshot.getString("threatLastMaskedPreview");
        String monitoringStatus = documentSnapshot.getString("threatMonitoringStatusLabel");

        if (TextUtils.isEmpty(threatLevel)) {
            if (cardThreatDetection != null) {
                cardThreatDetection.setCardBackgroundColor(
                        "Monitoring Active".equalsIgnoreCase(monitoringStatus)
                                ? ThreatNotificationHelper.resolvePastelColor(ThreatResult.Level.GREEN)
                                : 0xFFFFFFFF
                );
            }
            tvThreatDetectionSubtitle.setText(
                    "AI Threat Detection for " + safeChildDisplayName(childName) + ": "
                            + (TextUtils.isEmpty(monitoringStatus) ? "No synced setup status yet." : monitoringStatus) + "."
            );
            if (ThreatMonitoringPermissionHelper.hasNotificationPermission(this)) {
                tvThreatDetectionStatus.setText("Parent notifications are enabled on this phone.");
            } else {
                tvThreatDetectionStatus.setText("Enable Parent Alerts");
            }
            return;
        }

        // Dynamic card color update based on threat level.
        if (cardThreatDetection != null) {
            cardThreatDetection.setCardBackgroundColor(ThreatNotificationHelper.resolvePastelColor(threatLevel));
        }
        tvThreatDetectionSubtitle.setText("Latest alert for " + safeChildDisplayName(childName) + ": "
                + threatLevel + (TextUtils.isEmpty(appName) ? "" : " in " + appName) + ".");
        if (!ThreatMonitoringPermissionHelper.hasNotificationPermission(this)) {
            tvThreatDetectionStatus.setText("Enable Parent Alerts");
        } else {
            tvThreatDetectionStatus.setText(TextUtils.isEmpty(maskedPreview)
                    ? "Open the threat section for the full report."
                    : "Masked preview: " + maskedPreview);
        }
    }

    private void resetThreatSummary() {
        if (cardThreatDetection != null) {
            cardThreatDetection.setCardBackgroundColor(0xFFFFFFFF);
        }
        if (tvThreatDetectionSubtitle != null) {
            tvThreatDetectionSubtitle.setText(
                    "Monitor possible cyberbullying, harmful messages, and unsafe communication patterns automatically."
            );
        }
        if (tvThreatDetectionStatus != null) {
            tvThreatDetectionStatus.setText("Open the threat section for live setup and alert history.");
        }
    }

    private void startParentAlertServiceIfAllowed() {
        if (!ThreatMonitoringPermissionHelper.hasNotificationPermission(this)) {
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
            Toast.makeText(this, "Parent alert monitor will start from AI Threat Detection when available.", Toast.LENGTH_SHORT).show();
        }
    }

    private String safeChildDisplayName(String childName) {
        return TextUtils.isEmpty(childName) ? "the selected child" : childName;
    }

    private String safeName(String name) {
        return TextUtils.isEmpty(name) ? "" : name.toLowerCase();
    }

    private int dpToPx(int dp) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics()
        ));
    }

    private static class ChildAccountRow {
        final String uid;
        final String name;
        final String email;

        ChildAccountRow(String uid, String name, String email) {
            this.uid = uid;
            this.name = name;
            this.email = email;
        }
    }
}
