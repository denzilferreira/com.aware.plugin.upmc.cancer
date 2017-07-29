package com.aware.plugin.upmc.cancer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by RaghuTeja on 7/24/17.
 */

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(Constants.TAG, "AlarmReceiver:onReceive:Alarm Received!");
        if(intent.hasExtra(Constants.ALARM_COMM)) {
            Log.d(Constants.TAG, "AlarmReceiver:onReceive:Extra: " + intent.getStringExtra(Constants.ALARM_COMM));
        }
    }
}
