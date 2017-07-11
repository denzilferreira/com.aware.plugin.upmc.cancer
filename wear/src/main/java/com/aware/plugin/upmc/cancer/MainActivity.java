package com.aware.plugin.upmc.cancer;


import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends WearableActivity{

    private static final SimpleDateFormat AMBIENT_DATE_FORMAT =
            new SimpleDateFormat("HH:mm", Locale.US);

    private BoxInsetLayout mContainerView;
    private TextView mTextView;
    private TextView mClockView;
    private static final String LC_DEBUG = "DASH";
    private Intent msgServiceIntent;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setAmbientEnabled();
        mContainerView = (BoxInsetLayout) findViewById(R.id.container);
        mTextView = (TextView) findViewById(R.id.text);
        mClockView = (TextView) findViewById(R.id.clock);
        msgServiceIntent = new Intent(this, MessageService.class);
        Log.d(Constants.TAG, "MainActivity: is MsgServiceRunning: " +  isMyServiceRunning(MessageService.class));
        if(!isMyServiceRunning(MessageService.class))
            startService(msgServiceIntent);
        Log.d(Constants.TAG, "MainActivity: is MsgServiceRunning: " +  isMyServiceRunning(MessageService.class));
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
//        sensorManager.unregisterListener(this);
        if(isMyServiceRunning(MessageService.class)) {
            Intent msgService = new Intent(this,MessageService.class);
            stopService(msgService);
        }
        if(isMyServiceRunning(SensorService.class)) {
            Intent snsrService = new Intent(this,SensorService.class);
            stopService(snsrService);
        }
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
