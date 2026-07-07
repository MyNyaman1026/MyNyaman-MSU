package com.mynyaman.mynyamanapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class Splash extends BaseActivity {

    private static final int SPLASH_TIME = 2000;
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash);

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        new Handler(Looper.getMainLooper()).postDelayed(this::checkUserSession, SPLASH_TIME);
    }

    private void checkUserSession() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            goToStart();
            return;
        }

        currentUser.reload().addOnCompleteListener(task -> {
            FirebaseUser refreshedUser = mAuth.getCurrentUser();
            if (refreshedUser == null || !refreshedUser.isEmailVerified()) {
                mAuth.signOut();
                goToStart();
                return;
            }

            firestore.collection("users")
                    .document(refreshedUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (!documentSnapshot.exists()) {
                            routeParentFallbackAfterChildLinkCheck(refreshedUser, refreshedUser.getDisplayName());
                            return;
                        }

                        routeUserFromSnapshot(refreshedUser, documentSnapshot);
                    })
                    .addOnFailureListener(e -> {
                        mAuth.signOut();
                        goToStart();
                    });
        });
    }

    private void goToStart() {
        startActivity(new Intent(Splash.this, Start.class));
        finish();
    }

    private void routeUserFromSnapshot(FirebaseUser currentUser, DocumentSnapshot documentSnapshot) {
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
            intent = new Intent(Splash.this, Consultantdash.class);
        } else if (RoleUtils.ROLE_CHILD.equals(safeRole)) {
            intent = new Intent(Splash.this, Childdash.class);
        } else {
            intent = new Intent(Splash.this, Parentdash.class);
        }

        startActivity(intent);
        finish();
    }
}
