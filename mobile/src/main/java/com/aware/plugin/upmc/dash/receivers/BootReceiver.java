package com.aware.plugin.upmc.dash.receivers;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.aware.plugin.upmc.dash.services.FitbitMessageService;
import com.aware.plugin.upmc.dash.utils.Constants;

/**
 * Created by RaghuTeja on 8/6/17.
 */

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(Constants.TAG,"BootReceiver:onReceiver:BootReceived");
        if(!isMyServiceRunning(FitbitMessageService.class, context)) {
            Intent msgServiceIntent = new Intent(context, FitbitMessageService.class).setAction(Constants.ACTION_REBOOT); // fitbit service
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(msgServiceIntent);
            }
            else {
                context.startService(msgServiceIntent);
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
