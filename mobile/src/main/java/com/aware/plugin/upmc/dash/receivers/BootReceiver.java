package com.aware.plugin.upmc.dash.receivers;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.aware.Aware;
import com.aware.plugin.upmc.dash.services.FitbitMessageService;
import com.aware.plugin.upmc.dash.settings.Settings;
import com.aware.plugin.upmc.dash.utils.Constants;
import com.aware.plugin.upmc.dash.utils.Scheduler;

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
            setSchedules(context);
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


    private void setSchedules(Context context) {
        Aware.setSetting(context, Settings.STATUS_PLUGIN_UPMC_CANCER, true);
        int morning_hour = Integer.parseInt(Aware.getSetting(context,
                Settings.PLUGIN_UPMC_CANCER_MORNING_HOUR));
        int morning_minute = Integer.parseInt(
                Aware.getSetting(context, Settings.PLUGIN_UPMC_CANCER_MORNING_MINUTE));
        Scheduler.setSurveySchedule(context, morning_hour, morning_minute);
        int evening_hour = Integer.parseInt(
                Aware.getSetting(context, Settings.PLUGIN_UPMC_CANCER_NIGHT_HOUR));
        int evening_minute =
                Integer.parseInt(Aware.getSetting(context, Settings.PLUGIN_UPMC_CANCER_NIGHT_MINUTE));
        Scheduler.setSyncSchedule(context, evening_hour, evening_minute);

    }
}
