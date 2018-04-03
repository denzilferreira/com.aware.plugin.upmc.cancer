package com.aware.plugin.upmc.dash.activities;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.view.View;
import android.widget.TextView;

import com.aware.plugin.upmc.dash.R;
import com.aware.plugin.upmc.dash.services.DemoMessageService;
import com.aware.plugin.upmc.dash.utils.Constants;

public class DemoMode extends WearableActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo_mode);


        // Enables Always-on
        setAmbientEnabled();
        Intent intent = new Intent(this, DemoMessageService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startActivity(intent);
        }

        sendAction(Constants.ACTION_FIRST_RUN);
    }




    public void testButton1(View view) {
        sendAction(Constants.ACTION_SETUP_WEAR);
    }


    public void testButton2(View view) {
        sendAction(Constants.ACTION_NOTIF_OK);
    }



    public void sendAction(String action) {
        Intent intent = new Intent(this, DemoMessageService.class).setAction(action);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }
}
