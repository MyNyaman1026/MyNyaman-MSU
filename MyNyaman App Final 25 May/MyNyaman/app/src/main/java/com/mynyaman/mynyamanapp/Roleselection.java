package com.mynyaman.mynyamanapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class Roleselection extends BaseActivity {

    private static final String USERS_COLLECTION = "users";

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirebaseFirestore firestore;
    private CardView cardParent;
    private CardView cardConsultant;
    private boolean isSavingRole = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role_selection);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        firestore = FirebaseFirestore.getInstance();

        if (currentUser == null) {
            startActivity(new Intent(Roleselection.this, Login.class));
            finish();
            return;
        }

        TextView tvPermanentChoice = findViewById(R.id.tvPermanentChoice);
        if (currentUser.getEmail() != null) {
            tvPermanentChoice.setText("This is a one-time permanent choice for " + currentUser.getEmail() + ".");
        }

        cardParent = findViewById(R.id.cardParent);
        cardConsultant = findViewById(R.id.cardConsultant);

        cardParent.setOnClickListener(v -> {
            if (!isSavingRole) {
                showConfirmationDialog(RoleUtils.ROLE_PARENT);
            }
        });

        cardConsultant.setOnClickListener(v -> {
            if (!isSavingRole) {
                showConfirmationDialog(RoleUtils.ROLE_CONSULTANT);
            }
        });

        checkExistingRole();
    }

    private void showConfirmationDialog(final String normalizedRole) {
        String displayRole = RoleUtils.toLegacyRoleLabel(normalizedRole);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirm Role");
        builder.setMessage("Are you sure you want to select '" + displayRole + "'? This role cannot be changed later.");
        builder.setPositiveButton("Yes, Confirm", (dialog, which) -> {
            dialog.dismiss();
            saveRoleSelection(normalizedRole);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.setCancelable(false);
        builder.create().show();
    }

    private void checkExistingRole() {
        firestore.collection(USERS_COLLECTION)
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String savedRole = RoleUtils.readNormalizedRole(documentSnapshot);
                    if (RoleUtils.ROLE_PARENT.equals(savedRole) || RoleUtils.ROLE_CONSULTANT.equals(savedRole)) {
                        navigateAfterRoleSelection(savedRole);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(
                        Roleselection.this,
                        "Unable to check saved role right now.",
                        Toast.LENGTH_SHORT
                ).show());
    }

    private void saveRoleSelection(final String normalizedRole) {
        isSavingRole = true;
        setRoleCardsEnabled(false);

        Map<String, Object> roleData = new HashMap<>();
        roleData.put("role", normalizedRole);
        roleData.put("roleKey", normalizedRole);
        roleData.put("roleLabel", RoleUtils.toLegacyRoleLabel(normalizedRole));
        roleData.put("email", currentUser.getEmail());
        roleData.put("name", !TextUtils.isEmpty(currentUser.getDisplayName()) ? currentUser.getDisplayName() : getIntent().getStringExtra("PENDING_NAME"));
        roleData.put("openId", currentUser.getUid());
        roleData.put("firebaseUid", currentUser.getUid());
        roleData.put("loginMethod", "email");
        roleData.put("emailVerified", currentUser.isEmailVerified());
        roleData.put("updatedAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

        firestore.collection(USERS_COLLECTION)
                .document(currentUser.getUid())
                .set(roleData, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    BlueprintSyncRepository.syncSignedInUser(
                            firestore,
                            currentUser,
                            normalizedRole,
                            (String) roleData.get("name")
                    );
                    showSuccessDialog(normalizedRole);
                })
                .addOnFailureListener(e -> {
                    isSavingRole = false;
                    setRoleCardsEnabled(true);
                    Toast.makeText(
                            Roleselection.this,
                            "Failed to save your role. Please try again.",
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void showSuccessDialog(final String normalizedRole) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Success!");
        String displayRole = RoleUtils.toLegacyRoleLabel(normalizedRole);

        if (currentUser != null && currentUser.isEmailVerified()) {
            builder.setMessage("Your permanent role has been set to: " + displayRole);
            builder.setPositiveButton("Go to Dashboard", (dialog, which) -> {
                dialog.dismiss();
                navigateAfterRoleSelection(normalizedRole);
            });
        } else {
            builder.setMessage("Role saved: " + displayRole + ".\n\nPlease check your email to verify your account before logging in.");
            builder.setPositiveButton("Go to Login", (dialog, which) -> {
                dialog.dismiss();
                navigateAfterRoleSelection(normalizedRole);
            });
        }

        builder.setCancelable(false);
        builder.create().show();
    }

    private void navigateAfterRoleSelection(String normalizedRole) {
        if (currentUser != null && currentUser.isEmailVerified()) {
            Intent intent;
            if (RoleUtils.ROLE_PARENT.equals(RoleUtils.normalizeRole(normalizedRole))) {
                intent = new Intent(Roleselection.this, Parentdash.class);
            } else {
                intent = new Intent(Roleselection.this, Consultantdash.class);
            }
            intent.putExtra("USER_ROLE", RoleUtils.normalizeRole(normalizedRole));
            startActivity(intent);
            finish();
        } else {
            mAuth.signOut();
            Intent intent = new Intent(Roleselection.this, Login.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }

    private void setRoleCardsEnabled(boolean enabled) {
        cardParent.setEnabled(enabled);
        cardParent.setClickable(enabled);
        cardConsultant.setEnabled(enabled);
        cardConsultant.setClickable(enabled);
    }
}
