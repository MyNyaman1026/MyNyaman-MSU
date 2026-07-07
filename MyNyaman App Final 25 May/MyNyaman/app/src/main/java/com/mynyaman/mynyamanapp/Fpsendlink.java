package com.mynyaman.mynyamanapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

public class Fpsendlink extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fpsendlink);

        AppCompatButton btnResetEmail = findViewById(R.id.btnResetEmail);
        AppCompatButton btnResetPhone = findViewById(R.id.btnResetPhone);

        btnResetEmail.setOnClickListener(v -> {
            Intent intent = new Intent(Fpsendlink.this, Resetemail.class);
            startActivity(intent);
            finish();
        });

        btnResetPhone.setOnClickListener(v ->
                Toast.makeText(Fpsendlink.this, "Phone reset coming soon", Toast.LENGTH_SHORT).show()
        );
    }
}
