package com.aware.plugin.upmc.dash.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
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
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import com.aware.Applications;
import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.plugin.upmc.dash.R;
import com.aware.plugin.upmc.dash.services.MessageService;
import com.aware.plugin.upmc.dash.settings.Settings;
import com.aware.plugin.upmc.dash.utils.Constants;
import com.aware.ui.PermissionsHandler;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class UPMC extends AppCompatActivity {
    public boolean isWatchAround = false;
    public boolean WEARLESS_DEBUG = true;
    public boolean STUDYLESS_DEBUG = true;
    public BroadcastReceiver vicinityCheckBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(Constants.TAG, "UPMC:: vicinitycheck received from MessageService: " + intent.getIntExtra(Constants.VICINITY_RESULT_KEY, -1));
            if (intent.hasExtra(Constants.VICINITY_RESULT_KEY)) {
                if ((intent.getIntExtra(Constants.VICINITY_RESULT_KEY, -1) == Constants.WEAR_VICINITY_CHECK_FAILED)
                        || (intent.getIntExtra(Constants.VICINITY_RESULT_KEY, -1) == Constants.WEAR_NOT_IN_RANGE)) {
                    setWatchAround(false);
                } else if (intent.getIntExtra(Constants.VICINITY_RESULT_KEY, -1) == Constants.WEAR_IN_RANGE) {
                    setWatchAround(true);
                }
            }
        }
    };
    private boolean permissions_ok = true;
    private List<Integer> ratingList;
    private BroadcastReceiver mNotifBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra(Constants.COMM_KEY_UPMC)) {
                Log.d(Constants.TAG, "UPMC:BR:wearStopMessageReceived, killing application");
                finish();
            }
        }
    };
    private TimePicker morning_timer;
    private TimePicker night_timer;
    private ProgressBar progressBar;
    private JoinedStudy joinedObserver = new JoinedStudy();

    public boolean isWatchAround() {
        return isWatchAround;
    }

    public void setWatchAround(boolean watchAround) {
        isWatchAround = watchAround;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this)
                .unregisterReceiver(mNotifBroadcastReceiver);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LocalBroadcastManager.getInstance(this)
                .registerReceiver(mNotifBroadcastReceiver, new IntentFilter(Constants.NOTIFICATION_MESSAGE_INTENT_FILTER));
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        assert manager != null;
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void loadSchedule(final boolean firstRun) {
        setContentView(R.layout.settings_upmc_dash);

        progressBar = findViewById(R.id.progress_bar_schedule);

        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        final Button saveSchedule = findViewById(R.id.save_button);

        morning_timer = findViewById(R.id.morning_start_time);
        night_timer = findViewById(R.id.night_sleep_time);
        Log.d(Constants.TAG, "UPMC:loadSchedule:firstRun" + firstRun);

        if (firstRun) {
            saveSchedule.setText("Join Study");
            IntentFilter filter = new IntentFilter(Aware.ACTION_JOINED_STUDY);
            if(!STUDYLESS_DEBUG)
                registerReceiver(joinedObserver, filter);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                morning_timer.setHour(9);
                morning_timer.setMinute(0);
                night_timer.setHour(21);
                night_timer.setMinute(0);
            } else {
                morning_timer.setCurrentHour(9);
                morning_timer.setCurrentMinute(0);
                night_timer.setCurrentHour(21);
                night_timer.setCurrentMinute(0);
            }
            saveSchedule.setOnClickListener(new View.OnClickListener() {
                @SuppressLint("SetTextI18n")
                @Override
                public void onClick(View v) {
                    progressBar.setVisibility(View.VISIBLE);
                    saveSchedule.setEnabled(false);
                    saveSchedule.setText("Saving Schedule....");
                    int morningHour;
                    int morningMinute;
                    int nightHour;
                    int nightMinute;
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        morningHour = morning_timer.getHour();
                        morningMinute = morning_timer.getMinute();
                        nightHour = night_timer.getHour();
                        nightMinute = night_timer.getMinute();
                    }
                    else {
                        morningHour = morning_timer.getCurrentHour();
                        morningMinute = morning_timer.getCurrentMinute();
                        nightHour = night_timer.getCurrentHour();
                        nightMinute = night_timer.getCurrentMinute();
                    }
                    Aware.setSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_MORNING_HOUR, ""+ morningHour);
                    Aware.setSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_MORNING_MINUTE, ""+ morningMinute);
                    Aware.setSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_NIGHT_HOUR, ""+ nightHour);
                    Aware.setSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_NIGHT_MINUTE, ""+ nightMinute);
                    if(!STUDYLESS_DEBUG) {
                        if (!Aware.isStudy(getApplicationContext())) {
                            //UPMC Dash
                             Aware.joinStudy(getApplicationContext(), "https://r2d2.hcii.cs.cmu.edu/aware/dashboard/index.php/webservice/index/81/Rhi4Q8PqLASf");
                            //Aware.joinStudy(getApplicationContext(), "https://api.awareframework.com/index.php/webservice/index/1625/1RNJ8hhucJ9M");
                        }
                    }
                    else {
                        sendMessageServiceAction(Constants.ACTION_FIRST_RUN);
                    }

                    // alarm
                    finish();

                }
            });
        } else {
            int morning_hour = Integer.parseInt(Aware.getSetting(this, Settings.PLUGIN_UPMC_CANCER_MORNING_HOUR));
            int morning_minute = Integer.parseInt(Aware.getSetting(this, Settings.PLUGIN_UPMC_CANCER_MORNING_MINUTE));
            int night_hour = Integer.parseInt(Aware.getSetting(this, Settings.PLUGIN_UPMC_CANCER_NIGHT_HOUR));
            int night_minute = Integer.parseInt(Aware.getSetting(this, Settings.PLUGIN_UPMC_CANCER_NIGHT_MINUTE));
            Log.d(Constants.TAG, "UPMC:loadSchedule:savedTimes:" + morning_hour + "" + morning_minute + "" + night_hour + "" + night_minute);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                morning_timer.setHour(morning_hour);
                morning_timer.setMinute(morning_minute);
                night_timer.setHour(night_hour);
                night_timer.setMinute(night_minute);
            } else {
                morning_timer.setCurrentHour(morning_hour);
                morning_timer.setCurrentMinute(morning_minute);
                night_timer.setCurrentHour(night_hour);
                night_timer.setCurrentMinute(night_minute);
            }
            saveSchedule.setOnClickListener(new View.OnClickListener() {
                @SuppressLint("SetTextI18n")
                @Override
                public void onClick(View v) {
                    progressBar.setVisibility(View.VISIBLE);
                    saveSchedule.setEnabled(false);
                    saveSchedule.setText("Saving Schedule....");
                    int morningHour;
                    int morningMinute;
                    int nightHour;
                    int nightMinute;
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        morningHour = morning_timer.getHour();
                        morningMinute = morning_timer.getMinute();
                        nightHour = night_timer.getHour();
                        nightMinute = night_timer.getMinute();
                    }
                    else {
                        morningHour = morning_timer.getCurrentHour();
                        morningMinute = morning_timer.getCurrentMinute();
                        nightHour = night_timer.getCurrentHour();
                        nightMinute = night_timer.getCurrentMinute();
                    }
                    Aware.setSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_MORNING_HOUR, ""+ morningHour);
                    Aware.setSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_MORNING_MINUTE, ""+ morningMinute);
                    Aware.setSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_NIGHT_HOUR, ""+ nightHour);
                    Aware.setSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_NIGHT_MINUTE, ""+ nightMinute);
                    Log.d(Constants.TAG, "UPMC: Sending Settings Changed Broadcast");
                    sendMessageServiceAction(Constants.ACTION_SETTINGS_CHANGED);
                    finish();
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        ArrayList<String> REQUIRED_PERMISSIONS = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            REQUIRED_PERMISSIONS.add(Manifest.permission.BODY_SENSORS);
        }
        REQUIRED_PERMISSIONS.add(Manifest.permission.VIBRATE);
        REQUIRED_PERMISSIONS.add(Manifest.permission.BLUETOOTH);
        REQUIRED_PERMISSIONS.add(Manifest.permission.BLUETOOTH_ADMIN);
        REQUIRED_PERMISSIONS.add(Manifest.permission.READ_CALL_LOG);
        REQUIRED_PERMISSIONS.add(Manifest.permission.READ_CONTACTS);
        REQUIRED_PERMISSIONS.add(Manifest.permission.READ_SMS);
        REQUIRED_PERMISSIONS.add(Manifest.permission.READ_PHONE_STATE);
        REQUIRED_PERMISSIONS.add(Manifest.permission.ACCESS_WIFI_STATE);
        REQUIRED_PERMISSIONS.add(Manifest.permission.RECORD_AUDIO);
        REQUIRED_PERMISSIONS.add(Manifest.permission.WAKE_LOCK);
        REQUIRED_PERMISSIONS.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        //these are needed for the sync adapter to work...
        REQUIRED_PERMISSIONS.add(Manifest.permission.GET_ACCOUNTS);
        REQUIRED_PERMISSIONS.add(Manifest.permission.WRITE_SYNC_SETTINGS);
        REQUIRED_PERMISSIONS.add(Manifest.permission.READ_SYNC_SETTINGS);
        REQUIRED_PERMISSIONS.add(Manifest.permission.READ_SYNC_STATS);

        for (String p : REQUIRED_PERMISSIONS) {
            if (PermissionChecker.checkSelfPermission(this, p) != PermissionChecker.PERMISSION_GRANTED) {
                permissions_ok = false;
                break;
            }
        }

        if (!permissions_ok) {
            Intent permissions = new Intent(this, PermissionsHandler.class);
            permissions.putExtra(PermissionsHandler.EXTRA_REQUIRED_PERMISSIONS, REQUIRED_PERMISSIONS);
            permissions.putExtra(PermissionsHandler.EXTRA_REDIRECT_ACTIVITY, getPackageName() + "/" + getClass().getName());
            permissions.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(permissions);
            finish();
        } else {

            if (!Aware.IS_CORE_RUNNING) {
                //This initialises the core framework, assigns Device ID if it doesn't exist yet, etc.
                Intent aware = new Intent(getApplicationContext(), Aware.class);
                startService(aware);
            }

            Aware.setSetting(this, Aware_Preferences.DEBUG_FLAG, false);
            //NOTE: needed for demo to participants
            Aware.setSetting(this, Aware_Preferences.STATUS_ESM, true);
            //Aware.startESM(this);
            if (Aware.getSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_MORNING_HOUR).length() == 0) {
                // When app is run for the first time
                loadSchedule(true);
            } else {

                if(!isMyServiceRunning(MessageService.class)) {
                    sendMessageServiceAction(Constants.ACTION_FIRST_RUN);
                }
                // check if watch is around. If you do not have a Android Wear device and you'd like debug, set WIRELESS_DEBUG to true
                if (WEARLESS_DEBUG) {
                    showSymptomSurvey();
                } else {
                    // When the app is opened later
                    sendMessageServiceAction(Constants.ACTION_VICINITY);
                    LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(vicinityCheckBroadcastReceiver, new IntentFilter(Constants.VICINITY_CHECK_INTENT_FILTER));
                    setContentView(R.layout.activity_upmc_loading);
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(vicinityCheckBroadcastReceiver);
                            if (!isWatchAround()) {
                                Toast.makeText(getApplicationContext(), "Wear is not in range. Please reinitiate setup", Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                showSymptomSurvey();
                            }
                        }
                    }, 10000);
                }
            }
        }
    }


    public void sendMessageServiceAction(String action) {
        Intent intent = new Intent(this, MessageService.class).setAction(action);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    public void showSymptomSurvey() {
        ratingList = new ArrayList<>(12);
        for (int i = 0; i < 12; i++) {
            ratingList.add(i, -1);
        }
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(System.currentTimeMillis());
        setContentView(R.layout.activity_upmc_dash);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        final LinearLayout morning_questions = findViewById(R.id.morning_questions);
        final TimePicker to_bed = findViewById(R.id.bed_time);
        final TimePicker from_bed = findViewById(R.id.woke_time);
        final RadioGroup qos_sleep = findViewById(R.id.qos_sleep);
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
        final TextView pain_rating = findViewById(R.id.pain_rating);
        pain_rating.setText("?");
        SeekBar pain = findViewById(R.id.rate_pain);
        pain.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                pain_rating.setText(String.valueOf(i));
                ratingList.set(0, i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        final TextView fatigue_rating = findViewById(R.id.fatigue_rating);
        fatigue_rating.setText("?");
        SeekBar fatigue = findViewById(R.id.rate_fatigue);
        fatigue.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                fatigue_rating.setText(String.valueOf(i));
                ratingList.set(1, i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        final TextView concentrating_rating = findViewById(R.id.concentrating_rating);
        concentrating_rating.setText("?");
        SeekBar concentrating = findViewById(R.id.rate_concentrating);
        concentrating.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                concentrating_rating.setText(String.valueOf(i));
                ratingList.set(2, i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        final TextView sad_rating = findViewById(R.id.sad_rating);
        sad_rating.setText("?");
        SeekBar sad = findViewById(R.id.rate_sad);
        sad.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                sad_rating.setText(String.valueOf(i));
                ratingList.set(3, i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        final TextView anxious_rating = findViewById(R.id.anxious_rating);
        anxious_rating.setText("?");
        SeekBar anxious = findViewById(R.id.rate_anxious);
        anxious.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                anxious_rating.setText(String.valueOf(i));
                ratingList.set(4, i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        final TextView breath_rating = findViewById(R.id.breath_rating);
        breath_rating.setText("?");
        SeekBar breath = findViewById(R.id.rate_breath);
        breath.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                breath_rating.setText(String.valueOf(i));
                ratingList.set(5, i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        final TextView numb_rating = findViewById(R.id.numb_rating);
        numb_rating.setText("?");
        SeekBar numb = findViewById(R.id.rate_numb);
        numb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                numb_rating.setText(String.valueOf(i));
                ratingList.set(6, i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        final TextView nausea_rating = findViewById(R.id.nausea_rating);
        nausea_rating.setText("?");
        SeekBar nausea = findViewById(R.id.rate_nausea);
        nausea.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                nausea_rating.setText(String.valueOf(i));
                ratingList.set(7, i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        final TextView sleep_disturb_rating = findViewById(R.id.sleep_disturbance_rating);
        sleep_disturb_rating.setText("?");
        SeekBar sleep_disturb = findViewById(R.id.rate_sleep_disturbance);
        sleep_disturb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                sleep_disturb_rating.setText(String.valueOf(i));
                ratingList.set(8, i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        final TextView diarrhea_rating = findViewById(R.id.diarrhea_rating);
        diarrhea_rating.setText("?");
        SeekBar diarrhea = findViewById(R.id.rate_diarrhea);
        diarrhea.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                diarrhea_rating.setText(String.valueOf(i));
                ratingList.set(9, i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        final TextView other_rating = findViewById(R.id.other_rating);
        other_rating.setText("?");
        final TextView other_label = findViewById(R.id.lbl_other);
        other_label.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
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
                    @SuppressLint("SetTextI18n")
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

        SeekBar other = findViewById(R.id.rate_other);
        other.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                other_rating.setText(String.valueOf(i));
                ratingList.set(10, i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @SuppressLint("SetTextI18n")
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

                        @SuppressLint("SetTextI18n")
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

        final Button answer_questions = findViewById(R.id.answer_questionnaire);
        final ProgressBar progressBar = findViewById(R.id.progress_bar_syms);
        answer_questions.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View v) {
                answer_questions.setEnabled(false);
                answer_questions.setText("Saving answers..");
                Log.d(Constants.TAG, "UPMC:Questionnaire");
                progressBar.setVisibility(View.VISIBLE);
                ContentValues answer = new ContentValues();
                answer.put(Provider.Symptom_Data.DEVICE_ID, Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
                answer.put(Provider.Symptom_Data.TIMESTAMP, System.currentTimeMillis());
                if (morning_questions != null && morning_questions.getVisibility() == View.VISIBLE) {
                    answer.put(Provider.Symptom_Data.TO_BED, (to_bed != null) ? to_bed.getCurrentHour() + "h" + to_bed.getCurrentMinute() : "");
                    answer.put(Provider.Symptom_Data.FROM_BED, (from_bed != null) ? from_bed.getCurrentHour() + "h" + from_bed.getCurrentMinute() : "");
                    answer.put(Provider.Symptom_Data.SCORE_SLEEP, (qos_sleep != null && qos_sleep.getCheckedRadioButtonId() != -1) ? (String) ((RadioButton) findViewById(qos_sleep.getCheckedRadioButtonId())).getText() : "");
                }
                answer.put(Provider.Symptom_Data.SCORE_PAIN, parseAnswer(pain_rating.getText().toString()));
                answer.put(Provider.Symptom_Data.SCORE_FATIGUE, parseAnswer(fatigue_rating.getText().toString()));
                answer.put(Provider.Symptom_Data.SCORE_SLEEP_DISTURBANCE, parseAnswer(sleep_disturb_rating.getText().toString()));
                answer.put(Provider.Symptom_Data.SCORE_CONCENTRATING, parseAnswer(concentrating_rating.getText().toString()));
                answer.put(Provider.Symptom_Data.SCORE_SAD, parseAnswer(sad_rating.getText().toString()));
                answer.put(Provider.Symptom_Data.SCORE_ANXIOUS, parseAnswer(anxious_rating.getText().toString()));
                answer.put(Provider.Symptom_Data.SCORE_SHORT_BREATH, parseAnswer(breath_rating.getText().toString()));
                answer.put(Provider.Symptom_Data.SCORE_NUMBNESS, parseAnswer(numb_rating.getText().toString()));
                answer.put(Provider.Symptom_Data.SCORE_NAUSEA, parseAnswer(nausea_rating.getText().toString()));
                answer.put(Provider.Symptom_Data.SCORE_DIARRHEA, parseAnswer(diarrhea_rating.getText().toString()));
                answer.put(Provider.Symptom_Data.SCORE_OTHER, parseAnswer(other_rating.getText().toString()));
                answer.put(Provider.Symptom_Data.OTHER_LABEL, other_label.getText().toString());
                getContentResolver().insert(Provider.Symptom_Data.CONTENT_URI, answer);
                int severity = checkSymptoms();
                final Context context = getApplicationContext();
                Aware.setSetting(context, Settings.PLUGIN_UPMC_CANCER_SYMPTOM_SEVERITY, severity);
                sendMessageServiceAction(Constants.ACTION_SETTINGS_CHANGED);
                Toast.makeText(getApplicationContext(), "Thank you!", Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }



    //Handles ? as scores. Can't use ? in SQLite
    private String parseAnswer(String rating) {
        if (rating.equalsIgnoreCase("?"))
            return "NA";
        return rating;
    }

    public int checkSymptoms() {
        for (Integer i : ratingList) {
            if (i >= 7) {
              return 1;
            }
        }
        return 0;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_upmc, menu);

        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            if (item.getTitle().toString().equalsIgnoreCase("Sync") && !Aware.isStudy(getApplicationContext())) {
                item.setVisible(false);
            }
            if (item.getTitle().toString().equalsIgnoreCase("settings") && !Aware.isStudy(getApplicationContext())) {
                item.setVisible(false);
            }
        }

        return true;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onResume();
            return true;
        }

        String title = item.getTitle().toString();
        if (title.equalsIgnoreCase("Settings")) {
            loadSchedule(false);
            return true;
        } else if (title.equalsIgnoreCase("Participant")) {

            @SuppressLint("InflateParams") View participantInfo = getLayoutInflater().inflate(R.layout.participant_info, null);
            TextView uuid = participantInfo.findViewById(R.id.device_id);
            uuid.setText("UUID: " + Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
            final EditText device_label = participantInfo.findViewById(R.id.device_label);
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
        } else if (title.equalsIgnoreCase("Sync")) {
            sendBroadcast(new Intent(Aware.ACTION_AWARE_SYNC_DATA));
            Log.d(Constants.TAG, "UPMC:Sync happened");
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private class JoinedStudy extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equalsIgnoreCase(Aware.ACTION_JOINED_STUDY)) {
                Aware.setSetting(getApplicationContext(), Aware_Preferences.DEBUG_FLAG, true); //enable logcat debug messages
                Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_SIGNIFICANT_MOTION, true);
                Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_ACCELEROMETER, true);
                Aware.setSetting(getApplicationContext(), Aware_Preferences.FREQUENCY_ACCELEROMETER, 200000);
                Aware.setSetting(getApplicationContext(), com.aware.plugin.google.activity_recognition.Settings.STATUS_PLUGIN_GOOGLE_ACTIVITY_RECOGNITION, true);
                Aware.setSetting(getApplicationContext(), com.aware.plugin.google.activity_recognition.Settings.FREQUENCY_PLUGIN_GOOGLE_ACTIVITY_RECOGNITION, 300);
                Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_ESM, true);
                Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_LIGHT, true);
                Aware.setSetting(getApplicationContext(), Aware_Preferences.THRESHOLD_LIGHT, 5);
                Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_BATTERY, true);
                Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_SCREEN, true);
                Aware.setSetting(getApplicationContext(), Aware_Preferences.WEBSERVICE_WIFI_ONLY, true);
                Aware.setSetting(getApplicationContext(), Aware_Preferences.WEBSERVICE_FALLBACK_NETWORK, 6);
                Aware.setSetting(getApplicationContext(), Aware_Preferences.FREQUENCY_WEBSERVICE, 30);
                Aware.setSetting(getApplicationContext(), Aware_Preferences.DEBUG_FLAG, true);
                Aware.setSetting(getApplicationContext(), Aware_Preferences.FREQUENCY_CLEAN_OLD_DATA, 1);
                Aware.setSetting(getApplicationContext(), Aware_Preferences.WEBSERVICE_SILENT, true);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Aware.setSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_MORNING_HOUR, morning_timer.getHour());
                    Aware.setSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_MORNING_MINUTE, morning_timer.getMinute());
                    Aware.setSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_NIGHT_HOUR, night_timer.getHour());
                    Aware.setSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_NIGHT_MINUTE, night_timer.getMinute());
                } else {
                    Aware.setSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_MORNING_HOUR, morning_timer.getCurrentHour());
                    Aware.setSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_MORNING_MINUTE, morning_timer.getCurrentMinute());
                    Aware.setSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_NIGHT_HOUR, night_timer.getCurrentHour());
                    Aware.setSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_NIGHT_MINUTE, night_timer.getCurrentMinute());
                }
                Aware.startPlugin(getApplicationContext(), "com.aware.plugin.google.activity_recognition");
                Aware.startPlugin(getApplicationContext(), "com.aware.plugin.upmc.dash");
                //Ask accessibility to be activated
                Applications.isAccessibilityServiceActive(getApplicationContext());
                Aware.isBatteryOptimizationIgnored(getApplicationContext(), "com.aware.plugin.upmc.dash");

                unregisterReceiver(joinedObserver);
                Toast.makeText(getApplicationContext(), "Joined Study!", Toast.LENGTH_SHORT).show();
                Toast.makeText(getApplicationContext(), "ID:" + Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID), Toast.LENGTH_SHORT).show();
                Log.d(Constants.TAG, "ID: " + Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
                sendMessageServiceAction(Constants.ACTION_FIRST_RUN);
                finish();
            }
        }


    }
}
