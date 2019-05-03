package com.aware.plugin.upmc.dash.activities;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.aware.Aware;
import com.aware.plugin.upmc.dash.R;
import com.aware.plugin.upmc.dash.fileutils.FileManager;
import com.aware.plugin.upmc.dash.settings.Settings;
import com.aware.plugin.upmc.dash.utils.Constants;

import java.io.IOException;

public class OnboardingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setFinishOnTouchOutside(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            String[] REQUIRED_PERMISSIONS = new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.FOREGROUND_SERVICE};
            requestPermissions(REQUIRED_PERMISSIONS, 1);
        }
        else {
            Toast.makeText(getApplicationContext(), "Please grant permission!", Toast.LENGTH_SHORT).show();
        }


    }

    public void onRadioButtonClicked(View view) {
        try {
            FileManager.writeResourcesToHtdocs(view.getContext());
        } catch (IllegalAccessException | IOException e) {
            e.printStackTrace();
        }

        if(view.getId() == R.id.radio_control) {
            Log.d(Constants.TAG, "OnboardingActivity: starting in control mode");
            Aware.setSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_DEVICE_TYPE, Constants.DEVICE_TYPE_CONTROL );

        }
        else {
            Log.d(Constants.TAG, "OnboardingActivity: starting in regular mode");
            Aware.setSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_DEVICE_TYPE, Constants.DEVICE_TYPE_REGULAR );

        }
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1: {
                Log.d(Constants.TAG, "OnboardingActivity:Permissions Granted");
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    String deviceType = readDeviceType();
                    String deviceType = Aware.getSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_DEVICE_TYPE);
                    Log.d(Constants.TAG, "OnboardingActivity: deviceType " + deviceType);
                    switch (deviceType) {
                        case Constants.DEVICE_TYPE_REGULAR:
                        case Constants.DEVICE_TYPE_CONTROL:
                            Intent intent = new Intent(this, MainActivity.class);
                            startActivity(intent);
                            finish();
                            break;
                        default:
                            Log.d(Constants.TAG, "OnboardingActivity: showing mode selection UI");
                            setContentView(R.layout.activity_onboarding);
                            break;
                    }


                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.2
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(OnboardingActivity.this, "Permission denied. Cannot continue", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }



    public void writeDeviceType(String deviceType) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(Constants.PREFERENCES_KEY_DEVICE_TYPE, deviceType);
        editor.apply();
        Log.d(Constants.TAG, "OnboardingActivity:writeDeviceType: " + deviceType);
    }

    public String readDeviceType() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String deviceType = sharedPref.getString(Constants.PREFERENCES_KEY_DEVICE_TYPE, Constants.PREFERENCES_DEFAULT_DEVICE_TYPE);
        if (deviceType.equals(Constants.PREFERENCES_DEFAULT_DEVICE_TYPE))
            Log.d(Constants.TAG, "OnboardingActivity:readDeviceType: " + deviceType);
        return deviceType;
    }

}
