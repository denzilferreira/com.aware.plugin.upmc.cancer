package com.aware.plugin.upmc.dash.services;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.plugin.upmc.dash.R;
import com.aware.plugin.upmc.dash.activities.MainActivity;
import com.aware.plugin.upmc.dash.activities.NotificationResponseActivity;
import com.aware.plugin.upmc.dash.activities.Plugin;
import com.aware.plugin.upmc.dash.activities.Provider;
import com.aware.plugin.upmc.dash.settings.Settings;
import com.aware.plugin.upmc.dash.utils.Constants;
import com.aware.utils.Scheduler;

import org.json.JSONException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static com.aware.plugin.upmc.dash.utils.Constants.ACTION_CHECK_PROMPT;
import static com.aware.plugin.upmc.dash.utils.Constants.BLUETOOTH_ON;
import static com.aware.plugin.upmc.dash.utils.Constants.CLOSE_COMMAND;
import static com.aware.plugin.upmc.dash.utils.Constants.CLOSE_NOTIF;
import static com.aware.plugin.upmc.dash.utils.Constants.CONNECTED_WEAR;
import static com.aware.plugin.upmc.dash.utils.Constants.CONTENT_TITLE_FITBIT;
import static com.aware.plugin.upmc.dash.utils.Constants.DB_NAME;
import static com.aware.plugin.upmc.dash.utils.Constants.DB_URL;
import static com.aware.plugin.upmc.dash.utils.Constants.DO_NOT_DISTURB_COMMAND;
import static com.aware.plugin.upmc.dash.utils.Constants.FAILED_WEAR;
import static com.aware.plugin.upmc.dash.utils.Constants.HOST_URL;
import static com.aware.plugin.upmc.dash.utils.Constants.INTERVENTION_TIMEOUT;
import static com.aware.plugin.upmc.dash.utils.Constants.JDBC_DRIVER;
import static com.aware.plugin.upmc.dash.utils.Constants.LIGHTTPD_ADD_HOST;
import static com.aware.plugin.upmc.dash.utils.Constants.LIGHTTPD_START;
import static com.aware.plugin.upmc.dash.utils.Constants.MINIMESSAGE;
import static com.aware.plugin.upmc.dash.utils.Constants.NOTIFICATION;
import static com.aware.plugin.upmc.dash.utils.Constants.NOTIF_NO_SNOOZE;
import static com.aware.plugin.upmc.dash.utils.Constants.OTHER;
import static com.aware.plugin.upmc.dash.utils.Constants.PACKAGE_FITBIT;
import static com.aware.plugin.upmc.dash.utils.Constants.PACKAGE_KSWEB;
import static com.aware.plugin.upmc.dash.utils.Constants.PASS;
import static com.aware.plugin.upmc.dash.utils.Constants.SNOOZE_COMMAND;
import static com.aware.plugin.upmc.dash.utils.Constants.TABLE_COMMAND;
import static com.aware.plugin.upmc.dash.utils.Constants.TABLE_CONN;
import static com.aware.plugin.upmc.dash.utils.Constants.TABLE_PROMPT;
import static com.aware.plugin.upmc.dash.utils.Constants.TABLE_SENSOR_DATA;
import static com.aware.plugin.upmc.dash.utils.Constants.TABLE_TS;
import static com.aware.plugin.upmc.dash.utils.Constants.TAG_KEY;
import static com.aware.plugin.upmc.dash.utils.Constants.USER;
import static com.aware.plugin.upmc.dash.utils.KSWEBControl.DATA_KEY;

public class FitbitMessageService extends Service {

    private Notification.Builder setupNotifBuilder;
    private NotificationCompat.Builder setupNotifCompatBuilder;
    //    private int count = 0;
    private int id = 0;
    private int promptCount = 0;
    private String message;
    private Notification.Builder surveyNotifBuilder;
    private NotificationCompat.Builder surveyCompatNotifBuilder;
    private BroadcastReceiver mBluetootLocalReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            switch (state) {
                case BluetoothAdapter.STATE_OFF:
                    Log.d(Constants.TAG, "FitbitMessageService:BluetoothReceiver:StateOff");
                    notifySetup(Constants.FAILED_WEAR_BLUETOOTH);
                    if (!enableBluetoothIfOff())
                        Toast.makeText(getApplicationContext(), "Bluetooth Error", Toast.LENGTH_SHORT).show();
                    break;
                case BluetoothAdapter.STATE_TURNING_OFF:
                    Log.d(Constants.TAG, "FitbitMessageService:BluetoothReceiver:StateTurningOff");
                    BluetoothAdapter.getDefaultAdapter().enable();
                    notifySetup(Constants.FAILED_WEAR_BLUETOOTH);
                    break;
                case BluetoothAdapter.STATE_ON:
                    Log.d(Constants.TAG, "FitbitMessageService:BluetoothReceiver:StateOn");
                    notifySetup(BLUETOOTH_ON);
                    break;
                case BluetoothAdapter.STATE_TURNING_ON:
                    Log.d(Constants.TAG, "FitbitMessageService:BluetoothReceiver:StateTurningOn");
                    notifySetup(BLUETOOTH_ON);
                    break;
            }

        }
    };
    private BroadcastReceiver mConnectivityReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(ConnectivityManager.EXTRA_NETWORK_TYPE, -1);
            enableWifiIfOff();
            Log.d(Constants.TAG, "Caught intent ");
            switch (state) {
                case ConnectivityManager.TYPE_BLUETOOTH:
                    Log.d(Constants.TAG, "mConnectivityReceiver: Blue");
                    break;
                case ConnectivityManager.TYPE_WIFI:
                    Log.d(Constants.TAG, "mConnectivityReceiver: Wifi");
                    break;
                case ConnectivityManager.TYPE_ETHERNET:
                    Log.d(Constants.TAG, "mConnecti vityReceiver: ether");
                    break;

                case ConnectivityManager.TYPE_MOBILE:
                    Log.d(Constants.TAG, "mConnectivityReceiver: mob");
                    break;

            }

        }
    };

    public void registerBluetoothReceiver() {
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mBluetootLocalReceiver, filter);
    }

    public void registerConnectivityReceiver() {
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mConnectivityReceiver, filter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(Constants.TAG, "FitbitMessageService:onDestroy");
        stopForeground(true);
        unregisterReceiver(mBluetootLocalReceiver);
        unregisterReceiver(mConnectivityReceiver);
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert mNotificationManager != null;
        mNotificationManager.cancelAll();
        stopSelf();
    }

    @Override
    public void onCreate() {
        Log.d(Constants.TAG, "FitbitMessageService: onCreate");
        super.onCreate();
    }

    public void enableWifiIfOff() {
        WifiManager wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        assert wifiManager != null;
        if (!wifiManager.isWifiEnabled())
            wifiManager.setWifiEnabled(true);
    }

    public boolean enableBluetoothIfOff() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter!=null) {
            boolean isEnabled = bluetoothAdapter.isEnabled();
            if (!isEnabled)
                return bluetoothAdapter.enable();
            return true;
        }
        else
            return false;
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final int i = super.onStartCommand(intent, flags, startId);
        String intentAction = null;
        if (intent != null)
            intentAction = intent.getAction();
        if (intentAction == null)
            return i;
        Log.d(Constants.TAG, "FitbitMessageService: onStartCommand " + intentAction);
        switch (intentAction) {
            case Constants.ACTION_REBOOT:
                enableBluetoothIfOff();
                enableWifiIfOff();
                registerBluetoothReceiver();
                registerConnectivityReceiver();
                showSurveyNotif();
                showFitbitNotif();
                createInterventionNotifChannel();
                if (!isAppRunning(PACKAGE_FITBIT)) {
                    launchApp(PACKAGE_FITBIT);
                }
                if (!isAppRunning(PACKAGE_KSWEB)) {
                    launchApp(PACKAGE_KSWEB);
                }
                new Handler().postDelayed(() -> {
                    Intent upmc = new Intent(getApplicationContext(), MainActivity.class);
                    upmc.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(upmc);
                    new ResetTable().execute();
                    startFitbitCheckPromptAlarm();
                }, 3000);
                break;
            case Constants.ACTION_FIRST_RUN:
                Log.d(Constants.TAG, "FitbitMessageService: onStartCommand first run");
                enableBluetoothIfOff();
                enableWifiIfOff();
                registerBluetoothReceiver();
                registerConnectivityReceiver();
                showSurveyNotif();
                showFitbitNotif();
                createInterventionNotifChannel();
                if (!isAppRunning(PACKAGE_FITBIT)) {
                    launchApp(PACKAGE_FITBIT);
                }
                if (!isAppRunning(PACKAGE_KSWEB)) {
                    launchApp(PACKAGE_KSWEB);
                }
                new Handler().postDelayed(() -> {
                    Intent upmc = new Intent(getApplicationContext(), MainActivity.class);
                    upmc.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(upmc);
                    setUpDatabase();
                    startFitbitCheckPromptAlarm();
                }, 5000);
                break;
            case Constants.ACTION_CHECK_PROMPT:
                Log.d(Constants.TAG, "FitbitMessageService: onStartCommand check prompt repeatedly");
                new CheckPrompt().execute();
                break;
            case Constants.ACTION_CHECK_CONN:
                Log.d(Constants.TAG, "FitbitMessageService: onStartCommand check watch connection");
//                startActivity(new Intent(this, SetupLoadingActvity.class));
                new CheckConn().execute();
                break;
            case Constants.ACTION_SURVEY_COMPLETED:
                Log.d(Constants.TAG, "FitbitMessageService: onStartCommand notify survey completed");
                Log.d("yiyi", "action_survey_completed got called!");
                notifySurvey(false);
                break;
            case Plugin.ACTION_CANCER_SURVEY:
                Log.d(Constants.TAG, "FitbitMessageService:onStartCommand: ACTION_CANCER_SURVEY");
                notifySurvey(true);
                break;
            case Constants.ACTION_SETTINGS_CHANGED:
                Log.d(Constants.TAG, "FibitMessageService:onStartCommand : ACTION_SETTINGS_CHANGED");
                saveTimeSchedule();
                break;
            case Constants.ACTION_SYNC_DATA:
                new SyncData().execute();
                break;
            case Constants.ACTION_APPRAISAL:
                Log.d(Constants.TAG, "FitbitMessageService: onStartCommand appraisal");
                dismissIntervention();
                break;
            case Constants.ACTION_INACTIVITY:
                Log.d(Constants.TAG, "FitbitMessageService: onStartCommand : inactivity");
                startActivity(new Intent(this, NotificationResponseActivity.class));
                break;
            case Constants.ACTION_NOTIF_SNOOZE:
                new PostData().execute(TABLE_COMMAND, SNOOZE_COMMAND);
                dismissIntervention();
                break;
            case Constants.ACTION_NOTIF_OK:
                new PostData().execute(TABLE_COMMAND, CLOSE_COMMAND);
                dismissIntervention();
                break;
            case Constants.ACTION_NOTIF_NO:
                new PostData().execute(TABLE_COMMAND, CLOSE_COMMAND);
                Log.d(Constants.TAG, "FitbitMessageService:" + intentAction);
                dismissIntervention();
                break;
            case Constants.ACTION_DO_NOT_DISTURB:
                Log.d("yiyi", "FitbitMessageService:" + intentAction);
                String mode = Aware.getSetting(getApplicationContext(),Settings.PLUGIN_UPMC_CANCER_DND_MODE);
                new PostData().execute(TABLE_COMMAND, mode.equals(Constants.DND_MODE_ON)?Constants.DO_NOT_DISTURB_COMMAND:Constants.REMOVE_DO_NOT_DISTURB);
                break;

            default:
                return i;
        }
        return i;
    }

    private void showSurveyNotif() {
        final Intent dashIntent = new Intent(this, MainActivity.class);
        createSurveyNotifChannel(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            surveyNotifBuilder = new Notification.Builder(this, Constants.SURVEY_NOTIF_CHNL_ID);
            PendingIntent dashPendingIntent = PendingIntent.getActivity(this, 0, dashIntent, 0);
            surveyNotifBuilder.setAutoCancel(false)
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark_normal)
                    .setContentTitle(Constants.SELF_REPORT_TITLE)
                    .setGroup("Survey")
                    .setContentText(Constants.SELF_REPORT_CONTENT)
                    .setContentIntent(dashPendingIntent);
            startForeground(Constants.SURVEY_NOTIF_ID, surveyNotifBuilder.build());

        } else {
            PendingIntent dashPendingIntent = PendingIntent.getActivity(this, 0, dashIntent, 0);
            surveyCompatNotifBuilder = new NotificationCompat.Builder(this, Constants.SURVEY_NOTIF_CHNL_ID);
            surveyCompatNotifBuilder.setAutoCancel(false)
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark_normal)
                    .setContentTitle(Constants.SELF_REPORT_TITLE)
                    .setContentText(Constants.SELF_REPORT_CONTENT)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setGroup("Survey")
                    .setContentInfo("Survey Notification")
                    .setContentIntent(dashPendingIntent);
            startForeground(Constants.SURVEY_NOTIF_ID, surveyCompatNotifBuilder.build());
        }
    }




    private void showFitbitNotif() {
        final Intent dashIntent = new Intent(this, FitbitMessageService.class);
        dashIntent.setAction(Constants.ACTION_CHECK_CONN);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setupNotifBuilder = new Notification.Builder(this, Constants.SURVEY_NOTIF_CHNL_ID);
            PendingIntent dashPendingIntent = PendingIntent.getForegroundService(this, 0, dashIntent, 0);
            setupNotifBuilder.setAutoCancel(false)
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark_normal)
                    .setContentTitle(CONTENT_TITLE_FITBIT)
                    .setContentText(Constants.CONN_STATUS)
                    .setGroup("Setup")
                    .setOngoing(true)
                    .setContentIntent(dashPendingIntent);
            notificationManager.notify(2, setupNotifBuilder.build());
        } else {
            PendingIntent dashPendingIntent = PendingIntent.getService(this, 0, dashIntent, 0);
            setupNotifCompatBuilder = new NotificationCompat.Builder(this, Constants.SURVEY_NOTIF_CHNL_ID);
            setupNotifCompatBuilder.setAutoCancel(false)
                    .setOngoing(true)
                    .setGroup("Setup")
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark_normal)
                    .setContentTitle(CONTENT_TITLE_FITBIT)
                    .setContentText(Constants.CONN_STATUS)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentInfo("info")
                    .setContentIntent(dashPendingIntent);
            notificationManager.notify(2, setupNotifCompatBuilder.build());
        }
    }


    public void notifySetup(String contentText) {
        PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, Constants.WAKELOCK_TAG);
        wl.acquire(6000);
        final Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        vibrator.vibrate(500);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setupNotifBuilder.setContentText(contentText);
            notificationManager.notify(Constants.SETUP_NOTIF_ID, setupNotifBuilder.build());
        } else {
            setupNotifCompatBuilder.setContentText(contentText);
            notificationManager.notify(Constants.SETUP_NOTIF_ID, setupNotifCompatBuilder.build());
        }
    }

    private void notifySurvey(boolean daily) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        createSurveyNotifChannel(daily);
        if(daily) {
            wakeUpAndVibrate(getApplicationContext(), Constants.DURATION_AWAKE, Constants.DURATION_VIBRATE);
            final Intent dashIntent = new Intent(this, MainActivity.class).setAction(Constants.ACTION_SHOW_MORNING);
            PendingIntent dashPendingIntent = PendingIntent.getActivity(this, 0, dashIntent, 0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                surveyNotifBuilder.setContentTitle(Constants.COMPLETE_SURVEY_TITLE);
                surveyNotifBuilder.setContentText(Constants.COMPLETE_SURVEY_CONTENT);
                surveyNotifBuilder.setContentIntent(dashPendingIntent);
                assert notificationManager != null;
                notificationManager.notify(Constants.SURVEY_NOTIF_ID, surveyNotifBuilder.build());
            }
            else {
                surveyCompatNotifBuilder.setContentTitle(Constants.COMPLETE_SURVEY_TITLE);
                surveyCompatNotifBuilder.setContentText(Constants.COMPLETE_SURVEY_CONTENT);
                surveyCompatNotifBuilder.setContentIntent(dashPendingIntent);
                assert notificationManager != null;
                notificationManager.notify(Constants.SURVEY_NOTIF_ID, surveyNotifBuilder.build());
            }
        }
        else {
            final Intent dashIntent = new Intent(this, MainActivity.class);
            PendingIntent dashPendingIntent = PendingIntent.getActivity(this, 0, dashIntent, 0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                surveyNotifBuilder.setContentTitle(Constants.SELF_REPORT_TITLE);
                surveyNotifBuilder.setContentText(Constants.SELF_REPORT_CONTENT);
                surveyNotifBuilder.setContentIntent(dashPendingIntent);
                assert notificationManager != null;
                notificationManager.notify(Constants.SURVEY_NOTIF_ID, surveyNotifBuilder.build());
            }
            else {
                surveyCompatNotifBuilder.setContentTitle(Constants.SELF_REPORT_TITLE);
                surveyCompatNotifBuilder.setContentText(Constants.SELF_REPORT_CONTENT);
                surveyCompatNotifBuilder.setContentIntent(dashPendingIntent);
                assert notificationManager != null;
                notificationManager.notify(Constants.SURVEY_NOTIF_ID, surveyCompatNotifBuilder.build());

            }

        }
    }

    public void createInterventionNotifChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(Constants.INTERVENTION_NOTIF_CHNL_ID, Constants.INTERVENTION_NOTIF_CHNL_NAME, NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.setDescription(Constants.INTERVENTION_NOTIF_CHNL_DESC);
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.enableVibration(false);
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(notificationChannel);
        }
    }

    public void createSurveyNotifChannel(boolean daily) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel;
            if(daily) {
                notificationChannel = new NotificationChannel(Constants.SURVEY_NOTIF_CHNL_ID, Constants.SURVEY_NOTIF_CHNL_NAME, NotificationManager.IMPORTANCE_HIGH);
                notificationChannel.enableLights(true);
                notificationChannel.setLightColor(Color.RED);
                notificationChannel.enableVibration(true);
            }
            else {
                notificationChannel = new NotificationChannel(Constants.SURVEY_NOTIF_CHNL_ID, Constants.SURVEY_NOTIF_CHNL_NAME, NotificationManager.IMPORTANCE_LOW);
                notificationChannel.enableLights(false);
                notificationChannel.enableVibration(false);
            }
            notificationChannel.setDescription(Constants.SURVEY_NOTIF_CHNL_DESC);
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            assert notificationManager != null;
            notificationManager.createNotificationChannel(notificationChannel);
        }
    }

    public void dismissIntervention() {
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(Constants.INTERVENTION_NOTIF_ID);
    }

    public void notifyUserWithInactivity(Context context, boolean snoozeOption) {
        wakeUpAndVibrate(context, Constants.DURATION_AWAKE, Constants.DURATION_VIBRATE);
        Intent dashIntent = new Intent(context, NotificationResponseActivity.class);
        if (snoozeOption)
            dashIntent.setAction(Constants.ACTION_SHOW_SNOOZE);
        PendingIntent dashPendingIntent = PendingIntent.getActivity(context, 0, dashIntent, 0);
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder monitorNotifBuilder = new Notification.Builder(context, Constants.INTERVENTION_NOTIF_CHNL_ID);
            monitorNotifBuilder.setAutoCancel(false)
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark_normal)
                    .setContentTitle("UPMC Dash Monitor")
                    .setContentText(Constants.NOTIF_INACTIVITY)
                    .setGroup("Prompt")
                    .setOngoing(true)
                    .setContentIntent(dashPendingIntent)
                    .setTimeoutAfter(INTERVENTION_TIMEOUT);
            mNotificationManager.notify(Constants.INTERVENTION_NOTIF_ID, monitorNotifBuilder.build());
        } else {
            NotificationCompat.Builder monitorNotifCompatBuilder = new NotificationCompat.Builder(getApplicationContext(), Constants.INTERVENTION_NOTIF_CHNL_ID)
                    .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark_normal)
                    .setContentTitle("UPMC Dash Monitor")
                    .setContentText(Constants.NOTIF_INACTIVITY)
                    .setOngoing(true)
                    .setContentIntent(dashPendingIntent)
                    .setGroup("Prompt")
                    .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setTimeoutAfter(INTERVENTION_TIMEOUT);
            mNotificationManager.notify(Constants.INTERVENTION_NOTIF_ID, monitorNotifCompatBuilder.build());
        }
    }


    public void notifyUserWithAppraisal() {
        wakeUpAndVibrate(getApplicationContext(), Constants.DURATION_AWAKE, Constants.DURATION_VIBRATE);
        Intent dashIntent = new Intent(this, FitbitMessageService.class).setAction(Constants.ACTION_APPRAISAL);
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder monitorNotifBuilder = new Notification.Builder(this, Constants.INTERVENTION_NOTIF_CHNL_ID);
            PendingIntent dashPendingIntent = PendingIntent.getForegroundService(this, 0, dashIntent, 0);
            monitorNotifBuilder.setAutoCancel(false)
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark_normal)
                    .setContentTitle("UPMC Dash Monitor")
                    .setContentText(Constants.NOTIF_APPRAISAL)
                    .setGroup("Prompt")
                    .setOngoing(true)
                    .setContentIntent(dashPendingIntent)
                    .setTimeoutAfter(INTERVENTION_TIMEOUT);
            assert mNotificationManager != null;
            mNotificationManager.notify(Constants.INTERVENTION_NOTIF_ID, monitorNotifBuilder.build());

        } else {
            PendingIntent dashPendingIntent = PendingIntent.getService(this, 0, dashIntent, 0);
            NotificationCompat.Builder monitorCompatNotifBuilder = new NotificationCompat.Builder(getApplicationContext(), Constants.INTERVENTION_NOTIF_CHNL_ID)
                    .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark_normal)
                    .setContentTitle("UPMC Dash Monitor")
                    .setContentText(Constants.NOTIF_APPRAISAL)
                    .setOngoing(true)
                    .setContentIntent(dashPendingIntent)
                    .setGroup("Prompt")
                    .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setTimeoutAfter(INTERVENTION_TIMEOUT);
            assert mNotificationManager != null;
            mNotificationManager.notify(Constants.INTERVENTION_NOTIF_ID, monitorCompatNotifBuilder.build());
        }
    }


    public void wakeUpAndVibrate(Context context, int duration_awake, int duration_vibrate) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        assert pm != null;
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, Constants.WAKELOCK_TAG);
        wl.acquire(duration_awake);
        final Vibrator vibrator = (Vibrator) context.getSystemService(VIBRATOR_SERVICE);
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

    private boolean isAppRunning(String app) {
        ActivityManager activityManager = (ActivityManager)
                this.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> serviceList = activityManager.getRunningServices(Integer.MAX_VALUE);
        for (ActivityManager.RunningServiceInfo service : serviceList) {
            if (service.process.equals(app)) return true;
        }
        return false;
    }

    private void launchApp(String app) {
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(app);
        if (launchIntent != null) {
            startActivity(launchIntent);
        }
    }


    private void setUpDatabase() {
        lighttpdStart(getApplicationContext(), "start_lighttpd");
        lighttpdAddHost(getApplicationContext(), "add_host", "localhost", "8001", "/storage/emulated/0/ksweb/tools/phpMyAdmin");
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                new CreateDB().execute();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        new CreateTables().execute();
                    }
                }, 1000);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        saveTimeSchedule();
                    }
                }, 3000);

            }
        }, 1000);
    }

    public static void lighttpdStart(Context context, String tag) {
        Intent intent = new Intent();
        intent.setAction(LIGHTTPD_START);
        intent.putExtra(TAG_KEY, tag);
        context.sendBroadcast(intent);
    }

    public static void lighttpdAddHost(Context context, String tag, String hostname, String port, String rootDir) {
        Intent intent = new Intent();
        intent.setAction(LIGHTTPD_ADD_HOST);
        intent.putExtra(TAG_KEY, tag);
        intent.putExtra(DATA_KEY, new String[]{hostname, port, rootDir});
        context.sendBroadcast(intent);
    }

    public void startFitbitCheckPromptAlarm() {
        AlarmManager myAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent alarmIntent_min = new Intent(getApplicationContext(), FitbitMessageService.class);
        alarmIntent_min.setAction(ACTION_CHECK_PROMPT);
        int interval = 60 * 1000;
        PendingIntent alarmPendingIntent_min = PendingIntent.getService(getApplicationContext(), 668, alarmIntent_min, 0);
        assert myAlarmManager != null;
        myAlarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), interval, alarmPendingIntent_min);
    }

    private void saveTimeSchedule() {
        // Yiyi's code here....
        StringBuilder sb = new StringBuilder();
        sb.append(zeroPad(Integer.valueOf(Aware.getSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_MORNING_HOUR))));
        sb.append(zeroPad(Integer.valueOf(Aware.getSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_MORNING_MINUTE))));
        Integer evening_hour = Integer.valueOf(Aware.getSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_NIGHT_HOUR));
        Integer evening_min = Integer.valueOf(Aware.getSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_NIGHT_MINUTE));
        sb.append(zeroPad(evening_hour));
        sb.append(zeroPad(evening_min));
        Log.d(Constants.TAG, "time schedule is: " + sb.toString());
        new PostData().execute(TABLE_TS, sb.toString());
        // now schedule a task to sync sensor data to aware every night after the patient's sleeping time
//        scheduleTimeForDataSyncing(evening_hour, evening_min);
    }



//    public void scheduleTimeForMorningSurvey() {
//        Log.d(Constants.TAG, "FitbitMessageService:scheduleTimeForMorningSurvey");
//        Aware.setSetting(this, Settings.STATUS_PLUGIN_UPMC_CANCER, true);
//        try {
//
//            String  className = getPackageName() + "/" + FitbitMessageService.class.getName();
//            if (Aware.getSetting(this, Settings.PLUGIN_UPMC_CANCER_MORNING_HOUR).length() == 0)
//                Aware.setSetting(this, Settings.PLUGIN_UPMC_CANCER_MORNING_HOUR, 9);
//
//            if (Aware.getSetting(this, Settings.PLUGIN_UPMC_CANCER_MORNING_MINUTE).length() == 0)
//                Aware.setSetting(this, Settings.PLUGIN_UPMC_CANCER_MORNING_MINUTE, 0);
//
//            int morning_hour = Integer.parseInt(Aware.getSetting(this, Settings.PLUGIN_UPMC_CANCER_MORNING_HOUR));
//            int morning_minute = Integer.parseInt(Aware.getSetting(this, Settings.PLUGIN_UPMC_CANCER_MORNING_MINUTE));
//            Scheduler.Schedule currentScheduler = Scheduler.getSchedule(getApplicationContext(), "cancer_survey_morning");
//            Log.d(Constants.TAG, "FitbitMessageService:scheduleTimeForMorningSurvey:schedule");
//            if (currentScheduler == null) {
//                Log.d(Constants.TAG, "FitbitMessageService:scheduleTimeForMorningSurvey:schedule");
//
//                Scheduler.Schedule schedule = new Scheduler.Schedule("cancer_survey_morning");
//                schedule.addHour(morning_hour)
//                        .addMinute(morning_minute)
//                        .setActionClass(className)
//                        .setActionIntentAction(Plugin.ACTION_CANCER_SURVEY)
//                        .setActionType(Scheduler.ACTION_TYPE_SERVICE);
//                Scheduler.saveSchedule(getApplicationContext(), schedule);
//                Aware.startScheduler(getApplicationContext()); //apply scheduler
//            } else {
//                Log.d(Constants.TAG, "FitbitMessageService:scheduleTimeForMorningSurvey:else part");
//                JSONArray hours = currentScheduler.getHours();
//                JSONArray minutes = currentScheduler.getMinutes();
//                boolean hour_changed = false;
//                boolean minute_changed = false;
//                for (int i = 0; i < hours.length(); i++) {
//                    if (hours.getInt(i) != morning_hour) {
//                        hour_changed = true;
//                        break;
//                    }
//                }
//                for (int i = 0; i < minutes.length(); i++) {
//                    if (minutes.getInt(i) != morning_minute) {
//                        minute_changed = true;
//                        break;
//                    }
//                }
//                if (hour_changed || minute_changed) {
//                    Scheduler.removeSchedule(getApplicationContext(), "cancer_survey_morning");
//                    Scheduler.Schedule schedule = new Scheduler.Schedule("cancer_survey_morning");
//                    schedule.addHour(morning_hour)
//                            .addMinute(morning_minute)
//                            .setActionClass(className)
//                            .setActionIntentAction(Plugin.ACTION_CANCER_SURVEY)
//                            .setActionType(Scheduler.ACTION_TYPE_SERVICE);
//                    Scheduler.saveSchedule(this, schedule);
//                    Aware.startScheduler(getApplicationContext()); //apply scheduler
//                }
//            }
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//    }

//
//    private void scheduleTimeForDataSyncing(Integer hour, Integer min) {
//        try {
//            Scheduler.Schedule data_syncing = Scheduler.getSchedule(getApplicationContext(), "data_syncing");
//            if (data_syncing == null) {
//                data_syncing = new Scheduler.Schedule("data_syncing");
//                data_syncing.addHour((hour + 1) % 24);
//                data_syncing.addMinute(min);
//                data_syncing.setActionType(Scheduler.ACTION_TYPE_SERVICE);
//                data_syncing.setActionIntentAction(Constants.ACTION_SYNC_DATA);
//                data_syncing.setActionClass(getPackageName() + "/" + FitbitMessageService.class.getName());
//                Scheduler.saveSchedule(getApplicationContext(), data_syncing);
//            } else if (data_syncing.getHours().getInt(0) != hour
//                    || data_syncing.getMinutes().getInt(0) != min) {
//                Scheduler.removeSchedule(getApplicationContext(), "data_syncing");
//                data_syncing = new Scheduler.Schedule("data_syncing");
//                data_syncing.addHour((hour + 1) % 24);
//                data_syncing.addMinute(min);
//                data_syncing.setActionType(Scheduler.ACTION_TYPE_SERVICE);
//                data_syncing.setActionIntentAction(Constants.ACTION_SYNC_DATA);
//                data_syncing.setActionClass(getPackageName() + "/" + FitbitMessageService.class.getName());
//                Scheduler.saveSchedule(getApplicationContext(), data_syncing);
//            }
//            Aware.startScheduler(getApplicationContext()); //apply scheduler
//
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//    }

    public void syncSCWithServer(long timeStamp, int type, int data) {
        ContentValues step_count = new ContentValues();
        step_count.put(Provider.Stepcount_Data.DEVICE_ID, Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
        step_count.put(Provider.Stepcount_Data.TIMESTAMP, timeStamp);
        step_count.put(Provider.Stepcount_Data.STEP_COUNT, data);
        step_count.put(Provider.Stepcount_Data.ALARM_TYPE, type);
        getContentResolver().insert(Provider.Stepcount_Data.CONTENT_URI, step_count);
        Log.d("yiyi", "Sent data to aware server");
    }

    private String zeroPad(Integer i) {
        StringBuilder sb = new StringBuilder();
        if (i < 10) {
            sb.append(0).append(i);
            return sb.toString();
        }
        return i.toString();
    }

    private class CheckConn extends AsyncTask<String, Void, Void> {


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }

        @Override
        protected Void doInBackground(String... strings) {
            Connection conn = null;
            Statement stmt = null;
            try {
                //Register JDBC driver
                Class.forName("com.mysql.jdbc.Driver");
                //Open a connection
                conn = DriverManager.getConnection(DB_URL, USER, PASS);
                stmt = conn.createStatement();
                Log.d("yiyi", "Connecting to database to check connection...");
                StringBuilder sql = new StringBuilder();
                sql.append("SELECT id, status FROM ");
                sql.append(TABLE_CONN);
                sql.append(" ORDER BY id DESC");
                ResultSet rs = stmt.executeQuery(sql.toString());
                if (rs.next()) {
                    int status = rs.getInt("status");
                    Log.d("yiyi", "Current connection status is: " + status);
                    if (status == 1) {
                        notifySetup(CONNECTED_WEAR);
                    } else {
                        notifySetup(FAILED_WEAR);
                    }
                }
                //Clean-up environment
                rs.close();
                stmt.close();
                conn.close();
            } catch (Exception e) {
//                launchApp(PACKAGE_KSWEB);
            } finally {
                try {
                    if (stmt != null)
                        stmt.close();
                } catch (SQLException se2) {
                }
                try {
                    if (conn != null)
                        conn.close();
                } catch (SQLException se) {
                    se.printStackTrace();
                }
            }
            return null;
        }
    }

    private class PostData extends AsyncTask<String, Void, Void> {


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }

        @Override
        protected Void doInBackground(String... strings) {
            Connection conn = null;
            Statement stmt = null;
            try {
                Class.forName("com.mysql.jdbc.Driver");
                conn = DriverManager.getConnection(DB_URL, USER, PASS);
                stmt = conn.createStatement();
//                Log.d("yiyi", "connection built. Trying to insert survey result to db.");
                StringBuilder sb = new StringBuilder();
                sb.append("INSERT INTO ");
                sb.append(strings[0]);
                sb.append(" VALUES (null, '");
                sb.append(strings[1]);
                sb.append("')");
                stmt.executeUpdate(sb.toString());
                Log.d("yiyi", "statement is: "+sb.toString());

            } catch (SQLException se) {
                Log.d("yiyi", "sql error");
                Log.d("yiyi",se.getMessage());
                Log.d("yiyi", se.getSQLState());
                //Handle errors for JDBC
                se.printStackTrace();
            } catch (Exception e) {
                //Handle errors for Class.forName
                Log.d("yiyi", "found exception");
                e.printStackTrace();
            } finally {
                try {
                    if (conn != null)
                        conn.close();
                    Log.d("yiyi", "PostData success!");
                } catch (SQLException se) {
                    se.printStackTrace();
                }//end finally try
            }//end try


            return null;

        }
    }

    private class CheckPrompt extends AsyncTask<String, Void, Void> {


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (id > promptCount) {
                Log.d("yiyi", "Prompt ID: " + id + ". Show new prompt on phone!!!");
                promptCount = id;
                if (message.equals(NOTIFICATION)) {
                    notifyUserWithInactivity(getApplicationContext(), true);
                } else if (message.equals(NOTIF_NO_SNOOZE)) {
                    notifyUserWithInactivity(getApplicationContext(), false);
                } else if (message.equals(MINIMESSAGE)) {
                    notifyUserWithAppraisal();
                } else if (message.equals(CLOSE_NOTIF)) {
                    dismissIntervention();
                } else if (message.equals(OTHER)) {
                    // show up a text input box for specified reasons
                }
            }
        }


        @Override
        protected Void doInBackground(String... strings) {
            Connection conn = null;
            Statement stmt = null;
            try {
                //Register JDBC driver
                Class.forName("com.mysql.jdbc.Driver");
                //Open a connection
                Log.d("yiyi", "Connecting to database to check new prompt...");
                conn = DriverManager.getConnection(DB_URL, USER, PASS);
                stmt = conn.createStatement();
                StringBuilder sql1 = new StringBuilder();
                sql1.append("SELECT id, message FROM ");
                sql1.append(TABLE_PROMPT);
                sql1.append(" ORDER BY id DESC");
                ResultSet rs = stmt.executeQuery(sql1.toString());
                if (rs.next()) {
                    id = rs.getInt("id");
                    message = rs.getString("message");
                }
                //Clean-up environment
                rs.close();
                stmt.close();
                conn.close();
            } catch (Exception e) {
//                launchApp(PACKAGE_KSWEB);
            } finally {
                try {
                    if (stmt != null)
                        stmt.close();
                } catch (SQLException se2) {
                }
                try {
                    if (conn != null)
                        conn.close();
                } catch (SQLException se) {
                    se.printStackTrace();
                }
            }
            return null;
        }
    }

    private class CreateDB extends AsyncTask<String, Void, Void> {


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }

        @Override
        protected Void doInBackground(String... strings) {
            Connection conn = null;
            Statement stmt = null;
            try {
                Class.forName(JDBC_DRIVER);
                conn = DriverManager.getConnection(HOST_URL, USER, PASS);
                stmt = conn.createStatement();
                String sql;
                sql = "DROP DATABASE IF EXISTS " + DB_NAME;
                stmt.executeUpdate(sql);
                sql = "CREATE DATABASE " + DB_NAME;
                stmt.executeUpdate(sql);
                Log.d("yiyi", "Database created successfully...");
            } catch (SQLException se) {
                se.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (stmt != null)
                        stmt.close();
                } catch (SQLException se2) {
                }// nothing we can do
                try {
                    if (conn != null)
                        conn.close();
                } catch (SQLException se) {
                    se.printStackTrace();
                }
            }
            return null;

        }
    }

    private class CreateTables extends AsyncTask<String, Void, Void> {


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }

        @Override
        protected Void doInBackground(String... strings) {
            Connection conn = null;
            Statement stmt = null;
            try {
                Class.forName(JDBC_DRIVER);
                conn = DriverManager.getConnection(DB_URL, USER, PASS);
                stmt = conn.createStatement();
                String sql = "CREATE TABLE Connection " +
                        "(id int(11) not NULL AUTO_INCREMENT, " +
                        " status int(11) not NULL, " +
                        " PRIMARY KEY ( id ))";
                stmt.executeUpdate(sql);
                sql = "CREATE TABLE PromptFromWatch " +
                        "(id int(11) not NULL AUTO_INCREMENT, " +
                        " message varchar(255) not NULL, " +
                        " PRIMARY KEY ( id ))";
                stmt.executeUpdate(sql);
                sql = "CREATE TABLE CommandFromPhone " +
                        "(id int(11) not NULL AUTO_INCREMENT, " +
                        " command varchar(255) not NULL, " +
                        " PRIMARY KEY ( id ))";
                stmt.executeUpdate(sql);
                sql = "CREATE TABLE PatientSurvey " +
                        "(id int(11) not NULL AUTO_INCREMENT, " +
                        " result varchar(255) not NULL, " +
                        " PRIMARY KEY ( id ))";
                stmt.executeUpdate(sql);
                sql = "CREATE TABLE SensorData " +
                        "(unixTime bigint(20) not NULL, " +
                        " type int(11) not NULL, " +
                        " data int(11) not NULL)";
                stmt.executeUpdate(sql);
                sql = "CREATE TABLE TimeSchedule " +
                        "(id int(11) not NULL AUTO_INCREMENT, " +
                        " timeRange varchar(255) not NULL, " +
                        " PRIMARY KEY ( id ))";
                stmt.executeUpdate(sql);
                sql = "CREATE TABLE Notification " +
                        "(unixTime bigint(20) not NULL, " +
                        " message varchar(255) not NULL)";
                stmt.executeUpdate(sql);
                sql = "CREATE TABLE InterventionResponse " +
                        "(unixTime bigint(20) not NULL, " +
                        " response varchar(255) not NULL)";
                stmt.executeUpdate(sql);
                sql = "CREATE TABLE ReasonsForNo " +
                        "(unixTime bigint(20) not NULL, " +
                        " reasons varchar(255) not NULL)";
                stmt.executeUpdate(sql);
                Log.d("yiyi", "Created tables in given database...");
            } catch (SQLException se) {
                se.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (stmt != null)
                        conn.close();
                } catch (SQLException se) {
                }
                try {
                    if (conn != null)
                        conn.close();
                } catch (SQLException se) {
                    se.printStackTrace();
                }
            }
            return null;

        }
    }


    /**
     * Private class to drop the communication table and recreate it again for reboot issues.
     */
    private class ResetTable extends AsyncTask<String, Void, Void> {


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }

        @Override
        protected Void doInBackground(String... strings) {
            Log.d("yiyi", "reset table is called!");
            Connection conn = null;
            Statement stmt = null;
            try {
                Class.forName(JDBC_DRIVER);
                conn = DriverManager.getConnection(DB_URL, USER, PASS);
                Log.d("yiyi", "Connected database successfully");
                stmt = conn.createStatement();
                String sql;
                sql = "DROP TABLE PromptFromWatch";
                stmt.executeUpdate(sql);
                Log.d("yiyi", "Table deleted!!!");
                sql = "CREATE TABLE PromptFromWatch " +
                        "(id int(11) not NULL AUTO_INCREMENT, " +
                        " message varchar(255) not NULL, " +
                        " PRIMARY KEY ( id ))";
                stmt.executeUpdate(sql);
                Log.d("yiyi", "Reset table in given database...");
            } catch (SQLException se) {
                se.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (stmt != null)
                        conn.close();
                } catch (SQLException se) {
                }
                try {
                    if (conn != null)
                        conn.close();
                } catch (SQLException se) {
                    se.printStackTrace();
                }
            }
            return null;

        }
    }

    private class SyncData extends AsyncTask<String, Void, Void> {


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }

        @Override
        protected Void doInBackground(String... strings) {
            Connection conn = null;
            Statement stmt = null;
            try {
                //Register JDBC driver
                Class.forName("com.mysql.jdbc.Driver");
                //Open a connection
                Log.d("yiyi", "Connecting to database to sync sensor data...");
                conn = DriverManager.getConnection(DB_URL, USER, PASS);
                stmt = conn.createStatement();
                StringBuilder sql = new StringBuilder();
                sql.append("SELECT unixTime, type, data FROM ");
                sql.append(TABLE_SENSOR_DATA);
                ResultSet rs = stmt.executeQuery(sql.toString());
                while (rs.next()) {
                    long timeStamp = rs.getLong("unixTime");
                    int type = rs.getInt("type");
                    int data = rs.getInt("data");
                    syncSCWithServer(timeStamp, type, data);
                }
                //After syncing all the data records, clear the table
                String command = "DROP TABLE SensorData";
                stmt.executeUpdate(command);
                Log.d("yiyi", "Table deleted!!!");
                command = "CREATE TABLE SensorData " +
                        "(unixTime bigint(20) not NULL, " +
                        " type int(11) not NULL, " +
                        " data int(11) not NULL)";
                stmt.executeUpdate(command);
                Log.d("yiyi", "Reset table SensorData");
                //Clean-up environment
                rs.close();
                stmt.close();
                conn.close();
            } catch (Exception e) {
            } finally {
                try {
                    if (stmt != null)
                        stmt.close();
                } catch (SQLException se2) {
                }
                try {
                    if (conn != null)
                        conn.close();
                } catch (SQLException se) {
                    se.printStackTrace();
                }
            }
            return null;

        }
    }
}
