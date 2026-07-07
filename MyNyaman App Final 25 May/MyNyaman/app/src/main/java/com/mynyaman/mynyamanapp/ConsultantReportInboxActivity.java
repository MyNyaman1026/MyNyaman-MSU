package com.mynyaman.mynyamanapp;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ConsultantReportInboxActivity extends BaseActivity {

    private final List<InboxRow> inboxRows = new ArrayList<>();
    private final InboxAdapter adapter = new InboxAdapter(inboxRows, row -> {
        Intent intent = new Intent(this, ConsultantReportDetailActivity.class);
        intent.putExtra(ConsultantReportDetailActivity.EXTRA_REPORT_ID, row.reportId);
        startActivity(intent);
    });

    private FirebaseFirestore firestore;
    private FirebaseUser currentUser;
    private ListenerRegistration reportListener;
    private TextView tvEmptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_consultant_report_inbox);

        firestore = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, Login.class));
            finish();
            return;
        }

        ImageView btnBack = findViewById(R.id.btnBack);
        RecyclerView rvIncomingReports = findViewById(R.id.rvIncomingReports);
        tvEmptyState = findViewById(R.id.tvEmptyState);

        rvIncomingReports.setLayoutManager(new LinearLayoutManager(this));
        rvIncomingReports.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());
        startListening();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (reportListener != null) {
            reportListener.remove();
            reportListener = null;
        }
    }

    private void startListening() {
        reportListener = firestore.collection("consultant_reports")
                .addSnapshotListener((queryDocumentSnapshots, error) -> {
                    if (error != null || queryDocumentSnapshots == null) {
                        return;
                    }

                    inboxRows.clear();
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        if (!isVisibleToCurrentConsultant(document)) {
                            continue;
                        }
                        inboxRows.add(new InboxRow(
                                document.getId(),
                                buildTitle(document),
                                stringOrFallback(document.getString("reportPreview"), BlueprintSyncRepository.buildConsultantReportPreview(document.getString("reportText"))),
                                buildMeta(document),
                                formatTimestamp(document.getTimestamp("createdAt")),
                                buildStatusLabel(document),
                                Boolean.TRUE.equals(document.getBoolean("followUpRequested")),
                                document.getTimestamp("updatedAt") != null ? document.getTimestamp("updatedAt").toDate() : timestampDate(document.getTimestamp("createdAt"))
                        ));
                    }
                    inboxRows.sort(Comparator.comparing((InboxRow row) -> row.sortTimestamp == null ? new Date(0L) : row.sortTimestamp).reversed());
                    adapter.notifyDataSetChanged();
                    tvEmptyState.setVisibility(inboxRows.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    private boolean isVisibleToCurrentConsultant(DocumentSnapshot document) {
        if (document == null) {
            return false;
        }
        if ("solved".equalsIgnoreCase(document.getString("visibleStatus"))) {
            return false;
        }
        String claimState = document.getString("claimState");
        String claimedByUid = document.getString("claimedByConsultantUid");
        if ("claimed".equalsIgnoreCase(claimState) && !TextUtils.isEmpty(claimedByUid)) {
            return TextUtils.equals(claimedByUid, currentUser.getUid());
        }
        return true;
    }

    private String buildTitle(DocumentSnapshot document) {
        String childName = document.getString("childName");
        return TextUtils.isEmpty(childName) ? "Child report" : childName;
    }

    private String buildMeta(DocumentSnapshot document) {
        String parentName = stringOrFallback(document.getString("parentName"), "Unknown parent");
        String reporterRole = RoleUtils.toLegacyRoleLabel(document.getString("reporterRole"));
        return "Parent: " + parentName + "  |  Reporter: " + reporterRole;
    }

    private String buildStatusLabel(DocumentSnapshot document) {
        String claimedByUid = document.getString("claimedByConsultantUid");
        if (TextUtils.equals(claimedByUid, currentUser.getUid())) {
            return "Pending (Yours)";
        }
        return "Pending";
    }

    private Date timestampDate(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toDate();
    }

    private String formatTimestamp(Timestamp timestamp) {
        if (timestamp == null) {
            return "Submitted just now";
        }
        return new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(timestamp.toDate());
    }

    private String stringOrFallback(String value, String fallback) {
        return TextUtils.isEmpty(value) ? fallback : value;
    }

    private static class InboxRow {
        final String reportId;
        final String title;
        final String preview;
        final String meta;
        final String timestamp;
        final String status;
        final boolean followUpRequested;
        final Date sortTimestamp;

        InboxRow(String reportId, String title, String preview, String meta, String timestamp, String status, boolean followUpRequested, Date sortTimestamp) {
            this.reportId = reportId;
            this.title = title;
            this.preview = preview;
            this.meta = meta;
            this.timestamp = timestamp;
            this.status = status;
            this.followUpRequested = followUpRequested;
            this.sortTimestamp = sortTimestamp;
        }
    }

    private static class InboxAdapter extends RecyclerView.Adapter<InboxAdapter.ViewHolder> {

        interface OnInboxClickListener {
            void onInboxClick(InboxRow row);
        }

        private final List<InboxRow> rows;
        private final OnInboxClickListener onInboxClickListener;

        InboxAdapter(List<InboxRow> rows, OnInboxClickListener onInboxClickListener) {
            this.rows = rows;
            this.onInboxClickListener = onInboxClickListener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_threat_report_card, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            InboxRow row = rows.get(position);
            holder.tvTitle.setText(row.title);
            holder.tvPreview.setText(row.preview);
            holder.tvMeta.setText(row.meta + (row.followUpRequested ? "  |  Follow-up active" : ""));
            holder.tvTimestamp.setText(row.timestamp);
            holder.tvStatus.setText(row.status);
            holder.tvStatus.getBackground().mutate().setColorFilter(0xFF2188FF, PorterDuff.Mode.SRC_ATOP);
            holder.tvFollowUp.setVisibility(View.GONE);
            holder.btnAction.setVisibility(View.GONE);
            holder.itemView.setOnClickListener(v -> onInboxClickListener.onInboxClick(row));
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
