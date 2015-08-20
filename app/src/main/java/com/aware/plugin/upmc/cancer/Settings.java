package com.aware.plugin.upmc.cancer;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);
        syncSettings();
    }

    @Override
    protected void onResume() {
        super.onResume();
        syncSettings();
    }

    private void syncSettings() {
        CheckBoxPreference status = (CheckBoxPreference) findPreference(STATUS_PLUGIN_UPMC_CANCER);
        status.setChecked(Aware.getSetting(this, STATUS_PLUGIN_UPMC_CANCER).equals("true"));

        EditTextPreference max_prompts = (EditTextPreference) findPreference(PLUGIN_UPMC_CANCER_MAX_PROMPTS);
        max_prompts.setText(Aware.getSetting(this, PLUGIN_UPMC_CANCER_MAX_PROMPTS));
        max_prompts.setSummary(Aware.getSetting(this, PLUGIN_UPMC_CANCER_MAX_PROMPTS) + " questions");

        EditTextPreference min_interval_prompts = (EditTextPreference) findPreference(PLUGIN_UPMC_CANCER_PROMPT_INTERVAL);
        min_interval_prompts.setText(Aware.getSetting(this, PLUGIN_UPMC_CANCER_PROMPT_INTERVAL));
        min_interval_prompts.setSummary(Aware.getSetting(this, PLUGIN_UPMC_CANCER_PROMPT_INTERVAL) + " minutes");
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if( key.equalsIgnoreCase(STATUS_PLUGIN_UPMC_CANCER) ) {
            if( sharedPreferences.getBoolean(key, false) ) {
                Aware.setSetting(this, key, true);
                Aware.startPlugin(this, getPackageName());
            } else {
                Aware.setSetting(this, key, false);
                Aware.stopPlugin(this, getPackageName());
            }
            return;
        }
        if( key.equalsIgnoreCase(PLUGIN_UPMC_CANCER_MAX_PROMPTS) ) {
            Aware.setSetting(this, key, sharedPreferences.getInt(key, 8));
        }
        if( key.equalsIgnoreCase(PLUGIN_UPMC_CANCER_PROMPT_INTERVAL) ) {
            Aware.setSetting(this, key, sharedPreferences.getInt(key, 30));
        }
        //start plugin again with the new settings
        Aware.startPlugin(this, getPackageName());
    }
}
