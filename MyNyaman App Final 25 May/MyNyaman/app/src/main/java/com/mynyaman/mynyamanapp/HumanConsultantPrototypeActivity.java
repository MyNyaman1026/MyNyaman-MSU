package com.mynyaman.mynyamanapp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

public class HumanConsultantPrototypeActivity extends BaseActivity {

    public static final String EXTRA_OWNER_ROLE = "EXTRA_OWNER_ROLE";

    public static Intent createIntent(Context context, String ownerRole) {
        Intent intent = new Intent(context, HumanConsultantPrototypeActivity.class);
        intent.putExtra(EXTRA_OWNER_ROLE, RoleUtils.normalizeRole(ownerRole));
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_human_consultant_prototype);

        String ownerRole = RoleUtils.normalizeRole(getIntent().getStringExtra(EXTRA_OWNER_ROLE));
        if (ownerRole == null || ownerRole.isEmpty()) {
            ownerRole = RoleUtils.ROLE_PARENT;
        }

        ImageView btnBack = findViewById(R.id.btnBack);
        TextView tvSubtitle = findViewById(R.id.tvSubtitle);
        TextView tvBody = findViewById(R.id.tvBody);

        btnBack.setOnClickListener(v -> finish());
        tvSubtitle.setText(RoleUtils.ROLE_CHILD.equals(ownerRole)
                ? "Human consultant chat is planned for a future child-safe release."
                : "Human consultant chat is prepared here as the next support channel.");
        tvBody.setText("This prototype keeps the MyNyaman consultant entry flow ready while the live human support backend is still being prepared.");
    }
}
