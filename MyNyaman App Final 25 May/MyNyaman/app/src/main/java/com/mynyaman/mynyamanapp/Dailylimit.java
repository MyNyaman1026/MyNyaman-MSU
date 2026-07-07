package com.mynyaman.mynyamanapp;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Dailylimit extends BaseActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private RecyclerView rvAppLimits;
    private TextView tvSelectedChildName;
    private TextView tvLimitEmptyState;
    private LinearLayout childSelectorContainer;
    private final List<AppLimitInfo> appLimitList = new ArrayList<>();
    private final List<ChildAccountRow> childAccounts = new ArrayList<>();
    private final Map<String, Long> savedLimitSecondsByPackage = new HashMap<>();
    private AppLimitAdapter adapter;
    private String childDocumentId;
    private String childDisplayName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dailylimit);

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        childDocumentId = getIntent().getStringExtra("CHILD_UID");
        childDisplayName = getIntent().getStringExtra("CHILD_NAME");

        if (!TextUtils.isEmpty(childDocumentId)) {
            SelectedChildStore.saveSelectedChildUid(this, childDocumentId);
        }

        ImageView btnBack = findViewById(R.id.btnBack);
        rvAppLimits = findViewById(R.id.rvAppLimits);
        tvSelectedChildName = findViewById(R.id.tvSelectedChildName);
        tvLimitEmptyState = findViewById(R.id.tvLimitEmptyState);
        childSelectorContainer = findViewById(R.id.childSelectorContainer);
        AppCompatButton btnSaveLimit = findViewById(R.id.btnSaveLimit);

        rvAppLimits.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AppLimitAdapter(appLimitList);
        rvAppLimits.setAdapter(adapter);

        tvSelectedChildName.setText(TextUtils.isEmpty(childDisplayName) ? "Loading child..." : childDisplayName);
        btnBack.setOnClickListener(v -> finish());
        btnSaveLimit.setOnClickListener(v -> saveLimitsToFirebase());

        fetchChildren();
    }

    private void fetchChildren() {
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
                    childAccounts.clear();
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        childAccounts.add(new ChildAccountRow(
                                document.getId(),
                                document.getString("name"),
                                document.getString("email")
                        ));
                    }
                    Collections.sort(childAccounts, Comparator.comparing(child -> safeName(child.name)));
                    resolveSelectedChild();
                    renderChildSelector();
                    fetchChildApps();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Unable to load child accounts.", Toast.LENGTH_SHORT).show());
    }

    private void resolveSelectedChild() {
        if (childAccounts.isEmpty()) {
            childDocumentId = null;
            childDisplayName = null;
            tvSelectedChildName.setText("No child selected");
            return;
        }

        if (!TextUtils.isEmpty(childDocumentId)) {
            for (ChildAccountRow child : childAccounts) {
                if (child.uid.equals(childDocumentId)) {
                    childDisplayName = child.name;
                    SelectedChildStore.saveSelectedChildUid(this, childDocumentId);
                    tvSelectedChildName.setText(displayChildName(childDisplayName));
                    return;
                }
            }
        }

        List<String> childUids = new ArrayList<>();
        for (ChildAccountRow child : childAccounts) {
            childUids.add(child.uid);
        }

        childDocumentId = SelectedChildStore.resolveSelectedChildUid(this, childUids);
        for (ChildAccountRow child : childAccounts) {
            if (child.uid.equals(childDocumentId)) {
                childDisplayName = child.name;
                break;
            }
        }
        tvSelectedChildName.setText(displayChildName(childDisplayName));
    }

    private void renderChildSelector() {
        childSelectorContainer.removeAllViews();
        for (ChildAccountRow child : childAccounts) {
            AppCompatButton chip = new AppCompatButton(this);
            chip.setBackgroundResource(R.drawable.child_chip_background);
            chip.setAllCaps(false);
            chip.setMinWidth(dpToPx(132));
            chip.setPadding(dpToPx(22), 0, dpToPx(22), 0);
            chip.setText(displayChildName(child.name));
            chip.setTextColor(0xFFFFFFFF);
            chip.setTypeface(chip.getTypeface(), Typeface.BOLD);
            chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            chip.setSelected(TextUtils.equals(child.uid, childDocumentId));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    dpToPx(40)
            );
            params.setMarginEnd(dpToPx(10));
            chip.setLayoutParams(params);
            chip.setOnClickListener(v -> selectChild(child));
            childSelectorContainer.addView(chip);
        }
    }

    private void selectChild(ChildAccountRow child) {
        childDocumentId = child.uid;
        childDisplayName = child.name;
        SelectedChildStore.saveSelectedChildUid(this, child.uid);
        tvSelectedChildName.setText(displayChildName(childDisplayName));
        renderChildSelector();
        fetchChildApps();
    }

    private void fetchChildApps() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, Login.class));
            finish();
            return;
        }

        if (TextUtils.isEmpty(childDocumentId)) {
            appLimitList.clear();
            adapter.notifyDataSetChanged();
            showEmptyState(true);
            return;
        }

        loadChildUsageApps(currentUser.getUid());
    }

    @SuppressWarnings("unchecked")
    private void loadChildUsageApps(String parentUid) {
        firestore.collection("users")
                .document(parentUid)
                .collection("children")
                .document(childDocumentId)
                .get()
                .addOnSuccessListener(document -> {
                    childDisplayName = document.getString("name");
                    tvSelectedChildName.setText(displayChildName(childDisplayName));

                    savedLimitSecondsByPackage.clear();
                    savedLimitSecondsByPackage.putAll(readLimitSeconds(document));

                    List<Map<String, Object>> topApps = (List<Map<String, Object>>) document.get("topApps");
                    firestore.collection("app_time_limits")
                            .whereEqualTo("childUid", childDocumentId)
                            .get()
                            .addOnSuccessListener(limitSnapshots -> {
                                mergeMirroredAppLimits(limitSnapshots.getDocuments());
                                appLimitList.clear();
                                appLimitList.addAll(buildFilteredAppLimitList(topApps, savedLimitSecondsByPackage));
                                adapter.notifyDataSetChanged();
                                showEmptyState(appLimitList.isEmpty());
                            })
                            .addOnFailureListener(e -> {
                                appLimitList.clear();
                                appLimitList.addAll(buildFilteredAppLimitList(topApps, savedLimitSecondsByPackage));
                                adapter.notifyDataSetChanged();
                                showEmptyState(appLimitList.isEmpty());
                            });
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Unable to load child apps.", Toast.LENGTH_SHORT).show());
    }

    private void mergeMirroredAppLimits(List<DocumentSnapshot> mirroredLimitDocs) {
        for (DocumentSnapshot document : mirroredLimitDocs) {
            String packageName = document.getString("packageName");
            if (TextUtils.isEmpty(packageName)) {
                continue;
            }
            Long limitSeconds = document.getLong("limitSeconds");
            if (limitSeconds == null) {
                Long limitMinutes = document.getLong("limitMinutes");
                limitSeconds = limitMinutes == null ? 0L : limitMinutes * 60L;
            }
            savedLimitSecondsByPackage.put(packageName, limitSeconds);
        }
    }

    private Map<String, Long> readLimitSeconds(DocumentSnapshot document) {
        Map<String, Long> result = new HashMap<>();
        Map<String, Object> appLimitSeconds = document.get("appLimitSeconds") instanceof Map
                ? (Map<String, Object>) document.get("appLimitSeconds")
                : null;
        if (appLimitSeconds != null) {
            for (Map.Entry<String, Object> entry : appLimitSeconds.entrySet()) {
                Object value = entry.getValue();
                result.put(entry.getKey(), value instanceof Number ? ((Number) value).longValue() : 0L);
            }
            return result;
        }

        Map<String, Object> appLimitsMinutes = document.get("appLimits") instanceof Map
                ? (Map<String, Object>) document.get("appLimits")
                : null;
        if (appLimitsMinutes != null) {
            for (Map.Entry<String, Object> entry : appLimitsMinutes.entrySet()) {
                Object value = entry.getValue();
                long minutes = value instanceof Number ? ((Number) value).longValue() : 0L;
                result.put(entry.getKey(), minutes * 60L);
            }
        }
        return result;
    }

    private List<AppLimitInfo> buildFilteredAppLimitList(List<Map<String, Object>> topApps, Map<String, Long> existingLimits) {
        Map<String, AppLimitInfo> filteredApps = new LinkedHashMap<>();

        if (topApps != null) {
            for (Map<String, Object> item : topApps) {
                String packageName = asString(item.get("packageName"));
                String appName = DistractionAppFilter.defaultDisplayName(asString(item.get("appName")), packageName);
                if (!DistractionAppFilter.isHighDistractionApp(appName, packageName)) {
                    continue;
                }
                long limitSeconds = existingLimits.containsKey(packageName) ? existingLimits.get(packageName) : 0L;
                filteredApps.put(packageName, new AppLimitInfo(appName, packageName, limitSeconds, limitSeconds > 0L));
            }
        }

        for (Map.Entry<String, Long> entry : existingLimits.entrySet()) {
            String packageName = entry.getKey();
            if (!DistractionAppFilter.isHighDistractionApp(null, packageName)) {
                continue;
            }
            if (!filteredApps.containsKey(packageName)) {
                long limitSeconds = entry.getValue() == null ? 0L : entry.getValue();
                filteredApps.put(packageName, new AppLimitInfo(
                        DistractionAppFilter.defaultDisplayName(null, packageName),
                        packageName,
                        limitSeconds,
                        limitSeconds > 0L
                ));
            }
        }

        List<AppLimitInfo> items = new ArrayList<>(filteredApps.values());
        items.sort(Comparator.comparing(info -> safeName(info.appName)));
        return items;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private void showEmptyState(boolean show) {
        rvAppLimits.setVisibility(show ? View.GONE : View.VISIBLE);
        tvLimitEmptyState.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void saveLimitsToFirebase() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null || TextUtils.isEmpty(childDocumentId)) {
            return;
        }

        Map<String, Long> mergedLimitSeconds = new HashMap<>(savedLimitSecondsByPackage);
        Map<String, BlueprintSyncRepository.AppLimitPayload> mirroredPayload = new HashMap<>();
        for (AppLimitInfo info : appLimitList) {
            long seconds = info.isEnabled ? info.limitSeconds : 0L;
            mergedLimitSeconds.put(info.packageName, seconds);
            mirroredPayload.put(info.packageName, new BlueprintSyncRepository.AppLimitPayload(
                    DistractionAppFilter.defaultDisplayName(info.appName, info.packageName),
                    seconds,
                    info.isEnabled,
                    !savedLimitSecondsByPackage.containsKey(info.packageName)
            ));
        }

        Map<String, Long> mergedLimitMinutes = new HashMap<>();
        for (Map.Entry<String, Long> entry : mergedLimitSeconds.entrySet()) {
            long seconds = entry.getValue() == null ? 0L : entry.getValue();
            mergedLimitMinutes.put(entry.getKey(), seconds / 60L);
        }

        Map<String, Object> updateData = new HashMap<>();
        updateData.put("appLimitSeconds", mergedLimitSeconds);
        updateData.put("appLimits", mergedLimitMinutes);

        firestore.collection("users")
                .document(currentUser.getUid())
                .collection("children")
                .document(childDocumentId)
                .set(updateData, SetOptions.merge());

        firestore.collection("users")
                .document(childDocumentId)
                .set(updateData, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    BlueprintSyncRepository.syncAppTimeLimits(
                            firestore,
                            currentUser.getUid(),
                            childDocumentId,
                            childDisplayName,
                            mirroredPayload
                    );
                    savedLimitSecondsByPackage.clear();
                    savedLimitSecondsByPackage.putAll(mergedLimitSeconds);
                    Toast.makeText(this, "Limits saved successfully!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to save limits.", Toast.LENGTH_LONG).show());
    }

    private String displayChildName(String name) {
        return TextUtils.isEmpty(name) ? "Child Account" : name;
    }

    private String safeName(String value) {
        return TextUtils.isEmpty(value) ? "" : value.toLowerCase(Locale.US);
    }

    private int dpToPx(int dp) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics()
        ));
    }

    private static class ChildAccountRow {
        final String uid;
        final String name;
        final String email;

        ChildAccountRow(String uid, String name, String email) {
            this.uid = uid;
            this.name = name;
            this.email = email;
        }
    }

    private static class AppLimitInfo {
        final String appName;
        final String packageName;
        long limitSeconds;
        boolean isEnabled;

        AppLimitInfo(String appName, String packageName, long limitSeconds, boolean isEnabled) {
            this.appName = appName;
            this.packageName = packageName;
            this.limitSeconds = limitSeconds;
            this.isEnabled = isEnabled;
        }
    }

    private static class AppLimitAdapter extends RecyclerView.Adapter<AppLimitAdapter.ViewHolder> {
        private final List<AppLimitInfo> list;

        AppLimitAdapter(List<AppLimitInfo> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app_limit, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            AppLimitInfo info = list.get(position);
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

            holder.tvTimePicker.setText(formatDuration(info.limitSeconds));
            holder.switchEnableLimit.setOnCheckedChangeListener(null);
            holder.switchEnableLimit.setChecked(info.isEnabled);
            holder.tvTimePicker.setAlpha(info.isEnabled ? 1.0f : 0.4f);

            holder.switchEnableLimit.setOnCheckedChangeListener((buttonView, isChecked) -> {
                info.isEnabled = isChecked;
                holder.tvTimePicker.setAlpha(isChecked ? 1.0f : 0.4f);
                if (!isChecked) {
                    info.limitSeconds = 0L;
                    holder.tvTimePicker.setText(formatDuration(0L));
                }
            });

            holder.tvTimePicker.setOnClickListener(v -> {
                if (!info.isEnabled) {
                    return;
                }
                Context context = holder.itemView.getContext();
                LinearLayout layout = new LinearLayout(context);
                layout.setOrientation(LinearLayout.HORIZONTAL);
                layout.setGravity(Gravity.CENTER);
                layout.setPadding(20, 40, 20, 40);

                NumberPicker hourPicker = new NumberPicker(context);
                hourPicker.setMinValue(0);
                hourPicker.setMaxValue(23);
                hourPicker.setValue((int) (info.limitSeconds / 3600));

                NumberPicker minutePicker = new NumberPicker(context);
                minutePicker.setMinValue(0);
                minutePicker.setMaxValue(59);
                minutePicker.setValue((int) ((info.limitSeconds % 3600) / 60));

                NumberPicker secondPicker = new NumberPicker(context);
                secondPicker.setMinValue(0);
                secondPicker.setMaxValue(59);
                secondPicker.setValue((int) (info.limitSeconds % 60));

                layout.addView(hourPicker);
                layout.addView(buildLabel(context, " hrs "));
                layout.addView(minutePicker);
                layout.addView(buildLabel(context, " mins "));
                layout.addView(secondPicker);
                layout.addView(buildLabel(context, " secs "));

                new AlertDialog.Builder(context)
                        .setTitle("Set Limit: " + holder.tvAppName.getText())
                        .setView(layout)
                        .setPositiveButton("Save", (dialog, which) -> {
                            info.limitSeconds = (hourPicker.getValue() * 3600L)
                                    + (minutePicker.getValue() * 60L)
                                    + secondPicker.getValue();
                            holder.tvTimePicker.setText(formatDuration(info.limitSeconds));
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        private static TextView buildLabel(Context context, String text) {
            TextView textView = new TextView(context);
            textView.setText(text);
            textView.setTextSize(18);
            textView.setTextColor(Color.BLACK);
            return textView;
        }

        private static String formatDuration(long totalSeconds) {
            long hours = totalSeconds / 3600;
            long minutes = (totalSeconds % 3600) / 60;
            long seconds = totalSeconds % 60;
            return hours + "h " + minutes + "m " + seconds + "s";
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            final ImageView ivAppIcon;
            final TextView tvAppName;
            final TextView tvTimePicker;
            final SwitchCompat switchEnableLimit;

            ViewHolder(View itemView) {
                super(itemView);
                ivAppIcon = itemView.findViewById(R.id.ivAppIcon);
                tvAppName = itemView.findViewById(R.id.tvAppName);
                tvTimePicker = itemView.findViewById(R.id.tvTimePicker);
                switchEnableLimit = itemView.findViewById(R.id.switchEnableLimit);
            }
        }
    }
}
