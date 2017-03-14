package com.aware.plugin.upmc.cancer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.ESM;
import com.aware.ui.esms.ESMFactory;
import com.aware.ui.esms.ESM_Radio;
import com.aware.utils.Aware_Plugin;
import com.aware.utils.PluginsManager;
import com.aware.utils.Scheduler;

import org.json.JSONException;

import java.util.Calendar;

public class Plugin extends Aware_Plugin {

    public static String ACTION_CANCER_SURVEY = "ACTION_CANCER_SURVEY";

    @Override
    public void onCreate() {
        super.onCreate();

        TAG = "UPMC Cancer";

        DATABASE_TABLES = Provider.DATABASE_TABLES;
        TABLES_FIELDS = Provider.TABLES_FIELDS;
        CONTEXT_URIS = new Uri[]{Provider.Symptom_Data.CONTENT_URI, Provider.Motivational_Data.CONTENT_URI};
    }

    public static class SurveyListener extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_CANCER_SURVEY)) {
                Intent surveyService = new Intent(context, Survey.class);
                context.startService(surveyService);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        if (PERMISSIONS_OK) {

            PluginsManager.enablePlugin(this, "com.aware.plugin.upmc.cancer");

            Aware.setSetting(this, Settings.STATUS_PLUGIN_UPMC_CANCER, true);

            if (intent != null && intent.getExtras() != null && intent.getBooleanExtra("schedule", false)) {
                int morning_hour = Integer.parseInt(Aware.getSetting(this, Settings.PLUGIN_UPMC_CANCER_MORNING_HOUR));
                int morning_minute = Integer.parseInt(Aware.getSetting(this, Settings.PLUGIN_UPMC_CANCER_MORNING_MINUTE));

                try {
                    Scheduler.Schedule schedule = new Scheduler.Schedule("cancer_survey_morning");
                    schedule.addHour(morning_hour)
                            .addMinute(morning_minute)
                            .setActionIntentAction(Plugin.ACTION_CANCER_SURVEY)
                            .setActionType(Scheduler.ACTION_TYPE_BROADCAST);
                    Scheduler.saveSchedule(this, schedule);

                    //Apply schedule
                    Aware.startScheduler(this);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            Aware.startPlugin(this, "com.aware.plugin.upmc.cancer");
            Aware.startAWARE(this);
        }

        return START_STICKY;
    }

    public static Float symptomAvg(Context c) {

        Float todays_symptoms;

        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        Cursor symptoms = c.getContentResolver().query(Provider.Symptom_Data.CONTENT_URI, new String[]{
                "AVG(" +
                        Provider.Symptom_Data.SCORE_PAIN + "+" +
                        Provider.Symptom_Data.SCORE_FATIGUE + "+" +
                        Provider.Symptom_Data.SCORE_SLEEP_DISTURBANCE + "+" +
                        Provider.Symptom_Data.SCORE_CONCENTRATING + "+" +
                        Provider.Symptom_Data.SCORE_SAD + "+" +
                        Provider.Symptom_Data.SCORE_ANXIOUS + "+" +
                        Provider.Symptom_Data.SCORE_SHORT_BREATH + "+" +
                        Provider.Symptom_Data.SCORE_NUMBNESS + "+" +
                        Provider.Symptom_Data.SCORE_NAUSEA + "+" +
                        Provider.Symptom_Data.SCORE_DIARRHEA + "+" +
                        Provider.Symptom_Data.SCORE_OTHER + ") as avg_symptoms"
        }, Provider.Symptom_Data.TIMESTAMP + " >= " + today.getTimeInMillis() + " AND " +
                        Provider.Symptom_Data.SCORE_PAIN + ">=0 AND " +
                        Provider.Symptom_Data.SCORE_FATIGUE + ">=0 AND " +
                        Provider.Symptom_Data.SCORE_SLEEP_DISTURBANCE + ">=0 AND " +
                        Provider.Symptom_Data.SCORE_CONCENTRATING + ">=0 AND " +
                        Provider.Symptom_Data.SCORE_SAD + ">=0 AND " +
                        Provider.Symptom_Data.SCORE_ANXIOUS + ">=0 AND " +
                        Provider.Symptom_Data.SCORE_SHORT_BREATH + ">=0 AND " +
                        Provider.Symptom_Data.SCORE_NUMBNESS + ">=0 AND " +
                        Provider.Symptom_Data.SCORE_NAUSEA + ">=0 AND " +
                        Provider.Symptom_Data.SCORE_DIARRHEA + ">=0 AND " +
                        Provider.Symptom_Data.SCORE_OTHER + ">=0"
                , null, null);
        if (symptoms != null && symptoms.moveToFirst()) {
            todays_symptoms = symptoms.getFloat(0);
        } else {
            return null;
        }
        if (!symptoms.isClosed()) symptoms.close();

        return todays_symptoms;
    }

    public static Integer walkingPromptsCount(Context c) {
        Integer todays_count;

        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        Cursor todayPrompts = c.getContentResolver().query(Provider.Motivational_Data.CONTENT_URI, new String[]{"count(*) as total_count"}, Provider.Motivational_Data.TIMESTAMP + " >= " + today.getTimeInMillis(), null, null);
        if (todayPrompts != null && todayPrompts.moveToFirst()) {
            todays_count = todayPrompts.getInt(0);
        } else {
            return null;
        }
        if (!todayPrompts.isClosed()) todayPrompts.close();
        return todays_count;
    }

    public static class FitbitAnalyser extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equalsIgnoreCase(com.aware.plugin.fitbit.Plugin.ACTION_AWARE_PLUGIN_FITBIT)) {
                Integer walks = walkingPromptsCount(context);
                if (walks != null && walks <= 4) {
                    int last_3h = intent.getIntExtra(com.aware.plugin.fitbit.Plugin.EXTRA_LAST_3H, 1000);
                    int last_5h = intent.getIntExtra(com.aware.plugin.fitbit.Plugin.EXTRA_LAST_5H, 1000);
                    Float symptoms_today = symptomAvg(context);

                    if (symptoms_today != null && last_3h < 50 && symptoms_today < 7) {
                        Intent walking = new Intent(context, UPMC_Motivation.class);
                        walking.putExtra("question_type", 1); //< 50 steps in past 3h, all symptoms < 7
                        walking.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        notifyUser(context, walking);
                    } else if (symptoms_today != null && last_5h < 50 && symptoms_today >= 7) {
                        Intent walking = new Intent(context, UPMC_Motivation.class);
                        walking.putExtra("question_type", 2); //< 50 steps in past 5h, any symptoms >= 7
                        walking.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        notifyUser(context, walking);
                    }
                }
            }
        }

        private void notifyUser(Context context, Intent walk_condition) {
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context);
            mBuilder.setSmallIcon( R.drawable.ic_stat_fitbit );
            mBuilder.setContentTitle( "UPMC" );
            mBuilder.setContentText( "Tap to answer." );
            mBuilder.setDefaults( Notification.DEFAULT_ALL );
            mBuilder.setOnlyAlertOnce( true );
            mBuilder.setAutoCancel( true );

            PendingIntent onclick = PendingIntent.getActivity(context, 0, walk_condition, PendingIntent.FLAG_UPDATE_CURRENT);
            mBuilder.setContentIntent(onclick);

            NotificationManager notManager = (NotificationManager) context.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
            notManager.notify(677, mBuilder.build());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Aware.setSetting(this, Settings.STATUS_PLUGIN_UPMC_CANCER, false);
        Aware.stopAWARE(this);
    }
}
