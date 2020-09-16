package com.robocore.mytemiservice.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.robocore.mytemiservice.R;
import com.robocore.mytemiservice.services.MainService;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    private void startMainService() {
        // use this to start and trigger a service
        Intent i = new Intent(MainActivity.this, MainService.class);
        // potentially add data to the intent
        i.putExtra("KEY1", "Value to be used by the service");
        startService(i);
    }

    public void startPressed(View view) {
        startMainService();
    }

    public void stopPressed(View view) {
        Intent i = new Intent(MainActivity.this, MainService.class);
        stopService(i);
    }
}