package com.aware.plugin.upmc.dash.receivers;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.aware.plugin.upmc.dash.services.MessageService;
import com.aware.plugin.upmc.dash.services.SensorService;
import com.aware.plugin.upmc.dash.utils.Constants;

public class ShutdownReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(Constants.TAG,"ShutdownReceiver: onReceive: Shutdown");
        if(isMyServiceRunning(MessageService.class, context)) {
            Intent messageServiceIntent = new Intent(context, MessageService.class).setAction(Constants.ACTION_STOP_SELF);
            Intent sensorService = new Intent(context, SensorService.class).setAction(Constants.ACTION_STOP_SELF);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(messageServiceIntent);
                context.startForegroundService(sensorService);
            }
            else {
                context.startService(messageServiceIntent);
                context.startService(sensorService);
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
