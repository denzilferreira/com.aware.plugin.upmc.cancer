package com.aware.plugin.upmc.dash.receivers;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import com.aware.plugin.upmc.dash.services.FitbitMessageService;
import com.aware.plugin.upmc.dash.utils.Constants;
import com.aware.plugin.upmc.dash.services.MessageService;

/**
 * Created by RaghuTeja on 8/6/17.
 */

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(Constants.TAG,"BootReceived");
        Class<?> service = readDeviceType(context).equals(Constants.DEVICE_TYPE_FITBIT) ? FitbitMessageService.class:MessageService.class;
        if(!isMyServiceRunning(service, context)) {
            Intent msgServiceIntent = new Intent(context, service).setAction(Constants.ACTION_REBOOT); // fitbit service
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(msgServiceIntent);
            }
            else {
                context.startService(msgServiceIntent);
            }
        }

    }




    public String readDeviceType(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        String deviceType = sharedPref.getString(Constants.PREFERENCES_KEY_DEVICE_TYPE, Constants.PREFERENCES_DEFAULT_DEVICE_TYPE);
        if (deviceType.equals(Constants.PREFERENCES_DEFAULT_DEVICE_TYPE))
            Log.d(Constants.TAG, "OnboardingActivity:writeDeviceType: " + deviceType);
        return deviceType;
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
