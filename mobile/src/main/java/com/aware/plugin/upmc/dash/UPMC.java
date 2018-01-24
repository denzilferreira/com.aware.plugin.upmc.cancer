package com.aware.plugin.upmc.dash;

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
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
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
import com.aware.ui.PermissionsHandler;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class UPMC extends AppCompatActivity {

    private static ProgressDialog dialog;
    int[] morningTime = {-1, -1};
    int[] nightTime = {-1, -1};
    public boolean isWatchAround = false;
    private boolean debug = false;
    private boolean timeInvalid = false;
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

    private Menu menu;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("DASH", "UPMC:onDestroy");
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mNotifBroadcastReceiver);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d("DASH", "UPMC:onCreate");
        LocalBroadcastManager.getInstance(this).registerReceiver(mNotifBroadcastReceiver, new IntentFilter(Constants.NOTIFICATION_MESSAGE_INTENT_FILTER));

        ArrayList<String> REQUIRED_PERMISSIONS = new ArrayList<>();
        REQUIRED_PERMISSIONS.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);

        boolean permissions_ok = true;
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
        }
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean onPrepareOptionsPanel(View view, Menu menu) {
        this.menu = menu;
        return super.onPrepareOptionsPanel(view, menu);
    }

    public void writeTimePref(int morning_hour, int morning_minute, int night_hour, int night_minute) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(Constants.MORNING_HOUR, morning_hour);
        editor.putInt(Constants.MORNING_MINUTE, morning_minute);
        editor.putInt(Constants.NIGHT_HOUR, night_hour);
        editor.putInt(Constants.NIGHT_MINUTE, night_minute);
        editor.apply();
    }

    public void readTimePref() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        int morning_hour = sharedPref.getInt(Constants.MORNING_HOUR, -1);
        int morning_minute = sharedPref.getInt(Constants.MORNING_MINUTE, -1);
        this.morningTime[0] = morning_hour;
        this.morningTime[1] = morning_minute;
        int night_hour = sharedPref.getInt(Constants.NIGHT_HOUR, -1);
        int night_minute = sharedPref.getInt(Constants.NIGHT_MINUTE, -1);
        this.nightTime[0] = night_hour;
        this.nightTime[1] = night_minute;
        Log.d(Constants.TAG, "UPMC:readTimePref: " + morning_minute + " " + morning_minute + " " + night_hour + " " + night_minute);
    }

    public int[] getTime() {
        readTimePref();
        if ((this.morningTime[0] == -1) || (this.nightTime[0] == -1)) {
            setTimeInitilaized(false);
        } else {
            setTimeInitilaized(true);
        }
        return this.morningTime;
    }

    public boolean isTimeInitialized() {
        getTime();
        return this.timeInvalid;
    }

    public void setTimeInitilaized(boolean isinit) {
        this.timeInvalid = isinit;
    }

    private TimePicker morning_timer;
    private TimePicker night_timer;

    private void loadSchedule() {
        setContentView(R.layout.settings_upmc_dash);

        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        final Button saveSchedule = (Button) findViewById(R.id.save_button);

        morning_timer = (TimePicker) findViewById(R.id.morning_start_time);
        night_timer = (TimePicker) findViewById(R.id.night_sleep_time);

        if (Aware.getSetting(this, Settings.PLUGIN_UPMC_CANCER_MORNING_HOUR).length() > 0) {
            Log.d(Constants.TAG, "MORNING_HOUR" + Integer.parseInt(Aware.getSetting(this, Settings.PLUGIN_UPMC_CANCER_MORNING_HOUR)));
            morning_timer.setCurrentHour(Integer.parseInt(Aware.getSetting(this, Settings.PLUGIN_UPMC_CANCER_MORNING_HOUR)));
        } else {
            morning_timer.setCurrentHour(9);
        }
        if (Aware.getSetting(this, Settings.PLUGIN_UPMC_CANCER_MORNING_MINUTE).length() > 0) {
            Log.d(Constants.TAG, "MORNING_MINUTE" + Integer.parseInt(Aware.getSetting(this, Settings.PLUGIN_UPMC_CANCER_MORNING_MINUTE)));
            morning_timer.setCurrentMinute(Integer.parseInt(Aware.getSetting(this, Settings.PLUGIN_UPMC_CANCER_MORNING_MINUTE)));
        } else {
            morning_timer.setCurrentMinute(0);
        }

        if (Aware.getSetting(this, Settings.PLUGIN_UPMC_CANCER_NIGHT_HOUR).length() > 0) {
            int hour = Integer.parseInt(Aware.getSetting(this, Settings.PLUGIN_UPMC_CANCER_NIGHT_HOUR));
            if (hour > 12)
                hour = hour % 12;
            night_timer.setCurrentHour(hour);
            Log.d(Constants.TAG, hour + "NIGHT_HOUR" + Integer.parseInt(Aware.getSetting(this, Settings.PLUGIN_UPMC_CANCER_NIGHT_HOUR)));

        } else {
            night_timer.setCurrentHour(21);
        }
        if (Aware.getSetting(this, Settings.PLUGIN_UPMC_CANCER_NIGHT_MINUTE).length() > 0) {
            Log.d(Constants.TAG, "NIGHT_MINUTE" + Integer.parseInt(Aware.getSetting(this, Settings.PLUGIN_UPMC_CANCER_NIGHT_MINUTE)));
            night_timer.setCurrentMinute(Integer.parseInt(Aware.getSetting(this, Settings.PLUGIN_UPMC_CANCER_NIGHT_MINUTE)));
        } else {
            night_timer.setCurrentMinute(0);
        }

        final Context context = getApplicationContext();

        saveSchedule.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //if(menu.getItem(0).getTitle().equals("Demo Watch")) {
                Log.d(Constants.TAG, "thread started");
                isWatchAround = false;
                LocalBroadcastManager.getInstance(context).registerReceiver(vicinityCheckBroadcastReceiver, new IntentFilter(Constants.VICINITY_CHECK_INTENT_FILTER));
                LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(Constants.SETTING_INTENT_FILTER).putExtra(Constants.SETTINGS_EXTRA_KEY, Constants.VICINITY_CHECK));
                final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress_bar_schedule);
                progressBar.setVisibility(View.VISIBLE);
                saveSchedule.setEnabled(false);
                saveSchedule.setText("Saving Schedule....");
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                if (!isWatchAround && isTimeInitialized()) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            saveSchedule.setText("Save Answers");
                                            Toast.makeText(context, "Failed! Please make sure watch is in range", Toast.LENGTH_SHORT).show();
                                            progressBar.setVisibility(View.GONE);
                                            saveSchedule.setEnabled(true);
                                        }
                                    });
                                } else {
                                    // start MessageService
                                    if (!isTimeInitialized()) {
                                        Log.d(Constants.TAG, "UPMC:" + Aware.getSetting(getApplication(), Settings.PLUGIN_UPMC_CANCER_MORNING_HOUR) + " " + Aware.getSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_MORNING_MINUTE + " " + Settings.PLUGIN_UPMC_CANCER_NIGHT_HOUR + " " + Settings.PLUGIN_UPMC_CANCER_NIGHT_MINUTE));
                                        writeTimePref(morning_timer.getCurrentHour().intValue(), morning_timer.getCurrentMinute().intValue(), night_timer.getCurrentHour().intValue(), night_timer.getCurrentMinute().intValue());
                                        readTimePref();
                                        if (!isMyServiceRunning(MessageService.class)) {
                                            startService(new Intent(getApplicationContext(), MessageService.class));
                                            Log.d(Constants.TAG, "UPMC: Started Message Service");
                                        } else
                                            Log.d(Constants.TAG, "UPMC: Message Service already running");
                                        setTimeInitilaized(true);

                                    } else {
                                        Log.d(Constants.TAG, "UPMC: Sending Settings Changed Broadcast");
                                        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent(Constants.SETTING_INTENT_FILTER).putExtra(Constants.SETTINGS_EXTRA_KEY, Constants.SETTINGS_CHANGED));
                                        writeTimePref(morning_timer.getCurrentHour().intValue(), morning_timer.getCurrentMinute().intValue(), night_timer.getCurrentHour().intValue(), night_timer.getCurrentMinute().intValue());
                                    }

                                    if (!Aware.isStudy(getApplicationContext())) {
                                        new AsyncJoin().execute();
                                    }
                                }
                                LocalBroadcastManager.getInstance(context).unregisterReceiver(vicinityCheckBroadcastReceiver);

                            }
                        }).start();
                    }
                }, 7000);
            }
        });
    }

    private class AsyncJoin extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            //UPMC Dash
            Aware.joinStudy(getApplicationContext(), "https://r2d2.hcii.cs.cmu.edu/aware/dashboard/index.php/webservice/index/81/Rhi4Q8PqLASf");

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
            Aware.setSetting(getApplicationContext(), Aware_Preferences.WEBSERVICE_FALLBACK_NETWORK, 6);
            Aware.setSetting(getApplicationContext(), Aware_Preferences.FREQUENCY_WEBSERVICE, 30);
            Aware.setSetting(getApplicationContext(), Aware_Preferences.DEBUG_FLAG, true);

            Aware.setSetting(getApplicationContext(), Aware_Preferences.FREQUENCY_CLEAN_OLD_DATA, 1);
            Aware.setSetting(getApplicationContext(), Aware_Preferences.WEBSERVICE_SILENT, true);

            Aware.setSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_MORNING_HOUR, morning_timer.getCurrentHour().intValue());
            Aware.setSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_MORNING_MINUTE, morning_timer.getCurrentMinute().intValue());
            Aware.setSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_NIGHT_HOUR, night_timer.getCurrentHour().intValue());
            Aware.setSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_NIGHT_MINUTE, night_timer.getCurrentMinute().intValue());

            Aware.startPlugin(getApplicationContext(), "com.aware.plugin.upmc.dash");

            //Ask accessibility to be activated
            Applications.isAccessibilityServiceActive(getApplicationContext());
            Aware.isBatteryOptimizationIgnored(getApplicationContext(), "com.aware.plugin.upmc.dash");
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Toast.makeText(getApplicationContext(),"Joined OK!", Toast.LENGTH_LONG).show();
            Intent applySchedule = new Intent(getApplicationContext(), Plugin.class);
            applySchedule.putExtra("schedule", true);
            startService(applySchedule);
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        ratingList = new ArrayList<>(12);
        for (int i = 0; i < 12; i++) {
            ratingList.add(i, -1);
        }

        Aware.setSetting(this, Aware_Preferences.DEBUG_FLAG, debug);
        //NOTE: needed for demo to participants
        Aware.setSetting(this, Aware_Preferences.STATUS_ESM, true);
        //Aware.startESM(this);
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
                ratingList.set(0, i);
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
                ratingList.set(1, i);
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
                ratingList.set(2, i);
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
                ratingList.set(3, i);
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
                ratingList.set(4, i);
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
                ratingList.set(5, i);
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
                ratingList.set(6, i);
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
                ratingList.set(7, i);
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
                ratingList.set(8, i);
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
                ratingList.set(9, i);
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
                ratingList.set(10, i);
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
        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress_bar_syms);
        final Context context = this;


        answer_questions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                answer_questions.setEnabled(false);
                answer_questions.setText("Saving answers..");
                Log.d(Constants.TAG, "Trig::Questionnaire");
                progressBar.setVisibility(View.VISIBLE);


                Log.d(Constants.TAG, "UPMC: Progress&Vicinity Thread starts");
                LocalBroadcastManager.getInstance(context).registerReceiver(vicinityCheckBroadcastReceiver, new IntentFilter(Constants.VICINITY_CHECK_INTENT_FILTER));
                LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(Constants.SETTING_INTENT_FILTER).putExtra(Constants.SETTINGS_EXTRA_KEY, Constants.VICINITY_CHECK));

                Handler handler = new Handler();

                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(Constants.TAG, "UPMC:: Handler is running");
                        if (!isWatchAround) {
                            Toast.makeText(context, "Failed to save settings. Please try again with watch around!", Toast.LENGTH_LONG).show();
                            LocalBroadcastManager.getInstance(context).unregisterReceiver(vicinityCheckBroadcastReceiver);
                            progressBar.setVisibility(View.GONE);
                            answer_questions.setText("Save Answers");
                            answer_questions.setEnabled(true);
                        } else {
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
                            Log.d(Constants.TAG, "Trig::Questionnaire" + Integer.parseInt(pain_rating.getText().toString()));
                            getContentResolver().insert(Provider.Symptom_Data.CONTENT_URI, answer);
                            checkSymptoms();
                            Log.d("UPMC", "Answers:" + answer.toString());
                            Toast.makeText(getApplicationContext(), "Saved successfully.", Toast.LENGTH_LONG).show();
                            LocalBroadcastManager.getInstance(context).unregisterReceiver(vicinityCheckBroadcastReceiver);
                            finish();
                        }
                    }
                }, 7000);
            }
        });
    }

    public BroadcastReceiver vicinityCheckBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(Constants.TAG, "UPMC:: vicinitycheck received from MessageService: " + intent.getIntExtra(Constants.VICINITY_RESULT_KEY, -1));
            if (intent.hasExtra(Constants.VICINITY_RESULT_KEY)) {
                if ((intent.getIntExtra(Constants.VICINITY_RESULT_KEY, -1) == Constants.WEAR_VICINITY_CHECK_FAILED)
                        || (intent.getIntExtra(Constants.VICINITY_RESULT_KEY, -1) == Constants.WEAR_NOT_IN_RANGE)) {
                    isWatchAround = false;
                } else if (intent.getIntExtra(Constants.VICINITY_RESULT_KEY, -1) == Constants.WEAR_IN_RANGE) {
                    isWatchAround = true;
                }
            }

        }
    };

    public void checkSymptoms() {
        boolean badSymps = false;
        for (Integer i : ratingList) {
            if (i >= 7) {
                badSymps = true;
                break;
            }
        }
        if (badSymps) {
            Log.d(Constants.TAG, "Bad Symptoms");
            LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(Constants.SYMPTOMS_INTENT_FILTER).putExtra(Constants.SYMPTOMS_KEY, Constants.SYMPTOMS_1));
        } else {
            Log.d(Constants.TAG, "Good Symptoms");
            LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(Constants.SYMPTOMS_INTENT_FILTER).putExtra(Constants.SYMPTOMS_KEY, Constants.SYMPTOMS_0));
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
        } else if (title.equalsIgnoreCase("Participant")) {

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
        } else if (!isTimeInitialized()) {
            if (title.equalsIgnoreCase("Demo Watch")) {
//            Intent walking = new Intent(this, UPMC_Motivation.class);
//            walking.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            startActivity(walking);
                Log.d(Constants.TAG, "UPMC:Demo Watch happened");
                final Context context = this;
                final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
                final AlertDialog.Builder dialogBuilder1 = new AlertDialog.Builder(context);
                final View view = new ProgressBar(UPMC.this);
                dialogBuilder1.setView(view);
                dialogBuilder1.setCancelable(false);
                item.setTitle("Stop Demo");
                dialogBuilder.setTitle("Demo")
                        .setMessage("This is a walkthrough of the prompts that you will receive, when you are inactive")
                        .setCancelable(false)
                        .setPositiveButton("Proceed", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                final AlertDialog diag = dialogBuilder1.create();
                                diag.show();
                                Handler handler = new Handler();
                                if (!isMyServiceRunning(MessageService.class)) {
                                    startService(new Intent(getApplicationContext(), MessageService.class).putExtra(Constants.COMM_KEY_MSGSERVICE, Constants.DEMO_MODE));
                                    Log.d(Constants.TAG, "UPMC: Starting demo Message Service");
                                } else
                                    Log.d(Constants.TAG, "UPMC: Starting demo Service already running");
                                handler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        LocalBroadcastManager.getInstance(context).registerReceiver(vicinityCheckBroadcastReceiver, new IntentFilter(Constants.VICINITY_CHECK_INTENT_FILTER));
                                        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(Constants.SETTING_INTENT_FILTER).putExtra(Constants.SETTINGS_EXTRA_KEY, Constants.VICINITY_CHECK));
                                    }
                                }, 5000);
                                handler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (!isWatchAround) {
                                            Toast.makeText(getApplicationContext(), "Failed: Could not find your watch ", Toast.LENGTH_LONG).show();
                                            diag.dismiss();
                                        } else {
                                            Toast.makeText(getApplicationContext(), "Success: Demo will start in 5 seconds ", Toast.LENGTH_LONG).show();
                                            Toast.makeText(getApplicationContext(), "Save Answers will be disabled during demo. Please cancel demo to enable", Toast.LENGTH_SHORT).show();
                                            LocalBroadcastManager.getInstance(context).unregisterReceiver(vicinityCheckBroadcastReceiver);
                                            diag.dismiss();
                                        }
                                    }
                                }, 17000);
                            }
                        })
                        .create();
                dialogBuilder.create().show();
                dialogBuilder1.setTitle("Preparing Demo...")
                        .setView(view);
                return true;
            } else if (title.equalsIgnoreCase("Stop Demo")) {
                LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(Constants.SETTING_INTENT_FILTER).putExtra(Constants.SETTINGS_EXTRA_KEY, Constants.KILL_DEMO));
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (isMyServiceRunning(MessageService.class)) {
                            Intent intent = new Intent(getApplicationContext(), MessageService.class);
                            stopService(intent);
                        } else {
                            Toast.makeText(getApplicationContext(), "Demo has been stopped", Toast.LENGTH_LONG).show();
                        }
                    }
                }, 2000);

                item.setTitle("Demo Watch");
                final Button saveSchedule = (Button) findViewById(R.id.save_button);
                item.setEnabled(false);
            } else if (title.equalsIgnoreCase("Sync")) {
                sendBroadcast(new Intent(Aware.ACTION_AWARE_SYNC_DATA));
                Log.d(Constants.TAG, "UPMC:Sync happened");
                return true;
            } else if (title.equalsIgnoreCase("Demo ESM")) {
                Log.d(Constants.TAG, "UPMC:DemoESM happened");
                Intent intent = new Intent(this, DemoESM.class);
                startActivity(intent);
                item.setEnabled(false);
                return true;

            }
        } else {
            Toast.makeText(getApplicationContext(), "Unable to start during study", Toast.LENGTH_LONG).show();
        }

        return super.onOptionsItemSelected(item);
    }
}
