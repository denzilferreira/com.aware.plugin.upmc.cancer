package com.aware.plugin.upmc.dash.services;

import android.app.ActivityManager;
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
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;
import com.aware.plugin.upmc.dash.R;
import com.aware.plugin.upmc.dash.fileutils.FileManager;
import com.aware.plugin.upmc.dash.utils.Constants;
import com.aware.plugin.upmc.dash.utils.Preferences;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.CapabilityClient;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Created by RaghuTeja on 6/23/17.
 */

public class MessageService extends WearableListenerService implements
        MessageClient.OnMessageReceivedListener,
        CapabilityClient.OnCapabilityChangedListener,
        DataClient.OnDataChangedListener {
    private String STATE_WEAR;
    private Notification.Builder setupNotifBuilder;
    private NotificationCompat.Builder setupNotifCompatBuilder;
    private MessageClient messageClient;
    private CapabilityClient capabilityClient;
    private DataClient dataClient;
    private String prevNotif = "Tap to setup your watch";
    private boolean isPhoneAround;
    private BroadcastReceiver mBluetootLocalReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            switch (state) {
                case BluetoothAdapter.STATE_OFF:
                    Log.d(Constants.TAG, "MessageService:BluetoothReceiver:StateOff");
                    setPhoneAround(false);
                    notifySetup(Constants.FAILED_PHONE_BLUETOOTH);
                    if (!enableBluetoothIfOff())
                        Toast.makeText(getApplicationContext(), "Bluetooth Error", Toast.LENGTH_SHORT).show();
                    break;
                case BluetoothAdapter.STATE_TURNING_OFF:
                    Log.d(Constants.TAG, "MessageService:BluetoothReceiver:StateTurningOff");
                    setPhoneAround(false);
                    break;
                case BluetoothAdapter.STATE_ON:
                    Log.d(Constants.TAG, "MessageService:BluetoothReceiver:StateOn");
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            scanPhoneNode();
                        }
                    }, 10000);
                    break;
                case BluetoothAdapter.STATE_TURNING_ON:
                    Log.d(Constants.TAG, "MessageService:BluetoothReceiver:StateTurningOn");
                    break;
            }

        }
    };
    private BroadcastReceiver mConnectivityReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //int state = intent.getIntExtra(ConnectivityManager.EXTRA_NETWORK_TYPE, -1);
            enableWifiIfOff();
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int i = super.onStartCommand(intent, flags, startId);
        Log.d(Constants.TAG, "MessageService:onStartCommand");
        String action = intent.getAction();
        switch (action) {
            case Constants.ACTION_REBOOT:
            case Constants.ACTION_FIRST_RUN:
                Log.d(Constants.TAG, "MessageService:onStartCommand ACTION_FIRST_RUN ");
                showSetupNotif();
                enableBluetoothIfOff();
                enableWifiIfOff();
                registerConnectivityReceiver();
                registerBluetoothReceiver();
                if(checkAndStartSession()) {
                    setWearState(Constants.STATE_LOGGING);
                    sendStateToPhone();
                }
                else {
                    setWearState(Constants.ACTION_INIT);
                }
                break;
            case Constants.ACTION_SCAN_PHONE:
                Log.d(Constants.TAG, "MessageService:onStartCommand ACTION_SCAN_PHONE");
                scanPhoneNode();
                if(isPhoneAround())
                    sendMessageToPhone(Constants.ACTION_SYNC_SETTINGS);
                break;
            case Constants.ACTION_SYNC_DATA:
                Log.d(Constants.TAG, "MessageService:onStartCommand ACTION_SYNC_DATA");
                syncDataWithPhone();
                break;
            case Constants.ACTION_NOTIFY_INACTIVITY:
                Log.d(Constants.TAG, "MessageService:onStartCommand ACTION_NOTIFY_INACTIVITY");
                sendMessageToPhone(Constants.NOTIFY_INACTIVITY);
                break;
            case Constants.ACTION_NOTIFY_GREAT_JOB:
                Log.d(Constants.TAG, "MessageService:onStartCommand ACTION_NOTIFY_GREAT_JOB");
                sendMessageToPhone(Constants.NOTIFY_GREAT_JOB);
                break;
            case Constants.ACTION_SNOOZE_ALARM:
                Log.d(Constants.TAG, "MessageService:onStartCommand ACTION_SNOOZE_ALARM");
                sendMessageToPhone(Constants.NOTIFY_INACTIVITY_SNOOZED);
                break;
            case Constants.ACTION_NOTIF_NO:
            case Constants.ACTION_NOTIF_OK:
            case Constants.ACTION_NOTIF_SNOOZE:
                Log.d(Constants.TAG, "MessageService:onStartCommand " + action);
                sendMessageToPhone(action);
                break;
            case Constants.ACTION_STOP_SELF:
                Log.d(Constants.TAG, "MessageService:onStartCommand: ACTION_STOP_SELF");
                unregisterReceiver(mBluetootLocalReceiver);
                unregisterReceiver(mConnectivityReceiver);
                messageClient.removeListener(this);
                capabilityClient.removeListener(this);
                dataClient.removeListener(this);
                capabilityClient.removeLocalCapability(Constants.CAPABILITY_DEMO_WEAR_APP);
                stopForeground(true);
                stopSelf();
                break;
            default:
                return i;
        }
        return i;
    }

    public boolean checkAndStartSession() {
        if (isPhoneNodeSaved() && Preferences.isTimeAndRatingInitialized(getApplicationContext())) {
            if (!isMyServiceRunning(SensorService.class)) {
                Log.d(Constants.TAG, "checkAndStartSession:TimeInitialization done, Starting SensorService: ");
                sendSensorServiceAction(Constants.ACTION_FIRST_RUN);
            }
            return true;
        } else {
            Log.d(Constants.TAG, "checkAndStartSession:TimeInitialization failed");
            return false;
        }
    }


    public boolean isPhoneAround() {
        return isPhoneAround;
    }

    public void setPhoneAround(boolean phoneAround) {
        isPhoneAround = phoneAround;
    }


    public void writePhoneNodeId(String nodeId) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(Constants.PREFERENCES_KEY_PHONE_NODEID, nodeId);
        editor.apply();
        Log.d(Constants.TAG, "MessageService:writePhoneNodeId: " + nodeId);
    }

    public boolean enableBluetoothIfOff() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter!=null) {
            boolean isEnabled = bluetoothAdapter.isEnabled();
            return isEnabled || bluetoothAdapter.enable();
        }
        else {
            return false;
        }
    }

    public void registerBluetoothReceiver() {
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mBluetootLocalReceiver, filter);
    }

    public String getWearState() {
        return STATE_WEAR;
    }

    public void setWearState(String STATUS_WEAR) {
        this.STATE_WEAR = STATUS_WEAR;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBluetootLocalReceiver);
        unregisterReceiver(mConnectivityReceiver);
        Log.d(Constants.TAG, "MessageService: onDestroy");
        messageClient.removeListener(this);
        capabilityClient.removeListener(this);
        dataClient.removeListener(this);
        capabilityClient.removeLocalCapability(Constants.CAPABILITY_DEMO_WEAR_APP);
        stopForeground(true);
        stopSelf();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(Constants.TAG, "MessageService: onCreate");
        Wearable.WearableOptions options = new Wearable.WearableOptions.Builder().setLooper(Looper.myLooper()).build();
        messageClient = Wearable.getMessageClient(this, options);
        capabilityClient = Wearable.getCapabilityClient(this, options);
        dataClient = Wearable.getDataClient(this, options);
        messageClient.addListener(this, Constants.MESSAGE_URI, MessageClient.FILTER_PREFIX);
        capabilityClient.addListener(this, Constants.CAPABILITY_PHONE_APP);
        capabilityClient.addLocalCapability(Constants.CAPABILITY_WEAR_APP);
        dataClient.addListener(this);

    }

    @Override
    public void onConnectedNodes(List<Node> list) {
        Log.d(Constants.TAG, "MessageService: onConnectedNodes");
        super.onConnectedNodes(list);
    }

    public void enableWifiIfOff() {
        WifiManager wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled())
            wifiManager.setWifiEnabled(true);
    }

    public void registerConnectivityReceiver() {
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mConnectivityReceiver, filter);
    }

    public void syncDataWithPhone() {
        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create("/upmc-dash");
        try {
            Asset logAsset = FileManager.createAssetFromLogFile();
            Log.d(Constants.TAG, "MessageService:onDataChanged: ");
            putDataMapRequest.getDataMap().putAsset("logfile", logAsset);
        } catch (IOException e) {
            e.printStackTrace();
        }
        PutDataRequest putDataRequest = putDataMapRequest.asPutDataRequest();
        dataClient.putDataItem(putDataRequest.setUrgent()).addOnSuccessListener(new OnSuccessListener<DataItem>() {
            @Override
            public void onSuccess(DataItem dataItem) {
                    Log.d(Constants.TAG, "syncDataWithPhone: onSuccess " );
            }
            })
                .addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(Constants.TAG, "syncDataWithPhone: onFailure " );
            }
        }).addOnCompleteListener(new OnCompleteListener<DataItem>() {
            @Override
            public void onComplete(@NonNull Task<DataItem> task) {
                Log.d(Constants.TAG, "syncDataWithPhone: onComplete " );
            }
        });
    }

    public boolean isPhoneNodeSaved() {
        return !(Constants.PREFERENCES_DEFAULT_PHONE_NODEID.equals(readPhoneNodeId()));
    }

    public String readPhoneNodeId() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String nodeId = sharedPref.getString(Constants.PREFERENCES_KEY_PHONE_NODEID, Constants.PREFERENCES_DEFAULT_PHONE_NODEID);
        if (nodeId.equals(Constants.PREFERENCES_DEFAULT_PHONE_NODEID))
            Log.d(Constants.TAG, "MessageService:readWearNodeId: " + nodeId);
        return nodeId;
    }

    public boolean isPhoneNodeIdPresent(Set<Node> nodes) {
        if (nodes.size() == 0) {
            Log.d(Constants.TAG, "isPhoneNodeIdPresent: no connected nodes");
            return false;
        }
        for (Node node : nodes) {
            if (node.getId().equals(readPhoneNodeId()))
                return true;
        }
        Log.d(Constants.TAG, "isPhoneNodeIdPresent: no nodes with wear nodeID");
        return false;
    }

    private void scanPhoneNode() {
        notifySetup(Constants.NOTIFTEXT_TRY_CONNECT);
        if (!isPhoneNodeSaved()) {
            notifySetup(Constants.SETUP_WEAR);
            return;
        }
        capabilityClient.getCapability(Constants.CAPABILITY_PHONE_APP, CapabilityClient.FILTER_REACHABLE).addOnCompleteListener(new OnCompleteListener<CapabilityInfo>() {
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
                        if (isPhoneNodeIdPresent(nodes)) {
                            Log.d(Constants.TAG, "scanPhoneNode: connected");
                            setPhoneAround(true);
                        } else {
                            Log.d(Constants.TAG, "scanPhoneNode: disconnected");
                            setPhoneAround(false);
                        }
                    }
                });
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isPhoneAround()) {
                    notifySetup(Constants.CONNECTED_PHONE);
                } else {
                    notifySetup(Constants.FAILED_PHONE);
                }
            }
        }, 5000);
    }

    private void showSetupNotif() {
        final String contentText = isPhoneNodeSaved() ? Constants.FAILED_PHONE : Constants.SETUP_WEAR;
        Intent dashIntent = new Intent(this, MessageService.class);
        dashIntent.setAction(Constants.ACTION_SCAN_PHONE);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(Constants.MESSAGE_SERVICE_NOTIFICATION_CHANNEL_ID, "MESSAGE_SERVICE", NotificationManager.IMPORTANCE_MIN);
            notificationChannel.setDescription("UPMC Dash notification channel");
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.setVibrationPattern(new long[]{0, 800, 100, 800, 100, 800, 100, 800, 100, 800});
            notificationChannel.enableVibration(true);
            notificationManager.createNotificationChannel(notificationChannel);
            setupNotifBuilder = new Notification.Builder(this, Constants.MESSAGE_SERVICE_NOTIFICATION_CHANNEL_ID);

            PendingIntent dashPendingIntent = PendingIntent.getForegroundService(this, 0, dashIntent, 0);
            setupNotifBuilder.setAutoCancel(false)
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark_normal)
                    .setContentTitle("Dash Setup")
                    .setContentText(contentText)
                    .setOngoing(true)
                    .setContentIntent(dashPendingIntent);
            startForeground(Constants.MESSAGE_SERVICE_NOTIFICATION_ID, setupNotifBuilder.build());

        } else {
            PendingIntent dashPendingIntent = PendingIntent.getService(this, 0, dashIntent, 0);
            setupNotifCompatBuilder = new NotificationCompat.Builder(this, Constants.MESSAGE_SERVICE_NOTIFICATION_CHANNEL_ID);
            setupNotifCompatBuilder.setAutoCancel(false)
                    .setOngoing(true)
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark_normal)
                    .setContentTitle("Dash Setup")
                    .setContentText(contentText)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentInfo("info")
                    .setContentIntent(dashPendingIntent);
            startForeground(Constants.MESSAGE_SERVICE_NOTIFICATION_ID, setupNotifCompatBuilder.build());
        }
    }

    @Override
    public void onCapabilityChanged(@NonNull CapabilityInfo capabilityInfo) {
        Log.d(Constants.TAG, "MessageService: onCapability");
        super.onCapabilityChanged(capabilityInfo);
        if(isPhoneNodeSaved()) {
            if (isPhoneNodeIdPresent(capabilityInfo.getNodes())) {
                Log.d(Constants.TAG, "onCapabilityChanged: phone is connected");
                notifySetup(Constants.CONNECTED_PHONE);
            }
            else {
                Log.d(Constants.TAG, "onCapabilityChanged: phone is disconnected");
                notifySetup(Constants.FAILED_PHONE);
            }
        }
    }



    @Override
    public void onMessageReceived(@NonNull MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);
        Log.d(Constants.TAG, "MessageService:onMessageReceived");
        byte[] input = messageEvent.getData();
        String message = new String(input);
        setPhoneAround(true);
        if (messageEvent.getPath().equals(Constants.MESSAGE_URI.toString())) {
            switch (message.split("\\s+")[0]) {
                case Constants.ACK:
                    Log.d(Constants.TAG, "MessageService:onMessageReceived:ACK");
                    setPhoneAround(true);
                    break;
                case Constants.ACTION_INIT:
                    Log.d(Constants.TAG, "MessageService:onMessageReceived:INIT_TS");
                    parseAndStorePref(message);
                    checkAndStartSession();
                    sendStateToPhone();
                    break;
                case Constants.IS_WEAR_RUNNING:
                    Log.d(Constants.TAG, "MessageService:onMessageReceived:IS_WEAR_RUNNING");
                    sendStateToPhone();
                    break;
                case Constants.ACTION_SETUP_WEAR:
                    Log.d(Constants.TAG, "MessageService:onMessageReceived:ACTION_SETUP_WEAR");
                    writePhoneNodeId(messageEvent.getSourceNodeId());
                    notifySetup(Constants.CONNECTED_PHONE);
                    sendAckToPhone();
                    break;
                case Constants.ACTION_SETTINGS_CHANGED:
                    Log.d(Constants.TAG, "MessageService:onMessageReceived:ACTION_SETTINGS_CHANGED");
                    parseAndStorePref(message);
                    sendAckToPhone();
                    break;
                case Constants.ACTION_NOTIF_OK:
                case Constants.ACTION_NOTIF_NO:
                case Constants.ACTION_NOTIF_SNOOZE:
                    Log.d(Constants.TAG, "MessageService:onMessageReceived:" + message);
                    sendAckToPhone();
                    sendSensorServiceAction(message + "_PHONE");
                    break;
                case Constants.ACTION_SYNC_SETTINGS:
                    Log.d(Constants.TAG, "MessageService:onMessageReceived:ACTION_SYNC_SETTINGS");
                    sendAckToPhone();
                    parseAndStorePrefIfNeeded(message);
                    break;
            }

        }
    }

    public void parseAndStorePrefIfNeeded(String message) {
        String[] arr = message.split("\\s+");
        int[] newConfig = new int[5];
        newConfig[0] = Integer.parseInt(arr[1]);
        newConfig[1] = Integer.parseInt(arr[2]);
        newConfig[2] = Integer.parseInt(arr[3]);
        newConfig[3] = Integer.parseInt(arr[4]);
        newConfig[4] = Integer.parseInt(arr[5]);
        int[] oldConfig = Preferences.readIntoArray(getApplicationContext());
        for (int i =0; i<5; i++) {
            if(oldConfig[i]!=newConfig[i]) {
                parseAndStorePref(message);
                Log.d(Constants.TAG, "MessageReceived:parseAndStorePrefIfNeeded:change detected");
                return;
            }
        }
        Log.d(Constants.TAG, "MessageReceived:parseAndStorePrefIfNeeded:no change");
    }





    public void parseAndStorePref(String message) {
        String[] arr = message.split("\\s+");
        int morn_hour = Integer.parseInt(arr[1]);
        int morn_minute = Integer.parseInt(arr[2]);
        int night_hour = Integer.parseInt(arr[3]);
        int nigh_minute = Integer.parseInt(arr[4]);
        int type = Integer.parseInt(arr[5]);
        Preferences.writeTime(getApplicationContext(), morn_hour, morn_minute, night_hour, nigh_minute);
        Preferences.writeSymptomRating(getApplicationContext(),type);
        Log.d(Constants.TAG, "MessageService:parseAndStorePref:" + morn_hour + " " + morn_minute + " " + night_hour + " " + nigh_minute + " " + type);
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


    private void sendMessageToPhone(String message) {
        Log.d(Constants.TAG, "MessageService:sendMessageToPhone " + message);
        if (!isPhoneNodeSaved())
            return;
        final String nodeID = readPhoneNodeId();
        messageClient.sendMessage(nodeID, Constants.MESSAGE_URI.toString(), message.getBytes()).
                addOnSuccessListener(new OnSuccessListener<Integer>() {
                    @Override
                    public void onSuccess(Integer integer) {
                        Log.d(Constants.TAG, "sendMessageToPhone:onSuccess " + integer);

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(Constants.TAG, "sendMessageToPhone:onFailure");


                    }
                })
                .addOnCompleteListener(new OnCompleteListener<Integer>() {
                    @Override
                    public void onComplete(@NonNull Task<Integer> task) {
                        Log.d(Constants.TAG, "sendMessageToPhone:onComplete, waiting for ACK from phone...");

                    }
                });

        setPhoneAround(false);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isPhoneAround()) {
                    Log.d(Constants.TAG, "sendMessageToPhone: ACK received, phone is around");
                    notifySetup(Constants.CONNECTED_PHONE);
                }
                else {
                    Log.d(Constants.TAG, "sendMessageToPhone: ACK not received, phone is not around");
                    notifySetup(Constants.FAILED_PHONE);
                }
            }
        }, 5000);

    }


    private void sendStateToPhone() {
        Log.d(Constants.TAG, "MessageService:sendStateToPhone " + getWearState());
        if (!isPhoneNodeSaved())
            return;
        final String nodeID = readPhoneNodeId();
        messageClient.sendMessage(nodeID, Constants.MESSAGE_URI.toString(), getWearState().getBytes()).
                addOnSuccessListener(new OnSuccessListener<Integer>() {
                    @Override
                    public void onSuccess(Integer integer) {
                        Log.d(Constants.TAG, "sendStateToPhone:onSuccess" + integer);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(Constants.TAG, "sendStateToPhone:onFailure");
                    }
                })
                .addOnCompleteListener(new OnCompleteListener<Integer>() {
                    @Override
                    public void onComplete(@NonNull Task<Integer> task) {
                        Log.d(Constants.TAG, "sendStateToPhone:onComplete");
                    }
                });
    }

    private void sendAckToPhone() {
        Log.d(Constants.TAG, "MessageService:sendAckToPhone ");
        if (!isPhoneNodeSaved())
            return;
        final String nodeID = readPhoneNodeId();
        messageClient.sendMessage(nodeID, Constants.MESSAGE_URI.toString(), Constants.ACK.getBytes()).
                addOnSuccessListener(new OnSuccessListener<Integer>() {
                    @Override
                    public void onSuccess(Integer integer) {
                        Log.d(Constants.TAG, "sendAckToPhone:onSuccess" + integer);

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(Constants.TAG, "sendAckToPhone:onFailure");


                    }
                })
                .addOnCompleteListener(new OnCompleteListener<Integer>() {
                    @Override
                    public void onComplete(@NonNull Task<Integer> task) {
                        Log.d(Constants.TAG, "sendAckToPhone:onComplete");

                    }
                });
    }

    @Override
    public void onDataChanged(@NonNull DataEventBuffer dataEventBuffer) {
        super.onDataChanged(dataEventBuffer);
    }

    public void sendSensorServiceAction(String action) {
        Intent intent = new Intent(getApplicationContext(), SensorService.class).setAction(action);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    private void notifySetup(String contentText) {
        boolean canNotify = !this.prevNotif.equals(contentText);
        if(canNotify) {
            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                setupNotifBuilder.setContentText(contentText);
                mNotificationManager.notify(Constants.MESSAGE_SERVICE_NOTIFICATION_ID, setupNotifBuilder.build());
            } else {
                setupNotifCompatBuilder.setContentText(contentText);
                mNotificationManager.notify(Constants.MESSAGE_SERVICE_NOTIFICATION_ID, setupNotifCompatBuilder.build());
            }
            this.prevNotif = contentText;

        }

    }
}
