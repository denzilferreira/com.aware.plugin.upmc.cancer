package com.aware.plugin.upmc.dash;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
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

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Created by RaghuTeja on 6/23/17.
 */

public class MessageService extends WearableListenerService implements
        GoogleApiClient.ConnectionCallbacks, DataApi.DataListener {

    int[] morningTime = {-1, -1};
    int[] nightTime = {-1, -1};
    boolean isSyncedWithPhone;
    private GoogleApiClient mGoogleApiClient;
    private Notification.Builder messageServiceNotifBuilder;
    private String NODE_ID;
    private boolean timeInvalid = false;
    private String STATUS_WEAR;
    boolean isNodeSaved = false;

    public boolean isNodeSaved() {
        return isNodeSaved;
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
        Log.d(Constants.TAG, "MessageService:readSymptomsPref: " + type);
        return type;
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
            if(intent.hasExtra(Constants.SENSOR_EXTRA_KEY)) {
                String message = intent.getStringExtra(Constants.SENSOR_EXTRA_KEY);
                Log.d(Constants.TAG, "Sending the file over to the phone");
                if(message.equals(Constants.SENSOR_ALARM)) {
                    PutDataMapRequest putDataMapRequest = PutDataMapRequest.create("/upmc-dash");
                    try {
                        Asset logAsset = FileManager.createAssetFromLogFile();
                        Log.d(Constants.TAG, "MessageService:onDataChanged: " + logAsset.getData().length);
                        putDataMapRequest.getDataMap().putAsset("logfile", logAsset);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    PutDataRequest putDataRequest = putDataMapRequest.asPutDataRequest();
                    com.google.android.gms.common.api.PendingResult<DataApi.DataItemResult> pendingResult =
                            Wearable.DataApi.putDataItem(mGoogleApiClient, putDataRequest.setUrgent());
                    pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                        @Override
                        public void onResult(@NonNull DataApi.DataItemResult dataItemResult) {
                            if(dataItemResult.getStatus().isSuccess()) {
                                Log.d(Constants.TAG, "File sent successfully");
                            }
                        }
                    });

                }
                else if(message.equals(Constants.NOTIFY_INACTIVITY)) {
                    sendMessageToPhone(Constants.NOTIFY_INACTIVITY);
                }
                else if(message.equals(Constants.NOTIFY_GREAT_JOB)) {
                    sendMessageToPhone(Constants.NOTIFY_GREAT_JOB);
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
        if (isTimeInitialized()&& isSympInitialized()) {
            if (!isMyServiceRunning(SensorService.class)) {
                startService(new Intent(this, SensorService.class).putExtra(Constants.SENSOR_START_INTENT_KEY, buildInitMessage()));
                Log.d(Constants.TAG, "onStartCommand:TimeInitialization done, Starting SensorService: ");
                setWearStatus(Constants.STATUS_LOGGING);
                setUpNodeIdentities();
            }
        } else {
            Log.d(Constants.TAG, "onStartCommand:Need TimeInitialization , Requesting Phone:");
            setWearStatus(Constants.STATUS_INIT);
        }


        LocalBroadcastManager.getInstance(this).registerReceiver(mBluetootLocalReceiver, new IntentFilter(Constants.BLUETOOTH_COMM));
        LocalBroadcastManager.getInstance(this).registerReceiver(mSensorLocalReceiver, new IntentFilter(Constants.SENSOR_INTENT_FILTER));
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
        messageServiceNotifBuilder = new Notification.Builder(getApplicationContext())
                .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark_normal)
                .setContentTitle("UPMC Dash")
                .setContentText(Constants.NOTIFTEXT_SYNC_FAILED)
                .setPriority(Notification.PRIORITY_HIGH);
        startForeground(1, messageServiceNotifBuilder.build());
    }

    public void writeTimePref(int morn_hour, int morn_minute, int night_hour, int night_minute) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(Constants.MORNING_HOUR, morn_hour);
        editor.putInt(Constants.MORNING_MINUTE, morn_minute);
        editor.putInt(Constants.NIGHT_HOUR, night_hour);
        editor.putInt(Constants.NIGHT_MINUTE, night_minute);
        editor.apply();
        storeTime(morn_hour, morn_minute,night_hour, night_minute);
    }

    public void readTimePref() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        int morn_hour = sharedPref.getInt(Constants.MORNING_HOUR, -1);
        int morn_minute = sharedPref.getInt(Constants.MORNING_MINUTE, -1);
        int night_hour = sharedPref.getInt(Constants.NIGHT_HOUR, -1);
        int night_minute = sharedPref.getInt(Constants.NIGHT_MINUTE, -1);
        Log.d(Constants.TAG, "MessageService:readTimePref:" + morn_hour + " " + morn_minute);
        storeTime(morn_hour, morn_minute,night_hour, night_minute);
    }

    private void storeTime(int morn_hour, int morn_minute, int night_hour, int night_minute) {
        this.morningTime[0] = morn_hour;
        this.morningTime[1] = morn_minute;
        this.nightTime[0] = night_hour;
        this.nightTime[1] = night_minute;

    }

    public int[] getMorningTime() {
        return this.morningTime;
    }

    public int[] getNightTime() {
        return this.nightTime;
    }

    public boolean isTimeInitialized() {
        readTimePref();
        if ((this.morningTime[0] == -1)||(this.nightTime[0] == -1)) {
            setTimeInitilaized(false);
        } else {
            setTimeInitilaized(true);
        }
        return this.timeInvalid;
    }

    public boolean isSympInitialized() {
        if(readSymptomsPref()==-1)
            return false;
        return true;
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
                if (message.contains(Constants.INIT_TS)) {
                    String[] arr = message.split("\\s+");
                    int morn_hour = Integer.parseInt(arr[1]);
                    int morn_minute = Integer.parseInt(arr[2]);
                    int night_hour = Integer.parseInt(arr[3]);
                    int nigh_minute = Integer.parseInt(arr[4]);
                    int type = Integer.parseInt(arr[5]);
                    writeTimePref(morn_hour, morn_minute, night_hour, nigh_minute);
                    writeSymptomPref(type);
                    Log.d(Constants.TAG, "onMessageReceived:INIT_TS " + morn_hour + " " + morn_minute + " " + night_hour  + " " + nigh_minute + " " + type);
                    setWearStatus(Constants.STATUS_LOGGING);
                    if(!isMyServiceRunning(SensorService.class)) {
                        startService(new Intent(this, SensorService.class).putExtra(Constants.SENSOR_START_INTENT_KEY, buildInitMessage()));
                    }
                    sendStateToPhone();
                } else if(message.contains(Constants.TIME_RESET)) {
                    String[] arr = message.split("\\s+");
                    int morn_hour = Integer.parseInt(arr[1]);
                    int morn_minute = Integer.parseInt(arr[2]);
                    int night_hour = Integer.parseInt(arr[3]);
                    int night_minute = Integer.parseInt(arr[4]);
                    writeTimePref(morn_hour, morn_minute, night_hour, night_minute);
                    Log.d(Constants.TAG, "onMessageReceived:MORNING RESET " + morn_hour + " " + morn_minute + " " + night_hour + " " + night_minute);
                    LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(Constants.RESET_BROADCAST_INTENT_FILTER).putExtra(Constants.TIME_RESET_KEY, buildInitMessage()));
                }
                else if(message.contains(Constants.SYMP_RESET)) {
                    String[] arr = message.split("\\s+");
                    int type = Integer.parseInt(arr[1]);
                    writeSymptomPref(type);
                    Log.d(Constants.TAG, "onMessageReceived:SYMP RESET " + type);
                    LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(Constants.RESET_BROADCAST_INTENT_FILTER).putExtra(Constants.SYMP_RESET_KEY, readSymptomsPref()));
                }
                else if(message.contains(Constants.IS_WEAR_RUNNING)) {
                    if(!isSyncedWithPhone()) {
                        setSyncedWithPhone(true);
                        notifyUser(Constants.NOTIFTEXT_SYNC_SUCCESS);
                    }
                    sendStateToPhone();
                }
                else if(message.contains(Constants.DEMO_NOTIF)) {
                    Log.d(Constants.TAG, "MessageService: Demo mode starting on watch");
                    LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(Constants.SENSOR_INTENT_FILTER).putExtra(Constants.SENSOR_EXTRA_KEY, Constants.NOTIFY_INACTIVITY));
                    startDemoNotif();
                }
                else if(message.contains(Constants.OK_ACTION)) {
                    Log.d(Constants.TAG, "MessageService: feedback starts");
                    LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(Constants.SENSOR_INTENT_FILTER).putExtra(Constants.SENSOR_EXTRA_KEY, Constants.OK_ACTION));
                }
            }
        }
    }


    public void startDemoNotif() {
        final NotificationManager notifMnager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        messageServiceNotifBuilder = new Notification.Builder(getApplicationContext())
                .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark_normal)
                .setContentTitle("UPMC Dash Activity Monitor (DEMO)")
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setContentText("Ready for a quick walk ?")
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setPriority(Notification.PRIORITY_MAX)
                .setDefaults(Notification.DEFAULT_ALL);
        notifMnager.notify(444, messageServiceNotifBuilder.build());
        final Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        long[] pattern = { 0, 800, 100, 800, 100, 800, 100, 800, 100, 800};
        vibrator.vibrate(pattern,0);
        Handler handler2 = new Handler();
        handler2.postDelayed(new Runnable() {
            @Override
            public void run() {
                vibrator.cancel();
            }
        }, 3000);
    }

    private String buildInitMessage() {
        StringBuilder messageBuilder = new StringBuilder();
        int[] morningTime = getMorningTime();
        int[] nightTime = getNightTime();
        int symptom = readSymptomsPref();
        messageBuilder.append(morningTime[0]);
        messageBuilder.append(" ");
        messageBuilder.append(morningTime[1]);
        messageBuilder.append(" ");
        messageBuilder.append(nightTime[0]);
        messageBuilder.append(" ");
        messageBuilder.append(nightTime[1]);
        messageBuilder.append(" ");
        messageBuilder.append(symptom);
        return messageBuilder.toString();
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

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {
        super.onDataChanged(dataEventBuffer);
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
