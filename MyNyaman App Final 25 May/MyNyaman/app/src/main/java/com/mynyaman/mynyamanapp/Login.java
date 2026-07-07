package com.mynyaman.mynyamanapp;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class Login extends BaseActivity {

    private static final String USERS_COLLECTION = "users";

    private EditText etEmail;
    private EditText etPassword;
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        PasswordVisibilityToggle.attach(etPassword);
        Button btnLogin = findViewById(R.id.btnSignIn);
        TextView tvForgotPassword = findViewById(R.id.tvForgotPassword);
        TextView tvSignUp = findViewById(R.id.tvSignUp);
        TextView tvConsultantSignUp = findViewById(R.id.tvConsultantSignUp);

        btnLogin.setOnClickListener(v -> loginUser());
        tvForgotPassword.setOnClickListener(v -> startActivity(new Intent(Login.this, Fpsendlink.class)));
        tvSignUp.setOnClickListener(v -> startActivity(buildSignupIntent(RoleUtils.ROLE_PARENT)));
        tvConsultantSignUp.setOnClickListener(v -> startActivity(buildSignupIntent(RoleUtils.ROLE_CONSULTANT)));
    }

    private Intent buildSignupIntent(String role) {
        Intent intent = new Intent(Login.this, Signup.class);
        intent.putExtra(Signup.EXTRA_SIGNUP_ROLE, role);
        return intent;
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Please enter your email");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Please enter your password");
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        String errorMessage = task.getException() != null
                                ? task.getException().getMessage()
                                : "Unknown error";
                        Toast.makeText(Login.this, "Authentication Failed: " + errorMessage, Toast.LENGTH_LONG).show();
                        return;
                    }

                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user == null) {
                        Toast.makeText(Login.this, "Authentication failed. Please try again.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    user.reload().addOnCompleteListener(reloadTask -> {
                        FirebaseUser refreshedUser = mAuth.getCurrentUser();
                        if (refreshedUser != null && refreshedUser.isEmailVerified()) {
                            BlueprintSyncRepository.syncSignedInUser(firestore, refreshedUser, "", refreshedUser.getDisplayName());
                            showSuccessDialog(refreshedUser.getUid());
                        } else {
                            Toast.makeText(Login.this, "Please verify your email before logging in.", Toast.LENGTH_LONG).show();
                            mAuth.signOut();
                        }
                    });
                });
    }

    private void showSuccessDialog(String userId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(Login.this);
        builder.setTitle("Success");
        builder.setMessage("Log in success");
        builder.setPositiveButton("OK", (dialog, which) -> {
            dialog.dismiss();
            routeUserByRole(userId);
        });
        builder.create().show();
    }

    private void routeUserByRole(String userId) {
        firestore.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    FirebaseUser currentUser = mAuth.getCurrentUser();
                    String normalizedRole = RoleUtils.readNormalizedRole(documentSnapshot);
                    String savedName = documentSnapshot.getString("name");
                    if (!TextUtils.isEmpty(documentSnapshot.getString("parentUid"))
                            || RoleUtils.ROLE_CHILD.equals(normalizedRole)) {
                        syncAndOpenDashboard(currentUser, RoleUtils.ROLE_CHILD, savedName);
                        return;
                    }
                    if (RoleUtils.ROLE_CONSULTANT.equals(normalizedRole)) {
                        syncAndOpenDashboard(currentUser, RoleUtils.ROLE_CONSULTANT, savedName);
                        return;
                    }

                    routeParentFallbackAfterChildLinkCheck(currentUser, savedName);
                })
                .addOnFailureListener(e -> Toast.makeText(
                        Login.this,
                        "Unable to load your role right now. Please try again.",
                        Toast.LENGTH_LONG
                ).show());
    }

    private void routeParentFallbackAfterChildLinkCheck(FirebaseUser currentUser, String savedName) {
        if (currentUser == null) {
            syncAndOpenDashboard(null, RoleUtils.ROLE_PARENT, savedName);
            return;
        }

        firestore.collection("parent_child_relationships")
                .whereEqualTo("childUid", currentUser.getUid())
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    String resolvedRole = querySnapshot != null && !querySnapshot.isEmpty()
                            ? RoleUtils.ROLE_CHILD
                            : RoleUtils.ROLE_PARENT;
                    syncAndOpenDashboard(currentUser, resolvedRole, savedName);
                })
                .addOnFailureListener(e -> syncAndOpenDashboard(currentUser, RoleUtils.ROLE_PARENT, savedName));
    }

    private void syncAndOpenDashboard(FirebaseUser currentUser, String normalizedRole, String savedName) {
        String safeRole = RoleUtils.normalizeRole(normalizedRole);
        if (TextUtils.isEmpty(safeRole)) {
            safeRole = RoleUtils.ROLE_PARENT;
        }
        if (currentUser != null) {
            BlueprintSyncRepository.syncSignedInUser(
                    firestore,
                    currentUser,
                    safeRole,
                    savedName
            );
        }

        Intent intent;
        if (RoleUtils.ROLE_CONSULTANT.equals(safeRole)) {
            intent = new Intent(Login.this, Consultantdash.class);
        } else if (RoleUtils.ROLE_CHILD.equals(safeRole)) {
            intent = new Intent(Login.this, Childdash.class);
        } else {
            intent = new Intent(Login.this, Parentdash.class);
        }

        startActivity(intent);
        finish();
    }
}
