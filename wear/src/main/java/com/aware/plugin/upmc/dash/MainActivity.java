package com.aware.plugin.upmc.dash;


import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends WearableActivity{

    private static final SimpleDateFormat AMBIENT_DATE_FORMAT =
            new SimpleDateFormat("HH:mm", Locale.US);

    private BoxInsetLayout mContainerView;
    private TextView mTextView;
    private TextView mClockView;
    private static final String LC_DEBUG = "DASH";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(Constants.TAG,"MainActivty:onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setAmbientEnabled();
        mContainerView = (BoxInsetLayout) findViewById(R.id.container);
        mTextView = (TextView) findViewById(R.id.text);
        mClockView = (TextView) findViewById(R.id.clock);
        Log.d(Constants.TAG, "MainActivity: is MsgServiceRunning: " +  isMyServiceRunning(MessageService.class));
        String[] REQUIRED_PERMISSIONS = new String[]{
        Manifest.permission.BODY_SENSORS,
        Manifest.permission.RECEIVE_BOOT_COMPLETED,
        Manifest.permission.BLUETOOTH,
        Manifest.permission.VIBRATE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE};
        requestPermissions(REQUIRED_PERMISSIONS, 1);
        if(!isMyServiceRunning(MessageService.class)) {
            Intent messageService = new Intent(this, MessageService.class);
            messageService.setAction(Constants.ACTION_FIRST_RUN);
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(messageService);
                Log.d(Constants.TAG, "MainActivity: starting foreground message service");

            }
            else {
                startService(messageService);
                Log.d(Constants.TAG, "MainActivity: starting message service");

            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1: {

                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.2
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(MainActivity.this, "Permission denied. Cannot continue", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        updateDisplay();
    }

    @Override
    protected void onResume() {
        Log.d(LC_DEBUG,"MainActivity: onResume");
        super.onResume();
    }

    @Override
    protected void onPause() {
        Log.d(LC_DEBUG,"MainActivity: onPause");
        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.d(LC_DEBUG,"MainActivity: onStop");
        super.onStop();

    }

    @Override
    protected void onDestroy() {
        Log.d(LC_DEBUG,"MainActivity: onDestroy");
//        if(isMyServiceRunning(MessageService.class)) {
//            Intent msgService = new Intent(this,MessageService.class);
//            stopService(msgService);
//        }
//        if(isMyServiceRunning(SensorService.class)) {
//            Intent snsrService = new Intent(this,SensorService.class);
//            stopService(snsrService);
//        }
        super.onDestroy();


    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {

        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
        updateDisplay();
    }

    @Override
    public void onExitAmbient() {
        updateDisplay();
        super.onExitAmbient();
    }

    private void updateDisplay() {
        if (isAmbient()) {
            mContainerView.setBackgroundColor(getResources().getColor(android.R.color.black));
            mTextView.setTextColor(getResources().getColor(android.R.color.white));
            mClockView.setVisibility(View.VISIBLE);

            mClockView.setText(AMBIENT_DATE_FORMAT.format(new Date()));
        } else {
            mContainerView.setBackground(null);
            mTextView.setTextColor(getResources().getColor(android.R.color.black));
            mClockView.setVisibility(View.GONE);
        }
    }


}
