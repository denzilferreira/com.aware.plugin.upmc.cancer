package com.aware.plugin.upmc.cancer;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.util.Log;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.ESM;
import com.aware.providers.Scheduler_Provider;
import com.aware.utils.Aware_Plugin;
import com.aware.utils.Scheduler;

import org.json.JSONException;

import java.util.Random;

/**
 * Created by denzil on 25/11/14.
 */
public class Plugin extends Aware_Plugin {

    public static String ACTION_CANCER_SURVEY = "ACTION_CANCER_SURVEY";

    @Override
    public void onCreate() {
        super.onCreate();

        DATABASE_TABLES = Provider.DATABASE_TABLES;
        TABLES_FIELDS = Provider.TABLES_FIELDS;
        CONTEXT_URIS = new Uri[]{ Provider.Cancer_Data.CONTENT_URI };

        REQUIRED_PERMISSIONS.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        REQUIRED_PERMISSIONS.add(Manifest.permission.READ_SMS);
        REQUIRED_PERMISSIONS.add(Manifest.permission.READ_CALL_LOG);
        REQUIRED_PERMISSIONS.add(Manifest.permission.ACCESS_FINE_LOCATION);
        REQUIRED_PERMISSIONS.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        REQUIRED_PERMISSIONS.add(Manifest.permission.SYSTEM_ALERT_WINDOW);

        Aware.startPlugin(this, "com.aware.plugin.upmc.cancer");

        IntentFilter filter = new IntentFilter(ACTION_CANCER_SURVEY);
        registerReceiver(surveyListener, filter);
    }

    public static class SurveyListener extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Intent surveyService = new Intent(context, Survey.class);
            context.startService(surveyService);
        }
    }
    private static SurveyListener surveyListener = new SurveyListener();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        TAG = "UPMC-Cancer";
        DEBUG = Aware.getSetting(getApplicationContext(), Aware_Preferences.DEBUG_FLAG).equals("true");

        Aware.setSetting(this, Settings.STATUS_PLUGIN_UPMC_CANCER, true);

        if( Aware.getSetting(this, Settings.PLUGIN_UPMC_CANCER_MAX_PROMPTS).length() == 0 ) {
            Aware.setSetting(this, Settings.PLUGIN_UPMC_CANCER_MAX_PROMPTS, 8);
        }
        Log.d(TAG, "Max questions per day: " + Aware.getSetting(this, Settings.PLUGIN_UPMC_CANCER_MAX_PROMPTS));

        if( Aware.getSetting(this, Settings.PLUGIN_UPMC_CANCER_PROMPT_INTERVAL).length() == 0 ) {
            Aware.setSetting(this, Settings.PLUGIN_UPMC_CANCER_PROMPT_INTERVAL, 30);
        }
        Log.d(TAG, "Minimum interval between questions: " + Aware.getSetting(this, Settings.PLUGIN_UPMC_CANCER_PROMPT_INTERVAL) + " minutes");

        if( intent != null && intent.getExtras() != null && intent.getBooleanExtra("schedule", false) ) {
            if (Aware.getSetting(this, Settings.PLUGIN_UPMC_CANCER_MORNING_HOUR).length() > 0 && Aware.getSetting(this, Settings.PLUGIN_UPMC_CANCER_EVENING_HOUR).length() > 0) {
                final int morning_hour = Integer.parseInt(Aware.getSetting(this, Settings.PLUGIN_UPMC_CANCER_MORNING_HOUR));
                final int evening_hour = Integer.parseInt(Aware.getSetting(this, Settings.PLUGIN_UPMC_CANCER_EVENING_HOUR));

                try {
                    Scheduler.Schedule schedule = new Scheduler.Schedule("cancer_survey_morning");
                    schedule.addHour(morning_hour)
                            .setActionType(Scheduler.ACTION_TYPE_BROADCAST)
                            .setActionClass(Plugin.ACTION_CANCER_SURVEY);
                    Scheduler.saveSchedule(this, schedule);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                try {
                    Scheduler.Schedule schedule = new Scheduler.Schedule("cancer_survey_evening");
                    schedule.addHour(evening_hour)
                            .setActionType(Scheduler.ACTION_TYPE_BROADCAST)
                            .setActionClass(Plugin.ACTION_CANCER_SURVEY);
                    Scheduler.saveSchedule(this, schedule);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                //Create a random schedule for Angry
                try {
                    String angry_esm = "[{'esm':{'esm_type':'" + ESM.TYPE_ESM_RADIO + "', 'esm_title':'Angry/frustrated', 'esm_instructions':'Are you angry/frustrated?','esm_radios':['NO','No','Yes','YES'],'esm_expiration_threshold':0,'esm_submit':'OK','esm_trigger':'" + getPackageName() + "'}}]";

                    Scheduler.Schedule schedule = new Scheduler.Schedule("angry");
                    for (int i = morning_hour; i < evening_hour; i++) {
                        schedule.addHour(i);
                    }
                    schedule.randomize(Scheduler.RANDOM_TYPE_HOUR)
                            .setActionType(Scheduler.ACTION_TYPE_BROADCAST)
                            .setActionClass(ESM.ACTION_AWARE_QUEUE_ESM)
                            .addActionExtra(ESM.EXTRA_ESM, angry_esm);
                    Scheduler.saveSchedule(this, schedule);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                //Create a random schedule for Happy
                try {
                    String happy_esm = "[{'esm':{'esm_type':'" + ESM.TYPE_ESM_RADIO + "', 'esm_title':'Happy', 'esm_instructions':'Are you happy?','esm_radios':['NO','No','Yes','YES'],'esm_expiration_threshold':0,'esm_submit':'OK','esm_trigger':'" + getPackageName() + "'}}]";

                    Scheduler.Schedule schedule = new Scheduler.Schedule("happy");
                    for (int i = morning_hour; i < evening_hour; i++) {
                        schedule.addHour(i);
                    }
                    schedule.randomize(Scheduler.RANDOM_TYPE_HOUR)
                            .setActionType(Scheduler.ACTION_TYPE_BROADCAST)
                            .setActionClass(ESM.ACTION_AWARE_QUEUE_ESM)
                            .addActionExtra(ESM.EXTRA_ESM, happy_esm);
                    Scheduler.saveSchedule(this, schedule);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                //Create a random schedule for Stressed
                try {
                    String stressed_esm = "[{'esm':{'esm_type':'" + ESM.TYPE_ESM_RADIO + "', 'esm_title':'Stressed/nervous', 'esm_instructions':'Are you stressed/nervous?','esm_radios':['NO','No','Yes','YES'],'esm_expiration_threshold':0,'esm_submit':'OK','esm_trigger':'" + getPackageName() + "'}}]";

                    Scheduler.Schedule schedule = new Scheduler.Schedule("stressed");
                    for (int i = morning_hour; i < evening_hour; i++) {
                        schedule.addHour(i);
                    }
                    schedule.randomize(Scheduler.RANDOM_TYPE_HOUR)
                            .setActionType(Scheduler.ACTION_TYPE_BROADCAST)
                            .setActionClass(ESM.ACTION_AWARE_QUEUE_ESM)
                            .addActionExtra(ESM.EXTRA_ESM, stressed_esm);
                    Scheduler.saveSchedule(this, schedule);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            Cursor scheduled_tasks = getContentResolver().query( Scheduler_Provider.Scheduler_Data.CONTENT_URI, null, null, null, null );
            if( scheduled_tasks != null && scheduled_tasks.getCount() > 0 ) {
                Log.d(TAG, DatabaseUtils.dumpCursorToString(scheduled_tasks));
            }
            if( scheduled_tasks != null && ! scheduled_tasks.isClosed() ) scheduled_tasks.close();
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        unregisterReceiver(surveyListener);
        Aware.setSetting(this, Settings.STATUS_PLUGIN_UPMC_CANCER, false);
        Aware.stopPlugin(this, "com.aware.plugin.upmc.cancer");
    }
}
