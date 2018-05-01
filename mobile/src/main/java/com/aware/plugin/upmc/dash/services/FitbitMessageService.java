package com.aware.plugin.upmc.dash.services;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
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

import com.aware.plugin.upmc.dash.R;
import com.aware.plugin.upmc.dash.activities.NotificationResponseActivity;
import com.aware.plugin.upmc.dash.activities.UPMC;
import com.aware.plugin.upmc.dash.utils.Constants;
import com.aware.plugin.upmc.dash.utils.KSWEBControl;

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
import static com.aware.plugin.upmc.dash.utils.Constants.FAILED_WEAR;
import static com.aware.plugin.upmc.dash.utils.Constants.HOST_URL;
import static com.aware.plugin.upmc.dash.utils.Constants.INTERVENTION_TIMEOUT;
import static com.aware.plugin.upmc.dash.utils.Constants.JDBC_DRIVER;
import static com.aware.plugin.upmc.dash.utils.Constants.LIGHTTPD_ADD_HOST;
import static com.aware.plugin.upmc.dash.utils.Constants.LIGHTTPD_START;
import static com.aware.plugin.upmc.dash.utils.Constants.MINIMESSAGE;
import static com.aware.plugin.upmc.dash.utils.Constants.NOTIFICATION;
import static com.aware.plugin.upmc.dash.utils.Constants.NOTIF_NO_SNOOZE;
import static com.aware.plugin.upmc.dash.utils.Constants.PACKAGE_FITBIT;
import static com.aware.plugin.upmc.dash.utils.Constants.PACKAGE_KSWEB;
import static com.aware.plugin.upmc.dash.utils.Constants.PASS;
import static com.aware.plugin.upmc.dash.utils.Constants.SNOOZE_COMMAND;
import static com.aware.plugin.upmc.dash.utils.Constants.SURVEY_COMPLETED;
import static com.aware.plugin.upmc.dash.utils.Constants.TABLE_COMMAND;
import static com.aware.plugin.upmc.dash.utils.Constants.TABLE_CONN;
import static com.aware.plugin.upmc.dash.utils.Constants.TABLE_PROMPT;
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
                    Log.d(Constants.TAG, "mConnectivityReceiver: ether");
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
        if (!wifiManager.isWifiEnabled())
            wifiManager.setWifiEnabled(true);
    }

    public boolean enableBluetoothIfOff() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        boolean isEnabled = bluetoothAdapter.isEnabled();
        if (!isEnabled)
            return bluetoothAdapter.enable();
        return true;
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
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Intent upmc = new Intent(getApplicationContext(), UPMC.class);
                        startActivity(upmc);
                    }
                }, 3000);
                new ResetTable().execute();
                startFitbitCheckPromptAlarm();
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
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Intent upmc = new Intent(getApplicationContext(), UPMC.class);
                        startActivity(upmc);
                    }
                }, 3000);
                setUpDatabase();
                startFitbitCheckPromptAlarm();
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
                notifySurvey(SURVEY_COMPLETED);
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
//                sendMessageToWear(intentAction);
                Log.d(Constants.TAG, "FitbitMessageService:" + intentAction);
                dismissIntervention();
                break;
            default:
                return i;
        }
        return i;
    }

    private void showSurveyNotif() {
        final Intent dashIntent = new Intent(this, UPMC.class);
//        dashIntent.setAction(Constants.ACTION_SURVEY);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(Constants.SURVEY_NOTIF_CHNL_ID, "UPMC Dash", NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.setDescription("UPMC Dash Survey Notification");
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.enableVibration(true);
            notificationManager.createNotificationChannel(notificationChannel);
            Notification.Builder notificationBuilder = new Notification.Builder(this, Constants.SURVEY_NOTIF_CHNL_ID);
            PendingIntent dashPendingIntent = PendingIntent.getActivity(this, 0, dashIntent, 0);
            notificationBuilder.setAutoCancel(false)
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark_normal)
                    .setContentTitle("UPMC Dash Survey")
                    .setGroup("Survey")
                    .setContentText(Constants.COMPLETE_SURVEY)
                    .setContentIntent(dashPendingIntent);
            startForeground(Constants.SURVEY_NOTIF_ID, notificationBuilder.build());

        } else {
            PendingIntent dashPendingIntent = PendingIntent.getActivity(this, 0, dashIntent, 0);
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, Constants.SURVEY_NOTIF_CHNL_ID);
            notificationBuilder.setAutoCancel(false)
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark_normal)
                    .setContentTitle("UPMC Dash Survey")
                    .setContentText(Constants.COMPLETE_SURVEY)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setGroup("Survey")
                    .setContentInfo("Survey Notification")
                    .setContentIntent(dashPendingIntent);
            startForeground(Constants.SURVEY_NOTIF_ID, notificationBuilder.build());
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
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "Wake Up");
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

    public void notifySurvey(String contentText) {
        PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "Wake Up");
        wl.acquire(6000);
        final Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        vibrator.vibrate(500);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setupNotifBuilder.setContentText(contentText);
            notificationManager.notify(Constants.SURVEY_NOTIF_ID, setupNotifBuilder.build());
        } else {
            setupNotifCompatBuilder.setContentText(contentText);
            notificationManager.notify(Constants.SURVEY_NOTIF_ID, setupNotifCompatBuilder.build());
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
            mNotificationManager.notify(Constants.INTERVENTION_NOTIF_ID, monitorCompatNotifBuilder.build());
        }
    }


    public void wakeUpAndVibrate(Context context, int duration_awake, int duration_vibrate) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "Wake Up");
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
                new CreateTables().execute();
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
        myAlarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), interval, alarmPendingIntent_min);
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
                conn = DriverManager.getConnection(DB_NAME, USER, PASS);
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
                launchApp(PACKAGE_KSWEB);
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
                conn = DriverManager.getConnection(DB_NAME, USER, PASS);
                stmt = conn.createStatement();
//                Log.d("yiyi", "connection built. Trying to insert survey result to db.");
                StringBuilder sb = new StringBuilder();
                sb.append("INSERT INTO ");
                sb.append(strings[0]);
                sb.append(" VALUES (null, '");
                sb.append(strings[1]);
                sb.append("')");
                stmt.executeUpdate(sb.toString());
                Log.d("yiyi", "Inserted records into the table...");

            } catch (SQLException se) {
                //Handle errors for JDBC
                se.printStackTrace();
            } catch (Exception e) {
                //Handle errors for Class.forName
                e.printStackTrace();
            } finally {
                //finally block used to close resources
                try {
                    if (stmt != null)
                        conn.close();
                } catch (SQLException se) {
                }// do nothing
                try {
                    if (conn != null)
                        conn.close();
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
                conn = DriverManager.getConnection(DB_NAME, USER, PASS);
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
                launchApp(PACKAGE_KSWEB);
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
                        " message varchar(10) not NULL, " +
                        " PRIMARY KEY ( id ))";
                stmt.executeUpdate(sql);
                sql = "CREATE TABLE CommandFromPhone " +
                        "(id int(11) not NULL AUTO_INCREMENT, " +
                        " command varchar(10) not NULL, " +
                        " PRIMARY KEY ( id ))";
                stmt.executeUpdate(sql);
                sql = "CREATE TABLE PatientSurvey " +
                        "(id int(11) not NULL AUTO_INCREMENT, " +
                        " result varchar(10) not NULL, " +
                        " PRIMARY KEY ( id ))";
                stmt.executeUpdate(sql);
                sql = "CREATE TABLE SensorData " +
                        "(unixTime bigint(20) not NULL, " +
                        " type int(11) not NULL, " +
                        " data int(11) not NULL)";
                stmt.executeUpdate(sql);
                sql = "CREATE TABLE TimeSchedule " +
                        "(id int(11) not NULL AUTO_INCREMENT, " +
                        " timeRange varchar(10) not NULL, " +
                        " PRIMARY KEY ( id ))";
                stmt.executeUpdate(sql);
                sql = "CREATE TABLE Notification " +
                        "(unixTime bigint(20) not NULL, " +
                        " message varchar(10) not NULL)";
                stmt.executeUpdate(sql);
                sql = "CREATE TABLE InterventionResponse " +
                        "(unixTime bigint(20) not NULL, " +
                        " response varchar(10) not NULL)";
                stmt.executeUpdate(sql);
                sql = "CREATE TABLE ReasonsForNo " +
                        "(unixTime bigint(20) not NULL, " +
                        " reasons varchar(10) not NULL)";
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
            Connection conn = null;
            Statement stmt = null;
            try {
                Class.forName(JDBC_DRIVER);
                conn = DriverManager.getConnection(DB_URL, USER, PASS);
                stmt = conn.createStatement();
                String sql;
                sql = "DROP TABLE IF EXISTS PromptFromWatch";
                stmt.executeUpdate(sql);
                sql = "CREATE TABLE PromptFromWatch " +
                        "(id int(11) not NULL AUTO_INCREMENT, " +
                        " message varchar(10) not NULL, " +
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


}
