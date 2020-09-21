package com.robocore.mytemiservice.activities;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.robocore.mytemiservice.R;
import com.robocore.mytemiservice.services.MessengerService;
import com.robocore.permissionutil.CheckPermissionsActivity;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    /**
     * Check permissions
     */
    // PERMISSION_REQUEST_CODE can be arbitrary.
    private static final int PERMISSION_REQUEST_CODE = 102;
    private static boolean permit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initialize();
        Intent intent = new Intent(this, CheckPermissionsActivity.class);
        startActivityForResult(intent, PERMISSION_REQUEST_CODE);
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart()");
        super.onStart();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy()");
        super.onDestroy();
        release();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        Log.d(TAG, "onActivityResult()");
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    permit = true;
                }
                break;
        }
    }

    private static void initialize() {
        Log.d(TAG, "initialize()");
        permit = false;
    }
    private static void release() {
        Log.d(TAG, "release()");

    }

    private void startMessengerService() {
        Log.d(TAG, "startMessengerService()");
        // use this to start and trigger a service
        Intent i = new Intent(MainActivity.this, MessengerService.class);
        startService(i);
    }

    public void startPressed(View view) {
        Log.d(TAG, "startPressed()");
        if (permit) {
            Log.d(TAG, "permit==true");
            startMessengerService();
        }
    }

    public void stopPressed(View view) {
        Log.d(TAG, "stopPressed()");
        Intent i = new Intent(MainActivity.this, MessengerService.class);
        stopService(i);
    }
}