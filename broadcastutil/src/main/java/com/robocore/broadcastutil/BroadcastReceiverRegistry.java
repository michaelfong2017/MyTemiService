package com.robocore.broadcastutil;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class BroadcastReceiverRegistry {
    private static final String TAG = BroadcastReceiverRegistry.class.getSimpleName();

    private static BroadcastReceiverRegistry broadcastReceiverRegistry;

    /**
     * All broadcast receivers
     */
    private static List<BroadcastReceiver> allBroadcastReceivers = new ArrayList<>();

    // Prevent clients from using the constructor
    private BroadcastReceiverRegistry() {
    }
    //Control the accessible (allowed) instances
    public static BroadcastReceiverRegistry getInstance() {
        Log.d(TAG, "getInstance()");
        if (broadcastReceiverRegistry == null) {
            broadcastReceiverRegistry = new BroadcastReceiverRegistry();
        }
        return broadcastReceiverRegistry;
    }

    public static void unregisterAllBroadcastReceivers(Context context) {
        Log.d(TAG, "unregisterAllBroadcastReceivers()");
        for (BroadcastReceiver broadcastReceiver : allBroadcastReceivers) {
            context.unregisterReceiver(broadcastReceiver);
        }

        allBroadcastReceivers.clear();
    }

    public static void registerBroadcastReceiver(Context context, Messenger mService, BroadcastReceiver broadcastReceiver) {
        Log.d(TAG, "registerBroadcastReceiver()");
        String packageName = context.getPackageName();
        Log.d(TAG, "packageName: " + packageName);
        Log.d(TAG, "broadcastReceiver: " + broadcastReceiver);
        String actionName = "";
        if (broadcastReceiver instanceof SecretCameraBroadcastReceiver) {
            actionName = "SECRET_CAMERA";
        }
        else {
            return;
        }
        Log.d(TAG, "actionName: " + actionName);

        /**
         * Registration
         */
        IntentFilter filter = new IntentFilter(packageName + "." + actionName);
        filter.addAction(actionName);
        context.registerReceiver(broadcastReceiver, filter);

        allBroadcastReceivers.add(broadcastReceiver);
        /**
         * Notify MyTemiService about the registration
         */
        Bundle bundle = new Bundle();
        bundle.putString("action", packageName + "." + actionName);
        Message msg = Message.obtain();
        msg.obj = bundle;
        try {
            mService.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public static void unregisterBroadcastReceiver(Context context, BroadcastReceiver broadcastReceiver) {
        Log.d(TAG, "unregisterBroadcastReceiver()");
        String packageName = context.getPackageName();
        Log.d(TAG, "packageName: " + packageName);
        Log.d(TAG, "broadcastReceiver: " + broadcastReceiver);
        context.unregisterReceiver(broadcastReceiver);

        allBroadcastReceivers.remove(broadcastReceiver);
    }
}
