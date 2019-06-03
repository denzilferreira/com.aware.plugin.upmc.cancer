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
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.plugin.upmc.dash.R;
import com.aware.plugin.upmc.dash.activities.MainActivity;
import com.aware.plugin.upmc.dash.activities.NotificationResponseActivity;
import com.aware.plugin.upmc.dash.activities.Plugin;
import com.aware.plugin.upmc.dash.activities.Provider;
import com.aware.plugin.upmc.dash.settings.Settings;
import com.aware.plugin.upmc.dash.utils.Constants;
import com.aware.plugin.upmc.dash.utils.DBUtils;
import com.aware.plugin.upmc.dash.utils.LogFile;
import com.aware.plugin.upmc.dash.workers.LocalDBWorker;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
import static com.aware.plugin.upmc.dash.utils.Constants.TAG;
import static com.aware.plugin.upmc.dash.utils.Constants.TAG_KEY;
import static com.aware.plugin.upmc.dash.utils.Constants.USER;
import static com.aware.plugin.upmc.dash.utils.KSWEBControl.DATA_KEY;

public class FitbitMessageService extends Service {

    private Notification.Builder setupNotifBuilder;
    private NotificationCompat.Builder setupNotifCompatBuilder;
    private String session_id = "DEBUG";
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
                    Log.d(TAG, "FitbitMessageService:BluetoothReceiver:StateOff");
                    notifySetup(Constants.FAILED_WEAR_BLUETOOTH);
                    if (!enableBluetoothIfOff())
                        Toast.makeText(getApplicationContext(), "Bluetooth Error",
                                Toast.LENGTH_SHORT).show();
                    break;
                case BluetoothAdapter.STATE_TURNING_OFF:
                    Log.d(TAG, "FitbitMessageService:BluetoothReceiver:StateTurningOff");
                    BluetoothAdapter.getDefaultAdapter().enable();
                    notifySetup(Constants.FAILED_WEAR_BLUETOOTH);
                    break;
                case BluetoothAdapter.STATE_ON:
                    Log.d(TAG, "FitbitMessageService:BluetoothReceiver:StateOn");
                    notifySetup(BLUETOOTH_ON);
                    break;
                case BluetoothAdapter.STATE_TURNING_ON:
                    Log.d(TAG, "FitbitMessageService:BluetoothReceiver:StateTurningOn");
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
            Log.d(TAG, "Caught intent ");
            switch (state) {
                case ConnectivityManager.TYPE_BLUETOOTH:
                    Log.d(TAG, "mConnectivityReceiver: Blue");
                    break;
                case ConnectivityManager.TYPE_WIFI:
                    Log.d(TAG, "mConnectivityReceiver: Wifi");
                    break;
                case ConnectivityManager.TYPE_ETHERNET:
                    Log.d(TAG, "mConnecti vityReceiver: ether");
                    break;
                case ConnectivityManager.TYPE_MOBILE:
                    Log.d(TAG, "mConnectivityReceiver: mob");
                    break;
            }

        }
    };

    public static void lighttpdStart(Context context, String tag) {
        Intent intent = new Intent();
        intent.setAction(LIGHTTPD_START);
        intent.putExtra(TAG_KEY, tag);
        context.sendBroadcast(intent);
    }

    public static void lighttpdAddHost(Context context, String tag, String hostname, String port,
                                       String rootDir) {
        Intent intent = new Intent();
        intent.setAction(LIGHTTPD_ADD_HOST);
        intent.putExtra(TAG_KEY, tag);
        intent.putExtra(DATA_KEY, new String[]{hostname, port, rootDir});
        context.sendBroadcast(intent);
    }

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
        Log.d(TAG, "FitbitMessageService:onDestroy");
        stopForeground(true);
        unregisterReceiver(mBluetootLocalReceiver);
        unregisterReceiver(mConnectivityReceiver);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert mNotificationManager != null;
        mNotificationManager.cancelAll();
        stopSelf();
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "FitbitMessageService: onCreate");
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
        if (bluetoothAdapter != null) {
            boolean isEnabled = bluetoothAdapter.isEnabled();
            if (!isEnabled)
                return bluetoothAdapter.enable();
            return true;
        } else
            return false;
    }

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
        Log.d(TAG, "FitbitMessageService: onStartCommand " + intentAction);
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
                Log.d(TAG, "FitbitMessageService: onStartCommand first run");
                enableBluetoothIfOff();
                enableWifiIfOff();
                registerBluetoothReceiver();
                registerConnectivityReceiver();
                createInterventionNotifChannel();
                createSurveyNotifChannel();
                createSelfReportNotifChannel();
                createFitbitStatusNotifChannel();
                showSurveyNotif();
                showFitbitNotif();
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
                Log.d(TAG, "FitbitMessageService: onStartCommand check prompt repeatedly");
                new CheckPrompt().execute();
                startFitbitCheckPromptAlarm();
                break;
            case Constants.ACTION_CHECK_CONN:
                Log.d(TAG, "FitbitMessageService: onStartCommand check watch connection");
//                startActivity(new Intent(this, SetupLoadingActvity.class));
                new CheckConn().execute();
                break;
            case Constants.ACTION_SURVEY_COMPLETED:
                Log.d(TAG, "FitbitMessageService: onStartCommand notify survey completed");
                Log.d("yiyi", "action_survey_completed got called!");
                notifySurvey(false);
                setShowMorningSetting(false);
                break;
            case Plugin.ACTION_CANCER_SURVEY:
                try {
                    LogFile.writeToFile("FitbitMessageService: ACTION_CANCER_SURVEY");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Log.d(TAG, "FitbitMessageService:onStartCommand: ACTION_CANCER_SURVEY");
                // do not disturb mode forced to off.
                forceDndOffIfNeeded();
                notifySurvey(true);
                setShowMorningSetting(true);
                startFitbitCheckPromptAlarm();
                break;
            case Constants.ACTION_SETTINGS_CHANGED:
                Log.d(TAG, "FibitMessageService:onStartCommand : ACTION_SETTINGS_CHANGED");
                saveTimeSchedule();
                break;
            case Constants.ACTION_SYNC_DATA:
                try {
                    LogFile.writeToFile("FitbitMessageService: ACTION_SYNC_DATA");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Log.d(Constants.TAG, "FitbitMessageService:onCommand : ACTION_SYNC_DATA");
                cancelFitbitCheckPromptAlarm();
                enqueueOneTimeDBWorker();
                break;
            case Constants.ACTION_APPRAISAL:
                Log.d(TAG, "FitbitMessageService: onStartCommand appraisal");
                dismissIntervention();
                break;
            case Constants.ACTION_INACTIVITY:
                Log.d(TAG, "FitbitMessageService: onStartCommand : inactivity");
                startActivity(new Intent(this, NotificationResponseActivity.class));
                break;
            case Constants.ACTION_NOTIF_SNOOZE:
                new PostData().execute(TABLE_COMMAND, SNOOZE_COMMAND);
                dismissIntervention();
                saveResponseForOther(false);
                break;
            case Constants.ACTION_NOTIF_OK:
                new PostData().execute(TABLE_COMMAND, CLOSE_COMMAND);
                dismissIntervention();
                saveResponseForOther(true);
                break;
            case Constants.ACTION_NOTIF_NO:
                new PostData().execute(TABLE_COMMAND, CLOSE_COMMAND);
                Log.d(TAG, "FitbitMessageService:" + intentAction);
                saveResponseForNo(intent);
                dismissIntervention();
                break;
            case Constants.ACTION_DO_NOT_DISTURB:
                Log.d("yiyi", "FitbitMessageService:" + intentAction);
                String mode = Aware.getSetting(getApplicationContext(),
                        Settings.PLUGIN_UPMC_CANCER_DND_MODE);
                new PostData().execute(TABLE_COMMAND, mode.equals(
                        Constants.DND_MODE_ON) ? Constants.DO_NOT_DISTURB_COMMAND :
                        Constants.REMOVE_DO_NOT_DISTURB);
                break;
            case Constants.ACTION_TEST1:
                Log.d("yiyi", "FitbitMessageService:" + intentAction);
                new FakeData().execute();
                break;
            case Constants.ACTION_TEST2:
                Log.d("yiyi", "FitbitMessageService:" + intentAction);
                enqueueOneTimeDBWorker();
                break;
            case Constants.ACTION_TEST3:
                Log.d("yiyi", "FitbitMessageService:" + intentAction);
                notifyUserWithAppraisal("DEBUG");
                break;
            case Constants.ACTION_TEST4:
                Log.d("yiyi", "FitbitMessageService:" + intentAction);
                notifyUserWithInactivity(getApplicationContext(), true, "DEBUG");
                break;
            case Constants.ACTION_TEST5:
                Log.d("yiyi", "FitbitMessageService:" + intentAction);
                notifyUserWithInactivity(getApplicationContext(), false, "DEBUG");
                break;
            default:
                return i;
        }
        return i;
    }

    private void enqueueOneTimeDBWorker() {
        OneTimeWorkRequest localDbWorker = new OneTimeWorkRequest.Builder(LocalDBWorker.class).
                setInitialDelay(15, TimeUnit.MINUTES).
                addTag("LocalDBWorker").build();
        WorkManager.getInstance().enqueue(localDbWorker);
    }

    private void setShowMorningSetting(boolean b) {
        Aware.setSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_SHOW_MORNING,
                b ? Constants.SHOW_MORNING_SURVEY : Constants.SHOW_NORMAL_SURVEY);
    }

    public void forceDndOffIfNeeded() {
        if (Aware.getSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_DND_MODE)
                .equalsIgnoreCase(Constants.DND_MODE_ON)) {
            Aware.setSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_DND_MODE,
                    Constants.DND_MODE_OFF);
            saveDndAction(Constants.DND_MODE_OFF, Constants.DND_TOGGLE_AUTO);
        }
    }

    public void saveDndAction(String mode, int toggled_by) {
        ContentValues response = new ContentValues();
        response.put(Provider.Dnd_Toggle.DEVICE_ID,
                Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
        response.put(Provider.Dnd_Toggle.TIMESTAMP, System.currentTimeMillis());
        response.put(Provider.Dnd_Toggle.TOGGLE_POS, mode.equals(Constants.DND_MODE_ON) ? 1 : 0);
        response.put(Provider.Dnd_Toggle.TOGGLED_BY, toggled_by);
        getContentResolver().insert(Provider.Dnd_Toggle.CONTENT_URI, response);
        Log.d(Constants.TAG, "FitbitMessageService:saveDndAction");

    }

    public void saveResponseForNo(Intent intent) {
        String no_output = intent.getStringExtra(Constants.NOTIF_RESPONSE_EXTRA_KEY);
        Log.d(Constants.TAG, "FitbitMessageService:saveResponseForNo" + no_output);
        String resp = intent.getStringExtra(Constants.NOTIF_RESPONSE_EXTRA_KEY);
        char[] resp_array = resp.toCharArray();
        DBUtils.savePhoneResponse(getApplicationContext(), System.currentTimeMillis(), session_id,
                Integer.parseInt("" + resp_array[0]), Integer.parseInt("" + resp_array[1]),
                Integer.parseInt("" + resp_array[2]), Integer.parseInt("" + resp_array[3]),
                Integer.parseInt("" + resp_array[4]), resp.substring(5), 0, 1, 0);
//        ContentValues response = new ContentValues();
//        response.put(Provider.Notification_W_Responses.DEVICE_ID,
//                Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
//        response.put(Provider.Notification_W_Responses.TIMESTAMP, System.currentTimeMillis());
//        response.put(Provider.Notification_W_Responses.NOTIF_ID, session_id);
//        response.put(Provider.Notification_W_Responses.NOTIF_TYPE, Constants
//        .NOTIF_TYPE_INACTIVITY);
//        response.put(Provider.Notification_W_Responses.NOTIF_DEVICE, Constants
//        .NOTIF_DEVICE_PHONE);
//        response.put(Provider.Notification_W_Responses.RESP_OK, 0);
//        response.put(Provider.Notification_W_Responses.RESP_NO, 1);
//        response.put(Provider.Notification_W_Responses.RESP_SNOOZE, 0);
//        response.put(Provider.Notification_W_Responses.RESP_BUSY,
//                Integer.parseInt("" + resp_array[0]));
//        response.put(Provider.Notification_W_Responses.RESP_PAIN,
//                Integer.parseInt("" + resp_array[1]));
//        response.put(Provider.Notification_W_Responses.RESP_NAUSEA,
//                Integer.parseInt("" + resp_array[2]));
//        response.put(Provider.Notification_W_Responses.RESP_TIRED,
//                Integer.parseInt("" + resp_array[3]));
//        response.put(Provider.Notification_W_Responses.RESP_OTHER,
//                Integer.parseInt("" + resp_array[4]));
//        response.put(Provider.Notification_W_Responses.RESP_OTHER_SYMP, resp.substring(5));
//        getContentResolver().insert(Provider.Notification_W_Responses.CONTENT_URI, response);
    }

    public void saveResponseForOther(boolean ok) {
        Log.d(Constants.TAG, "FitbitMessageService:saveResponseForOther");
        DBUtils.savePhoneResponse(getApplicationContext(), System.currentTimeMillis(), session_id,
                Constants.NOTIF_DEVICE_PHONE, 0, 0, 0, 0, "", ok ? 1 : 0, 0, ok ? 0 : 1);
//
//        ContentValues response = new ContentValues();
//        response.put(Provider.Notification_W_Responses.DEVICE_ID,
//                Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
//        response.put(Provider.Notification_W_Responses.TIMESTAMP, System.currentTimeMillis());
//        response.put(Provider.Notification_W_Responses.NOTIF_ID, session_id);
//        response.put(Provider.Notification_W_Responses.NOTIF_TYPE, Constants
//        .NOTIF_TYPE_INACTIVITY);
//        response.put(Provider.Notification_W_Responses.NOTIF_DEVICE, Constants
//        .NOTIF_DEVICE_PHONE);
//        response.put(Provider.Notification_W_Responses.RESP_OK, ok ? 1 : 0);
//        response.put(Provider.Notification_W_Responses.RESP_NO, 0);
//        response.put(Provider.Notification_W_Responses.RESP_SNOOZE, ok ? 0 : 1);
//        response.put(Provider.Notification_W_Responses.RESP_BUSY, 0);
//        response.put(Provider.Notification_W_Responses.RESP_PAIN, 0);
//        response.put(Provider.Notification_W_Responses.RESP_NAUSEA, 0);
//        response.put(Provider.Notification_W_Responses.RESP_TIRED, 0);
//        response.put(Provider.Notification_W_Responses.RESP_OTHER, 0);
//        getContentResolver().insert(Provider.Notification_W_Responses.CONTENT_URI, response);
    }

    private void showSurveyNotif() {
        final Intent dashIntent = new Intent(this, MainActivity.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            surveyNotifBuilder = new Notification.Builder(this, Constants.SELF_REPORT_CHNL_ID);
            PendingIntent dashPendingIntent = PendingIntent.getActivity(this, 0, dashIntent, 0);
            surveyNotifBuilder.setAutoCancel(false).setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark_normal)
                    .setContentTitle(Constants.SELF_REPORT_TITLE).setGroup("Survey")
                    .setContentText(Constants.SELF_REPORT_CONTENT)
                    .setContentIntent(dashPendingIntent);
            startForeground(Constants.SURVEY_NOTIF_ID, surveyNotifBuilder.build());

        } else {
            PendingIntent dashPendingIntent = PendingIntent.getActivity(this, 0, dashIntent, 0);
            surveyCompatNotifBuilder =
                    new NotificationCompat.Builder(this, Constants.SELF_REPORT_CHNL_ID);
            surveyCompatNotifBuilder.setAutoCancel(false).setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark_normal)
                    .setContentTitle(Constants.SELF_REPORT_TITLE)
                    .setContentText(Constants.SELF_REPORT_CONTENT)
                    .setPriority(NotificationCompat.PRIORITY_HIGH).setGroup("Survey")
                    .setContentInfo("Survey Notification").setContentIntent(dashPendingIntent);
            startForeground(Constants.SURVEY_NOTIF_ID, surveyCompatNotifBuilder.build());
        }
    }

    private void showFitbitNotif() {
        final Intent dashIntent = new Intent(this, FitbitMessageService.class);
        dashIntent.setAction(Constants.ACTION_CHECK_CONN);
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setupNotifBuilder = new Notification.Builder(this, Constants.FITBIT_STATUS_CHNL_ID);
            PendingIntent dashPendingIntent =
                    PendingIntent.getForegroundService(this, 0, dashIntent, 0);
            setupNotifBuilder.setAutoCancel(false).setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark_normal)
                    .setContentTitle(CONTENT_TITLE_FITBIT).setContentText(Constants.CONN_STATUS)
                    .setGroup("Setup").setOngoing(true).setContentIntent(dashPendingIntent);
            assert notificationManager != null;
            notificationManager.notify(2, setupNotifBuilder.build());
        } else {
            PendingIntent dashPendingIntent = PendingIntent.getService(this, 0, dashIntent, 0);
            setupNotifCompatBuilder =
                    new NotificationCompat.Builder(this, Constants.FITBIT_STATUS_CHNL_ID);
            setupNotifCompatBuilder.setAutoCancel(false).setOngoing(true).setGroup("Setup")
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark_normal)
                    .setContentTitle(CONTENT_TITLE_FITBIT).setContentText(Constants.CONN_STATUS)
                    .setPriority(NotificationCompat.PRIORITY_HIGH).setContentInfo("info")
                    .setContentIntent(dashPendingIntent);
            assert notificationManager != null;
            notificationManager.notify(2, setupNotifCompatBuilder.build());
        }
    }

    public void notifySetup(String contentText) {
        PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
        assert pm != null;
        PowerManager.WakeLock wl = pm.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                Constants.WAKELOCK_TAG);
        wl.acquire(6000);
        final Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        assert vibrator != null;
        vibrator.vibrate(500);
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setupNotifBuilder.setContentText(contentText);
            assert notificationManager != null;
            notificationManager.notify(Constants.SETUP_NOTIF_ID, setupNotifBuilder.build());
        } else {
            setupNotifCompatBuilder.setContentText(contentText);
            assert notificationManager != null;
            notificationManager.notify(Constants.SETUP_NOTIF_ID, setupNotifCompatBuilder.build());
        }
    }

    private void notifySurvey(boolean daily) {
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (daily) {
//            wakeUpAndVibrate(getApplicationContext(), Constants.DURATION_AWAKE, Constants
//            .DURATION_VIBRATE);
            final Intent dashIntent =
                    new Intent(this, MainActivity.class).setAction(Constants.ACTION_SHOW_MORNING);
            PendingIntent dashPendingIntent = PendingIntent.getActivity(this, 0, dashIntent, 0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                surveyNotifBuilder = new Notification.Builder(this, Constants.SURVEY_NOTIF_CHNL_ID);
                surveyNotifBuilder.setAutoCancel(false).setWhen(System.currentTimeMillis())
                        .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark_normal)
                        .setContentTitle(Constants.COMPLETE_SURVEY_TITLE).setGroup("Survey")
                        .setContentText(Constants.COMPLETE_SURVEY_CONTENT)
                        .setContentIntent(dashPendingIntent);
                assert notificationManager != null;
                notificationManager.notify(Constants.SURVEY_NOTIF_ID, surveyNotifBuilder.build());
            } else {
                surveyCompatNotifBuilder =
                        new NotificationCompat.Builder(this, Constants.SURVEY_NOTIF_CHNL_ID);
                surveyCompatNotifBuilder.setAutoCancel(false).setWhen(System.currentTimeMillis())
                        .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark_normal)
                        .setContentTitle(Constants.COMPLETE_SURVEY_TITLE)
                        .setContentText(Constants.COMPLETE_SURVEY_CONTENT)
                        .setPriority(NotificationCompat.PRIORITY_HIGH).setGroup("Survey")
                        .setContentInfo("Survey Notification").setContentIntent(dashPendingIntent);
                assert notificationManager != null;
                notificationManager.notify(Constants.SURVEY_NOTIF_ID, surveyNotifBuilder.build());
            }
        } else {
            final Intent dashIntent = new Intent(this, MainActivity.class);
            PendingIntent dashPendingIntent = PendingIntent.getActivity(this, 0, dashIntent, 0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                surveyNotifBuilder = new Notification.Builder(this, Constants.SELF_REPORT_CHNL_ID);
                surveyNotifBuilder.setAutoCancel(false).setWhen(System.currentTimeMillis())
                        .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark_normal)
                        .setContentTitle(Constants.SELF_REPORT_TITLE).setGroup("Survey")
                        .setContentText(Constants.SELF_REPORT_CONTENT)
                        .setContentIntent(dashPendingIntent);
                assert notificationManager != null;
                notificationManager.notify(Constants.SURVEY_NOTIF_ID, surveyNotifBuilder.build());
            } else {
                surveyCompatNotifBuilder =
                        new NotificationCompat.Builder(this, Constants.SELF_REPORT_CHNL_ID);
                surveyCompatNotifBuilder.setAutoCancel(false).setWhen(System.currentTimeMillis())
                        .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark_normal)
                        .setContentTitle(Constants.SELF_REPORT_TITLE)
                        .setContentText(Constants.SELF_REPORT_CONTENT)
                        .setPriority(NotificationCompat.PRIORITY_HIGH).setGroup("Survey")
                        .setContentInfo("Survey Notification").setContentIntent(dashPendingIntent);
                assert notificationManager != null;
                notificationManager
                        .notify(Constants.SURVEY_NOTIF_ID, surveyCompatNotifBuilder.build());
            }

        }
    }

    public void createInterventionNotifChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel =
                    new NotificationChannel(Constants.INTERVENTION_NOTIF_CHNL_ID,
                            Constants.INTERVENTION_NOTIF_CHNL_NAME,
                            NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.setDescription(Constants.INTERVENTION_NOTIF_CHNL_DESC);
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            notificationChannel.enableVibration(false);
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            assert notificationManager != null;
            notificationManager.createNotificationChannel(notificationChannel);
        }
    }

    public void createSurveyNotifChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel notificationChannel;
            notificationChannel = new NotificationChannel(Constants.SURVEY_NOTIF_CHNL_ID,
                    Constants.SURVEY_NOTIF_CHNL_NAME, NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.setDescription(Constants.SURVEY_NOTIF_CHNL_DESC);
            notificationChannel.enableLights(true);
            notificationChannel.setVibrationPattern(
                    new long[]{0, 800, 100, 800, 100, 800, 100, 800, 100, 800});
            AudioAttributes audioAttributes =
                    new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
            notificationChannel
                    .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                            audioAttributes);
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.enableVibration(true);
            assert notificationManager != null;
            notificationManager.createNotificationChannel(notificationChannel);
        }
    }

    public void createSelfReportNotifChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel notificationChannel;
            notificationChannel = new NotificationChannel(Constants.SELF_REPORT_CHNL_ID,
                    Constants.SELF_REPORT_NOTIF_CHNL_NAME, NotificationManager.IMPORTANCE_LOW);
            notificationChannel.setDescription(Constants.SELF_REPORT_CHNL_DESC);
            notificationChannel.enableLights(false);
            notificationChannel.enableVibration(false);
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            assert notificationManager != null;
            notificationManager.createNotificationChannel(notificationChannel);

        }
    }

    public void createFitbitStatusNotifChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel notificationChannel;
            notificationChannel = new NotificationChannel(Constants.FITBIT_STATUS_CHNL_ID,
                    Constants.FITBIT_STATUS_CHNL_NAME, NotificationManager.IMPORTANCE_MIN);
            notificationChannel.setDescription(Constants.FITBIT_STATUS_CHNL_DESC);
            notificationChannel.enableLights(false);
            notificationChannel.enableVibration(false);
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            assert notificationManager != null;
            notificationManager.createNotificationChannel(notificationChannel);

        }
    }

    public void dismissIntervention() {
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(Constants.INTERVENTION_NOTIF_ID);
    }

    public void notifyUserWithInactivity(Context context, boolean snoozeOption, String session_id) {
        Log.d(TAG, "notifyUserWithInactivity");
        DBUtils.savePhoneIntervention(getApplicationContext(), System.currentTimeMillis(),
                session_id, Constants.NOTIF_TYPE_INACTIVITY, Constants.NOTIF_DEVICE_PHONE,
                snoozeOption ? Constants.SNOOZE_SHOWN : Constants.SNOOZE_NOT_SHOWN);
//        saveIntervention(session_id, Constants.NOTIF_TYPE_INACTIVITY, Constants
//        .NOTIF_DEVICE_PHONE,
//                snoozeOption ? Constants.SNOOZE_SHOWN : Constants.SNOOZE_NOT_SHOWN);
        wakeUpAndVibrate(context, Constants.DURATION_AWAKE, Constants.DURATION_VIBRATE);
        Intent dashIntent = new Intent(this, NotificationResponseActivity.class);
        if (snoozeOption)
            dashIntent.setAction(Constants.ACTION_SHOW_SNOOZE);
        PendingIntent dashPendingIntent = PendingIntent.getActivity(this, 0, dashIntent, 0);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder monitorNotifBuilder =
                    new Notification.Builder(this, Constants.INTERVENTION_NOTIF_CHNL_ID);
            monitorNotifBuilder.setAutoCancel(false)
                    .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark_normal)
                    .setContentTitle("UPMC Dash Monitor").setContentText(Constants.NOTIF_INACTIVITY)
                    .setOngoing(true).setContentIntent(dashPendingIntent)
                    .setTimeoutAfter(INTERVENTION_TIMEOUT);
            assert mNotificationManager != null;
            mNotificationManager
                    .notify(Constants.INTERVENTION_NOTIF_ID, monitorNotifBuilder.build());
        } else {
            NotificationCompat.Builder monitorNotifCompatBuilder =
                    new NotificationCompat.Builder(getApplicationContext(),
                            Constants.INTERVENTION_NOTIF_CHNL_ID)
                            .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark_normal)
                            .setContentTitle("UPMC Dash Monitor")
                            .setContentText(Constants.NOTIF_INACTIVITY).setOngoing(true)
                            .setContentIntent(dashPendingIntent).setGroup("Prompt").setSound(
                            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                            .setPriority(NotificationCompat.PRIORITY_MAX)
                            .setTimeoutAfter(INTERVENTION_TIMEOUT);
            assert mNotificationManager != null;
            mNotificationManager
                    .notify(Constants.INTERVENTION_NOTIF_ID, monitorNotifCompatBuilder.build());
        }
    }
//    public void saveIntervention(String notif_id, int notif_type, int notif_device,
//                                 int snooze_shown) {
//        ContentValues intervention = new ContentValues();
//        intervention.put(Provider.Notification_W_Interventions.DEVICE_ID,
//                Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
//        intervention
//                .put(Provider.Notification_W_Interventions.TIMESTAMP, System.currentTimeMillis());
//        intervention.put(Provider.Notification_W_Interventions.NOTIF_ID, notif_id);
//        intervention.put(Provider.Notification_W_Interventions.NOTIF_TYPE, notif_type);
//        intervention.put(Provider.Notification_W_Interventions.NOTIF_DEVICE, notif_device);
//        intervention.put(Provider.Notification_W_Interventions.SNOOZE_SHOWN, snooze_shown);
//        getContentResolver()
//                .insert(Provider.Notification_W_Interventions.CONTENT_URI, intervention);
//        Log.d(Constants.TAG, "saveIntervention:saving intervention");
//    }

    public void notifyUserWithAppraisal(String session_id) {
        Log.d(TAG, "FitbitMessageService:notifyUserWithAppraisal");
        DBUtils.savePhoneIntervention(getApplicationContext(), System.currentTimeMillis(),
                session_id, Constants.NOTIF_TYPE_APPRAISAL, Constants.NOTIF_DEVICE_PHONE,
                Constants.SNOOZE_NOT_SHOWN);
//        saveIntervention(session_id, Constants.NOTIF_TYPE_APPRAISAL, Constants.NOTIF_DEVICE_PHONE,
//                Constants.SNOOZE_NOT_SHOWN);
        wakeUpAndVibrate(getApplicationContext(), Constants.DURATION_AWAKE,
                Constants.DURATION_VIBRATE);
        Intent dashIntent =
                new Intent(this, FitbitMessageService.class).setAction(Constants.ACTION_APPRAISAL);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder monitorNotifBuilder =
                    new Notification.Builder(this, Constants.INTERVENTION_NOTIF_CHNL_ID);
            PendingIntent dashPendingIntent =
                    PendingIntent.getForegroundService(this, 0, dashIntent, 0);
            monitorNotifBuilder.setAutoCancel(false).setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark_normal)
                    .setContentTitle("UPMC Dash Monitor").setContentText(Constants.NOTIF_APPRAISAL)
                    .setGroup("Prompt").setOngoing(true).setContentIntent(dashPendingIntent)
                    .setTimeoutAfter(INTERVENTION_TIMEOUT);
            assert mNotificationManager != null;
            mNotificationManager
                    .notify(Constants.INTERVENTION_NOTIF_ID, monitorNotifBuilder.build());

        } else {
            PendingIntent dashPendingIntent = PendingIntent.getService(this, 0, dashIntent, 0);
            NotificationCompat.Builder monitorCompatNotifBuilder =
                    new NotificationCompat.Builder(getApplicationContext(),
                            Constants.INTERVENTION_NOTIF_CHNL_ID)
                            .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark_normal)
                            .setContentTitle("UPMC Dash Monitor")
                            .setContentText(Constants.NOTIF_APPRAISAL).setOngoing(true)
                            .setContentIntent(dashPendingIntent).setGroup("Prompt").setSound(
                            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                            .setPriority(NotificationCompat.PRIORITY_MAX)
                            .setTimeoutAfter(INTERVENTION_TIMEOUT);
            assert mNotificationManager != null;
            mNotificationManager
                    .notify(Constants.INTERVENTION_NOTIF_ID, monitorCompatNotifBuilder.build());
        }
    }

    public void wakeUpAndVibrate(Context context, int duration_awake, int duration_vibrate) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        assert pm != null;
        PowerManager.WakeLock wl = pm.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                Constants.WAKELOCK_TAG);
        wl.acquire(duration_awake);
        final Vibrator vibrator = (Vibrator) context.getSystemService(VIBRATOR_SERVICE);
        long[] pattern = {0, 800, 100, 800, 100, 800, 100, 800, 100, 800};
        assert vibrator != null;
        vibrator.vibrate(pattern, 0);
        Handler handler2 = new Handler();
        handler2.postDelayed(vibrator::cancel, duration_vibrate);
    }

    private boolean isAppRunning(String app) {
        ActivityManager activityManager =
                (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> serviceList =
                activityManager.getRunningServices(Integer.MAX_VALUE);
        for (ActivityManager.RunningServiceInfo service : serviceList) {
            if (service.process.equals(app))
                return true;
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
        lighttpdAddHost(getApplicationContext(), "add_host", "localhost", "8001",
                "/storage/emulated/0/ksweb/tools/phpMyAdmin");
        new Handler().postDelayed(() -> {
            new CreateDB().execute();
            new Handler().postDelayed(() -> new CreateTables().execute(), 1000);
            new Handler().postDelayed(() -> saveTimeSchedule(), 3000);

        }, 1000);
    }

    public void startFitbitCheckPromptAlarm() {
        AlarmManager myAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent alarmIntent_min = new Intent(getApplicationContext(), FitbitMessageService.class);
        alarmIntent_min.setAction(ACTION_CHECK_PROMPT);
        int interval = 60 * 1000;
        PendingIntent alarmPendingIntent_min =
                PendingIntent.getService(getApplicationContext(), 668, alarmIntent_min, 0);
        assert myAlarmManager != null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            myAlarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + interval, alarmPendingIntent_min);
        } else {
            myAlarmManager.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + interval,
                    alarmPendingIntent_min);
        }
    }

    public void cancelFitbitCheckPromptAlarm() {
        AlarmManager myAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent alarmIntent_min = new Intent(getApplicationContext(), FitbitMessageService.class);
        alarmIntent_min.setAction(ACTION_CHECK_PROMPT);
        PendingIntent alarmPendingIntent_min =
                PendingIntent.getService(getApplicationContext(), 668, alarmIntent_min, 0);
        assert myAlarmManager != null;
        myAlarmManager.cancel(alarmPendingIntent_min);
    }


    private void saveTimeSchedule() {
        // Yiyi's code here....
        StringBuilder sb = new StringBuilder();
        sb.append(zeroPad(Integer.valueOf(Aware.getSetting(getApplicationContext(),
                Settings.PLUGIN_UPMC_CANCER_MORNING_HOUR))));
        sb.append(zeroPad(Integer.valueOf(Aware.getSetting(getApplicationContext(),
                Settings.PLUGIN_UPMC_CANCER_MORNING_MINUTE))));
        Integer evening_hour = Integer.valueOf(
                Aware.getSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_NIGHT_HOUR));
        Integer evening_min = Integer.valueOf(Aware.getSetting(getApplicationContext(),
                Settings.PLUGIN_UPMC_CANCER_NIGHT_MINUTE));
        sb.append(zeroPad(evening_hour));
        sb.append(zeroPad(evening_min));
        Log.d(TAG, "time schedule is: " + sb.toString());
        new PostData().execute(TABLE_TS, sb.toString());
        // now schedule a task to sync sensor data to aware every night after the patient's
        // sleeping time
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
//            int morning_hour = Integer.parseInt(Aware.getSetting(this, Settings
//            .PLUGIN_UPMC_CANCER_MORNING_HOUR));
//            int morning_minute = Integer.parseInt(Aware.getSetting(this, Settings
//            .PLUGIN_UPMC_CANCER_MORNING_MINUTE));
//            Scheduler.Schedule currentScheduler = Scheduler.getSchedule(getApplicationContext()
//            , "cancer_survey_morning");
//            Log.d(Constants.TAG, "FitbitMessageService:scheduleTimeForMorningSurvey:schedule");
//            if (currentScheduler == null) {
//                Log.d(Constants.TAG,
//                "FitbitMessageService:scheduleTimeForMorningSurvey:schedule");
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
//                Log.d(Constants.TAG, "FitbitMessageService:scheduleTimeForMorningSurvey:else
//                part");
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
//            Scheduler.Schedule data_syncing = Scheduler.getSchedule(getApplicationContext(),
//            "data_syncing");
//            if (data_syncing == null) {
//                data_syncing = new Scheduler.Schedule("data_syncing");
//                data_syncing.addHour((hour + 1) % 24);
//                data_syncing.addMinute(min);
//                data_syncing.setActionType(Scheduler.ACTION_TYPE_SERVICE);
//                data_syncing.setActionIntentAction(Constants.ACTION_SYNC_DATA);
//                data_syncing.setActionClass(getPackageName() + "/" + FitbitMessageService.class
//                .getName());
//                Scheduler.saveSchedule(getApplicationContext(), data_syncing);
//            } else if (data_syncing.getHours().getInt(0) != hour
//                    || data_syncing.getMinutes().getInt(0) != min) {
//                Scheduler.removeSchedule(getApplicationContext(), "data_syncing");
//                data_syncing = new Scheduler.Schedule("data_syncing");
//                data_syncing.addHour((hour + 1) % 24);
//                data_syncing.addMinute(min);
//                data_syncing.setActionType(Scheduler.ACTION_TYPE_SERVICE);
//                data_syncing.setActionIntentAction(Constants.ACTION_SYNC_DATA);
//                data_syncing.setActionClass(getPackageName() + "/" + FitbitMessageService.class
//                .getName());
//                Scheduler.saveSchedule(getApplicationContext(), data_syncing);
//            }
//            Aware.startScheduler(getApplicationContext()); //apply scheduler
//
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//    }

    public void syncSCWithServer(double timeStamp, int type, int data, String sessionId) {
        ContentValues step_count = new ContentValues();
        step_count.put(Provider.Stepcount_Data.DEVICE_ID,
                Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
        step_count.put(Provider.Stepcount_Data.TIMESTAMP, timeStamp);
        step_count.put(Provider.Stepcount_Data.STEP_COUNT, data);
        step_count.put(Provider.Stepcount_Data.ALARM_TYPE, type);
        step_count.put(Provider.Stepcount_Data.SESSION_ID, sessionId);
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
                Log.d("yiyi", "statement is: " + sb.toString());

            } catch (SQLException se) {
                Log.d("yiyi", "sql error");
                Log.d("yiyi", se.getMessage());
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
                    notifyUserWithInactivity(getApplicationContext(), true, session_id);
                } else if (message.equals(NOTIF_NO_SNOOZE)) {
                    notifyUserWithInactivity(getApplicationContext(), false, session_id);
                } else if (message.equals(MINIMESSAGE)) {
                    notifyUserWithAppraisal(session_id);
                } else if (message.equals(CLOSE_NOTIF)) {
                    dismissIntervention();
                } else if (message.equals(OTHER)) {
                    dismissIntervention();
                    // TO DO(Raghu): show up a text input box for specified reasons
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
                sql1.append("SELECT * FROM ");
                sql1.append(TABLE_PROMPT);
                sql1.append(" ORDER BY id DESC");
                ResultSet rs = stmt.executeQuery(sql1.toString());
                if (rs.next()) {
                    id = rs.getInt("id");
                    message = rs.getString("message");
                    session_id = rs.getString("session_id");
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
                String sql =
                        "CREATE TABLE Connection " + "(id int(11) not NULL AUTO_INCREMENT, " + " "
                                + "status int(11) not NULL, " + " PRIMARY KEY ( id ))";
                stmt.executeUpdate(sql);
                sql =
                        "CREATE TABLE PromptFromWatch " + "(id int(11) not NULL AUTO_INCREMENT, " + " session_id varchar(255) NULL, " + " message varchar(255) not NULL, " + " PRIMARY KEY ( id ))";
                stmt.executeUpdate(sql);
                sql =
                        "CREATE TABLE CommandFromPhone " + "(id int(11) not NULL AUTO_INCREMENT, "
                                + " command varchar(255) not NULL, " + " PRIMARY KEY ( id ))";
                stmt.executeUpdate(sql);
                sql =
                        "CREATE TABLE PatientSurvey " + "(id int(11) not NULL AUTO_INCREMENT, " + " result varchar(255) not NULL, " + " PRIMARY KEY ( id ))";
                stmt.executeUpdate(sql);
                sql =
                        "CREATE TABLE SensorData " + "(timestamp double not NULL, " + " " +
                                "session_id varchar(255) NULL, " + " type int(11) not NULL, " +
                                " data int(11) not NULL)";
                stmt.executeUpdate(sql);
                sql =
                        "CREATE TABLE TimeSchedule " + "(id int(11) not NULL AUTO_INCREMENT, " +
                                " timeRange varchar(255) not NULL, " + " PRIMARY KEY ( id ))";
                stmt.executeUpdate(sql);
                sql =
                        "CREATE TABLE interventions_watch " + "(id int(11) not NULL " +
                                "AUTO_INCREMENT, " + " timestamp double not NULL, " + " " +
                                "session_id varchar(255) NULL, " + " notif_type int NOT NULL, " + " PRIMARY KEY ( id ))";
                stmt.executeUpdate(sql);
                sql =
                        "CREATE TABLE responses_watch " + "(id int(11) not NULL AUTO_INCREMENT, " + " timestamp double not NULL, " + " session_id varchar(255) NULL, " + " ok int NULL, " + " no int NULL, " + " snooze int NULL, " + " busy int NULL, " + " pain int NULL, " + " nausea int NULL, " + " tired int NULL, " + " other int NULL, " + " PRIMARY KEY ( id ))";
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
                sql =
                        "CREATE TABLE PromptFromWatch " + "(id int(11) not NULL AUTO_INCREMENT, " + " session_id varchar(255) NULL, " + " message varchar(255) not NULL, " + " PRIMARY KEY ( id ))";
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

    private class FakeData extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... strings) {
            Connection conn = null;
            Statement stmt = null;
            try {
                Class.forName("com.mysql.jdbc.Driver");
                //Open a connection
                Log.d("yiyi", "Connecting to database to ingest fake sensor data");
                conn = DriverManager.getConnection(DB_URL, USER, PASS);
                stmt = conn.createStatement();
                for (int i = 0; i < 1000; i++) {
                    // 1. Fake Step Count Data
                    StringBuilder sql = new StringBuilder();
                    String timestamp = "" + System.currentTimeMillis();
                    sql.append("INSERT into SensorData VALUES (");
                    sql.append(Double.valueOf(timestamp));
                    sql.append(", '1'");
                    sql.append(", 1");
                    sql.append(", 23)");
                    stmt.execute(sql.toString());
                }
                // 2. Interventions from watch
                for (int i = 0; i < 100; i++) {
                    StringBuilder sql = new StringBuilder();
                    String timestamp = "" + System.currentTimeMillis();
                    sql.append(
                            "INSERT into interventions_watch(notif_type, session_id, timestamp) " + "VALUES (");
                    sql.append("1");
                    sql.append(", '1'");
                    sql.append(",").append(Double.valueOf(timestamp)).append(")");
                    stmt.execute(sql.toString());
                }
                // 3. Interventions from watch
                for (int i = 0; i < 100; i++) {
                    StringBuilder sql = new StringBuilder();
                    String timestamp = "" + System.currentTimeMillis();
                    sql.append(
                            "INSERT into responses_watch(busy, nausea, no, ok, other, pain, " +
                                    "session_id, snooze, timestamp, tired) VALUES (");
                    sql.append("1");
                    sql.append(", 1");
                    sql.append(", 1");
                    sql.append(", 1");
                    sql.append(", '1'");
                    sql.append(", 1");
                    sql.append(", '1'");
                    sql.append(", 1");
                    sql.append(",").append(Double.valueOf(timestamp));
                    sql.append(", 1").append(")");
                    stmt.execute(sql.toString());
                }
                stmt.close();
                conn.close();

            } catch (Exception e) {
            }
            return null;
        }
    }

    private class SyncData extends AsyncTask<String, Void, Void> {


        @Override
        protected void onPreExecute() {
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
                sql.append("SELECT * FROM ");
                sql.append(TABLE_SENSOR_DATA);
                ResultSet rs = stmt.executeQuery(sql.toString());
                while (rs.next()) {
                    double timeStamp = rs.getDouble("timestamp");
                    int type = rs.getInt("type");
                    int data = rs.getInt("data");
                    String sessionId = rs.getString("session_id");
                    syncSCWithServer(timeStamp, type, data, sessionId);
                }
                //After syncing all the data records, clear the table
                String command = "DROP TABLE SensorData";
                stmt.executeUpdate(command);
                Log.d("yiyi", "Table deleted!!!");
                command =
                        "CREATE TABLE SensorData " + "(timestamp double not NULL, " + " " +
                                "session_id varchar(255) NULL, " + " type int(11) not NULL, " +
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
