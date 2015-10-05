package com.aware.plugin.upmc.cancer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.ESM;
import com.aware.utils.Aware_Plugin;
import com.aware.utils.Scheduler;

import org.json.JSONException;

import java.util.Random;

/**
 * Created by denzil on 25/11/14.
 */
public class Plugin extends Aware_Plugin {

    private static SharedPreferences prefs;
    public static String ACTION_PLUGIN_UPMC_CANCER_SCHEDULE = "ACTION_PLUGIN_UPMC_CANCER_SCHEDULE";

    @Override
    public void onCreate() {
        super.onCreate();

        prefs = getSharedPreferences("com.aware.plugin.upmc.cancer", MODE_PRIVATE);

        DATABASE_TABLES = Provider.DATABASE_TABLES;
        TABLES_FIELDS = Provider.TABLES_FIELDS;
        CONTEXT_URIS = new Uri[]{ Provider.Cancer_Data.CONTENT_URI };

        Aware.startPlugin(this, "com.aware.plugin.upmc.cancer");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        TAG = "UPMC-Cancer";
        DEBUG = Aware.getSetting(getApplicationContext(), Aware_Preferences.DEBUG_FLAG).equals("true");

        Aware.setSetting(this, Settings.STATUS_PLUGIN_UPMC_CANCER, true);

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

            int morning_hour = prefs.getInt("morning_hours", 0);
            int evening_hour = prefs.getInt("evening_hours", 0);

            if( morning_hour == 0 && evening_hour == 0 ) {
                Log.d(TAG,"Schedule not set yet... will try again later...");
                return START_STICKY;
            }

            try {
                Scheduler.Schedule schedule = new Scheduler.Schedule("cancer_survey");
                schedule.addHour(morning_hour)
                        .addHour(evening_hour)
                        .setActionType(Scheduler.ACTION_TYPE_SERVICE)
                        .setActionClass("com.aware.plugin.upmc.cancer/com.aware.plugin.upmc.cancer.Survey");

                Scheduler.saveSchedule(this, schedule);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            //Create a random schedule for Angry
            try {

                String angry_esm = "[{'esm':{'esm_type':'"+ESM.TYPE_ESM_RADIO+"', 'esm_title':'Angry/frustrated', 'esm_instructions':'Are you angry/frustrated?','esm_radios':['NO','No','Yes','YES'],'esm_expiration_threshold':0,'esm_submit':'OK','esm_trigger':'"+getPackageName()+"'}}]";

                Scheduler.Schedule schedule = new Scheduler.Schedule("angry");
                for(int i=morning_hour;i<evening_hour;i++) {
                    schedule.addHour(i);
                }
                schedule.randomize(Scheduler.RANDOM_TYPE_HOUR)
                        .setActionType(Scheduler.ACTION_TYPE_BROADCAST)
                        .setActionClass(ESM.ACTION_AWARE_QUEUE_ESM)
                        .addActionExtra(ESM.EXTRA_ESM, angry_esm);

                Scheduler.saveSchedule(this, schedule);

            } catch (JSONException e ) {
                e.printStackTrace();
            }

            //Create a random schedule for Happy
            try {

                String happy_esm = "[{'esm':{'esm_type':'"+ESM.TYPE_ESM_RADIO+"', 'esm_title':'Happy', 'esm_instructions':'Are you happy?','esm_radios':['NO','No','Yes','YES'],'esm_expiration_threshold':0,'esm_submit':'OK','esm_trigger':'"+getPackageName()+"'}}]";

                Scheduler.Schedule schedule = new Scheduler.Schedule("happy");
                for(int i=morning_hour;i<evening_hour;i++) {
                    schedule.addHour(i);
                }
                schedule.randomize(Scheduler.RANDOM_TYPE_HOUR)
                        .setActionType(Scheduler.ACTION_TYPE_BROADCAST)
                        .setActionClass(ESM.ACTION_AWARE_QUEUE_ESM)
                        .addActionExtra(ESM.EXTRA_ESM, happy_esm);

                Scheduler.saveSchedule(this, schedule);

            } catch (JSONException e ) {
                e.printStackTrace();
            }

            //Create a random schedule for Stressed
            try {

                String stressed_esm = "[{'esm':{'esm_type':'"+ESM.TYPE_ESM_RADIO+"', 'esm_title':'Stressed/nervous', 'esm_instructions':'Are you stressed/nervous?','esm_radios':['NO','No','Yes','YES'],'esm_expiration_threshold':0,'esm_submit':'OK','esm_trigger':'"+getPackageName()+"'}}]";

                Scheduler.Schedule schedule = new Scheduler.Schedule("stressed");
                for(int i=morning_hour;i<evening_hour;i++) {
                    schedule.addHour(i);
                }
                schedule.randomize(Scheduler.RANDOM_TYPE_HOUR)
                        .setActionType(Scheduler.ACTION_TYPE_BROADCAST)
                        .setActionClass(ESM.ACTION_AWARE_QUEUE_ESM)
                        .addActionExtra(ESM.EXTRA_ESM, stressed_esm);

                Scheduler.saveSchedule(this, schedule);

            } catch (JSONException e ) {
                e.printStackTrace();
            }
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Aware.setSetting(this, Settings.STATUS_PLUGIN_UPMC_CANCER, false);
        Aware.stopPlugin(this, "com.aware.plugin.upmc.cancer");
    }
}
