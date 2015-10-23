package com.aware.plugin.upmc.cancer;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

/**
 * Created by denzil on 17/09/15.
 */
public class Survey extends IntentService {
    public Survey() {
        super("Survey service");
    }

    private final int CANCER_SURVEY = 666;

    @Override
    protected void onHandleIntent(Intent intent) {
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
        mBuilder.setSmallIcon(R.drawable.ic_stat_survey);
        mBuilder.setContentTitle("UPMC");
        mBuilder.setContentText("Questionnaire available. Answer?");
        mBuilder.setDefaults(Notification.DEFAULT_ALL);
        mBuilder.setAutoCancel(true);

        Intent survey = new Intent(getApplicationContext(), UPMC.class);
        PendingIntent onclick = PendingIntent.getActivity(getApplicationContext(), 0, survey, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(onclick);

        NotificationManager notManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notManager.notify(CANCER_SURVEY, mBuilder.build());
    }
}
