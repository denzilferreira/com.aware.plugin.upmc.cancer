package com.aware.plugin.upmc.dash.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.aware.plugin.upmc.dash.R;
import com.aware.plugin.upmc.dash.services.FitbitMessageService;
import com.aware.plugin.upmc.dash.utils.Constants;

public class EasterEgg extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_easter_egg);
    }

    public void test1(View view) {
        Intent intent = new Intent(this, FitbitMessageService.class).setAction(Constants.ACTION_TEST1);
        startService(intent);
    }

    public void test2(View view) {
        Intent intent = new Intent(this, FitbitMessageService.class).setAction(Constants.ACTION_TEST2);
        startService(intent);
    }

    public void test3(View view) {
        Intent intent = new Intent(this, FitbitMessageService.class).setAction(Constants.ACTION_TEST3);
        startService(intent);
    }

    public void test4(View view) {
        Intent intent = new Intent(this, FitbitMessageService.class).setAction(Constants.ACTION_TEST4);
        startService(intent);
    }

    public void test5(View view) {
        Intent intent = new Intent(this, FitbitMessageService.class).setAction(Constants.ACTION_TEST5);
        startService(intent);
    }
}
