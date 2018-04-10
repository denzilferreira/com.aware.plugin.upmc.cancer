package com.aware.plugin.upmc.dash.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class Preferences {
    public static int[] readTime(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        int morn_hour = sharedPref.getInt(Constants.MORNING_HOUR, -1);
        int morn_minute = sharedPref.getInt(Constants.MORNING_MINUTE, -1);
        int night_hour = sharedPref.getInt(Constants.NIGHT_HOUR, -1);
        int night_minute = sharedPref.getInt(Constants.NIGHT_MINUTE, -1);
        int[] timePrefs = new int[4];
        timePrefs[0] = morn_hour;
        timePrefs[1] = morn_minute;
        timePrefs[2] = night_hour;
        timePrefs[3] = night_minute;
        Log.d(Constants.TAG, "Preferences:readTimePref:" + morn_hour + " " + morn_minute + " " + night_hour + " " + night_minute);
        return timePrefs;
    }

    public static void writeTime(Context context, int morn_hour, int morn_minute, int night_hour, int night_minute) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(Constants.MORNING_HOUR, morn_hour);
        editor.putInt(Constants.MORNING_MINUTE, morn_minute);
        editor.putInt(Constants.NIGHT_HOUR, night_hour);
        editor.putInt(Constants.NIGHT_MINUTE, night_minute);
        editor.apply();
    }



    public static boolean isTimeInitialized(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        int morn_hour = sharedPref.getInt(Constants.MORNING_HOUR, -1);
        int morn_minute = sharedPref.getInt(Constants.MORNING_MINUTE, -1);
        return !(morn_hour ==-1 || morn_minute == -1);
    }

    public static void writeSymptomRating(Context context, int type) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(Constants.SYMPTOMS_PREFS, type);
        editor.apply();
    }

    public static int getSymptomRating(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        int type = sharedPref.getInt(Constants.SYMPTOMS_PREFS, -1);
        Log.d(Constants.TAG, "Preferences:readSymptomsPref: " + type);
        return type;
    }

    public static boolean isSymptomRatingInitalized(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        int type = sharedPref.getInt(Constants.SYMPTOMS_PREFS, -1);
        return!(type==-1);
    }

    public static boolean isTimeAndRatingInitialized(Context context) {
        return (isTimeInitialized(context) && isSymptomRatingInitalized(context));
    }

    public static int[] getMorningTime(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        int morn_hour = sharedPref.getInt(Constants.MORNING_HOUR, -1);
        int morn_minute = sharedPref.getInt(Constants.MORNING_MINUTE, -1);
        int[] morning_time = new int[2];
        morning_time[0] = morn_hour;
        morning_time[1] = morn_minute;
        return morning_time;
    }

    public static int[] getNightTime(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        int night_hour = sharedPref.getInt(Constants.NIGHT_HOUR, -1);
        int night_minute = sharedPref.getInt(Constants.NIGHT_MINUTE, -1);
        int[] night_time = new int[2];
        night_time[0] = night_hour;
        night_time[1] = night_minute;
        return night_time;
    }


}
