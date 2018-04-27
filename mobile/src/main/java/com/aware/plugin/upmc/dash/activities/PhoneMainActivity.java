package com.aware.plugin.upmc.dash.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import com.aware.plugin.upmc.dash.R;
import com.aware.plugin.upmc.dash.utils.Constants;

public class PhoneMainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setFinishOnTouchOutside(false);
        String deviceType = readDeviceType();
        switch (deviceType) {
            case Constants.DEVICE_TYPE_FITBIT:
                Log.d(Constants.TAG, "PhoneMainActivity:starting in Fitbit mode");
                break;
            case Constants.DEVICE_TYPE_ANDROID:
                Log.d(Constants.TAG, "PhoneMainActivity: starting in Android mode");
                Intent intent = new Intent(this, UPMC.class);
                startActivity(intent);
                finish();
                break;
            case Constants.PREFERENCES_DEFAULT_DEVICE_TYPE:
                Log.d(Constants.TAG, "PhoneMainActivity: showing mode selection UI");
                setContentView(R.layout.activity_main);
                break;
        }
    }

    public void onRadioButtonClicked(View view) {
        if(view.getId() == R.id.radio_fitbit) {
            writeDeviceType(Constants.DEVICE_TYPE_FITBIT);
        }
        else {
            writeDeviceType(Constants.DEVICE_TYPE_ANDROID);
            Log.d(Constants.TAG, "PhoneMainActivity: starting in Android mode");
            Intent intent = new Intent(this, UPMC.class);
            startActivity(intent);
            finish();
        }
    }

    public void writeDeviceType(String deviceType) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(Constants.PREFERENCES_KEY_DEVICE_TYPE, deviceType);
        editor.apply();
        Log.d(Constants.TAG, "PhoneMainActivity:writeDeviceType: " + deviceType);
    }

    public String readDeviceType() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String deviceType = sharedPref.getString(Constants.PREFERENCES_KEY_DEVICE_TYPE, Constants.PREFERENCES_DEFAULT_DEVICE_TYPE);
        if (deviceType.equals(Constants.PREFERENCES_DEFAULT_DEVICE_TYPE))
            Log.d(Constants.TAG, "PhoneMainActivity:writeDeviceType: " + deviceType);
        return deviceType;
    }

}
