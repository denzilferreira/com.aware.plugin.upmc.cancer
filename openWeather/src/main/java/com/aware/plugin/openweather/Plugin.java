package com.aware.plugin.openweather;

import android.Manifest;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ServiceCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.plugin.openweather.Provider.OpenWeather_Data;
import com.aware.ui.PermissionsHandler;
import com.aware.utils.Aware_Plugin;
import com.aware.utils.Http;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;

public class Plugin extends Aware_Plugin implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
	
	/**
	 * Shared context: new OpenWeather data is available
	 */
	public static final String ACTION_AWARE_PLUGIN_OPENWEATHER = "ACTION_AWARE_PLUGIN_OPENWEATHER";
	
	/**
	 * Extra string: openweather<br/>
	 * JSONObject from OpenWeather<br/>
	 * http://bugs.openweathermap.org/projects/api/wiki/Weather_Data
	 */
	public static final String EXTRA_OPENWEATHER = "openweather";
	
	private static final String OPENWEATHER_API_URL = "http://api.openweathermap.org/data/2.5/weather?lat=%f&lon=%f&lang=%s&units=%s&appid=%s";
	private static ContextProducer sContextProducer;
	private static JSONObject sOpenWeather;

    private static GoogleApiClient mGoogleApiClient;
    private static LocationRequest locationRequest;

	@Override
	public void onCreate() {
		super.onCreate();

        if( Aware.getSetting(this, Settings.STATUS_PLUGIN_OPENWEATHER).length() == 0 ) {
            Aware.setSetting(this, Settings.STATUS_PLUGIN_OPENWEATHER, true);
        }
		if( Aware.getSetting(getApplicationContext(), Settings.UNITS_PLUGIN_OPENWEATHER).length() == 0 ) {
			Aware.setSetting(getApplicationContext(), Settings.UNITS_PLUGIN_OPENWEATHER, "metric");
		}
        if( Aware.getSetting(getApplicationContext(), Settings.PLUGIN_OPENWEATHER_FREQUENCY).length() == 0 ) {
            Aware.setSetting(getApplicationContext(), Settings.PLUGIN_OPENWEATHER_FREQUENCY, 30);
        }
        if( Aware.getSetting(getApplicationContext(), Settings.OPENWEATHER_API_KEY).length() == 0 ) {
            Aware.setSetting(getApplicationContext(), Settings.OPENWEATHER_API_KEY, "");
        }

		CONTEXT_PRODUCER = new ContextProducer() {
			@Override
			public void onContext() {
				Intent mOpenWeather = new Intent(ACTION_AWARE_PLUGIN_OPENWEATHER);
				mOpenWeather.putExtra(EXTRA_OPENWEATHER, sOpenWeather.toString());
				sendBroadcast(mOpenWeather);
			}
		};
		sContextProducer = CONTEXT_PRODUCER;

        //Permissions needed for our plugin
        REQUIRED_PERMISSIONS.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        REQUIRED_PERMISSIONS.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);

        DATABASE_TABLES = Provider.DATABASE_TABLES;
		TABLES_FIELDS = Provider.TABLES_FIELDS;
		CONTEXT_URIS = new Uri[]{ OpenWeather_Data.CONTENT_URI };

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApiIfAvailable(LocationServices.API)
                .build();

        locationRequest = new LocationRequest();
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_LOW_POWER);

        Aware.startPlugin(this, "com.aware.plugin.openweather");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
        if( ! is_google_services_available() ) {
            Log.e(TAG,"Google Services fused location is not available on this device.");
            stopSelf();
        } else {
            if (!mGoogleApiClient.isConnecting() && !mGoogleApiClient.isConnected()) {
                mGoogleApiClient.connect();
            }
            TAG = "AWARE::OpenWeather";
            DEBUG = Aware.getSetting(getApplicationContext(), Aware_Preferences.DEBUG_FLAG).equals("true");

            if ( mGoogleApiClient.isConnected() ) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    Intent openWeatherIntent = new Intent(getApplicationContext(), OpenWeather_Service.class);
                    PendingIntent openWeatherFetcher = PendingIntent.getService(getApplicationContext(), 0, openWeatherIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                    LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locationRequest, openWeatherFetcher);
                }
            }
        }
		return super.onStartCommand(intent, flags, startId);
	}

    private boolean is_google_services_available() {
        return (ConnectionResult.SUCCESS == GooglePlayServicesUtil.isGooglePlayServicesAvailable(getApplicationContext()));
    }

	@Override
	public void onDestroy() {
		super.onDestroy();
        if( mGoogleApiClient != null && mGoogleApiClient.isConnected() ) {
            Intent openWeatherIntent = new Intent(this, OpenWeather_Service.class);
            PendingIntent openWeatherFetcher = PendingIntent.getService(this, 0, openWeatherIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, openWeatherFetcher);
        }
        Aware.stopPlugin(this, "com.aware.plugin.openweather");
	}

    @Override
    public void onConnected(Bundle bundle) {
        Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if( lastLocation != null ) {
            if( DEBUG) Log.d(TAG,"Updating weather every " + Aware.getSetting(this, Settings.PLUGIN_OPENWEATHER_FREQUENCY) + " minute(s)");
            locationRequest.setInterval( Integer.valueOf(Aware.getSetting(this, Settings.PLUGIN_OPENWEATHER_FREQUENCY)) * 60 * 1000 ); //in minutes

            Intent openWeatherIntent = new Intent(getApplicationContext(), OpenWeather_Service.class);
            openWeatherIntent.putExtra(LocationServices.FusedLocationApi.KEY_LOCATION_CHANGED, lastLocation);
            startService(openWeatherIntent);
        }
        if( ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ) {
            Intent openWeatherIntent = new Intent(this, OpenWeather_Service.class);
            PendingIntent openWeatherFetcher = PendingIntent.getService(this, 0, openWeatherIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locationRequest, openWeatherFetcher );
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        if( ! mGoogleApiClient.isConnecting() ) mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if( connectionResult.getErrorCode() == ConnectionResult.API_UNAVAILABLE ) stopSelf();
    }

    /**
	 * Background service that will connect to OpenWeather API and fetch and store current weather conditions depending on the user's location
	 * @author dferreira
	 */
	public static class OpenWeather_Service extends IntentService {
		public OpenWeather_Service() {
			super("AWARE OpenWeather");
		}

		@Override
		protected void onHandleIntent(Intent intent) {
            Location location = (Location) intent.getExtras().get(LocationServices.FusedLocationApi.KEY_LOCATION_CHANGED);
            if( location == null ) return;

			double latitude = location.getLatitude();
			double longitude = location.getLongitude();
			
			if( latitude != 0 && longitude != 0 ) {

                Http httpObj = new Http(this);
				String server_response = httpObj.dataGET(String.format(OPENWEATHER_API_URL, latitude, longitude, Locale.getDefault().getLanguage(), Aware.getSetting(getApplicationContext(), Settings.UNITS_PLUGIN_OPENWEATHER), Aware.getSetting(getApplicationContext(), Settings.OPENWEATHER_API_KEY)), false);
                if( server_response == null || server_response.length() == 0 || server_response.contains("Invalid API key") ) return;

                try {
                    JSONObject raw_data = new JSONObject( server_response );

                    if( DEBUG ) Log.d(Plugin.TAG,"OpenWeather answer: " + raw_data.toString(5));

                    JSONObject wind = raw_data.getJSONObject("wind");
                    JSONObject weather_characteristics = raw_data.getJSONObject("main");
                    JSONObject weather = raw_data.getJSONArray("weather").getJSONObject(0);
                    JSONObject clouds = raw_data.getJSONObject("clouds");

                    JSONObject rain = null;
                    if( raw_data.opt("rain") != null ) {
                        rain = raw_data.optJSONObject("rain");
                    }
                    JSONObject snow = null;
                    if( raw_data.opt("snow") != null ) {
                        snow = raw_data.optJSONObject("snow");
                    }
                    JSONObject sys = raw_data.getJSONObject("sys");

                    ContentValues weather_data = new ContentValues();
                    weather_data.put(OpenWeather_Data.TIMESTAMP, System.currentTimeMillis());
                    weather_data.put(OpenWeather_Data.DEVICE_ID, Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
                    weather_data.put(OpenWeather_Data.CITY, raw_data.getString("name"));
                    weather_data.put(OpenWeather_Data.TEMPERATURE, weather_characteristics.getDouble("temp"));
                    weather_data.put(OpenWeather_Data.TEMPERATURE_MAX, weather_characteristics.getDouble("temp_max"));
                    weather_data.put(OpenWeather_Data.TEMPERATURE_MIN, weather_characteristics.getDouble("temp_min"));
                    weather_data.put(OpenWeather_Data.UNITS, Aware.getSetting(getApplicationContext(), Settings.UNITS_PLUGIN_OPENWEATHER));
                    weather_data.put(OpenWeather_Data.HUMIDITY, weather_characteristics.getDouble("humidity"));
                    weather_data.put(OpenWeather_Data.PRESSURE, weather_characteristics.getDouble("pressure"));
                    weather_data.put(OpenWeather_Data.WIND_SPEED, wind.getDouble("speed"));
                    weather_data.put(OpenWeather_Data.WIND_DEGREES, wind.getDouble("deg"));
                    weather_data.put(OpenWeather_Data.CLOUDINESS, clouds.getDouble("all"));

                    double rain_value = 0;
                    if( rain != null ) {
                        if (rain.opt("1h") != null) {
                            rain_value = rain.optDouble("1h", 0);
                        } else if (rain.opt("3h") != null) {
                            rain_value = rain.optDouble("3h", 0);
                        } else if (rain.opt("6h") != null) {
                            rain_value = rain.optDouble("6h", 0);
                        } else if (rain.opt("12h") != null) {
                            rain_value = rain.optDouble("12h", 0);
                        } else if (rain.opt("24h") != null) {
                            rain_value = rain.optDouble("24h", 0);
                        } else if (rain.opt("day") != null) {
                            rain_value = rain.optDouble("day", 0);
                        }
                    }

                    double snow_value = 0;
                    if( snow != null ) {
                        if (snow.opt("1h") != null) {
                            snow_value = snow.optDouble("1h", 0);
                        } else if (snow.opt("3h") != null) {
                            snow_value = snow.optDouble("3h", 0);
                        } else if (snow.opt("6h") != null) {
                            snow_value = snow.optDouble("6h", 0);
                        } else if (snow.opt("12h") != null) {
                            snow_value = snow.optDouble("12h", 0);
                        } else if (snow.opt("24h") != null) {
                            snow_value = snow.optDouble("24h", 0);
                        } else if (snow.opt("day") != null) {
                            snow_value = snow.optDouble("day", 0);
                        }
                    }
                    weather_data.put(OpenWeather_Data.RAIN, rain_value);
                    weather_data.put(OpenWeather_Data.SNOW, snow_value);
                    weather_data.put(OpenWeather_Data.SUNRISE, sys.getDouble("sunrise"));
                    weather_data.put(OpenWeather_Data.SUNSET, sys.getDouble("sunset"));
                    weather_data.put(OpenWeather_Data.WEATHER_ICON_ID, weather.getInt("id"));
                    weather_data.put(OpenWeather_Data.WEATHER_DESCRIPTION, weather.getString("main") + ": "+weather.getString("description"));

                    getContentResolver().insert(OpenWeather_Data.CONTENT_URI, weather_data);

                    sOpenWeather = raw_data;

                    sContextProducer.onContext();

                    if( DEBUG) Log.d(TAG, weather_data.toString());

                } catch (JSONException e) {
                    if( DEBUG ) Log.d(TAG,"Error reading JSON: " + e.getMessage());
                } catch( NullPointerException e ) {
                    if( DEBUG ) Log.d(TAG,"Failed to parse JSON from server:");
                    e.printStackTrace();
                }
			}
		}
	}
}
