package com.mynyaman.mynyamanapp;

import    android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

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

public class Usagereport extends BaseActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private final List<ChildUsageRow> childUsageRows = new ArrayList<>();
    private ChildUsageListAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView tvEmptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.usagereport);

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        ImageView btnBack = findViewById(R.id.btnBack);
        RecyclerView rvChildUsageList = findViewById(R.id.rvChildUsageList);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        tvEmptyState = findViewById(R.id.tvEmptyState);

        rvChildUsageList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChildUsageListAdapter(childUsageRows, row -> {
            Intent intent = new Intent(this, ChildUsageDetailActivity.class);
            intent.putExtra(ChildUsageDetailActivity.EXTRA_CHILD_UID, row.uid);
            intent.putExtra(ChildUsageDetailActivity.EXTRA_CHILD_NAME, row.name);
            startActivity(intent);
        });
        rvChildUsageList.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());
        swipeRefreshLayout.setOnRefreshListener(this::loadChildUsageRows);

        loadChildUsageRows();
    }

    private void loadChildUsageRows() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, Login.class));
            finish();
            return;
        }

        firestore.collection("users")
                .document(currentUser.getUid())
                .collection("children")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    childUsageRows.clear();
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        long usageSeconds = UsageWindowHelper.extractCurrentDayUsageSeconds(document);
                        childUsageRows.add(new ChildUsageRow(
                                document.getId(),
                                document.getString("name"),
                                usageSeconds,
                                document.getTimestamp("usageLastSyncedAt")
                        ));
                    }
                    childUsageRows.sort(Comparator.comparingLong((ChildUsageRow row) -> row.usageTodaySeconds).reversed());
                    adapter.notifyDataSetChanged();
                    tvEmptyState.setVisibility(childUsageRows.isEmpty() ? View.VISIBLE : View.GONE);
                    swipeRefreshLayout.setRefreshing(false);
                })
                .addOnFailureListener(e -> {
                    swipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(this, "Unable to load child usage.", Toast.LENGTH_SHORT).show();
                });
    }

    private static class ChildUsageRow {
        final String uid;
        final String name;
        final long usageTodaySeconds;
        final Timestamp lastSynced;

        ChildUsageRow(String uid, String name, long usageTodaySeconds, Timestamp lastSynced) {
            this.uid = uid;
            this.name = name;
            this.usageTodaySeconds = usageTodaySeconds;
            this.lastSynced = lastSynced;
        }
    }

    private static class ChildUsageListAdapter extends RecyclerView.Adapter<ChildUsageListAdapter.ViewHolder> {
        interface OnChildClickListener {
            void onChildClick(ChildUsageRow row);
        }

        private final List<ChildUsageRow> rows;
        private final OnChildClickListener onChildClickListener;

        ChildUsageListAdapter(List<ChildUsageRow> rows, OnChildClickListener onChildClickListener) {
            this.rows = rows;
            this.onChildClickListener = onChildClickListener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_child_usage_card, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ChildUsageRow row = rows.get(position);
            holder.tvUsageChildName.setText(!TextUtils.isEmpty(row.name) ? row.name : "Child Account");
            holder.tvUsageChildTotal.setText(UsageStatsHelper.formatDurationFromSeconds(row.usageTodaySeconds));
            if (row.lastSynced != null) {
                holder.tvUsageChildSync.setText(
                        "Last synced: " + new SimpleDateFormat("dd/MM/yyyy hh:mm:ss a", Locale.getDefault()).format(row.lastSynced.toDate())
                );
            } else {
                holder.tvUsageChildSync.setText("Last synced: --");
            }
            holder.itemView.setOnClickListener(v -> onChildClickListener.onChildClick(row));
        }

        @Override
        public int getItemCount() {
            return rows.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            final TextView tvUsageChildName;
            final TextView tvUsageChildTotal;
            final TextView tvUsageChildSync;

            ViewHolder(View itemView) {
                super(itemView);
                tvUsageChildName = itemView.findViewById(R.id.tvUsageChildName);
                tvUsageChildTotal = itemView.findViewById(R.id.tvUsageChildTotal);
                tvUsageChildSync = itemView.findViewById(R.id.tvUsageChildSync);
            }
        }
    }
}
