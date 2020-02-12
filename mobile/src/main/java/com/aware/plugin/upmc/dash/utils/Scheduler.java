package com.aware.plugin.upmc.dash.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.aware.plugin.upmc.dash.activities.Plugin;
import com.aware.plugin.upmc.dash.services.FitbitMessageService;

import java.util.Calendar;

public class Scheduler {

    public static void setSurveySchedule(Context context, int hour, int minute) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, FitbitMessageService.class)
                .setAction(Plugin.ACTION_CANCER_SURVEY);
        PendingIntent pendingIntent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            pendingIntent = PendingIntent.getForegroundService(context, 0, intent, 0);
        } else {
            pendingIntent = PendingIntent.getService(context, 0, intent, 0);
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        assert alarmManager != null;
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY, pendingIntent);

    }

    private static void cancelSurveySchedule(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, FitbitMessageService.class)
                .setAction(Plugin.ACTION_CANCER_SURVEY);
        PendingIntent pendingIntent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            pendingIntent = PendingIntent.getForegroundService(context, 0, intent, 0);
        } else {
            pendingIntent = PendingIntent.getService(context, 0, intent, 0);
        }
        assert alarmManager != null;
        alarmManager.cancel(pendingIntent);

    }

    public static void resetSurveySchedule(Context context, int hour, int minute) {
        cancelSurveySchedule(context);
        setSurveySchedule(context, hour, minute);
    }


    public static void setSyncSchedule(Context context, int hour, int minute) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, FitbitMessageService.class)
                .setAction(Constants.ACTION_SYNC_DATA);
        PendingIntent pendingIntent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            pendingIntent = PendingIntent.getForegroundService(context, 0, intent, 0);
        } else {
            pendingIntent = PendingIntent.getService(context, 0, intent, 0);
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        assert alarmManager != null;
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY, pendingIntent);

    }

    private static void cancelSyncSchedule(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, FitbitMessageService.class)
                .setAction(Constants.ACTION_SYNC_DATA);
        PendingIntent pendingIntent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            pendingIntent = PendingIntent.getForegroundService(context, 0, intent, 0);
        } else {
            pendingIntent = PendingIntent.getService(context, 0, intent, 0);
        }
        assert alarmManager != null;
        alarmManager.cancel(pendingIntent);

    }

    public static void resetSyncSchedule(Context context, int hour, int minute) {
        cancelSyncSchedule(context);
        setSyncSchedule(context, hour, minute);
    }
}
