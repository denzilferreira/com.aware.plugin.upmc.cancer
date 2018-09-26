package com.aware.plugin.upmc.dash.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;
import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.plugin.upmc.dash.R;
import com.aware.plugin.upmc.dash.activities.NotificationResponseActivity;
import com.aware.plugin.upmc.dash.activities.Plugin;
import com.aware.plugin.upmc.dash.activities.Provider;
import com.aware.plugin.upmc.dash.activities.SetupLoadingActvity;
import com.aware.plugin.upmc.dash.activities.UPMC;
import com.aware.plugin.upmc.dash.fileutils.SyncFilesParams;
import com.aware.plugin.upmc.dash.fileutils.SyncFilesResponse;
import com.aware.plugin.upmc.dash.fileutils.SyncFilesTask;
import com.aware.plugin.upmc.dash.settings.Settings;
import com.aware.plugin.upmc.dash.utils.Constants;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.CapabilityClient;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.List;
import java.util.Set;

/**
 * Created by RaghuTeja on 6/23/17.
 */

public class MessageService extends WearableListenerService implements
        MessageClient.OnMessageReceivedListener,
        CapabilityClient.OnCapabilityChangedListener,
        DataClient.OnDataChangedListener,
        SyncFilesResponse{

    private MessageClient messageClient;
    private CapabilityClient capabilityClient;
    private DataClient dataClient;
    private boolean isWearAround = false;
    private String prevNotif = "Tap to setup your watch";
    private Notification.Builder setupNotifBuilder;
    private NotificationCompat.Builder setupNotifCompatBuilder;
    private Notification.Builder surveyNotifBuilder;
    private NotificationCompat.Builder surveyCompatNotifBuilder;
    private int count = 0;
    private BroadcastReceiver mBluetootLocalReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            switch (state) {
                case BluetoothAdapter.STATE_OFF:
                    Log.d(Constants.TAG, "MessageService:BluetoothReceiver:STATE_OFF");
                    notifySetup(Constants.FAILED_WEAR_BLUETOOTH);
                    if (!enableBluetoothIfOff())
                        Toast.makeText(getApplicationContext(), "Bluetooth Error", Toast.LENGTH_SHORT).show();
                    break;
                case BluetoothAdapter.STATE_TURNING_OFF:
                    Log.d(Constants.TAG, "MessageService:BluetoothReceiver:STATE_TURNING_OFF");
                    break;
                case BluetoothAdapter.STATE_ON:
                    Log.d(Constants.TAG, "MessageService:BluetoothReceiver:STATE_ON");
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            scanWear();
                        }
                    }, 10000);
                    break;
                case BluetoothAdapter.STATE_TURNING_ON:
                    Log.d(Constants.TAG, "MessageService:BluetoothReceiver:STATE_TURNING_ON");
                    break;
            }

        }
    };
    private BroadcastReceiver mConnectivityReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
//            int state = intent.getIntExtra(ConnectivityManager.EXTRA_NETWORK_TYPE, -1);
            enableWifiIfOff();
//            Log.d(Constants.TAG, "Caught intent ");
//            switch (state) {
//                case ConnectivityManager.TYPE_BLUETOOTH:
//                    Log.d(Constants.TAG, "mConnectivityReceiver: TYPE_BLUETOOTH");
//                    break;
//                case ConnectivityManager.TYPE_WIFI:
//                    Log.d(Constants.TAG, "mConnectivityReceiver: TYPE_WIFI");
//                    break;
//                case ConnectivityManager.TYPE_ETHERNET:
//                    Log.d(Constants.TAG, "mConnectivityReceiver: TYPE_ETHERNET");
//                    break;
//
//                case ConnectivityManager.TYPE_MOBILE:
//                    Log.d(Constants.TAG, "mConnectivityReceiver: TYPE_MOBILE");
//                    break;
//
//            }

        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final int i = super.onStartCommand(intent, flags, startId);
        Log.d(Constants.TAG, "MessageService: onStartCommand");
        String intentAction = null;
        if (intent != null)
            intentAction = intent.getAction();
        if (intentAction == null)
            return i;
        switch (intentAction) {
            case Constants.ACTION_FIRST_RUN:
                Log.d(Constants.TAG, "MessageService: onStartCommand : ACTION_FIRST_RUN");
                enableBluetoothIfOff();
                enableWifiIfOff();
                registerBluetoothReceiver();
                registerConnectivityReceiver();
                showSurveyNotif();
                showSetupNotif();
                createInterventionNotifChannel();
                syncSCWithServer();
                break;
            case Constants.ACTION_REBOOT:
                Log.d(Constants.TAG, "MessageService: onStartCommand : ACTION_REBOOT");
                enableBluetoothIfOff();
                enableWifiIfOff();
                registerBluetoothReceiver();
                registerConnectivityReceiver();
                showSurveyNotif();
                showSetupNotif();
                createInterventionNotifChannel();
                if (isWearNodeSaved())
                    scanWear();
            case Constants.ACTION_INIT:
                Log.d(Constants.TAG, "MessageService: onStartCommand : ACTION_INIT");
                if(isWearInitializable()) {
                    initializeWearSettings();
                }
                else {
                    Log.d(Constants.TAG, "MessageService: not enough information to start logging");
                }
                break;
            case Constants.ACTION_SETUP_WEAR:
                Log.d(Constants.TAG, "MessageService: onStartCommand : ACTION_SETUP_WEAR");
                startActivity(new Intent(this, SetupLoadingActvity.class));
                initiateWearSetup();
                break;
            case Constants.ACTION_APPRAISAL:
                Log.d(Constants.TAG, "MessageService: onStartCommand ACTION_APPRAISAL");
                dismissIntervention();
                break;
            case Constants.ACTION_INACTIVITY:
                Log.d(Constants.TAG, "MessageService: onStartCommand : ACTION_INACTIVITY");
                startActivity(new Intent(this, NotificationResponseActivity.class));
                break;
            case Constants.ACTION_VICINITY:
                Log.d(Constants.TAG, "MessageService: onStartCommand : ACTION_VICINITY");
                scanWear();
                break;
            case Constants.ACTION_SETTINGS_CHANGED:
                Log.d(Constants.TAG, "MessageService:onStartCommand : ACTION_SETTINGS_CHANGED");
                changeWearSettings();
                break;
//            case Constants.ACTION_SNOOZE_ALARM:
//                Log.d(Constants.TAG, "MessageService:onStartCommand : ACTION_SNOOZE_ALARM");
//                Log.d(Constants.TAG, "MessageService:" + intentAction);
//                notifyUserWithInactivity(false);
//                break;
            case Constants.ACTION_NOTIF_SNOOZE:
            case Constants.ACTION_NOTIF_OK:
            case Constants.ACTION_NOTIF_NO:
                sendMessageToWear(intentAction);
                Log.d(Constants.TAG, "MessageService:" + intentAction);
                dismissIntervention();
                break;
            case Plugin.ACTION_CANCER_SURVEY:
                Log.d(Constants.TAG, "MessageService:onStartCommand: ACTION_CANCER_SURVEY");
                notifySurvey(true);
                break;
            case Constants.ACTION_SURVEY_COMPLETED:
                Log.d(Constants.TAG, "MessageService:onStartCommand: ACTION_SURVEY_COMPLETED");
                notifySurvey(false);
                break;
            case Constants.ACTION_STOP_SELF:
                Log.d(Constants.TAG, "MessageService:onStartCommand: ACTION_STOP_SELF");
                stopForeground(true);
                unregisterReceiver(mBluetootLocalReceiver);
                unregisterReceiver(mConnectivityReceiver);
                messageClient.removeListener(this);
                capabilityClient.removeListener(this);
                capabilityClient.removeLocalCapability(Constants.CAPABILITY_PHONE_APP);
                dataClient.removeListener(this);
                break;
            default:
                return i;
        }
        return i;
    }


    public void setupMorningSruveySchedule() {


    }



    public void syncSCWithServer(){
        ContentValues step_count = new ContentValues();
        step_count.put(Provider.Stepcount_Data.DEVICE_ID, Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
        step_count.put(Provider.Stepcount_Data.TIMESTAMP, System.currentTimeMillis());
        step_count.put(Provider.Stepcount_Data.STEP_COUNT, 15);
        step_count.put(Provider.Stepcount_Data.ALARM_TYPE, 0);
        getContentResolver().insert(Provider.Stepcount_Data.CONTENT_URI, step_count);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Wearable.WearableOptions options = new Wearable.WearableOptions.Builder().setLooper(Looper.myLooper()).build();
        messageClient = Wearable.getMessageClient(this, options);
        capabilityClient = Wearable.getCapabilityClient(this, options);
        dataClient = Wearable.getDataClient(this, options);
        messageClient.addListener(this, Constants.MESSAGE_URI, MessageClient.FILTER_PREFIX);
        capabilityClient.addListener(this, Constants.CAPABILITY_WEAR_APP);
        dataClient.addListener(this);
        capabilityClient.addLocalCapability(Constants.CAPABILITY_PHONE_APP);
        Log.d(Constants.TAG, "MessageService: onCreate");
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

    public void registerBluetoothReceiver() {
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mBluetootLocalReceiver, filter);
    }

    public void registerConnectivityReceiver() {
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mConnectivityReceiver, filter);
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

    public void enableWifiIfOff() {
        WifiManager wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled())
            wifiManager.setWifiEnabled(true);
    }

    private void saveConnectedWearNode() {
        setWearAround(false);
        capabilityClient.getCapability(Constants.CAPABILITY_WEAR_APP, CapabilityClient.FILTER_REACHABLE).addOnCompleteListener(new OnCompleteListener<CapabilityInfo>() {
            @Override
            public void onComplete(@NonNull Task<CapabilityInfo> task) {
                Log.d(Constants.TAG, "onComplete");
            }
        })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(Constants.TAG, "onFailure");
                    }
                })
                .addOnSuccessListener(new OnSuccessListener<CapabilityInfo>() {
                    @Override
                    public void onSuccess(CapabilityInfo capabilityInfo) {
                        Log.d(Constants.TAG, "onSuccess");
                        Set<Node> nodes = capabilityInfo.getNodes();
                        Node wearNode = getConnectedWearNode(nodes);
                        if (wearNode == null) {
                            Log.d(Constants.TAG, "saveConnectedWearNode: no wear nodes found");
                        } else {
                            Log.d(Constants.TAG, "saveConnectedWearNode: wear found with ID :" + wearNode.getId());
                            writeWearNodeId(wearNode.getId());
                            sendMessageToWear(Constants.ACTION_SETUP_WEAR);
                        }
                    }
                });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(Constants.TAG, "MessageService:onDestroy");
        stopForeground(true);
        unregisterReceiver(mBluetootLocalReceiver);
        unregisterReceiver(mConnectivityReceiver);
        messageClient.removeListener(this);
        capabilityClient.removeListener(this);
        capabilityClient.removeLocalCapability(Constants.CAPABILITY_PHONE_APP);
        dataClient.removeListener(this);
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancelAll();
        stopSelf();
    }

    @Override
    public void onMessageReceived(@NonNull MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);
        byte[] input = messageEvent.getData();
        String message = new String(input);
        Log.d(Constants.TAG, "MessageService: onMessageReceived: " + message + " " + count);
        count++;
        setWearAround(true);
        if (messageEvent.getPath().equals(Constants.MESSAGE_URI.toString())) {
            switch (message) {
                case Constants.ACK:
                    Log.d(Constants.TAG, "MessageService:onMessageReceived:ACK");
                    break;
                case Constants.STATE_LOGGING:
                    Log.d(Constants.TAG, "MessageService:onMessageReceived:STATUS_LOGGING");
                    notifySetup(Constants.CONNECTED_WEAR);
                    break;
                case Constants.STATE_INIT:
                    Log.d(Constants.TAG, "MessageService:onMessageReceived:STATUS_INIT");
                    notifySetup(Constants.CONNECTED_WEAR);
                    break;
                case Constants.NOTIFY_INACTIVITY:
                    sendAckToWear();
                    Log.d(Constants.TAG, "MessageService:onMessageReceived:NOTIFY_INACTIVITY");
                    notifyUserWithInactivity(true);
                    break;
                case Constants.NOTIFY_GREAT_JOB:
                    sendAckToWear();
                    Log.d(Constants.TAG, "MessageService:onMessageReceived:NOTIFY_GREAT_JOB");
                    notifyUserWithAppraisal();
                    break;
                case Constants.ACTION_NOTIF_NO:
                    showInabilityResponseDialog();
                case Constants.ACTION_NOTIF_OK:
                case Constants.ACTION_NOTIF_SNOOZE:
                    sendAckToWear();
                    dismissIntervention();
                    Log.d(Constants.TAG, "MessageService:onMessageReceived " + message);
                    break;
                case Constants.NOTIFY_INACTIVITY_SNOOZED:
                    sendAckToWear();
                    notifyUserWithInactivity(false);
                    break;
                case Constants.ACTION_SYNC_SETTINGS:
                    sendAckToWear();
                    syncWearSettings();
                    break;
            }
        }
    }

    public void showInabilityResponseDialog() {
        Intent intent = new Intent(this, NotificationResponseActivity.class).setAction(Constants.ACTION_SHOW_INABILITY);
        startActivity(intent);
    }

    public boolean isWearInitializable() {

        final Context context = getApplicationContext();
        if(Aware.getSetting(context, Settings.PLUGIN_UPMC_CANCER_SYMPTOM_SEVERITY).length()==0)
            return false;
        boolean morning_hour = Integer.parseInt(Aware.getSetting(context, Settings.PLUGIN_UPMC_CANCER_MORNING_HOUR)) != -1;
        boolean morning_minute = Integer.parseInt(Aware.getSetting(context, Settings.PLUGIN_UPMC_CANCER_MORNING_MINUTE)) != -1;
        boolean night_hour = Integer.parseInt(Aware.getSetting(context, Settings.PLUGIN_UPMC_CANCER_NIGHT_HOUR)) != -1;
        boolean night_minute = Integer.parseInt(Aware.getSetting(context, Settings.PLUGIN_UPMC_CANCER_NIGHT_MINUTE)) != -1;
        boolean severity = Integer.parseInt(Aware.getSetting(context, Settings.PLUGIN_UPMC_CANCER_SYMPTOM_SEVERITY)) != -1;
        return (morning_hour && morning_minute && night_hour && night_minute && severity);
    }

    public void notifyUserWithAppraisal() {
        wakeUpAndVibrate(Constants.DURATION_AWAKE, Constants.DURATION_VIBRATE);
        Intent dashIntent = new Intent(this, MessageService.class).setAction(Constants.ACTION_APPRAISAL);
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
                    .setTimeoutAfter(Constants.INTERVENTION_TIMEOUT)
                    .setOngoing(true)
                    .setContentIntent(dashPendingIntent);
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
                    .setTimeoutAfter(Constants.INTERVENTION_TIMEOUT)
                    .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                    .setPriority(NotificationCompat.PRIORITY_MAX);
            mNotificationManager.notify(Constants.INTERVENTION_NOTIF_ID, monitorCompatNotifBuilder.build());
        }
    }

    public void notifyUserWithInactivity(boolean snoozeOption) {
        wakeUpAndVibrate(Constants.DURATION_AWAKE, Constants.DURATION_VIBRATE);
        Intent dashIntent = new Intent(this, NotificationResponseActivity.class);
        if (snoozeOption)
            dashIntent.setAction(Constants.ACTION_SHOW_SNOOZE);
        PendingIntent dashPendingIntent = PendingIntent.getActivity(this, 0, dashIntent, 0);
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder monitorNotifBuilder = new Notification.Builder(this, Constants.INTERVENTION_NOTIF_CHNL_ID);
            monitorNotifBuilder.setAutoCancel(false)
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark_normal)
                    .setContentTitle("UPMC Dash Monitor")
                    .setContentText(Constants.NOTIF_INACTIVITY)
                    .setGroup("Prompt")
                    .setTimeoutAfter(Constants.INTERVENTION_TIMEOUT)
                    .setOngoing(true)
                    .setContentIntent(dashPendingIntent);
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
                    .setTimeoutAfter(Constants.INTERVENTION_TIMEOUT)
                    .setPriority(NotificationCompat.PRIORITY_MAX);
            mNotificationManager.notify(Constants.INTERVENTION_NOTIF_ID, monitorNotifCompatBuilder.build());
        }
    }

    public void initializeWearSettings() {
        StringBuilder initBuilder = new StringBuilder();
        initBuilder.append(Constants.ACTION_INIT);
        initBuilder.append(" ");
        initBuilder.append(Aware.getSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_MORNING_HOUR));
        initBuilder.append(" ");
        initBuilder.append(Aware.getSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_MORNING_MINUTE));
        initBuilder.append(" ");
        initBuilder.append(Aware.getSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_NIGHT_HOUR));
        initBuilder.append(" ");
        initBuilder.append(Aware.getSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_NIGHT_MINUTE));
        initBuilder.append(" ");
        initBuilder.append(Aware.getSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_SYMPTOM_SEVERITY));
        Log.d(Constants.TAG, "MessageService:initializeWear: " + initBuilder.toString());
        sendMessageToWear(initBuilder.toString());
    }


    public void changeWearSettings () {
        StringBuilder initBuilder = new StringBuilder();
        initBuilder.append(Constants.ACTION_SETTINGS_CHANGED);
        initBuilder.append(" ");
        initBuilder.append(Aware.getSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_MORNING_HOUR));
        initBuilder.append(" ");
        initBuilder.append(Aware.getSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_MORNING_MINUTE));
        initBuilder.append(" ");
        initBuilder.append(Aware.getSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_NIGHT_HOUR));
        initBuilder.append(" ");
        initBuilder.append(Aware.getSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_NIGHT_MINUTE));
        initBuilder.append(" ");
        initBuilder.append(Aware.getSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_SYMPTOM_SEVERITY));
        Log.d(Constants.TAG, "MessageService:changeWearSettings: " + initBuilder.toString());
        sendMessageToWear(initBuilder.toString());
    }


    public void syncWearSettings () {
        StringBuilder initBuilder = new StringBuilder();
        initBuilder.append(Constants.ACTION_SYNC_SETTINGS);
        initBuilder.append(" ");
        initBuilder.append(Aware.getSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_MORNING_HOUR));
        initBuilder.append(" ");
        initBuilder.append(Aware.getSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_MORNING_MINUTE));
        initBuilder.append(" ");
        initBuilder.append(Aware.getSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_NIGHT_HOUR));
        initBuilder.append(" ");
        initBuilder.append(Aware.getSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_NIGHT_MINUTE));
        initBuilder.append(" ");
        initBuilder.append(Aware.getSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_SYMPTOM_SEVERITY));
        Log.d(Constants.TAG, "MessageService:syncWearSettings: " + initBuilder.toString());
        sendMessageToWear(initBuilder.toString());
    }

    public void initiateWearSetup() {
        saveConnectedWearNode();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isWearNodeSaved()) {
                    if (isWearAround()) {
                        Log.d(Constants.TAG, "initiateWearSetup:Setup Complete");
                        notifySetup(Constants.CONNECTED_WEAR);
                        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent(Constants.LOADING_ACTIVITY_INTENT_FILTER).putExtra(Constants.MESSAGE_EXTRA_KEY, Constants.CONNECTED_WEAR));
                    } else {
                        Log.d(Constants.TAG, "initiateWearSetup: setupPartial");
                        notifySetup(Constants.FAILED_WEAR);
                        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent(Constants.LOADING_ACTIVITY_INTENT_FILTER).putExtra(Constants.MESSAGE_EXTRA_KEY, Constants.FAILED_WEAR));
                    }
                } else {
                    Log.d(Constants.TAG, "initiateWearSetup:setupFailed");
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent(Constants.LOADING_ACTIVITY_INTENT_FILTER).putExtra(Constants.MESSAGE_EXTRA_KEY, Constants.FAILED_WEAR_DISCOVERY));
                    notifySetup(Constants.FAILED_WEAR_DISCOVERY);
                }
            }
        }, 5000);
    }

    public void scanWear() {
        Log.d(Constants.TAG, "MessageService:scanWear");
        isWearServiceRunning();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isWearAround()) {
                    Log.d(Constants.TAG, "MessageService:scanWear");
                    notifySetup(Constants.CONNECTED_WEAR);
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent(Constants.VICINITY_CHECK_INTENT_FILTER).putExtra(Constants.VICINITY_RESULT_KEY, Constants.WEAR_IN_RANGE));
                } else {
                    notifySetup(Constants.FAILED_WEAR);
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent(Constants.VICINITY_CHECK_INTENT_FILTER).putExtra(Constants.VICINITY_RESULT_KEY, Constants.WEAR_NOT_IN_RANGE));

                }
            }
        }, 5000);
    }

    public boolean enableBluetoothIfOff() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        boolean isEnabled = bluetoothAdapter.isEnabled();
        return isEnabled || bluetoothAdapter.enable();
    }

    private void showSurveyNotif() {
        final Intent dashIntent = new Intent(this, UPMC.class);
        PendingIntent dashPendingIntent = PendingIntent.getActivity(this, 0, dashIntent, 0);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(Constants.SURVEY_NOTIF_CHNL_ID, "UPMC Dash", NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.setDescription("UPMC Dash Survey Notification");
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.enableVibration(true);
            assert notificationManager != null;
            notificationManager.createNotificationChannel(notificationChannel);
            surveyNotifBuilder = new Notification.Builder(this, Constants.SURVEY_NOTIF_CHNL_ID);
            surveyNotifBuilder.setAutoCancel(false)
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark_normal)
                    .setContentTitle(Constants.SELF_REPORT_TITLE)
                    .setGroup("Survey")
                    .setContentText(Constants.SELF_REPORT_CONTENT)
                    .setContentIntent(dashPendingIntent);
            startForeground(Constants.SURVEY_NOTIF_ID, surveyNotifBuilder.build());

        } else {
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


    private void notifySurvey(boolean daily) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if(daily) {
            final Intent dashIntent = new Intent(this, UPMC.class).setAction(Constants.ACTION_SHOW_MORNING);
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
            final Intent dashIntent = new Intent(this, UPMC.class);
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
                notificationManager.notify(Constants.SURVEY_NOTIF_ID, surveyNotifBuilder.build());

            }

        }
    }

    private void showSetupNotif() {
        final String contentText = isWearNodeSaved() ? Constants.FAILED_WEAR : Constants.SETUP_WEAR;
        final Intent dashIntent = new Intent(this, MessageService.class);
        dashIntent.setAction(Constants.ACTION_SETUP_WEAR);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setupNotifBuilder = new Notification.Builder(this, Constants.SURVEY_NOTIF_CHNL_ID); // using the survey notification ID, as it doesn't matter
            PendingIntent dashPendingIntent = PendingIntent.getForegroundService(this, 0, dashIntent, 0);
            setupNotifBuilder.setAutoCancel(false)
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark_normal)
                    .setContentTitle("UPMC Dash Setup")
                    .setContentText(contentText)
                    .setGroup("Setup")
                    .setOngoing(true)
                    .setContentIntent(dashPendingIntent);
            notificationManager.notify(Constants.SETUP_NOTIF_ID, setupNotifBuilder.build());
        } else {
            PendingIntent dashPendingIntent = PendingIntent.getService(this, 0, dashIntent, 0);
            setupNotifCompatBuilder = new NotificationCompat.Builder(this, Constants.SURVEY_NOTIF_CHNL_ID);
            setupNotifCompatBuilder.setAutoCancel(false)
                    .setOngoing(true)
                    .setGroup("Setup")
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark_normal)
                    .setContentTitle("UPMC Dash")
                    .setContentText(contentText)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentInfo("info")
                    .setContentIntent(dashPendingIntent);
            notificationManager.notify(Constants.SETUP_NOTIF_ID, setupNotifCompatBuilder.build());
        }
    }

    public void notifySetup(String contentText) {
        boolean canNotify = !this.prevNotif.equals(contentText);
        if(canNotify) {
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
            this.prevNotif = contentText;
        }
    }

    @Override
    public void onDataChanged(@NonNull DataEventBuffer dataEventBuffer) {
        Log.d(Constants.TAG, "MessageService:onDataChanged:received");
        for(DataEvent event : dataEventBuffer) {
            Log.d(Constants.TAG, "test1");
            if (event.getType() == DataEvent.TYPE_CHANGED &&
                    event.getDataItem().getUri().getPath().equals("/upmc-dash")) {
                Log.d(Constants.TAG, "test2");
                DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                final Asset logfileAsset = dataMapItem.getDataMap().getAsset("logfile");
                Log.d(Constants.TAG, "MessageService:onDataChanged: received logfileasset");
                SyncFilesTask syncFilesTask = new SyncFilesTask(MessageService.this);
                syncFilesTask.execute(new SyncFilesParams(MessageService.this, logfileAsset, dataClient));
            }
        }
        super.onDataChanged(dataEventBuffer);
    }

    @Override
    public void onConnectedNodes(List<Node> list) {
        Log.d(Constants.TAG, "MessageService: onConnectedNodes");
        super.onConnectedNodes(list);
    }

    @Override
    public void onPeerConnected(Node node) {
        Log.d(Constants.TAG, "MessageService: onPeerConnected");
        super.onPeerConnected(node);
    }

    @Override
    public void onPeerDisconnected(Node node) {
        Log.d(Constants.TAG, "MessageService: onPeerConnected");
        super.onPeerDisconnected(node);
    }

    @Override
    public void onCapabilityChanged(@NonNull CapabilityInfo capabilityInfo) {
        Log.d(Constants.TAG, "onCapabilityChanged)");
        if (isWearNodeSaved()) {
            if (isWearNodeIdPresent(capabilityInfo.getNodes())) {
                Log.d(Constants.TAG, "onCapabilityChanged: wear is connected");
                notifySetup(Constants.CONNECTED_WEAR);
                syncWearSettings();
            }
            else {
                Log.d(Constants.TAG, "onCapabilityChanged: wear is disconnected");
                notifySetup(Constants.FAILED_WEAR);
            }
        }
    }



    public boolean isWearNodeIdPresent(Set<Node> nodes) {
        if (nodes.size() == 0) {
            Log.d(Constants.TAG, "isWearNodeIdPresent: no connected nodes");
            return false;
        }
        for (Node node : nodes) {
            if (node.getId().equals(readWearNodeId()))
                return true;
        }
        Log.d(Constants.TAG, "isWearNodeIdPresent: no nodes with wear nodeID");
        return false;
    }

    private void sendMessageToWear(final String message) {
        Log.d(Constants.TAG, "MessageService:sendMessageToWear " + message);
        if (!isWearNodeSaved())
            return;
        final String nodeID = readWearNodeId();
        messageClient.sendMessage(nodeID, Constants.MESSAGE_URI.toString(), message.getBytes()).
                addOnSuccessListener(new OnSuccessListener<Integer>() {
                    @Override
                    public void onSuccess(Integer integer) {
                        Log.d(Constants.TAG, "sendMessageToWear:onSuccess " + integer);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(Constants.TAG, "sendMessageToWear:onFailure");
                    }
                })
                .addOnCompleteListener(new OnCompleteListener<Integer>() {
                    @Override
                    public void onComplete(@NonNull Task<Integer> task) {
                        Log.d(Constants.TAG, "sendMessageToWear:onComplete, waiting for ACK from wear...");
                    }
                });
        setWearAround(false);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                if (isWearAround()) {
                    Log.d(Constants.TAG, "sendMessageToWear: ACK received, wear is around");
                    if (!isWearAround()) {
                        notifySetup(Constants.CONNECTED_WEAR);
                    }
                    setWearAround(true);
                } else {
                    Log.d(Constants.TAG, "sendMessageToWear: ACK not received wear is not around");
                    setWearAround(false);
                    notifySetup(Constants.FAILED_WEAR);
                }
            }
        }, 5000);
    }

    private void sendAckToWear() {
        if (!isWearNodeSaved())
            return;
        final String nodeID = readWearNodeId();
        messageClient.sendMessage(nodeID, Constants.MESSAGE_URI.toString(), Constants.ACK.getBytes()).
                addOnSuccessListener(new OnSuccessListener<Integer>() {
                    @Override
                    public void onSuccess(Integer integer) {
                        Log.d(Constants.TAG, "sendAckToWear:onSuccess " + integer);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(Constants.TAG, "sendAckToWear:onFailure");
                    }
                })
                .addOnCompleteListener(new OnCompleteListener<Integer>() {
                    @Override
                    public void onComplete(@NonNull Task<Integer> task) {
                        Log.d(Constants.TAG, "sendAckToWear:onComplete, sent ACK to wear");
                    }
                });
    }

    public boolean isWearNodeSaved() {
        return !(Constants.PREFERENCES_DEFAULT_WEAR_NODEID.equals(readWearNodeId()));
    }

    public Node getConnectedWearNode(Set<Node> nodes) {
        if (nodes.size() == 0) {
            return null;
        }
        for (Node node : nodes) {
            if (node.getDisplayName().toLowerCase().contains("lg watch")) {
                return node;
            }
        }
        return null;
    }

    public void writeWearNodeId(String nodeId) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(Constants.PREFERENCES_KEY_WEAR_NODEID, nodeId);
        editor.apply();
        Log.d(Constants.TAG, "MessageService:writeWearNodeId: " + nodeId);
    }

    public String readWearNodeId() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String nodeId = sharedPref.getString(Constants.PREFERENCES_KEY_WEAR_NODEID, Constants.PREFERENCES_DEFAULT_WEAR_NODEID);
        if (nodeId.equals(Constants.PREFERENCES_DEFAULT_WEAR_NODEID))
            Log.d(Constants.TAG, "MessageService:readWearNodeId: " + nodeId);
        return nodeId;
    }

    private void isWearServiceRunning() {
        setWearAround(false);
        Log.d(Constants.TAG, "MessageService:isWearServiceRunning");
        if (!isWearNodeSaved())
            return;
        final String nodeID = readWearNodeId();
        messageClient.sendMessage(nodeID, Constants.MESSAGE_URI.toString(), Constants.IS_WEAR_RUNNING.getBytes()).
                addOnSuccessListener(new OnSuccessListener<Integer>() {
                    @Override
                    public void onSuccess(Integer integer) {
                        Log.d(Constants.TAG, "isWearServiceRunning:onSuccess " + integer);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(Constants.TAG, "isWearServiceRunning:onFailure");
                    }
                })
                .addOnCompleteListener(new OnCompleteListener<Integer>() {
                    @Override
                    public void onComplete(@NonNull Task<Integer> task) {
                        Log.d(Constants.TAG, "isWearServiceRunning:oncomplete, waiting for ACK from wear...");
                    }
                });
    }

    public boolean isWearAround() {
        return isWearAround;
    }

    public void setWearAround(boolean wearAround) {
        isWearAround = wearAround;
    }


    @Override
    public void onSyncSuccess() {
        Log.d(Constants.TAG, "MessageService:onSyncSuccess");
    }

    @Override
    public void onSyncFailed() {
        Log.d(Constants.TAG, "MessageService:onSyncFailed");
    }
}