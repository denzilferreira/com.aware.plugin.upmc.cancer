package com.aware.plugin.upmc.dash.activities;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.aware.plugin.upmc.dash.R;
import com.aware.plugin.upmc.dash.services.SensorService;
import com.aware.plugin.upmc.dash.utils.Constants;

public class NotificationResponse extends WearableActivity {

    private TextView mTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_response);
        mTextView =  findViewById(R.id.text);
        setAmbientEnabled();
        if(getIntent().getAction()!=null) {
            if (getIntent().getAction().equals(Constants.ACTION_SHOW_ALL)) {
                findViewById(R.id.snooze_response).setVisibility(View.VISIBLE);
            }
        }
    }

    public void notifResponse(View view) {
        switch (view.getId()) {
            case R.id.ok_response:
                sendActionToSensorService(Constants.ACTION_NOTIF_OK);
                break;

            case R.id.snooze_response:
                sendActionToSensorService(Constants.ACTION_NOTIF_SNOOZE);
                break;

            case R.id.no_response:
                sendActionToSensorService(Constants.ACTION_NOTIF_NO);
                break;
        }
        Toast.makeText(getApplicationContext(), "Thanks!", Toast.LENGTH_SHORT).show();
        finish();
    }

    public void sendActionToSensorService(String action) {
        Intent intent = new Intent(getApplicationContext(), SensorService.class).setAction(action);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);

        }
    }
}
