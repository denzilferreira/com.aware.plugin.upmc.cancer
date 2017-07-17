package com.aware.plugin.upmc.cancer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private String NODE_ID;
    private Button startButton;
    private Button stopButton;
    private GoogleApiClient mGoogleApiClient;
    private String TAG = "DASH";
    public static final String CAPABILITY_WEAR_APP = "upmcdash-wearapp";
    private SensorManager mSensorManager;
    private Sensor stepCounter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(Constants.TAG, "MainActivity:onCreate");
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        startButton = (Button) findViewById(R.id.start);
        stopButton = (Button) findViewById(R.id.stop);
        startButton.setEnabled(false);
        stopButton.setEnabled(true);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });


        startService(new Intent(this, MessageService.class));

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Constants.LOCAL_MESSAGE_INTENT_FILTER);
                intent.putExtra("message", "Get Schwifty");
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
            }
        });

        LocalBroadcastManager.getInstance(this).registerReceiver(wearStatusReceiver, new IntentFilter(Constants.WEAR_STATUS_INTENT_FILTER));

        mSensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
        stepCounter = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        Log.d(Constants.TAG, "Step counter size : " + mSensorManager.getSensorList(Sensor.TYPE_STEP_COUNTER).size());
        Log.d(Constants.TAG, "Default step counter name : " + mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER));


    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(Constants.TAG, "MainActivity:onResume");
        if(mSensorManager!=null) {
            if(stepCounter!=null) {
                mSensorManager.registerListener(this, stepCounter, SensorManager.SENSOR_DELAY_FASTEST, 500);
            }
            else
                Log.d(Constants.TAG, "Device is missing a step counter");
        }
        else
            Log.d(Constants.TAG, "Sensor manager is null");
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(mSensorManager!=null) {
            mSensorManager.unregisterListener(this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    private BroadcastReceiver wearStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(Constants.TAG, "wearStatusMsgReceived" + intent.getStringExtra("status"));
        }
    };

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Log.d(Constants.TAG, "onSensorChanged");
        if(sensorEvent.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            Log.d(Constants.TAG, "StepCounter " + String.valueOf(sensorEvent.values[0]));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
