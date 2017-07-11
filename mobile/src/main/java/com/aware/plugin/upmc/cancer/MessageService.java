package com.aware.plugin.upmc.cancer;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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

/**
 * Created by RaghuTeja on 6/23/17.
 */

public class MessageService extends WearableListenerService implements
        GoogleApiClient.ConnectionCallbacks{

    private GoogleApiClient mGoogleApiClient;
    public boolean wearConnected;
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(Constants.TAG, "onCreated");
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
        Log.d(Constants.TAG, "onMessageReceived");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int i = super.onStartCommand(intent, flags, startId);
        Log.d(Constants.TAG, "onStartCommand");
        LocalBroadcastManager.getInstance(this).registerReceiver(localMessageReceiver, new IntentFilter(Constants.LOCAL_MESSAGE_INTENT_FILTER));
        return i;
    }

    private BroadcastReceiver localMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("message");
            Log.d(Constants.TAG, "Local Message Received" + message);
            sendMessage("start");
        }
    };

    @Override
    public void onConnectedNodes(List<Node> list) {
        Log.d(Constants.TAG, "onConnectedNodes");
        super.onConnectedNodes(list);
    }

    @Override
    public void onPeerConnected(Node node) {
        Log.d(Constants.TAG, "onPeerConnected");
        super.onPeerConnected(node);
    }

    @Override
    public void onPeerDisconnected(Node node) {
        Log.d(Constants.TAG, "onPeerConnected");
        super.onPeerDisconnected(node);
    }


    @Override
    public void onCapabilityChanged(CapabilityInfo capabilityInfo) {
        Log.d(Constants.TAG, "onCapability");
        super.onCapabilityChanged(capabilityInfo);
        if(capabilityInfo.getNodes().size() > 0){
            Log.d(Constants.TAG, "Device Connected");
            setWearConnected(true);
            Intent connectedIntent = new Intent(Constants.WEAR_STATUS_INTENT_FILTER);
            connectedIntent.putExtra("status", "connected");
            LocalBroadcastManager.getInstance(this).sendBroadcast(connectedIntent);
        }else{
            Log.d(Constants.TAG, "No Devices");
            setWearConnected(false);
            Intent connectedIntent = new Intent(Constants.WEAR_STATUS_INTENT_FILTER);
            connectedIntent.putExtra("status", "disconnected");
            LocalBroadcastManager.getInstance(this).sendBroadcast(connectedIntent);
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(Constants.TAG, "onConnected");
        Wearable.CapabilityApi.addCapabilityListener(mGoogleApiClient,
                this,
                Constants.CAPABILITY_WEAR_APP);
        Wearable.MessageApi.addListener(mGoogleApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(Constants.TAG, "onConnectionSuspended");
    }


    public boolean isWearConnected() {
        return wearConnected;
    }

    private void sendMessage(String message) {

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
                    Log.d(Constants.TAG, "message failed");
                } else {
                    Log.d(Constants.TAG, "message sent");
                }
            }
        });

    }

    public void setWearConnected(boolean wearConnected) {
        this.wearConnected = wearConnected;
    }
}
