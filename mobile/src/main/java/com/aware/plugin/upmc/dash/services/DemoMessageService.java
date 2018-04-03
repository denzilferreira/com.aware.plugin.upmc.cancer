package com.aware.plugin.upmc.dash.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.aware.plugin.upmc.dash.R;
import com.aware.plugin.upmc.dash.activities.UPMC;
import com.aware.plugin.upmc.dash.utils.Constants;
import com.aware.plugin.upmc.dash.utils.InvalidWearNodeException;
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
import com.google.android.gms.wearable.NodeClient;
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


    int count = 0;
    private MessageClient messageClient;
    private CapabilityClient capabilityClient;
    private DataClient dataClient;
    private boolean isWearAround = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(Constants.TAG, "onStartCommand");
        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case Constants.ACTION_REBOOT:
                    case Constants.ACTION_FIRST_RUN:
                        if (readWearNodeId().equals(Constants.PREFERENCES_DEFAULT_WEAR_NODEID))
                            Log.d(Constants.TAG, "nodeNotSaved");
                        else
                            Log.d(Constants.TAG, "nodeAlreadySaved");
                        showDemoNotif();
                        saveConnectedWearNode();
                        break;
                    case Constants.ACTION_SETUP_WEAR:
                        scanWearNode();
                        break;
                    case Constants.ACTION_NOTIF_OK:
                        sendMessageToWear("test");

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
        capabilityClient.addListener(this, Constants.CAPABILITY_DEMO_WEAR_APP);
        dataClient.addListener(this);
        capabilityClient.addLocalCapability(Constants.CAPABILITY_DEMO_PHONE_APP);
        super.onCreate();

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

    private void sendMessageToWear(final String message) {
        Log.d(Constants.TAG, "MessageService:sendMessageToWear " + message);
        if(!isWearNodeSaved())
            return;
        final String nodeID = readWearNodeId();
        messageClient.sendMessage(nodeID, Constants.MESSAGE_URI.toString(), message.getBytes()).
                addOnSuccessListener(new OnSuccessListener<Integer>() {
                    @Override
                    public void onSuccess(Integer integer) {
                        Log.d(Constants.TAG, "sendmessage:onsuccess " + integer);

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
                        Log.d(Constants.TAG, "sendmessage:oncomplete, waiting for ACK from wear...");

                    }
                });

        setWearAround(false);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                if (isWearAround())
                    Log.d(Constants.TAG, "sendMessageToWear: ACK received, wear is around");
                else
                    Log.d(Constants.TAG, "sendMessageToWear: ACK not received wear is not around");
            }
        }, 5000);

    }

    private void saveConnectedWearNode() {
        capabilityClient.getCapability(Constants.CAPABILITY_DEMO_WEAR_APP, CapabilityClient.FILTER_REACHABLE).addOnCompleteListener(new OnCompleteListener<CapabilityInfo>() {
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
                            writeWearNodeId(wearNode.getId());
                            sendMessageToWear(Constants.ACTION_SETUP_WEAR);
                        }

                    }
                });

    }

    private void scanWearNode() {
        if(!isWearNodeSaved())
            return;
        if(readWearNodeId().equals(Constants.PREFERENCES_DEFAULT_WEAR_NODEID))
            return;
        capabilityClient.getCapability(Constants.CAPABILITY_DEMO_WEAR_APP, CapabilityClient.FILTER_REACHABLE).addOnCompleteListener(new OnCompleteListener<CapabilityInfo>() {
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
                        if (isWearNodeIdPresent(nodes)) {
                            Log.d(Constants.TAG, "scanWearNode: connected");
                        }
                        else {
                            Log.d(Constants.TAG, "scanWearNode: disconnected");
                        }
                    }
                });
    }

    @Override
    public void onDestroy() {
        Log.d(Constants.TAG, "onDestroy");
        super.onDestroy();
        messageClient.removeListener(this);
        capabilityClient.removeListener(this);
        capabilityClient.removeLocalCapability(Constants.CAPABILITY_DEMO_PHONE_APP);
        dataClient.removeListener(this);
    }

    @Override
    public void onMessageReceived(@NonNull MessageEvent messageEvent) {
        byte[] input = messageEvent.getData();
        String message = new String(input);
        Log.d(Constants.TAG, "MessageService: onMessageReceived: " + message);
        setWearAround(true);
        switch (message) {
            case "test":
                sendAckToWear();
                stopSelf();
                break;
        }

    }


    private void sendAckToWear() {
        if(!isWearNodeSaved())
            return;
        final String nodeID = readWearNodeId();
        messageClient.sendMessage(nodeID, Constants.MESSAGE_URI.toString(),Constants.ACK.getBytes()).
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

    @Override
    public void onDataChanged(@NonNull DataEventBuffer dataEventBuffer) {

    }

    @Override
    public void onCapabilityChanged(@NonNull CapabilityInfo capabilityInfo) {
        Log.d(Constants.TAG, "onCapabilityChanged)");
        if(isWearNodeSaved()) {
            if (isWearNodeIdPresent(capabilityInfo.getNodes())) {
                Log.d(Constants.TAG, "onCapabilityChanged: wear is connected");
            }
        }

    }

    public boolean isWearNodeSaved() {
        return !(Constants.PREFERENCES_DEFAULT_WEAR_NODEID.equals(readWearNodeId()));
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

    public Node getConnectedWearNode(Set<Node> nodes) {
        if (nodes.size() == 0) {
            Log.d(Constants.TAG, "isWatchMakePresent: no connected nodes");
            return null;
        }

        for (Node node : nodes) {
            if (node.getDisplayName().toLowerCase().contains("lg watch")) {
                Log.d(Constants.TAG, "isWatchMakePresent: wear nodeId is present");
                return node;
            }
        }
        Log.d(Constants.TAG, "isWatchMakePresent: no connected nodes");
        return null;
    }

    @Override
    public void onConnectedNodes(List<Node> list) {
        Log.d(Constants.TAG, "onConnectedNodes: ");
        super.onConnectedNodes(list);
    }

    private void showDemoNotif() {
        final Intent dashIntent = new Intent(this, UPMC.class);
        dashIntent.setAction(Constants.ACTION_SURVEY);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(Constants.DEMO_NOTIF_CHNL_ID, "UPMC Dash Demo", NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.setDescription("UPMC Dash Demo");
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.enableVibration(true);
            notificationManager.createNotificationChannel(notificationChannel);
            Notification.Builder notificationBuilder = new Notification.Builder(this, Constants.DEMO_NOTIF_CHNL_ID);
            PendingIntent dashPendingIntent = PendingIntent.getActivity(this, 0, dashIntent, 0);
            notificationBuilder.setAutoCancel(false)
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark_normal)
                    .setContentTitle("UPMC Dash Demo")
                    .setGroup("Survey")
                    .setContentText(Constants.DEMO_MODE)
                    .setContentIntent(dashPendingIntent);
            startForeground(Constants.DEMO_NOTIF_ID, notificationBuilder.build());

        } else {
            PendingIntent dashPendingIntent = PendingIntent.getActivity(this, 0, dashIntent, 0);
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, Constants.DEMO_NOTIF_CHNL_ID);
            notificationBuilder.setAutoCancel(false)
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark_normal)
                    .setContentTitle("UPMC Dash Demo")
                    .setContentText(Constants.DEMO_MODE)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setGroup("Survey")
                    .setContentInfo("Survey Notification")
                    .setContentIntent(dashPendingIntent);
            startForeground(Constants.DEMO_NOTIF_ID, notificationBuilder.build());
        }
    }

    public String getWearNodeID() throws InvalidWearNodeException {
        String wearNodeId = readWearNodeId();
        if (wearNodeId.equals(Constants.PREFERENCES_DEFAULT_WEAR_NODEID)) {
            throw new InvalidWearNodeException(Constants.PREFERENCES_DEFAULT_WEAR_NODEID);
        } else {
            return wearNodeId;
        }
    }

    public boolean isWearAround() {
        return isWearAround;
    }

    public void setWearAround(boolean wearAround) {
        isWearAround = wearAround;
    }
}
