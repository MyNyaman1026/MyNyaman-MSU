package com.mynyaman.mynyamanapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class Signup extends BaseActivity {

    public static final String EXTRA_SIGNUP_ROLE = "SIGNUP_ROLE";

    private static final String USERS_COLLECTION = "users";

    private EditText etName;
    private EditText etDateOfBirth;
    private EditText etEmail;
    private EditText etWorkEmail;
    private EditText etPersonalPhoneNumber;
    private EditText etWorkPhoneNumber;
    private EditText etAgencyWebsite;
    private EditText etMedicalLicense;
    private EditText etPersonalIdentification;
    private EditText etAgencyName;
    private EditText etPassword;
    private EditText etConfirmPassword;
    private LinearLayout consultantFieldsLayout;
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private String signupRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signup);

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        signupRole = RoleUtils.normalizeRole(getIntent().getStringExtra(EXTRA_SIGNUP_ROLE));
        if (!RoleUtils.ROLE_CONSULTANT.equals(signupRole)) {
            signupRole = RoleUtils.ROLE_PARENT;
        }

        etName = findViewById(R.id.etName);
        etDateOfBirth = findViewById(R.id.dateOfBirth);
        etEmail = findViewById(R.id.etSignUpEmail);
        etWorkEmail = findViewById(R.id.etWorkEmail);
        etPersonalPhoneNumber = findViewById(R.id.etPersonalPhoneNumber);
        etWorkPhoneNumber = findViewById(R.id.etWorkPhoneNumber);
        etAgencyWebsite = findViewById(R.id.etAgencyWebsite);
        etMedicalLicense = findViewById(R.id.etMedicalLicense);
        etPersonalIdentification = findViewById(R.id.etPersonalIdentification);
        etAgencyName = findViewById(R.id.etAgencyName);
        etPassword = findViewById(R.id.etSignUpPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        PasswordVisibilityToggle.attach(etPassword);
        PasswordVisibilityToggle.attach(etConfirmPassword);
        consultantFieldsLayout = findViewById(R.id.consultantFieldsLayout);

        View btnSubmitSignUp = findViewById(R.id.btnSubmitSignUp);
        TextView tvGoToLogin = findViewById(R.id.tvGoToLogin);

        configureSignupForm();

        btnSubmitSignUp.setOnClickListener(v -> registerUser());
        tvGoToLogin.setOnClickListener(v -> finish());
    }

    private void configureSignupForm() {
        boolean isConsultantSignup = RoleUtils.ROLE_CONSULTANT.equals(signupRole);
        etDateOfBirth.setVisibility(isConsultantSignup ? View.VISIBLE : View.GONE);
        consultantFieldsLayout.setVisibility(isConsultantSignup ? View.VISIBLE : View.GONE);

        TextView tvSignUpTitle = findViewById(R.id.tvSignUpTitle);
        TextView tvSignUpSubtitle = findViewById(R.id.tvSignUpSubtitle);

        if (isConsultantSignup) {
            tvSignUpTitle.setText("Consultant Sign Up");
            tvSignUpSubtitle.setText("Register as a professional consultant.");
            etEmail.setHint("Personal Email");
        } else {
            tvSignUpTitle.setText("Parent Sign Up");
            tvSignUpSubtitle.setText("Create an account to manage your family.");
            etEmail.setHint("Email Address");
        }
    }

    private void registerUser() {
        String name = etName.getText().toString().trim();
        String dateOfBirth = etDateOfBirth.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String workEmail = etWorkEmail.getText().toString().trim();
        String personalPhoneNumber = etPersonalPhoneNumber.getText().toString().trim();
        String workPhoneNumber = etWorkPhoneNumber.getText().toString().trim();
        String agencyWebsite = etAgencyWebsite.getText().toString().trim();
        String medicalLicense = etMedicalLicense.getText().toString().trim();
        String personalIdentification = etPersonalIdentification.getText().toString().trim();
        String agencyName = etAgencyName.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();
        boolean isConsultantSignup = RoleUtils.ROLE_CONSULTANT.equals(signupRole);

        if (TextUtils.isEmpty(name)) {
            etName.setError("Name cannot be empty");
            return;
        }
        if (isConsultantSignup && TextUtils.isEmpty(dateOfBirth)) {
            etDateOfBirth.setError("Date of birth cannot be empty");
            return;
        }
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email cannot be empty");
            return;
        }
        if (isConsultantSignup && TextUtils.isEmpty(workEmail)) {
            etWorkEmail.setError("Work email cannot be empty");
            return;
        }
        if (isConsultantSignup && TextUtils.isEmpty(personalPhoneNumber)) {
            etPersonalPhoneNumber.setError("Personal phone number cannot be empty");
            return;
        }
        if (isConsultantSignup && TextUtils.isEmpty(workPhoneNumber)) {
            etWorkPhoneNumber.setError("Work phone number cannot be empty");
            return;
        }
        if (isConsultantSignup && TextUtils.isEmpty(agencyWebsite)) {
            etAgencyWebsite.setError("Website link cannot be empty");
            return;
        }
        if (isConsultantSignup && TextUtils.isEmpty(medicalLicense)) {
            etMedicalLicense.setError("Medical license cannot be empty");
            return;
        }
        if (isConsultantSignup && TextUtils.isEmpty(personalIdentification)) {
            etPersonalIdentification.setError("Personal identification cannot be empty");
            return;
        }
        if (isConsultantSignup && TextUtils.isEmpty(agencyName)) {
            etAgencyName.setError("Agency name cannot be empty");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password cannot be empty");
            return;
        }
        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match!");
            return;
        }

        String passwordPattern = "^(?=.*[0-9])(?=.*[A-Z])(?=.*[!@#$%^&*+=?-_]).{6,}$";
        if (!password.matches(passwordPattern)) {
            etPassword.setError("Follow the password rules below.");
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Toast.makeText(Signup.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }

                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user == null) {
                        Toast.makeText(Signup.this, "Unable to create the account right now.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    UserProfileChangeRequest profileUpdate = new UserProfileChangeRequest.Builder()
                            .setDisplayName(name)
                            .build();
                    user.updateProfile(profileUpdate)
                            .addOnCompleteListener(profileTask -> {
                                saveUserProfileAndContinue(
                                        user,
                                        name,
                                        dateOfBirth,
                                        email,
                                        workEmail,
                                        personalPhoneNumber,
                                        workPhoneNumber,
                                        agencyWebsite,
                                        medicalLicense,
                                        personalIdentification,
                                        agencyName
                                );
                            });
                });
    }

    private void saveUserProfileAndContinue(
            FirebaseUser user,
            String name,
            String dateOfBirth,
            String email,
            String workEmail,
            String personalPhoneNumber,
            String workPhoneNumber,
            String agencyWebsite,
            String medicalLicense,
            String personalIdentification,
            String agencyName
    ) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("role", signupRole);
        userData.put("roleKey", signupRole);
        userData.put("roleLabel", RoleUtils.toLegacyRoleLabel(signupRole));
        userData.put("email", email);
        userData.put("name", name);
        userData.put("openId", user.getUid());
        userData.put("firebaseUid", user.getUid());
        userData.put("loginMethod", "email");
        userData.put("emailVerified", user.isEmailVerified());
        userData.put("updatedAt", FieldValue.serverTimestamp());
        userData.put("lastSignedIn", FieldValue.serverTimestamp());

        if (RoleUtils.ROLE_CONSULTANT.equals(signupRole)) {
            userData.put("dateOfBirth", dateOfBirth);
            userData.put("personalEmail", email);
            userData.put("workEmail", workEmail);
            userData.put("personalPhoneNumber", personalPhoneNumber);
            userData.put("workPhoneNumber", workPhoneNumber);
            userData.put("agencyWebsiteLink", agencyWebsite);
            userData.put("medicalLicense", medicalLicense);
            userData.put("personalIdentification", personalIdentification);
            userData.put("agencyName", agencyName);
        }

        firestore.collection(USERS_COLLECTION)
                .document(user.getUid())
                .set(userData, SetOptions.merge())
                .addOnSuccessListener(unused -> sendVerificationAndContinue(user))
                .addOnFailureListener(e -> Toast.makeText(
                        Signup.this,
                        "Account created, but failed to save your profile. Please try logging in again later.",
                        Toast.LENGTH_LONG
                ).show());
    }

    private void sendVerificationAndContinue(FirebaseUser user) {
        user.sendEmailVerification()
                .addOnCompleteListener(emailTask -> {
                    if (emailTask.isSuccessful()) {
                        Toast.makeText(
                                Signup.this,
                                "Account Created! Please check your email to verify.",
                                Toast.LENGTH_LONG
                        ).show();

                        mAuth.signOut();
                        Intent intent = new Intent(Signup.this, Login.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(
                                Signup.this,
                                "Failed to send verification email.",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
    }
}
