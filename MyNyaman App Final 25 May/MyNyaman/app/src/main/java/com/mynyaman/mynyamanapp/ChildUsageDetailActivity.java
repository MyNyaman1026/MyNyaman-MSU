package com.mynyaman.mynyamanapp;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
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
import java.util.Map;

public class ChildUsageDetailActivity extends BaseActivity {

    public static final String EXTRA_CHILD_UID = "CHILD_UID";
    public static final String EXTRA_CHILD_NAME = "CHILD_NAME";

    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private String childUid;
    private TextView tvChildName;
    private TextView tvTotalTime;
    private TextView tvLastSynced;
    private TextView tvWeeklyUsage;
    private TextView tvMostUsedApp;
    private PieChart pieChart;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ListenerRegistration childUsageListenerRegistration;
    private final List<RemoteAppUsageInfo> appUsageList = new ArrayList<>();
    private ParentAppUsageAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_usage_detail);

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        childUid = getIntent().getStringExtra(EXTRA_CHILD_UID);

        ImageView btnBack = findViewById(R.id.btnBack);
        tvChildName = findViewById(R.id.tvChildName);
        tvTotalTime = findViewById(R.id.tvTotalTime);
        tvLastSynced = findViewById(R.id.tvLastSynced);
        tvWeeklyUsage = findViewById(R.id.tvWeeklyUsage);
        tvMostUsedApp = findViewById(R.id.tvMostUsedApp);
        pieChart = findViewById(R.id.pieChart);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        RecyclerView rvAppUsage = findViewById(R.id.rvAppUsage);

        rvAppUsage.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ParentAppUsageAdapter(appUsageList);
        rvAppUsage.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());
        swipeRefreshLayout.setOnRefreshListener(this::loadChildUsage);

        String childName = getIntent().getStringExtra(EXTRA_CHILD_NAME);
        tvChildName.setText(TextUtils.isEmpty(childName) ? "Child Device" : childName + "'s Device");

        setupPieChart();
    }

    @Override
    protected void onStart() {
        super.onStart();
        startChildUsageListener();
    }

    @Override
    protected void onStop() {
        removeChildUsageListener();
        super.onStop();
    }

    private void setupPieChart() {
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.WHITE);
        pieChart.setTransparentCircleRadius(55f);
        pieChart.setCenterText("Child\nUsage");
        pieChart.setCenterTextSize(16f);
        pieChart.getDescription().setEnabled(false);
        pieChart.setUsePercentValues(true);
        pieChart.setDrawEntryLabels(false);
        pieChart.setExtraOffsets(12f, 12f, 12f, 18f);

        Legend legend = pieChart.getLegend();
        legend.setEnabled(true);
        legend.setWordWrapEnabled(true);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
    }

    private void loadChildUsage() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, Login.class));
            finish();
            return;
        }
        if (TextUtils.isEmpty(childUid)) {
            Toast.makeText(this, "No child selected.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        firestore.collection("users")
                .document(currentUser.getUid())
                .collection("children")
                .document(childUid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    swipeRefreshLayout.setRefreshing(false);
                    if (documentSnapshot == null || !documentSnapshot.exists()) {
                        bindEmptyState("Child account not found.");
                        return;
                    }
                    bindChildData(documentSnapshot);
                    loadWeeklySummary(documentSnapshot);
                })
                .addOnFailureListener(e -> {
                    swipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(this, "Failed to load remote data", Toast.LENGTH_SHORT).show();
                });
    }

    private void startChildUsageListener() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, Login.class));
            finish();
            return;
        }
        if (TextUtils.isEmpty(childUid)) {
            Toast.makeText(this, "No child selected.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        removeChildUsageListener();
        childUsageListenerRegistration = firestore.collection("users")
                .document(currentUser.getUid())
                .collection("children")
                .document(childUid)
                .addSnapshotListener((documentSnapshot, error) -> {
                    swipeRefreshLayout.setRefreshing(false);
                    if (error != null) {
                        Toast.makeText(this, "Failed to listen for usage updates.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (documentSnapshot == null || !documentSnapshot.exists()) {
                        bindEmptyState("Child account not found.");
                        return;
                    }
                    bindChildData(documentSnapshot);
                    loadWeeklySummary(documentSnapshot);
                });
    }

    private void removeChildUsageListener() {
        if (childUsageListenerRegistration != null) {
            childUsageListenerRegistration.remove();
            childUsageListenerRegistration = null;
        }
    }

    @SuppressWarnings("unchecked")
    private void bindChildData(DocumentSnapshot document) {
        String name = document.getString("name");
        Timestamp lastSynced = document.getTimestamp("usageLastSyncedAt");

        tvChildName.setText(!TextUtils.isEmpty(name) ? name + "'s Device" : "Child Device");
        if (lastSynced != null) {
            tvLastSynced.setText("Last Synced: " + formatTimestamp(lastSynced.toDate()));
        } else {
            tvLastSynced.setText("Last Synced: --");
        }

        if (!UsageWindowHelper.isCurrentDay(document)) {
            bindEmptyUsagePreservingHeader();
            return;
        }

        appUsageList.clear();
        List<Map<String, Object>> topApps = (List<Map<String, Object>>) document.get("topApps");
        long totalMillis = 0L;
        String mostUsedApp = "None";
        if (topApps != null) {
            for (int index = 0; index < topApps.size(); index++) {
                Map<String, Object> map = topApps.get(index);
                String appName = stringValue(map.get("appName"));
                String packageName = stringValue(map.get("packageName"));
                long timeInForeground = readLong(map.get("timeInForeground"));
                totalMillis += timeInForeground;
                if (index == 0) {
                    mostUsedApp = DistractionAppFilter.defaultDisplayName(appName, packageName);
                }
                appUsageList.add(new RemoteAppUsageInfo(appName, packageName, timeInForeground, 0));
            }
        }

        if (totalMillis <= 0L) {
            totalMillis = UsageWindowHelper.extractCurrentDayUsageSeconds(document) * 1000L;
        }

        appUsageList.sort(Comparator.comparingLong((RemoteAppUsageInfo info) -> info.timeInForeground).reversed());
        for (RemoteAppUsageInfo info : appUsageList) {
            info.percentage = totalMillis > 0L ? (int) ((info.timeInForeground * 100L) / totalMillis) : 0;
        }

        tvTotalTime.setText(UsageStatsHelper.formatDurationFromMillis(totalMillis));
        tvMostUsedApp.setText(mostUsedApp);
        populatePieChart();
        adapter.notifyDataSetChanged();
    }

    private void bindEmptyUsagePreservingHeader() {
        tvTotalTime.setText("00 hrs 00 mins 00 secs");
        tvMostUsedApp.setText("None");
        pieChart.clear();
        pieChart.setVisibility(View.GONE);
        appUsageList.clear();
        adapter.notifyDataSetChanged();
    }

    private void bindEmptyState(String childTitle) {
        tvChildName.setText(childTitle);
        tvLastSynced.setText("Last Synced: --");
        tvWeeklyUsage.setText("00 hrs 00 mins 00 secs");
        bindEmptyUsagePreservingHeader();
    }

    private void loadWeeklySummary(DocumentSnapshot document) {
        long currentDayUsageSeconds = -1L;
        Object currentDayTopAppsData = null;
        if (UsageWindowHelper.isCurrentDay(document)) {
            currentDayUsageSeconds = UsageWindowHelper.extractCurrentDayUsageSeconds(document);
            currentDayTopAppsData = document.get("topApps");
        }

        BlueprintSyncRepository.fetchWeeklyUsageSummary(firestore, childUid, currentDayUsageSeconds, currentDayTopAppsData, new BlueprintSyncRepository.WeeklyUsageCallback() {
            @Override
            public void onSuccess(BlueprintSyncRepository.WeeklyUsageSummary summary) {
                tvWeeklyUsage.setText(UsageStatsHelper.formatDurationFromSeconds(summary.totalSeconds));
                if (TextUtils.isEmpty(tvMostUsedApp.getText()) || "None".contentEquals(tvMostUsedApp.getText())) {
                    tvMostUsedApp.setText(summary.mostUsedApp);
                }
            }

            @Override
            public void onFailure(Exception exception) {
                tvWeeklyUsage.setText("00 hrs 00 mins 00 secs");
            }
        });
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private long readLong(Object value) {
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }

    private void populatePieChart() {
        if (appUsageList.isEmpty()) {
            pieChart.clear();
            pieChart.setVisibility(View.GONE);
            return;
        }

        pieChart.setVisibility(View.VISIBLE);
        List<PieEntry> entries = new ArrayList<>();
        int maxSlices = Math.min(5, appUsageList.size());
        long otherTime = 0L;

        for (int i = 0; i < appUsageList.size(); i++) {
            RemoteAppUsageInfo info = appUsageList.get(i);
            if (i < maxSlices) {
                entries.add(new PieEntry((float) info.timeInForeground, DistractionAppFilter.defaultDisplayName(info.appName, info.packageName)));
            } else {
                otherTime += info.timeInForeground;
            }
        }

        if (otherTime > 0L) {
            entries.add(new PieEntry((float) otherTime, "Others"));
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        ArrayList<Integer> colors = new ArrayList<>();
        for (int color : ColorTemplate.MATERIAL_COLORS) {
            colors.add(color);
        }
        for (int color : ColorTemplate.PASTEL_COLORS) {
            colors.add(color);
        }
        dataSet.setColors(colors);
        dataSet.setSliceSpace(2f);

        PieData data = new PieData(dataSet);
        data.setDrawValues(true);
        data.setValueFormatter(new PercentFormatter(pieChart));
        data.setValueTextColor(Color.WHITE);
        data.setValueTextSize(12f);
        pieChart.setData(data);
        pieChart.invalidate();
    }

    private String formatTimestamp(Date date) {
        return new SimpleDateFormat("dd/MM/yyyy hh:mm:ss a", Locale.getDefault()).format(date);
    }

    private static class RemoteAppUsageInfo {
        final String appName;
        final String packageName;
        final long timeInForeground;
        int percentage;

        RemoteAppUsageInfo(String appName, String packageName, long timeInForeground, int percentage) {
            this.appName = appName;
            this.packageName = packageName;
            this.timeInForeground = timeInForeground;
            this.percentage = percentage;
        }
    }

    private static class ParentAppUsageAdapter extends RecyclerView.Adapter<ParentAppUsageAdapter.ViewHolder> {
        private final List<RemoteAppUsageInfo> list;

        ParentAppUsageAdapter(List<RemoteAppUsageInfo> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app_usage, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            RemoteAppUsageInfo info = list.get(position);
            holder.tvAppName.setText(DistractionAppFilter.defaultDisplayName(info.appName, info.packageName));

            PackageManager packageManager = holder.itemView.getContext().getPackageManager();
            if (!TextUtils.isEmpty(info.packageName)) {
                try {
                    Drawable icon = packageManager.getApplicationIcon(info.packageName);
                    holder.ivAppIcon.setImageDrawable(icon);
                } catch (PackageManager.NameNotFoundException e) {
                    holder.ivAppIcon.setImageResource(android.R.drawable.sym_def_app_icon);
                }
            } else {
                holder.ivAppIcon.setImageResource(android.R.drawable.sym_def_app_icon);
            }

            holder.tvAppTime.setText(UsageStatsHelper.formatDurationFromMillis(info.timeInForeground));
            holder.pbAppUsage.setProgress(info.percentage);
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            final ImageView ivAppIcon;
            final TextView tvAppName;
            final TextView tvAppTime;
            final ProgressBar pbAppUsage;

            ViewHolder(View itemView) {
                super(itemView);
                ivAppIcon = itemView.findViewById(R.id.ivAppIcon);
                tvAppName = itemView.findViewById(R.id.tvAppName);
                tvAppTime = itemView.findViewById(R.id.tvAppTime);
                pbAppUsage = itemView.findViewById(R.id.pbAppUsage);
            }
        }
    }
}
