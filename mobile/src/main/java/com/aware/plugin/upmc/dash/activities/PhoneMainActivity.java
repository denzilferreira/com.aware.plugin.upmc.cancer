package com.aware.plugin.upmc.dash.activities;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.aware.plugin.upmc.dash.R;
import com.aware.plugin.upmc.dash.fileutils.FileManager;
import com.aware.plugin.upmc.dash.services.MessageService;
import com.aware.plugin.upmc.dash.utils.Constants;

import java.io.IOException;

public class PhoneMainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setFinishOnTouchOutside(false);
        String[] REQUIRED_PERMISSIONS = new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE};
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(REQUIRED_PERMISSIONS, 1);
        }
        else {
            Toast.makeText(getApplicationContext(), "Please grant permission!", Toast.LENGTH_SHORT).show();
        }

    }

    public void onRadioButtonClicked(View view) {
        if(view.getId() == R.id.radio_fitbit) {
            Log.d(Constants.TAG, "PhoneMainActivity: starting in Fitbit mode");
            writeDeviceType(Constants.DEVICE_TYPE_FITBIT);
            try {
                FileManager.writeResourcesToHtdocs(view.getContext());
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            Log.d(Constants.TAG, "PhoneMainActivity: starting in Android mode");
            writeDeviceType(Constants.DEVICE_TYPE_ANDROID);
        }
        Intent intent = new Intent(this, UPMC.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1: {
                Log.d(Constants.TAG, "PhoneMainActivity:Permissions Granted");
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    String deviceType = readDeviceType();
                    switch (deviceType) {
                        case Constants.DEVICE_TYPE_FITBIT:
                        case Constants.DEVICE_TYPE_ANDROID:
                            Intent intent = new Intent(this, UPMC.class);
                            startActivity(intent);
                            finish();
                            break;
                        case Constants.PREFERENCES_DEFAULT_DEVICE_TYPE:
                            Log.d(Constants.TAG, "PhoneMainActivity: showing mode selection UI");
                            setContentView(R.layout.activity_main);
                            break;
                    }


                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.2
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(PhoneMainActivity.this, "Permission denied. Cannot continue", Toast.LENGTH_SHORT).show();
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
        Log.d(Constants.TAG, "PhoneMainActivity:writeDeviceType: " + deviceType);
    }

    public String readDeviceType() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String deviceType = sharedPref.getString(Constants.PREFERENCES_KEY_DEVICE_TYPE, Constants.PREFERENCES_DEFAULT_DEVICE_TYPE);
        if (deviceType.equals(Constants.PREFERENCES_DEFAULT_DEVICE_TYPE))
            Log.d(Constants.TAG, "PhoneMainActivity:readDeviceType: " + deviceType);
        return deviceType;
    }

}
