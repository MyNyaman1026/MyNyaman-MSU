package com.mynyaman.mynyamanapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.AppCompatButton;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class Userprofile extends BaseActivity {

    private static final String USERS_COLLECTION = "users";
    private static final String NAME_FIELD = "name";
    private static final String CONTACT_FIELD = "contact";
    private static final String ROLE_FIELD = "role";

    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private EditText etName;
    private EditText etContact;
    private EditText etEmail;
    private TextView tvProfileTitle;
    private String currentRole = "User";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_profile);

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(Userprofile.this, Login.class));
            finish();
            return;
        }

        ImageView btnBack = findViewById(R.id.btnBack);
        etName = findViewById(R.id.etProfileName);
        etContact = findViewById(R.id.etProfileContact);
        etEmail = findViewById(R.id.etProfileEmail);
        tvProfileTitle = findViewById(R.id.tvProfileTitle);
        AppCompatButton btnSaveProfile = findViewById(R.id.btnSaveProfile);

        btnBack.setOnClickListener(v -> finish());
        btnSaveProfile.setOnClickListener(v -> saveProfile());

        loadProfile();
    }

    private void loadProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        String email = currentUser.getEmail() != null ? currentUser.getEmail() : "";
        String fallbackName = getFallbackName(email);
        currentRole = getIntent().getStringExtra("USER_ROLE");
        if (TextUtils.isEmpty(currentRole)) {
            currentRole = "User";
        }

        etEmail.setText(email);
        etName.setText(fallbackName);
        tvProfileTitle.setText(currentRole + " Profile");

        firestore.collection(USERS_COLLECTION)
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String savedName = documentSnapshot.getString(NAME_FIELD);
                    String savedContact = documentSnapshot.getString(CONTACT_FIELD);
                    String savedRole = documentSnapshot.getString(ROLE_FIELD);

                    if (!TextUtils.isEmpty(savedName)) {
                        etName.setText(savedName);
                    }

                    if (savedContact != null) {
                        etContact.setText(savedContact);
                    }

                    if (!TextUtils.isEmpty(savedRole)) {
                        currentRole = savedRole;
                        tvProfileTitle.setText(savedRole + " Profile");
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(
                        Userprofile.this,
                        "Unable to load your profile right now.",
                        Toast.LENGTH_SHORT
                ).show());
    }

    private void saveProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(Userprofile.this, Login.class));
            finish();
            return;
        }

        String name = etName.getText().toString().trim();
        String contact = etContact.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            etName.setError("Please enter your name");
            return;
        }

        Map<String, Object> profileData = new HashMap<>();
        profileData.put(NAME_FIELD, name);
        profileData.put(CONTACT_FIELD, contact);

        firestore.collection(USERS_COLLECTION)
                .document(currentUser.getUid())
                .set(profileData, SetOptions.merge())
                .addOnSuccessListener(unused -> Toast.makeText(
                        Userprofile.this,
                        "Profile updated successfully.",
                        Toast.LENGTH_SHORT
                ).show())
                .addOnFailureListener(e -> Toast.makeText(
                        Userprofile.this,
                        "Failed to update profile. Please try again.",
                        Toast.LENGTH_LONG
                ).show());
    }

    private String getFallbackName(String email) {
        if (!TextUtils.isEmpty(email) && email.contains("@")) {
            return email.split("@")[0];
        }
        return "MyNyaman User";
    }
}
