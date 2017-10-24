package com.aware.plugin.upmc.dash;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

/**
 * Created by RaghuTeja on 7/24/17.
 */

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        //Log.d(Constants.TAG, "AlarmReceiver:onReceive:Alarm Received!");
        if(intent.hasExtra(Constants.ALARM_COMM)) {
            Log.d(Constants.TAG, "AlarmReceiver:onReceive:Extra: " + intent.getIntExtra(Constants.ALARM_COMM,-1));
            int alarm_type = intent.getIntExtra(Constants.ALARM_COMM,-1);
            LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(Constants.ALARM_LOCAL_RECEIVER_INTENT_FILTER).putExtra(Constants.ALARM_COMM, alarm_type));
        }
    }
}
