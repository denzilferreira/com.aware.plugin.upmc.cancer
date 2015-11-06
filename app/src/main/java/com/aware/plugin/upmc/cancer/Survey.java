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

    @Override
    protected void onHandleIntent(Intent intent) {
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getApplicationContext());
        mBuilder.setSmallIcon( R.drawable.ic_stat_survey );
        mBuilder.setContentTitle( "UPMC Questionnaire" );
        mBuilder.setContentText( "Tap to answer." );
        mBuilder.setDefaults( Notification.DEFAULT_ALL );
        mBuilder.setOnlyAlertOnce( true );
        mBuilder.setAutoCancel( true );

        Intent survey = new Intent(getApplicationContext(), UPMC.class);
        survey.setFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
        PendingIntent onclick = PendingIntent.getActivity(getApplicationContext(), 0, survey, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(onclick);

        NotificationManager notManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notManager.notify(600, mBuilder.build());
    }
}
