package com.aware.plugin.upmc.cancer;

import android.app.ActivityManager;
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
        Log.d(Constants.TAG, "NotifRecevier:onReceive");
        if(intent.hasExtra(Constants.COMM_KEY_NOTIF)) {
            Log.d(Constants.TAG, "NotifRecevier:onReceive:hasExtras");
            if(intent.getStringExtra(Constants.COMM_KEY_NOTIF).equals("STOP")) {
                Log.d(Constants.TAG, "NotifReceiver:OnReceive:STOP");
                LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(Constants.NOTIFICATION_MESSAGE_INTENT_FILTER).putExtra(Constants.COMM_KEY_UPMC,"KILL"));
                LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(Constants.NOTIFICATION_MESSAGE_INTENT_FILTER).putExtra(Constants.COMM_KEY_MSGSERVICE,"KILL"));
            }
            else if(intent.getStringExtra(Constants.COMM_KEY_NOTIF).equals("START SESSION")) {
                Log.d(Constants.TAG, "NotifReceiver:OnReceive:START");
                // start step count from notification
                LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(Constants.NOTIFICATION_MESSAGE_INTENT_FILTER).putExtra(Constants.COMM_KEY_MSGSERVICE, Constants.START_SC));

            }
            else if(intent.getStringExtra(Constants.COMM_KEY_NOTIF).equals("STOP SESSION")) {
                Log.d(Constants.TAG, "NotifReceiver:OnReceive:STOP SESSION");
                // stop step count from notification
                LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(Constants.NOTIFICATION_MESSAGE_INTENT_FILTER).putExtra(Constants.COMM_KEY_MSGSERVICE, Constants.STOP_SC));

            }
            else if(intent.getStringExtra(Constants.COMM_KEY_NOTIF).equals("KILL_REQUEST")) {
                Log.d(Constants.TAG,"NotifReceiver:OnReceive:KILL_REQUEST");
                if(isMyServiceRunning(MessageService.class, context)) {
                    Intent msgServiceIntent = new Intent(context, MessageService.class);
                    context.stopService(msgServiceIntent);
                }
            }
        }

    }


    private boolean isMyServiceRunning(Class<?> serviceClass, Context context) {

        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
