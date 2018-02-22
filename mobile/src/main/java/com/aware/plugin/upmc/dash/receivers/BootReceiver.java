package com.aware.plugin.upmc.dash.receivers;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.aware.plugin.upmc.dash.utils.Constants;
import com.aware.plugin.upmc.dash.services.MessageService;

/**
 * Created by RaghuTeja on 8/6/17.
 */

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(Constants.TAG,"BootReceived");
        if(!isMyServiceRunning(MessageService.class, context)) {
            context.startService(new Intent(context, MessageService.class));
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
