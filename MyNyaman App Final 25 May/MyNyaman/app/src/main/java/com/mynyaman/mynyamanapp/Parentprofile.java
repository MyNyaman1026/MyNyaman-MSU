package com.mynyaman.mynyamanapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.AppCompatButton;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class Parentprofile extends BaseActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.parent_profile);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        // Security check
        if (currentUser == null) {
            startActivity(new Intent(Parentprofile.this, Login.class));
            finish();
            return;
        }

        // --- 1. Link Header UI ---
        TextView tvWelcomeParent = findViewById(R.id.tvWelcomeParent);
        AppCompatButton btnMyFamily = findViewById(R.id.btnMyFamily);
        ImageView icNotification = findViewById(R.id.icNotification);
        ImageView ivHamburger = findViewById(R.id.ivHamburger);
        androidx.drawerlayout.widget.DrawerLayout drawerLayout = findViewById(R.id.drawerLayout);

        if (ivHamburger != null && drawerLayout != null) {
            ivHamburger.setOnClickListener(v -> drawerLayout.openDrawer(androidx.core.view.GravityCompat.START));
        }

        AppCompatButton menuDashboard = findViewById(R.id.menuDashboard);
        if (menuDashboard != null) {
            menuDashboard.setOnClickListener(v -> {
                finish();
            });
        }
        
        AppCompatButton menuLogout = findViewById(R.id.menuLogout);
        if (menuLogout != null) {
            menuLogout.setOnClickListener(v -> {
                mAuth.signOut();
                Intent intent = new Intent(this, Login.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }

        // --- 2. Link Profile Card UI ---
        TextView tvProfileName = findViewById(R.id.tvProfileName);
        TextView tvProfileEmail = findViewById(R.id.tvProfileEmail);

        if (currentUser.getEmail() != null) {
            String email = currentUser.getEmail();
            String namePrefix = email.split("@")[0];
            tvWelcomeParent.setText("Welcome, " + namePrefix);
            tvProfileName.setText(namePrefix);
            tvProfileEmail.setText(email);
        }

        // --- 3. Link Menu Buttons ---
        TextView btnEditProfile = findViewById(R.id.btnEditProfile);
        TextView btnSettingsPrivacy = findViewById(R.id.btnSettingsPrivacy);
        TextView btnNotificationsMenu = findViewById(R.id.btnNotificationsMenu);
        TextView btnHelpFeedback = findViewById(R.id.btnHelpFeedback);

        // --- 4. Link Bottom Navigation ---
        LinearLayout navHome = findViewById(R.id.navHome);
        LinearLayout navKnowledge = findViewById(R.id.navKnowledge);
        LinearLayout navProfile = findViewById(R.id.navProfile);
        LinearLayout navMenu = findViewById(R.id.navMenu);

        // ==========================================
        // CLICK LISTENERS
        // ==========================================

        icNotification.setOnClickListener(v -> Toast.makeText(this, "Notifications clicked", Toast.LENGTH_SHORT).show());
        btnMyFamily.setOnClickListener(v -> Toast.makeText(this, "My Family clicked", Toast.LENGTH_SHORT).show());

        btnEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(this, Userprofile.class);
            intent.putExtra("USER_ROLE", "Parent");
            startActivity(intent);
        });
        btnSettingsPrivacy.setOnClickListener(v -> Toast.makeText(this, "Settings & Privacy coming soon!", Toast.LENGTH_SHORT).show());
        btnNotificationsMenu.setOnClickListener(v -> Toast.makeText(this, "Notifications Menu coming soon!", Toast.LENGTH_SHORT).show());
        btnHelpFeedback.setOnClickListener(v -> Toast.makeText(this, "Help & Feedback coming soon!", Toast.LENGTH_SHORT).show());

        navHome.setOnClickListener(v -> finish());
        navKnowledge.setOnClickListener(v -> {
            startActivity(new Intent(this, Tutorial.class));
            finish();
        });
        navProfile.setOnClickListener(v -> Toast.makeText(this, "You are currently on the Profile screen", Toast.LENGTH_SHORT).show());
        navMenu.setOnClickListener(v -> {
            startActivity(new Intent(this, FullMenu.class));
            finish();
        });
    }
}