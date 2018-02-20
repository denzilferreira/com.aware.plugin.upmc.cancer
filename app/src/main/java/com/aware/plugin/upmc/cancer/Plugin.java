package com.aware.plugin.upmc.cancer;

import android.accounts.Account;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SyncRequest;
import android.os.Bundle;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.utils.Aware_Plugin;
import com.aware.utils.Scheduler;

import org.json.JSONArray;
import org.json.JSONException;

public class Plugin extends Aware_Plugin {

    public static String ACTION_CANCER_SURVEY = "ACTION_CANCER_SURVEY";

    @Override
    public void onCreate() {
        super.onCreate();

        AUTHORITY = Provider.getAuthority(this);

        TAG = "UPMC Cancer";
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

            Aware.setSetting(this, Settings.STATUS_PLUGIN_UPMC_CANCER, true);

            try {
                if (Aware.getSetting(this, Settings.PLUGIN_UPMC_CANCER_MORNING_HOUR).length() == 0)
                    Aware.setSetting(this, Settings.PLUGIN_UPMC_CANCER_MORNING_HOUR, 9);

                if (Aware.getSetting(this, Settings.PLUGIN_UPMC_CANCER_MORNING_MINUTE).length() == 0)
                    Aware.setSetting(this, Settings.PLUGIN_UPMC_CANCER_MORNING_MINUTE, 0);

                int morning_hour = Integer.parseInt(Aware.getSetting(this, Settings.PLUGIN_UPMC_CANCER_MORNING_HOUR));
                int morning_minute = Integer.parseInt(Aware.getSetting(this, Settings.PLUGIN_UPMC_CANCER_MORNING_MINUTE));

                Scheduler.Schedule currentScheduler = Scheduler.getSchedule(getApplicationContext(), "cancer_survey_morning");
                if (currentScheduler == null) {
                    Scheduler.Schedule schedule = new Scheduler.Schedule("cancer_survey_morning");
                    schedule.addHour(morning_hour)
                            .addMinute(morning_minute)
                            .setActionIntentAction(Plugin.ACTION_CANCER_SURVEY)
                            .setActionType(Scheduler.ACTION_TYPE_BROADCAST);
                    Scheduler.saveSchedule(this, schedule);
                } else {
                    JSONArray hours = currentScheduler.getHours();
                    JSONArray minutes = currentScheduler.getMinutes();
                    boolean hour_changed = false;
                    boolean minute_changed = false;
                    for (int i = 0; i < hours.length(); i++) {
                        if (hours.getInt(i) != morning_hour) {
                            hour_changed = true;
                            break;
                        }
                    }
                    for (int i = 0; i < minutes.length(); i++) {
                        if (minutes.getInt(i) != morning_minute) {
                            minute_changed = true;
                            break;
                        }
                    }
                    if (hour_changed || minute_changed) {
                        Scheduler.removeSchedule(getApplicationContext(), "cancer_survey_morning");
                        Scheduler.Schedule schedule = new Scheduler.Schedule("cancer_survey_morning");
                        schedule.addHour(morning_hour)
                                .addMinute(morning_minute)
                                .setActionIntentAction(Plugin.ACTION_CANCER_SURVEY)
                                .setActionType(Scheduler.ACTION_TYPE_BROADCAST);
                        Scheduler.saveSchedule(this, schedule);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            if (Aware.isStudy(this)) {
                Account aware_account = Aware.getAWAREAccount(getApplicationContext());
                String authority = Provider.getAuthority(getApplicationContext());
                long frequency = Long.parseLong(Aware.getSetting(this, Aware_Preferences.FREQUENCY_WEBSERVICE)) * 60;

                ContentResolver.setIsSyncable(aware_account, authority, 1);
                ContentResolver.setSyncAutomatically(aware_account, authority, true);
                SyncRequest request = new SyncRequest.Builder()
                        .syncPeriodic(frequency, frequency / 3)
                        .setSyncAdapter(aware_account, authority)
                        .setExtras(new Bundle()).build();
                ContentResolver.requestSync(request);
            }

            Aware.startAWARE(this);
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ContentResolver.setSyncAutomatically(Aware.getAWAREAccount(this), Provider.getAuthority(this), false);
        ContentResolver.removePeriodicSync(
                Aware.getAWAREAccount(this),
                Provider.getAuthority(this),
                Bundle.EMPTY
        );

        Aware.setSetting(this, Settings.STATUS_PLUGIN_UPMC_CANCER, false);
        Aware.stopAWARE(this);
    }
}
