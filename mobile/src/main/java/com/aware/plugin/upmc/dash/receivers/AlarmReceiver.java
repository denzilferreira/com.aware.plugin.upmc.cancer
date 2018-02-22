package com.aware.plugin.upmc.dash.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.aware.plugin.upmc.dash.utils.Constants;

/**
 * Created by RaghuTeja on 9/5/17.
 */

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.hasExtra(Constants.ALARM_COMM)) {
            LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(Constants.SNOOZE_ALARM_INTENT_FILTER).putExtra(Constants.SNOOZE_ALARM_EXTRA_KEY, Constants.SNOOZE_ALARM_EXTRA));
            Log.d(Constants.TAG, "AlarmReceiver: Snooze End!");
        }
    }
}