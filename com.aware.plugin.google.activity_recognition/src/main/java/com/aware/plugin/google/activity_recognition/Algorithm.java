/**
@author: denzil
 */

package com.aware.plugin.google.activity_recognition;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.util.Log;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.plugin.google.activity_recognition.Google_AR_Provider.Google_Activity_Recognition_Data;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

public class Algorithm extends IntentService {

    public Algorithm() {
        super(Plugin.TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        if (ActivityRecognitionResult.hasResult(intent)) {
            
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);

            DetectedActivity mostProbable = result.getMostProbableActivity();
            
            JSONArray activities = new JSONArray();
            List<DetectedActivity> otherActivities = result.getProbableActivities();
            for(DetectedActivity activity : otherActivities ) {
                try {
                    JSONObject item = new JSONObject();
                    item.put("activity", getActivityName(activity.getType()));
                    item.put("confidence", activity.getConfidence());
                    activities.put(item);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            Plugin.current_confidence = mostProbable.getConfidence();
            Plugin.current_activity = mostProbable.getType();
            String activity_name = getActivityName(Plugin.current_activity);

            ContentValues data = new ContentValues();
            data.put(Google_Activity_Recognition_Data.TIMESTAMP, System.currentTimeMillis());
            data.put(Google_Activity_Recognition_Data.DEVICE_ID, Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
            data.put(Google_Activity_Recognition_Data.ACTIVITY_NAME, activity_name);
            data.put(Google_Activity_Recognition_Data.ACTIVITY_TYPE, Plugin.current_activity);
            data.put(Google_Activity_Recognition_Data.CONFIDENCE, Plugin.current_confidence);
            data.put(Google_Activity_Recognition_Data.ACTIVITIES, activities.toString());

            getContentResolver().insert(Google_Activity_Recognition_Data.CONTENT_URI, data);

            if ( Plugin.DEBUG ) {
            	Log.d(Plugin.TAG, "User is: " + activity_name + " (conf:" + Plugin.current_confidence + ")");
            }

            Intent context = new Intent( Plugin.ACTION_AWARE_GOOGLE_ACTIVITY_RECOGNITION );
            context.putExtra( Plugin.EXTRA_ACTIVITY, Plugin.current_activity );
            context.putExtra( Plugin.EXTRA_CONFIDENCE, Plugin.current_confidence );
            sendBroadcast( context );
        }
    }

    public static String getActivityName(int type) {
        switch (type) {
            case DetectedActivity.IN_VEHICLE:
                return "in_vehicle";
            case DetectedActivity.ON_BICYCLE:
                return "on_bicycle";
            case DetectedActivity.ON_FOOT:
                return "on_foot";
            case DetectedActivity.STILL:
                return "still";
            case DetectedActivity.UNKNOWN:
                return "unknown";
            case DetectedActivity.TILTING:
                return "tilting";
            case DetectedActivity.RUNNING:
                return "running";
            case DetectedActivity.WALKING:
                return "walking";
            default:
                return "unknown";
        }
    }
}
