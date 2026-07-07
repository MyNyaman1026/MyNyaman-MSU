package com.mynyaman.mynyamanapp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class ConsultantSessionHistoryActivity extends BaseActivity {

    public static final String EXTRA_OWNER_ROLE = "EXTRA_OWNER_ROLE";

    public static Intent createIntent(Context context, String ownerRole) {
        Intent intent = new Intent(context, ConsultantSessionHistoryActivity.class);
        intent.putExtra(EXTRA_OWNER_ROLE, RoleUtils.normalizeRole(ownerRole));
        return intent;
    }

    private final List<HistoryRow> sessions = new ArrayList<>();
    private final SessionHistoryAdapter adapter = new SessionHistoryAdapter(sessions);
    private FirebaseFirestore firestore;
    private TextView tvEmptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_consultant_session_history);

        firestore = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, Login.class));
            finish();
            return;
        }

        ImageView btnBack = findViewById(R.id.btnBack);
        RecyclerView rvHistory = findViewById(R.id.rvSessionHistory);
        tvEmptyState = findViewById(R.id.tvEmptyState);

        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        rvHistory.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());
        loadHistory(currentUser.getUid());
    }

    private void loadHistory(String ownerUid) {
        firestore.collection("conversations")
                .whereEqualTo("ownerUid", ownerUid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    sessions.clear();
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        if (!"ai".equals(document.getString("consultantType"))) {
                            continue;
                        }
                        sessions.add(new HistoryRow(
                                document.getString("title"),
                                document.getString("lastMessage"),
                                document.getLong("messageCount") == null ? 0L : document.getLong("messageCount"),
                                document.getTimestamp("updatedAt"),
                                document.getString("status")
                        ));
                    }
                    loadSolvedReportHistory(ownerUid);
                })
                .addOnFailureListener(e -> {
                    sessions.clear();
                    loadSolvedReportHistory(ownerUid);
                });
    }

    private void loadSolvedReportHistory(String consultantUid) {
        firestore.collection("consultant_reports")
                .whereEqualTo("claimedByConsultantUid", consultantUid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        if (!isSolvedReport(document)) {
                            continue;
                        }
                        sessions.add(new HistoryRow(
                                buildSolvedReportTitle(document),
                                buildSolvedReportPreview(document),
                                buildSolvedReportMeta(document),
                                bestReportTimestamp(document),
                                "Solved Report"
                        ));
                    }
                    publishHistoryRows();
                })
                .addOnFailureListener(e -> publishHistoryRows());
    }

    private boolean isSolvedReport(DocumentSnapshot document) {
        return "solved".equalsIgnoreCase(document.getString("visibleStatus"))
                || "solved".equalsIgnoreCase(document.getString("workflowState"));
    }

    private String buildSolvedReportTitle(DocumentSnapshot document) {
        String childName = document.getString("childName");
        return TextUtils.isEmpty(childName) ? "Solved family report" : childName;
    }

    private String buildSolvedReportPreview(DocumentSnapshot document) {
        String preview = document.getString("reportPreview");
        if (!TextUtils.isEmpty(preview)) {
            return preview;
        }
        return BlueprintSyncRepository.buildConsultantReportPreview(document.getString("reportText"));
    }

    private String buildSolvedReportMeta(DocumentSnapshot document) {
        String reporterRole = RoleUtils.toLegacyRoleLabel(document.getString("reporterRole"));
        String parentName = document.getString("parentName");
        String meta = "Report from " + (TextUtils.isEmpty(reporterRole) ? "Family" : reporterRole);
        if (!TextUtils.isEmpty(parentName)) {
            meta += " | Parent: " + parentName;
        }
        return meta;
    }

    private Timestamp bestReportTimestamp(DocumentSnapshot document) {
        Timestamp solvedAt = document.getTimestamp("solvedAt");
        if (solvedAt != null) {
            return solvedAt;
        }
        Timestamp updatedAt = document.getTimestamp("updatedAt");
        if (updatedAt != null) {
            return updatedAt;
        }
        return document.getTimestamp("createdAt");
    }

    private void publishHistoryRows() {
        sessions.sort(Comparator.comparing((HistoryRow row) -> row.updatedAt == null ? Timestamp.now() : row.updatedAt).reversed());
        adapter.notifyDataSetChanged();
        tvEmptyState.setVisibility(sessions.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private static class HistoryRow {
        final String title;
        final String lastMessage;
        final String meta;
        final Timestamp updatedAt;
        final String status;

        HistoryRow(String title, String lastMessage, long messageCount, Timestamp updatedAt, String status) {
            this(title, lastMessage, (messageCount <= 0 ? 0 : messageCount) + " messages", updatedAt, status);
        }

        HistoryRow(String title, String lastMessage, String meta, Timestamp updatedAt, String status) {
            this.title = title;
            this.lastMessage = lastMessage;
            this.meta = meta;
            this.updatedAt = updatedAt;
            this.status = status;
        }
    }

    private static class SessionHistoryAdapter extends RecyclerView.Adapter<SessionHistoryAdapter.ViewHolder> {

        private final List<HistoryRow> sessions;
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());

        SessionHistoryAdapter(List<HistoryRow> sessions) {
            this.sessions = sessions;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_consultant_history, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            HistoryRow row = sessions.get(position);
            holder.tvTitle.setText(TextUtils.isEmpty(row.title) ? "AI Consultant Session" : row.title);
            holder.tvPreview.setText(TextUtils.isEmpty(row.lastMessage) ? "No messages saved." : row.lastMessage);
            holder.tvMeta.setText(TextUtils.isEmpty(row.meta) ? "0 messages" : row.meta);
            holder.tvStatus.setText(TextUtils.isEmpty(row.status) ? "Closed" : capitalize(row.status));
            holder.tvTimestamp.setText(row.updatedAt == null ? "Saved just now" : dateFormat.format(row.updatedAt.toDate()));
        }

        @Override
        public int getItemCount() {
            return sessions.size();
        }

        private String capitalize(String value) {
            if (TextUtils.isEmpty(value)) {
                return "Closed";
            }
            return value.substring(0, 1).toUpperCase(Locale.getDefault()) + value.substring(1);
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            final CardView cardRoot;
            final TextView tvTitle;
            final TextView tvPreview;
            final TextView tvMeta;
            final TextView tvStatus;
            final TextView tvTimestamp;

            ViewHolder(View itemView) {
                super(itemView);
                cardRoot = itemView.findViewById(R.id.cardRoot);
                tvTitle = itemView.findViewById(R.id.tvTitle);
                tvPreview = itemView.findViewById(R.id.tvPreview);
                tvMeta = itemView.findViewById(R.id.tvMeta);
                tvStatus = itemView.findViewById(R.id.tvStatus);
                tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            }
        }
    }
}
