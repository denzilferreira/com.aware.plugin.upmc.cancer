package com.aware.plugin.upmc.cancer;

import android.app.AlarmManager;
import android.app.IntentService;
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
import android.widget.Toast;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.utils.Aware_Plugin;

import java.util.Calendar;

/**
 * Created by denzil on 25/11/14.
 */
public class Plugin extends Aware_Plugin {

    public static final String ACTION_JOIN_STUDY = "ACTION_JOIN_STUDY";

    public static AlarmManager alarmManager;
    private static SharedPreferences prefs;

    public static Intent survey;
    public static PendingIntent surveyTrigger;

    public static boolean is_scheduled = false;

    @Override
    public void onCreate() {
        super.onCreate();

        prefs = getSharedPreferences(getPackageName(), MODE_PRIVATE);
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        survey = new Intent(getApplicationContext(), Survey.class);
        surveyTrigger = PendingIntent.getService(getApplicationContext(), 0, survey, PendingIntent.FLAG_UPDATE_CURRENT);

        TAG = "UPMC-Cancer";
        DEBUG = Aware.getSetting(getApplicationContext(), Aware_Preferences.DEBUG_FLAG).equals("true");

        DATABASE_TABLES = Provider.DATABASE_TABLES;
        TABLES_FIELDS = Provider.TABLES_FIELDS;
        CONTEXT_URIS = new Uri[]{ Provider.Cancer_Data.CONTENT_URI };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        TAG = "UPMC-Cancer";
        DEBUG = Aware.getSetting(getApplicationContext(), Aware_Preferences.DEBUG_FLAG).equals("true");

        Calendar now = Calendar.getInstance();
        now.setTimeInMillis(System.currentTimeMillis());
        if( now.get(Calendar.HOUR_OF_DAY) == 0 || ! prefs.contains("stress_counter") ) {
            //start daily counter
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("stress_counter", 0);
            editor.commit();
        }

        if( ! prefs.contains(Settings.PLUGIN_UPMC_CANCER_MAX_PROMPTS) ) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(Settings.PLUGIN_UPMC_CANCER_MAX_PROMPTS, 8);
            editor.commit();
        }
        if( DEBUG ) Log.d(TAG, "Max questions per day: " + prefs.getInt(Settings.PLUGIN_UPMC_CANCER_MAX_PROMPTS, 8));

        if( ! prefs.contains(Settings.PLUGIN_UPMC_CANCER_PROMPT_INTERVAL) ) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(Settings.PLUGIN_UPMC_CANCER_PROMPT_INTERVAL, 30);
            editor.commit();
        }
        if( DEBUG) Log.d(TAG, "Minimum interval between questions: " + prefs.getInt(Settings.PLUGIN_UPMC_CANCER_PROMPT_INTERVAL, 30) + " minutes");

        if( intent != null && intent.getAction() != null && intent.getAction().equals(ACTION_JOIN_STUDY) ) {
            if (Aware.getSetting(getApplicationContext(), "study_id").length() == 0) {
                Intent join_study = new Intent(getApplicationContext(), Aware_Preferences.StudyConfig.class);
                join_study.putExtra("study_url", "https://api.awareframework.com/index.php/webservice/index/205/tgj4NVrQK5Wl");
                startService(join_study);
            }
        }

        if( ! is_scheduled ) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(System.currentTimeMillis());

            String feedback = "";
            if( cal.get(Calendar.HOUR_OF_DAY) > prefs.getInt("morning_hours",0) || cal.get(Calendar.MINUTE) > prefs.getInt("morning_minutes",0) ) {
                //lets set the calendar for the following day, repeating every day after that
                cal.add(Calendar.DAY_OF_YEAR, 1); //set it to tomorrow
                cal.set(Calendar.HOUR_OF_DAY, prefs.getInt("morning_hours",0));
                cal.set(Calendar.MINUTE, prefs.getInt("morning_minutes",0));
                cal.set(Calendar.SECOND, 0);
            } else {
                cal.set(Calendar.HOUR_OF_DAY, prefs.getInt("morning_hours",0));
                cal.set(Calendar.MINUTE, prefs.getInt("morning_minutes",0));
                cal.set(Calendar.SECOND, 0);
            }

            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), AlarmManager.INTERVAL_DAY, surveyTrigger);
            Log.d("UPMC", "Morning: " + cal.getTime().toString());
            feedback+= "Morning: " + cal.getTime().toString() + "\n";

            cal.setTimeInMillis(System.currentTimeMillis());
            if( cal.get(Calendar.HOUR_OF_DAY) > prefs.getInt("evening_hours",0) || cal.get(Calendar.MINUTE) > prefs.getInt("evening_minutes",0) ) {
                //lets set the calendar for the following day, repeating every day after that
                cal.add(Calendar.DAY_OF_YEAR, 1); //set it to tomorrow
                cal.set(Calendar.HOUR_OF_DAY, prefs.getInt("evening_hours",0));
                cal.set(Calendar.MINUTE, prefs.getInt("evening_minutes",0));
                cal.set(Calendar.SECOND, 0);
            } else {
                cal.set(Calendar.HOUR_OF_DAY, prefs.getInt("evening_hours",0));
                cal.set(Calendar.MINUTE, prefs.getInt("evening_minutes", 0));
                cal.set(Calendar.SECOND, 0);
            }

            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), AlarmManager.INTERVAL_DAY, surveyTrigger);
            Log.d("UPMC", "Evening: " + cal.getTime().toString());
            feedback+= "Evening: " + cal.getTime().toString();

            is_scheduled = true;

            Toast.makeText(this, "Next questions:\n" + feedback, Toast.LENGTH_LONG).show();
        }

        return START_STICKY;
    }

    /**
     * Get a random schedule between start and end
     * @param start
     * @param end
     * @param amount
     * @return
     */
    private long[] getRandomTimes(int start, int end, int amount) {
        long[] schedules = new long[amount];
        //TODO
        return schedules;
    }

    public static class Survey extends IntentService {
        public Survey() {
            super("Survey service");
        }

        @Override
        protected void onHandleIntent(Intent intent) {
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
            mBuilder.setSmallIcon(R.drawable.ic_stat_survey);
            mBuilder.setContentTitle("UPMC");
            mBuilder.setContentText("Questionnaire available. Answer?");
            mBuilder.setDefaults(Notification.DEFAULT_ALL);
            mBuilder.setAutoCancel(true);

            Intent survey = new Intent(this, UPMC.class);
            PendingIntent onclick = PendingIntent.getActivity(this, 0, survey, PendingIntent.FLAG_UPDATE_CURRENT);
            mBuilder.setContentIntent(onclick);

            NotificationManager notManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notManager.notify(getPackageName().hashCode(), mBuilder.build());
        }
    }
}
