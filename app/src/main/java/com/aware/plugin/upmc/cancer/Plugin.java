package com.aware.plugin.upmc.cancer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.aware.Aware;
import com.aware.ESM;
import com.aware.ui.esms.ESMFactory;
import com.aware.ui.esms.ESM_Radio;
import com.aware.utils.Aware_Plugin;
import com.aware.utils.Scheduler;

import org.json.JSONException;

public class Plugin extends Aware_Plugin {

    public static String ACTION_CANCER_SURVEY = "ACTION_CANCER_SURVEY";
    public static String ACTION_CANCER_EMOTION = "ACTION_CANCER_EMOTION";

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

            if (intent.getAction().equals(ACTION_CANCER_EMOTION)) {
                try {
                    ESMFactory factory = new ESMFactory();
                    ESM_Radio angry = new ESM_Radio();
                    angry.setTitle("Angry/frustrated");
                    angry.setInstructions("Are you angry/frustrated?");
                    angry.addRadio("NO");
                    angry.addRadio("No");
                    angry.addRadio("Yes");
                    angry.addRadio("YES");
                    angry.setReplaceQueue(true); //replace the old queue if we get a new queue
                    angry.setNotificationRetry(3);
                    angry.setNotificationTimeout(10 * 60); //repeat every 10 minutes, up to 3 times
                    angry.setSubmitButton("Next");

                    ESM_Radio happy = new ESM_Radio();
                    happy.setTitle("Happy");
                    happy.setInstructions("Are you happy?");
                    happy.addRadio("NO");
                    happy.addRadio("No");
                    happy.addRadio("Yes");
                    happy.addRadio("YES");
                    happy.setSubmitButton("Next");

                    ESM_Radio stressed = new ESM_Radio();
                    stressed.setTitle("Stressed/nervous");
                    stressed.setInstructions("Are you stressed/nervous?");
                    stressed.addRadio("NO");
                    stressed.addRadio("No");
                    stressed.addRadio("Yes");
                    stressed.addRadio("YES");
                    stressed.setSubmitButton("Thanks!");

                    factory.addESM(angry);
                    factory.addESM(happy);
                    factory.addESM(stressed);

                    Intent esm;
                    if (intent.getBooleanExtra("demo", false)) {
                        esm = new Intent(ESM.ACTION_AWARE_TRY_ESM);
                    } else {
                        esm = new Intent(ESM.ACTION_AWARE_QUEUE_ESM);
                    }
                    esm.putExtra(ESM.EXTRA_ESM, factory.build());
                    context.sendBroadcast(esm);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        if (PERMISSIONS_OK) {

            Aware.setSetting(this, Settings.STATUS_PLUGIN_UPMC_CANCER, true);

            if (Aware.getSetting(this, Settings.PLUGIN_UPMC_CANCER_MAX_PROMPTS).length() == 0) {
                Aware.setSetting(this, Settings.PLUGIN_UPMC_CANCER_MAX_PROMPTS, 8);
            }
            if (DEBUG)
                Log.d(TAG, "Max questions per day: " + Aware.getSetting(this, Settings.PLUGIN_UPMC_CANCER_MAX_PROMPTS));

            if (Aware.getSetting(this, Settings.PLUGIN_UPMC_CANCER_PROMPT_INTERVAL).length() == 0) {
                Aware.setSetting(this, Settings.PLUGIN_UPMC_CANCER_PROMPT_INTERVAL, 30);
            }
            if (DEBUG)
                Log.d(TAG, "Minimum interval between questions: " + Aware.getSetting(this, Settings.PLUGIN_UPMC_CANCER_PROMPT_INTERVAL) + " minutes");

            if (intent != null && intent.getExtras() != null && intent.getBooleanExtra("schedule", false)) {
                int morning_hour = Integer.parseInt(Aware.getSetting(this, Settings.PLUGIN_UPMC_CANCER_MORNING_HOUR));
                int morning_minute = Integer.parseInt(Aware.getSetting(this, Settings.PLUGIN_UPMC_CANCER_MORNING_MINUTE));
                int evening_hour = Integer.parseInt(Aware.getSetting(this, Settings.PLUGIN_UPMC_CANCER_EVENING_HOUR));
                int evening_minute = Integer.parseInt(Aware.getSetting(this, Settings.PLUGIN_UPMC_CANCER_EVENING_MINUTE));
                int max_prompts = Integer.parseInt(Aware.getSetting(this, Settings.PLUGIN_UPMC_CANCER_MAX_PROMPTS));
                int min_interval = Integer.parseInt(Aware.getSetting(this, Settings.PLUGIN_UPMC_CANCER_PROMPT_INTERVAL));

                try {
                    Scheduler.removeSchedule(getApplicationContext(), "cancer_survey_morning");
                    Scheduler.Schedule schedule = new Scheduler.Schedule("cancer_survey_morning");
                    schedule.addHour(morning_hour)
                            .addMinute(morning_minute)
                            .setActionIntentAction(Plugin.ACTION_CANCER_SURVEY)
                            .setActionType(Scheduler.ACTION_TYPE_BROADCAST);
                    Scheduler.saveSchedule(this, schedule);

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                try {
                    Scheduler.removeSchedule(getApplicationContext(), "cancer_survey_evening");
                    Scheduler.Schedule schedule = new Scheduler.Schedule("cancer_survey_evening");
                    schedule.addHour(evening_hour)
                            .addMinute(evening_minute)
                            .setActionType(Scheduler.ACTION_TYPE_BROADCAST)
                            .setActionIntentAction(Plugin.ACTION_CANCER_SURVEY);
                    Scheduler.saveSchedule(this, schedule);

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                try {
                    Scheduler.Schedule schedule = Scheduler.getSchedule(getApplicationContext(), "cancer_emotion");
                    if (schedule == null) {
                        schedule = new Scheduler.Schedule("cancer_emotion");
                        schedule.addHour(morning_hour);
                        schedule.addHour(evening_hour);
                        schedule.random(max_prompts, min_interval); //randoms between morning and evening hours
                        schedule.setActionType(Scheduler.ACTION_TYPE_BROADCAST)
                                .setActionIntentAction(Plugin.ACTION_CANCER_EMOTION);
                        Scheduler.saveSchedule(this, schedule);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            Aware.startAWARE(this);
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Aware.setSetting(this, Settings.STATUS_PLUGIN_UPMC_CANCER, false);
        Aware.stopAWARE(this);
    }
}
