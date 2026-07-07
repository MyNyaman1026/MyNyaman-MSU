package com.mynyaman.mynyamanapp;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatButton;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class FullMenu extends BaseActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private String userRole = "User";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.full_menu);

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() == null) {
            finish();
            return;
        }

        String roleHint = getIntent().getStringExtra("USER_ROLE");
        if (!TextUtils.isEmpty(roleHint)) {
            userRole = roleHint;
        }
        fetchUserRole();

        LinearLayout btnBack = findViewById(R.id.btnBack);
        LinearLayout menuProfile = findViewById(R.id.menuProfile);
        LinearLayout menuSettings = findViewById(R.id.menuSettings);
        LinearLayout menuHelp = findViewById(R.id.menuHelp);
        LinearLayout menuFeedback = findViewById(R.id.menuFeedback);
        LinearLayout menuAbout = findViewById(R.id.menuAbout);
        AppCompatButton menuLogout = findViewById(R.id.menuLogout);

        // Closes the menu and returns to whichever dashboard opened it
        btnBack.setOnClickListener(v -> finish());

        menuProfile.setOnClickListener(v -> {
            Intent intent;
            if ("Child".equalsIgnoreCase(userRole)) {
                intent = new Intent(this, ProfileeditChild.class);
            } else if ("Consultant".equalsIgnoreCase(userRole)) {
                intent = new Intent(this, Userprofile.class);
                intent.putExtra("USER_ROLE", "Consultant");
            } else {
                // Default to Parent Dashboard
                intent = new Intent(this, Parentprofile.class);
            }
            startActivity(intent);
        });

        menuSettings.setOnClickListener(v -> Toast.makeText(this, "Settings coming soon!", Toast.LENGTH_SHORT).show());

        menuHelp.setOnClickListener(v -> Toast.makeText(this, "Help center coming soon!", Toast.LENGTH_SHORT).show());

        menuFeedback.setOnClickListener(v -> {
            Intent feedbackIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:support@mynyaman.app"));
            feedbackIntent.putExtra(Intent.EXTRA_SUBJECT, "MyNyaman App Feedback");
            try {
                startActivity(feedbackIntent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, "No email app found.", Toast.LENGTH_SHORT).show();
            }
        });

        menuAbout.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("About MyNyaman")
                    .setMessage("Version 1.0\nDeveloped for Final Year Project.")
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                    .show();
        });

        menuLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();

            // Clear backstack and go to Login
            Intent intent = new Intent(FullMenu.this, Login.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void fetchUserRole() {
        String uid = mAuth.getCurrentUser().getUid();
        firestore.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String savedRole = documentSnapshot.getString("role");
                        if (!TextUtils.isEmpty(savedRole)) {
                            userRole = savedRole;
                        }
                    }
                });
    }
}
