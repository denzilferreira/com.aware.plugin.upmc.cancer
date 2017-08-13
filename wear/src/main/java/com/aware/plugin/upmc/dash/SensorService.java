package com.aware.plugin.upmc.dash;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
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
    private boolean stepCountHasChanged = true;
    boolean FIRST_TIME = true;
    private int STEP_COUNT = 0;
    private int INIT_STEP_COUNT = 0;


    public boolean isALARM_1HR_FLAG() {
        return ALARM_1HR_FLAG;
    }

    public void initializeStepCount(int count) {
        INIT_STEP_COUNT = count;
    }

    public void calculateStepCount(int count) {
            if(count==INIT_STEP_COUNT){
                STEP_COUNT = 0;
            }
            else {
                STEP_COUNT = count - INIT_STEP_COUNT;
            }

    }

    public int getStepCount() {
        return STEP_COUNT;
    }

    public boolean hasStepCountChanged() {
        return stepCountHasChanged;
    }

    public void checkIfStepCountChanged(int count) {
        if(count==INIT_STEP_COUNT)
            stepCountHasChanged = false;
        else
            stepCountHasChanged = true;
    }


    public void setALARM_1HR_FLAG(boolean ALARM_1HR_FLAG) {
        this.ALARM_1HR_FLAG = ALARM_1HR_FLAG;
    }

    private boolean ALARM_1HR_FLAG = false;

    public boolean isALARM_2HR_FLAG() {
        return ALARM_2HR_FLAG;
    }

    public void setALARM_2HR_FLAG(boolean ALARM_2HR_FLAG) {
        this.ALARM_2HR_FLAG = ALARM_2HR_FLAG;
    }

    private boolean ALARM_2HR_FLAG = false;

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
            unregisterSensorListener();
        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(alarmLocalBroadcastReceiver);
        Log.d(Constants.TAG, "SensorService : onDestroy");
        stopForeground(true);
        Intent alarmIntent_1hr = new Intent(this, AlarmReceiver.class);
        alarmIntent_1hr.putExtra(Constants.ALARM_COMM, 0);
        myAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmPendingIntent_1hr = PendingIntent.getBroadcast(this, 667, alarmIntent_1hr,0);
        myAlarmManager.cancel(alarmPendingIntent_1hr);
        setALARM_1HR_FLAG(false);
        setALARM_2HR_FLAG(false);
        stopSelf();
    }

    public BroadcastReceiver alarmLocalBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(Constants.TAG, "alarmLocalBroadcastReceiver: " + intent.getIntExtra(Constants.ALARM_COMM,0));
            if(intent.getIntExtra(Constants.ALARM_COMM,0)==0) {
                setALARM_1HR_FLAG(true);
                registerSensorListener();

            }
            else if (intent.getIntExtra(Constants.ALARM_COMM,1)==1) {
                setALARM_2HR_FLAG(true);
                registerSensorListener();

            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(Constants.TAG, "SensorService : onCreate");
        sensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        sensorServiceNotifBuilder = new NotificationCompat.Builder(getApplicationContext())
                .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark_normal)
                .setContentTitle("UPMC Dash Wear Monitor")
                .setContentText("Monitoring")
                .setPriority(NotificationCompat.PRIORITY_HIGH);
        startForeground(3, sensorServiceNotifBuilder.build());
        myAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent alarmIntent_1hr = new Intent(this, AlarmReceiver.class);
        Intent alarmIntent_2hr = new Intent(this, AlarmReceiver.class);
        alarmIntent_1hr.putExtra(Constants.ALARM_COMM, 0);
        alarmIntent_2hr.putExtra(Constants.ALARM_COMM,1);
        alarmPendingIntent_1hr = PendingIntent.getBroadcast(this, 667, alarmIntent_1hr,0);
        //alarmPendingIntent_2hr = PendingIntent.getBroadcast(this,(int)System.currentTimeMillis(),alarmIntent_2hr,0);
        myAlarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(),60*1000, alarmPendingIntent_1hr);
        //myAlarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(),  120*1000, alarmPendingIntent_2hr);
        LocalBroadcastManager.getInstance(this).registerReceiver(alarmLocalBroadcastReceiver, new IntentFilter(Constants.ALARM_LOCAL_RECEIVER_INTENT_FILTER));
    }

    public void notifyUser(int sc_count) {
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if(sc_count < 100) {
            sensorServiceNotifBuilder = new NotificationCompat.Builder(getApplicationContext())
                    .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark_normal)
                    .setContentTitle("UPMC Dash Wear Monitor")
                    .setContentText("You have been inactive! " + sc_count)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setDefaults(Notification.DEFAULT_ALL);
            mNotificationManager.notify(3, sensorServiceNotifBuilder.build());
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            long[] vibrationPattern = {0, 500, 50, 300};
            //-1 - don't repeat
            final int indexInPatternToRepeat = 2;
            vibrator.vibrate(vibrationPattern, indexInPatternToRepeat);
            LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(Constants.SENSOR_COMM).putExtra(Constants.SENSOR_INTENT_COMM, Constants.NOTIFY_INACTIVITY));
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Log.d(Constants.TAG, "SensorService : onSensorChanged");
        if(sensorEvent.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            if(FIRST_TIME) {
            Log.d(Constants.TAG, "Step(firsttime): " + (int) sensorEvent.values[0]);
            initializeStepCount((int) sensorEvent.values[0]);
            FIRST_TIME = false;
            }
            else {
                int count = (int)sensorEvent.values[0];
                if(ALARM_1HR_FLAG) {
                    Log.d(Constants.TAG, "Step(1hr):  " + count);
                    unregisterSensorListener();
                    calculateStepCount(count);
                    initializeStepCount(count);
                    setALARM_1HR_FLAG(false);
                    Log.d(Constants.TAG, "Steps(taken): " + getStepCount());
                    notifyUser(getStepCount());

                }
//                else if(ALARM_2HR_FLAG) {
//                    Log.d(Constants.TAG, "Step(2hr):  " + count);
//                    unregisterSensorListener();
//                    if(hasStepCountChanged()) {
//                        calculateStepCount(count);
//                        initializeStepCount(count);
//                        setALARM_1HR_FLAG(false);
//                    }
//                    Log.d(Constants.TAG, "Steps(taken): " + getStepCount() );
//
//                }
            }
        }
    }

    private void registerSensorListener() {
        sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_FASTEST);
    }

    private void unregisterSensorListener() {
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }
}
