package com.aware.plugin.upmc.cancer;

import android.Manifest;
import android.app.ActivityManager;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.content.PermissionChecker;
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
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class UPMC extends AppCompatActivity {

    private boolean debug = false;
    private static ProgressDialog dialog;
    private boolean firstRun = true;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("DASH", "UPMC:onDestroy");
        if(isMyServiceRunning(MessageService.class)) {
            stopService(new Intent(this, MessageService.class));
            Log.d("DASH", "Stopped Message Service");
        }
        else
            Log.d("DASH", "Message Service is not running");

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mNotifBroadcastReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(wearStatusReceiver);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent aware = new Intent(this, Aware.class);
        startService(aware);
        Log.d("DASH","UPMC:onCreate");

        if(!isMyServiceRunning(MessageService.class)) {
            startService(new Intent(this, MessageService.class));
            Log.d("DASH", "Started Message Service");
        }
        else
            Log.d("DASH", "Message Service already running");

        LocalBroadcastManager.getInstance(this).registerReceiver(wearStatusReceiver, new IntentFilter(Constants.WEAR_STATUS_INTENT_FILTER));
        LocalBroadcastManager.getInstance(this).registerReceiver(mNotifBroadcastReceiver, new IntentFilter(Constants.NOTIFICATION_MESSAGE_INTENT_FILTER));

    }

    private BroadcastReceiver wearStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(Constants.TAG, "UPMC:BR:wearStatusMsgReceived" + intent.getStringExtra(Constants.COMM_KEY));
        }
    };

    private BroadcastReceiver mNotifBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(Constants.TAG, "UPMC:BR:wearStopMessageReceived, killing application");
            finish();

        }
    };




    private boolean isMyServiceRunning(Class<?> serviceClass) {

        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }



    private void loadSchedule() {

        dialog = new ProgressDialog(UPMC.this);

        setContentView(R.layout.settings_upmc_dash);

        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

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

                        Intent applySchedule = new Intent(getApplicationContext(), Plugin.class);
                        applySchedule.putExtra("schedule", true);
                        startService(applySchedule);

                        if (!Aware.isStudy(getApplicationContext())) {
                            //UPMC Dash
                            Aware.joinStudy(getApplicationContext(), "https://r2d2.hcii.cs.cmu.edu/aware/dashboard/index.php/webservice/index/81/Rhi4Q8PqLASf");

                            Aware.startPlugin(getApplicationContext(), "com.aware.plugin.upmc.cancer");
                            Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_SIGNIFICANT_MOTION, true);

                            Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_ACCELEROMETER, true);
                            Aware.setSetting(getApplicationContext(), Aware_Preferences.FREQUENCY_ACCELEROMETER, 200000);
                            Aware.setSetting(getApplicationContext(), com.aware.plugin.google.activity_recognition.Settings.STATUS_PLUGIN_GOOGLE_ACTIVITY_RECOGNITION, true);
                            Aware.setSetting(getApplicationContext(), com.aware.plugin.google.activity_recognition.Settings.FREQUENCY_PLUGIN_GOOGLE_ACTIVITY_RECOGNITION, 300);
                            Aware.startPlugin(getApplicationContext(), "com.aware.plugin.google.activity_recognition");

                            Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_ESM, true);

                            Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_LIGHT, true);
                            Aware.setSetting(getApplicationContext(), Aware_Preferences.THRESHOLD_LIGHT, 5);

                            Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_BATTERY, true);
                            Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_SCREEN, true);

                            Aware.setSetting(getApplicationContext(), Aware_Preferences.WEBSERVICE_WIFI_ONLY, true);
                            Aware.setSetting(getApplicationContext(), Aware_Preferences.FREQUENCY_WEBSERVICE, 360);
                            Aware.setSetting(getApplicationContext(), Aware_Preferences.FREQUENCY_CLEAN_OLD_DATA, 1);
                            Aware.setSetting(getApplicationContext(), Aware_Preferences.WEBSERVICE_SILENT, true);

                            //Aware.startPlugin(getApplicationContext(), "com.aware.plugin.fitbit");

                            //Ask accessibility to be activated
                            Applications.isAccessibilityServiceActive(getApplicationContext());
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
        final Handler handler = new Handler();

        if(firstRun) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(Constants.LOCAL_MESSAGE_INTENT_FILTER);
                    // starting step count after 2 seconds
                    intent.putExtra(Constants.COMM_KEY, Constants.STATUS_WEAR);
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
                    Log.d(Constants.TAG, "UPMC:onResume:Handler");
                }
            }, 2000);
            firstRun = false;
        }



        ArrayList<String> REQUIRED_PERMISSIONS = new ArrayList<>();
        REQUIRED_PERMISSIONS.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);

        boolean permissions_ok = true;
        for (String p : REQUIRED_PERMISSIONS) {
            if (PermissionChecker.checkSelfPermission(this, p) != PermissionChecker.PERMISSION_GRANTED) {
                permissions_ok = false;
                break;
            }
        }

        if (permissions_ok) {

            Aware.setSetting(this, Aware_Preferences.DEBUG_FLAG, debug);

            //NOTE: needed for demo to participants
            Aware.setSetting(this, Aware_Preferences.STATUS_ESM, true);
            Aware.startESM(this);

            if (Aware.getSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_MORNING_HOUR).length() == 0) {
                loadSchedule();
                return;
            }

            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(System.currentTimeMillis());

            setContentView(R.layout.activity_upmc_dash);
            if (getSupportActionBar() != null)
                getSupportActionBar().setDisplayHomeAsUpEnabled(false);


            final LinearLayout morning_questions = (LinearLayout) findViewById(R.id.morning_questions);

            final TimePicker to_bed = (TimePicker) findViewById(R.id.bed_time);
            final TimePicker from_bed = (TimePicker) findViewById(R.id.woke_time);

            final RadioGroup qos_sleep = (RadioGroup) findViewById(R.id.qos_sleep);

            if (cal.get(Calendar.HOUR_OF_DAY) >= Integer.parseInt(Aware.getSetting(this, Settings.PLUGIN_UPMC_CANCER_MORNING_HOUR)) && cal.get(Calendar.HOUR_OF_DAY) <= 12) {
                morning_questions.setVisibility(View.VISIBLE);

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

            final TextView sleep_disturb_rating = (TextView) findViewById(R.id.sleep_disturbance_rating);
            sleep_disturb_rating.setText("-1");
            SeekBar sleep_disturb = (SeekBar) findViewById(R.id.rate_sleep_disturbance);
            sleep_disturb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                    sleep_disturb_rating.setText(String.valueOf(i));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });

            final TextView diarrhea_rating = (TextView) findViewById(R.id.diarrhea_rating);
            diarrhea_rating.setText("-1");
            SeekBar diarrhea = (SeekBar) findViewById(R.id.rate_diarrhea);
            diarrhea.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                    diarrhea_rating.setText(String.valueOf(i));
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

                    answer.put(Provider.Symptom_Data.SCORE_PAIN, pain_rating.getText().toString());
                    answer.put(Provider.Symptom_Data.SCORE_FATIGUE, fatigue_rating.getText().toString());
                    answer.put(Provider.Symptom_Data.SCORE_SLEEP_DISTURBANCE, sleep_disturb_rating.getText().toString());
                    answer.put(Provider.Symptom_Data.SCORE_CONCENTRATING, concentrating_rating.getText().toString());
                    answer.put(Provider.Symptom_Data.SCORE_SAD, sad_rating.getText().toString());
                    answer.put(Provider.Symptom_Data.SCORE_ANXIOUS, anxious_rating.getText().toString());
                    answer.put(Provider.Symptom_Data.SCORE_SHORT_BREATH, breath_rating.getText().toString());
                    answer.put(Provider.Symptom_Data.SCORE_NUMBNESS, numb_rating.getText().toString());
                    answer.put(Provider.Symptom_Data.SCORE_NAUSEA, nausea_rating.getText().toString());
                    answer.put(Provider.Symptom_Data.SCORE_DIARRHEA, diarrhea_rating.getText().toString());
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
            if (item.getTitle().toString().equalsIgnoreCase("Sync") && ! Aware.isStudy(getApplicationContext())) {
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

        if (title.equalsIgnoreCase("Demo Fitbit")) {
            Intent walking = new Intent(this, UPMC_Motivation.class);
            walking.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(walking);

            return true;
        }

        if (title.equalsIgnoreCase("Sync")) {
            sendBroadcast(new Intent(Aware.ACTION_AWARE_SYNC_DATA));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
