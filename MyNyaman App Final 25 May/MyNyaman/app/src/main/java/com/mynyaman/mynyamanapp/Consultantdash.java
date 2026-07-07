package com.mynyaman.mynyamanapp;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatButton;
import androidx.cardview.widget.CardView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class Consultantdash extends BaseActivity {

    private static final String USERS_COLLECTION = "users";
    private static final String DEFAULT_ROLE = "Consultant";

    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private DrawerLayout drawerLayout;
    private TextView tvWelcomeConsultant;
    private TextView tvMenuSubtitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_consultantdash);

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, Login.class));
            finish();
            return;
        }

        drawerLayout = findViewById(R.id.drawerLayout);
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        ImageView ivHamburger = findViewById(R.id.ivHamburger);
        ImageView icProfileTop = findViewById(R.id.icProfileTop);
        ImageView icNotification = findViewById(R.id.icNotification);
        ImageView menuProfileAvatar = findViewById(R.id.menuProfileAvatar);
        tvWelcomeConsultant = findViewById(R.id.tvWelcomeConsultant);
        tvMenuSubtitle = findViewById(R.id.tvMenuSubtitle);

        AppCompatButton btnMyDesk = findViewById(R.id.btnMyDesk);
        CardView cardReports = findViewById(R.id.cardReportsConsultant);
        CardView cardConsultation = findViewById(R.id.cardConsultationConsultant);
        CardView cardSessionHistory = findViewById(R.id.cardSessionHistoryConsultant);
        CardView cardProfile = findViewById(R.id.cardProfileConsultant);
        CardView cardInfo = findViewById(R.id.cardInfoConsultant);

        LinearLayout navProfile = findViewById(R.id.navProfile);
        LinearLayout navMenu = findViewById(R.id.navMenu);
        LinearLayout navKnowledge = findViewById(R.id.navKnowledge);

        AppCompatButton menuDashboard = findViewById(R.id.menuDashboard);
        AppCompatButton menuSettings = findViewById(R.id.menuSettings);
        AppCompatButton menuHelp = findViewById(R.id.menuHelp);
        AppCompatButton menuFeedback = findViewById(R.id.menuFeedback);
        AppCompatButton menuAbout = findViewById(R.id.menuAbout);
        AppCompatButton menuLogout = findViewById(R.id.menuLogout);

        ivHamburger.setOnClickListener(v -> openFullMenu());
        icNotification.setOnClickListener(v -> Toast.makeText(this, "Notifications empty", Toast.LENGTH_SHORT).show());
        icProfileTop.setOnClickListener(v -> openProfileScreen());
        menuProfileAvatar.setOnClickListener(v -> openProfileScreen());
        btnMyDesk.setOnClickListener(v -> openReportsInbox());

        cardReports.setOnClickListener(v -> openReportsInbox());
        cardConsultation.setOnClickListener(v -> startActivity(HumanConsultantPrototypeActivity.createIntent(this, RoleUtils.ROLE_CONSULTANT)));
        cardSessionHistory.setOnClickListener(v -> startActivity(ConsultantSessionHistoryActivity.createIntent(this, RoleUtils.ROLE_CONSULTANT)));
        cardProfile.setOnClickListener(v -> openProfileScreen());
        cardInfo.setOnClickListener(v -> Toast.makeText(this, "More consultant guidance is coming soon.", Toast.LENGTH_SHORT).show());

        navProfile.setOnClickListener(v -> openProfileScreen());
        navMenu.setOnClickListener(v -> openFullMenu());
        if (navKnowledge != null) {
            navKnowledge.setOnClickListener(v -> Toast.makeText(this, "Consultant guidance coming soon!", Toast.LENGTH_SHORT).show());
        }

        menuDashboard.setOnClickListener(v -> drawerLayout.closeDrawer(GravityCompat.START));
        menuSettings.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            Toast.makeText(this, "Settings coming soon!", Toast.LENGTH_SHORT).show();
        });
        menuHelp.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            Toast.makeText(this, "Help center coming soon!", Toast.LENGTH_SHORT).show();
        });
        menuFeedback.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            openFeedbackEmail();
        });
        menuAbout.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            showAboutDialog();
        });
        menuLogout.setOnClickListener(v -> signOutUser());

        loadUserSummary();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserSummary();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private void loadUserSummary() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        tvWelcomeConsultant.setText("Welcome, " + fallbackDisplayName(currentUser));
        tvMenuSubtitle.setText(DEFAULT_ROLE + " Account");

        firestore.collection(USERS_COLLECTION)
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String savedName = documentSnapshot.getString("name");
                    String savedRole = documentSnapshot.getString("role");
                    if (!TextUtils.isEmpty(savedName)) {
                        tvWelcomeConsultant.setText("Welcome, " + savedName);
                    }
                    if (!TextUtils.isEmpty(savedRole)) {
                        tvMenuSubtitle.setText(savedRole + " Account");
                    }
                });
    }

    private void openReportsInbox() {
        startActivity(new Intent(this, ConsultantReportInboxActivity.class));
    }

    private void openFullMenu() {
        Intent intent = new Intent(this, FullMenu.class);
        intent.putExtra("USER_ROLE", DEFAULT_ROLE);
        startActivity(intent);
    }

    private void openProfileScreen() {
        Intent intent = new Intent(this, Userprofile.class);
        intent.putExtra("USER_ROLE", DEFAULT_ROLE);
        startActivity(intent);
    }

    private void openFeedbackEmail() {
        Intent feedbackIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:support@mynyaman.app"));
        feedbackIntent.putExtra(Intent.EXTRA_SUBJECT, "MyNyaman Feedback");

        try {
            startActivity(feedbackIntent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No email app found on this device.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("About MyNyaman")
                .setMessage("MyNyaman helps families and consultants manage a healthier digital experience together.")
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void signOutUser() {
        mAuth.signOut();
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, Login.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String fallbackDisplayName(FirebaseUser user) {
        if (user == null || TextUtils.isEmpty(user.getEmail())) {
            return DEFAULT_ROLE;
        }
        String email = user.getEmail();
        return email != null && email.contains("@") ? email.substring(0, email.indexOf('@')) : DEFAULT_ROLE;
    }
}
