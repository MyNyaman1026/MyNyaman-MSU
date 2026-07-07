package com.mynyaman.mynyamanapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

public class ConsultantHubActivity extends BaseActivity {

    public static final String EXTRA_OWNER_ROLE = "EXTRA_OWNER_ROLE";

    public static Intent createIntent(android.content.Context context, String ownerRole) {
        Intent intent = new Intent(context, ConsultantHubActivity.class);
        intent.putExtra(EXTRA_OWNER_ROLE, RoleUtils.normalizeRole(ownerRole));
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_consultant_hub);

        String ownerRole = RoleUtils.normalizeRole(getIntent().getStringExtra(EXTRA_OWNER_ROLE));
        if (ownerRole == null || ownerRole.isEmpty()) {
            ownerRole = RoleUtils.ROLE_PARENT;
        }

        ImageView btnBack = findViewById(R.id.btnBack);
        TextView btnHistory = findViewById(R.id.btnSessionHistory);
        TextView tvSubtitle = findViewById(R.id.tvSubtitle);
        CardView cardAiConsultant = findViewById(R.id.cardAiConsultant);
        CardView cardHumanConsultant = findViewById(R.id.cardHumanConsultant);

        tvSubtitle.setText(RoleUtils.ROLE_CHILD.equals(ownerRole)
                ? "Safe support for screen time, digital wellbeing, and cyberbullying questions."
                : "Choose the support path you want for your family guidance session.");

        String finalOwnerRole = ownerRole;
        btnBack.setOnClickListener(v -> finish());
        btnHistory.setOnClickListener(v -> startActivity(ConsultantSessionHistoryActivity.createIntent(this, finalOwnerRole)));
        cardAiConsultant.setOnClickListener(v -> startActivity(AiConsultantChatActivity.createIntent(this, finalOwnerRole)));
        cardHumanConsultant.setOnClickListener(v -> startActivity(HumanConsultantPrototypeActivity.createIntent(this, finalOwnerRole)));
    }
}
