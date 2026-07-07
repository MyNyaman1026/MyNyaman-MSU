package com.mynyaman.mynyamanapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

public class ChildAccountActionsActivity extends BaseActivity {

    public static final String EXTRA_CHILD_UID = "CHILD_UID";
    public static final String EXTRA_CHILD_NAME = "CHILD_NAME";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_account_actions);

        String childUid = getIntent().getStringExtra(EXTRA_CHILD_UID);
        String childName = getIntent().getStringExtra(EXTRA_CHILD_NAME);

        ImageView btnBack = findViewById(R.id.btnBack);
        TextView tvChildName = findViewById(R.id.tvChildName);
        CardView cardEditChild = findViewById(R.id.cardEditChild);
        CardView cardDeleteChild = findViewById(R.id.cardDeleteChild);

        tvChildName.setText(TextUtils.isEmpty(childName) ? "Child Account" : childName);

        btnBack.setOnClickListener(v -> finish());
        cardEditChild.setOnClickListener(v -> {
            Intent intent = new Intent(this, ProfileeditChild.class);
            intent.putExtra(ProfileeditChild.EXTRA_CHILD_UID, childUid);
            intent.putExtra(ProfileeditChild.EXTRA_CHILD_NAME, childName);
            startActivity(intent);
        });
        cardDeleteChild.setOnClickListener(v -> {
            Intent intent = new Intent(this, DeleteChildActivity.class);
            intent.putExtra(DeleteChildActivity.EXTRA_PRESELECT_CHILD_UID, childUid);
            startActivity(intent);
        });
    }
}
