package com.mynyaman.mynyamanapp;

import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ThreatReportListActivity extends BaseActivity {

    public static final String EXTRA_OWNER_ROLE = "EXTRA_OWNER_ROLE";
    public static final String EXTRA_SELECTED_CHILD_UID = "EXTRA_SELECTED_CHILD_UID";
    public static final String EXTRA_SELECTED_CHILD_NAME = "EXTRA_SELECTED_CHILD_NAME";

    public static Intent createIntent(Context context, String ownerRole, String selectedChildUid, String selectedChildName) {
        Intent intent = new Intent(context, ThreatReportListActivity.class);
        intent.putExtra(EXTRA_OWNER_ROLE, RoleUtils.normalizeRole(ownerRole));
        intent.putExtra(EXTRA_SELECTED_CHILD_UID, selectedChildUid);
        intent.putExtra(EXTRA_SELECTED_CHILD_NAME, selectedChildName);
        return intent;
    }

    private String ownerRole;
    private String selectedChildUid;
    private String selectedChildName;
    private final List<ReportRow> reportRows = new ArrayList<>();
    private final ReportCardAdapter adapter = new ReportCardAdapter(reportRows, row -> {
        if (row.followUpVisible) {
            startActivity(ConsultantHubActivity.createIntent(this, ownerRole));
        }
    });

    private FirebaseFirestore firestore;
    private FirebaseUser currentUser;
    private ListenerRegistration reportListener;
    private TextView tvSubtitle;
    private TextView tvSelectedContext;
    private TextView tvEmptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_threat_report_list);

        firestore = FirebaseFirestore.getInstance();
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
        CardView cardLodgeReport = findViewById(R.id.cardLodgeReport);
        RecyclerView rvReports = findViewById(R.id.rvReports);
        tvSubtitle = findViewById(R.id.tvSubtitle);
        tvSelectedContext = findViewById(R.id.tvSelectedContext);
        tvEmptyState = findViewById(R.id.tvEmptyState);

        rvReports.setLayoutManager(new LinearLayoutManager(this));
        rvReports.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());
        cardLodgeReport.setOnClickListener(v -> openComposeScreen());

        bindHeader();
        startListeningForReports();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (reportListener != null) {
            reportListener.remove();
            reportListener = null;
        }
    }

    private void bindHeader() {
        if (RoleUtils.ROLE_PARENT.equals(ownerRole)) {
            tvSubtitle.setText("Review reports linked to the active child context and lodge a new report when you need consultant support.");
            if (!TextUtils.isEmpty(selectedChildUid)) {
                tvSelectedContext.setVisibility(View.VISIBLE);
                String childLabel = TextUtils.isEmpty(selectedChildName) ? "the selected child" : selectedChildName;
                tvSelectedContext.setText("Currently showing reports for: " + childLabel);
            } else {
                tvSelectedContext.setVisibility(View.GONE);
            }
            return;
        }

        tvSubtitle.setText("Review reports connected to your account and reach out when something feels unsafe online.");
        tvSelectedContext.setVisibility(View.GONE);
    }

    private void openComposeScreen() {
        if (RoleUtils.ROLE_PARENT.equals(ownerRole) && TextUtils.isEmpty(selectedChildUid)) {
            Toast.makeText(this, "Select a child account first.", Toast.LENGTH_SHORT).show();
            return;
        }

        startActivity(ThreatReportComposeActivity.createIntent(this, ownerRole, selectedChildUid, selectedChildName));
    }

    private void startListeningForReports() {
        if (reportListener != null) {
            reportListener.remove();
            reportListener = null;
        }

        Query query;
        if (RoleUtils.ROLE_PARENT.equals(ownerRole)) {
            query = firestore.collection("consultant_reports")
                    .whereEqualTo("parentUid", currentUser.getUid());
        } else {
            query = firestore.collection("consultant_reports")
                    .whereEqualTo("childUid", currentUser.getUid());
        }

        reportListener = query.addSnapshotListener((queryDocumentSnapshots, error) -> {
            if (error != null) {
                Toast.makeText(this, "Unable to load reports right now.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (queryDocumentSnapshots == null) {
                return;
            }

            reportRows.clear();
            for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                if (RoleUtils.ROLE_PARENT.equals(ownerRole)
                        && !TextUtils.isEmpty(selectedChildUid)
                        && !TextUtils.equals(selectedChildUid, document.getString("childUid"))) {
                    continue;
                }
                reportRows.add(toReportRow(document));
            }
            reportRows.sort(Comparator.comparing((ReportRow row) -> row.sortTimestamp == null ? new Date(0L) : row.sortTimestamp).reversed());
            adapter.notifyDataSetChanged();
            tvEmptyState.setVisibility(reportRows.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    private ReportRow toReportRow(DocumentSnapshot document) {
        String reporterRole = RoleUtils.normalizeRole(document.getString("reporterRole"));
        String childName = document.getString("childName");
        String preview = document.getString("reportPreview");
        if (TextUtils.isEmpty(preview)) {
            preview = BlueprintSyncRepository.buildConsultantReportPreview(document.getString("reportText"));
        }

        boolean solved = "solved".equalsIgnoreCase(document.getString("visibleStatus"));
        boolean followUpRequested = Boolean.TRUE.equals(document.getBoolean("followUpRequested")) && !solved;
        String followUpTarget = document.getString("followUpTarget");
        String followUpMessage = document.getString("followUpMessage");
        Timestamp updatedAt = document.getTimestamp("updatedAt");
        Timestamp createdAt = document.getTimestamp("createdAt");

        return new ReportRow(
                buildTitle(reporterRole, childName),
                TextUtils.isEmpty(preview) ? "No report details available." : preview,
                buildMeta(reporterRole, document),
                formatTimestamp(createdAt),
                solved ? "Solved" : "Pending",
                solved,
                buildFollowUpNote(followUpRequested, followUpTarget, followUpMessage),
                followUpRequested,
                updatedAt != null ? updatedAt.toDate() : (createdAt == null ? null : createdAt.toDate())
        );
    }

    private String buildTitle(String reporterRole, String childName) {
        if (RoleUtils.ROLE_PARENT.equals(ownerRole)) {
            if (RoleUtils.ROLE_CHILD.equals(reporterRole)) {
                String label = TextUtils.isEmpty(childName) ? "Child" : childName;
                return label + " reported a threat";
            }
            String label = TextUtils.isEmpty(childName) ? "the selected child" : childName;
            return "Your report for " + label;
        }

        if (RoleUtils.ROLE_PARENT.equals(reporterRole)) {
            return "Parent-submitted report";
        }
        return "Your report";
    }

    private String buildMeta(String reporterRole, DocumentSnapshot document) {
        String childName = document.getString("childName");
        String parentName = document.getString("parentName");
        String reporterLabel = TextUtils.isEmpty(reporterRole)
                ? "Unknown"
                : RoleUtils.toLegacyRoleLabel(reporterRole);

        if (RoleUtils.ROLE_PARENT.equals(ownerRole)) {
            String childLabel = TextUtils.isEmpty(childName) ? "Selected child" : childName;
            return "Reporter: " + reporterLabel + "  |  Child: " + childLabel;
        }

        String parentLabel = TextUtils.isEmpty(parentName) ? "Linked parent" : parentName;
        return "Reporter: " + reporterLabel + "  |  Parent: " + parentLabel;
    }

    private String buildFollowUpNote(boolean followUpRequested, String followUpTarget, String followUpMessage) {
        if (!followUpRequested) {
            return "";
        }

        String targetLabel;
        if ("both".equalsIgnoreCase(followUpTarget)) {
            targetLabel = "Child and parent";
        } else if ("parent".equalsIgnoreCase(followUpTarget)) {
            targetLabel = "Parent";
        } else {
            targetLabel = "Child";
        }

        String message = TextUtils.isEmpty(followUpMessage)
                ? "Consultant requested a follow-up conversation."
                : followUpMessage;
        return targetLabel + " follow-up requested. " + message;
    }

    private String formatTimestamp(Timestamp timestamp) {
        if (timestamp == null) {
            return "Submitted just now";
        }
        return new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(timestamp.toDate());
    }

    private static class ReportRow {
        final String title;
        final String preview;
        final String meta;
        final String timestamp;
        final String visibleStatus;
        final boolean solved;
        final String followUpNote;
        final boolean followUpVisible;
        final Date sortTimestamp;

        ReportRow(
                String title,
                String preview,
                String meta,
                String timestamp,
                String visibleStatus,
                boolean solved,
                String followUpNote,
                boolean followUpVisible,
                Date sortTimestamp
        ) {
            this.title = title;
            this.preview = preview;
            this.meta = meta;
            this.timestamp = timestamp;
            this.visibleStatus = visibleStatus;
            this.solved = solved;
            this.followUpNote = followUpNote;
            this.followUpVisible = followUpVisible;
            this.sortTimestamp = sortTimestamp;
        }
    }

    private static class ReportCardAdapter extends RecyclerView.Adapter<ReportCardAdapter.ViewHolder> {

        interface OnActionClickListener {
            void onActionClick(ReportRow row);
        }

        private final List<ReportRow> rows;
        private final OnActionClickListener onActionClickListener;

        ReportCardAdapter(List<ReportRow> rows, OnActionClickListener onActionClickListener) {
            this.rows = rows;
            this.onActionClickListener = onActionClickListener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_threat_report_card, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ReportRow row = rows.get(position);
            holder.tvTitle.setText(row.title);
            holder.tvPreview.setText(row.preview);
            holder.tvMeta.setText(row.meta);
            holder.tvTimestamp.setText(row.timestamp);
            holder.tvStatus.setText(row.visibleStatus);
            if (row.solved) {
                holder.tvStatus.getBackground().mutate().setColorFilter(0xFF2E7D32, PorterDuff.Mode.SRC_ATOP);
            } else {
                holder.tvStatus.getBackground().mutate().setColorFilter(0xFF2188FF, PorterDuff.Mode.SRC_ATOP);
            }

            holder.tvFollowUp.setVisibility(row.followUpVisible ? View.VISIBLE : View.GONE);
            holder.btnAction.setVisibility(row.followUpVisible ? View.VISIBLE : View.GONE);
            holder.tvFollowUp.setText(row.followUpNote);
            holder.btnAction.setOnClickListener(v -> onActionClickListener.onActionClick(row));
        }

        @Override
        public int getItemCount() {
            return rows.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            final TextView tvTitle;
            final TextView tvPreview;
            final TextView tvMeta;
            final TextView tvTimestamp;
            final TextView tvStatus;
            final TextView tvFollowUp;
            final AppCompatButton btnAction;

            ViewHolder(View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvReportCardTitle);
                tvPreview = itemView.findViewById(R.id.tvReportCardPreview);
                tvMeta = itemView.findViewById(R.id.tvReportCardMeta);
                tvTimestamp = itemView.findViewById(R.id.tvReportCardTimestamp);
                tvStatus = itemView.findViewById(R.id.tvReportCardStatus);
                tvFollowUp = itemView.findViewById(R.id.tvReportCardFollowUp);
                btnAction = itemView.findViewById(R.id.btnReportCardAction);
            }
        }
    }
}
