package com.aware.plugin.upmc.cancer;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.utils.Aware_Plugin;

import java.util.Calendar;

/**
 * Created by denzil on 25/11/14.
 */
public class Plugin extends Aware_Plugin {

    public static final String ACTION_JOIN_STUDY = "ACTION_JOIN_STUDY";
    public static final String ACTION_UPMC_SURVEY = "ACTION_UPMC_SURVEY";

    @Override
    public void onCreate() {
        super.onCreate();

        Intent aware = new Intent(this, Aware.class);
        startService(aware);

        TAG = "UPMC-Cancer";
        DEBUG = Aware.getSetting(this, Aware_Preferences.DEBUG_FLAG).equals("true");

        DATABASE_TABLES = Provider.DATABASE_TABLES;
        TABLES_FIELDS = Provider.TABLES_FIELDS;
        CONTEXT_URIS = new Uri[]{ Provider.Cancer_Data.CONTENT_URI };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        DEBUG = Aware.getSetting(this, Aware_Preferences.DEBUG_FLAG).equals("true");

        SharedPreferences prefs = getSharedPreferences(getPackageName(), MODE_PRIVATE);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        Intent survey = new Intent(ACTION_UPMC_SURVEY);
        PendingIntent surveyTrigger = PendingIntent.getBroadcast(getApplicationContext(), 0, survey, PendingIntent.FLAG_UPDATE_CURRENT);

        if( intent != null && intent.getAction() != null && intent.getAction().equals(ACTION_JOIN_STUDY) ) {
            if( Aware.getSetting(getApplicationContext(), "study_id").length() == 0 ) {
                Intent join_study = new Intent(getApplicationContext(), Aware_Preferences.StudyConfig.class);
                join_study.putExtra("study_url", "https://api.awareframework.com/index.php/webservice/index/205/tgj4NVrQK5Wl");
                startService(join_study);
                return START_STICKY;
            }
        }

        if( prefs.contains("scheduled") && prefs.getBoolean("scheduled", false ) ) {

            alarmManager.cancel(surveyTrigger); //clean-up

            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(System.currentTimeMillis());

            if( cal.get(Calendar.HOUR_OF_DAY) > prefs.getInt("morning_hours",0) ) {
                //lets set the calendar for the following day, repeating every day after that
                cal.add(Calendar.DATE, 1); //set it to tomorrow
                cal.set(Calendar.HOUR_OF_DAY, prefs.getInt("morning_hours",0));
                cal.set(Calendar.MINUTE, prefs.getInt("morning_minutes",0));
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
            } else {
                cal.set(Calendar.HOUR_OF_DAY, prefs.getInt("morning_hours",0));
                cal.set(Calendar.MINUTE, prefs.getInt("morning_minutes",0));
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
            }
            alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), AlarmManager.INTERVAL_DAY, surveyTrigger);
            Log.d("UPMC", "Morning: " + cal.getTime().toString());

            cal.setTimeInMillis(System.currentTimeMillis());
            if( cal.get(Calendar.HOUR_OF_DAY) > prefs.getInt("evening_hours",0) ) {
                //lets set the calendar for the following day, repeating every day after that
                cal.add(Calendar.DATE, 1); //set it to tomorrow
                cal.set(Calendar.HOUR_OF_DAY, prefs.getInt("evening_hours",0));
                cal.set(Calendar.MINUTE, prefs.getInt("evening_minutes",0));
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
            } else {
                cal.set(Calendar.HOUR_OF_DAY, prefs.getInt("evening_hours",0));
                cal.set(Calendar.MINUTE, prefs.getInt("evening_minutes",0));
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
            }
            alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), AlarmManager.INTERVAL_DAY, surveyTrigger);
            Log.d("UPMC", "Evening: " + cal.getTime().toString());
        }

        return START_STICKY;
    }

    public static class Survey extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context);
            mBuilder.setSmallIcon(R.drawable.ic_stat_survey);
            mBuilder.setContentTitle("UPMC");
            mBuilder.setContentText("Questionnaire available. Answer?");
            mBuilder.setDefaults(Notification.DEFAULT_ALL);
            mBuilder.setAutoCancel(true);

            Intent survey = new Intent(context, UPMC.class);
            PendingIntent onclick = PendingIntent.getActivity(context, 0, survey, PendingIntent.FLAG_UPDATE_CURRENT);
            mBuilder.setContentIntent(onclick);

            NotificationManager notManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notManager.notify(42, mBuilder.build());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
