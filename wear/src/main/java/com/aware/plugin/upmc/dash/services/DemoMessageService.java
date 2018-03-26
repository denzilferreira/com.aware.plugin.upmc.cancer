package com.aware.plugin.upmc.dash.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.aware.plugin.upmc.dash.R;
import com.aware.plugin.upmc.dash.utils.Constants;
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

/**
 * Created by RaghuTeja on 3/23/18.
 */

public class DemoMessageService extends WearableListenerService implements MessageClient.OnMessageReceivedListener,
        CapabilityClient.OnCapabilityChangedListener,
        DataClient.OnDataChangedListener {


    private MessageClient messageClient;
    private CapabilityClient capabilityClient;
    private DataClient dataClient;
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(Constants.TAG, "onStartCommand");
        showDemoNotif();
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
        capabilityClient.addListener(this, Constants.CAPABILITY_PHONE_APP);
        dataClient.addListener(this);
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        Log.d(Constants.TAG, "onDestroy");
        super.onDestroy();
        messageClient.removeListener(this);
        capabilityClient.removeListener(this);
        dataClient.removeListener(this);
    }



    @Override
    public void onMessageReceived(@NonNull MessageEvent messageEvent) {
        byte[] input = messageEvent.getData();
        String message = new String(input);
        Log.d(Constants.TAG, "MessageService: onMessageReceived: " + message );


    }

    @Override
    public void onDataChanged(@NonNull DataEventBuffer dataEventBuffer) {

    }

    @Override
    public void onCapabilityChanged(@NonNull CapabilityInfo capabilityInfo) {

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
