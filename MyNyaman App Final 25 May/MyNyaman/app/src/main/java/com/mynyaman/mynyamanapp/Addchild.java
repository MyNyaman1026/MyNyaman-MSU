package com.mynyaman.mynyamanapp;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatButton;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Addchild extends BaseActivity {

    private static final int MAX_CHILDREN = 8;

    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private EditText etChildName;
    private EditText etChildContact;
    private EditText etChildEmail;
    private EditText etChildDOB;
    private EditText etChildPassword;
    private EditText etChildConfirmPassword;
    private AppCompatButton btnSaveChild;
    private final Calendar selectedDob = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_child);

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        etChildName = findViewById(R.id.etChildName);
        etChildContact = findViewById(R.id.etChildContact);
        etChildEmail = findViewById(R.id.etChildEmail);
        etChildDOB = findViewById(R.id.etChildDOB);
        etChildPassword = findViewById(R.id.etChildPassword);
        etChildConfirmPassword = findViewById(R.id.etChildConfirmPassword);
        PasswordVisibilityToggle.attach(etChildPassword);
        PasswordVisibilityToggle.attach(etChildConfirmPassword);
        btnSaveChild = findViewById(R.id.btnSaveChild);
        ImageView btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());
        etChildDOB.setOnClickListener(v -> showDatePicker());
        etChildDOB.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                showDatePicker();
            }
        });
        btnSaveChild.setOnClickListener(v -> registerChild());
    }

    private void showDatePicker() {
        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedDob.set(Calendar.YEAR, year);
                    selectedDob.set(Calendar.MONTH, month);
                    selectedDob.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    etChildDOB.setText(formatter.format(selectedDob.getTime()));
                },
                selectedDob.get(Calendar.YEAR),
                selectedDob.get(Calendar.MONTH),
                selectedDob.get(Calendar.DAY_OF_MONTH)
        );
        dialog.show();
    }

    private void registerChild() {
        FirebaseUser parentUser = mAuth.getCurrentUser();
        if (parentUser == null) {
            startActivity(new Intent(this, Login.class));
            finish();
            return;
        }

        String childName = etChildName.getText().toString().trim();
        String childContact = etChildContact.getText().toString().trim();
        String childEmail = etChildEmail.getText().toString().trim();
        String childDob = etChildDOB.getText().toString().trim();
        String childPassword = etChildPassword.getText().toString().trim();
        String childConfirmPassword = etChildConfirmPassword.getText().toString().trim();
        String parentEmail = parentUser.getEmail() != null ? parentUser.getEmail().trim() : "";

        if (TextUtils.isEmpty(childName)) {
            etChildName.setError("Child's name cannot be empty");
            return;
        }
        if (TextUtils.isEmpty(childContact)) {
            etChildContact.setError("Contact cannot be empty");
            return;
        }
        if (TextUtils.isEmpty(childEmail)) {
            etChildEmail.setError("Child email cannot be empty");
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(childEmail).matches()) {
            etChildEmail.setError("Please enter a valid child email");
            return;
        }
        if (TextUtils.isEmpty(childDob)) {
            etChildDOB.setError("Date of birth cannot be empty");
            return;
        }
        if (TextUtils.isEmpty(childPassword)) {
            etChildPassword.setError("Child password cannot be empty");
            return;
        }
        if (childPassword.length() < 6) {
            etChildPassword.setError("Password must be at least 6 characters");
            return;
        }
        if (!childPassword.equals(childConfirmPassword)) {
            etChildConfirmPassword.setError("Passwords do not match");
            return;
        }
        if (!TextUtils.isEmpty(parentEmail) && childEmail.equalsIgnoreCase(parentEmail)) {
            etChildEmail.setError("Child email must be different from the parent email");
            return;
        }

        btnSaveChild.setEnabled(false);
        firestore.collection("users")
                .document(parentUser.getUid())
                .collection("children")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.size() >= MAX_CHILDREN) {
                        btnSaveChild.setEnabled(true);
                        Toast.makeText(this, "You can only link up to 8 child accounts.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    createChildAccount(parentUser, childName, childContact, childEmail, childDob, childPassword);
                })
                .addOnFailureListener(e -> {
                    btnSaveChild.setEnabled(true);
                    Toast.makeText(this, "Unable to check your family size right now.", Toast.LENGTH_LONG).show();
                });
    }

    private void createChildAccount(
            FirebaseUser parentUser,
            String childName,
            String childContact,
            String childEmail,
            String childDob,
            String childPassword
    ) {
        String appName = "childRegistration_" + System.currentTimeMillis();
        FirebaseOptions options = FirebaseApp.getInstance().getOptions();
        FirebaseApp secondaryApp = FirebaseApp.initializeApp(this, options, appName);
        FirebaseAuth childAuth = FirebaseAuth.getInstance(secondaryApp);

        childAuth.createUserWithEmailAndPassword(childEmail, childPassword)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        btnSaveChild.setEnabled(true);
                        cleanupSecondaryAuth(childAuth, secondaryApp);
                        Toast.makeText(this, childSetupErrorMessage(task.getException()), Toast.LENGTH_LONG).show();
                        return;
                    }

                    FirebaseUser childUser = childAuth.getCurrentUser();
                    if (childUser == null) {
                        btnSaveChild.setEnabled(true);
                        cleanupSecondaryAuth(childAuth, secondaryApp);
                        Toast.makeText(this, "Child account could not be created. Please try again.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    writeChildData(parentUser, childUser, childName, childContact, childEmail, childDob, childAuth, secondaryApp);
                });
    }

    private void writeChildData(
            FirebaseUser parentUser,
            FirebaseUser childUser,
            String childName,
            String childContact,
            String childEmail,
            String childDob,
            FirebaseAuth childAuth,
            FirebaseApp secondaryApp
    ) {
        Map<String, Object> childProfile = new HashMap<>();
        childProfile.put("uid", childUser.getUid());
        childProfile.put("childUid", childUser.getUid());
        childProfile.put("role", RoleUtils.ROLE_CHILD);
        childProfile.put("roleKey", RoleUtils.ROLE_CHILD);
        childProfile.put("roleLabel", RoleUtils.toLegacyRoleLabel(RoleUtils.ROLE_CHILD));
        childProfile.put("openId", childUser.getUid());
        childProfile.put("firebaseUid", childUser.getUid());
        childProfile.put("loginMethod", "email");
        childProfile.put("emailVerified", false);
        childProfile.put("name", childName);
        childProfile.put("contact", childContact);
        childProfile.put("email", childEmail);
        childProfile.put("dob", childDob);
        childProfile.put("parentUid", parentUser.getUid());
        childProfile.put("parentEmail", parentUser.getEmail());
        childProfile.put("accountStatus", "PENDING_SETUP");
        childProfile.put("passwordSetByParent", true);
        childProfile.put("usageTodayMinutes", 0L);
        childProfile.put("usageTodaySeconds", 0L);
        childProfile.put("usagePermissionGranted", false);
        childProfile.put("createdAt", FieldValue.serverTimestamp());
        childProfile.put("updatedAt", FieldValue.serverTimestamp());

        Map<String, Object> childLink = new HashMap<>();
        childLink.put("uid", childUser.getUid());
        childLink.put("childUid", childUser.getUid());
        childLink.put("parentUid", parentUser.getUid());
        childLink.put("name", childName);
        childLink.put("childName", childName);
        childLink.put("contact", childContact);
        childLink.put("email", childEmail);
        childLink.put("childEmail", childEmail);
        childLink.put("dob", childDob);
        childLink.put("role", RoleUtils.ROLE_CHILD);
        childLink.put("roleKey", RoleUtils.ROLE_CHILD);
        childLink.put("roleLabel", RoleUtils.toLegacyRoleLabel(RoleUtils.ROLE_CHILD));
        childLink.put("accountStatus", "PENDING_SETUP");
        childLink.put("passwordSetByParent", true);
        childLink.put("usageTodayMinutes", 0L);
        childLink.put("usageTodaySeconds", 0L);
        childLink.put("usagePermissionGranted", false);
        childLink.put("createdAt", FieldValue.serverTimestamp());
        childLink.put("updatedAt", FieldValue.serverTimestamp());

        Map<String, Object> relationship = new HashMap<>();
        relationship.put("parentUid", parentUser.getUid());
        relationship.put("childUid", childUser.getUid());
        relationship.put("parentId", parentUser.getUid());
        relationship.put("childId", childUser.getUid());
        relationship.put("nickname", childName);
        relationship.put("childName", childName);
        relationship.put("parentEmail", parentUser.getEmail());
        relationship.put("childEmail", childEmail);
        relationship.put("createdAt", FieldValue.serverTimestamp());

        WriteBatch batch = firestore.batch();
        batch.set(firestore.collection("users").document(childUser.getUid()), childProfile, SetOptions.merge());
        batch.set(
                firestore.collection("users")
                        .document(parentUser.getUid())
                        .collection("children")
                        .document(childUser.getUid()),
                childLink,
                SetOptions.merge()
        );
        batch.set(
                firestore.collection("parent_child_relationships")
                        .document(parentUser.getUid() + "_" + childUser.getUid()),
                relationship,
                SetOptions.merge()
        );

        batch.commit()
                .addOnSuccessListener(unused -> sendChildVerificationEmail(parentUser, childUser, childName, childEmail, childAuth, secondaryApp))
                .addOnFailureListener(e -> rollbackFailedChildCreation(childUser, childAuth, secondaryApp, "Failed to link the child account. Please try again."));
    }

    private void sendChildVerificationEmail(
            FirebaseUser parentUser,
            FirebaseUser childUser,
            String childName,
            String childEmail,
            FirebaseAuth childAuth,
            FirebaseApp secondaryApp
    ) {
        childUser.sendEmailVerification().addOnCompleteListener(verificationTask -> {
            if (!verificationTask.isSuccessful()) {
                rollbackFirestoreAndChildAuth(parentUser.getUid(), childUser.getUid(), childUser, childAuth, secondaryApp,
                        "Failed to send the child verification email. Please try again.");
                return;
            }

            cleanupSecondaryAuth(childAuth, secondaryApp);
            btnSaveChild.setEnabled(true);

            Intent resultIntent = new Intent();
            resultIntent.putExtra("CHILD_NAME", childName);
            resultIntent.putExtra("CHILD_UID", childUser.getUid());
            resultIntent.putExtra("CHILD_EMAIL", childEmail);
            setResult(RESULT_OK, resultIntent);

            Toast.makeText(
                    this,
                    "Child account created. Share the temporary password with the child. They can change it later with Forgot Password.",
                    Toast.LENGTH_LONG
            ).show();
            finish();
        });
    }

    private void rollbackFirestoreAndChildAuth(
            String parentUid,
            String childUid,
            FirebaseUser childUser,
            FirebaseAuth childAuth,
            FirebaseApp secondaryApp,
            String message
    ) {
        firestore.collection("users").document(childUid).delete();
        firestore.collection("users")
                .document(parentUid)
                .collection("children")
                .document(childUid)
                .delete();
        BlueprintSyncRepository.removeParentChildRelationship(firestore, parentUid, childUid);

        rollbackFailedChildCreation(childUser, childAuth, secondaryApp, message);
    }

    private void rollbackFailedChildCreation(
            FirebaseUser childUser,
            FirebaseAuth childAuth,
            FirebaseApp secondaryApp,
            String message
    ) {
        childUser.delete().addOnCompleteListener(deleteTask -> {
            cleanupSecondaryAuth(childAuth, secondaryApp);
            btnSaveChild.setEnabled(true);
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        });
    }

    private void cleanupSecondaryAuth(FirebaseAuth childAuth, FirebaseApp secondaryApp) {
        childAuth.signOut();
        try {
            secondaryApp.delete();
        } catch (IllegalStateException ignored) {
        }
    }

    private String childSetupErrorMessage(Exception exception) {
        String message = exception == null ? "" : exception.getMessage();
        if (exception instanceof FirebaseAuthUserCollisionException
                || (!TextUtils.isEmpty(message) && message.toLowerCase(Locale.US).contains("already exists"))) {
            return "A child account with this email already exists.";
        }
        if (!TextUtils.isEmpty(message)) {
            return message;
        }
        return "Unable to register child account right now.";
    }
}
