package com.aware.plugin.upmc.dash;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.IOException;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

/**
 * Created by RaghuTeja on 6/24/17.
 */

public class SensorService extends Service implements SensorEventListener {
    public BroadcastReceiver feedbackLocalBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.hasExtra(Constants.SENSOR_EXTRA_KEY)) {
                if(intent.getStringExtra(Constants.SENSOR_EXTRA_KEY).equals(Constants.OK_ACTION)) {
                    Log.d(Constants.TAG, "SensorService:feedbackLocalBroadcastReceiver:okaction");
                    //setFeedbackEnabled(true);
                    //startFeedbackAlarm();
                }
            }
        }
    };
    boolean FIRST_TIME = true;
    boolean DEBUG_MODE = false;
    int[] config;
    private int alarmType;
    private SensorManager sensorManager;
    private Sensor stepSensor;
    private AlarmManager myAlarmManager;
    private PendingIntent alarmPendingIntent_min;
    private PendingIntent alarmPendingIntent_feedback;
    private int STEP_COUNT = 0;
    private int INIT_STEP_COUNT = 0;
    private int INIT_MINUTE_STEP_COUNT = 0;
    private boolean feedbackEnabled = false;
    private int MINUTE_STEP_COUNT = 0;
    private long timepoint;
    private Notification.Builder sensorNotifBuilder;
    private NotificationCompat.Builder sensorNotifCompatBuilder;
    public BroadcastReceiver resetLocalBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.hasExtra(Constants.TIME_RESET_KEY)) {
                Log.d(Constants.TAG, "SensorService:: resetLocalBroadcastReceiver: time reset " + intent.getStringExtra(Constants.TIME_RESET_KEY));
            }
            else if(intent.hasExtra(Constants.SYMP_RESET_KEY)) {
                Log.d(Constants.TAG, "SensorService:: resetLocalBroadcastReceiver: symp reset " + intent.getIntExtra(Constants.SYMP_RESET_KEY, -1));
                if(getCurrentAlarmType() != intent.getIntExtra(Constants.SYMP_RESET_KEY, -1)) {
                    if(intent.getIntExtra(Constants.SYMP_RESET_KEY, -1)==Constants.SYMPTOMS_0) {
                        startAlarmOfType(Constants.SYMPTOMS_0);
                        Log.d(Constants.TAG, "SensorService: Changing to 0");
                    }
                    else if(intent.getIntExtra(Constants.SYMP_RESET_KEY, -1)==Constants.SYMPTOMS_1) {
                        startAlarmOfType(Constants.SYMPTOMS_1);
                        Log.d(Constants.TAG, "SensorService: Changing to 1");
                    }
                }
            }

        }
    };
    private boolean ALARM_MINUTE_FLAG = false;
    public BroadcastReceiver alarmLocalBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(Constants.TAG, "alarmLocalBroadcastReceiver: " + intent.getIntExtra(Constants.ALARM_COMM, 0));
            if (intent.getIntExtra(Constants.ALARM_COMM, -2) == 0) {
                registerSensorListener();

            } else if (intent.getIntExtra(Constants.ALARM_COMM, -2) == 1) {
                registerSensorListener();

            }
            if (intent.getIntExtra(Constants.ALARM_COMM, -2) == 2) {
                setALARM_MINUTE_FLAG(true);
                registerSensorListener();
            }
            if(intent.getIntExtra(Constants.ALARM_COMM, -2) == 3) {
                Log.d(Constants.TAG, "SensorManger:Feedback alarm received");
                // this is void
            }
        }
    };

    public int getMINUTE_STEP_COUNT() {
        return MINUTE_STEP_COUNT;
    }

    public long getTimepoint() {
        return timepoint;
    }

    public void setTimepoint(long timepoint) {
        this.timepoint = timepoint;
    }

    public void setALARM_MINUTE_FLAG(boolean ALARM_MINUTE_FLAG) {
        this.ALARM_MINUTE_FLAG = ALARM_MINUTE_FLAG;
    }

    public int getCurrentAlarmType() {
        return alarmType;
    }

    public void setCurrentAlarmType(int alarmType) {
        this.alarmType = alarmType;
    }

    public void initializeStepCount(int count) {
        INIT_STEP_COUNT = count;
    }

    public void initializeMinuteStepCount(int count) {
        INIT_MINUTE_STEP_COUNT = count;
    }

    public void calculateStepCount(int count) {
        if (count == INIT_STEP_COUNT) {
            STEP_COUNT = 0;
        } else {
            STEP_COUNT = count - INIT_STEP_COUNT;
        }

    }

    public int peekStepCount(int count) {
        if(count == INIT_STEP_COUNT) {
            return 0;
        }
        else
            return (count - INIT_STEP_COUNT);
    }

    public void calculateMinuteStepCount(int count) {
        if (count == INIT_MINUTE_STEP_COUNT) {
            MINUTE_STEP_COUNT = 0;
        } else {
            MINUTE_STEP_COUNT = count - INIT_MINUTE_STEP_COUNT;
        }
    }

    public int getStepCount() {
        return STEP_COUNT;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (sensorManager != null) {
            unregisterSensorListener();
        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(alarmLocalBroadcastReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(resetLocalBroadcastReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(feedbackLocalBroadcastReceiver);
        Log.d(Constants.TAG, "SensorService : onDestroy");
        stopForeground(true);
        cancelMinuteAlarm();
        stopSelf();
    }

    public int[] parseConfig(Intent intent) {
        if(intent.hasExtra(Constants.SENSOR_START_INTENT_KEY)) {
            String extra = intent.getStringExtra(Constants.SENSOR_START_INTENT_KEY);
            Log.d(Constants.TAG, "SensorService:parseConfig " + extra);
            String[] extraArray = extra.split("\\s+");
            int[] configArray = new int[5];
            configArray[0] = Integer.parseInt(extraArray[0]);
            configArray[1] = Integer.parseInt(extraArray[1]);
            configArray[2] = Integer.parseInt(extraArray[2]);
            configArray[3] = Integer.parseInt(extraArray[3]);
            configArray[4] = Integer.parseInt(extraArray[4]);
            return configArray;
        }
        return null;
    }

    public void storeConfig(int[] config) {
        this.config = config;
    }

    public int[] getMorningTime() {
        int[] morn_time = new int[2];
        morn_time[0] = this.config[0];
        morn_time[1] = this.config[1];
        return morn_time;
    }

    public int[] getNightTime() {
        int[] night_time = new int[2];
        night_time[0] = this.config[2];
        night_time[1] = this.config[3];
        return night_time;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int[] config = parseConfig(intent);
        storeConfig(config);
        final int type = config[4];
        myAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        LocalBroadcastManager.getInstance(this).registerReceiver(alarmLocalBroadcastReceiver, new IntentFilter(Constants.ALARM_LOCAL_RECEIVER_INTENT_FILTER));
        LocalBroadcastManager.getInstance(this).registerReceiver(resetLocalBroadcastReceiver, new IntentFilter(Constants.RESET_BROADCAST_INTENT_FILTER));
        LocalBroadcastManager.getInstance(this).registerReceiver(feedbackLocalBroadcastReceiver, new IntentFilter(Constants.FEEDBACK_BROADCAST_INTENT_FILTER));
        Log.d(Constants.TAG, "SensorService:onStartCommand:Starting Alarm of type:" + type);
        setFirstRun(true);
        registerSensorListener();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startAlarmOfType(type);
                startMinuteAlarm();

            }
        }, 5000);
        return super.onStartCommand(intent, flags, startId);
    }

    public void startAlarmOfType(int type) {
        setTimepoint(System.currentTimeMillis());
        if(type==Constants.SYMPTOMS_0) {
            setCurrentAlarmType(Constants.SYMPTOMS_0);
        }
        else if(type==Constants.SYMPTOMS_1) {
            setCurrentAlarmType(Constants.SYMPTOMS_1);
        }
    }

    public void startMinuteAlarm() {
        Intent alarmIntent_min = new Intent(this, AlarmReceiver.class);
        alarmIntent_min.putExtra(Constants.ALARM_COMM, 2);
        int interval = 60 * 1000;
        alarmPendingIntent_min = PendingIntent.getBroadcast(this, 668, alarmIntent_min, 0);
        Intent alarmInfoIntent = new Intent(this, MainActivity.class);
        PendingIntent alarmInfoPendingIntent = PendingIntent.getActivity(this, 777,alarmInfoIntent,0);
        myAlarmManager.setAlarmClock(new AlarmManager.AlarmClockInfo(System.currentTimeMillis()+interval, alarmInfoPendingIntent),alarmPendingIntent_min );
    }

    public void cancelMinuteAlarm() {
        Intent alarmIntent_min = new Intent(this, AlarmReceiver.class);
        alarmIntent_min.putExtra(Constants.ALARM_COMM, 2);
        alarmPendingIntent_min = PendingIntent.getBroadcast(this, 668, alarmIntent_min, 0);
        myAlarmManager.cancel(alarmPendingIntent_min);
        setALARM_MINUTE_FLAG(false);
    }

    public void startFeedbackAlarm() {
        Intent alarmIntent_feedback = new Intent(this, AlarmReceiver.class);
        alarmIntent_feedback.putExtra(Constants.ALARM_COMM, 3);
        int interval = 15 * 60 * 1000;
        if(DEBUG_MODE){
            interval = 60 * 1000;
        }
        alarmPendingIntent_feedback = PendingIntent.getBroadcast(this, 1212, alarmIntent_feedback, 0);
        myAlarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + interval, alarmPendingIntent_feedback);
        Intent alarmInfoIntent = new Intent(this, MainActivity.class);
        PendingIntent alarmInfoPendingIntent = PendingIntent.getActivity(this, 1212,alarmInfoIntent,0);
        myAlarmManager.setAlarmClock(new AlarmManager.AlarmClockInfo(System.currentTimeMillis()+interval, alarmInfoPendingIntent),alarmPendingIntent_feedback );
    }


    public boolean isTimeToNotify() {
        int[] morn_time = getMorningTime();
        int[] night_time = getNightTime();
        Calendar now = Calendar.getInstance();
        Calendar morningTime = Calendar.getInstance();
        Calendar nightTime = Calendar.getInstance();
        morningTime.set(Calendar.HOUR_OF_DAY, morn_time[0]);
        morningTime.set(Calendar.MINUTE, morn_time[1]);
        nightTime.set(Calendar.HOUR_OF_DAY, night_time[0]);
        nightTime.set(Calendar.MINUTE, night_time[1]);

        Log.d(Constants.TAG, "isTimeNotify: Now: " + now.get(Calendar.HOUR_OF_DAY) + " " + now.get(Calendar.MINUTE));
        Log.d(Constants.TAG, "isTimeNotify: Morn: " + morningTime.get(Calendar.HOUR_OF_DAY) + " " + morningTime.get(Calendar.MINUTE));
        Log.d(Constants.TAG, "isTimeNotify: Night: " + nightTime.get(Calendar.HOUR_OF_DAY) + " " + nightTime.get(Calendar.MINUTE));

        int now_hour = now.get(Calendar.HOUR_OF_DAY);
        int now_minute = now.get(Calendar.MINUTE);
        int morn_hour = morningTime.get(Calendar.HOUR_OF_DAY);
        int morn_minute = morningTime.get(Calendar.MINUTE);
        int night_hour = nightTime.get(Calendar.HOUR_OF_DAY);
        int night_minute = nightTime.get(Calendar.MINUTE);
        if (morn_hour > night_hour) {
            if (now_hour >= morn_hour) {
                if (now_hour == morn_hour) {
                    if (now_minute >= morn_minute) {
                        Log.d(Constants.TAG,"isTimeToNotify: True - Worked at minute level 1");
                        return true;
                    }
                }
                else {
                    Log.d(Constants.TAG,"isTimeToNotify: True - Worked at hour level 1");
                    return true;
                }

            } else if (now_hour <= night_hour) {
                if(now_hour == night_hour) {
                    if(now_minute <= night_minute) {
                        Log.d(Constants.TAG,"isTimeToNotify: True - Worked at minute level 2");
                        return true;
                    }
                }
                else {
                    Log.d(Constants.TAG,"isTimeToNotify: True - Worked at hour level 2");
                    return true;
                }

            }
        } else if (morn_hour < night_hour) {
            if ((now_hour > morn_hour) && (now_hour < night_hour)) {
                Log.d(Constants.TAG,"isTimeToNotify: True - Worked at hour level 3");
                return true;
            } else if (now_hour == morn_hour) {
                if (now_minute >= morn_minute) {
                    Log.d(Constants.TAG,"isTimeToNotify: True - Worked at minute level 3");
                    return true;
                }
            } else if (now_hour == night_hour) {
                if (now_minute <= night_minute) {
                    Log.d(Constants.TAG,"isTimeToNotify: True - Worked at minute level 4");
                    return true;
                }
            }
        }

        Log.d(Constants.TAG,"isTimeToNotify: False - Exit");
        return false;

    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(Constants.TAG, "SensorService : onCreate");
        sensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
        assert sensorManager != null;
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        showMonitorNotif();
        try {
            FileManager.createFile();
            Log.d(Constants.TAG, "SensorService:FileCreated!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showMonitorNotif() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(Constants.SENSOR_SERVICE_NOTIFICATION_CHANNEL_ID, "SENSOR_SEVICE", NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.setDescription("UPMC Dash notification channel");
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.setVibrationPattern(new long[]{0, 800, 100, 800, 100, 800, 100, 800, 100, 800});
            notificationChannel.enableVibration(true);
            notificationManager.createNotificationChannel(notificationChannel);
            sensorNotifBuilder = new Notification.Builder(this, Constants.SENSOR_SERVICE_NOTIFICATION_CHANNEL_ID);
            Intent dashIntent = new Intent(this, MessageService.class);
            dashIntent.setAction(Constants.ACTION_SETUP_WEAR);
            PendingIntent dashPendingIntent = PendingIntent.getForegroundService(this, 0, dashIntent, 0);
            sensorNotifBuilder.setAutoCancel(false)
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark_normal)
                    .setContentTitle("UPMC Dash Wear Monitor")
                    .setContentText("Waiting for Survey")
                    .setOngoing(true)
                    .setContentIntent(dashPendingIntent);

            startForeground(Constants.SENSOR_SERVICE_NOTIFICATION_ID, sensorNotifBuilder.build());

        } else {
            Intent dashIntent = new Intent(this, MessageService.class);
            dashIntent.setAction(Constants.ACTION_SETUP_WEAR);
            PendingIntent dashPendingIntent = PendingIntent.getService(this, 0, dashIntent, 0);
            sensorNotifCompatBuilder = new NotificationCompat.Builder(this, Constants.SENSOR_SERVICE_NOTIFICATION_CHANNEL_ID);
            sensorNotifCompatBuilder.setAutoCancel(false)
                    .setOngoing(true)
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark_normal)
                    .setContentTitle("UPMC Dash")
                    .setContentText("Waiting for Survey")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentInfo("info")
                    .setContentIntent(dashPendingIntent);
            startForeground(Constants.SENSOR_SERVICE_NOTIFICATION_ID, sensorNotifCompatBuilder.build());
        }
    }


    private void notifyMonitoring() {
        final NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            sensorNotifBuilder = new Notification.Builder(this, Constants.SENSOR_SERVICE_NOTIFICATION_CHANNEL_ID);
            Intent dashIntent = new Intent(this, MessageService.class);
            dashIntent.setAction(Constants.ACTION_SETUP_WEAR);
            PendingIntent dashPendingIntent = PendingIntent.getForegroundService(this, 0, dashIntent, 0);
            sensorNotifBuilder.setAutoCancel(false)
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark_normal)
                    .setContentTitle("UPMC Dash Wear Monitor")
                    .setContentText("Waiting for Survey")
                    .setOngoing(true)
                    .setContentIntent(dashPendingIntent);

            mNotificationManager.notify(Constants.SENSOR_SERVICE_NOTIFICATION_ID, sensorNotifBuilder.build());

        } else {
            Intent dashIntent = new Intent(this, MessageService.class);
            dashIntent.setAction(Constants.ACTION_SETUP_WEAR);
            PendingIntent dashPendingIntent = PendingIntent.getService(this, 0, dashIntent, 0);
            sensorNotifCompatBuilder = new NotificationCompat.Builder(this, Constants.SENSOR_SERVICE_NOTIFICATION_CHANNEL_ID);
            sensorNotifCompatBuilder.setAutoCancel(false)
                    .setOngoing(true)
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark_normal)
                    .setContentTitle("UPMC Dash")
                    .setContentText("Waiting for Survey")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentInfo("info")
                    .setContentIntent(dashPendingIntent);
            mNotificationManager.notify(Constants.SENSOR_SERVICE_NOTIFICATION_ID, sensorNotifCompatBuilder.build());
        }
    }

    public void notifyUserIfThreshold(int sc_count) {
        if(isTimeToNotify()) {
            if (sc_count < 50) {
                notifyInactive(sc_count);
            }
            else {
                Log.d(Constants.TAG, "SensorService: good job! lol");
            }
        }
    }



   public void  notifyInactive(int sc_count) {
       final NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
       if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

           sensorNotifBuilder = new Notification.Builder(this, Constants.SENSOR_SERVICE_NOTIFICATION_CHANNEL_ID);
           Intent dashIntent = new Intent(this, MessageService.class);
           dashIntent.setAction(Constants.ACTION_SETUP_WEAR);
           PendingIntent dashPendingIntent = PendingIntent.getForegroundService(this, 0, dashIntent, 0);
           sensorNotifBuilder.setAutoCancel(false)
                   .setWhen(System.currentTimeMillis())
                   .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark_normal)
                   .setContentTitle("UPMC Dash Activity Monitor")
                   .setContentText("Ready for a quick walk ? #" + sc_count)
                   .setOngoing(true)
                   .setContentIntent(dashPendingIntent);

           mNotificationManager.notify(Constants.SENSOR_SERVICE_NOTIFICATION_ID, sensorNotifBuilder.build());
       }
       else {

           sensorNotifCompatBuilder = new NotificationCompat.Builder(this, Constants.SENSOR_SERVICE_NOTIFICATION_CHANNEL_ID)
                   .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark_normal)
                   .setContentTitle("UPMC Dash Activity Monitor")
                   .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                   .setContentText("Ready for a quick walk ? #" + sc_count)
                   .setVisibility(Notification.VISIBILITY_PUBLIC)
                   .setPriority(Notification.PRIORITY_MAX)
                   .setDefaults(Notification.DEFAULT_ALL);

           assert mNotificationManager != null;
           mNotificationManager.notify(Constants.SENSOR_SERVICE_NOTIFICATION_ID, sensorNotifCompatBuilder.build());

           final Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
           LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(Constants.SENSOR_INTENT_FILTER).putExtra(Constants.SENSOR_EXTRA_KEY, Constants.NOTIFY_INACTIVITY));
           long[] pattern = {0, 800, 100, 800, 100, 800, 100, 800, 100, 800};
           assert vibrator != null;
           vibrator.vibrate(pattern, 0);
           Handler handler2 = new Handler();
           handler2.postDelayed(new Runnable() {
               @Override
               public void run() {
                   vibrator.cancel();
               }
           }, 3000);
       }
       Handler handler3 = new Handler();
       handler3.postDelayed(new Runnable() {
           @Override
           public void run() {
               notifyMonitoring();
           }
       }, 15000);
   }

    public void notifyFeedback(int sc_count) {
        final NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            sensorNotifBuilder = new Notification.Builder(this, Constants.SENSOR_SERVICE_NOTIFICATION_CHANNEL_ID);
            Intent dashIntent = new Intent(this, MessageService.class);
            dashIntent.setAction(Constants.ACTION_SETUP_WEAR);
            PendingIntent dashPendingIntent = PendingIntent.getForegroundService(this, 0, dashIntent, 0);
            sensorNotifBuilder.setAutoCancel(false)
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark_normal)
                    .setContentTitle("UPMC Dash Activity Monitor")
                    .setContentText("Great Job! You are active " + sc_count)
                    .setOngoing(true)
                    .setContentIntent(dashPendingIntent);
            mNotificationManager.notify(Constants.SENSOR_SERVICE_NOTIFICATION_ID, sensorNotifBuilder.build());
        }
        else {
            sensorNotifCompatBuilder = new NotificationCompat.Builder(getApplicationContext(),  Constants.SENSOR_SERVICE_NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark_normal)
                    .setContentTitle("UPMC Dash Activity Monitor")
                    .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                    .setContentText("Great Job! You are active " + sc_count)
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setPriority(Notification.PRIORITY_MAX)
                    .setDefaults(Notification.DEFAULT_ALL);

            assert mNotificationManager != null;
            mNotificationManager.notify(Constants.SENSOR_SERVICE_NOTIFICATION_ID, sensorNotifCompatBuilder.build());
            final Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(Constants.SENSOR_INTENT_FILTER).putExtra(Constants.SENSOR_EXTRA_KEY, Constants.NOTIFY_GREAT_JOB));
            long[] pattern = { 0, 800, 100, 800, 100, 800, 100, 800, 100, 800};
            assert vibrator != null;
            vibrator.vibrate(pattern, 0);
            Handler handler2 = new Handler();
            handler2.postDelayed(new Runnable() {
                @Override
                public void run() {
                    vibrator.cancel();
                }
            }, 3000);
        }



        Handler handler3 = new Handler();
        handler3.postDelayed(new Runnable() {
            @Override
            public void run() {
                notifyMonitoring();
            }
        }, 15000);

    }
    //to debug change 'minutes' to 'seconds'
    public boolean isEndOfInterval() {
        long timeDiff = System.currentTimeMillis() - getTimepoint();
        long minutes = TimeUnit.MILLISECONDS.toMinutes(timeDiff);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(timeDiff);
        Log.d(Constants.TAG, "SensorService:TimePointDiffReal: " + minutes + "mins or" + seconds + "s");
        if(DEBUG_MODE) {
            switch (getCurrentAlarmType()) {
                case Constants.ALARM_TYPE_1HR:
                    return (seconds >= 60);  //here
                case Constants.ALARM_TYPE_2HR:
                    return (seconds>=120);  //and here
                default:
                    return false;
            }
        }
        else {
            switch (getCurrentAlarmType()) {
                case Constants.ALARM_TYPE_1HR:
                    return (minutes >= 60);
                case Constants.ALARM_TYPE_2HR:
                    return (minutes>=120);
                default:
                    return false;
            }
        }

    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Log.d(Constants.TAG, "SensorService : onSensorChanged");
        int count = (int) sensorEvent.values[0];
        if (sensorEvent.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            if (isFirstRun()) {
                Log.d(Constants.TAG, "Step(firsttime): " + (int) sensorEvent.values[0]);
                initializeStepCount((int) sensorEvent.values[0]);
                initializeMinuteStepCount((int) sensorEvent.values[0]);
                setFirstRun(false);
                unregisterSensorListener();
                Log.d(Constants.TAG, "SensorService:TimePoint set");
                setTimepoint(System.currentTimeMillis());
            }
             else if(ALARM_MINUTE_FLAG) {
                    Log.d(Constants.TAG, "SensorService: Peeking step count" + peekStepCount(count));
                    LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(Constants.SENSOR_INTENT_FILTER).putExtra(Constants.SENSOR_EXTRA_KEY, Constants.SENSOR_ALARM));
                    Log.d(Constants.TAG, "SensorService: alarm minute flag");
                    unregisterSensorListener();
                    calculateMinuteStepCount(count);
                    initializeMinuteStepCount(count);
                    Log.d(Constants.TAG, "SensorService (min_steps taken)" + getMINUTE_STEP_COUNT());
                    startMinuteAlarm();
                    if(isTimeToNotify()) {
                        Log.d(Constants.TAG, "SensorService:isTimeToNotify(), writing to file...");
                        try {
                            FileManager.writeToFile(getMINUTE_STEP_COUNT(), 2);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        if(isFeedbackEnabled() && peekStepCount(count)>=15) {
                            Log.d(Constants.TAG, "SensorService: feedback successful: " + peekStepCount(count));
                            notifyFeedback(peekStepCount(count));
                            setFeedbackEnabled(false);
                        }
                        else if(isEndOfInterval()) {
                            Log.d(Constants.TAG, "SensorService: End of interval: " + getCurrentAlarmType());
                            calculateStepCount(count);
                            initializeStepCount(count);
                            Log.d(Constants.TAG, "SensorService:Logged: " + getStepCount());
                            try {
                                FileManager.writeToFile(getStepCount(), getCurrentAlarmType());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            notifyUserIfThreshold(getStepCount());
                            setTimepoint(System.currentTimeMillis());
                            setFeedbackEnabled(true);
                            startFeedbackAlarm();
                        } else if(peekStepCount(count) >= 50) {
                            calculateStepCount(count);
                            Log.d(Constants.TAG, "SensorService: reached threshold before interval " + getStepCount());
                            initializeStepCount(count);
                            try {
                                FileManager.writeToFile(getStepCount(), getCurrentAlarmType());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            setTimepoint(System.currentTimeMillis());
                        }
                    }


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

    public boolean isFeedbackEnabled() {
        return feedbackEnabled;
    }

    public void setFeedbackEnabled(boolean feedbackEnabled) {
        this.feedbackEnabled = feedbackEnabled;
    }

    public boolean isFirstRun() {
        return FIRST_TIME;
    }

    public void setFirstRun(boolean FIRST_TIME) {
        this.FIRST_TIME = FIRST_TIME;
    }
}
