package com.aware.plugin.upmc.dash.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.aware.plugin.upmc.dash.R;
import com.aware.plugin.upmc.dash.services.FitbitMessageService;
import com.aware.plugin.upmc.dash.utils.Constants;

public class NotificationResponseActivity extends AppCompatActivity {

    public int CHECKBOX_IDS[] = {R.id.busy_checkbox, R.id.pain_checkbox, R.id.nausea_checkbox,
            R.id.tired_checkbox};
    public int OHTER_CHECKBOX_ID = R.id.other_checkbox;
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

    public int mapSelection(View view) {
        switch (view.getId()) {
            case R.id.busy_checkbox:
                return 0;
            case R.id.pain_checkbox:
                return 1;
            case R.id.nausea_checkbox:
                return 2;
            case R.id.tired_checkbox:
                return 3;
            case R.id.other_checkbox:
                return 4;
            default:
                return -1;
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
                    startForegroundService(new Intent(this, FitbitMessageService.class).setAction(
                            action));

                } else {
                    startService(new Intent(this, FitbitMessageService.class).setAction(action));

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
        submitButton.setOnClickListener(view -> {
            if (editText.getText().length() == 0 && isOtherChecked()) {
                Toast.makeText(view.getContext(),
                        "Please specify a reason for other",
                        Toast.LENGTH_SHORT).show();
            } else {
                StringBuilder sb = new StringBuilder();
                for (int id : CHECKBOX_IDS) {
                    CheckBox box = findViewById(id);
                    if (box.isChecked())
                        sb.append(1);
                    else
                        sb.append(0);
                }
                CheckBox other_box = findViewById(OHTER_CHECKBOX_ID);
                if (other_box.isChecked()) {
                    sb.append(1);
                    sb.append(editText.getText().toString());
                } else
                    sb.append(0);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    startForegroundService(new Intent(getApplicationContext(),
                            FitbitMessageService.class).setAction(action)
                            .putExtra(Constants.NOTIF_RESPONSE_EXTRA_KEY, sb.toString()));
                else
                    startService(new Intent(getApplicationContext(),
                            FitbitMessageService.class).setAction(action)
                            .putExtra(Constants.NOTIF_RESPONSE_EXTRA_KEY, sb.toString()));
                finish();
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
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(
                getApplicationContext());
        String deviceType = sharedPref.getString(Constants.PREFERENCES_KEY_DEVICE_TYPE,
                Constants.PREFERENCES_DEFAULT_DEVICE_TYPE);
        if (deviceType.equals(Constants.PREFERENCES_DEFAULT_DEVICE_TYPE))
            Log.d(Constants.TAG, "OnboardingActivity:writeDeviceType: " + deviceType);
        return deviceType;
    }

}
