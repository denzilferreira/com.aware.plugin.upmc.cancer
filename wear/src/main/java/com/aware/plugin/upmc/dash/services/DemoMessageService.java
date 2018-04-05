package com.aware.plugin.upmc.dash.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.aware.plugin.upmc.dash.R;
import com.aware.plugin.upmc.dash.utils.Constants;
import com.aware.plugin.upmc.dash.utils.InvalidPhoneNodeException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.CapabilityClient;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.List;
import java.util.Set;

/**
 * Created by RaghuTeja on 3/23/18.
 */

public class DemoMessageService extends WearableListenerService implements MessageClient.OnMessageReceivedListener,
        CapabilityClient.OnCapabilityChangedListener,
        DataClient.OnDataChangedListener {


    private MessageClient messageClient;
    private CapabilityClient capabilityClient;
    private DataClient dataClient;

    public boolean isPhoneAround() {
        return isPhoneAround;
    }

    public void setPhoneAround(boolean phoneAround) {
        isPhoneAround = phoneAround;
    }

    private boolean isPhoneAround;


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(Constants.TAG, "onStartCommand");
        if(intent!=null) {
            String action = intent.getAction();
            if(action!=null) {
                switch (action) {
                    case Constants.ACTION_FIRST_RUN:
                        showDemoNotif();
                        break;
                    case Constants.ACTION_SETUP_WEAR:
                        scanPhoneNode();
                        break;
                    case Constants.ACTION_NOTIF_OK:
                        sendMessageToPhone("test");
                        break;
                }

            }
        }
        return super.onStartCommand(intent, flags, startId);

    }

    @Override
    public void onCreate() {
        Log.d(Constants.TAG, "onCreate");
        Wearable.WearableOptions options = new Wearable.WearableOptions.Builder().setLooper(Looper.myLooper()).build();
        messageClient = Wearable.getMessageClient(this, options);
        capabilityClient = Wearable.getCapabilityClient(this, options);
        dataClient = Wearable.getDataClient(this, options);
        messageClient.addListener(this, Constants.MESSAGE_URI, MessageClient.FILTER_PREFIX);
        capabilityClient.addListener(this, Constants.CAPABILITY_DEMO_PHONE_APP);
        capabilityClient.addLocalCapability(Constants.CAPABILITY_DEMO_WEAR_APP);
        dataClient.addListener(this);
        super.onCreate();
    }




    @Override
    public void onDestroy() {
        Log.d(Constants.TAG, "onDestroy");
        super.onDestroy();
        capabilityClient.removeLocalCapability(Constants.CAPABILITY_DEMO_WEAR_APP);
        messageClient.removeListener(this);
        capabilityClient.removeListener(this);
        dataClient.removeListener(this);

    }

    private void scanPhoneNode() {
        if(!isPhoneNodeSaved())
            return;
        capabilityClient.getCapability(Constants.CAPABILITY_DEMO_PHONE_APP, CapabilityClient.FILTER_REACHABLE).addOnCompleteListener(new OnCompleteListener<CapabilityInfo>() {
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
                        }
                        else {
                            Log.d(Constants.TAG, "scanPhoneNode: disconnected");
                        }
                    }
                });
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


    public String getPhoneNodeID() throws InvalidPhoneNodeException {
        String phoneNodeId = readPhoneNodeId();
        if (phoneNodeId.equals(Constants.PREFERENCES_DEFAULT_PHONE_NODEID)) {
            throw new InvalidPhoneNodeException(Constants.PREFERENCES_DEFAULT_PHONE_NODEID);
        } else {
            return phoneNodeId;
        }
    }


    public String readPhoneNodeId() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String nodeId = sharedPref.getString(Constants.PREFERENCES_KEY_PHONE_NODEID, Constants.PREFERENCES_DEFAULT_PHONE_NODEID);
        if (nodeId.equals(Constants.PREFERENCES_DEFAULT_PHONE_NODEID))
            Log.d(Constants.TAG, "MessageService:readWearNodeId: " + nodeId);
        return nodeId;
    }




    private void sendAckToPhone() {
        final String nodeID = readPhoneNodeId();
        messageClient.sendMessage(nodeID, Constants.MESSAGE_URI.toString(),Constants.ACK.getBytes()).
                addOnSuccessListener(new OnSuccessListener<Integer>() {
                    @Override
                    public void onSuccess(Integer integer) {
                        Log.d(Constants.TAG, "sendAckToPhone:oNsuccess " + integer);

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
                        Log.d(Constants.TAG, "sendAckToPhone:onComplete, sent ACK to phone...");

                    }
                });

    }



    private void sendMessageToPhone(String message) {
        Log.d(Constants.TAG, "MessageService:sendMessageToPhone " + message);
        if(!isPhoneNodeSaved())
            return;
        final String nodeID = readPhoneNodeId();
        messageClient.sendMessage(nodeID, Constants.MESSAGE_URI.toString(), message.getBytes()).
                addOnSuccessListener(new OnSuccessListener<Integer>() {
                    @Override
                    public void onSuccess(Integer integer) {
                        Log.d(Constants.TAG, "sendMessageToPhone:sendmessage:onsuccess " + integer);

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(Constants.TAG, "sendmessage:onfailure");


                    }
                })
                .addOnCompleteListener(new OnCompleteListener<Integer>() {
                    @Override
                    public void onComplete(@NonNull Task<Integer> task) {
                        Log.d(Constants.TAG, "sendmessage:oncomplete, waiting for ACK from phone...");

                    }
                });

        setPhoneAround(false);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isPhoneAround())
                    Log.d(Constants.TAG, "sendMessageToPhone: ACK received, phone is around");
                else
                    Log.d(Constants.TAG, "sendMessageToPhone: ACK not received phone is not around");
            }
        }, 5000);

    }



    @Override
    public void onMessageReceived(@NonNull MessageEvent messageEvent) {
        byte[] input = messageEvent.getData();
        String message = new String(input);
        Log.d(Constants.TAG, "MessageService: onMessageReceived: " + message );
        setPhoneAround(true);
        switch (message) {
            case Constants.ACTION_SETUP_WEAR:
                String phoneNodeID = messageEvent.getSourceNodeId();
                writePhoneNodeId(phoneNodeID);
                sendAckToPhone();
                break;
            case "test":
                sendAckToPhone();
                stopSelf();
                break;
        }
    }

    public void writePhoneNodeId(String nodeId) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(Constants.PREFERENCES_KEY_PHONE_NODEID, nodeId);
        editor.apply();
        Log.d(Constants.TAG, "MessageService:writePhoneNodeId: " + nodeId);
    }

    @Override
    public void onDataChanged(@NonNull DataEventBuffer dataEventBuffer) {

    }

    @Override
    public void onCapabilityChanged(@NonNull CapabilityInfo capabilityInfo) {
        Log.d(Constants.TAG, "onCapabilityChanged)");
        if(isPhoneNodeSaved()) {
            if (isPhoneNodeIdPresent(capabilityInfo.getNodes())) {
                Log.d(Constants.TAG, "onCapabilityChanged: phone is connected");
            }
        }
    }

    public boolean isPhoneNodeSaved() {
        return !(Constants.PREFERENCES_DEFAULT_PHONE_NODEID.equals(readPhoneNodeId()));
    }

    @Override
    public void onConnectedNodes(List<Node> list) {
        super.onConnectedNodes(list);
    }



    private void showDemoNotif() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(Constants.DEMO_NOTIF_CHNL_ID, "UPMC Dash Demo", NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.setDescription("UPMC Dash Demo");
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.enableVibration(true);
            notificationManager.createNotificationChannel(notificationChannel);
            Notification.Builder notificationBuilder = new Notification.Builder(this, Constants.DEMO_NOTIF_CHNL_ID);
            notificationBuilder.setAutoCancel(false)
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark_normal)
                    .setContentTitle("UPMC Dash Demo")
                    .setGroup("Survey")
                    .setContentText(Constants.DEMO_MODE);
            startForeground(Constants.DEMO_NOTIF_ID, notificationBuilder.build());

        } else {
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, Constants.DEMO_NOTIF_CHNL_ID);
            notificationBuilder.setAutoCancel(false)
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark_normal)
                    .setContentTitle("UPMC Dash Demo")
                    .setContentText(Constants.DEMO_MODE)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setGroup("Survey")
                    .setContentInfo("Survey Notification");
            startForeground(Constants.DEMO_NOTIF_ID,notificationBuilder.build());
        }
    }


}
