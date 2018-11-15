package com.aware.plugin.upmc.dash.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

import com.aware.plugin.upmc.dash.R;
import com.aware.plugin.upmc.dash.services.FitbitMessageService;
import com.aware.plugin.upmc.dash.services.MessageService;
import com.aware.plugin.upmc.dash.utils.Constants;

public class NotificationResponseActivity extends AppCompatActivity {

    private String action = "";
    private int checkCount = 0;
    private EditText editText;
    private Button submitButton;
    private boolean isOtherChecked;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_response);
        if (getIntent().getAction() != null) {
            if (getIntent().getAction().equals(Constants.ACTION_SHOW_SNOOZE)) {
                findViewById(R.id.radio_snooze).setVisibility(View.VISIBLE);
            }
            if (getIntent().getAction().equals(Constants.ACTION_SHOW_INABILITY)) {
                showInabilityResponseDialog();
            }
        }

    }

    public void onRadioButtonClicked(View view) {
        // Is the button now checked?
        Log.d(Constants.TAG, "NotificationResponseActivity:onRadioButtonClicked " + view.getId());
        boolean checked = ((RadioButton) view).isChecked();
        // Check which radio button was clicked
        switch (view.getId()) {
            case R.id.radio_ok:
                if (checked)
                    action = Constants.ACTION_NOTIF_OK;
                break;
            case R.id.radio_snooze:
                if (checked)
                    action = Constants.ACTION_NOTIF_SNOOZE;
                break;
            case R.id.radio_no:
                if (checked)
                    action = Constants.ACTION_NOTIF_NO;
                break;
        }
    }

    public void submitResponse(View view) {


        if (action.equals(Constants.ACTION_NOTIF_NO)) {
            showInabilityResponseDialog();
        } else {
            if (action.length() != 0) {
                Log.d(Constants.TAG, "NotificationResponseActivity:submitResponse " + action);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (readDeviceType().equals(Constants.DEVICE_TYPE_ANDROID))
                    startForegroundService(new Intent(this, MessageService.class).setAction(action));
                    else startForegroundService(new Intent(this, FitbitMessageService.class).setAction(action));
                } else {
                    if (readDeviceType().equals(Constants.DEVICE_TYPE_ANDROID))
                    startService(new Intent(this, MessageService.class).setAction(action));
                    else startService(new Intent(this, FitbitMessageService.class).setAction(action));
                }
                finish();
            }
        }
    }


    public void showInabilityResponseDialog() {
        setContentView(R.layout.content_inability_response_form);
        editText = findViewById(R.id.reason_field);
        submitButton = findViewById(R.id.inability_submit);
        submitButton.setEnabled(false);
        editText.setVisibility(View.INVISIBLE);
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                submitButton.setEnabled(true);
            }
        });

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (editText.getText().length() == 0 && isOtherChecked()) {
                    Toast.makeText(view.getContext(), "Please specify a reason for other", Toast.LENGTH_SHORT).show();
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        if (readDeviceType().equals(Constants.DEVICE_TYPE_ANDROID))
                            startForegroundService(new Intent(getApplicationContext(), MessageService.class).setAction(action));
                        else
                            startForegroundService(new Intent(getApplicationContext(), FitbitMessageService.class).setAction(action));
                    } else {
                        if (readDeviceType().equals(Constants.DEVICE_TYPE_ANDROID))
                            startService(new Intent(getApplicationContext(), MessageService.class).setAction(action));
                        else
                            startService(new Intent(getApplicationContext(), FitbitMessageService.class).setAction(action));

                    }
                    finish();
                }
            }
        });


    }

    public boolean isOtherChecked() {
        return isOtherChecked;
    }

    public void setOtherChecked(boolean otherChecked) {
        isOtherChecked = otherChecked;
        if (otherChecked)
            editText.setVisibility(View.VISIBLE);
        else
            editText.setVisibility(View.GONE);
    }

    public void onCheckBoxClicked(View view) {
        boolean checked = ((CheckBox) view).isChecked();
        if (checked)
            checkCount++;
        else
            checkCount--;

        if (checkCount > 0)
            submitButton.setEnabled(true);
        else
            submitButton.setEnabled(false);

        switch (view.getId()) {
            case R.id.other_checkbox:
                setOtherChecked(checked);
                break;
        }
    }

    public String readDeviceType() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String deviceType = sharedPref.getString(Constants.PREFERENCES_KEY_DEVICE_TYPE, Constants.PREFERENCES_DEFAULT_DEVICE_TYPE);
        if (deviceType.equals(Constants.PREFERENCES_DEFAULT_DEVICE_TYPE))
            Log.d(Constants.TAG, "OnboardingActivity:writeDeviceType: " + deviceType);
        return deviceType;
    }

}
