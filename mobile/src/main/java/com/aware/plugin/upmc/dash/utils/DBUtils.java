package com.aware.plugin.upmc.dash.utils;

import android.content.ContentValues;
import android.content.Context;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.plugin.upmc.dash.activities.Provider;

public class DBUtils {

    public static void saveSensor(Context context, double timeStamp, int type, int data, String session_id) {
        ContentValues step_count = new ContentValues();
        step_count.put(Provider.Stepcount_Data.DEVICE_ID, Aware.getSetting(context, Aware_Preferences.DEVICE_ID));
        step_count.put(Provider.Stepcount_Data.TIMESTAMP, timeStamp);
        step_count.put(Provider.Stepcount_Data.STEP_COUNT, data);
        step_count.put(Provider.Stepcount_Data.ALARM_TYPE, type);
        step_count.put(Provider.Stepcount_Data.SESSION_ID, session_id);
        context.getContentResolver().insert(Provider.Stepcount_Data.CONTENT_URI, step_count);
    }


    public static void saveIntervention(Context context, double timestamp, String session_id, int notif_type, int notif_device, int snooze_shown) {
        ContentValues intervention = new ContentValues();
        intervention.put(Provider.Notification_Interventions.DEVICE_ID, Aware.getSetting(context, Aware_Preferences.DEVICE_ID));
        intervention.put(Provider.Notification_Interventions.TIMESTAMP, timestamp);
        intervention.put(Provider.Notification_Interventions.NOTIF_ID, session_id);
        intervention.put(Provider.Notification_Interventions.NOTIF_TYPE, notif_type);
        intervention.put(Provider.Notification_Interventions.NOTIF_DEVICE, notif_device);
        intervention.put(Provider.Notification_Interventions.SNOOZE_SHOWN, snooze_shown);
        context.getContentResolver().insert(Provider.Notification_Interventions.CONTENT_URI, intervention);
    }

    public static void saveResponseWatch(Context context,   double timestamp, String session_id, int busy, int pain, int nausea,
                                      int tired, int other, int ok, int no, int snooze
                                      ) {
        ContentValues response = new ContentValues();
        response.put(Provider.Notification_Responses.DEVICE_ID, Aware.getSetting(context, Aware_Preferences.DEVICE_ID));
        response.put(Provider.Notification_Responses.TIMESTAMP, timestamp);
        response.put(Provider.Notification_Responses.NOTIF_ID, session_id);
        response.put(Provider.Notification_Responses.NOTIF_TYPE, Constants.NOTIF_TYPE_INACTIVITY);
        response.put(Provider.Notification_Responses.NOTIF_DEVICE, Constants.NOTIF_DEVICE_WATCH);
        response.put(Provider.Notification_Responses.RESP_OK, ok);
        response.put(Provider.Notification_Responses.RESP_NO, no);
        response.put(Provider.Notification_Responses.RESP_SNOOZE, snooze);
        response.put(Provider.Notification_Responses.RESP_BUSY, busy);
        response.put(Provider.Notification_Responses.RESP_PAIN, pain);
        response.put(Provider.Notification_Responses.RESP_NAUSEA, nausea);
        response.put(Provider.Notification_Responses.RESP_TIRED, tired);
        response.put(Provider.Notification_Responses.RESP_OTHER, other);
        context.getContentResolver().insert(Provider.Notification_Responses.CONTENT_URI, response);
    }


}
