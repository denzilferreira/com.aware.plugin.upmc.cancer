package com.aware.plugin.upmc.dash;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

/**
 * Created by RaghuTeja on 8/6/17.
 */

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(Constants.TAG,"BootReceiver: onReceive: BootReceived");
        if(!isMyServiceRunning(MessageService.class, context)) {
            Intent messageService = new Intent(context, MessageService.class).setAction(Constants.ACTION_REBOOT_RUN);
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                context.startForegroundService(messageService);
            else
                context.startService(messageService);
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
