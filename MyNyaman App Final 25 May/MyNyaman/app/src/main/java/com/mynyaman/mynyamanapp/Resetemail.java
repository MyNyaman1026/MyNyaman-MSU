package com.mynyaman.mynyamanapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

public class Resetemail extends BaseActivity {

    private EditText etResetEmail;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.resetemail);

        mAuth = FirebaseAuth.getInstance();
        etResetEmail = findViewById(R.id.etResetEmail);
        AppCompatButton btnSendResetLink = findViewById(R.id.btnSendResetLink);

        btnSendResetLink.setOnClickListener(v -> sendResetLink());
    }

    private void sendResetLink() {
        String email = etResetEmail.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etResetEmail.setError("Please enter your email");
            return;
        }

        // Send the reset email directly, bypassing the fetchSignInMethodsForEmail check
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> resetTask) {
                        if (resetTask.isSuccessful()) {
                            // Standard security practice: Don't confirm if the email exists to the user
                            Toast.makeText(Resetemail.this, "If this email is registered, a reset link has been sent.", Toast.LENGTH_LONG).show();
                            finish();
                        } else {
                            String errorMessage = resetTask.getException() != null
                                    ? resetTask.getException().getMessage()
                                    : "Unable to send reset link right now.";
                            Toast.makeText(Resetemail.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
}