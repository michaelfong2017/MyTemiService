package com.robocore.mytemiservice.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.robocore.mytemiservice.R;
import com.robocore.secretcamera.AutoFitTextureView;
import com.robocore.secretcamera.CameraService;
import com.robocore.secretcamera.OnImageProcessedListener;


public class MessengerService extends Service implements OnImageProcessedListener {

    private static final String TAG = MessengerService.class.getSimpleName();

    /**
     * Command to the service to display a message
     */
    static final int MSG_SAY_HELLO = 1;


    // secretcamera api
    private static String cameraID = "1";
    private static boolean canSendImage = false;


    /**
     * Handler of incoming messages from clients.
     */
    class IncomingHandler extends Handler {
        private Context applicationContext;

        IncomingHandler(Context context) {
            applicationContext = context.getApplicationContext();
        }

        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "handleMessage()");
            Log.d(TAG, "msg.obj: "+msg.obj);
            canSendImage = true;
        }
    }

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    Messenger mMessenger;


    /**
     * When binding to the service, we return an interface to our messenger
     * for sending messages to the service.
     */
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind()");
        mMessenger = new Messenger(new IncomingHandler(this));
        return mMessenger.getBinder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand()");
        CameraService.getInstance().initialize(this);
        CameraService.getInstance().setCamera(cameraID);
        CameraService.getInstance().setOnImageProcessedListener(this);
        CameraService.getInstance().startSecretCamera();
        initialize();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");
        super.onDestroy();
        CameraService.getInstance().closeSecretCamera();
        CameraService.clear();
        release();
    }

    private static void initialize() {
        Log.d(TAG, "initialize()");

    }
    private static void release() {
        Log.d(TAG, "release()");
        canSendImage = false;
    }

    @Override
    public void onImageProcessed(byte[] imageBytes) {
        Log.d(TAG, "onImageProcessed()");
        if (canSendImage) {
            sendData(imageBytes);
        }
    }

    private void sendData(byte[] imageBytes) {
        Log.d(TAG, "sendData()");
        Intent intent = new Intent();
        intent.setAction("SECRET_CAMERA");
        intent.putExtra("image", imageBytes);
        sendBroadcast(intent);
    }
}
