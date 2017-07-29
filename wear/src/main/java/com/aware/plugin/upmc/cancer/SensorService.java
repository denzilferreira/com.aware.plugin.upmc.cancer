package com.aware.plugin.upmc.cancer;

import android.app.AlarmManager;
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
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

/**
 * Created by RaghuTeja on 6/24/17.
 */

public class SensorService extends Service implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor stepSensor;
    private AlarmManager myAlarmManager;
    private PendingIntent alarmPendingIntent_1hr;
    private PendingIntent alarmPendingIntent_2hr;

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
                .setContentText("Monitoring")
                .setPriority(NotificationCompat.PRIORITY_HIGH);
        startForeground(3, sensorServiceNotifBuilder.build());
        myAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent alarmIntent_1hr = new Intent(this, AlarmReceiver.class);
        Intent alarmIntent_2hr = new Intent(this, AlarmReceiver.class);
        alarmIntent_1hr.putExtra(Constants.ALARM_COMM, "1hr");
        alarmIntent_2hr.putExtra(Constants.ALARM_COMM,"2hr");

        alarmPendingIntent_1hr = PendingIntent.getBroadcast(this, (int)System.currentTimeMillis(), alarmIntent_1hr,0);
        //alarmPendingIntent_2hr = PendingIntent.getBroadcast(this,(int)System.currentTimeMillis(),alarmIntent_2hr,0);
        myAlarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(),60*1000, alarmPendingIntent_1hr);
        //myAlarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(),  70*1000, alarmPendingIntent_2hr);



    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Log.d(Constants.TAG, "SensorService : onSensorChanged");
        if(sensorEvent.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            Log.d(Constants.TAG, "Step:  " + (int)sensorEvent.values[0]);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }
}
