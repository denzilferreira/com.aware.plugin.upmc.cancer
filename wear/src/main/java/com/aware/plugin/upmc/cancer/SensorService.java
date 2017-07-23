package com.aware.plugin.upmc.cancer;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

/**
 * Created by RaghuTeja on 6/24/17.
 */

public class SensorService extends Service implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor stepSensor;
    public static final String PREFS_SC1_FILE = "SC_1_HRS";
    public static final String PREFS_SC2_FILE = "SC_2_HRS";
    private boolean firstUse = true;

    private NotificationCompat.Builder sensorServiceNotifBuilder;
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(sensorManager!=null) {
            sensorManager.unregisterListener(this);
        }
        Log.d(Constants.TAG, "SensorService : onDestroy");
        stopForeground(true);
        stopSelf();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(Constants.TAG, "SensorService : onCreate");
        sensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_FASTEST);

        sensorServiceNotifBuilder = new NotificationCompat.Builder(getApplicationContext())
                .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark_normal)
                .setContentTitle("UPMC Dash Wear Monitor")
                .setContentText("Logging...")
                .setPriority(NotificationCompat.PRIORITY_HIGH);
        //.setContentIntent(dashPendingIntent);






        startForeground(3, sensorServiceNotifBuilder.build());


    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Log.d(Constants.TAG, "SensorService : onSensorChanged");
        if(sensorEvent.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            Log.d(Constants.TAG, "Step:  " + (int)sensorEvent.values[0]);
            if(firstUse) {
                SharedPreferences sharedPreferences1 = getSharedPreferences(PREFS_SC1_FILE, 0);
                SharedPreferences sharedPreferences2 = getSharedPreferences(PREFS_SC2_FILE, 0);
                SharedPreferences.Editor editor1 = sharedPreferences1.edit();
                SharedPreferences.Editor editor2 = sharedPreferences2.edit();
                editor1.putInt("count",(int)sensorEvent.values[0] );
                editor2.putInt("count", (int)sensorEvent.values[0]);
                editor1.commit();
                editor2.commit();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }
}
