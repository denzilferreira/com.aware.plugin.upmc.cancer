package com.aware.plugin.upmc.dash.services;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.Vibrator;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import android.util.Log;

import com.aware.plugin.upmc.dash.activities.NotificationResponse;
import com.aware.plugin.upmc.dash.activities.WearMainActivity;
import com.aware.plugin.upmc.dash.utils.Constants;
import com.aware.plugin.upmc.dash.fileutils.FileManager;
import com.aware.plugin.upmc.dash.R;
import com.aware.plugin.upmc.dash.utils.Preferences;

import java.io.IOException;
import java.util.Calendar;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Created by RaghuTeja on 6/24/17.
 */

public class SensorService extends Service implements SensorEventListener {
    boolean FIRST_TIME = true;
    boolean DEBUG_MODE = true;
    private boolean wasPrevTimePointTimeToNotify = false;
    private int alarmType;
    private SensorManager sensorManager;
    private Sensor stepSensor;
    private AlarmManager myAlarmManager;
    private PendingIntent alarmPendingIntent_min;
    private PendingIntent alarmPendingIntent_feedback;
    private int STEP_COUNT = 0;
    private int INIT_STEP_COUNT = 0;
    private int INIT_MINUTE_STEP_COUNT = 0;
    private int INIT_FEEDBACK_STEP_COUNT = 0;
    private boolean feedbackEnabled = false;
    private int MINUTE_STEP_COUNT = 0;
    private long timepoint;
    private Notification.Builder sessionStatusNotifBuilder;
    private NotificationCompat.Builder sessionStatusCompatNotifBuilder;
    private boolean ALARM_MINUTE_FLAG = false;
    private boolean snoozeActive = false;
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



    public void initializeFeedbackStepCount(int count) {
        Log.d(Constants.TAG, "initializing Feedback SC to " + count);
        INIT_FEEDBACK_STEP_COUNT = count;
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


    public int peakFeedbackStepCount(int count) {
        if(count == INIT_FEEDBACK_STEP_COUNT) {
            return 0;
        }
        else
            return (count - INIT_FEEDBACK_STEP_COUNT);
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
        Log.d(Constants.TAG, "SensorService : onDestroy");
        stopForeground(true);
        cancelMinuteAlarm();
        stopSelf();
    }



    public void warmUpSensor() {
        final int alarmType = Preferences.getSymptomRating(getApplicationContext());
        setCurrentAlarmType(alarmType);
        Log.d(Constants.TAG, "SensorService:onStartCommand:Starting Alarm of type:" + alarmType);
        setFirstRun(true);
        registerSensorListener();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                setInterventionAlarm();
                setMinuteAlarm();
            }
        }, 5000);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        switch (action) {
            case Constants.ACTION_FIRST_RUN:
                Log.d(Constants.TAG, "SensorService:onStartCommand:" +  action);
                myAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                showSessionStatus();
                createInterventionNotifChannel();
                warmUpSensor();
                break;
            case Constants.ACTION_MINUTE_ALARM:
                Log.d(Constants.TAG, "SensorService:onStartCommand: ACTION_MINUTE_ALARM");
                setALARM_MINUTE_FLAG(true);
                registerSensorListener();
                if(randomizer() && isTimeToNotify()) {
                    sendMessageServiceAction(Constants.ACTION_SCAN_PHONE);
                    sendMessageServiceAction(Constants.ACTION_SYNC_DATA);
                }

                Calendar cal = Calendar.getInstance();
                if(randomizer() && !isTimeToNotify()) {
                    sendMessageServiceAction(Constants.ACTION_UPSTREAM_OK);
                }

                break;

            case Constants.ACTION_NOTIF_OK:
                Log.d(Constants.TAG, "SensorService:onStartCommand: ACTION_NOTIF_OK" );
                sendMessageServiceAction(Constants.ACTION_NOTIF_OK);
                dismissIntervention();
                break;

            case Constants.ACTION_NOTIF_NO:
                Log.d(Constants.TAG, "SensorService:onStartCommand: ACTION_NOTIF_NO");
                sendMessageServiceAction(Constants.ACTION_NOTIF_NO);
                dismissIntervention();
                break;


            case Constants.ACTION_NOTIF_SNOOZE:
                Log.d(Constants.TAG, "SensorService:onStartCommand: ACTION_NOTIF_SNOOZE");
                sendMessageServiceAction(Constants.ACTION_NOTIF_SNOOZE);
                snoozeInactivityNotif();
                break;


            case Constants.ACTION_SNOOZE_ALARM:
                Log.d(Constants.TAG, "SensorService:onStartCommand: ACTION_SNOOZE_ALARM");
                notifyInactive(0, false);
                sendMessageServiceAction(action);
                setSnoozeActive(false);
                break;

            case Constants.ACTION_NOTIF_OK_PHONE:
                Log.d(Constants.TAG, "SensorService:onStartCommand: ACTION_NOTIF_OK_PHONE");
                dismissIntervention();
                break;

            case Constants.ACTION_NOTIF_NO_PHONE:
                Log.d(Constants.TAG, "SensorService:onStartCommand: ACTION_NOTIF_NO_PHONE");
                dismissIntervention();
                break;

            case Constants.ACTION_NOTIF_SNOOZE_PHONE:
                Log.d(Constants.TAG, "SensorService:onStartCommand: ACTION_NOTIF_SNOOZE_PHONE");
                snoozeInactivityNotif();
                break;

            case Constants.ACTION_FEEDBACK_ALARM:
                Log.d(Constants.TAG, "SensorService:onStartCommand: ACTION_FEEDBACK_ALARM (ends)");
                cancelFeedbackAlarm();
                break;

            case Constants.ACTION_FEEDBACK_OK:
                Log.d(Constants.TAG, "SensorService:onStartCommand: ACTION_FEEDBACK_OK");
                dismissIntervention();
                break;

            case Constants.ACTION_STOP_SELF:
                Log.d(Constants.TAG, "SensorService:onStartCommand: ACTION_STOP_SELF");
                if (sensorManager != null) {
                    unregisterSensorListener();
                }
                stopForeground(true);
                cancelMinuteAlarm();
                stopSelf();
                break;
            default:
                Log.d(Constants.TAG, "SensorService:onStartCommand:UndefinedAction:" + action);
        }

        return super.onStartCommand(intent, flags, startId);
    }

    public boolean randomizer() {
        return ((new Random().nextInt(100) + 1) >= 50);
    }



    public void snoozeInactivityNotif() {
        AlarmManager mAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent snoozeInt = new Intent(this, SensorService.class).setAction(Constants.ACTION_SNOOZE_ALARM);
        PendingIntent snoozePendInt;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            snoozePendInt = PendingIntent.getForegroundService(this, 56, snoozeInt, 0);
        } else {
            snoozePendInt = PendingIntent.getService(this, 56, snoozeInt, 0);
        }
        int interval = 0;
        if(DEBUG_MODE)
            interval = 60 * 1000;
        else
            interval = 15*60*1000;
        mAlarmManager.set(AlarmManager.RTC_WAKEUP,System.currentTimeMillis() + interval, snoozePendInt);
        dismissIntervention();
        setSnoozeActive(true);
    }

    public void cancelSnoozeInactivityNotif() {
        if(!isSnoozeActive())
            return;
        AlarmManager mAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent snoozeInt = new Intent(this, SensorService.class).setAction(Constants.ACTION_SNOOZE_ALARM);
        PendingIntent snoozePendInt;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            snoozePendInt = PendingIntent.getForegroundService(this, 56, snoozeInt, 0);
        } else {
            snoozePendInt = PendingIntent.getService(this, 56, snoozeInt, 0);
        }
        mAlarmManager.cancel(snoozePendInt);
        Log.d(Constants.TAG, "SensorService: cancelling snooze");
    }

    public void createInterventionNotifChannel() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(Constants.INTERVENTION_NOTIF_CHNL_ID, Constants.INTERVENTION_NOTIF_CHNL_NAME, NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.setDescription(Constants.INTERVENTION_NOTIF_CHNL_DESC);
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(notificationChannel);
        }

    }

    public void setInterventionAlarm() {
        int rating = Preferences.getSymptomRating(getApplicationContext());
        if(rating!=getCurrentAlarmType())
            Log.d(Constants.TAG, "setInterventionAlarm: detected SR change, using: " + rating);
        switch (rating) {
            case Constants.SYMPTOMS_0:
                setCurrentAlarmType(Constants.ALARM_TYPE_1HR);
                break;
            case Constants.SYMPTOMS_1:
                setCurrentAlarmType(Constants.ALARM_TYPE_2HR);
        }
        setTimepoint(System.currentTimeMillis());
    }

    public void setMinuteAlarm() {
        Intent alarmIntent_min = new Intent(this, SensorService.class).setAction(Constants.ACTION_MINUTE_ALARM);
        int interval = 60 * 1000;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            alarmPendingIntent_min = PendingIntent.getForegroundService(this, 668, alarmIntent_min, 0);

        }
        else {
            alarmPendingIntent_min = PendingIntent.getService(this, 668, alarmIntent_min, 0);

        }
        Intent alarmInfoIntent = new Intent(this, WearMainActivity.class);
        PendingIntent alarmInfoPendingIntent = PendingIntent.getActivity(this, 777,alarmInfoIntent,0);
        myAlarmManager.setAlarmClock(new AlarmManager.AlarmClockInfo(System.currentTimeMillis()+interval, alarmInfoPendingIntent),alarmPendingIntent_min );
    }

    public void cancelMinuteAlarm() {
        if(myAlarmManager!=null &&alarmPendingIntent_min!=null) {
            Intent alarmIntent_min = new Intent(this, SensorService.class).setAction(Constants.ACTION_MINUTE_ALARM);
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                alarmPendingIntent_min = PendingIntent.getForegroundService(this, 668, alarmIntent_min, 0);

            }
            else {
                alarmPendingIntent_min = PendingIntent.getService(this, 668, alarmIntent_min, 0);

            }
            myAlarmManager.cancel(alarmPendingIntent_min);
            setALARM_MINUTE_FLAG(false);
        }
    }

    public void startFeedbackAlarm() {
        if(myAlarmManager==null)
            return;
        if(isFeedbackEnabled())
            cancelFeedbackAlarm();
        Log.d(Constants.TAG, "StartFeedbackAlarmCalled");
        initializeFeedbackStepCount(INIT_MINUTE_STEP_COUNT);
        Intent alarmIntent_feedback = new Intent(this, SensorService.class).setAction(Constants.ACTION_FEEDBACK_ALARM);
        int interval = 15 * 60 * 1000;

        if(DEBUG_MODE){
            interval = 60 * 1000;
            return;
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            alarmPendingIntent_feedback = PendingIntent.getForegroundService(this, 1212, alarmIntent_feedback, 0);

        }
        else {
            alarmPendingIntent_feedback = PendingIntent.getService(this, 1212, alarmIntent_feedback, 0);

        }
        Intent alarmInfoIntent = new Intent(this, WearMainActivity.class);
        PendingIntent alarmInfoPendingIntent = PendingIntent.getActivity(this, 1212,alarmInfoIntent,0);
        myAlarmManager.setAlarmClock(new AlarmManager.AlarmClockInfo(System.currentTimeMillis()+interval, alarmInfoPendingIntent),alarmPendingIntent_feedback );
        setFeedbackEnabled(true);
    }



    public void cancelFeedbackAlarm() {
        Log.d(Constants.TAG, "SensorService: cancelFeedbackAlarm called");
        if(isFeedbackEnabled() && myAlarmManager!=null) {
            myAlarmManager.cancel(alarmPendingIntent_feedback);
            setFeedbackEnabled(false);
        }
    }



    public boolean isTimeToNotify() {
        int[] morn_time = Preferences.getMorningTime(getApplicationContext());
        int[] night_time = Preferences.getNightTime(getApplicationContext());
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
        try {
            FileManager.createFile();
            Log.d(Constants.TAG, "SensorService:FileCreated!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showSessionStatus() {
        final String contentText = isTimeToNotify()? Constants.SS_MONITORING:Constants.SS_NOT_MONITORING;
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(Constants.SESSION_STATUS_CHNL_ID, Constants.SESSION_STATUS_CHNL_NAME, NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.setDescription("session notification channel");
            notificationManager.createNotificationChannel(notificationChannel);
            sessionStatusNotifBuilder = new Notification.Builder(this, Constants.SESSION_STATUS_CHNL_ID);
            sessionStatusNotifBuilder.setAutoCancel(false)
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark_normal)
                    .setContentTitle("Dash Monitor")
                    .setContentText(contentText)
                    .setOngoing(true);
            startForeground(Constants.SESSION_STATUS_NOTIF_ID, sessionStatusNotifBuilder.build());

        } else {
            sessionStatusCompatNotifBuilder = new NotificationCompat.Builder(this, Constants.SESSION_STATUS_CHNL_ID);
            sessionStatusCompatNotifBuilder.setAutoCancel(false)
                    .setOngoing(true)
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark_normal)
                    .setContentTitle("Dash Monitor")
                    .setContentText(contentText)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentInfo("info");
            startForeground(Constants.SESSION_STATUS_NOTIF_ID, sessionStatusCompatNotifBuilder.build());
        }
    }


    private void notifySessionStatus(boolean isTimeToNotify) {
        final String contentText = isTimeToNotify? Constants.SS_MONITORING:Constants.SS_NOT_MONITORING;
        final NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            sessionStatusNotifBuilder.setContentText(contentText);
            mNotificationManager.notify(Constants.SESSION_STATUS_NOTIF_ID, sessionStatusNotifBuilder.build());

        } else {
            sessionStatusCompatNotifBuilder.setContentText(contentText);
            mNotificationManager.notify(Constants.SESSION_STATUS_NOTIF_ID, sessionStatusCompatNotifBuilder.build());
        }
    }

    public void notifyUserIfThreshold(int sc_count) {
        if(isTimeToNotify()) {
            if (sc_count < 50) {
                sendMessageServiceAction(Constants.ACTION_NOTIFY_INACTIVITY);
                notifyInactive(sc_count, true);
            }
            else {
                Log.d(Constants.TAG, "SensorService: good job! lol");
            }
        }
    }


   public void  notifyInactive(int sc_count, boolean showSnooze) {
       wakeUpAndVibrate(Constants.DURATION_AWAKE, Constants.DURATION_VIBRATE);
       final NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
       Intent contentIntent = contentIntent = new Intent(this, NotificationResponse.class);
       if(showSnooze)
           contentIntent.setAction(Constants.ACTION_SHOW_SNOOZE);
       PendingIntent contentPI = PendingIntent.getActivity(this, 0, contentIntent, 0);
       if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
           Notification.Builder interventionNotifBuilder = new Notification.Builder(this, Constants.INTERVENTION_NOTIF_CHNL_ID);
           interventionNotifBuilder.setAutoCancel(false)
                   .setWhen(System.currentTimeMillis())
                   .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark_normal)
                   .setContentTitle("Dash Monitor")
                   .setContentText(Constants.NOTIF_INACTIVITY + " #" + sc_count)
                   .setOngoing(true)
                   .setContentIntent(contentPI)
                   .setTimeoutAfter(Constants.INTERVENTION_TIMEOUT)
                   .setGroup("intervention");
           mNotificationManager.notify(Constants.INTERVENTION_NOTIF_ID, interventionNotifBuilder.build());
       }
       else {
           NotificationCompat.Builder interventionNotifCompatBuilder = new NotificationCompat.Builder(this, "sensor_service_intervention")
                   .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark_normal)
                   .setContentTitle("Dash Monitor")
                   .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                   .setContentText(Constants.NOTIF_INACTIVITY + " #" + sc_count)
                   .setVisibility(Notification.VISIBILITY_PUBLIC)
                   .setContentIntent(contentPI)
                   .setPriority(Notification.PRIORITY_MAX)
                   .setAutoCancel(true)
                   .setTimeoutAfter(Constants.INTERVENTION_TIMEOUT)
                   .setOngoing(true)
                   .setDefaults(Notification.DEFAULT_ALL);
           assert mNotificationManager != null;
           mNotificationManager.notify(Constants.INTERVENTION_NOTIF_ID, interventionNotifCompatBuilder.build());
       }
       startFeedbackAlarm();
   }


   public void wakeUpAndVibrate(int duration_awake, int duration_vibrate) {
       PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
       PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "Wake Up");
       wl.acquire(duration_awake);
       final Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
       long[] pattern = {0, 800, 100, 800, 100, 800, 100, 800, 100, 800};
       assert vibrator != null;
       vibrator.vibrate(pattern, 0);
       Handler handler2 = new Handler();
       handler2.postDelayed(new Runnable() {
           @Override
           public void run() {
               vibrator.cancel();
           }
       }, duration_vibrate);
   }

    public void notifyFeedback(int sc_count) {
        wakeUpAndVibrate(Constants.DURATION_AWAKE, Constants.DURATION_VIBRATE);
        cancelFeedbackAlarm();
        Intent contentIntent = new Intent(this, SensorService.class).setAction(Constants.ACTION_FEEDBACK_OK);
        final NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent contentPI = PendingIntent.getForegroundService(this, 0, contentIntent, 0);
            Notification.Builder interventionNotifBuilder = new Notification.Builder(this, Constants.INTERVENTION_NOTIF_CHNL_ID);
            interventionNotifBuilder.setAutoCancel(true)
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark_normal)
                    .setContentTitle("Dash Monitor")
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setContentText(Constants.NOTIF_APPRAISAL + " #" + sc_count)
                    .setOngoing(true)
                    .setContentIntent(contentPI)
                    .setGroup("intervention")
                    .setTimeoutAfter(Constants.INTERVENTION_TIMEOUT);
            mNotificationManager.notify(Constants.INTERVENTION_NOTIF_ID, interventionNotifBuilder.build());
        }
        else {
            PendingIntent contentPI = PendingIntent.getService(this, 0, contentIntent, 0);
            NotificationCompat.Builder interventionNotifCompatBuilder = new NotificationCompat.Builder(getApplicationContext(), "sensor_service_intervention" )
                    .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark_normal)
                    .setContentTitle("Dash Monitor")
                    .setWhen(System.currentTimeMillis())
                    .setAutoCancel(true)
                    .setOngoing(true)
                    .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                    .setContentText(Constants.NOTIF_APPRAISAL + " #" + sc_count)
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setPriority(Notification.PRIORITY_MAX)
                    .setContentIntent(contentPI)
                    .setTimeoutAfter(Constants.INTERVENTION_TIMEOUT)
                    .setDefaults(Notification.DEFAULT_ALL);

            assert mNotificationManager != null;
            mNotificationManager.notify(Constants.INTERVENTION_NOTIF_ID, interventionNotifCompatBuilder.build());
        }
    }

    public void dismissIntervention() {
        final NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(Constants.INTERVENTION_NOTIF_ID);
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
                    return (seconds>= 120);  //and here
                default:
                    return false;
            }
        }
        else {
            switch (getCurrentAlarmType()) {
                case Constants.ALARM_TYPE_1HR:
                    return (minutes >= 60);
                case Constants.ALARM_TYPE_2HR:
                    return (minutes>= 120);
                default:
                    return false;
            }
        }
    }

    public void sendMessageServiceAction(String action) {
        Intent intent = new Intent(getApplicationContext(), MessageService.class).setAction(action);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        }
        else {
            startService(intent);
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
//                notifyUserIfThreshold(10);
            }
             else if(ALARM_MINUTE_FLAG) {
                    Log.d(Constants.TAG, "SensorService: Peeking step count" + peekStepCount(count));
                    Log.d(Constants.TAG, "SensorService: alarm minute flag");
                    unregisterSensorListener();
                    calculateMinuteStepCount(count);
                    initializeMinuteStepCount(count);
                    Log.d(Constants.TAG, "SensorService (min_steps taken)" + getMINUTE_STEP_COUNT());
                    setMinuteAlarm();
                    boolean isTimeToNotify = isTimeToNotify();
                    if(isTimeToNotify!=wasPrevTimePointTimeToNotify())
                        notifySessionStatus(isTimeToNotify);
                    setWasPrevTimePointTimeToNotify(isTimeToNotify);
                    if(isTimeToNotify) {
                        Log.d(Constants.TAG, "SensorService:isTimeToNotify(), writing to file...");
                        try {
                            FileManager.writeToFile(getMINUTE_STEP_COUNT(), 2);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        if(isFeedbackEnabled() && peakFeedbackStepCount(count)>=10) {
                            Log.d(Constants.TAG, "SensorService: feedback successful: " + peakFeedbackStepCount(count));
                            sendMessageServiceAction(Constants.ACTION_NOTIFY_GREAT_JOB);
                            notifyFeedback(peakFeedbackStepCount(count));
                            try {
                                FileManager.writeToFile(peakFeedbackStepCount(count), 3);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            cancelSnoozeInactivityNotif();
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
                            setInterventionAlarm();
                        } else if(peekStepCount(count) >= 50) {
                            calculateStepCount(count);
                            Log.d(Constants.TAG, "SensorService: reached threshold before interval " + getStepCount());
                            initializeStepCount(count);
                            try {
                                FileManager.writeToFile(getStepCount(), getCurrentAlarmType());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            setInterventionAlarm();
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

    public boolean wasPrevTimePointTimeToNotify() {
        return wasPrevTimePointTimeToNotify;
    }

    public void setWasPrevTimePointTimeToNotify(boolean wasPrevTimePointTimeToNotify) {
        this.wasPrevTimePointTimeToNotify = wasPrevTimePointTimeToNotify;
    }

    public boolean isSnoozeActive() {
        return snoozeActive;
    }

    public void setSnoozeActive(boolean snoozeActive) {
        this.snoozeActive = snoozeActive;
    }
}
