package com.aware.plugin.upmc.cancer;

import android.content.ContentValues;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.utils.Scheduler;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;

/**
 * Created by denzil on 07/01/2017.
 */

public class UPMC_Motivation extends AppCompatActivity {

    JSONObject user_answers = new JSONObject();
    EditText other_reason, other_symptom;
    int question_type = 0;

    @Override
    protected void onResume() {
        super.onResume();
        setContentView(R.layout.mobility_trigger);
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        if (getIntent().getExtras() != null) {
            question_type = getIntent().getExtras().getInt("question_type");
        }

        TextView header = (TextView) findViewById(R.id.mobility_question);
        switch (question_type) {
            case 1:
                header.setText("Ready for a two minute walk?");
                try {
                    user_answers.put("trigger", "< 50 in past 3h, all symptoms < 7");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case 2:
                header.setText("Ready for a quick walk?");
                try {
                    user_answers.put("trigger", "< 50 in past 5h, any symptoms >= 7");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case 0:
                header.setText("Walking prompt demo");
                try {
                    user_answers.put("trigger", "demo");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
        }

        final LinearLayout rationale_container = (LinearLayout) findViewById(R.id.rationale);

        RadioGroup is_walking = (RadioGroup) findViewById(R.id.walking);
        final RadioButton no = (RadioButton) findViewById(R.id.walking_no);
        no.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    user_answers.put("is_walking", false);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        final RadioButton yes = (RadioButton) findViewById(R.id.walking_yes);
        yes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    user_answers.put("is_walking", true);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        //Toggle extra questions
        is_walking.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == no.getId()) {
                    rationale_container.setVisibility(View.VISIBLE);
                }
                if (checkedId == yes.getId()) {
                    rationale_container.setVisibility(View.GONE);
                }
            }
        });

        final CheckBox is_busy = (CheckBox) findViewById(R.id.busy);
        is_busy.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                try {
                    user_answers.put("is_busy", isChecked);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        final CheckBox is_pain = (CheckBox) findViewById(R.id.pain);
        is_pain.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                try {
                    user_answers.put("is_pain", isChecked);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        final CheckBox is_nausea = (CheckBox) findViewById(R.id.nausea);
        is_nausea.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                try {
                    user_answers.put("is_nausea", isChecked);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        final CheckBox is_other_symptom = (CheckBox) findViewById(R.id.symptom);
        is_other_symptom.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                try {
                    user_answers.put("is_other_symptom", isChecked);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        other_symptom = (EditText) findViewById(R.id.other_symptom);

        final CheckBox is_other_reason = (CheckBox) findViewById(R.id.reason);
        is_other_reason.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                try {
                    user_answers.put("is_other_reason", isChecked);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        other_reason = (EditText) findViewById(R.id.other_reason);

        final CheckBox already_walked = (CheckBox) findViewById(R.id.already_walked);
        already_walked.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                try {
                    user_answers.put("already_walked", isChecked);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        final CheckBox snoozed = (CheckBox) findViewById(R.id.snoozed);
        snoozed.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                try {
                    user_answers.put("snoozed", isChecked);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        Button dismiss = (Button) findViewById(R.id.motivation_dismiss);
        dismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        try {
            if (user_answers.has("is_other_symptom") && user_answers.getBoolean("is_other_symptom")) {
                if (other_symptom.getText().length() > 0)
                    user_answers.put("other_symptom", other_symptom.getText().toString());
            }
            if (user_answers.has("is_other_reason") && user_answers.getBoolean("is_other_reason")) {
                if (other_reason.getText().length() > 0)
                    user_answers.put("other_reason", other_reason.getText().toString());
            }

            if (user_answers.has("snoozed") && user_answers.getBoolean("snoozed")) {
                Calendar snoozeTime = Calendar.getInstance();
                snoozeTime.setTimeInMillis(System.currentTimeMillis());
                snoozeTime.add(Calendar.MINUTE, 15); //move forward 15 minutes

                Scheduler.Schedule snoozed = new Scheduler.Schedule("upmc_motivation_snooze");
                snoozed.setTimer(snoozeTime)
                        .setActionType(Scheduler.ACTION_TYPE_ACTIVITY)
                        .setActionClass(getPackageName() + "/" + getClass().getName())
                        .addActionExtra("question_type", question_type);

                Scheduler.saveSchedule(getApplicationContext(), snoozed);

                //Apply schedule
                Aware.startScheduler(this);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        ContentValues mobility_answer = new ContentValues();
        mobility_answer.put(Provider.Motivational_Data.TIMESTAMP, System.currentTimeMillis());
        mobility_answer.put(Provider.Motivational_Data.DEVICE_ID, Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
        mobility_answer.put(Provider.Motivational_Data.RATIONALE, user_answers.toString());
        getContentResolver().insert(Provider.Motivational_Data.CONTENT_URI, mobility_answer);

        if (Aware.DEBUG)
            Log.d(Aware.TAG, "Motivation answer: " + mobility_answer.toString());
    }
}
