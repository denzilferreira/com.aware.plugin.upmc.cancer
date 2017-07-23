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
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.ArrayList;
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
    private int count = 0;


    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(Constants.TAG,"MessageService:onDestroy");
        stopForeground(true);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mNotifBroadcastReceiver);
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
        LocalBroadcastManager.getInstance(this).registerReceiver(mNotifBroadcastReceiver, new IntentFilter(Constants.NOTIFICATION_MESSAGE_INTENT_FILTER));

    }



    private BroadcastReceiver mNotifBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if(intent.hasExtra(Constants.COMM_KEY_MSGSERVICE)) {
                if (intent.getStringExtra(Constants.COMM_KEY_MSGSERVICE).equals(Constants.START_SC)) {
                    Log.d(Constants.TAG, "START SC");
                    sendMessageToWear(Constants.START_SC);
                    ArrayList<NotificationCompat.Action> actionList = messageServiceNotifBuilder.mActions;
                    for (int i = 0; i < actionList.size(); i++) {
                        if (actionList.get(i).getTitle().equals("START SESSION")) {
                            actionList.remove(i);
                            Intent dashStopSessionIntent = new Intent(Constants.NOTIFICATION_RECEIVER_INTENT_FILTER);
                            //dashStartSessionIntent.setAction(Long.toString(System.currentTimeMillis()));
                            dashStopSessionIntent.putExtra(Constants.COMM_KEY_NOTIF, "STOP SESSION");
                            //dashStartSessionIntent.setAction(new Random().nextInt(50) + "_action");
                            PendingIntent dashStopSessionPendingIntent = PendingIntent.getBroadcast(context, 0, dashStopSessionIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                            messageServiceNotifBuilder.addAction(R.drawable.ic_action_rotation, "STOP SESSION", dashStopSessionPendingIntent);
                            mNotificationManager.notify(1, messageServiceNotifBuilder.build());
                        }
                    }
                } else if (intent.getStringExtra(Constants.COMM_KEY_MSGSERVICE).equals(Constants.STOP_SC)) {
                    Log.d(Constants.TAG, "STOP SC");
                    sendMessageToWear(Constants.STOP_SC);
                    ArrayList<NotificationCompat.Action> actionList = messageServiceNotifBuilder.mActions;
                    for (int i = 0; i < actionList.size(); i++) {
                        if (actionList.get(i).getTitle().equals("STOP SESSION")) {
                            actionList.remove(i);
                            Intent dashStartSessionIntent = new Intent(Constants.NOTIFICATION_RECEIVER_INTENT_FILTER);
                            //dashStartSessionIntent.setAction(Long.toString(System.currentTimeMillis()));
                            dashStartSessionIntent.putExtra(Constants.COMM_KEY_NOTIF, "START SESSION");
                            //dashStartSessionIntent.setAction(new Random().nextInt(50) + "_action");
                            PendingIntent dashStartSessionPendingIntent = PendingIntent.getBroadcast(context, 0, dashStartSessionIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                            messageServiceNotifBuilder.addAction(R.drawable.ic_action_rotation, "START SESSION", dashStartSessionPendingIntent);
                            mNotificationManager.notify(1, messageServiceNotifBuilder.build());
                        }
                    }
                }
                else if(intent.getStringExtra(Constants.COMM_KEY_MSGSERVICE).equals("KILL")) {
                    killWear(context);
                }
            }
        }
    };


    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);
        byte[] input = messageEvent.getData();
        String message = new String(input);
        Log.d(Constants.TAG, "MessageService: onMessageReceived: " + message + " " + count);
        count ++;
        if(message.equals(Constants.STATUS_READY)) {
            setWearConnected(true);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int i = super.onStartCommand(intent, flags, startId);
        Log.d(Constants.TAG, "MessageService: onStartCommand");
        Intent dashIntent = new Intent(this, UPMC.class);
        dashIntent.setAction(new Random().nextInt(50) + "_action");
        PendingIntent dashPendingIntent = PendingIntent.getActivity(this, 0,dashIntent,0);

        messageServiceNotifBuilder = new NotificationCompat.Builder(getApplicationContext())
                .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark_normal)
                .setContentTitle("UPMC Dash Wear Client")
                .setContentText("Looking for Connections")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(dashPendingIntent);


        startForeground(1, messageServiceNotifBuilder.build());

        detectWearStatus();


        return i;
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
        if(capabilityInfo.getNodes().size() > 0){
            Log.d(Constants.TAG, "MessageService: Device Connected");
//            setWearConnected(true);
//            Intent connectedIntent = new Intent(Constants.WEAR_STATUS_INTENT_FILTER);
//            connectedIntent.putExtra("status", "connected");
//            LocalBroadcastManager.getInstance(this).sendBroadcast(connectedIntent);
//            messageServiceNotifBuilder.setContentText("Connected");
//            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//            mNotificationManager.notify(1, messageServiceNotifBuilder.build());
        }else{
            Log.d(Constants.TAG, "MessageService: No Devices, onCapabilityChanged");
//            setWearConnected(false);
//            Intent connectedIntent = new Intent(Constants.WEAR_STATUS_INTENT_FILTER);
//            connectedIntent.putExtra("status", "disconnected");
//            LocalBroadcastManager.getInstance(this).sendBroadcast(connectedIntent);
//            messageServiceNotifBuilder.setContentText("Disconnected");
//            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//            mNotificationManager.notify(1, messageServiceNotifBuilder.build());


        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(Constants.TAG, "MessageService: onConnected");
        Wearable.CapabilityApi.addCapabilityListener(mGoogleApiClient,
                this,
                Constants.CAPABILITY_WEAR_APP);
        //Wearable.MessageApi.addListener(mGoogleApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(Constants.TAG, "MessageService:onConnectionSuspended");
    }


    public boolean isWearConnected() {
        return wearConnected;
    }

    private void sendMessageToWear(final String message) {
        Log.d(Constants.TAG, "MessageService:sendMessageToWear " + message);

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
                    Log.d(Constants.TAG, "MessageService:sendMessageToWear:message failed" + message);
                } else {
                    Log.d(Constants.TAG, "MessageService:sendMessageToWear:message sent" + message);
                }
            }
        });

    }

    private void killWear(final Context context) {
        Log.d(Constants.TAG, "MessageService:killing wear and phone ");
        String message = Constants.KILL_DASH;

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
                    Log.d(Constants.TAG, "MessageService:sendMessageToWear: kill failed" );
                    Toast.makeText(context,"Failed to kill Wear App. Please check manually", Toast.LENGTH_LONG).show();
                } else {
                    Log.d(Constants.TAG, "MessageService:sendMessageToWear: kill sent" );
                    Toast.makeText(context,"Kill app on Wear Successful.", Toast.LENGTH_LONG).show();
                }
                sendBroadcast(new Intent(Constants.NOTIFICATION_RECEIVER_INTENT_FILTER).putExtra(Constants.COMM_KEY_NOTIF, "KILL_REQUEST"));
            }
        });

    }

    private void detectWearStatus() {
        setWearConnected(false);
        String message  = Constants.GET_STATUS_WEAR;

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
                    Log.d(Constants.TAG, "MessageService:wearStatus: disconnected");
                    setWearConnected(false);
                } else {
                    Log.d(Constants.TAG, "MessageService:wearStatus: connected");

                }
            }
        });

        final Handler handler = new Handler();
        final Context mContext = this;
        //do something here
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                Intent dashStopIntent = new Intent(Constants.NOTIFICATION_RECEIVER_INTENT_FILTER);
                dashStopIntent.putExtra(Constants.COMM_KEY_NOTIF, "STOP");
                PendingIntent dashStopPendingIntent = PendingIntent.getBroadcast(mContext,0,dashStopIntent,PendingIntent.FLAG_ONE_SHOT);
                messageServiceNotifBuilder.addAction(R.drawable.ic_action_rotation,"STOP CLIENT",dashStopPendingIntent);
                mNotificationManager.notify(1, messageServiceNotifBuilder.build());
                if(isWearConnected()) {
                    Log.d(Constants.TAG, "MessageService:detectWearStatus:wear is connected");
                    setWearConnected(true);
                    messageServiceNotifBuilder.setContentText("Connected");
                    Intent dashStartSessionIntent = new Intent(Constants.NOTIFICATION_RECEIVER_INTENT_FILTER);
                    //dashStartSessionIntent.setAction(Long.toString(System.currentTimeMillis()));
                    dashStartSessionIntent.putExtra(Constants.COMM_KEY_NOTIF, "START SESSION");
                    //dashStartSessionIntent.setAction(new Random().nextInt(50) + "_action");
                    PendingIntent dashStartSessionPendingIntent = PendingIntent.getBroadcast(mContext,0,dashStartSessionIntent,PendingIntent.FLAG_UPDATE_CURRENT);
                    messageServiceNotifBuilder.addAction(R.drawable.ic_action_rotation,"START SESSION",dashStartSessionPendingIntent);
                    mNotificationManager.notify(1, messageServiceNotifBuilder.build());
                }
                else {
                    Log.d(Constants.TAG, "MessageService:detectWearStatus:wear is not connected");
                    setWearConnected(false);
                    messageServiceNotifBuilder.setContentText("Disconnected");
                    mNotificationManager.notify(1, messageServiceNotifBuilder.build());
                }
            }
        }, 3000);

    }

    public void setWearConnected(boolean wearConnected) {
        this.wearConnected = wearConnected;
    }
}
