package com.aware.plugin.openweather;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import com.aware.Aware;

public class Settings extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	
	/**
	 * State
	 */
	public static final String STATUS_PLUGIN_OPENWEATHER = "status_plugin_openweather";
	
	/**
	 * Measurement units 
	 */
	public static final String UNITS_PLUGIN_OPENWEATHER = "units_plugin_openweather";

    /**
     * How frequently we status the weather conditions
     */
    public static final String PLUGIN_OPENWEATHER_FREQUENCY = "plugin_openweather_frequency";

    /**
     * Openweather API key
     */
	public static final String OPENWEATHER_API_KEY = "api_key_plugin_openweather";

	private static CheckBoxPreference status;
	private static ListPreference units;
	private static EditTextPreference frequency, openweather_api_key;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		prefs.registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onResume() {
		super.onResume();

        status = (CheckBoxPreference) findPreference(STATUS_PLUGIN_OPENWEATHER);
        if( Aware.getSetting(this, STATUS_PLUGIN_OPENWEATHER).length() == 0 ) {
            Aware.setSetting(this, STATUS_PLUGIN_OPENWEATHER, true); //by default, the setting is true on install
        }
        status.setChecked(Aware.getSetting(getApplicationContext(), STATUS_PLUGIN_OPENWEATHER).equals("true"));

        units = (ListPreference) findPreference(UNITS_PLUGIN_OPENWEATHER);
        units.setSummary( Aware.getSetting(getApplicationContext(), UNITS_PLUGIN_OPENWEATHER) );

        frequency = (EditTextPreference) findPreference(PLUGIN_OPENWEATHER_FREQUENCY);
        frequency.setText(Aware.getSetting(getApplicationContext(), PLUGIN_OPENWEATHER_FREQUENCY));
        frequency.setSummary("Every " + Aware.getSetting(getApplicationContext(), PLUGIN_OPENWEATHER_FREQUENCY) + " minute(s)");

        openweather_api_key = (EditTextPreference) findPreference(OPENWEATHER_API_KEY);
        openweather_api_key.setText(Aware.getSetting(getApplicationContext(), OPENWEATHER_API_KEY));

	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		Preference preference = findPreference(key);
		if( preference.getKey().equals(STATUS_PLUGIN_OPENWEATHER)) {
			boolean is_active = sharedPreferences.getBoolean(key, false);
			Aware.setSetting(getApplicationContext(), key, is_active);
			status.setChecked(is_active);
		}
		if( preference.getKey().equals(UNITS_PLUGIN_OPENWEATHER)) {
			preference.setSummary(sharedPreferences.getString(key, "metric"));
			Aware.setSetting(getApplicationContext(), key, sharedPreferences.getString(key, "metric"));
		}
        if( preference.getKey().equals(PLUGIN_OPENWEATHER_FREQUENCY)) {
            preference.setSummary("Every " + sharedPreferences.getString(key,"30") + " minute(s)");
            Aware.setSetting(getApplicationContext(), key, sharedPreferences.getString(key, "30"));
        }
        if( preference.getKey().equals(OPENWEATHER_API_KEY)) {
            Aware.setSetting(getApplicationContext(), key, sharedPreferences.getString(key, ""));
        }

		if( status.isChecked() ) {
			Aware.startPlugin(getApplicationContext(), "com.aware.plugin.openweather");
		} else {
			Aware.stopPlugin(getApplicationContext(), "com.aware.plugin.openweather");
		}
	}
}
