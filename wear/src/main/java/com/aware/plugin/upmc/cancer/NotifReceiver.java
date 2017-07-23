package com.aware.plugin.upmc.cancer;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by RaghuTeja on 7/22/17.
 */

public class NotifReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(Constants.TAG, "Let's stop this shit");

        if(isMyServiceRunning(MessageService.class, context)) {
            Intent msgService = new Intent(context,MessageService.class);
            context.stopService(msgService);
        }
        if(isMyServiceRunning(SensorService.class, context)) {
            Intent snsrService = new Intent(context,SensorService.class);
            context.stopService(snsrService);
        }

        Log.d(Constants.TAG, " stoped this shit");

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
