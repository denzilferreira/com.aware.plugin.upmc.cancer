package com.aware.plugin.upmc.cancer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.aware.Aware;

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
    public static final String PLUGIN_UPMC_CANCER_EVENING_HOUR = "plugin_upmc_cancer_evening_hour";
    public static final String PLUGIN_UPMC_CANCER_EVENING_MINUTE = "plugin_upmc_cancer_evening_minute";

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
