package com.aware.plugin.upmc.dash.services;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.aware.Aware;
import com.aware.plugin.upmc.dash.activities.SetupLoadingActvity;
import com.aware.plugin.upmc.dash.utils.Constants;
import com.aware.plugin.upmc.dash.activities.InabilityResponseForm;
import com.aware.plugin.upmc.dash.R;
import com.aware.plugin.upmc.dash.settings.Settings;
import com.aware.plugin.upmc.dash.fileutils.SyncFilesParams;
import com.aware.plugin.upmc.dash.fileutils.SyncFilesResponse;
import com.aware.plugin.upmc.dash.fileutils.SyncFilesTask;
import com.aware.plugin.upmc.dash.activities.DemoESM;
import com.aware.plugin.upmc.dash.activities.UPMC;
import com.aware.plugin.upmc.dash.receivers.AlarmReceiver;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Created by RaghuTeja on 6/23/17.
 */

public class MessageService extends WearableListenerService implements
        GoogleApiClient.ConnectionCallbacks, DataApi.DataListener, SyncFilesResponse {

    public boolean wearConnected = false;
    private GoogleApiClient mGoogleApiClient;
    private NotificationCompat.Builder messageServiceNotifBuilder;
    private NotificationCompat.Builder watchClientNotifBuilder;
    private Notification.Builder setupNotifBuilder;
    private NotificationCompat.Builder setupNotifCompatBuilder;

    private AlarmManager mAlarmManager;
    private int count = 0;
    private boolean demoMode;
    private String NODE_ID;
    private boolean isNodeSaved = false;
    private BroadcastReceiver mBluetootLocalReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra(Constants.BLUETOOTH_COMM_KEY)) {
                int state = intent.getIntExtra(Constants.BLUETOOTH_COMM_KEY, BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(Constants.TAG, "MessageService:BluetoothReceiver:StateOff");
                        notifySetup(Constants.FAILED_WEAR_BLUETOOTH);
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(Constants.TAG, "MessageService:BluetoothReceiver:StateTurningOff");
                        notifySetup(Constants.FAILED_WEAR_BLUETOOTH);
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(Constants.TAG, "MessageService:BluetoothReceiver:StateOn");
                        setUpNodeIdentities();
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(Constants.TAG, "MessageService:BluetoothReceiver:StateTurningOn");
                        break;
                }
            }
        }
    };

    private BroadcastReceiver mSettingsLocalReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra(Constants.SETTINGS_EXTRA_KEY)) {
                if (intent.getStringExtra(Constants.SETTINGS_EXTRA_KEY).equals(Constants.SETTINGS_CHANGED)) {
                    Log.d(Constants.TAG, "MessageService: mSettingsLocalReceiver");
                    timeResetWear();
                }
                else if(intent.getStringExtra(Constants.SETTINGS_EXTRA_KEY).equals(Constants.VICINITY_CHECK)) {
                    Log.d(Constants.TAG, "MessageService: vicinity check");
                    isWearServiceRunning(getNODE_ID());
                    if(isDemoMode()) {
                        final Handler handler = new Handler();
                        //do something here
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (isWearConnected()) {
                                    Log.d(Constants.TAG, "MessageService:: Wear replied with ACK");
                                    if(isDemoMode()) {
                                        Handler handler = new Handler();
                                        handler.postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                sendMessageToWear(Constants.DEMO_NOTIF);
                                                Log.d(Constants.TAG, "MessageService:Sending Demo Notif");
                                            }
                                        }, 16000);
                                    }


                                } else {
                                    Log.d(Constants.TAG, "MessageService:: Wear gave no ACK response");
                                    notifyUserSyncFailed(Constants.FAILED_WEAR);
                                }
                            }
                        }, 5000);
                    }
                    Handler handler = new Handler();
                    final Context mContext = context;
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if(isWearConnected()) {
                                LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent(Constants.VICINITY_CHECK_INTENT_FILTER).putExtra(Constants.VICINITY_RESULT_KEY, Constants.WEAR_IN_RANGE));
                                Log.d(Constants.TAG, "MessageService: vicinity check pass");
                            }
                            else {
                                LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent(Constants.VICINITY_CHECK_INTENT_FILTER).putExtra(Constants.VICINITY_RESULT_KEY, Constants.WEAR_NOT_IN_RANGE));
                                Log.d(Constants.TAG, "MessageService: vicinity check fail");
                            }
                        }
                    }, 10000);
                }
                else if(intent.getStringExtra(Constants.SETTINGS_EXTRA_KEY).equals(Constants.KILL_DEMO)) {
                    Log.d(Constants.TAG, "MessageService:EndingDemo");
                }
            }
        }
    };

    private BroadcastReceiver mSnoozeAlarmLocalReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.hasExtra(Constants.SNOOZE_ALARM_EXTRA_KEY)) {
                afterSnoozeInactivityNotif();

            }
        }
    };

    public boolean isDemoMode() {
        return demoMode;
    }

    public void setDemoMode(boolean demoMode) {
        this.demoMode = demoMode;
    }

    public void afterSnoozeInactivityNotif() {
        Log.d(Constants.TAG, "MessageService:InactivityNotif:Snooze Done!");
        if(isDemoMode()) {
            noSnoozeInactivityDemoNotif();
        }
        else {
            noSnoozeInactivityNotif();
        }

    }


    public void noSnoozeInactivityNotif() {

        Intent okIntent = new Intent();
        okIntent.setAction(Constants.OK_ACTION);
        PendingIntent pendingIntentOk = PendingIntent.getBroadcast(this, 555, okIntent, PendingIntent.FLAG_ONE_SHOT);

//        Intent snoozeIntent = new Intent();
//        snoozeIntent.setAction(Constants.SNOOZE_ACTION);
//        PendingIntent pendingIntentSnooze = PendingIntent.getBroadcast(this, 555, snoozeIntent, PendingIntent.FLAG_ONE_SHOT);

        Intent noIntent = new Intent();
        noIntent.setAction(Constants.NO_ACTION);
        PendingIntent pendingIntentNo = PendingIntent.getBroadcast(this, 555, noIntent, PendingIntent.FLAG_ONE_SHOT);

        PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "Wake Up");

        wl.acquire(6000);


        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        watchClientNotifBuilder = new NotificationCompat.Builder(getApplicationContext())
                .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark_normal)
                .setContentTitle("UPMC Dash Monitor")
                .setContentText("Ready for a quick walk?")
                .setOngoing(true)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .addAction(R.drawable.ic_done_black_18dp, "OK!", pendingIntentOk)
                .addAction(R.drawable.ic_not_interested_black_18dp, "No", pendingIntentNo);
        mNotificationManager.notify(66, watchClientNotifBuilder.build());
        final Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        vibrator.vibrate(3000);
    }



    public void noSnoozeInactivityDemoNotif() {

        Intent okIntent = new Intent();
        okIntent.setAction(Constants.OK_ACTION);
        PendingIntent pendingIntentOk = PendingIntent.getBroadcast(this, 555, okIntent, PendingIntent.FLAG_ONE_SHOT);

//        Intent snoozeIntent = new Intent();
//        snoozeIntent.setAction(Constants.SNOOZE_ACTION);
//        PendingIntent pendingIntentSnooze = PendingIntent.getBroadcast(this, 555, snoozeIntent, PendingIntent.FLAG_ONE_SHOT);

        Intent noIntent = new Intent();
        noIntent.setAction(Constants.NO_ACTION);
        PendingIntent pendingIntentNo = PendingIntent.getBroadcast(this, 555, noIntent, PendingIntent.FLAG_ONE_SHOT);
        PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "Wake Up");
        wl.acquire(6000);
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        watchClientNotifBuilder = new NotificationCompat.Builder(getApplicationContext())
                .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark_normal)
                .setContentTitle("UPMC Dash Monitor (DEMO)")
                .setContentText("Ready for a quick walk?")
                .setOngoing(true)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .addAction(R.drawable.ic_done_black_18dp, "OK!", pendingIntentOk)
                .addAction(R.drawable.ic_not_interested_black_18dp, "No", pendingIntentNo);
        mNotificationManager.notify(22, watchClientNotifBuilder.build());
        final Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        vibrator.vibrate(3000);
    }




    public void writeSymptomPref(int type) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(Constants.SYMPTOMS_PREFS, type);
        editor.apply();
    }

    public int readSymptomsPref() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        int type = sharedPref.getInt(Constants.SYMPTOMS_PREFS, -1);
        return type;
    }

    public boolean isSympInitialized() {
        if(readSymptomsPref()==-1) {
            return false;
        }
        else
            return true;
    }


    private BroadcastReceiver mSymptomsLocalReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(isSympInitialized()) {
                if(intent.getIntExtra(Constants.SYMPTOMS_KEY,-1)!=-1) {
                    sympResetWear(intent.getIntExtra(Constants.SYMPTOMS_KEY,-1));
                    writeSymptomPref(intent.getIntExtra(Constants.SYMPTOMS_KEY, -1));
                }
            }
            else if(!isSympInitialized()){
                writeSymptomPref(intent.getIntExtra(Constants.SYMPTOMS_KEY,-1));
                Log.d(Constants.TAG, "MessageService:SymptomsReceiver" + intent.getIntExtra(Constants.SYMPTOMS_KEY,-1));
                initializeWear();
            }
        }
    };

    private BroadcastReceiver mNotifLocalReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.hasExtra(Constants.NOTIF_KEY)) {
                Log.d(Constants.TAG, "MessageService: NotifLocalReceiver:Received :  " + intent.getStringExtra(Constants.NOTIF_KEY));
                if(intent.getStringExtra(Constants.NOTIF_KEY).equals(Constants.SNOOZE_ACTION)) {
                    if(isDemoMode()) {
                        snoozeDemoInactivityNotif();
                    }
                    else {
                        snoozeInactivityNotif();
                    }
                }
                else if(intent.getStringExtra(Constants.NOTIF_KEY).equals(Constants.OK_ACTION)) {
                    if(isDemoMode()) {
                        dismissInacticityDemoNotif();
                    }
                    else {
                        dismissInactivtyNotif();
                        sendMessageToWear(Constants.OK_ACTION);
                    }
                }
                else if(intent.getStringExtra(Constants.NOTIF_KEY).equals(Constants.NO_ACTION)) {
                    if(isDemoMode()) {
                        dismissInacticityDemoNotif();
                    }
                    else {
                        dismissInactivtyNotif();
                    }
                    showResponseForm();
                }
                else if(intent.getStringExtra(Constants.NOTIF_KEY).equals(Constants.RETRY_ACTION)) {
                    Log.d(Constants.TAG, "Retry action happened");
                    isWearServiceRunning(getNODE_ID());
                }
                else if(intent.getStringExtra(Constants.NOTIF_KEY).equals(Constants.OK_ACTION_GJ)) {
                    dismissAppraisal();
                }
            }
        }
    };

    public void dismissAppraisal() {
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(66);
    }

    public void showResponseForm() {
        Intent respIntent = new Intent(this, InabilityResponseForm.class);
        respIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(respIntent);
    }



    public void dismissInactivtyNotif() {
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(66);
    }


    public void dismissInacticityDemoNotif() {
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(22);
    }

    public void snoozeInactivityNotif() {
        Intent snoozeInt = new Intent(this, AlarmReceiver.class);
        snoozeInt.putExtra(Constants.ALARM_COMM,Constants.SNOOZE_ALARM_EXTRA);
        mAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        PendingIntent snoozePendInt = PendingIntent.getBroadcast(this, 56, snoozeInt, 0);
        mAlarmManager.set(AlarmManager.RTC_WAKEUP,System.currentTimeMillis() + 15*60*1000, snoozePendInt);
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(66);
    }


    public void snoozeDemoInactivityNotif() {
        Intent snoozeInt = new Intent(this, AlarmReceiver.class);
        snoozeInt.putExtra(Constants.ALARM_COMM,Constants.SNOOZE_ALARM_EXTRA);
        mAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        PendingIntent snoozePendInt = PendingIntent.getBroadcast(this, 56, snoozeInt, 0);
        int interval = 15 * 60 * 1000;
        mAlarmManager.set(AlarmManager.RTC_WAKEUP,System.currentTimeMillis() + interval, snoozePendInt);
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(22);
    }

    public boolean isNodeSaved() {
        return isNodeSaved;
    }

    public void setNodeSaved(boolean nodeSaved) {
        isNodeSaved = nodeSaved;
    }

    private void timeResetWear() {
        StringBuilder initBuilder = new StringBuilder();
        initBuilder.append(Constants.TIME_RESET);
        initBuilder.append(" ");
        initBuilder.append(Aware.getSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_MORNING_HOUR));
        initBuilder.append(" ");
        initBuilder.append(Aware.getSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_MORNING_MINUTE));
        initBuilder.append(" ");
        initBuilder.append(Aware.getSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_NIGHT_HOUR));
        initBuilder.append(" ");
        initBuilder.append(Aware.getSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_NIGHT_MINUTE));
        initBuilder.append(" ");
        initBuilder.append(readSymptomsPref());
        sendMessageToWear(initBuilder.toString());
    }

    private void sympResetWear(int type) {
        sendMessageToWear(Constants.SYMP_RESET + " " + type);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(Constants.TAG, "MessageService:onDestroy");
        if(isDemoMode()) {
            stopForeground(true);
            Wearable.MessageApi.removeListener(mGoogleApiClient, this);
            Wearable.CapabilityApi.removeListener(mGoogleApiClient, this);
            if (mGoogleApiClient.isConnected()) {
                mGoogleApiClient.disconnect();
            }
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mSettingsLocalReceiver);
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mNotifLocalReceiver);
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mSnoozeAlarmLocalReceiver);
            stopSelf();
            return;

        }
        stopForeground(true);
        Wearable.DataApi.removeListener(mGoogleApiClient, this);
        Wearable.MessageApi.removeListener(mGoogleApiClient, this);
        Wearable.CapabilityApi.removeListener(mGoogleApiClient, this);
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBluetootLocalReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mSettingsLocalReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mSymptomsLocalReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mNotifLocalReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mSnoozeAlarmLocalReceiver);
        stopSelf();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(Constants.TAG, "MessageService: onCreate");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .build();
        mGoogleApiClient.connect();
    }

    public String getNODE_ID() {
        return NODE_ID;
    }

    public void setNODE_ID(String NODE_ID) {
        this.NODE_ID = NODE_ID;
        setNodeSaved(true);
    }

    private void setUpNodeIdentities() {
        Wearable.CapabilityApi.getCapability(mGoogleApiClient, Constants.CAPABILITY_WEAR_APP, CapabilityApi.FILTER_REACHABLE).setResultCallback(new ResultCallback<CapabilityApi.GetCapabilityResult>() {
            @Override
            public void onResult(@NonNull CapabilityApi.GetCapabilityResult getCapabilityResult) {
                CapabilityInfo info = getCapabilityResult.getCapability();
                Set<Node> nodes = info.getNodes();
                String NODE_ID;
                if (nodes.size() == 1) {
                    for (Node node : nodes) {
                        NODE_ID = node.getId();
                        Log.d(Constants.TAG, "MessageService:setUpNodeIdentities: " + NODE_ID);
                        setNODE_ID(NODE_ID);
                        isWearServiceRunning(getNODE_ID());
                    }
                }
            }
        });
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);
        byte[] input = messageEvent.getData();
        String message = new String(input);
        Log.d(Constants.TAG, "MessageService: onMessageReceived: " + message + " " + count);
        //Log.d(Constants.TAG, "MessageService: onMessageReceived: buildPath" + messageEvent.getPath());
        count++;
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.scheme("wear").path("/upmc-dash").build();
        if (messageEvent.getPath().equals(uriBuilder.toString())) {
            if (!isNodeSaved()) {
                setNODE_ID(messageEvent.getSourceNodeId());
            }
            if (message.equals(Constants.ACK)) {
                if (!isWearConnected()) {
                    setWearConnected(true);
                    if(!isDemoMode())
                        notifySetup(Constants.CONNECTED_WEAR);
                }
            } else {
                sendMessageToWear(Constants.ACK);
                if (message.equals(Constants.STATUS_LOGGING)) {
                    notifySetup(Constants.CONNECTED_WEAR);
                } else if (message.equals(Constants.STATUS_INIT)) {
                    Log.d(Constants.TAG, "MessageService:onMessageReceived:TimeInit");
                    notifySetup(Constants.CONNECTED_WEAR);
                    if(isSympInitialized()){
                        initializeWear();
                    }
                }
                else if(message.equals((Constants.NOTIFY_INACTIVITY))) {
                    Log.d(Constants.TAG, "MessageService:onMessageReceived:InactiveUser");
                    if(isDemoMode()) {
                        demoInactivityNotif();
                    }
                    else {
                        notifyUserWithInactivity();
                    }
                }
                else if(message.equals(Constants.NOTIFY_GREAT_JOB)) {
                    Log.d(Constants.TAG, "MessageService:onMessageReceived:GreatJobUser");
                    notifyUserWithAppraisal();
                }
            }
        }
    }

    public void notifyUserWithAppraisal() {
        Intent okIntent = new Intent();
        okIntent.setAction(Constants.OK_ACTION_GJ);
        PendingIntent pendingIntentOk = PendingIntent.getBroadcast(this, 555, okIntent, PendingIntent.FLAG_ONE_SHOT);

//        Intent snoozeIntent = new Intent();
//        snoozeIntent.setAction(Constants.SNOOZE_ACTION);
//        PendingIntent pendingIntentSnooze = PendingIntent.getBroadcast(this, 555, snoozeIntent, PendingIntent.FLAG_ONE_SHOT);
//
//        Intent noIntent = new Intent();
//        noIntent.setAction(Constants.NO_ACTION);
//        PendingIntent pendingIntentNo = PendingIntent.getBroadcast(this, 555, noIntent, PendingIntent.FLAG_ONE_SHOT);

        PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "Wake Up");

        wl.acquire(6000);

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        watchClientNotifBuilder = new NotificationCompat.Builder(getApplicationContext())
                .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark_normal)
                .setContentTitle("UPMC Dash Monitor")
                .setContentText("Great Job! You have been active")
                .setOngoing(true)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .addAction(R.drawable.ic_done_black_18dp, "OK!", pendingIntentOk);
        mNotificationManager.notify(66, watchClientNotifBuilder.build());
        final Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        vibrator.vibrate(3000);
    }

    public void demoInactivityNotif() {
        Intent okIntent = new Intent();
        okIntent.setAction(Constants.OK_ACTION);
        PendingIntent pendingIntentOk = PendingIntent.getBroadcast(this, 555, okIntent, PendingIntent.FLAG_ONE_SHOT);

        Intent snoozeIntent = new Intent();
        snoozeIntent.setAction(Constants.SNOOZE_ACTION);
        PendingIntent pendingIntentSnooze = PendingIntent.getBroadcast(this, 555, snoozeIntent, PendingIntent.FLAG_ONE_SHOT);

        Intent noIntent = new Intent();
        noIntent.setAction(Constants.NO_ACTION);
        PendingIntent pendingIntentNo = PendingIntent.getBroadcast(this, 555, noIntent, PendingIntent.FLAG_ONE_SHOT);

        PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "Wake Up");
        wl.acquire(6000);
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        watchClientNotifBuilder = new NotificationCompat.Builder(getApplicationContext())
                .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark_normal)
                .setContentTitle("UPMC Dash Monitor (DEMO)")
                .setContentText("Ready for a quick walk?")
                .setOngoing(true)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .addAction(R.drawable.ic_done_black_18dp, "OK!", pendingIntentOk)
                .addAction(R.drawable.ic_snooze_black_18dp, "Snooze", pendingIntentSnooze)
                .addAction(R.drawable.ic_not_interested_black_18dp, "No", pendingIntentNo);
        mNotificationManager.notify(22, watchClientNotifBuilder.build());
        final Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        vibrator.vibrate(3000);
    }

    public void notifyUserWithInactivity() {

        Intent okIntent = new Intent();
        okIntent.setAction(Constants.OK_ACTION);
        PendingIntent pendingIntentOk = PendingIntent.getBroadcast(this, 555, okIntent, PendingIntent.FLAG_ONE_SHOT);

        Intent snoozeIntent = new Intent();
        snoozeIntent.setAction(Constants.SNOOZE_ACTION);
        PendingIntent pendingIntentSnooze = PendingIntent.getBroadcast(this, 555, snoozeIntent, PendingIntent.FLAG_ONE_SHOT);

        Intent noIntent = new Intent();
        noIntent.setAction(Constants.NO_ACTION);
        PendingIntent pendingIntentNo = PendingIntent.getBroadcast(this, 555, noIntent, PendingIntent.FLAG_ONE_SHOT);

        PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "Wake Up");

        wl.acquire(6000);

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        watchClientNotifBuilder = new NotificationCompat.Builder(getApplicationContext())
                .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark_normal)
                .setContentTitle("UPMC Dash Monitor")
                .setContentText("Ready for a quick walk?")
                .setOngoing(true)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .addAction(R.drawable.ic_done_black_18dp, "OK!", pendingIntentOk)
                .addAction(R.drawable.ic_snooze_black_18dp, "Snooze", pendingIntentSnooze)
                .addAction(R.drawable.ic_not_interested_black_18dp, "No", pendingIntentNo);
        mNotificationManager.notify(66, watchClientNotifBuilder.build());
        final Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        vibrator.vibrate(3000);
    }

    public void initializeWear() {
        StringBuilder initBuilder = new StringBuilder();
        initBuilder.append(Constants.INIT_TS);
        initBuilder.append(" ");
        initBuilder.append(Aware.getSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_MORNING_HOUR));
        initBuilder.append(" ");
        initBuilder.append(Aware.getSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_MORNING_MINUTE));
        initBuilder.append(" ");
        initBuilder.append(Aware.getSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_NIGHT_HOUR));
        initBuilder.append(" ");
        initBuilder.append(Aware.getSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_NIGHT_MINUTE));
        initBuilder.append(" ");
        initBuilder.append(readSymptomsPref());
        sendMessageToWear(initBuilder.toString());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int i = super.onStartCommand(intent, flags, startId);
          Log.d(Constants.TAG, "MessageService: onStartCommand");
        if(intent.getAction().equals(Constants.ACTION_FIRST_RUN)) {
            Log.d(Constants.TAG, "MessageService: onStartCommand first run");
            mAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            LocalBroadcastManager.getInstance(this).registerReceiver(mBluetootLocalReceiver, new IntentFilter(Constants.BLUETOOTH_COMM));
            LocalBroadcastManager.getInstance(this).registerReceiver(mSettingsLocalReceiver, new IntentFilter(Constants.SETTING_INTENT_FILTER));
            LocalBroadcastManager.getInstance(this).registerReceiver(mSymptomsLocalReceiver, new IntentFilter(Constants.SYMPTOMS_INTENT_FILTER));
            LocalBroadcastManager.getInstance(this).registerReceiver(mNotifLocalReceiver, new IntentFilter(Constants.NOTIF_COMM));
            LocalBroadcastManager.getInstance(this).registerReceiver(mSnoozeAlarmLocalReceiver, new IntentFilter(Constants.SNOOZE_ALARM_INTENT_FILTER));
            showSurveyNotif();
            showSetupNotif();
        }
        else if (intent.getAction().equals(Constants.ACTION_SETUP_WEAR)) {
            Log.d(Constants.TAG, "MessageService: onStartCommand setup wear");
            notifySetup(Constants.SETUP_ONGOING);
            startActivity(new Intent(this, SetupLoadingActvity.class));
            setUpNodeIdentities();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if(isNodeSaved()) {
                        if(isWearConnected()) {
                            Log.d(Constants.TAG, "onStartCommand:Setup Complete");
                            notifySetup(Constants.CONNECTED_WEAR);
                            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent(Constants.LOADING_ACTIVITY_INTENT_FILTER).putExtra(Constants.MESSAGE_EXTRA_KEY, Constants.CONNECTED_WEAR));
                        }
                        else {
                            Log.d(Constants.TAG, "onStartCommand: setupFailed");
                            notifySetup(Constants.FAILED_WEAR);
                            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent(Constants.LOADING_ACTIVITY_INTENT_FILTER).putExtra(Constants.MESSAGE_EXTRA_KEY, Constants.FAILED_WEAR));

                        }
                    }

                    else {
                        Log.d(Constants.TAG, "onStartCommand:setupFailed");
                        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent(Constants.LOADING_ACTIVITY_INTENT_FILTER).putExtra(Constants.MESSAGE_EXTRA_KEY, Constants.FAILED_WEAR));
                        notifySetup(Constants.FAILED_WEAR);

                    }
                }
            }, 5000);

        }
        return i;
    }


    private void showSurveyNotif() {

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(Constants.NOTIFICATION_CHANNEL_ID, "UPMC Dash", NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.setDescription("UPMC Dash notification channel");
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.setVibrationPattern(new long[]{0, 1000, 500, 1000});
            notificationChannel.enableVibration(true);
            notificationManager.createNotificationChannel(notificationChannel);
            Notification.Builder notificationBuilder = new Notification.Builder(this, Constants.NOTIFICATION_CHANNEL_ID);
            Intent dashIntent = new Intent(this, UPMC.class);
            dashIntent.setAction(Constants.ACTION_SURVEY);
            PendingIntent dashPendingIntent = PendingIntent.getActivity(this, 0, dashIntent, 0);
            notificationBuilder.setAutoCancel(false)
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark_normal)
                    .setContentTitle("UPMC Dash Survey")
                    .setContentText(Constants.COMPLETE_SURVEY)
                    .setContentIntent(dashPendingIntent);

            startForeground(1, notificationBuilder.build());

        } else {
            Intent dashIntent = new Intent(this, UPMC.class);
            dashIntent.setAction(Constants.ACTION_SURVEY);
            PendingIntent dashPendingIntent = PendingIntent.getActivity(this, 0, dashIntent, 0);
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID);
            notificationBuilder.setAutoCancel(false)
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark_normal)
                    .setContentTitle("UPMC Dash Survey")
                    .setContentText(Constants.COMPLETE_SURVEY)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentInfo("info")
                    .setContentIntent(dashPendingIntent);

            startForeground(1,notificationBuilder.build());
        }

    }

    private void showSetupNotif() {

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setupNotifBuilder = new Notification.Builder(this, Constants.NOTIFICATION_CHANNEL_ID);
            Intent dashIntent = new Intent(this, MessageService.class);
            dashIntent.setAction(Constants.ACTION_SETUP_WEAR);
            PendingIntent dashPendingIntent = PendingIntent.getForegroundService(this, 0, dashIntent, 0);
            setupNotifBuilder.setAutoCancel(false)
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark_normal)
                    .setContentTitle("UPMC Dash")
                    .setContentText(Constants.SETUP_WEAR)
                    .setOngoing(true)
                    .setContentIntent(dashPendingIntent);

            notificationManager.notify(2, setupNotifBuilder.build());

        } else {
            Intent dashIntent = new Intent(this, MessageService.class);
            dashIntent.setAction(Constants.ACTION_SETUP_WEAR);
            PendingIntent dashPendingIntent = PendingIntent.getService(this, 0, dashIntent, 0);
            setupNotifCompatBuilder = new NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID);
            setupNotifCompatBuilder.setAutoCancel(false)
                    .setOngoing(true)
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark_normal)
                    .setContentTitle("UPMC Dash")
                    .setContentText(Constants.SETUP_WEAR)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentInfo("info")
                    .setContentIntent(dashPendingIntent);

            notificationManager.notify(2, setupNotifCompatBuilder.build());
        }

    }



    public void notifySetup(String contentText) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setupNotifBuilder.setContentText(contentText);
            notificationManager.cancel(2);
            notificationManager.notify(2, setupNotifBuilder.build());
        }
        else {
            setupNotifCompatBuilder.setContentText(contentText);
            notificationManager.cancel(2);
            notificationManager.notify(2, setupNotifCompatBuilder.build());

        }
    }





    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {
        Log.d(Constants.TAG, "MessageService:onDataChanged:received");
        for(DataEvent event : dataEventBuffer) {
            if (event.getType() == DataEvent.TYPE_CHANGED &&
                    event.getDataItem().getUri().getPath().equals("/upmc-dash")) {
                DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                final Asset logfileAsset = dataMapItem.getDataMap().getAsset("logfile");
                Log.d(Constants.TAG, "MessageService:onDataChanged: received logfileasset");
                SyncFilesTask syncFilesTask = new SyncFilesTask(MessageService.this);
                syncFilesTask.execute(new SyncFilesParams(mGoogleApiClient,logfileAsset, getApplicationContext()));
            }
        }
        super.onDataChanged(dataEventBuffer);
    }




    private void startDemoClientNotif() {
        Intent dashIntent = new Intent(this, DemoESM.class);
        dashIntent.setAction(new Random().nextInt(50) + "_action");
        PendingIntent dashPendingIntent = PendingIntent.getActivity(this, 0, dashIntent, 0);
        messageServiceNotifBuilder = new NotificationCompat.Builder(getApplicationContext())
                .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark_normal)
                .setContentTitle("UPMC Dash (DEMO)")
                .setContentText(Constants.COMPLETE_SURVEY)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(dashPendingIntent);
        startForeground(11, messageServiceNotifBuilder.build());
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
    public void onCapabilityChanged(CapabilityInfo capabilityInfo) {
        Log.d(Constants.TAG, "MessageService: onCapability");
        super.onCapabilityChanged(capabilityInfo);
        if (capabilityInfo.getNodes().size() > 0) {
            Log.d(Constants.TAG, "MessageService: Device Connected");
        } else {
            Log.d(Constants.TAG, "MessageService: No Devices, onCapabilityChanged");
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(Constants.TAG, "MessageService: onConnected");
        ;
        Wearable.CapabilityApi.addCapabilityListener(mGoogleApiClient,
                this,
                Constants.CAPABILITY_WEAR_APP);
        Uri uri = new Uri.Builder().scheme("wear").path("/upmc-dash").build();
        Wearable.MessageApi.addListener(mGoogleApiClient, this, uri, MessageApi.FILTER_PREFIX);
        Wearable.DataApi.addListener(mGoogleApiClient, this);
        //setUpNodeIdentities();
        //notifyUserWithInactivity();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(Constants.TAG, "MessageService:onConnectionSuspended");
    }


    public boolean isWearConnected() {
        return wearConnected;
    }

    public void setWearConnected(boolean wearConnected) {
        this.wearConnected = wearConnected;
    }

    private void sendMessageToWear(final String message) {
        Log.d(Constants.TAG, "MessageService:sendMessageToWear " + message);
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.scheme("wear").path("/upmc-dash");

        PendingResult<MessageApi.SendMessageResult> pendingResult =
                Wearable.MessageApi.sendMessage(
                        mGoogleApiClient,
                        getNODE_ID(),
                        uriBuilder.toString(),
                        message.getBytes());

        pendingResult.setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
            @Override
            public void onResult(@NonNull MessageApi.SendMessageResult sendMessageResult) {
                if (!sendMessageResult.getStatus().isSuccess()) {
                    Log.d(Constants.TAG, "MessageService:sendMessageToWear:message failed" + message);
                    notifyUserSyncFailed(Constants.FAILED_WEAR);
                } else {
                    Log.d(Constants.TAG, "MessageService:sendMessageToWear:message sent" + message);
                }
            }
        });
    }

//    private void killWear(final Context context) {
//        Log.d(Constants.TAG, "MessageService:killing wear and phone ");
//        String message = Constants.KILL_DASH;
//        Uri.Builder uriBuilder = new Uri.Builder();
//        uriBuilder.scheme("wear").path("/upmc-dash");
//        PendingResult<MessageApi.SendMessageResult> pendingResult =
//                Wearable.MessageApi.sendMessage(
//                        mGoogleApiClient,
//                        getNODE_ID(),
//                        uriBuilder.toString(),
//                        message.getBytes());
//        pendingResult.setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
//            @Override
//            public void onResult(@NonNull MessageApi.SendMessageResult sendMessageResult) {
//                if (!sendMessageResult.getStatus().isSuccess()) {
//                    Log.d(Constants.TAG, "MessageService:sendMessageToWear: kill failed");
//                    Toast.makeText(context, "Failed to kill Wear App. Please check manually", Toast.LENGTH_LONG).show();
//                } else {
//                    Log.d(Constants.TAG, "MessageService:sendMessageToWear: kill sent");
//                    Toast.makeText(context, "Kill app on Wear Successful.", Toast.LENGTH_LONG).show();
//                }
//                sendBroadcast(new Intent(Constants.NOTIFICATION_RECEIVER_INTENT_FILTER).putExtra(Constants.COMM_KEY_NOTIF, "KILL_REQUEST"));
//            }
//        });
//    }

    private void isWearServiceRunning(String NODE_ID) {
        setWearConnected(false);
        String message = Constants.IS_WEAR_RUNNING;
        notifySetup(Constants.SETUP_ONGOING);
        Uri.Builder uriPath = new Uri.Builder();
        uriPath.scheme("wear").path("/upmc-dash").build();
        PendingResult<MessageApi.SendMessageResult> pendingResult = Wearable.MessageApi.sendMessage(
                mGoogleApiClient,
                NODE_ID,
                uriPath.toString(),
                message.getBytes());

        pendingResult.setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
            @Override
            public void onResult(@NonNull MessageApi.SendMessageResult sendMessageResult) {
                if (!sendMessageResult.getStatus().isSuccess()) {
                    Log.d(Constants.TAG, "MessageService:detectWearStatus:wearStatus: Failed to send message");
                } else {
                    Log.d(Constants.TAG, "MessageService:detectWearStatus:wearStatus: Message successfully sent");
                }
            }
        });
    }

    public void notifyUserSyncFailed(String message) {
        if(isDemoMode()) {
            Intent okIntent = new Intent();
            okIntent.setAction(Constants.RETRY_ACTION);
            PendingIntent pendingIntentRetry = PendingIntent.getBroadcast(this, 111, okIntent, PendingIntent.FLAG_ONE_SHOT);
            PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "Wake Up");
            wl.acquire(6000);
            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            watchClientNotifBuilder = new NotificationCompat.Builder(getApplicationContext())
                    .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark_normal)
                    .setContentTitle("UPMC Dash Monitor (DEMO)")
                    .setContentText(message)
                    .setOngoing(true)
                    .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .addAction(R.drawable.ic_undo_black_24dp, "RETRY", pendingIntentRetry);
            mNotificationManager.notify(11, watchClientNotifBuilder.build());
            final Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            vibrator.vibrate(3000);
        }
        else {
            Intent okIntent = new Intent();
            okIntent.setAction(Constants.RETRY_ACTION);
            PendingIntent pendingIntentRetry = PendingIntent.getBroadcast(this, 111, okIntent, PendingIntent.FLAG_ONE_SHOT);
            PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "Wake Up");
            wl.acquire(6000);
            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            watchClientNotifBuilder = new NotificationCompat.Builder(getApplicationContext())
                    .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark_normal)
                    .setContentTitle("UPMC Dash Monitor")
                    .setContentText(message)
                    .setOngoing(true)
                    .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .addAction(R.drawable.ic_undo_black_24dp, "RETRY", pendingIntentRetry);
            mNotificationManager.notify(1, watchClientNotifBuilder.build());
            final Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            vibrator.vibrate(3000);
        }

    }


    public void notifyUser(String notifContent) {
//        if(!isDemoMode()) {
//            messageServiceNotifBuilder.setContentText(notifContent);
//            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//            mNotificationManager.notify(1, messageServiceNotifBuilder.build());
//        }
//        else {
//            messageServiceNotifBuilder.setContentText(notifContent);
//            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//            mNotificationManager.notify(11, messageServiceNotifBuilder.build());
//        }

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