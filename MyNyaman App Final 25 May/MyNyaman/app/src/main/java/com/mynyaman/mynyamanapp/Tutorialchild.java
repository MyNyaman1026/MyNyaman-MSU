package com.mynyaman.mynyamanapp;

import android.os.Bundle;
import android.widget.ImageView;

public class Tutorialchild extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tutorialchild);

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
    }
}
