package com.mynyaman.mynyamanapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;

public class DeleteChildActivity extends BaseActivity {

    public static final String EXTRA_PRESELECT_CHILD_UID = "PRESELECT_CHILD_UID";

    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private final List<ChildRow> childRows = new ArrayList<>();
    private DeleteChildAdapter adapter;

    private RecyclerView rvDeleteChildren;
    private TextView tvDeleteEmptyState;
    private AppCompatButton btnConfirmDelete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delete_child);

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        ImageView btnBack = findViewById(R.id.btnBack);
        rvDeleteChildren = findViewById(R.id.rvDeleteChildren);
        tvDeleteEmptyState = findViewById(R.id.tvDeleteEmptyState);
        btnConfirmDelete = findViewById(R.id.btnConfirmDelete);

        rvDeleteChildren.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DeleteChildAdapter(childRows);
        rvDeleteChildren.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());
        btnConfirmDelete.setOnClickListener(v -> confirmDeletion());

        loadChildren();
    }

    private void loadChildren() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, Login.class));
            finish();
            return;
        }

        String preselectedChildUid = getIntent().getStringExtra(EXTRA_PRESELECT_CHILD_UID);

        firestore.collection("users")
                .document(currentUser.getUid())
                .collection("children")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    childRows.clear();
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        ChildRow row = new ChildRow(
                                document.getId(),
                                document.getString("name"),
                                document.getString("email")
                        );
                        row.isSelected = !TextUtils.isEmpty(preselectedChildUid)
                                && preselectedChildUid.equals(document.getId());
                        childRows.add(row);
                    }
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Unable to load child accounts.", Toast.LENGTH_SHORT).show());
    }

    private void updateEmptyState() {
        boolean isEmpty = childRows.isEmpty();
        rvDeleteChildren.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        tvDeleteEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        btnConfirmDelete.setEnabled(!isEmpty);
    }

    private void confirmDeletion() {
        List<ChildRow> selectedRows = new ArrayList<>();
        for (ChildRow row : childRows) {
            if (row.isSelected) {
                selectedRows.add(row);
            }
        }

        if (selectedRows.isEmpty()) {
            Toast.makeText(this, "Select at least one child account.", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Delete child account")
                .setMessage("This will remove the selected child accounts from the parent dashboard and request permanent deletion on the child device. Continue?")
                .setPositiveButton("Delete", (dialog, which) -> deleteSelectedChildren(selectedRows))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteSelectedChildren(List<ChildRow> selectedRows) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, Login.class));
            finish();
            return;
        }

        btnConfirmDelete.setEnabled(false);
        WriteBatch batch = firestore.batch();
        for (ChildRow row : selectedRows) {
            batch.delete(
                    firestore.collection("users")
                            .document(currentUser.getUid())
                            .collection("children")
                            .document(row.uid)
            );
            batch.delete(
                    firestore.collection("parent_child_relationships")
                            .document(currentUser.getUid() + "_" + row.uid)
            );
            batch.set(
                    firestore.collection("users").document(row.uid),
                    new java.util.HashMap<String, Object>() {{
                        put("pendingDeletion", true);
                        put("accountStatus", "PENDING_DELETION");
                        put("deletionRequestedAt", FieldValue.serverTimestamp());
                    }},
                    SetOptions.merge()
            );
        }

        batch.commit()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Child deletion requested.", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnConfirmDelete.setEnabled(true);
                    Toast.makeText(this, "Failed to delete child accounts.", Toast.LENGTH_LONG).show();
                });
    }

    private static class ChildRow {
        final String uid;
        final String name;
        final String email;
        boolean isSelected;

        ChildRow(String uid, String name, String email) {
            this.uid = uid;
            this.name = name;
            this.email = email;
        }
    }

    private static class DeleteChildAdapter extends RecyclerView.Adapter<DeleteChildAdapter.ViewHolder> {
        private final List<ChildRow> childRows;

        DeleteChildAdapter(List<ChildRow> childRows) {
            this.childRows = childRows;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_delete_child, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ChildRow row = childRows.get(position);
            holder.tvDeleteChildName.setText(!TextUtils.isEmpty(row.name) ? row.name : "Child Account");
            holder.tvDeleteChildEmail.setText(!TextUtils.isEmpty(row.email) ? row.email : "No email saved");
            holder.cbDeleteChild.setOnCheckedChangeListener(null);
            holder.cbDeleteChild.setChecked(row.isSelected);
            holder.itemView.setOnClickListener(v -> {
                row.isSelected = !row.isSelected;
                holder.cbDeleteChild.setChecked(row.isSelected);
            });
            holder.cbDeleteChild.setOnCheckedChangeListener((buttonView, isChecked) -> row.isSelected = isChecked);
        }

        @Override
        public int getItemCount() {
            return childRows.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            final CheckBox cbDeleteChild;
            final TextView tvDeleteChildName;
            final TextView tvDeleteChildEmail;

            ViewHolder(View itemView) {
                super(itemView);
                cbDeleteChild = itemView.findViewById(R.id.cbDeleteChild);
                tvDeleteChildName = itemView.findViewById(R.id.tvDeleteChildName);
                tvDeleteChildEmail = itemView.findViewById(R.id.tvDeleteChildEmail);
            }
        }
    }
}
