package com.aware.plugin.upmc.cancer;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.aware.Applications;
import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.ui.PermissionsHandler;

import java.util.ArrayList;
import java.util.Calendar;

public class UPMC extends AppCompatActivity {

    private boolean debug = true;
    private static ProgressDialog dialog;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);

        //initialise framework + assign UUID
        if (!Aware.IS_CORE_RUNNING) {
            Intent aware = new Intent(this, Aware.class);
            startService(aware);
        }
    }

    private void loadSchedule() {

        dialog = new ProgressDialog(UPMC.this);

        setContentView(R.layout.settings_upmc_cancer);

        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Button saveSchedule = (Button) findViewById(R.id.save_button);

        final TimePicker morning_timer = (TimePicker) findViewById(R.id.morning_start_time);
        if (Aware.getSetting(this, Settings.PLUGIN_UPMC_CANCER_MORNING_HOUR).length() > 0) {
            morning_timer.setCurrentHour(Integer.parseInt(Aware.getSetting(this, Settings.PLUGIN_UPMC_CANCER_MORNING_HOUR)));
        } else {
            morning_timer.setCurrentHour(9);
        }
        if (Aware.getSetting(this, Settings.PLUGIN_UPMC_CANCER_MORNING_MINUTE).length() > 0) {
            morning_timer.setCurrentMinute(Integer.parseInt(Aware.getSetting(this, Settings.PLUGIN_UPMC_CANCER_MORNING_MINUTE)));
        } else {
            morning_timer.setCurrentMinute(0);
        }

        final TimePicker evening_timer = (TimePicker) findViewById(R.id.evening_start_time);
        if (Aware.getSetting(this, Settings.PLUGIN_UPMC_CANCER_EVENING_HOUR).length() > 0) {
            evening_timer.setCurrentHour(Integer.parseInt(Aware.getSetting(this, Settings.PLUGIN_UPMC_CANCER_EVENING_HOUR)));
        } else {
            evening_timer.setCurrentHour(21);
        }
        if (Aware.getSetting(this, Settings.PLUGIN_UPMC_CANCER_EVENING_MINUTE).length() > 0) {
            evening_timer.setCurrentMinute(Integer.parseInt(Aware.getSetting(this, Settings.PLUGIN_UPMC_CANCER_EVENING_MINUTE)));
        } else {
            evening_timer.setCurrentMinute(0);
        }

        saveSchedule.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.setIndeterminate(true);
                        if (Aware.isStudy(getApplicationContext())) {
                            dialog.setMessage("Please wait...");
                        } else {
                            dialog.setMessage("Joining study...");
                        }
                        dialog.setInverseBackgroundForced(true);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                dialog.show();
                            }
                        });

                        Aware.setSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_MORNING_HOUR, morning_timer.getCurrentHour().intValue());
                        Aware.setSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_MORNING_MINUTE, morning_timer.getCurrentMinute().intValue());
                        Aware.setSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_EVENING_HOUR, evening_timer.getCurrentHour().intValue());
                        Aware.setSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_EVENING_MINUTE, evening_timer.getCurrentMinute().intValue());

                        Intent applySchedule = new Intent(getApplicationContext(), Plugin.class);
                        applySchedule.putExtra("schedule", true);
                        startService(applySchedule);

                        if (!Aware.isStudy(getApplicationContext())) {
                            //UPMC sQOL
                            Aware.joinStudy(getApplicationContext(), "https://r2d2.hcii.cs.cmu.edu/aware/dashboard/index.php/webservice/index/80/7UHUVQVyXQ8x");

                            Aware.setSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_MAX_PROMPTS, 8);
                            Aware.setSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_PROMPT_INTERVAL, 30);

                            Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_SIGNIFICANT_MOTION, true);

                            Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_ESM, true);

                            Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_ACCELEROMETER, true);
                            Aware.setSetting(getApplicationContext(), Aware_Preferences.FREQUENCY_ACCELEROMETER, 200000);

                            Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_APPLICATIONS, true);
                            Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_NOTIFICATIONS, true);

                            Aware.setSetting(getApplicationContext(), com.aware.plugin.google.activity_recognition.Settings.STATUS_PLUGIN_GOOGLE_ACTIVITY_RECOGNITION, true);
                            Aware.setSetting(getApplicationContext(), com.aware.plugin.google.activity_recognition.Settings.FREQUENCY_PLUGIN_GOOGLE_ACTIVITY_RECOGNITION, 300);
                            Aware.startPlugin(getApplicationContext(), "com.aware.plugin.google.activity_recognition");

                            Aware.setSetting(getApplicationContext(), com.aware.plugin.device_usage.Settings.STATUS_PLUGIN_DEVICE_USAGE, true);
                            Aware.startPlugin(getApplicationContext(), "com.aware.plugin.device_usage");

                            Aware.setSetting(getApplicationContext(), com.aware.plugin.google.fused_location.Settings.STATUS_GOOGLE_FUSED_LOCATION, true);
                            Aware.setSetting(getApplicationContext(), com.aware.plugin.google.fused_location.Settings.FREQUENCY_GOOGLE_FUSED_LOCATION, 300);
                            Aware.setSetting(getApplicationContext(), com.aware.plugin.google.fused_location.Settings.MAX_FREQUENCY_GOOGLE_FUSED_LOCATION, 300);
                            Aware.setSetting(getApplicationContext(), com.aware.plugin.google.fused_location.Settings.ACCURACY_GOOGLE_FUSED_LOCATION, 102);
                            Aware.setSetting(getApplicationContext(), com.aware.plugin.google.fused_location.Settings.FALLBACK_LOCATION_TIMEOUT, 20);
                            Aware.setSetting(getApplicationContext(), com.aware.plugin.google.fused_location.Settings.LOCATION_SENSITIVITY, 5);
                            Aware.startPlugin(getApplicationContext(), "com.aware.plugin.google.fused_location");

                            Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_LIGHT, true);
                            Aware.setSetting(getApplicationContext(), Aware_Preferences.THRESHOLD_LIGHT, 5);

                            Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_BATTERY, true);
                            Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_CALLS, true);
                            Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_MESSAGES, true);
                            Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_SCREEN, true);

                            Aware.setSetting(getApplicationContext(), Aware_Preferences.WEBSERVICE_WIFI_ONLY, true);
                            Aware.setSetting(getApplicationContext(), Aware_Preferences.WEBSERVICE_FALLBACK_NETWORK, 6);
                            Aware.setSetting(getApplicationContext(), Aware_Preferences.FREQUENCY_WEBSERVICE, 30);
                            Aware.setSetting(getApplicationContext(), Aware_Preferences.FREQUENCY_CLEAN_OLD_DATA, 1);
                            Aware.setSetting(getApplicationContext(), Aware_Preferences.WEBSERVICE_SILENT, true);

                            Aware.startPlugin(getApplicationContext(), "com.aware.plugin.studentlife.audio_final");
                            Aware.startPlugin(getApplicationContext(), "com.aware.plugin.fitbit");

                            Aware.startPlugin(getApplicationContext(), "com.aware.plugin.upmc.cancer");

                            //Ask accessibility to be activated
                            Applications.isAccessibilityServiceActive(getApplicationContext());

                            //Ask to ignore Doze
                            Aware.isBatteryOptimizationIgnored(getApplicationContext(), getPackageName());
                        }

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                dialog.dismiss();
                                finish();
                            }
                        });
                    }
                }).start();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        ArrayList<String> REQUIRED_PERMISSIONS = new ArrayList<>();
        REQUIRED_PERMISSIONS.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        REQUIRED_PERMISSIONS.add(Manifest.permission.ACCESS_FINE_LOCATION);
        REQUIRED_PERMISSIONS.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        REQUIRED_PERMISSIONS.add(Manifest.permission.READ_CALL_LOG);
        REQUIRED_PERMISSIONS.add(Manifest.permission.READ_CONTACTS);
        REQUIRED_PERMISSIONS.add(Manifest.permission.READ_SMS);
        REQUIRED_PERMISSIONS.add(Manifest.permission.READ_PHONE_STATE);
        REQUIRED_PERMISSIONS.add(Manifest.permission.RECORD_AUDIO);

        boolean permissions_ok = true;
        for (String p : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                permissions_ok = false;
                break;
            }
        }

        if (permissions_ok) {

            Aware.setSetting(this, Aware_Preferences.DEBUG_FLAG, debug);

            if (Aware.getSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_MORNING_HOUR).length() == 0
                    || Aware.getSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_EVENING_HOUR).length() == 0) {
                loadSchedule();
                return;
            }

            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(System.currentTimeMillis());

            setContentView(R.layout.activity_upmc_cancer);
            if (getSupportActionBar() != null)
                getSupportActionBar().setDisplayHomeAsUpEnabled(false);


            final LinearLayout morning_questions = (LinearLayout) findViewById(R.id.morning_questions);
            final LinearLayout evening_questions = (LinearLayout) findViewById(R.id.evening_questions);

            final TimePicker to_bed = (TimePicker) findViewById(R.id.bed_time);
            final TimePicker from_bed = (TimePicker) findViewById(R.id.woke_time);

            final RadioGroup qos_sleep = (RadioGroup) findViewById(R.id.qos_sleep);
            final RadioGroup qos_stress = (RadioGroup) findViewById(R.id.quality_of_stress);

            final EditText most_stress = (EditText) findViewById(R.id.most_stressed_moment);
            most_stress.setText("");

            //if (cal.get(Calendar.HOUR_OF_DAY) >= Integer.parseInt(Aware.getSetting(this, Settings.PLUGIN_UPMC_CANCER_MORNING_HOUR)) && cal.get(Calendar.HOUR_OF_DAY) <= 12) {
                morning_questions.setVisibility(View.VISIBLE);
                evening_questions.setVisibility(View.GONE);

                Calendar today = Calendar.getInstance();
                today.setTimeInMillis(System.currentTimeMillis());
                today.set(Calendar.HOUR_OF_DAY, 1);
                today.set(Calendar.MINUTE, 0);
                today.set(Calendar.SECOND, 0);

                Cursor already_answered = getContentResolver().query(Provider.Symptom_Data.CONTENT_URI, null, Provider.Symptom_Data.TIMESTAMP + " > " + today.getTimeInMillis() + " AND (" + Provider.Symptom_Data.TO_BED + " != '' OR " + Provider.Symptom_Data.FROM_BED + " !='')", null, null);
                if (already_answered != null && already_answered.getCount() > 0) {
                    morning_questions.setVisibility(View.GONE);
                }
                if (already_answered != null && !already_answered.isClosed())
                    already_answered.close();
            //}

            if (cal.get(Calendar.HOUR_OF_DAY) >= Integer.parseInt(Aware.getSetting(this, Settings.PLUGIN_UPMC_CANCER_EVENING_HOUR)) && cal.get(Calendar.HOUR_OF_DAY) <= 23) {
                morning_questions.setVisibility(View.GONE);
                evening_questions.setVisibility(View.VISIBLE);

                Calendar today_2 = Calendar.getInstance();
                today.setTimeInMillis(System.currentTimeMillis());
                today.set(Calendar.HOUR_OF_DAY, 1);
                today.set(Calendar.MINUTE, 0);
                today.set(Calendar.SECOND, 0);

                Cursor already_answered_2 = getContentResolver().query(Provider.Symptom_Data.CONTENT_URI, null, Provider.Symptom_Data.TIMESTAMP + " > " + today.getTimeInMillis() + " AND (" + Provider.Symptom_Data.MOST_STRESS_LABEL + " != '' OR " + Provider.Symptom_Data.SCORE_MOST_STRESS + " !='')", null, null);
                if (already_answered != null && already_answered.getCount() > 0) {
                    evening_questions.setVisibility(View.GONE);
                }
                if (already_answered != null && !already_answered.isClosed())
                    already_answered.close();
            }

            final TextView pain_rating = (TextView) findViewById(R.id.pain_rating);
            pain_rating.setText("-1");
            SeekBar pain = (SeekBar) findViewById(R.id.rate_pain);
            pain.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                    pain_rating.setText(String.valueOf(i));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });

            final TextView fatigue_rating = (TextView) findViewById(R.id.fatigue_rating);
            fatigue_rating.setText("-1");
            SeekBar fatigue = (SeekBar) findViewById(R.id.rate_fatigue);
            fatigue.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                    fatigue_rating.setText(String.valueOf(i));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });

            final TextView disconnected_rating = (TextView) findViewById(R.id.disconnected_rating);
            disconnected_rating.setText("-1");
            SeekBar disconnected = (SeekBar) findViewById(R.id.rate_disconnected);
            disconnected.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                    disconnected_rating.setText(String.valueOf(i));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });

            final TextView concentrating_rating = (TextView) findViewById(R.id.concentrating_rating);
            concentrating_rating.setText("-1");
            SeekBar concentrating = (SeekBar) findViewById(R.id.rate_concentrating);
            concentrating.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                    concentrating_rating.setText(String.valueOf(i));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });

            final TextView sad_rating = (TextView) findViewById(R.id.sad_rating);
            sad_rating.setText("-1");
            SeekBar sad = (SeekBar) findViewById(R.id.rate_sad);
            sad.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                    sad_rating.setText(String.valueOf(i));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });

            final TextView anxious_rating = (TextView) findViewById(R.id.anxious_rating);
            anxious_rating.setText("-1");
            SeekBar anxious = (SeekBar) findViewById(R.id.rate_anxious);
            anxious.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                    anxious_rating.setText(String.valueOf(i));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });

            final TextView not_enjoying_rating = (TextView) findViewById(R.id.not_enjoying_rating);
            not_enjoying_rating.setText("-1");
            SeekBar not_enjoying = (SeekBar) findViewById(R.id.rate_not_enjoying);
            not_enjoying.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                    not_enjoying_rating.setText(String.valueOf(i));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });

            final TextView irritable_rating = (TextView) findViewById(R.id.irritable_rating);
            irritable_rating.setText("-1");
            SeekBar irritable = (SeekBar) findViewById(R.id.rate_irritable);
            irritable.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                    irritable_rating.setText(String.valueOf(i));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });

            final TextView breath_rating = (TextView) findViewById(R.id.breath_rating);
            breath_rating.setText("-1");
            SeekBar breath = (SeekBar) findViewById(R.id.rate_breath);
            breath.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                    breath_rating.setText(String.valueOf(i));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });

            final TextView numb_rating = (TextView) findViewById(R.id.numb_rating);
            numb_rating.setText("-1");
            SeekBar numb = (SeekBar) findViewById(R.id.rate_numb);
            numb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                    numb_rating.setText(String.valueOf(i));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });

            final TextView nausea_rating = (TextView) findViewById(R.id.nausea_rating);
            nausea_rating.setText("-1");
            SeekBar nausea = (SeekBar) findViewById(R.id.rate_nausea);
            nausea.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                    nausea_rating.setText(String.valueOf(i));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });

            final TextView appetite_rating = (TextView) findViewById(R.id.appetite_rating);
            appetite_rating.setText("-1");
            SeekBar appetite = (SeekBar) findViewById(R.id.rate_appetite);
            appetite.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                    appetite_rating.setText(String.valueOf(i));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });

            final TextView other_rating = (TextView) findViewById(R.id.other_rating);
            other_rating.setText("-1");
            final TextView other_label = (TextView) findViewById(R.id.lbl_other);
            other_label.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final Dialog other_labeler = new Dialog(UPMC.this);
                    other_labeler.setTitle("Can you be more specific, please?");
                    other_labeler.getWindow().setGravity(Gravity.TOP);
                    other_labeler.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);

                    LinearLayout editor = new LinearLayout(UPMC.this);
                    editor.setOrientation(LinearLayout.VERTICAL);
                    other_labeler.setContentView(editor);
                    other_labeler.show();

                    final EditText label = new EditText(UPMC.this);
                    label.setHint("Can you be more specific, please?");
                    editor.addView(label);
                    label.requestFocus();
                    other_labeler.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

                    Button confirm = new Button(UPMC.this);
                    confirm.setText("OK");
                    confirm.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (label.getText().length() == 0) label.setText("Other");
                            other_label.setText(label.getText().toString());
                            other_labeler.dismiss();
                        }
                    });

                    editor.addView(confirm);
                }
            });

            SeekBar other = (SeekBar) findViewById(R.id.rate_other);
            other.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                    other_rating.setText(String.valueOf(i));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    if (other_label.getText().equals("Other")) {
                        final Dialog other_labeler = new Dialog(UPMC.this);
                        other_labeler.getWindow().setGravity(Gravity.TOP);
                        other_labeler.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);

                        LinearLayout editor = new LinearLayout(UPMC.this);
                        editor.setOrientation(LinearLayout.VERTICAL);
                        other_labeler.setContentView(editor);
                        other_labeler.show();

                        final EditText label = new EditText(UPMC.this);
                        label.setHint("Can you be more specific, please?");
                        editor.addView(label);
                        label.requestFocus();
                        other_labeler.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

                        Button confirm = new Button(UPMC.this);
                        confirm.setText("OK");
                        confirm.setOnClickListener(new View.OnClickListener() {

                            @Override
                            public void onClick(View v) {
                                if (label.getText().length() == 0) label.setText("Other");
                                other_label.setText(label.getText().toString());
                                other_labeler.dismiss();
                            }
                        });

                        editor.addView(confirm);
                    }
                }
            });

            final Button answer_questions = (Button) findViewById(R.id.answer_questionnaire);
            answer_questions.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    ContentValues answer = new ContentValues();
                    answer.put(Provider.Symptom_Data.DEVICE_ID, Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
                    answer.put(Provider.Symptom_Data.TIMESTAMP, System.currentTimeMillis());

                    if (morning_questions != null && morning_questions.getVisibility() == View.VISIBLE) {
                        answer.put(Provider.Symptom_Data.TO_BED, (to_bed != null) ? to_bed.getCurrentHour() + "h" + to_bed.getCurrentMinute() : "");
                        answer.put(Provider.Symptom_Data.FROM_BED, (from_bed != null) ? from_bed.getCurrentHour() + "h" + from_bed.getCurrentMinute() : "");
                        answer.put(Provider.Symptom_Data.SCORE_SLEEP, (qos_sleep != null && qos_sleep.getCheckedRadioButtonId() != -1) ? (String) ((RadioButton) findViewById(qos_sleep.getCheckedRadioButtonId())).getText() : "");
                    }

                    if (evening_questions != null && evening_questions.getVisibility() == View.VISIBLE) {
                        answer.put(Provider.Symptom_Data.MOST_STRESS_LABEL, most_stress.getText().toString());
                        answer.put(Provider.Symptom_Data.SCORE_STRESS, (qos_stress != null && qos_stress.getCheckedRadioButtonId() != -1) ? (String) ((RadioButton) findViewById(qos_stress.getCheckedRadioButtonId())).getText() : "");
                    }

                    answer.put(Provider.Symptom_Data.SCORE_PAIN, pain_rating.getText().toString());
                    answer.put(Provider.Symptom_Data.SCORE_FATIGUE, fatigue_rating.getText().toString());
                    answer.put(Provider.Symptom_Data.SCORE_DISCONNECTED, disconnected_rating.getText().toString());
                    answer.put(Provider.Symptom_Data.SCORE_CONCENTRATING, concentrating_rating.getText().toString());
                    answer.put(Provider.Symptom_Data.SCORE_SAD, sad_rating.getText().toString());
                    answer.put(Provider.Symptom_Data.SCORE_ANXIOUS, anxious_rating.getText().toString());
                    answer.put(Provider.Symptom_Data.SCORE_ENJOY, not_enjoying_rating.getText().toString());
                    answer.put(Provider.Symptom_Data.SCORE_IRRITABLE, irritable_rating.getText().toString());
                    answer.put(Provider.Symptom_Data.SCORE_SHORT_BREATH, breath_rating.getText().toString());
                    answer.put(Provider.Symptom_Data.SCORE_NUMBNESS, numb_rating.getText().toString());
                    answer.put(Provider.Symptom_Data.SCORE_NAUSEA, nausea_rating.getText().toString());
                    answer.put(Provider.Symptom_Data.SCORE_APPETITE, appetite_rating.getText().toString());
                    answer.put(Provider.Symptom_Data.SCORE_OTHER, other_rating.getText().toString());
                    answer.put(Provider.Symptom_Data.OTHER_LABEL, other_label.getText().toString());

                    getContentResolver().insert(Provider.Symptom_Data.CONTENT_URI, answer);

                    Log.d("UPMC", "Answers:" + answer.toString());

                    Toast.makeText(getApplicationContext(), "Saved successfully.", Toast.LENGTH_LONG).show();
                    finish();
                }
            });

        } else {
            Intent permissions = new Intent(this, PermissionsHandler.class);
            permissions.putExtra(PermissionsHandler.EXTRA_REQUIRED_PERMISSIONS, REQUIRED_PERMISSIONS);
            permissions.putExtra(PermissionsHandler.EXTRA_REDIRECT_ACTIVITY, getPackageName() + "/" + getClass().getName());
            permissions.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(permissions);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_upmc, menu);

        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            if (item.getTitle().toString().equalsIgnoreCase("Sync") && !Aware.isStudy(getApplicationContext())) {
                item.setVisible(false);
            }
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onResume();
            return true;
        }

        String title = item.getTitle().toString();
        if (title.equalsIgnoreCase("Settings")) {
            loadSchedule();
            return true;
        }
        if (title.equalsIgnoreCase("Participant")) {

            View participantInfo = getLayoutInflater().inflate(R.layout.participant_info, null);

            TextView uuid = (TextView) participantInfo.findViewById(R.id.device_id);
            uuid.setText("UUID: " + Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));

            final EditText device_label = (EditText) participantInfo.findViewById(R.id.device_label);
            device_label.setText(Aware.getSetting(this, Aware_Preferences.DEVICE_LABEL));

            AlertDialog.Builder mBuilder = new AlertDialog.Builder(this);
            mBuilder.setTitle("UPMC Participant");
            mBuilder.setView(participantInfo);
            mBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (device_label.getText().length() > 0 && !device_label.getText().toString().equals(Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_LABEL))) {
                        Aware.setSetting(getApplicationContext(), Aware_Preferences.DEVICE_LABEL, device_label.getText().toString());
                    }
                    dialog.dismiss();
                }
            });
            mBuilder.create().show();

            return true;
        }

        if (title.equalsIgnoreCase("Sync")) {
            sendBroadcast(new Intent(Aware.ACTION_AWARE_SYNC_DATA));
            return true;
        }

        if (title.equalsIgnoreCase("Schedule")) {
            Intent scheduleDebug = new Intent(getApplicationContext(), DebugSchedules.class);
            startActivity(scheduleDebug);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
