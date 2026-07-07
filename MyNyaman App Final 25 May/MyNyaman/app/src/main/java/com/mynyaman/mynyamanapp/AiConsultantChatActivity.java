package com.mynyaman.mynyamanapp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class AiConsultantChatActivity extends BaseActivity {

    public static final String EXTRA_OWNER_ROLE = "EXTRA_OWNER_ROLE";
    private static final String WELCOME_MESSAGE = "Welcome to MyNyaman, what can i help you today";

    public static Intent createIntent(Context context, String ownerRole) {
        Intent intent = new Intent(context, AiConsultantChatActivity.class);
        intent.putExtra(EXTRA_OWNER_ROLE, RoleUtils.normalizeRole(ownerRole));
        return intent;
    }

    private final List<ConsultantUiMessage> messages = new ArrayList<>();
    private final ConsultantMessageAdapter adapter = new ConsultantMessageAdapter(messages);
    private FirebaseFirestore firestore;
    private FirebaseUser currentUser;
    private String ownerRole;
    private String sessionId;
    private RecyclerView rvMessages;
    private EditText etMessage;
    private TextView btnSend;
    private ProgressBar progressBar;
    private boolean isAwaitingResponse;
    private boolean isSessionClosed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_consultant_chat);

        firestore = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, Login.class));
            finish();
            return;
        }

        ownerRole = RoleUtils.normalizeRole(getIntent().getStringExtra(EXTRA_OWNER_ROLE));
        if (TextUtils.isEmpty(ownerRole)) {
            ownerRole = RoleUtils.ROLE_PARENT;
        }
        sessionId = UUID.randomUUID().toString();

        ImageView btnBack = findViewById(R.id.btnBack);
        TextView tvSubtitle = findViewById(R.id.tvSubtitle);
        rvMessages = findViewById(R.id.rvMessages);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        progressBar = findViewById(R.id.progressBar);

        tvSubtitle.setText(RoleUtils.ROLE_CHILD.equals(ownerRole)
                ? "Talk to AI about your digital wellbeing and screen habits."
                : "Ask for family guidance on screen time, wellbeing, and cyberbullying.");

        rvMessages.setLayoutManager(new LinearLayoutManager(this));
        rvMessages.setAdapter(adapter);
        appendMessage("assistant", WELCOME_MESSAGE);

        btnBack.setOnClickListener(v -> promptToCloseSession());
        btnSend.setOnClickListener(v -> sendCurrentMessage());
        etMessage.setOnEditorActionListener((textView, actionId, keyEvent) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND || actionId == EditorInfo.IME_ACTION_DONE) {
                sendCurrentMessage();
                return true;
            }
            if (keyEvent != null && keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                sendCurrentMessage();
                return true;
            }
            return false;
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                promptToCloseSession();
            }
        });
    }

    private void sendCurrentMessage() {
        if (isAwaitingResponse) {
            return;
        }

        String text = etMessage.getText() == null ? "" : etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(text)) {
            return;
        }

        appendMessage("user", text);
        etMessage.setText("");
        setLoading(true);

        AiConsultantApi.sendMessage(toRepositoryMessages(), new AiConsultantApi.Callback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    setLoading(false);
                    appendMessage("assistant", response);
                });
            }

            @Override
            public void onFailure(String message) {
                runOnUiThread(() -> {
                    setLoading(false);
                    String fallback = TextUtils.isEmpty(message)
                            ? "I could not respond right now. Please try again shortly."
                            : message;
                    appendMessage("assistant", fallback);
                    Toast.makeText(AiConsultantChatActivity.this, fallback, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void setLoading(boolean loading) {
        isAwaitingResponse = loading;
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        etMessage.setEnabled(!loading);
        btnSend.setEnabled(!loading);
        btnSend.setAlpha(loading ? 0.6f : 1f);
    }

    private void appendMessage(String role, String content) {
        messages.add(new ConsultantUiMessage(role, content, new Date()));
        adapter.notifyItemInserted(messages.size() - 1);
        rvMessages.scrollToPosition(messages.size() - 1);
    }

    private List<BlueprintSyncRepository.ConsultantMessageRecord> toRepositoryMessages() {
        List<BlueprintSyncRepository.ConsultantMessageRecord> records = new ArrayList<>();
        for (ConsultantUiMessage message : messages) {
            records.add(new BlueprintSyncRepository.ConsultantMessageRecord(
                    message.role,
                    message.content,
                    new Timestamp(message.createdAt)
            ));
        }
        return records;
    }

    private void promptToCloseSession() {
        if (isSessionClosed) {
            finish();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("End session?")
                .setMessage("Leaving this chat will terminate the current session and save it to Session History.")
                .setNegativeButton("Stay", null)
                .setPositiveButton("End Session", (dialog, which) -> saveSessionAndFinish())
                .show();
    }

    private void saveSessionAndFinish() {
        if (isSessionClosed) {
            finish();
            return;
        }
        isSessionClosed = true;

        String title = "AI Consultant Session";
        for (ConsultantUiMessage message : messages) {
            if ("user".equals(message.role) && !TextUtils.isEmpty(message.content)) {
                title = message.content.length() > 48
                        ? message.content.substring(0, 48).trim() + "..."
                        : message.content;
                break;
            }
        }

        BlueprintSyncRepository.saveAiConsultantSession(
                firestore,
                currentUser.getUid(),
                ownerRole,
                sessionId,
                title,
                toRepositoryMessages()
        );
        Toast.makeText(this, "Session saved to history.", Toast.LENGTH_SHORT).show();
        finish();
    }

    private static class ConsultantUiMessage {
        final String role;
        final String content;
        final Date createdAt;

        ConsultantUiMessage(String role, String content, Date createdAt) {
            this.role = role;
            this.content = content;
            this.createdAt = createdAt;
        }
    }

    private static class ConsultantMessageAdapter extends RecyclerView.Adapter<ConsultantMessageAdapter.ViewHolder> {

        private final List<ConsultantUiMessage> messages;
        private final SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

        ConsultantMessageAdapter(List<ConsultantUiMessage> messages) {
            this.messages = messages;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ai_message, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ConsultantUiMessage message = messages.get(position);
            boolean isUser = "user".equals(message.role);

            FrameLayout.LayoutParams bubbleParams = (FrameLayout.LayoutParams) holder.cardBubble.getLayoutParams();
            bubbleParams.gravity = isUser ? android.view.Gravity.END : android.view.Gravity.START;
            holder.cardBubble.setLayoutParams(bubbleParams);
            holder.cardBubble.setCardBackgroundColor(isUser ? 0xFF2A8CFF : 0xFFF4F7FB);
            holder.tvMessage.setTextColor(isUser ? 0xFFFFFFFF : 0xFF1E1E1E);
            holder.tvTimestamp.setTextColor(isUser ? 0xFFDCEBFF : 0xFF6E6E6E);
            holder.tvMessage.setText(message.content);
            holder.tvTimestamp.setText(timeFormat.format(message.createdAt));
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            final CardView cardBubble;
            final TextView tvMessage;
            final TextView tvTimestamp;

            ViewHolder(View itemView) {
                super(itemView);
                cardBubble = itemView.findViewById(R.id.cardBubble);
                tvMessage = itemView.findViewById(R.id.tvMessage);
                tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            }
        }
    }
}
