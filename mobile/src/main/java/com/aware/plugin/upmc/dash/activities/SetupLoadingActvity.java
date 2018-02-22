package com.aware.plugin.upmc.dash.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.aware.plugin.upmc.dash.R;
import com.aware.plugin.upmc.dash.utils.Constants;

public class SetupLoadingActvity extends AppCompatActivity {

    private Button button;
    private TextView textView;
    private ProgressBar progressBar;
    private BroadcastReceiver messageServiceBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.hasExtra(Constants.MESSAGE_EXTRA_KEY)) {
                button.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
                textView.setVisibility(View.VISIBLE);
                button.setText("Okay!");
                if(intent.getStringExtra(Constants.MESSAGE_EXTRA_KEY).equals(Constants.CONNECTED_WEAR)) {
                    textView.setText(Constants.LOADING_ACTIVITY_CONNECTED_MESSAGE);
                }
                else {

                    textView.setText(Constants.LOADING_ACTIVITY_FAILED_MESSAGE);

                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_loading_actvity);
        button = findViewById(R.id.setup_button);
        textView = findViewById(R.id.setup_message);
        progressBar = findViewById(R.id.setup_progressbar);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        LocalBroadcastManager.getInstance(this).registerReceiver(messageServiceBroadcastReceiver, new IntentFilter(Constants.LOADING_ACTIVITY_INTENT_FILTER));
        this.setFinishOnTouchOutside(false);


    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageServiceBroadcastReceiver);
        super.onDestroy();
    }
}
