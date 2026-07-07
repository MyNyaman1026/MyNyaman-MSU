package com.mynyaman.mynyamanapp;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.FirebaseApp;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageException;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ThreatReportComposeActivity extends BaseActivity {

    public static final String EXTRA_OWNER_ROLE = "EXTRA_OWNER_ROLE";
    public static final String EXTRA_SELECTED_CHILD_UID = "EXTRA_SELECTED_CHILD_UID";
    public static final String EXTRA_SELECTED_CHILD_NAME = "EXTRA_SELECTED_CHILD_NAME";

    private static final int MAX_WORDS = 250;
    private static final int MAX_IMAGES = 5;

    public static Intent createIntent(Context context, String ownerRole, String selectedChildUid, String selectedChildName) {
        Intent intent = new Intent(context, ThreatReportComposeActivity.class);
        intent.putExtra(EXTRA_OWNER_ROLE, RoleUtils.normalizeRole(ownerRole));
        intent.putExtra(EXTRA_SELECTED_CHILD_UID, selectedChildUid);
        intent.putExtra(EXTRA_SELECTED_CHILD_NAME, selectedChildName);
        return intent;
    }

    private final List<Uri> selectedImageUris = new ArrayList<>();
    private final SelectedImageAdapter selectedImageAdapter = new SelectedImageAdapter(selectedImageUris);
    private final ActivityResultLauncher<String[]> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenMultipleDocuments(),
            this::onImagesChosen
    );

    private FirebaseFirestore firestore;
    private FirebaseStorage storage;
    private FirebaseUser currentUser;
    private String ownerRole;
    private String selectedChildUid;
    private String selectedChildName;
    private EditText etReportText;
    private TextView tvWordCount;
    private TextView tvContextTitle;
    private TextView tvContextSubtitle;
    private TextView tvImageHint;
    private AppCompatButton btnChooseImages;
    private AppCompatButton btnSubmitReport;
    private ProgressBar progressBar;
    private boolean isSubmitting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_threat_report_compose);

        firestore = FirebaseFirestore.getInstance();
        storage = resolveStorageInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, Login.class));
            finish();
            return;
        }

        ownerRole = RoleUtils.normalizeRole(getIntent().getStringExtra(EXTRA_OWNER_ROLE));
        selectedChildUid = getIntent().getStringExtra(EXTRA_SELECTED_CHILD_UID);
        selectedChildName = getIntent().getStringExtra(EXTRA_SELECTED_CHILD_NAME);
        if (RoleUtils.ROLE_PARENT.equals(ownerRole) && TextUtils.isEmpty(selectedChildUid)) {
            selectedChildUid = SelectedChildStore.getSelectedChildUid(this);
        }

        ImageView btnBack = findViewById(R.id.btnBack);
        RecyclerView rvSelectedImages = findViewById(R.id.rvSelectedImages);
        etReportText = findViewById(R.id.etReportText);
        tvWordCount = findViewById(R.id.tvWordCount);
        tvContextTitle = findViewById(R.id.tvContextTitle);
        tvContextSubtitle = findViewById(R.id.tvContextSubtitle);
        tvImageHint = findViewById(R.id.tvImageHint);
        btnChooseImages = findViewById(R.id.btnChooseImages);
        btnSubmitReport = findViewById(R.id.btnSubmitReport);
        progressBar = findViewById(R.id.progressBar);

        rvSelectedImages.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
        rvSelectedImages.setAdapter(selectedImageAdapter);

        btnBack.setOnClickListener(v -> finish());
        btnChooseImages.setOnClickListener(v -> imagePickerLauncher.launch(new String[]{"image/*"}));
        btnSubmitReport.setOnClickListener(v -> submitReport());
        etReportText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateWordCount();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        bindContext();
        updateWordCount();
        updateImageHint();
    }

    private void bindContext() {
        if (RoleUtils.ROLE_PARENT.equals(ownerRole)) {
            tvContextTitle.setText("Parent report context");
            String childLabel = TextUtils.isEmpty(selectedChildName) ? "the selected child" : selectedChildName;
            tvContextSubtitle.setText("This report will be linked to " + childLabel + " so consultants can review the right family context.");
            return;
        }

        tvContextTitle.setText("Child report context");
        tvContextSubtitle.setText("This report will be linked to your account and the parent account connected to you.");
    }

    private void onImagesChosen(List<Uri> uris) {
        if (uris == null) {
            return;
        }

        selectedImageUris.clear();
        for (Uri uri : uris) {
            if (selectedImageUris.size() >= MAX_IMAGES) {
                break;
            }
            selectedImageUris.add(uri);
        }
        if (uris.size() > MAX_IMAGES) {
            Toast.makeText(this, "Only the first 5 images were kept.", Toast.LENGTH_SHORT).show();
        }
        selectedImageAdapter.notifyDataSetChanged();
        updateImageHint();
    }

    private void updateWordCount() {
        int wordCount = countWords(etReportText.getText() == null ? "" : etReportText.getText().toString());
        tvWordCount.setText(String.format(Locale.getDefault(), "%d/%d words", wordCount, MAX_WORDS));
        tvWordCount.setTextColor(wordCount > MAX_WORDS ? 0xFFC62828 : 0xFF6B7280);
    }

    private void updateImageHint() {
        tvImageHint.setText(String.format(Locale.getDefault(),
                "Add up to %d screenshots or photos if they help explain the threat. Selected: %d/%d",
                MAX_IMAGES,
                selectedImageUris.size(),
                MAX_IMAGES
        ));
    }

    private void submitReport() {
        if (isSubmitting) {
            return;
        }

        String reportText = etReportText.getText() == null ? "" : etReportText.getText().toString().trim();
        int wordCount = countWords(reportText);
        if (TextUtils.isEmpty(reportText)) {
            etReportText.setError("Please describe the online safety threat.");
            return;
        }
        if (wordCount > MAX_WORDS) {
            etReportText.setError("Keep the report to 250 words or fewer.");
            return;
        }
        if (selectedImageUris.size() > MAX_IMAGES) {
            Toast.makeText(this, "You can attach up to 5 images.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (RoleUtils.ROLE_PARENT.equals(ownerRole) && TextUtils.isEmpty(selectedChildUid)) {
            Toast.makeText(this, "Select a child account first.", Toast.LENGTH_SHORT).show();
            return;
        }

        setSubmitting(true);
        firestore.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(userDocument -> resolveContextAndUpload(userDocument, reportText))
                .addOnFailureListener(e -> {
                    setSubmitting(false);
                    Toast.makeText(this, "Unable to prepare the report right now.", Toast.LENGTH_SHORT).show();
                });
    }

    private void resolveContextAndUpload(DocumentSnapshot userDocument, String reportText) {
        if (RoleUtils.ROLE_PARENT.equals(ownerRole)) {
            resolveParentContext(userDocument, reportText);
            return;
        }
        resolveChildContext(userDocument, reportText);
    }

    private void resolveParentContext(DocumentSnapshot userDocument, String reportText) {
        final String parentName = stringOrFallback(userDocument.getString("name"), fallbackDisplayName(currentUser));
        final String childUid = selectedChildUid;
        firestore.collection("users")
                .document(currentUser.getUid())
                .collection("children")
                .document(childUid)
                .get()
                .addOnSuccessListener(childDocument -> {
                    String childName = stringOrFallback(childDocument.getString("name"), selectedChildName);
                    if (TextUtils.isEmpty(childName)) {
                        setSubmitting(false);
                        Toast.makeText(this, "Unable to resolve the selected child.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    ResolvedReportContext context = new ResolvedReportContext(
                            currentUser.getUid(),
                            ownerRole,
                            parentName,
                            childUid,
                            childName,
                            currentUser.getUid(),
                            parentName
                    );
                    uploadEvidenceAndSave(context, reportText);
                })
                .addOnFailureListener(e -> {
                    setSubmitting(false);
                    Toast.makeText(this, "Unable to load the selected child right now.", Toast.LENGTH_SHORT).show();
                });
    }

    private void resolveChildContext(DocumentSnapshot userDocument, String reportText) {
        final String childName = stringOrFallback(userDocument.getString("name"), fallbackDisplayName(currentUser));
        final String parentUid = userDocument.getString("parentUid");
        if (TextUtils.isEmpty(parentUid)) {
            resolveChildParentFromRelationship(childName, reportText);
            return;
        }

        resolveChildParentProfile(childName, parentUid, reportText);
    }

    private void resolveChildParentFromRelationship(String childName, String reportText) {
        firestore.collection("parent_child_relationships")
                .whereEqualTo("childUid", currentUser.getUid())
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot == null || querySnapshot.isEmpty()) {
                        uploadEvidenceAndSave(buildChildReportContext(childName, "", ""), reportText);
                        return;
                    }

                    DocumentSnapshot relationship = querySnapshot.getDocuments().get(0);
                    String parentUid = relationship.getString("parentUid");
                    if (TextUtils.isEmpty(parentUid)) {
                        parentUid = relationship.getString("parentId");
                    }
                    String resolvedChildName = stringOrFallback(childName, relationship.getString("childName"));
                    resolvedChildName = stringOrFallback(resolvedChildName, relationship.getString("nickname"));

                    if (TextUtils.isEmpty(parentUid)) {
                        uploadEvidenceAndSave(buildChildReportContext(resolvedChildName, "", ""), reportText);
                        return;
                    }

                    resolveChildParentProfile(resolvedChildName, parentUid, reportText);
                })
                .addOnFailureListener(e -> uploadEvidenceAndSave(buildChildReportContext(childName, "", ""), reportText));
    }

    private void resolveChildParentProfile(String childName, String parentUid, String reportText) {
        firestore.collection("users")
                .document(parentUid)
                .get()
                .addOnSuccessListener(parentDocument -> {
                    String parentName = stringOrFallback(parentDocument.getString("name"), "Linked Parent");
                    uploadEvidenceAndSave(buildChildReportContext(childName, parentUid, parentName), reportText);
                })
                .addOnFailureListener(e -> uploadEvidenceAndSave(buildChildReportContext(childName, parentUid, "Linked Parent"), reportText));
    }

    private ResolvedReportContext buildChildReportContext(String childName, String parentUid, String parentName) {
        return new ResolvedReportContext(
                currentUser.getUid(),
                ownerRole,
                childName,
                currentUser.getUid(),
                childName,
                parentUid,
                parentName
        );
    }

    private void uploadEvidenceAndSave(ResolvedReportContext context, String reportText) {
        String reportId = BlueprintSyncRepository.newConsultantReportId(firestore);
        if (selectedImageUris.isEmpty()) {
            saveReportDocument(reportId, context, reportText, new ArrayList<>());
            return;
        }
        uploadEvidenceAtIndex(reportId, 0, context, reportText, new ArrayList<>());
    }

    private void uploadEvidenceAtIndex(
            String reportId,
            int index,
            ResolvedReportContext context,
            String reportText,
            List<Map<String, Object>> uploadedImages
    ) {
        if (index >= selectedImageUris.size()) {
            saveReportDocument(reportId, context, reportText, uploadedImages);
            return;
        }

        Uri imageUri = selectedImageUris.get(index);
        String fileName = safeFileName(imageUri, index);
        StorageReference evidenceReference = storage.getReference()
                .child("consultant_reports/" + reportId + "/evidence/" + fileName);

        evidenceReference.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> evidenceReference.getDownloadUrl()
                        .addOnSuccessListener(downloadUri -> {
                            Map<String, Object> imagePayload = new HashMap<>();
                            imagePayload.put("storagePath", evidenceReference.getPath());
                            imagePayload.put("downloadUrl", downloadUri.toString());
                            imagePayload.put("fileName", fileName);
                            imagePayload.put("uploadedAt", Timestamp.now());
                            uploadedImages.add(imagePayload);
                            uploadEvidenceAtIndex(reportId, index + 1, context, reportText, uploadedImages);
                        })
                        .addOnFailureListener(e -> handleStorageUploadFailure(
                                e,
                                "Image upload finished but the download link could not be saved.",
                                reportId,
                                context,
                                reportText,
                                uploadedImages
                        )))
                .addOnFailureListener(e -> handleStorageUploadFailure(
                        e,
                        "Unable to upload one of the selected images.",
                        reportId,
                        context,
                        reportText,
                        uploadedImages
                ));
    }

    private void saveReportDocument(String reportId, ResolvedReportContext context, String reportText, List<Map<String, Object>> uploadedImages) {
        BlueprintSyncRepository.createConsultantReport(
                firestore,
                reportId,
                context.reporterUid,
                context.reporterRole,
                context.reporterDisplayName,
                context.childUid,
                context.childName,
                context.parentUid,
                context.parentName,
                reportText,
                uploadedImages,
                new BlueprintSyncRepository.ConsultantReportWriteCallback() {
                    @Override
                    public void onSuccess() {
                        setSubmitting(false);
                        Toast.makeText(ThreatReportComposeActivity.this, "Report submitted successfully.", Toast.LENGTH_SHORT).show();
                        finish();
                    }

                    @Override
                    public void onFailure(Exception exception) {
                        setSubmitting(false);
                        Toast.makeText(ThreatReportComposeActivity.this, "Unable to save the report right now.", Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    private void setSubmitting(boolean submitting) {
        isSubmitting = submitting;
        progressBar.setVisibility(submitting ? View.VISIBLE : View.GONE);
        btnChooseImages.setEnabled(!submitting);
        btnSubmitReport.setEnabled(!submitting);
        etReportText.setEnabled(!submitting);
        btnSubmitReport.setAlpha(submitting ? 0.6f : 1f);
    }

    private int countWords(String text) {
        if (TextUtils.isEmpty(text)) {
            return 0;
        }
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return 0;
        }
        return trimmed.split("\\s+").length;
    }

    private FirebaseStorage resolveStorageInstance() {
        try {
            FirebaseApp app = FirebaseApp.getInstance();
            String bucketName = app.getOptions() == null ? null : app.getOptions().getStorageBucket();
            if (!TextUtils.isEmpty(bucketName)) {
                String bucketUrl = bucketName.startsWith("gs://") ? bucketName : "gs://" + bucketName;
                return FirebaseStorage.getInstance(app, bucketUrl);
            }
        } catch (IllegalStateException ignored) {
        }
        return FirebaseStorage.getInstance();
    }

    private void handleStorageUploadFailure(
            Exception exception,
            String fallbackMessage,
            String reportId,
            ResolvedReportContext context,
            String reportText,
            List<Map<String, Object>> uploadedImages
    ) {
        setSubmitting(false);
        String errorMessage = buildStorageErrorMessage(exception, fallbackMessage);
        new AlertDialog.Builder(this)
                .setTitle("Submit without images?")
                .setMessage(errorMessage + "\n\nYou can still submit this report without the remaining images.")
                .setPositiveButton("Submit report", (dialog, which) -> {
                    setSubmitting(true);
                    saveReportDocument(reportId, context, reportText, new ArrayList<>(uploadedImages));
                })
                .setNegativeButton("Keep editing", null)
                .show();
    }

    private String buildStorageErrorMessage(Exception exception, String fallbackMessage) {
        if (exception instanceof StorageException) {
            int errorCode = ((StorageException) exception).getErrorCode();
            if (errorCode == StorageException.ERROR_OBJECT_NOT_FOUND
                    || errorCode == StorageException.ERROR_BUCKET_NOT_FOUND
                    || errorCode == StorageException.ERROR_PROJECT_NOT_FOUND) {
                return "Firebase Storage bucket is not ready. Check Cloud Storage in Firebase Console and refresh google-services.json.";
            }
            if (errorCode == StorageException.ERROR_NOT_AUTHENTICATED) {
                return "Sign in again before uploading report evidence.";
            }
            if (errorCode == StorageException.ERROR_NOT_AUTHORIZED) {
                return "This account is not allowed to upload report evidence right now.";
            }
        }
        return fallbackMessage;
    }

    private String fallbackDisplayName(FirebaseUser user) {
        if (user == null || TextUtils.isEmpty(user.getEmail())) {
            return "MyNyaman User";
        }
        String email = user.getEmail();
        return email != null && email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
    }

    private String stringOrFallback(String value, String fallback) {
        return TextUtils.isEmpty(value) ? fallback : value;
    }

    private String safeFileName(Uri imageUri, int index) {
        String displayName = queryDisplayName(imageUri);
        if (!TextUtils.isEmpty(displayName)) {
            return displayName.replaceAll("[^A-Za-z0-9._-]", "_");
        }
        return "evidence_" + (index + 1) + ".jpg";
    }

    private String queryDisplayName(Uri imageUri) {
        if (imageUri == null) {
            return "";
        }
        ContentResolver contentResolver = getContentResolver();
        try (Cursor cursor = contentResolver.query(imageUri, null, null, null, null)) {
            if (cursor == null || !cursor.moveToFirst()) {
                return "";
            }
            int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            if (nameIndex < 0) {
                return "";
            }
            return cursor.getString(nameIndex);
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private static class ResolvedReportContext {
        final String reporterUid;
        final String reporterRole;
        final String reporterDisplayName;
        final String childUid;
        final String childName;
        final String parentUid;
        final String parentName;

        ResolvedReportContext(
                String reporterUid,
                String reporterRole,
                String reporterDisplayName,
                String childUid,
                String childName,
                String parentUid,
                String parentName
        ) {
            this.reporterUid = reporterUid;
            this.reporterRole = reporterRole;
            this.reporterDisplayName = reporterDisplayName;
            this.childUid = childUid;
            this.childName = childName;
            this.parentUid = parentUid;
            this.parentName = parentName;
        }
    }

    private static class SelectedImageAdapter extends RecyclerView.Adapter<SelectedImageAdapter.ViewHolder> {
        private final List<Uri> imageUris;

        SelectedImageAdapter(List<Uri> imageUris) {
            this.imageUris = imageUris;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_report_evidence_image, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.progressBar.setVisibility(View.GONE);
            holder.imageView.setImageURI(imageUris.get(position));
        }

        @Override
        public int getItemCount() {
            return imageUris.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            final ImageView imageView;
            final ProgressBar progressBar;

            ViewHolder(View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.ivEvidencePreview);
                progressBar = itemView.findViewById(R.id.progressEvidence);
            }
        }
    }
}
