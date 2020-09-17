package com.robocore.mytemiservice.activities;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.robocore.mytemiservice.R;
import com.robocore.mytemiservice.services.MessengerService;
import com.robocore.objectfollowing.secretcamera.activities.CheckPermissionsActivity;

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
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Initialize();
        Intent intent = new Intent(this, CheckPermissionsActivity.class);
        startActivityForResult(intent, PERMISSION_REQUEST_CODE);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Release();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    permit = true;
                }
                break;
        }
    }

    private static void Initialize() {
        permit = false;
    }
    private static void Release() {

    }

    private void startMessengerService() {
        Log.d(TAG, "startMessengerService()");
        // use this to start and trigger a service
        Intent i = new Intent(MainActivity.this, MessengerService.class);
        // potentially add data to the intent
//        i.putExtra("KEY1", "Value to be used by the service");
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