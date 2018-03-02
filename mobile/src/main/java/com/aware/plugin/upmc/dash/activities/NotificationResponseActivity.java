package com.aware.plugin.upmc.dash.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
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

    }
    public void onRadioButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();
        // Check which radio button was clicked
        switch(view.getId()) {
            case R.id.radio_ok:
                if (checked)
                    action = Constants.OK_ACTION;
                    break;
            case R.id.radio_snooze:
                if (checked)
                    action = Constants.SNOOZE_ACTION;
                    break;
            case R.id.radio_no:
                if (checked)
                    action = Constants.NO_ACTION;
                    break;
        }
    }
    public void submitResponse(View view) {
        Log.d(Constants.TAG, "NotificationResponseActivity");
        if(action.length()!=0) {
            if(action.equals(Constants.NO_ACTION)) {
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
                        if(editText.getText().length() == 0 && isOtherChecked()) {
                            Toast.makeText(view.getContext(), "Please specify a reason for other", Toast.LENGTH_SHORT).show();
                        }
                        else {
                            finish();
                        }
                    }
                });
            }
            else {
                LocalBroadcastManager.getInstance(view.getContext()).sendBroadcast(new Intent(Constants.NOTIF_COMM).putExtra(Constants.NOTIF_KEY, action));
                Toast.makeText(view.getContext(), "Thanks!", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }


    public boolean isOtherChecked() {
        return isOtherChecked;
    }

    public void setOtherChecked(boolean otherChecked) {
        isOtherChecked = otherChecked;
        if(otherChecked)
            editText.setVisibility(View.VISIBLE);
        else
            editText.setVisibility(View.GONE);
    }

    public void onCheckBoxClocked(View view) {
        boolean checked = ((CheckBox) view).isChecked();
        if(checked)
            checkCount++;
        else
            checkCount--;

        if(checkCount>0)
            submitButton.setEnabled(true);
        else
            submitButton.setEnabled(false);

        switch (view.getId()) {
            case R.id.other_checkbox:
                setOtherChecked(checked);
                break;
        }
    }

}
