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

public class ProfileeditChild extends BaseActivity {

    public static final String EXTRA_CHILD_UID = "CHILD_UID";
    public static final String EXTRA_CHILD_NAME = "CHILD_NAME";

    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private EditText etName;
    private EditText etContact;
    private EditText etEmail;
    private TextView tvProfileTitle;
    private String targetChildUid;
    private boolean isParentEditingChild;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profileedit_child);

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, Login.class));
            finish();
            return;
        }

        targetChildUid = getIntent().getStringExtra(EXTRA_CHILD_UID);
        isParentEditingChild = !TextUtils.isEmpty(targetChildUid) && !targetChildUid.equals(currentUser.getUid());

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

        String documentId = isParentEditingChild ? targetChildUid : currentUser.getUid();
        String headerName = getIntent().getStringExtra(EXTRA_CHILD_NAME);
        tvProfileTitle.setText(isParentEditingChild
                ? (!TextUtils.isEmpty(headerName) ? "Edit " + headerName : "Edit Child")
                : "Child Profile");

        firestore.collection("users")
                .document(documentId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String savedName = documentSnapshot.getString("name");
                    String savedContact = documentSnapshot.getString("contact");
                    String savedEmail = documentSnapshot.getString("email");

                    etName.setText(!TextUtils.isEmpty(savedName) ? savedName : headerName);
                    etContact.setText(savedContact != null ? savedContact : "");
                    etEmail.setText(savedEmail != null ? savedEmail : "");
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Unable to load child profile.", Toast.LENGTH_SHORT).show());
    }

    private void saveProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, Login.class));
            finish();
            return;
        }

        String name = etName.getText().toString().trim();
        String contact = etContact.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            etName.setError("Please enter your name");
            return;
        }

        if (isParentEditingChild) {
            saveParentManagedChild(currentUser.getUid(), name, contact);
        } else {
            saveSelfManagedChild(currentUser.getUid(), name, contact);
        }
    }

    private void saveParentManagedChild(String parentUid, String name, String contact) {
        firestore.collection("users")
                .document(targetChildUid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String email = documentSnapshot.getString("email");
                    Map<String, Object> childUpdates = new HashMap<>();
                    childUpdates.put("name", name);
                    childUpdates.put("contact", contact);

                    firestore.collection("users")
                            .document(targetChildUid)
                            .set(childUpdates, SetOptions.merge())
                            .addOnSuccessListener(unused -> {
                                Map<String, Object> childLinkUpdates = new HashMap<>();
                                childLinkUpdates.put("name", name);
                                childLinkUpdates.put("contact", contact);
                                if (!TextUtils.isEmpty(email)) {
                                    childLinkUpdates.put("email", email);
                                }
                                firestore.collection("users")
                                        .document(parentUid)
                                        .collection("children")
                                        .document(targetChildUid)
                                        .set(childLinkUpdates, SetOptions.merge())
                                        .addOnSuccessListener(result -> Toast.makeText(this, "Child profile updated.", Toast.LENGTH_SHORT).show())
                                        .addOnFailureListener(e -> Toast.makeText(this, "Failed to update child profile.", Toast.LENGTH_LONG).show());
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "Failed to update child profile.", Toast.LENGTH_LONG).show());
                });
    }

    private void saveSelfManagedChild(String currentUserUid, String name, String contact) {
        firestore.collection("users")
                .document(currentUserUid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String parentUid = documentSnapshot.getString("parentUid");
                    String email = documentSnapshot.getString("email");

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("name", name);
                    updates.put("contact", contact);

                    firestore.collection("users")
                            .document(currentUserUid)
                            .set(updates, SetOptions.merge())
                            .addOnSuccessListener(unused -> {
                                if (!TextUtils.isEmpty(parentUid)) {
                                    Map<String, Object> childLinkUpdates = new HashMap<>();
                                    childLinkUpdates.put("name", name);
                                    childLinkUpdates.put("contact", contact);
                                    if (!TextUtils.isEmpty(email)) {
                                        childLinkUpdates.put("email", email);
                                    }
                                    firestore.collection("users")
                                            .document(parentUid)
                                            .collection("children")
                                            .document(currentUserUid)
                                            .set(childLinkUpdates, SetOptions.merge());
                                }
                                Toast.makeText(this, "Child profile updated.", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "Failed to update child profile.", Toast.LENGTH_LONG).show());
                });
    }
}
