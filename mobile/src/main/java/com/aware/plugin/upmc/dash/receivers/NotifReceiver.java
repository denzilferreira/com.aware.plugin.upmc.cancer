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

public class NotifReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction()!=null) {
            Log.d(Constants.TAG, "NotifRecevier: Received: " + intent.getAction());
            LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(Constants.NOTIF_COMM).putExtra(Constants.NOTIF_KEY, intent.getAction()));
        }

    }
}
