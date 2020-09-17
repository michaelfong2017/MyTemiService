package com.robocore.mytemiservice.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

import com.robocore.objectfollowing.secretcamera.listeners.OnImageProcessedListener;
import com.robocore.objectfollowing.secretcamera.services.CameraService;

public class MessengerService extends Service implements OnImageProcessedListener {

    private static final String TAG = MessengerService.class.getSimpleName();

//    public OnImageProcessedListener onImageProcessedListener;

    /**
     * Command to the service to display a message
     */
    static final int MSG_SAY_HELLO = 1;


    // secretcamera api
    private CameraService cameraService;
    private static String cameraID = "1";


    /**
     * Handler of incoming messages from clients.
     */
    static class IncomingHandler extends Handler {
        private Context applicationContext;

        IncomingHandler(Context context) {
            applicationContext = context.getApplicationContext();
        }

        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "handleMessage()");
            Log.d(TAG, "msg.what: "+msg.what);
//            ((MessengerService)applicationContext).onImageProcessedListener = (OnImageProcessedListener)msg.obj;
            switch (msg.what) {
                case MSG_SAY_HELLO:
                    Log.d(TAG, "hello!");
                    break;
                default:
                    super.handleMessage(msg);
            }
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
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (cameraService != null) {
            cameraService.closeSecretCamera();
        }
    }

    @Override
    public void onImageProcessed(Bitmap imageBitmap) {
        Log.d(TAG, "onImageProcessed()");
//        if (onImageProcessedListener != null) {
//            onImageProcessedListener.onImageProcessed(imageBitmap);
//        }
    }
}
