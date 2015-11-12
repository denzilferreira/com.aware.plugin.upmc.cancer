
package com.aware.plugin.google.activity_recognition;

import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.plugin.google.activity_recognition.Google_AR_Provider.Google_Activity_Recognition_Data;
import com.aware.utils.Aware_Plugin;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;

public class Plugin extends Aware_Plugin implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private String PACKAGE_NAME = "com.aware.plugin.google.activity_recognition";

	public static String ACTION_AWARE_GOOGLE_ACTIVITY_RECOGNITION = "ACTION_AWARE_GOOGLE_ACTIVITY_RECOGNITION";
	public static String EXTRA_ACTIVITY = "activity";
	public static String EXTRA_CONFIDENCE = "confidence";

	private static GoogleApiClient gARClient;

	public static int current_activity = -1;
	public static int current_confidence = -1;

	@Override
	public void onCreate() {
		super.onCreate();

		TAG = "AWARE::Google Activity Recognition";

		DATABASE_TABLES = Google_AR_Provider.DATABASE_TABLES;
		TABLES_FIELDS = Google_AR_Provider.TABLES_FIELDS;
		CONTEXT_URIS = new Uri[]{ Google_Activity_Recognition_Data.CONTENT_URI };
		
		CONTEXT_PRODUCER = new ContextProducer() {
			@Override
			public void onContext() {
				Intent context = new Intent(ACTION_AWARE_GOOGLE_ACTIVITY_RECOGNITION);
				context.putExtra(EXTRA_ACTIVITY, current_activity);
				context.putExtra(EXTRA_CONFIDENCE, current_confidence);
				sendBroadcast(context);
			}
		};

		Aware.setSetting(getApplicationContext(), Settings.STATUS_PLUGIN_GOOGLE_ACTIVITY_RECOGNITION, true);
		if( Aware.getSetting(getApplicationContext(), Settings.FREQUENCY_PLUGIN_GOOGLE_ACTIVITY_RECOGNITION).length() == 0 ) {
			Aware.setSetting(getApplicationContext(), Settings.FREQUENCY_PLUGIN_GOOGLE_ACTIVITY_RECOGNITION, 60);
		}

		Intent gARIntent = new Intent(getApplicationContext(), com.aware.plugin.google.activity_recognition.Algorithm.class);
		PendingIntent gARPending = PendingIntent.getService(getApplicationContext(), 0, gARIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        gARClient = new GoogleApiClient.Builder(this)
                .addApiIfAvailable(ActivityRecognition.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

		Aware.setSetting(this, Settings.STATUS_PLUGIN_GOOGLE_ACTIVITY_RECOGNITION, true);
		if( Aware.getSetting(this, Settings.FREQUENCY_PLUGIN_GOOGLE_ACTIVITY_RECOGNITION).length() == 0 ) {
			Aware.setSetting(this, Settings.FREQUENCY_PLUGIN_GOOGLE_ACTIVITY_RECOGNITION, 60);
		}

		Aware.startPlugin(this, PACKAGE_NAME);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		DEBUG = Aware.getSetting(this, Aware_Preferences.DEBUG_FLAG).equals("true");

		if ( ! is_google_services_available() ) {
			Log.e(TAG,"Google Services activity recognition not available on this device.");
			stopSelf();
		} else {
			gARClient.connect();
            if( gARClient.isConnected() ) {
				Intent gARIntent = new Intent(getApplicationContext(), com.aware.plugin.google.activity_recognition.Algorithm.class);
				PendingIntent gARPending = PendingIntent.getService(getApplicationContext(), 0, gARIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(gARClient, Long.valueOf(Aware.getSetting(getApplicationContext(), Settings.FREQUENCY_PLUGIN_GOOGLE_ACTIVITY_RECOGNITION)) * 1000, gARPending);
            }
		}
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

        Aware.setSetting(getApplicationContext(), Settings.STATUS_PLUGIN_GOOGLE_ACTIVITY_RECOGNITION, false);

		//we might get here if phone doesn't support Google Services
		if ( gARClient != null && gARClient.isConnected() ) {
			Intent gARIntent = new Intent(getApplicationContext(), com.aware.plugin.google.activity_recognition.Algorithm.class);
			PendingIntent gARPending = PendingIntent.getService(getApplicationContext(), 0, gARIntent, PendingIntent.FLAG_UPDATE_CURRENT);
			ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates( gARClient, gARPending );
		}

        Aware.stopPlugin(this, PACKAGE_NAME);
	}

	private boolean is_google_services_available() {
		return (ConnectionResult.SUCCESS == GooglePlayServicesUtil.isGooglePlayServicesAvailable(getApplicationContext()));
	}

	@Override
	public void onConnectionFailed(ConnectionResult connection_result) {
		if( connection_result.getErrorCode() == ConnectionResult.API_UNAVAILABLE ) {
			stopSelf();
		}
		Log.w(TAG,"Error connecting to Google's activity recognition services, will try again in 5 minutes");
	}

	@Override
	public void onConnected(Bundle bundle) {
        Log.i(TAG, "Connected to Google's Activity Recognition API");
		Aware.startPlugin(this, PACKAGE_NAME);
	}

    @Override
    public void onConnectionSuspended(int i) {
        Log.w(TAG,"Error connecting to Google's activity recognition services, will try again in 5 minutes");
    }
}
