package com.bloatwareremoval.samsung;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Welcome screen for Samsung Galaxy S10e users
 * Shows device info and bloatware storage usage
 */
public class WelcomeActivity extends AppCompatActivity {

    private SamsungBloatwareManager mBloatwareManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        mBloatwareManager = new SamsungBloatwareManager(this);

        // Display device info
        TextView deviceName = findViewById(R.id.device_name);
        TextView androidVersion = findViewById(R.id.android_version);
        TextView storageUsed = findViewById(R.id.storage_used);

        deviceName.setText("Device: " + Build.MODEL);
        androidVersion.setText("Android: " + Build.VERSION.RELEASE);

        long bloatwareSizeMB = mBloatwareManager.getTotalBloatwareSize() / (1024 * 1024);
        storageUsed.setText("Bloatware: " + bloatwareSizeMB + " MB");

        // Start button
        Button startButton = findViewById(R.id.start_button);
        startButton.setOnClickListener(v -> {
            Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
            startActivity(intent);
        });
    }
}
