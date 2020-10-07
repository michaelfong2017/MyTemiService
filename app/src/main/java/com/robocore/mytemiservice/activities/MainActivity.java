package com.robocore.mytemiservice.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;

import com.robocore.mytemiservice.R;
import com.robocore.mytemiservice.controllers.GridsController;
import com.robocore.mytemiservice.services.MessengerService;
import com.robocore.permissionutil.CheckPermissionsActivity;
import com.robocore.secretcamera.CameraService;
import com.robocore.secretcamera.OnImageProcessedListener;

public class MainActivity extends AppCompatActivity implements OnImageProcessedListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    /**
     * Check permissions
     */
    // PERMISSION_REQUEST_CODE can be arbitrary.
    private static final int PERMISSION_REQUEST_CODE = 102;
    private static boolean permit;

    /**
     * TableLayout
     */
    GridsController gridsController;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Permissions
        permit = false;
        Intent intent = new Intent(this, CheckPermissionsActivity.class);
        startActivityForResult(intent, PERMISSION_REQUEST_CODE);

        // Initialize
        initialize();

    }

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart()");
        super.onStart();

        CameraService.getInstance().initialize(this);
        CameraService.getInstance().setCamera("1");
        CameraService.getInstance().setOnImageProcessedListener(this);
        CameraService.getInstance().startPreviewCamera(findViewById(R.id.secretcamera_texture_view));

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

    private void initialize() {
        Log.d(TAG, "initialize()");
        gridsController = new GridsController(this);
        gridsController.initializeTableLayout();
    }

    private void release() {
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

    @Override
    public void onImageProcessed(byte[] imageBytes) {
        Log.d(TAG, "onImageProcessed()");
        Bitmap decodedByte = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        Log.d(TAG, ""+decodedByte);
    }
}