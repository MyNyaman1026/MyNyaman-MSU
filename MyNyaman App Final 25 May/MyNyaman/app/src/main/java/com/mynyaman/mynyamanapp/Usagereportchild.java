package com.mynyaman.mynyamanapp;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Usagereportchild extends BaseActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private TextView tvPermissionStatus;
    private TextView tvTotalTime;
    private TextView tvWeeklyUsage;
    private TextView tvMostUsedApp;
    private AppCompatButton btnGrantAccess;
    private SwipeRefreshLayout swipeRefreshLayout;
    private PieChart pieChart;
    private final List<AppUsageInfo> appUsageList = new ArrayList<>();
    private AppUsageAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.usagereportchild);

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        ImageView btnBack = findViewById(R.id.btnBack);
        btnGrantAccess = findViewById(R.id.btnGrantUsageAccess);
        tvPermissionStatus = findViewById(R.id.tvPermissionStatus);
        tvTotalTime = findViewById(R.id.tvTotalTime);
        tvWeeklyUsage = findViewById(R.id.tvWeeklyUsage);
        tvMostUsedApp = findViewById(R.id.tvMostUsedApp);
        RecyclerView rvAppUsage = findViewById(R.id.rvAppUsage);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        pieChart = findViewById(R.id.pieChart);

        rvAppUsage.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AppUsageAdapter(appUsageList);
        rvAppUsage.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());
        btnGrantAccess.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)));
        swipeRefreshLayout.setOnRefreshListener(this::loadUsageData);

        setupPieChart();
        loadUsageData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (appUsageList.isEmpty()) {
            loadUsageData();
        }
    }

    private void setupPieChart() {
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.WHITE);
        pieChart.setTransparentCircleRadius(55f);
        pieChart.setCenterText("Usage\nShare");
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

    private void loadUsageData() {
        forceBackgroundSync();

        if (!UsageStatsHelper.hasUsageStatsPermission(this)) {
            tvPermissionStatus.setVisibility(View.VISIBLE);
            btnGrantAccess.setVisibility(View.VISIBLE);
            pieChart.setVisibility(View.GONE);
            tvTotalTime.setText("00 hrs 00 mins 00 secs");
            tvWeeklyUsage.setText("00 hrs 00 mins 00 secs");
            tvMostUsedApp.setText("None");
            appUsageList.clear();
            adapter.notifyDataSetChanged();
            swipeRefreshLayout.setRefreshing(false);
            return;
        }

        tvPermissionStatus.setVisibility(View.GONE);
        btnGrantAccess.setVisibility(View.GONE);

        List<UsageStatsHelper.AppUsageRecord> usageRecords = UsageStatsHelper.getTodayUsageRecords(this);
        long totalForegroundTime = 0L;
        appUsageList.clear();
        String mostUsedApp = "None";

        for (int index = 0; index < usageRecords.size(); index++) {
            UsageStatsHelper.AppUsageRecord record = usageRecords.get(index);
            totalForegroundTime += record.timeInForegroundMillis;
            if (index == 0) {
                mostUsedApp = record.appName;
            }
            appUsageList.add(new AppUsageInfo(
                    record.appName,
                    record.packageName,
                    record.timeInForegroundMillis,
                    record.percentage
            ));
        }

        tvTotalTime.setText(UsageStatsHelper.formatDurationFromMillis(totalForegroundTime));
        tvMostUsedApp.setText(TextUtils.isEmpty(mostUsedApp) ? "None" : mostUsedApp);
        populatePieChart();
        adapter.notifyDataSetChanged();
        List<Map<String, Object>> topAppsData = UsageStatsHelper.toTopAppsPayload(usageRecords);
        syncUsageToFirebase(totalForegroundTime, topAppsData);
        loadWeeklySummary(totalForegroundTime / 1000L, topAppsData);
        swipeRefreshLayout.setRefreshing(false);
    }

    private void loadWeeklySummary(long currentDayUsageSeconds, List<Map<String, Object>> currentDayTopAppsData) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            tvWeeklyUsage.setText("00 hrs 00 mins 00 secs");
            return;
        }

        BlueprintSyncRepository.fetchWeeklyUsageSummary(firestore, currentUser.getUid(), currentDayUsageSeconds, currentDayTopAppsData, new BlueprintSyncRepository.WeeklyUsageCallback() {
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

    private void forceBackgroundSync() {
        Intent serviceIntent = new Intent(this, LimitService.class);
        serviceIntent.setAction(LimitService.ACTION_FORCE_SYNC);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
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
            AppUsageInfo info = appUsageList.get(i);
            if (i < maxSlices) {
                entries.add(new PieEntry((float) info.timeInForeground, info.appName));
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

    private void syncUsageToFirebase(long totalForegroundTime, List<Map<String, Object>> topAppsData) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null || !UsageStatsHelper.hasUsageStatsPermission(this)) {
            return;
        }

        long usageSeconds = totalForegroundTime / 1000L;
        long usageMinutes = usageSeconds / 60L;
        Timestamp now = Timestamp.now();

        firestore.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String parentUid = documentSnapshot.getString("parentUid");
                    String name = documentSnapshot.getString("name");

                    Map<String, Object> childUsage = new HashMap<>();
                    childUsage.put("usageDayKey", UsageWindowHelper.currentDayKey());
                    childUsage.put("usageWeekKey", UsageWindowHelper.currentWeekStartKey());
                    childUsage.put("usageTodayMinutes", usageMinutes);
                    childUsage.put("usageTodaySeconds", usageSeconds);
                    childUsage.put("usageLastSyncedAt", now);
                    childUsage.put("topApps", topAppsData);
                    childUsage.put("usagePermissionGranted", true);

                    firestore.collection("users")
                            .document(currentUser.getUid())
                            .set(childUsage, SetOptions.merge())
                            .addOnSuccessListener(unused -> {
                                if (!TextUtils.isEmpty(parentUid)) {
                                    Map<String, Object> parentChildUsage = new HashMap<>(childUsage);
                                    parentChildUsage.put("name", name);
                                    firestore.collection("users")
                                            .document(parentUid)
                                            .collection("children")
                                            .document(currentUser.getUid())
                                            .set(parentChildUsage, SetOptions.merge());
                                }
                                BlueprintSyncRepository.syncUsageReport(
                                        firestore,
                                        currentUser.getUid(),
                                        parentUid,
                                        name,
                                        totalForegroundTime,
                                        topAppsData
                                );
                            });
                });
    }

    private static class AppUsageInfo {
        final String appName;
        final String packageName;
        final long timeInForeground;
        final int percentage;

        AppUsageInfo(String appName, String packageName, long timeInForeground, int percentage) {
            this.appName = appName;
            this.packageName = packageName;
            this.timeInForeground = timeInForeground;
            this.percentage = percentage;
        }
    }

    private static class AppUsageAdapter extends RecyclerView.Adapter<AppUsageAdapter.ViewHolder> {
        private final List<AppUsageInfo> list;

        AppUsageAdapter(List<AppUsageInfo> list) {
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
            AppUsageInfo info = list.get(position);
            holder.tvAppName.setText(info.appName);

            if (!TextUtils.isEmpty(info.packageName)) {
                try {
                    Drawable icon = holder.itemView.getContext().getPackageManager().getApplicationIcon(info.packageName);
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


