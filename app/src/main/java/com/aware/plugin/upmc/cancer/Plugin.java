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
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.ESM;
import com.aware.providers.ESM_Provider;
import com.aware.utils.Aware_Plugin;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Random;

/**
 * Created by denzil on 25/11/14.
 */
public class Plugin extends Aware_Plugin {

    public static AlarmManager alarmManager;
    private static SharedPreferences prefs;

    public static Intent survey, stressEMA;
    public static PendingIntent surveyTrigger, stressEMATrigger;

    public static String ACTION_PLUGIN_UPMC_CANCER_SCHEDULE = "ACTION_PLUGIN_UPMC_CANCER_SCHEDULE";

    @Override
    public void onCreate() {
        super.onCreate();

        prefs = getSharedPreferences(getPackageName(), MODE_PRIVATE);
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        survey = new Intent(getApplicationContext(), Survey.class);
        stressEMA = new Intent(getApplicationContext(), StressEMA.class);
        surveyTrigger = PendingIntent.getService(getApplicationContext(), 0, survey, PendingIntent.FLAG_UPDATE_CURRENT);
        stressEMATrigger = PendingIntent.getService(getApplicationContext(), 0, stressEMA, PendingIntent.FLAG_UPDATE_CURRENT);

        TAG = "UPMC-Cancer";
        DEBUG = Aware.getSetting(getApplicationContext(), Aware_Preferences.DEBUG_FLAG).equals("true");

        DATABASE_TABLES = Provider.DATABASE_TABLES;
        TABLES_FIELDS = Provider.TABLES_FIELDS;
        CONTEXT_URIS = new Uri[]{ Provider.Cancer_Data.CONTENT_URI };

        Aware.setSetting(this, Aware_Preferences.STATUS_ESM, true);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        TAG = "UPMC-Cancer";
        DEBUG = Aware.getSetting(getApplicationContext(), Aware_Preferences.DEBUG_FLAG).equals("true");

        if( ! prefs.contains(Settings.PLUGIN_UPMC_CANCER_MAX_PROMPTS) ) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(Settings.PLUGIN_UPMC_CANCER_MAX_PROMPTS, 8);
            editor.commit();
            Aware.setSetting(this, Settings.PLUGIN_UPMC_CANCER_MAX_PROMPTS, 8);
        }
        Log.d(TAG, "Max questions per day: " + Aware.getSetting(this, Settings.PLUGIN_UPMC_CANCER_MAX_PROMPTS));

        if( ! prefs.contains(Settings.PLUGIN_UPMC_CANCER_PROMPT_INTERVAL) ) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(Settings.PLUGIN_UPMC_CANCER_PROMPT_INTERVAL, 30);
            editor.commit();
            Aware.setSetting(this, Settings.PLUGIN_UPMC_CANCER_PROMPT_INTERVAL, 30);
        }
        Log.d(TAG, "Minimum interval between questions: " + Aware.getSetting(this, Settings.PLUGIN_UPMC_CANCER_PROMPT_INTERVAL) + " minutes");

        if( intent != null && intent.getAction() != null && intent.getAction().equalsIgnoreCase(ACTION_PLUGIN_UPMC_CANCER_SCHEDULE) ) {
            long start_questions;
            long end_questions;

            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(System.currentTimeMillis());

            String feedback = "";
            if( cal.get(Calendar.HOUR_OF_DAY) > prefs.getInt("morning_hours", 0) || cal.get(Calendar.MINUTE) > prefs.getInt("morning_minutes", 0) ) {
                //lets set the calendar for the following day, repeating every day after that
                cal.add(Calendar.DAY_OF_YEAR, 1); //set it to tomorrow
                cal.set(Calendar.HOUR_OF_DAY, prefs.getInt("morning_hours", 0));
                cal.set(Calendar.MINUTE, prefs.getInt("morning_minutes", 0));
                cal.set(Calendar.SECOND, 0);
            } else {
                cal.set(Calendar.HOUR_OF_DAY, prefs.getInt("morning_hours", 0));
                cal.set(Calendar.MINUTE, prefs.getInt("morning_minutes", 0));
                cal.set(Calendar.SECOND, 0);
            }
            start_questions = cal.getTimeInMillis();

            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), AlarmManager.INTERVAL_DAY, surveyTrigger);
            Log.d(TAG, "Morning: " + cal.getTime().toString());
            feedback+= "Morning: " + cal.getTime().toString() + "\n";

            cal.setTimeInMillis(System.currentTimeMillis());
            if( cal.get(Calendar.HOUR_OF_DAY) > prefs.getInt("evening_hours", 0) || cal.get(Calendar.MINUTE) > prefs.getInt("evening_minutes", 0) ) {
                //lets set the calendar for the following day, repeating every day after that
                cal.add(Calendar.DAY_OF_YEAR, 1); //set it to tomorrow
                cal.set(Calendar.HOUR_OF_DAY, prefs.getInt("evening_hours", 0));
                cal.set(Calendar.MINUTE, prefs.getInt("evening_minutes", 0));
                cal.set(Calendar.SECOND, 0);
            } else {
                cal.set(Calendar.HOUR_OF_DAY, prefs.getInt("evening_hours", 0));
                cal.set(Calendar.MINUTE, prefs.getInt("evening_minutes", 0));
                cal.set(Calendar.SECOND, 0);
            }
            end_questions = cal.getTimeInMillis();

            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), AlarmManager.INTERVAL_DAY, surveyTrigger);
            Log.d(TAG, "Evening: " + cal.getTime().toString());
            feedback+= "Evening: " + cal.getTime().toString();

            int random_hour;
            if( end_questions > start_questions ) {
                random_hour = getRandomTime(start_questions, start_questions + (2 * Integer.valueOf(Aware.getSetting(this, Settings.PLUGIN_UPMC_CANCER_PROMPT_INTERVAL)) * 60 * 1000)); // between sooner and 2x interval
            } else {
                random_hour = getRandomTime(end_questions, end_questions + (2 * Integer.valueOf(Aware.getSetting(this, Settings.PLUGIN_UPMC_CANCER_PROMPT_INTERVAL)) * 60 * 1000)); // between sooner and 2x interval
            }

            cal = Calendar.getInstance();
            cal.setTimeInMillis(System.currentTimeMillis());
            cal.set(Calendar.HOUR_OF_DAY, random_hour);
            cal.set(Calendar.MINUTE, randomizer(0, 59));
            cal.set(Calendar.SECOND, randomizer(0, 59));

            Log.d(TAG, "StressEMA: " + cal.getTime().toString());

            alarmManager.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), stressEMATrigger);

            Toast.makeText(this, "Next questions:\n" + feedback, Toast.LENGTH_LONG).show();
        }

        return START_STICKY;
    }

    /**
     * Get a random schedule between start and end
     * @param start_timestamp
     * @param end_timestamp
     * @return long
     */
    private static int getRandomTime(long start_timestamp, long end_timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(start_timestamp);
        int start_hour = cal.get(Calendar.HOUR_OF_DAY);
        cal.setTimeInMillis(end_timestamp);
        int end_hour = cal.get(Calendar.HOUR_OF_DAY);

        return randomizer(start_hour, end_hour);
    }

    private static int randomizer(int start, int end) {
        Random random = new Random();
        return (random.nextInt(end) + start);
    }

    public static class StressEMA extends IntentService {
        public StressEMA() {
            super("Stress EMA service");
        }

        @Override
        protected void onHandleIntent(Intent intent) {

            String stress_esm = "[{'esm':{'esm_type':'"+ESM.TYPE_ESM_RADIO+"', 'esm_title':'Stress', 'esm_instructions':'Are you stressed?','esm_radios':['NO','No','Yes','YES'],'esm_expiration_threashold':0,'esm_submit':'OK','esm_trigger':'"+getPackageName()+"'}}]";
            Intent esm = new Intent(ESM.ACTION_AWARE_QUEUE_ESM);
            esm.putExtra(ESM.EXTRA_ESM, stress_esm);
            sendBroadcast(esm);

            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(System.currentTimeMillis());
            c.set(Calendar.HOUR_OF_DAY, 0);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);

            Cursor esms_today = getContentResolver().query(ESM_Provider.ESM_Data.CONTENT_URI, null, ESM_Provider.ESM_Data.TIMESTAMP + " >= " + c.getTimeInMillis() + " AND " + ESM_Provider.ESM_Data.TRIGGER + " LIKE '" + getPackageName() + "'", null, null);
            if( esms_today != null && esms_today.moveToFirst() ) {
                if( esms_today.getCount() < Integer.valueOf(Aware.getSetting(this, Settings.PLUGIN_UPMC_CANCER_MAX_PROMPTS))) {
                    Log.d(TAG, "Stress EMAs so far: " + esms_today.getCount() );

                    int random_hour = getRandomTime(System.currentTimeMillis(), System.currentTimeMillis() + (2 * Integer.valueOf(Aware.getSetting(this, Settings.PLUGIN_UPMC_CANCER_PROMPT_INTERVAL))*60*1000));

                    Calendar cal = Calendar.getInstance();
                    cal.setTimeInMillis(System.currentTimeMillis());
                    cal.set(Calendar.HOUR_OF_DAY, random_hour);
                    cal.set(Calendar.MINUTE, randomizer(0, 59));
                    cal.set(Calendar.SECOND, randomizer(0, 59));

                    Log.d(TAG, "Next StressEMA: " + cal.getTime().toString());
                    alarmManager.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), stressEMATrigger);
                } else {
                    Log.d(TAG, "Done for today! Stress EMAs today: " + esms_today.getCount() );
                }
            } else {
                int random_hour = getRandomTime(System.currentTimeMillis(), System.currentTimeMillis() + (2 * Integer.valueOf(Aware.getSetting(this, Settings.PLUGIN_UPMC_CANCER_PROMPT_INTERVAL))*60*1000));
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(System.currentTimeMillis());
                cal.set(Calendar.HOUR_OF_DAY, random_hour);
                cal.set(Calendar.MINUTE, randomizer(0, 59));
                cal.set(Calendar.SECOND, randomizer(0, 59));

                Log.d(TAG, "Next StressEMA: " + cal.getTime().toString());
                alarmManager.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), stressEMATrigger);
            }
            if( esms_today != null && ! esms_today.isClosed()) esms_today.close();
        }
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
