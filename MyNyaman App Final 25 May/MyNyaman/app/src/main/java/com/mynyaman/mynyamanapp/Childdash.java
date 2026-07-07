package com.mynyaman.mynyamanapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class Childdash extends BaseActivity {

    private static final int REQUEST_POST_NOTIFICATIONS = 101;

    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private TextView tvWelcome;
    private TextView tvMenuSubtitle;
    private SwipeRefreshLayout swipeRefreshLayout;
    private long lastThreatSetupPromptAt = 0L;
    private boolean notificationPermissionRequestedThisSession = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.childdash);

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, Login.class));
            finish();
            return;
        }

        tvWelcome = findViewById(R.id.tvWelcomeChild);
        tvMenuSubtitle = findViewById(R.id.tvMenuSubtitle);
        ImageView ivHamburger = findViewById(R.id.ivHamburger);
        ImageView icNotification = findViewById(R.id.icNotification);
        CardView cardTutorial = findViewById(R.id.cardTutorialChild);
        CardView cardChat = findViewById(R.id.cardChatChild);
        CardView cardProfile = findViewById(R.id.cardProfileChild);
        CardView cardReportThreats = findViewById(R.id.cardReportThreatsChild);
        CardView cardUsage = findViewById(R.id.cardUsageChild);
        CardView cardThreatMonitoring = findViewById(R.id.cardThreatMonitoringChild);
        LinearLayout navProfile = findViewById(R.id.navProfile);
        LinearLayout navMenu = findViewById(R.id.navMenu);
        LinearLayout navKnowledge = findViewById(R.id.navKnowledge);

        setupSwipeRefresh();
        stopService(new Intent(this, ParentThreatAlertService.class));
        ensurePermissions();
        startLimitService(LimitService.ACTION_START_OR_REFRESH);

        if (ivHamburger != null) {
            ivHamburger.setOnClickListener(v -> startActivity(new Intent(this, FullMenu.class)));
        }
        if (icNotification != null) {
            icNotification.setOnClickListener(v -> Toast.makeText(this, "Notifications empty", Toast.LENGTH_SHORT).show());
        }
        if (cardTutorial != null) {
            cardTutorial.setOnClickListener(v -> startActivity(new Intent(this, Tutorialchild.class)));
        }
        if (cardChat != null) {
            cardChat.setOnClickListener(v -> startActivity(ConsultantHubActivity.createIntent(this, RoleUtils.ROLE_CHILD)));
        }
        if (cardUsage != null) {
            cardUsage.setOnClickListener(v -> startActivity(new Intent(this, Usagereportchild.class)));
        }
        if (cardThreatMonitoring != null) {
            cardThreatMonitoring.setOnClickListener(v -> startActivity(new Intent(this, AiThreatDetectionActivity.class)));
        }
        if (cardProfile != null) {
            cardProfile.setOnClickListener(v -> startActivity(new Intent(this, ProfileeditChild.class)));
        }
        if (cardReportThreats != null) {
            cardReportThreats.setOnClickListener(v -> startActivity(ThreatReportListActivity.createIntent(this, RoleUtils.ROLE_CHILD, null, null)));
        }

        if (navProfile != null && cardProfile != null) {
            navProfile.setOnClickListener(v -> cardProfile.performClick());
        }
        if (navMenu != null) {
            navMenu.setOnClickListener(v -> startActivity(new Intent(this, FullMenu.class)));
        }
        if (navKnowledge != null) {
            navKnowledge.setOnClickListener(v -> startActivity(new Intent(this, Tutorialchild.class)));
        }

        checkPendingDeletion();
        loadSummary();
    }

    @Override
    protected void onResume() {
        super.onResume();
        stopService(new Intent(this, ParentThreatAlertService.class));
        ensurePermissions();
        startLimitService(LimitService.ACTION_START_OR_REFRESH);
        checkPendingDeletion();
        loadSummary();
    }

    private void ensurePermissions() {
        if (requestRuntimePermissionIfNeeded()) {
            return;
        }

        syncThreatMonitoringStatus();
        maybePromptThreatSetup();
    }

    private boolean requestRuntimePermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                && !notificationPermissionRequestedThisSession) {
            notificationPermissionRequestedThisSession = true;
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_POST_NOTIFICATIONS);
            return true;
        }

        return false;
    }

    private void maybePromptThreatSetup() {
        if (ThreatMonitoringPermissionHelper.hasAnyRequiredSetupMissing(this)
                && SystemClock.uptimeMillis() - lastThreatSetupPromptAt > 10000L) {
            lastThreatSetupPromptAt = SystemClock.uptimeMillis();
            Toast.makeText(this, "Complete AI Threat Detection setup from the dashboard card to keep monitoring reliable.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_POST_NOTIFICATIONS) {
            syncThreatMonitoringStatus();
            ensurePermissions();
        }
    }

    private void setupSwipeRefresh() {
        ScrollView dashboardScroll = findViewById(R.id.dashboardScroll);
        if (dashboardScroll == null) {
            swipeRefreshLayout = null;
            return;
        }

        ViewGroup parent = (ViewGroup) dashboardScroll.getParent();
        if (parent instanceof SwipeRefreshLayout) {
            swipeRefreshLayout = (SwipeRefreshLayout) parent;
            swipeRefreshLayout.setOnRefreshListener(() -> {
                loadSummary();
                startLimitService(LimitService.ACTION_FORCE_SYNC);
                swipeRefreshLayout.setRefreshing(false);
            });
            return;
        }

        if (parent == null) {
            swipeRefreshLayout = null;
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
            startLimitService(LimitService.ACTION_FORCE_SYNC);
            swipeRefreshLayout.setRefreshing(false);
        });
    }

    private void startLimitService(String action) {
        Intent serviceIntent = new Intent(this, LimitService.class);
        serviceIntent.setAction(action);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        } catch (RuntimeException ignored) {
            Toast.makeText(this, "Protection service will retry after permissions are ready.", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadSummary() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null || tvWelcome == null || tvMenuSubtitle == null) {
            return;
        }

        firestore.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String savedName = documentSnapshot.getString("name");
                    String savedRole = documentSnapshot.getString("role");
                    if (!TextUtils.isEmpty(savedName)) {
                        tvWelcome.setText("Welcome, " + savedName);
                    }
                    if (!TextUtils.isEmpty(savedRole)) {
                        tvMenuSubtitle.setText(savedRole + " Account");
                    }
        });
    }

    private void syncThreatMonitoringStatus() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        firestore.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> BlueprintSyncRepository.syncThreatMonitoringStatus(
                        firestore,
                        currentUser.getUid(),
                        documentSnapshot.getString("parentUid"),
                        documentSnapshot.getString("name"),
                        ThreatMonitoringPermissionHelper.buildStatusPayload(this)
                ));
    }

    private void checkPendingDeletion() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        firestore.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Boolean pendingDeletion = documentSnapshot.getBoolean("pendingDeletion");
                    if (Boolean.TRUE.equals(pendingDeletion)) {
                        currentUser.delete()
                                .addOnSuccessListener(unused -> {
                                    firestore.collection("users").document(currentUser.getUid()).delete();
                                    mAuth.signOut();
                                    stopService(new Intent(this, LimitService.class));
                                    Toast.makeText(this, "This child account has been deleted.", Toast.LENGTH_LONG).show();
                                    Intent intent = new Intent(this, Login.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    mAuth.signOut();
                                    stopService(new Intent(this, LimitService.class));
                                    Toast.makeText(this, "Deletion is pending. Please sign in again if this account still appears.", Toast.LENGTH_LONG).show();
                                    Intent intent = new Intent(this, Login.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    finish();
                                });
                    }
                });
    }
}

