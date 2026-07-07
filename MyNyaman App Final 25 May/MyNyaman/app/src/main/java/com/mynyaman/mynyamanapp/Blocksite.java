package com.mynyaman.mynyamanapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.AppCompatButton;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Blocksite extends BaseActivity {

    private static final String USERS_COLLECTION = "users";
    private static final String BLOCKED_SITES_FIELD = "blockedSites";

    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private EditText etWebsite;
    private LinearLayout blockedSitesContainer;
    private final List<String> blockedSites = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.blocksite);

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        ImageView btnBack = findViewById(R.id.btnBack);
        etWebsite = findViewById(R.id.etWebsite);
        blockedSitesContainer = findViewById(R.id.blockedSitesContainer);
        AppCompatButton btnAddSite = findViewById(R.id.btnAddSite);

        btnBack.setOnClickListener(v -> finish());
        btnAddSite.setOnClickListener(v -> addBlockedSite());

        loadBlockedSites();
    }

    private void loadBlockedSites() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(Blocksite.this, Login.class));
            finish();
            return;
        }

        firestore.collection(USERS_COLLECTION)
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    blockedSites.clear();
                    List<String> savedSites = (List<String>) documentSnapshot.get(BLOCKED_SITES_FIELD);
                    if (savedSites != null) {
                        blockedSites.addAll(savedSites);
                    }
                    renderBlockedSites();
                });
    }

    private void addBlockedSite() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(Blocksite.this, Login.class));
            finish();
            return;
        }

        String website = etWebsite.getText().toString().trim();
        if (TextUtils.isEmpty(website)) {
            etWebsite.setError("Enter a website or domain");
            return;
        }

        if (!blockedSites.contains(website)) {
            blockedSites.add(website);
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put(BLOCKED_SITES_FIELD, blockedSites);

        firestore.collection(USERS_COLLECTION)
                .document(currentUser.getUid())
                .set(updates, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    etWebsite.setText("");
                    renderBlockedSites();
                    Toast.makeText(this, "Blocked sites updated.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to save blocked site.", Toast.LENGTH_LONG).show());
    }

    private void renderBlockedSites() {
        blockedSitesContainer.removeAllViews();

        if (blockedSites.isEmpty()) {
            TextView emptyView = new TextView(this);
            emptyView.setText("No blocked sites saved yet.");
            emptyView.setTextColor(0xFF666666);
            emptyView.setTextSize(15f);
            blockedSitesContainer.addView(emptyView);
            return;
        }

        for (String site : blockedSites) {
            TextView chip = new TextView(this);
            chip.setText(site);
            chip.setBackgroundResource(R.drawable.rounded_input_gray);
            chip.setTextColor(0xFF000000);
            chip.setTextSize(15f);
            chip.setPadding(28, 18, 28, 18);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.bottomMargin = 16;
            chip.setLayoutParams(params);
            blockedSitesContainer.addView(chip);
        }
    }
}
