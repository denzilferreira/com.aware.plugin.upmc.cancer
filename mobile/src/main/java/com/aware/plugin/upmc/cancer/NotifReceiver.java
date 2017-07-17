package com.aware.plugin.upmc.cancer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

/**
 * Created by RaghuTeja on 7/16/17.
 */

public class NotifReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(Constants.TAG, "NotifRecevier:Received");
        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(Constants.NOTIFICATION_MESSAGE_INTENT_FILTER));
    }
}
