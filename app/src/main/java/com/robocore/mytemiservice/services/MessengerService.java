package com.robocore.mytemiservice.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

import com.robocore.objectfollowing.secretcamera.listeners.OnImageProcessedListener;
import com.robocore.objectfollowing.secretcamera.services.CameraService;

import java.io.ByteArrayOutputStream;

public class MessengerService extends Service implements OnImageProcessedListener {

    private static final String TAG = MessengerService.class.getSimpleName();

    /**
     * Command to the service to display a message
     */
    static final int MSG_SAY_HELLO = 1;


    // secretcamera api
    private CameraService cameraService;
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
        cameraService = new CameraService(this);
        cameraService.setCamera(cameraID);
        cameraService.setOnImageProcessedListener(this);
        cameraService.startSecretCamera();
        initialize();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");
        super.onDestroy();
        if (cameraService != null) {
            cameraService.closeSecretCamera();
        }
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
    public void onImageProcessed(Bitmap imageBitmap) {
        Log.d(TAG, "onImageProcessed()");
        if (canSendImage) {
            sendData(imageBitmap);
        }
    }

    private void sendData(Bitmap imageBitmap) {
        Log.d(TAG, "sendData()");
        //Convert to byte array
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();

        Intent intent = new Intent();
        intent.setAction("SECRET_CAMERA");
        intent.putExtra("image", byteArray);
        sendBroadcast(intent);
    }
}
