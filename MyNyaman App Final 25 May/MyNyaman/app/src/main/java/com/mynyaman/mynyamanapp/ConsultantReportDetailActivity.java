package com.mynyaman.mynyamanapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.FirebaseApp;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ConsultantReportDetailActivity extends BaseActivity {

    public static final String EXTRA_REPORT_ID = "EXTRA_REPORT_ID";

    private final List<EvidenceRow> evidenceRows = new ArrayList<>();
    private final EvidenceAdapter evidenceAdapter = new EvidenceAdapter(
            evidenceRows,
            this::showImagePreview,
            this::bindEvidenceThumbnail
    );

    private FirebaseFirestore firestore;
    private FirebaseStorage storage;
    private FirebaseUser currentUser;
    private String reportId;
    private String consultantName;
    private ListenerRegistration reportListener;
    private boolean claimedByCurrentConsultant;
    private boolean reportSolved;

    private TextView tvTitle;
    private TextView tvSubtitle;
    private TextView tvStatus;
    private TextView tvFamilySummary;
    private TextView tvTimestampSummary;
    private TextView tvReportText;
    private TextView tvNoEvidence;
    private TextView tvFollowUpSummary;
    private ProgressBar progressBar;
    private AppCompatButton btnRequestTalk;
    private AppCompatButton btnMarkDone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_consultant_report_detail);

        firestore = FirebaseFirestore.getInstance();
        storage = resolveStorageInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, Login.class));
            finish();
            return;
        }

        reportId = getIntent().getStringExtra(EXTRA_REPORT_ID);
        if (TextUtils.isEmpty(reportId)) {
            Toast.makeText(this, "Report not found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ImageView btnBack = findViewById(R.id.btnBack);
        RecyclerView rvEvidence = findViewById(R.id.rvEvidence);
        tvTitle = findViewById(R.id.tvTitle);
        tvSubtitle = findViewById(R.id.tvSubtitle);
        tvStatus = findViewById(R.id.tvStatus);
        tvFamilySummary = findViewById(R.id.tvFamilySummary);
        tvTimestampSummary = findViewById(R.id.tvTimestampSummary);
        tvReportText = findViewById(R.id.tvReportText);
        tvNoEvidence = findViewById(R.id.tvNoEvidence);
        tvFollowUpSummary = findViewById(R.id.tvFollowUpSummary);
        progressBar = findViewById(R.id.progressBar);
        btnRequestTalk = findViewById(R.id.btnRequestTalk);
        btnMarkDone = findViewById(R.id.btnMarkDone);

        rvEvidence.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
        rvEvidence.setAdapter(evidenceAdapter);

        btnBack.setOnClickListener(v -> finish());
        btnRequestTalk.setOnClickListener(v -> showFollowUpOptions());
        btnMarkDone.setOnClickListener(v -> markReportDone());

        setActionButtonsEnabled(false);
        loadConsultantNameAndClaim();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (reportListener != null) {
            reportListener.remove();
            reportListener = null;
        }
    }

    private void loadConsultantNameAndClaim() {
        firestore.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    consultantName = stringOrFallback(documentSnapshot.getString("name"), fallbackDisplayName(currentUser));
                    claimReport();
                })
                .addOnFailureListener(e -> {
                    consultantName = fallbackDisplayName(currentUser);
                    claimReport();
                });
    }

    private void claimReport() {
        BlueprintSyncRepository.claimConsultantReport(
                firestore,
                reportId,
                currentUser.getUid(),
                consultantName,
                new BlueprintSyncRepository.ConsultantReportClaimCallback() {
                    @Override
                    public void onSuccess(BlueprintSyncRepository.ConsultantReportClaimResult result) {
                        if (!result.availableToCurrentConsultant) {
                            if (result.solved) {
                                Toast.makeText(ConsultantReportDetailActivity.this, "This report has already been solved.", Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(ConsultantReportDetailActivity.this, "Another consultant already claimed this report.", Toast.LENGTH_LONG).show();
                            }
                            finish();
                            return;
                        }

                        claimedByCurrentConsultant = true;
                        tvSubtitle.setText(result.newlyClaimed
                                ? "Claimed by you. Review the report and choose the next action."
                                : "Already claimed by you. Continue your review below.");
                        startListeningToReport();
                    }

                    @Override
                    public void onFailure(Exception exception) {
                        Toast.makeText(ConsultantReportDetailActivity.this, "Unable to claim this report right now.", Toast.LENGTH_LONG).show();
                        finish();
                    }
                }
        );
    }

    private void startListeningToReport() {
        if (reportListener != null) {
            reportListener.remove();
        }
        reportListener = firestore.collection("consultant_reports")
                .document(reportId)
                .addSnapshotListener((documentSnapshot, error) -> {
                    if (error != null || documentSnapshot == null || !documentSnapshot.exists()) {
                        return;
                    }
                    bindReport(documentSnapshot);
                });
    }

    private void bindReport(DocumentSnapshot document) {
        progressBar.setVisibility(View.GONE);

        reportSolved = "solved".equalsIgnoreCase(document.getString("visibleStatus"));
        String claimedByUid = document.getString("claimedByConsultantUid");
        claimedByCurrentConsultant = TextUtils.equals(claimedByUid, currentUser.getUid());

        String childName = stringOrFallback(document.getString("childName"), "Child account");
        String parentName = stringOrFallback(document.getString("parentName"), "Linked parent");
        String reporterRole = RoleUtils.toLegacyRoleLabel(document.getString("reporterRole"));

        tvTitle.setText(childName);
        tvSubtitle.setText(reportSolved
                ? "This report has been completed."
                : "Claimed by you for consultant review.");
        tvStatus.setText(reportSolved ? "Solved" : "Pending");
        tvStatus.getBackground().mutate().setColorFilter(reportSolved ? 0xFF2E7D32 : 0xFF2188FF, PorterDuff.Mode.SRC_ATOP);
        tvFamilySummary.setText("Child: " + childName + "\nParent: " + parentName + "\nReporter: " + reporterRole);
        tvTimestampSummary.setText(
                "Submitted: " + formatTimestamp(document.getTimestamp("createdAt"))
                        + "\nUpdated: " + formatTimestamp(document.getTimestamp("updatedAt"))
        );
        tvReportText.setText(stringOrFallback(document.getString("reportText"), "No report details were saved."));

        boolean followUpRequested = Boolean.TRUE.equals(document.getBoolean("followUpRequested"));
        if (followUpRequested) {
            String target = followUpTargetLabel(document.getString("followUpTarget"));
            String message = stringOrFallback(document.getString("followUpMessage"), "Consultant requested a follow-up conversation.");
            tvFollowUpSummary.setText(target + " follow-up requested. " + message);
        } else {
            tvFollowUpSummary.setText("No follow-up request has been sent yet.");
        }

        bindEvidence(document);
        setActionButtonsEnabled(claimedByCurrentConsultant && !reportSolved);
    }

    @SuppressWarnings("unchecked")
    private void bindEvidence(DocumentSnapshot document) {
        evidenceRows.clear();
        Object rawEvidence = document.get("evidenceImages");
        if (rawEvidence instanceof List) {
            for (Object item : (List<?>) rawEvidence) {
                if (!(item instanceof Map)) {
                    continue;
                }
                Map<String, Object> map = (Map<String, Object>) item;
                evidenceRows.add(new EvidenceRow(
                        stringValue(map.get("storagePath")),
                        stringValue(map.get("downloadUrl")),
                        stringValue(map.get("fileName"))
                ));
            }
        }
        evidenceAdapter.notifyDataSetChanged();
        tvNoEvidence.setVisibility(evidenceRows.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showFollowUpOptions() {
        if (!claimedByCurrentConsultant || reportSolved) {
            return;
        }

        CharSequence[] options = new CharSequence[]{"Child", "Parent", "Child and Parent"};
        new AlertDialog.Builder(this)
                .setTitle("Request follow-up talk")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        requestFollowUp("child", "Consultant requested the child to talk with a consultant or parent.");
                    } else if (which == 1) {
                        requestFollowUp("parent", "Consultant requested the parent to talk with a consultant or child.");
                    } else {
                        requestFollowUp("both", "Consultant requested a follow-up conversation with both child and parent.");
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void requestFollowUp(String target, String message) {
        BlueprintSyncRepository.requestConsultantReportFollowUp(
                firestore,
                reportId,
                currentUser.getUid(),
                consultantName,
                target,
                message,
                new BlueprintSyncRepository.ConsultantReportWriteCallback() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(ConsultantReportDetailActivity.this, "Follow-up request saved.", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(Exception exception) {
                        Toast.makeText(ConsultantReportDetailActivity.this, "Unable to save the follow-up request right now.", Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    private void markReportDone() {
        if (!claimedByCurrentConsultant || reportSolved) {
            return;
        }

        BlueprintSyncRepository.markConsultantReportSolved(
                firestore,
                reportId,
                currentUser.getUid(),
                consultantName,
                new BlueprintSyncRepository.ConsultantReportWriteCallback() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(ConsultantReportDetailActivity.this, "Report marked as solved.", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(Exception exception) {
                        Toast.makeText(ConsultantReportDetailActivity.this, "Unable to mark this report as solved right now.", Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    private void setActionButtonsEnabled(boolean enabled) {
        btnRequestTalk.setEnabled(enabled);
        btnMarkDone.setEnabled(enabled);
        btnRequestTalk.setAlpha(enabled ? 1f : 0.6f);
        btnMarkDone.setAlpha(enabled ? 1f : 0.6f);
    }

    private void showImagePreview(EvidenceRow row) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(32, 32, 32, 32);

        ProgressBar dialogProgress = new ProgressBar(this);
        dialogProgress.setVisibility(View.VISIBLE);
        container.addView(dialogProgress, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        AppCompatImageView imageView = new AppCompatImageView(this);
        imageView.setAdjustViewBounds(true);
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        container.addView(imageView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(TextUtils.isEmpty(row.fileName) ? "Evidence Image" : row.fileName)
                .setView(container)
                .setPositiveButton("Close", null)
                .create();
        dialog.show();

        loadStorageImage(row.storagePath, imageView, dialogProgress);
    }

    private void bindEvidenceThumbnail(EvidenceRow row, ImageView imageView, ProgressBar imageProgress) {
        loadStorageImage(row.storagePath, imageView, imageProgress);
    }

    private void loadStorageImage(String storagePath, ImageView imageView, ProgressBar imageProgress) {
        if (imageProgress != null) {
            imageProgress.setVisibility(View.VISIBLE);
        }
        if (TextUtils.isEmpty(storagePath)) {
            if (imageProgress != null) {
                imageProgress.setVisibility(View.GONE);
            }
            imageView.setImageResource(android.R.drawable.ic_menu_report_image);
            return;
        }

        StorageReference storageReference = storage.getReference().child(normalizeStoragePath(storagePath));
        storageReference.getStream((taskSnapshot, inputStream) -> {
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            runOnUiThread(() -> {
                if (imageProgress != null) {
                    imageProgress.setVisibility(View.GONE);
                }
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap);
                } else {
                    imageView.setImageResource(android.R.drawable.ic_menu_report_image);
                }
            });
        }).addOnFailureListener(e -> runOnUiThread(() -> {
            if (imageProgress != null) {
                imageProgress.setVisibility(View.GONE);
            }
            imageView.setImageResource(android.R.drawable.ic_menu_report_image);
        }));
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
    private String normalizeStoragePath(String storagePath) {
        if (TextUtils.isEmpty(storagePath)) {
            return "";
        }
        return storagePath.startsWith("/") ? storagePath.substring(1) : storagePath;
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String stringOrFallback(String value, String fallback) {
        return TextUtils.isEmpty(value) ? fallback : value;
    }

    private String followUpTargetLabel(String target) {
        if ("both".equalsIgnoreCase(target)) {
            return "Child and parent";
        }
        if ("parent".equalsIgnoreCase(target)) {
            return "Parent";
        }
        return "Child";
    }

    private String formatTimestamp(Timestamp timestamp) {
        if (timestamp == null) {
            return "-";
        }
        return new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(timestamp.toDate());
    }

    private String fallbackDisplayName(FirebaseUser user) {
        if (user == null || TextUtils.isEmpty(user.getEmail())) {
            return "Consultant";
        }
        String email = user.getEmail();
        return email != null && email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
    }

    private static class EvidenceRow {
        final String storagePath;
        final String downloadUrl;
        final String fileName;

        EvidenceRow(String storagePath, String downloadUrl, String fileName) {
            this.storagePath = storagePath;
            this.downloadUrl = downloadUrl;
            this.fileName = fileName;
        }
    }

    private static class EvidenceAdapter extends RecyclerView.Adapter<EvidenceAdapter.ViewHolder> {

        interface OnEvidenceClickListener {
            void onEvidenceClick(EvidenceRow row);
        }

        interface OnEvidenceBindListener {
            void onEvidenceBind(EvidenceRow row, ImageView imageView, ProgressBar progressBar);
        }

        private final List<EvidenceRow> rows;
        private final OnEvidenceClickListener onEvidenceClickListener;
        private final OnEvidenceBindListener onEvidenceBindListener;

        EvidenceAdapter(
                List<EvidenceRow> rows,
                OnEvidenceClickListener onEvidenceClickListener,
                OnEvidenceBindListener onEvidenceBindListener
        ) {
            this.rows = rows;
            this.onEvidenceClickListener = onEvidenceClickListener;
            this.onEvidenceBindListener = onEvidenceBindListener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_report_evidence_image, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            EvidenceRow row = rows.get(position);
            holder.progressBar.setVisibility(View.VISIBLE);
            onEvidenceBindListener.onEvidenceBind(row, holder.imageView, holder.progressBar);
            holder.itemView.setOnClickListener(v -> onEvidenceClickListener.onEvidenceClick(row));
        }

        @Override
        public int getItemCount() {
            return rows.size();
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
