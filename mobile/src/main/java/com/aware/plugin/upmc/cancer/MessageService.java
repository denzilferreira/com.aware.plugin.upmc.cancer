package com.aware.plugin.upmc.cancer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.List;
import java.util.Random;


/**
 * Created by RaghuTeja on 6/23/17.
 */

public class MessageService extends WearableListenerService implements
        GoogleApiClient.ConnectionCallbacks{

    private GoogleApiClient mGoogleApiClient;
    public boolean wearConnected;
    private NotificationCompat.Builder messageServiceNotifBuilder;

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(Constants.TAG,"MessageService:onDestroy");
        LocalBroadcastManager.getInstance(this).unregisterReceiver(localMessageReceiver);
        stopForeground(true);
        stopSelf();

    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(Constants.TAG, "MessageService: onCreated");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .build();
        if(mGoogleApiClient!=null) {
            mGoogleApiClient.connect();
        }




    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);
        Log.d(Constants.TAG, "MessageService: onMessageReceived");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int i = super.onStartCommand(intent, flags, startId);
        Log.d(Constants.TAG, "MessageService: onStartCommand");
        LocalBroadcastManager.getInstance(this).registerReceiver(localMessageReceiver, new IntentFilter(Constants.LOCAL_MESSAGE_INTENT_FILTER));


        Intent dashIntent = new Intent(this, UPMC.class);
        dashIntent.setAction(new Random().nextInt(50) + "_action");
        PendingIntent dashPendingIntent = PendingIntent.getActivity(this, 0,dashIntent,0);





        messageServiceNotifBuilder = new NotificationCompat.Builder(getApplicationContext())
                .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark_normal)
                .setContentTitle("UPMC Dash Wear Client")
                .setContentText("Looking for connections..")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(dashPendingIntent);
        startForeground(1, messageServiceNotifBuilder.build());

        return i;
    }

    private BroadcastReceiver localMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra(Constants.COMM_KEY);
            Log.d(Constants.TAG, "MessageService: Local Message Received :" + message);
            // send messages directly, please check Constants for a list of messages.
            if(message.equals(Constants.STATUS_WEAR)) {
                detectWearStatus();
            }
            else {
                sendMessageToWear(message);
            }
        }
    };

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
        if(capabilityInfo.getNodes().size() > 0){
            Log.d(Constants.TAG, "MessageService: Device Connected");
            setWearConnected(true);
            Intent connectedIntent = new Intent(Constants.WEAR_STATUS_INTENT_FILTER);
            connectedIntent.putExtra("status", "connected");
            LocalBroadcastManager.getInstance(this).sendBroadcast(connectedIntent);
            messageServiceNotifBuilder.setContentText("Connected");
            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(1, messageServiceNotifBuilder.build());
        }else{
            Log.d(Constants.TAG, "MessageService: No Devices, onCapabilityChanged");
            setWearConnected(false);
            Intent connectedIntent = new Intent(Constants.WEAR_STATUS_INTENT_FILTER);
            connectedIntent.putExtra("status", "disconnected");
            LocalBroadcastManager.getInstance(this).sendBroadcast(connectedIntent);
            messageServiceNotifBuilder.setContentText("Disconnected");
            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(1, messageServiceNotifBuilder.build());


        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(Constants.TAG, "MessageService: onConnected");
        Wearable.CapabilityApi.addCapabilityListener(mGoogleApiClient,
                this,
                Constants.CAPABILITY_WEAR_APP);
        Wearable.MessageApi.addListener(mGoogleApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(Constants.TAG, "MessageService:onConnectionSuspended");
    }


    public boolean isWearConnected() {
        return wearConnected;
    }

    private void sendMessageToWear(String message) {

        PendingResult<MessageApi.SendMessageResult> pendingResult =
                Wearable.MessageApi.sendMessage(
                        mGoogleApiClient,
                        Constants.NODE_ID,
                        Constants.CONNECTION_PATH,
                        message.getBytes());

        pendingResult.setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
            @Override
            public void onResult(@NonNull MessageApi.SendMessageResult sendMessageResult) {
                if(!sendMessageResult.getStatus().isSuccess()) {
                    Log.d(Constants.TAG, "MessageService:sendMessageToWear:message failed");
                } else {
                    Log.d(Constants.TAG, "MessageService:sendMessageToWear:message sent");
                }
            }
        });

    }

    private void detectWearStatus() {
        String message  = Constants.STATUS_WEAR;

        PendingResult<MessageApi.SendMessageResult> pendingResult =
                Wearable.MessageApi.sendMessage(
                        mGoogleApiClient,
                        Constants.NODE_ID,
                        Constants.CONNECTION_PATH,
                        message.getBytes());

        pendingResult.setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
            @Override
            public void onResult(@NonNull MessageApi.SendMessageResult sendMessageResult) {
                if(!sendMessageResult.getStatus().isSuccess()) {
                    Log.d(Constants.TAG, "MessageService:wearStatus:message failed");
                    setWearConnected(false);
                } else {
                    Log.d(Constants.TAG, "MessageService:wearStatus:message sent");
                    setWearConnected(true);
                }
            }
        });

        final Handler handler = new Handler();
        final Context mContext = this;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent dashStopIntent = new Intent(Constants.NOTIFICATION_INTENT_FILTER);
                PendingIntent dashStopPendingIntent = PendingIntent.getBroadcast(mContext,0,dashStopIntent,0);
                messageServiceNotifBuilder.addAction(R.drawable.ic_action_rotation,"STOP CLIENT",dashStopPendingIntent);
                if(isWearConnected()) {
                    Log.d(Constants.TAG, "MessageService:detectWearStatus:wear is connected");
                    setWearConnected(true);
                    messageServiceNotifBuilder.setContentText("Connected");
                    NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    mNotificationManager.notify(1, messageServiceNotifBuilder.build());
                }
                else {
                    Log.d(Constants.TAG, "MessageService:detectWearStatus:wear is not connected");
                    setWearConnected(false);
                    messageServiceNotifBuilder.setContentText("Disconnected");
                    NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    mNotificationManager.notify(1, messageServiceNotifBuilder.build());
                }
            }
        }, 5000);

    }

    public void setWearConnected(boolean wearConnected) {
        this.wearConnected = wearConnected;
    }
}
