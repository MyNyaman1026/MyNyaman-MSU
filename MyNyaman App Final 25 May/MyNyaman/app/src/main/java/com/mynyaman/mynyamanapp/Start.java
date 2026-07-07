package com.mynyaman.mynyamanapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class Start extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Link to the new start.xml layout
        setContentView(R.layout.start);

        // Find the button by its ID
        Button btnGetStarted = findViewById(R.id.btnGetStarted);

        // Set what happens when the button is clicked
        btnGetStarted.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Move from Start page to Login page
                Intent intent = new Intent(Start.this, Login.class);
                startActivity(intent);

                // Optional: Close this Start screen so the back button doesn't bring them here again
                finish();
            }
        });
    }
}