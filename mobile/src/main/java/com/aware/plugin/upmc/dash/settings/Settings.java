package com.aware.plugin.upmc.dash.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;

import com.aware.Aware;
import com.aware.plugin.upmc.dash.R;
import com.aware.plugin.upmc.dash.utils.Constants;

/**
 * Created by denzil on 13/04/15.
 * Edited by Grace on 19/08/15
 */
public class Settings extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    /**
     * Activate/deactivate plugin
     */
    public static final String STATUS_PLUGIN_UPMC_CANCER = "status_plugin_upmc_cancer";

    /**
     * How many prompts per day: default = 8 per day
     */
    public static final String PLUGIN_UPMC_CANCER_MAX_PROMPTS = "plugin_upmc_cancer_max_prompts";

    /**
     * How many minutes between each prompt: default = 30 minutes
     */
    public static final String PLUGIN_UPMC_CANCER_PROMPT_INTERVAL = "plugin_upmc_cancer_prompt_interval";

    public static final String PLUGIN_UPMC_CANCER_MORNING_HOUR = "plugin_upmc_cancer_morning_hour";
    public static final String PLUGIN_UPMC_CANCER_MORNING_MINUTE = "plugin_upmc_cancer_morning_minute";
    public static final String PLUGIN_UPMC_CANCER_NIGHT_HOUR = "plugin_upmc_cancer_night_hour";
    public static final String PLUGIN_UPMC_CANCER_NIGHT_MINUTE = "plugin_upmc_cancer_night_minute";
    public static final String PLUGIN_UPMC_CANCER_SYMPTOM_SEVERITY =  "plugin_upmc_cancer_symptom_severity";

    private CheckBoxPreference status;
    private EditTextPreference max_prompts, min_interval_prompts;

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
        status = (CheckBoxPreference) findPreference(STATUS_PLUGIN_UPMC_CANCER);
        if (Aware.getSetting(this, STATUS_PLUGIN_UPMC_CANCER).length() == 0) {
            Aware.setSetting(this, STATUS_PLUGIN_UPMC_CANCER, true);
        }
        status.setChecked(Aware.getSetting(this, STATUS_PLUGIN_UPMC_CANCER).equals("true"));

        max_prompts = (EditTextPreference) findPreference(PLUGIN_UPMC_CANCER_MAX_PROMPTS);
        if (Aware.getSetting(this, PLUGIN_UPMC_CANCER_MAX_PROMPTS).length() == 0) {
            Aware.setSetting(this, PLUGIN_UPMC_CANCER_MAX_PROMPTS, 8);
        }
        max_prompts.setText(Aware.getSetting(this, PLUGIN_UPMC_CANCER_MAX_PROMPTS));
        max_prompts.setSummary(Aware.getSetting(this, PLUGIN_UPMC_CANCER_MAX_PROMPTS) + " questions");

        min_interval_prompts = (EditTextPreference) findPreference(PLUGIN_UPMC_CANCER_PROMPT_INTERVAL);
        if (Aware.getSetting(this, PLUGIN_UPMC_CANCER_PROMPT_INTERVAL).length() == 0) {
            Aware.setSetting(this, PLUGIN_UPMC_CANCER_PROMPT_INTERVAL, 30);
        }
        min_interval_prompts.setText(Aware.getSetting(this, PLUGIN_UPMC_CANCER_PROMPT_INTERVAL));
        min_interval_prompts.setSummary(Aware.getSetting(this, PLUGIN_UPMC_CANCER_PROMPT_INTERVAL) + " minutes");
    }



    public static String readDeviceType(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        String deviceType = sharedPref.getString(Constants.PREFERENCES_KEY_DEVICE_TYPE, Constants.PREFERENCES_DEFAULT_DEVICE_TYPE);
        if (deviceType.equals(Constants.PREFERENCES_DEFAULT_DEVICE_TYPE))
            Log.d(Constants.TAG, "Settings:readDeviceType: " + deviceType);
        return deviceType;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equalsIgnoreCase(PLUGIN_UPMC_CANCER_MAX_PROMPTS)) {
            Aware.setSetting(this, key, sharedPreferences.getInt(key, 8));
            Aware.startPlugin(this, "com.aware.plugin.upmc.cancer");
        }
        if (key.equalsIgnoreCase(PLUGIN_UPMC_CANCER_PROMPT_INTERVAL)) {
            Aware.setSetting(this, key, sharedPreferences.getInt(key, 30));
            Aware.startPlugin(this, "com.aware.plugin.upmc.cancer");
        }
        if (key.equalsIgnoreCase(STATUS_PLUGIN_UPMC_CANCER)) {
            if (sharedPreferences.getBoolean(key, false)) {
                Aware.setSetting(this, key, true);
                Aware.startPlugin(this, "com.aware.plugin.upmc.cancer");
            } else {
                Aware.setSetting(this, key, false);
                Aware.stopPlugin(this, "com.aware.plugin.upmc.cancer");
            }
        }
    }
}
