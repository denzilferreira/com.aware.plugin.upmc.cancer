package com.aware.plugin.upmc.dash;

import android.app.ActivityManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

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

    int[] morningTime = {-1, -1};
    boolean isSyncedWithPhone;
    private GoogleApiClient mGoogleApiClient;
    private NotificationCompat.Builder messageServiceNotifBuilder;
    private String NODE_ID;
    private boolean timeInvalid = false;
    private String STATUS_WEAR;
    boolean isNodeSaved = false;

    public boolean isNodeSaved() {
        return isNodeSaved;
    }

    public void setNodeSaved(boolean nodeSaved) {
        isNodeSaved = nodeSaved;
    }
    private BroadcastReceiver mBluetootLocalReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra(Constants.BLUETOOTH_COMM_KEY)) {
                int state = intent.getIntExtra(Constants.BLUETOOTH_COMM_KEY, BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(Constants.TAG, "MessageService:BluetoothReceiver:StateOff");
                        setSyncedWithPhone(false);
                        notifyUser(Constants.NOTIFTEXT_SYNC_FAILED_BLUETOOTH);
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(Constants.TAG, "MessageService:BluetoothReceiver:StateTurningOff");
                        setSyncedWithPhone(false);
                        notifyUser(Constants.NOTIFTEXT_SYNC_FAILED_BLUETOOTH);
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(Constants.TAG, "MessageService:BluetoothReceiver:StateOn");
                        //setUpNodeIdentities();
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(Constants.TAG, "MessageService:BluetoothReceiver:StateTurningOn");
                        break;
                }
            }
        }
    };

    private BroadcastReceiver mSensorLocalReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.hasExtra(Constants.SENSOR_INTENT_COMM)) {
                String message = intent.getStringExtra(Constants.SENSOR_INTENT_COMM);
                if(message.equals(Constants.SENSOR_ALARM)) {
                    notifyUser(Constants.NOTIFTEXT_SENDING_MESSAGE);
                }
                else if(message.equals(Constants.NOTIFY_INACTIVITY)) {
                    sendMessageToPhone(Constants.NOTIFY_INACTIVITY);
                }
            }
        }
    };

    public boolean isSyncedWithPhone() {
        return isSyncedWithPhone;
    }

    public void setSyncedWithPhone(boolean syncedWithPhone) {
        isSyncedWithPhone = syncedWithPhone;
    }

    public String getWearStatus() {
        return STATUS_WEAR;
    }

    public void setWearStatus(String STATUS_WEAR) {
        this.STATUS_WEAR = STATUS_WEAR;
    }

    public String getNODE_ID() {
        return NODE_ID;
    }

    public void setNODE_ID(String NODE_ID) {
        this.NODE_ID = NODE_ID;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(Constants.TAG, "MessageService: onDestroy");
        Wearable.CapabilityApi.removeCapabilityListener(mGoogleApiClient, this, Constants.CAPABILITY_PHONE_APP);
        Wearable.MessageApi.removeListener(mGoogleApiClient, this);
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBluetootLocalReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mSensorLocalReceiver);
        stopForeground(true);
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

    @Override
    public void onConnectedNodes(List<Node> list) {
        Log.d(Constants.TAG, "MessageService: onConnectedNodes");
        super.onConnectedNodes(list);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(Constants.TAG, "MessageService:onStartCommand");
        startClientNotif();
        if (isTimeInitialized()) {
            if (!isMyServiceRunning(SensorService.class)) {
                startService(new Intent(this, SensorService.class));
                Log.d(Constants.TAG, "onStartCommand:TimeInitialization done, Starting SensorService: ");
                setWearStatus(Constants.STATUS_LOGGING);
            }
        } else {
            Log.d(Constants.TAG, "onStartCommand:Need TimeInitialization , Requesting Phone:");
            setWearStatus(Constants.STATUS_INIT);
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(mBluetootLocalReceiver, new IntentFilter(Constants.BLUETOOTH_COMM));
        LocalBroadcastManager.getInstance(this).registerReceiver(mSensorLocalReceiver, new IntentFilter(Constants.SENSOR_ALARM));
        return START_NOT_STICKY;
    }

    private void setUpNodeIdentities() {
        notifyUser(Constants.NOTIFTEXT_TRY_CONNECT);
        Wearable.CapabilityApi.getCapability(mGoogleApiClient, Constants.CAPABILITY_PHONE_APP, CapabilityApi.FILTER_REACHABLE).setResultCallback(new ResultCallback<CapabilityApi.GetCapabilityResult>() {
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
                        sendStateToPhone();
                    }
                }
            }
        });

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(!isSyncedWithPhone()) {
                    notifyUser(Constants.NOTIFTEXT_SYNC_FAILED);
                }
                else {
                    notifyUser(Constants.NOTIFTEXT_SYNC_SUCCESS);
                }
            }
        }, 3000);
    }

    private void startClientNotif() {

        Intent dashIntent = new Intent(this, MainActivity.class);
        dashIntent.setAction(new Random().nextInt(50) + "_action");
        messageServiceNotifBuilder = new NotificationCompat.Builder(getApplicationContext())
                .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark_normal)
                .setContentTitle("UPMC Dash")
                .setContentText(Constants.NOTIFTEXT_SYNC_FAILED)
                .setPriority(NotificationCompat.PRIORITY_HIGH);
        startForeground(1, messageServiceNotifBuilder.build());
    }

    public void writeTimePref(int hour, int minute) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(Constants.MORNING_HOUR, hour);
        editor.putInt(Constants.MORNING_MINUTE, minute);
        editor.apply();
    }

    public void readTimePref() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        int minute = sharedPref.getInt(Constants.MORNING_MINUTE, -1);
        Log.d(Constants.TAG, "READ SOME PREFS " + minute);
        int hour = sharedPref.getInt(Constants.MORNING_HOUR, -1);
        storeMorningTime(hour, minute);
    }

    private void storeMorningTime(int hour, int minute) {
        this.morningTime[0] = hour;
        this.morningTime[1] = minute;

    }

    public int[] getMorningTime() {
        readTimePref();
        return this.morningTime;
    }

    public boolean isTimeInitialized() {
        getMorningTime();
        if (this.morningTime[0] == -1) {
            setTimeInitilaized(false);
        } else {
            setTimeInitilaized(true);
        }
        return this.timeInvalid;
    }

    public void setTimeInitilaized(boolean isinit) {
        this.timeInvalid = isinit;
    }

    @Override
    public void onCapabilityChanged(CapabilityInfo capabilityInfo) {
        Log.d(Constants.TAG, "MessageService: onCapability");
        super.onCapabilityChanged(capabilityInfo);
        if (capabilityInfo.getNodes().size() > 0) {
            Log.d(Constants.TAG, "MessageService: onCapability: Mobile Connected");
        } else {
            Log.d(Constants.TAG, "MessageService: onCapability: No Mobile Found");
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(Constants.TAG, "MessageService: onConnected");
        Wearable.CapabilityApi.addCapabilityListener(mGoogleApiClient,
                this,
                Constants.CAPABILITY_PHONE_APP);
        Uri uri = new Uri.Builder().scheme("wear").path("/upmc-dash").build();
        Wearable.MessageApi.addListener(mGoogleApiClient, this, uri, MessageApi.FILTER_PREFIX);
        setUpNodeIdentities();
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);
        Log.d(Constants.TAG, "MessageService:onMessageReceived");
        Log.d(Constants.TAG, "MessageService: onMessageReceived: buildPath" + messageEvent.getPath());
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.scheme("wear").path("/upmc-dash").build();
        if (messageEvent.getPath().equals(uriBuilder.toString())) {
            byte[] input = messageEvent.getData();
            String message = new String(input);
            Log.d(Constants.TAG, "MessageService: onMessageReceived: " + message);
            if(!isNodeSaved()) {
                setNODE_ID(messageEvent.getSourceNodeId());
                setNodeSaved(true);
            }
            if (message.equals(Constants.ACK)) {
                if(!isSyncedWithPhone()) {
                    notifyUser(Constants.NOTIFTEXT_SYNC_SUCCESS);
                    setSyncedWithPhone(true);
                }
            }
            else {
                sendMessageToPhone(Constants.ACK);
                if (message.contains(Constants.MORNING_TIME)) {
                    String[] arr = message.split("\\s+");
                    int hour = Integer.parseInt(arr[1]);
                    int minute = Integer.parseInt(arr[2]);
                    writeTimePref(hour, minute);
                    Log.d(Constants.TAG, "Stuff: " + hour + " " + minute);
                    setWearStatus(Constants.STATUS_LOGGING);
                    if(!isMyServiceRunning(SensorService.class)) {
                        startService(new Intent(this, SensorService.class));
                    }
                    sendStateToPhone();
                } else if(message.contains(Constants.MORNING_TIME_RESET)) {
                    String[] arr = message.split("\\s+");
                    int hour = Integer.parseInt(arr[1]);
                    int minute = Integer.parseInt(arr[2]);
                    writeTimePref(hour, minute);
                    Log.d(Constants.TAG, "Stuff: " + hour + " " + minute);
                }
                else if(message.contains(Constants.IS_WEAR_RUNNING)) {
                    if(!isSyncedWithPhone()) {
                        setSyncedWithPhone(true);
                        notifyUser(Constants.NOTIFTEXT_SYNC_SUCCESS);
                    }
                    sendStateToPhone();
                }
            }
        }
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(Constants.TAG, "MessageService: onConnectionSuspended");
    }

    private void sendMessageToPhone(final String message) {

        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.scheme("wear").path("/upmc-dash").build();
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
                    Log.d(Constants.TAG, "MessageService:sendMessageToPhone:message failed: " + message);
                } else {
                    Log.d(Constants.TAG, "MessageService:sendMessageToPhone:message sent : " + message);
                }
            }
        });

    }

    private void sendStateToPhone() {
        final String message = getWearStatus();
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.scheme("wear").path("/upmc-dash").build();
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
                    Log.d(Constants.TAG, "MessageService:sendMessageToPhone:message failed: " + message);
                } else {
                    Log.d(Constants.TAG, "MessageService:sendMessageToPhone:message sent : " + message);
                }
            }
        });

    }

    private void notifyUser(String notifContent) {
        messageServiceNotifBuilder.setContentText(notifContent);
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(1, messageServiceNotifBuilder.build());

    }

}
