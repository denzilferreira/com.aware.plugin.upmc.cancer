package com.aware.plugin.upmc.dash;


import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.aware.Aware;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
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
        GoogleApiClient.ConnectionCallbacks {

    public boolean wearConnected = false;
    private GoogleApiClient mGoogleApiClient;
    private NotificationCompat.Builder messageServiceNotifBuilder;
    private NotificationCompat.Builder watchClientNotifBuilder;
    private AlarmManager mAlarmManager;
    private int count = 0;
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
                        notifyUser(Constants.NOTIFTEXT_SYNC_FAILED_BLUETOOTH);
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(Constants.TAG, "MessageService:BluetoothReceiver:StateTurningOff");
                        notifyUser(Constants.NOTIFTEXT_SYNC_FAILED_BLUETOOTH);
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

            if (intent.hasExtra(Constants.SETTINGS_COMM)) {
                if (intent.getStringExtra(Constants.SETTINGS_COMM).equals(Constants.SETTINGS_CHANGED)) {
                    Log.d(Constants.TAG, "MessageService: mSettingsLocalReceiver");
                    timeResetWear();

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

    public void afterSnoozeInactivityNotif() {
        Log.d(Constants.TAG, "Snooze done!");
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
                if(intent.getIntExtra(Constants.SYMPTOMS_KEY,-1) != readSymptomsPref()) {
                    sympResetWear(intent.getIntExtra(Constants.SYMPTOMS_KEY,-1));
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
                snoozeInactivityNotif();
            }
        }
    };

    public void snoozeInactivityNotif() {
        Intent snoozeInt = new Intent(this, AlarmReceiver.class);
        snoozeInt.putExtra(Constants.ALARM_COMM,Constants.SNOOZE_ALARM_EXTRA);
        mAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        PendingIntent snoozePendInt = PendingIntent.getBroadcast(this, 56, snoozeInt, 0);
        mAlarmManager.set(AlarmManager.RTC_WAKEUP,System.currentTimeMillis() + 60*1000, snoozePendInt);
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(66);
    }

    public boolean isNodeSaved() {
        return isNodeSaved;
    }

    public void setNodeSaved(boolean nodeSaved) {
        isNodeSaved = nodeSaved;
    }

    private void timeResetWear() {
        sendMessageToWear(Constants.MORNING_TIME_RESET + " " + Aware.getSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_MORNING_HOUR)
                + " " + Aware.getSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_MORNING_MINUTE));
    }

    private void sympResetWear(int type) {
        sendMessageToWear(Constants.SYMP_RESET + " " + type);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(Constants.TAG, "MessageService:onDestroy");
        stopForeground(true);
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
                    notifyUser(Constants.NOTIFTEXT_SYNC_SUCCESS);
                }
            } else {
                sendMessageToWear(Constants.ACK);
                if (message.equals(Constants.STATUS_LOGGING)) {
                    notifyUser(Constants.NOTIFTEXT_IN_PROGRESS);
                } else if (message.equals(Constants.STATUS_INIT)) {
                    Log.d(Constants.TAG, "MessageService:onMessageReceived:TimeInit");
                    if(isSympInitialized()){
                        initializeWear();
                    }
                }
                else if(message.equals((Constants.NOTIFY_INACTIVITY))) {
                    Log.d(Constants.TAG, "MessageService:onMessageReceived:InactiveUser");
                    notifyUserWithInactivity();
                }
            }
        }

    }


    public void notifyUserWithInactivity() {
        Intent snoozeIntent = new Intent();
        snoozeIntent.setAction(Constants.SNOOZE_ACTION);
        PendingIntent pendingIntentSnooze = PendingIntent.getBroadcast(this, 555, snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        watchClientNotifBuilder = new NotificationCompat.Builder(getApplicationContext())
                .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark_normal)
                .setContentTitle("UPMC Dash Monitor")
                .setContentText("Ready for a quick walk?")
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_MAX)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .addAction(R.drawable.ic_action_timer, "Snooze", pendingIntentSnooze);
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
        startClientNotif();
        LocalBroadcastManager.getInstance(this).registerReceiver(mBluetootLocalReceiver, new IntentFilter(Constants.BLUETOOTH_COMM));
        LocalBroadcastManager.getInstance(this).registerReceiver(mSettingsLocalReceiver, new IntentFilter(Constants.SETTING_INTENT_FILTER));
        LocalBroadcastManager.getInstance(this).registerReceiver(mSymptomsLocalReceiver, new IntentFilter(Constants.SYMPTOMS_INTENT_FILTER));
        LocalBroadcastManager.getInstance(this).registerReceiver(mNotifLocalReceiver, new IntentFilter(Constants.NOTIF_COMM));
        LocalBroadcastManager.getInstance(this).registerReceiver(mSnoozeAlarmLocalReceiver, new IntentFilter(Constants.SNOOZE_ALARM_INTENT_FILTER));
        notifyUserWithInactivity();
        return i;
    }


    private void startClientNotif() {
        Intent dashIntent = new Intent(this, UPMC.class);
        dashIntent.setAction(new Random().nextInt(50) + "_action");
        PendingIntent dashPendingIntent = PendingIntent.getActivity(this, 0, dashIntent, 0);
        messageServiceNotifBuilder = new NotificationCompat.Builder(getApplicationContext())
                .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark_normal)
                .setContentTitle("UPMC Dash")
                .setContentText(Constants.NOTIFTEXT_SYNC_FAILED)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(dashPendingIntent);
        startForeground(1, messageServiceNotifBuilder.build());
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
        setUpNodeIdentities();
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
                    notifyUser(Constants.NOTIFTEXT_SYNC_FAILED);
                } else {
                    Log.d(Constants.TAG, "MessageService:sendMessageToWear:message sent" + message);
                }
            }
        });
    }

    private void killWear(final Context context) {
        Log.d(Constants.TAG, "MessageService:killing wear and phone ");
        String message = Constants.KILL_DASH;
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
                    Log.d(Constants.TAG, "MessageService:sendMessageToWear: kill failed");
                    Toast.makeText(context, "Failed to kill Wear App. Please check manually", Toast.LENGTH_LONG).show();
                } else {
                    Log.d(Constants.TAG, "MessageService:sendMessageToWear: kill sent");
                    Toast.makeText(context, "Kill app on Wear Successful.", Toast.LENGTH_LONG).show();
                }
                sendBroadcast(new Intent(Constants.NOTIFICATION_RECEIVER_INTENT_FILTER).putExtra(Constants.COMM_KEY_NOTIF, "KILL_REQUEST"));
            }
        });
    }

    private void isWearServiceRunning(String NODE_ID) {
        setWearConnected(false);
        String message = Constants.IS_WEAR_RUNNING;
        notifyUser(Constants.NOTIFTEXT_TRY_CONNECT);
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

        final Handler handler = new Handler();
        //do something here
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isWearConnected()) {
                    Log.d(Constants.TAG, "MessageService:: Wear replied with ACK");


                } else {
                    Log.d(Constants.TAG, "MessageService:: Wear gave no ACK response");
                    notifyUser(Constants.NOTIFTEXT_SYNC_FAILED);
                }
            }
        }, 5000);
    }

    public void notifyUser(String notifContent) {
        messageServiceNotifBuilder.setContentText(notifContent);
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(1, messageServiceNotifBuilder.build());
    }
}


