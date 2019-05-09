package com.aware.plugin.upmc.dash.activities;

import android.Manifest;
import android.accounts.Account;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SyncRequest;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.PermissionChecker;

import com.aware.Applications;
import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.plugin.upmc.dash.R;
import com.aware.plugin.upmc.dash.services.FitbitMessageService;
import com.aware.plugin.upmc.dash.settings.Settings;
import com.aware.plugin.upmc.dash.utils.Constants;
import com.aware.ui.PermissionsHandler;
import com.aware.utils.Scheduler;
import com.crashlytics.android.Crashlytics;
import com.ramotion.fluidslider.FluidSlider;

import org.json.JSONException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Random;

import io.fabric.sdk.android.Fabric;
import kotlin.Unit;

import static com.aware.plugin.upmc.dash.utils.Constants.CASE1;
import static com.aware.plugin.upmc.dash.utils.Constants.CASE2;
import static com.aware.plugin.upmc.dash.utils.Constants.DB_URL;
import static com.aware.plugin.upmc.dash.utils.Constants.PASS;
import static com.aware.plugin.upmc.dash.utils.Constants.TABLE_PS;
import static com.aware.plugin.upmc.dash.utils.Constants.USER;

public class MainActivity extends AppCompatActivity {

    private boolean STUDYLESS_DEBUG = false;
    private boolean isRegistered = false;
    private JoinedStudy joinedObserver = new JoinedStudy();
    private boolean SHOW_INCOMPLETE_NOTIF = false;
    private int easter = 0;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(Constants.TAG, "MainActivity:onDestroy");
        if (isRegistered)
            unregisterReceiver(joinedObserver);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(Constants.TAG, "MainActivity:onCreate");
        // Fabric
        Fabric.with(this, new Crashlytics());
        easter = 0;
    }


    public void hideOtherSlidersExcept(int except_id, int[] FS_IDS, int[] TVE_IDS, int[] IBE_IDS,
                                       int other_entry) {
        for (int i = 0; i < FS_IDS.length; i++) {
            if (i != except_id) {
                View view = findViewById(FS_IDS[i]);
                if (view.getVisibility() == View.VISIBLE) {
                    findViewById(IBE_IDS[i]).setVisibility(View.VISIBLE);
                    findViewById(TVE_IDS[i]).setVisibility(View.VISIBLE);
                    findViewById(FS_IDS[i]).setVisibility(View.GONE);
                }
                if (i == 11)
                    findViewById(other_entry).setVisibility(View.GONE);

            }
        }
    }


    public void showIncompleteAlert(int newSeverity, boolean daily, ContentValues answer) {
        AlertDialog.Builder builder =
                new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.myDialog));
        builder.setMessage("Symptom ratings not complete. Submit anyway?").setTitle("Alert!");
        builder.setPositiveButton("Submit Anyway", (dialogInterface, i) -> {
            new PostData().execute(TABLE_PS, newSeverity == 1 ? CASE2 : CASE1);
            // switch back to old notification
            if (daily)
                sendFitbitMessageServiceAction(Constants.ACTION_SURVEY_COMPLETED);
            Log.d(Constants.TAG, "MainActivity:showSymptomSurvey:submit:" + answer.toString());
            getContentResolver().insert(Provider.Symptom_Data.CONTENT_URI, answer);
            toastThanks(getApplicationContext());
            finish();
        });
        builder.setNegativeButton("Go back", (dialogInterface, i) -> dialogInterface.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
        //        new PostData().execute(TABLE_PS, newSeverity==1? CASE2:CASE1);
        //        // switch back to old notification
        //        if(daily)
        //            sendFitbitMessageServiceAction(Constants.ACTION_SURVEY_COMPLETED);
        //
        //        Log.d(Constants.TAG, "MainActivity:showSymptomSurvey:submit:" + answer.toString
        //        ());
        //        getContentResolver().insert(Provider.Symptom_Data.CONTENT_URI, answer);
        //        toastThanks(getApplicationContext());
        //        finish();
    }


    @Override
    protected void onResume() {
        super.onResume();
        invalidateOptionsMenu();
        // Checking for more permissions
        Log.d(Constants.TAG, "MainActivity:onResume");
        ArrayList<String> REQUIRED_PERMISSIONS = new ArrayList<>();
        REQUIRED_PERMISSIONS.add(Manifest.permission.VIBRATE);
        REQUIRED_PERMISSIONS.add(Manifest.permission.BLUETOOTH);
        REQUIRED_PERMISSIONS.add(Manifest.permission.BLUETOOTH_ADMIN);
        REQUIRED_PERMISSIONS.add(Manifest.permission.ACCESS_WIFI_STATE);
        REQUIRED_PERMISSIONS.add(Manifest.permission.ACCESS_WIFI_STATE);
        REQUIRED_PERMISSIONS.add(Manifest.permission.WAKE_LOCK);
        //these are needed for the sync adapter to work...
        REQUIRED_PERMISSIONS.add(Manifest.permission.GET_ACCOUNTS);
        REQUIRED_PERMISSIONS.add(Manifest.permission.WRITE_SYNC_SETTINGS);
        REQUIRED_PERMISSIONS.add(Manifest.permission.READ_SYNC_SETTINGS);
        REQUIRED_PERMISSIONS.add(Manifest.permission.READ_SYNC_STATS);
        boolean permissions_ok = true;
        for (String p : REQUIRED_PERMISSIONS) {
            if (PermissionChecker
                    .checkSelfPermission(this, p) != PermissionChecker.PERMISSION_GRANTED) {
                permissions_ok = false;
                break;
            }
        }
        if (!permissions_ok) {
            Intent permissions = new Intent(this, PermissionsHandler.class);
            permissions
                    .putExtra(PermissionsHandler.EXTRA_REQUIRED_PERMISSIONS, REQUIRED_PERMISSIONS);
            permissions.putExtra(PermissionsHandler.EXTRA_REDIRECT_ACTIVITY,
                    getPackageName() + "/" + getClass().getName());
            permissions.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(permissions);
            finish();
        } else {
            // continue only if permissions are ok
            if (!STUDYLESS_DEBUG) {
                if (!Aware.IS_CORE_RUNNING) {
                    //This initialises the core framework, assigns Device ID if it doesn't exist
                    // yet, etc.
                    Log.d(Constants.TAG, "MainActivity:startAware");
                    Intent aware = new Intent(getApplicationContext(), Aware.class);
                    startService(aware);
                }
                if (!Aware.isStudy(getApplicationContext())) {
                    // if app hasn't joined the study yet
                    Log.d(Constants.TAG,
                            "MainActivity:onResume:FIRST_RUN - registering join " + "study " +
                                    "observer");
                    IntentFilter filter = new IntentFilter(Aware.ACTION_JOINED_STUDY);
                    registerReceiver(joinedObserver, filter);
                    showSettings(true);
                } else {
                    if (!isMyServiceRunning(FitbitMessageService.class))
                        sendFitbitMessageServiceAction(Constants.ACTION_REBOOT);
                    if (getIntent() != null) {
                        if (getIntent().getAction() != null) {
                            if (getIntent().getAction().equals(Constants.ACTION_SHOW_MORNING))
                                showSymptomSurvey(true);
                            //check this out
                            if (Aware.getSetting(getApplicationContext(),
                                    Settings.PLUGIN_UPMC_CANCER_DND_MODE)
                                    .equals(Constants.DND_MODE_ON)) {
                                Aware.setSetting(getApplicationContext(),
                                        Settings.PLUGIN_UPMC_CANCER_DND_MODE,
                                        Constants.DND_MODE_OFF);
                            }
                        } else {
                            boolean show_daily = Aware.getSetting(getApplicationContext(),
                                    Settings.PLUGIN_UPMC_CANCER_SHOW_MORNING)
                                    .equalsIgnoreCase(Constants.SHOW_MORNING_SURVEY);
                            showSymptomSurvey(show_daily);
                        }
                    }

                }
            } else {
                if (!isMyServiceRunning(FitbitMessageService.class))
                    sendFitbitMessageServiceAction(Constants.ACTION_FIRST_RUN);
                if (getIntent() != null) {
                    if (getIntent().getAction() != null) {
                        if (getIntent().getAction().equals(Constants.ACTION_SHOW_MORNING))
                            showSymptomSurvey(true);
                        //check this out
                        if (Aware.getSetting(getApplicationContext(),
                                Settings.PLUGIN_UPMC_CANCER_DND_MODE)
                                .equals(Constants.DND_MODE_ON)) {
                            Aware.setSetting(getApplicationContext(),
                                    Settings.PLUGIN_UPMC_CANCER_DND_MODE, Constants.DND_MODE_OFF);
                        }
                    } else {
                        showSymptomSurvey(false);
                    }
                }
            }
        }

    }


    public void engageScheduler() {
        Aware.setSetting(getApplicationContext(), Settings.STATUS_PLUGIN_UPMC_CANCER, true);
        int morning_hour = Integer.parseInt(Aware.getSetting(getApplicationContext(),
                Settings.PLUGIN_UPMC_CANCER_MORNING_HOUR));
        int morning_minute = Integer.parseInt(
                Aware.getSetting(this, Settings.PLUGIN_UPMC_CANCER_MORNING_MINUTE));
        String className = getPackageName() + "/" + FitbitMessageService.class.getName();
        createSchedule(morning_hour, morning_minute, Constants.MORNING_SURVEY_SCHED_ID, className,
                Scheduler.ACTION_TYPE_SERVICE, Plugin.ACTION_CANCER_SURVEY);
        int evening_hour = Integer.parseInt(
                Aware.getSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_NIGHT_HOUR));
        int evening_minute =
                Integer.parseInt(Aware.getSetting(this, Settings.PLUGIN_UPMC_CANCER_NIGHT_MINUTE));
        createSchedule(evening_hour, evening_minute, Constants.EVENING_SYNC_ID, className,
                Scheduler.ACTION_TYPE_SERVICE, Constants.ACTION_SYNC_DATA);
    }

    public void createSchedule(int hour, int minute, String id, String className, String classType,
                               String action) {
        Log.d(Constants.TAG, "MainActivity:createSchedule:creating a schedule..");
        Scheduler.Schedule currentScheduler = Scheduler.getSchedule(getApplicationContext(), id);
        if (currentScheduler != null)
            Scheduler.removeSchedule(getApplicationContext(), id);
        Scheduler.Schedule schedule = new Scheduler.Schedule(id);
        try {
            schedule.addHour(hour).addMinute(minute).setActionClass(className)
                    .setActionIntentAction(action).setActionType(classType);
            Scheduler.saveSchedule(this, schedule);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Aware.startScheduler(getApplicationContext());
    }


    public void showSymptomSurvey(boolean daily) {
        final int[] FS_IDS =
                {R.id.fluidSlider1, R.id.fluidSlider2, R.id.fluidSlider3, R.id.fluidSlider4,
                        R.id.fluidSlider5, R.id.fluidSlider6, R.id.fluidSlider7, R.id.fluidSlider8,
                        R.id.fluidSlider9, R.id.fluidSlider10, R.id.fluidSlider11,
                        R.id.fluidSlider12};
        final int[] RG_IDS =
                {R.id.rg1, R.id.rg2, R.id.rg3, R.id.rg4, R.id.rg5, R.id.rg6, R.id.rg7, R.id.rg8,
                        R.id.rg9, R.id.rg10, R.id.rg11, R.id.rg12};
        final int[] IBE_IDS =
                {R.id.ibe1, R.id.ibe2, R.id.ibe3, R.id.ibe4, R.id.ibe5, R.id.ibe6, R.id.ibe7,
                        R.id.ibe8, R.id.ibe9, R.id.ibe10, R.id.ibe11, R.id.ibe12};
        final int[] TVE_IDS =
                {R.id.editView1, R.id.editView2, R.id.editView3, R.id.editView4, R.id.editView5,
                        R.id.editView6, R.id.editView7, R.id.editView8, R.id.editView9,
                        R.id.editView10, R.id.editView11, R.id.editView12};
        final int[] RBP_IDS =
                {R.id.rbp1, R.id.rbp2, R.id.rbp3, R.id.rbp4, R.id.rbp5, R.id.rbp6, R.id.rbp7,
                        R.id.rbp8, R.id.rbp9, R.id.rbp10, R.id.rbp11, R.id.rbp12};
        setContentView(R.layout.activity_main);
        final String[] PROVIDERS =
                new String[]{Provider.Symptom_Data.SCORE_PAIN, Provider.Symptom_Data.SCORE_FATIGUE,
                        Provider.Symptom_Data.SCORE_SLEEP_DISTURBANCE,
                        Provider.Symptom_Data.SCORE_CONCENTRATING, Provider.Symptom_Data.SCORE_SAD,
                        Provider.Symptom_Data.SCORE_ANXIOUS,
                        Provider.Symptom_Data.SCORE_SHORT_BREATH,
                        Provider.Symptom_Data.SCORE_NUMBNESS, Provider.Symptom_Data.SCORE_NAUSEA,
                        Provider.Symptom_Data.SCORE_DIARRHEA, Provider.Symptom_Data.SCORE_DIZZY,
                        Provider.Symptom_Data.SCORE_OTHER};
        final int other_entry = R.id.other_entry;
        //        if (daily) {
        //            morning_questions.setVisibility(View.VISIBLE);
        //            Calendar today = Calendar.getInstance();
        //            today.setTimeInMillis(System.currentTimeMillis());
        //            today.set(Calendar.HOUR_OF_DAY, 1);
        //            today.set(Calendar.MINUTE, 0);
        //            today.set(Calendar.SECOND, 0);
        //            Cursor already_answered =
        //                    getContentResolver().query(Provider.Symptom_Data.CONTENT_URI, null,
        //                            Provider.Symptom_Data.TIMESTAMP + " > " + today
        //                            .getTimeInMillis() +
        //                                    " AND (" + Provider.Symptom_Data.TO_BED + " != ''
        //                                    OR " + Provider.Symptom_Data.FROM_BED + " !='')",
        //                                    null, null);
        //            if (already_answered != null && already_answered.getCount() > 0) {
        //                findViewById(R.id.morning_questions).setVisibility(View.GONE);
        //            }
        //            if (already_answered != null && !already_answered.isClosed())
        //                already_answered.close();
        //
        //        }
        // UI stuff
        ScrollView sv = findViewById(R.id.scroll_view);
        for (int FS_ID : FS_IDS) {
            FluidSlider fs = findViewById(FS_ID);
            fs.setPositionListener(pos -> {
                final String value = String.valueOf((int) (pos * 100) / 10);
                fs.setBubbleText(value);
                return Unit.INSTANCE;
            });

        }
        for (int i = 0; i < FS_IDS.length; i++) {
            final int index = i;
            RadioGroup rg = findViewById(RG_IDS[index]);
            rg.setOnCheckedChangeListener((radioGroup, i1) -> {
                RadioButton rb = findViewById(i1);
                if (rb.getText().equals("Yes")) {
                    hideOtherSlidersExcept(index, FS_IDS, TVE_IDS, IBE_IDS, other_entry);
                    FluidSlider fs = findViewById(FS_IDS[index]);
                    fs.setVisibility(View.VISIBLE);
                    if ((index > 5))
                        sv.post(() -> sv.scrollTo(0, fs.getTop()));
                    fs.setEndTrackingListener(() -> {
                        Log.d(Constants.TAG, "POS " + fs.getPosition());
                        new Handler().postDelayed(() -> {
                            fs.setVisibility(View.GONE);
                            radioGroup.check(RBP_IDS[index]);
                            findViewById(IBE_IDS[index]).setVisibility(View.VISIBLE);
                            TextView tv = findViewById(TVE_IDS[index]);
                            tv.setVisibility(View.VISIBLE);
                            int rating = (int) (fs.getPosition() * 10);
                            tv.setText(String.valueOf(rating));

                        }, 750);
                        return Unit.INSTANCE;
                    });
                    if (index == 11) {
                        Toast.makeText(getApplicationContext(), "Please specify",
                                Toast.LENGTH_SHORT).show();
                        EditText editText = findViewById(other_entry);
                        editText.setVisibility(View.VISIBLE);
                    }

                } else if (rb.getText().equals("No")) {
                    hideOtherSlidersExcept(index, FS_IDS, TVE_IDS, IBE_IDS, other_entry);
                    findViewById(IBE_IDS[index]).setVisibility(View.GONE);
                    findViewById(TVE_IDS[index]).setVisibility(View.GONE);
                    FluidSlider fs = findViewById(FS_IDS[index]);
                    if (fs.getVisibility() == View.VISIBLE) {
                        fs.setVisibility(View.GONE);
                    }
                    if (index == 11) {
                        EditText editText = findViewById(other_entry);
                        editText.setVisibility(View.GONE);
                    }
                }


            });
            findViewById(IBE_IDS[index]).setOnClickListener(view -> {
                hideOtherSlidersExcept(index, FS_IDS, TVE_IDS, IBE_IDS, other_entry);
                Log.d("tv", "clicked");
                FluidSlider fs = findViewById(FS_IDS[index]);
                fs.setVisibility(View.VISIBLE);
                if ((index > 5))
                    sv.post(() -> sv.scrollTo(0, fs.getTop()));
            });

        }
        // submit
        findViewById(R.id.submit).setOnClickListener(view -> {
            ContentValues answer = new ContentValues();
            answer.put(Provider.Symptom_Data.DEVICE_ID,
                    Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
            answer.put(Provider.Symptom_Data.TIMESTAMP, System.currentTimeMillis());
            int newSeverity = 0;
            SHOW_INCOMPLETE_NOTIF = false;
            for (int i = 0; i < RG_IDS.length; i++) {
                RadioGroup rg = findViewById(RG_IDS[i]);
                RadioButton rb = findViewById(rg.getCheckedRadioButtonId());
                if (rb == null) {
                    // nothing has been selected
                    answer.put(PROVIDERS[i], "NA");
                    if (i != 11)
                        SHOW_INCOMPLETE_NOTIF = true;
                } else if (rb.getText().equals("Yes")) {
                    FluidSlider fs = findViewById(FS_IDS[i]);
                    int rating = (int) (fs.getPosition() * 10);
                    if (rating >= 7)
                        newSeverity = 1;
                    answer.put(PROVIDERS[i], String.valueOf(rating));
                    if (i == 11) {
                        EditText other_label = findViewById(other_entry);
                        answer.put(Provider.Symptom_Data.OTHER_LABEL,
                                other_label.getText().toString());
                    }
                } else if (rb.getText().equals("No")) {
                    answer.put(PROVIDERS[i], "No");
                }
            }
            if (SHOW_INCOMPLETE_NOTIF)
                showIncompleteAlert(newSeverity, daily, answer);
            else {
                // post it to KSWEB DB
                new PostData().execute(TABLE_PS, newSeverity == 1 ? CASE2 : CASE1);
                // switch back to old notification
                if (daily)
                    sendFitbitMessageServiceAction(Constants.ACTION_SURVEY_COMPLETED);
                Log.d(Constants.TAG, "MainActivity:showSymptomSurvey:submit:" + answer.toString());
                getContentResolver().insert(Provider.Symptom_Data.CONTENT_URI, answer);
                toastThanks(getApplicationContext());
                finish();
            }

        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_upmc, menu);
        if (!Aware.isStudy(getApplicationContext()) || Aware
                .getSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_DEVICE_TYPE)
                .equalsIgnoreCase(Constants.DEVICE_TYPE_CONTROL)) {
            for (int i = 0; i < menu.size(); i++) {
                MenuItem item = menu.getItem(i);
                if (item.getTitle().toString().equalsIgnoreCase("sync"))
                    item.setVisible(false);
                if (item.getTitle().toString().equalsIgnoreCase("settings")) {
                    item.setVisible(false);
                    if (Aware.isStudy(getApplicationContext()) && Aware
                            .getSetting(getApplicationContext(),
                                    Settings.PLUGIN_UPMC_CANCER_DEVICE_TYPE)
                            .equalsIgnoreCase(Constants.DEVICE_TYPE_CONTROL)) {
                        item.setVisible(true);
                    }
                }
                if (item.getTitle().toString().equalsIgnoreCase("dnd1"))
                    item.setVisible(false);
            }
        } else {
            for (int i = 0; i < menu.size(); i++) {
                MenuItem item = menu.getItem(i);
                if ((item.getTitle().toString().equalsIgnoreCase("dnd1") || item.getTitle()
                        .toString().equalsIgnoreCase("dnd2")) && Aware
                        .getSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_DND_MODE)
                        .equals(Constants.DND_MODE_ON)) {
                    item.setIcon(R.drawable.do_not_disturb_off_white_24x24);
                    item.setTitle("Dnd2");
                } else if ((item.getTitle().toString().equalsIgnoreCase("dnd1") || item.getTitle()
                        .toString().equalsIgnoreCase("dnd2")) && Aware
                        .getSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_DND_MODE)
                        .equals(Constants.DND_MODE_OFF)) {
                    item.setIcon(R.drawable.do_not_disturb_on_white_24x24);
                    item.setTitle("Dnd1");
                }
            }
        }
        return true;
    }


    public void showSnoozeAlert(String DND_STATUS, MenuItem item) {
        String message = DND_STATUS
                .equals(Constants.DND_MODE_ON) ? Constants.SNOOZE_ON_ALERT :
                Constants.SNOOZE_OFF_ALERT;
        AlertDialog.Builder builder =
                new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.myDialog));
        builder.setMessage(message).setTitle("Alert!");
        builder.setPositiveButton(Constants.SNOOZE_ALERT_POS, (dialogInterface, i) -> {
            if (Aware.getSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_DND_MODE)
                    .equals(Constants.DND_MODE_OFF)) {
                item.setIcon(R.drawable.do_not_disturb_off_white_24x24);
                Aware.setSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_DND_MODE,
                        Constants.DND_MODE_ON);
                //            Toast.makeText(getApplicationContext(), "You will not receive
                //            prompts for the rest of the day", Toast.LENGTH_SHORT).show();
                item.setTitle("Dnd2");
            } else {
                item.setIcon(R.drawable.do_not_disturb_on_white_24x24);
                Aware.setSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_DND_MODE,
                        Constants.DND_MODE_OFF);
                item.setTitle("Dnd1");
            }
            sendFitbitMessageServiceAction(Constants.ACTION_DO_NOT_DISTURB);
            saveDndAction(
                    Aware.getSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_DND_MODE),
                    Constants.DND_TOGGLE_MANUAL);
        });
        builder.setNegativeButton(Constants.SNOOZE_ALERT_NEG,
                (dialogInterface, i) -> dialogInterface.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
    }


    public void saveDndAction(String mode, int toggled_by) {
        ContentValues response = new ContentValues();
        response.put(Provider.Dnd_Toggle.DEVICE_ID,
                Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
        response.put(Provider.Dnd_Toggle.TIMESTAMP, System.currentTimeMillis());
        response.put(Provider.Dnd_Toggle.TOGGLE_POS, mode.equals(Constants.DND_MODE_ON) ? 1 : 0);
        response.put(Provider.Dnd_Toggle.TOGGLED_BY, toggled_by);
        getContentResolver().insert(Provider.Dnd_Toggle.CONTENT_URI, response);
        Log.d(Constants.TAG, "MainActivity:saveDndAction");

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
            showSettings(false);
            return true;
        } else if (title.equalsIgnoreCase("Participant")) {
            @SuppressLint("InflateParams") View participantInfo =
                    getLayoutInflater().inflate(R.layout.participant_info, null);
            TextView uuid = participantInfo.findViewById(R.id.device_id);
            uuid.setText("UUID: " + Aware
                    .getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
            final EditText device_label = participantInfo.findViewById(R.id.device_label);
            device_label.setText(Aware.getSetting(this, Aware_Preferences.DEVICE_LABEL));
            AlertDialog.Builder mBuilder = new AlertDialog.Builder(this);
            mBuilder.setTitle("UPMC Participant");
            mBuilder.setView(participantInfo);
            mBuilder.setPositiveButton("OK", (dialog, which) -> {
                if (device_label.getText().length() > 0 && !device_label.getText().toString()
                        .equals(Aware.getSetting(getApplicationContext(),
                                Aware_Preferences.DEVICE_LABEL))) {
                    Aware.setSetting(getApplicationContext(), Aware_Preferences.DEVICE_LABEL,
                            device_label.getText().toString());
                }
                dialog.dismiss();
            });
            mBuilder.create().show();
            return true;
        } else if (title.equalsIgnoreCase("Sync")) {
            Log.d(Constants.TAG, "MainActivity:Sync happened");
            Random ran = new Random();
            for (int i = 0; i < 1000; i++)
                syncSCWithServer(System.currentTimeMillis(), ran.nextInt(3), ran.nextInt(101),
                        "DEBUG");
            return true;
        }
        // do not disturb on
        else if (title.equalsIgnoreCase("Dnd1")) {
            Log.d(Constants.TAG, "MainActivity:Do not disturb on");
            showSnoozeAlert(Constants.DND_MODE_ON, item);
        }
        // do not disturb off
        else if (title.equalsIgnoreCase("Dnd2")) {
            Log.d(Constants.TAG, "MainActivity:Do not disturb on");
            showSnoozeAlert(Constants.DND_MODE_OFF, item);
        } else if (title.equalsIgnoreCase("About")) {
            Log.d(Constants.TAG, "MainActivity:Easter egg selected");
            Toast.makeText(getApplicationContext(), "UPMC Dash app", Toast.LENGTH_SHORT).show();
            easter++;
            if (easter == 3) {
                Log.d(Constants.TAG, "Congratulations! You are a developer now!");
                Intent intent = new Intent(this, EasterEgg.class);
                startActivity(intent);

            }
        }
        return super.onOptionsItemSelected(item);
    }


    public void syncSCWithServer(long timeStamp, int type, int data, String session_id) {
        ContentValues step_count = new ContentValues();
        step_count.put(Provider.Stepcount_Data.DEVICE_ID,
                Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
        step_count.put(Provider.Stepcount_Data.TIMESTAMP, timeStamp);
        step_count.put(Provider.Stepcount_Data.STEP_COUNT, data);
        step_count.put(Provider.Stepcount_Data.ALARM_TYPE, type);
        step_count.put(Provider.Stepcount_Data.SESSION_ID, session_id);
        getContentResolver().insert(Provider.Stepcount_Data.CONTENT_URI, step_count);
    }


    private void showSettings(final boolean firstRun) {
        Log.d(Constants.TAG, "MainActivity:showSettings: Joining Study: " + firstRun);
        setContentView(R.layout.settings_upmc_dash);
        ProgressBar progressBar = findViewById(R.id.progress_bar_schedule);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        final Button saveSchedule = findViewById(R.id.save_button);
        TimePicker mornPicker = findViewById(R.id.morning_start_time);
        TimePicker nightPicker = findViewById(R.id.night_sleep_time);
        if (firstRun) {
            saveSchedule.setText(R.string.join_btn_text);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mornPicker.setHour(9);
                mornPicker.setMinute(0);
                nightPicker.setHour(21);
                nightPicker.setMinute(0);
            } else {
                mornPicker.setCurrentHour(9);
                mornPicker.setCurrentMinute(0);
                nightPicker.setCurrentHour(21);
                nightPicker.setCurrentMinute(0);
            }

        } else {
            int morning_hour = Integer.parseInt(
                    Aware.getSetting(this, Settings.PLUGIN_UPMC_CANCER_MORNING_HOUR));
            int morning_minute = Integer.parseInt(
                    Aware.getSetting(this, Settings.PLUGIN_UPMC_CANCER_MORNING_MINUTE));
            int night_hour = Integer.parseInt(
                    Aware.getSetting(this, Settings.PLUGIN_UPMC_CANCER_NIGHT_HOUR));
            int night_minute = Integer.parseInt(
                    Aware.getSetting(this, Settings.PLUGIN_UPMC_CANCER_NIGHT_MINUTE));
            Log.d(Constants.TAG,
                    "UPMC:loadSchedule:savedTimes:" + morning_hour + "" + morning_minute + "" + night_hour + "" + night_minute);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mornPicker.setHour(morning_hour);
                mornPicker.setMinute(morning_minute);
                nightPicker.setHour(night_hour);
                nightPicker.setMinute(night_minute);
            } else {
                mornPicker.setCurrentHour(morning_hour);
                mornPicker.setCurrentMinute(morning_minute);
                nightPicker.setCurrentHour(night_hour);
                nightPicker.setCurrentMinute(night_minute);
            }
        }
        saveSchedule.setOnClickListener(view -> {
            saveSchedule.setText(R.string.join_btn_saving_txt);
            int morningHour;
            int morningMinute;
            int nightHour;
            int nightMinute;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                morningHour = mornPicker.getHour();
                morningMinute = mornPicker.getMinute();
                nightHour = nightPicker.getHour();
                nightMinute = nightPicker.getMinute();
            } else {
                morningHour = mornPicker.getCurrentHour();
                morningMinute = mornPicker.getCurrentMinute();
                nightHour = nightPicker.getCurrentHour();
                nightMinute = nightPicker.getCurrentMinute();
            }
            final Context context = getApplicationContext();
            boolean morning_hour;
            boolean morning_minute;
            boolean night_hour;
            boolean night_minute;
            try {
                morning_hour = Integer.parseInt(Aware.getSetting(context,
                        Settings.PLUGIN_UPMC_CANCER_MORNING_HOUR)) != morningHour;
                morning_minute = Integer.parseInt(Aware.getSetting(context,
                        Settings.PLUGIN_UPMC_CANCER_MORNING_MINUTE)) != morningMinute;
                night_hour = Integer.parseInt(Aware.getSetting(context,
                        Settings.PLUGIN_UPMC_CANCER_NIGHT_HOUR)) != nightHour;
                night_minute = Integer.parseInt(Aware.getSetting(context,
                        Settings.PLUGIN_UPMC_CANCER_NIGHT_MINUTE)) != nightMinute;
            } catch (NumberFormatException ex) {
                morning_hour = true;
                morning_minute = true;
                night_hour = true;
                night_minute = true;
            }
            if (morning_hour || morning_minute || night_hour || night_minute) {
                Aware.setSetting(context, Settings.PLUGIN_UPMC_CANCER_MORNING_HOUR,
                        "" + morningHour);
                Aware.setSetting(context, Settings.PLUGIN_UPMC_CANCER_MORNING_MINUTE,
                        "" + morningMinute);
                Aware.setSetting(context, Settings.PLUGIN_UPMC_CANCER_NIGHT_HOUR, "" + nightHour);
                Aware.setSetting(context, Settings.PLUGIN_UPMC_CANCER_NIGHT_MINUTE,
                        "" + nightMinute);
                if (firstRun) {
                    // if first run & study not joined, join the study
                    if (!Aware.isStudy(getApplicationContext())) {
                        isRegistered = true;
                        Log.d(Constants.TAG, "MainActivity:showSettings: Joining Study");
                        progressBar.setVisibility(View.VISIBLE);
                        Aware.joinStudy(getApplicationContext(),
                                "https://upmcdash.pittbotlab.org/aware-server/index" + ".php" +
                                        "/webservice/index/8/NPJHTw5kC255");
                    } else {
                        if (!isMyServiceRunning(FitbitMessageService.class))
                            sendFitbitMessageServiceAction(Constants.ACTION_FIRST_RUN);
                    }
                } else {
                    ContentValues settings = new ContentValues();
                    settings.put(Provider.Symptom_Data.DEVICE_ID,
                            Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
                    settings.put(Provider.Symptom_Data.TIMESTAMP, System.currentTimeMillis());
                    settings.put(Provider.Symptom_Data.TO_BED, nightHour + "h" + nightMinute + "m");
                    settings.put(Provider.Symptom_Data.FROM_BED,
                            morningHour + "h" + morningMinute + "m");
                    getContentResolver().insert(Provider.Symptom_Data.CONTENT_URI, settings);
                    // if not first time, settings have changed
                    Aware.startAWARE(getApplicationContext());  // need call startAware to apply
                    // the settings
                    Log.d(Constants.TAG, "MainActivity:showSettings: settings changed");
                    sendFitbitMessageServiceAction(Constants.ACTION_SETTINGS_CHANGED);
                    toastThanks(getApplicationContext());
                    finish();
                }
                // modifying the schedules
                engageScheduler();
            } else {
                // settings did not change, do nothing!
                Log.d(Constants.TAG, "MainActivity:showSettings: settings did not change");
                toastThanks(getApplicationContext());
                finish();
            }
        });

    }

    public void toastThanks(Context context) {
        Toast.makeText(context, "Thanks!", Toast.LENGTH_SHORT).show();
    }


    public void sendFitbitMessageServiceAction(String action) {
        Intent intent = new Intent(this, FitbitMessageService.class).setAction(action);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }


    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        assert manager != null;
        for (ActivityManager.RunningServiceInfo service : manager
                .getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private static class PostData extends AsyncTask<String, Void, Void> {


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }

        @Override
        protected Void doInBackground(String... strings) {
            Connection conn = null;
            Statement stmt = null;
            try {
                Class.forName("com.mysql.jdbc.Driver");
                conn = DriverManager.getConnection(DB_URL, USER, PASS);
                stmt = conn.createStatement();
                StringBuilder sb = new StringBuilder();
                sb.append("INSERT INTO ");
                sb.append(strings[0]);
                sb.append(" VALUES (null, '");
                sb.append(strings[1]);
                sb.append("')");
                stmt.executeUpdate(sb.toString());
                Log.d("yiyi", "Inserted records into the table...");

            } catch (SQLException se) {
                //Handle errors for JDBC
                se.printStackTrace();
            } catch (Exception e) {
                //Handle errors for Class.forName
                e.printStackTrace();
            } finally {
                //finally block used to close resources
                try {
                    if (stmt != null)
                        conn.close();
                } catch (SQLException se) {
                }// do nothing
                try {
                    if (conn != null)
                        conn.close();
                } catch (SQLException se) {
                    se.printStackTrace();
                }//end finally try
            }//end try
            return null;

        }
    }


    private class JoinedStudy extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(Constants.TAG, "MainActivity:JoinedStudy:onReceive:");
            if (intent != null) {
                if (intent.getAction().equalsIgnoreCase(Aware.ACTION_JOINED_STUDY)) {
                    Aware.setSetting(getApplicationContext(), Aware_Preferences.DEBUG_FLAG,
                            true); //enable logcat debug messages
                    Aware.setSetting(getApplicationContext(),
                            Aware_Preferences.WEBSERVICE_WIFI_ONLY, false);
                    Aware.setSetting(getApplicationContext(),
                            Aware_Preferences.WEBSERVICE_FALLBACK_NETWORK, 1);
                    Aware.setSetting(getApplicationContext(),
                            Aware_Preferences.FREQUENCY_WEBSERVICE, 15);
                    Aware.setSetting(getApplicationContext(),
                            Aware_Preferences.FREQUENCY_WEBSERVICE, 1);
                    Aware.setSetting(getApplicationContext(),
                            Aware_Preferences.FREQUENCY_CLEAN_OLD_DATA, 1);
                    Aware.setSetting(getApplicationContext(), Aware_Preferences.WEBSERVICE_SILENT,
                            true);
                    Aware.setSetting(getApplicationContext(),
                            Settings.PLUGIN_UPMC_CANCER_SYMPTOM_SEVERITY, String.valueOf(-1));
                    Aware.setSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_DND_MODE,
                            Constants.DND_MODE_OFF);
                    Aware.setSetting(getApplicationContext(),
                            Settings.PLUGIN_UPMC_CANCER_SHOW_MORNING, Constants.SHOW_NORMAL_SURVEY);
                    Aware.startPlugin(getApplicationContext(), "com.aware.plugin.upmc.dash");
                    Aware.isBatteryOptimizationIgnored(getApplicationContext(),
                            "com.aware" + ".plugin" + ".upmc.dash");
                    Applications.isAccessibilityServiceActive(getApplicationContext());
                    Toast.makeText(getApplicationContext(), "Joined Study!", Toast.LENGTH_SHORT)
                            .show();
                    Toast.makeText(getApplicationContext(), "ID:" + Aware
                                    .getSetting(getApplicationContext(),
                                            Aware_Preferences.DEVICE_ID),
                            Toast.LENGTH_SHORT).show();
                    String deviceType = Aware.getSetting(getApplicationContext(),
                            Settings.PLUGIN_UPMC_CANCER_DEVICE_TYPE);
                    switch (deviceType) {
                        case Constants.DEVICE_TYPE_REGULAR:
                            Aware.setSetting(getApplicationContext(),
                                    Aware_Preferences.DEVICE_LABEL, "device_regular");
                            break;
                        case Constants.DEVICE_TYPE_CONTROL:
                            Aware.setSetting(getApplicationContext(),
                                    Aware_Preferences.DEVICE_LABEL, "device_control");
                            break;
                    }
                    unregisterReceiver(joinedObserver);
                    isRegistered = false;
                    sendFitbitMessageServiceAction(Constants.ACTION_FIRST_RUN);
                    Account aware_account = Aware.getAWAREAccount(getApplicationContext());
                    String authority = Provider.getAuthority(getApplicationContext());
                    long frequency = Long.parseLong(Aware.getSetting(getApplicationContext(),
                            Aware_Preferences.FREQUENCY_WEBSERVICE)) * 60;
                    ContentResolver.setIsSyncable(aware_account, authority, 1);
                    ContentResolver.setSyncAutomatically(aware_account, authority, true);
                    SyncRequest request =
                            new SyncRequest.Builder().syncPeriodic(frequency, frequency / 3)
                                    .setSyncAdapter(aware_account, authority)
                                    .setExtras(new Bundle()).build();
                    ContentResolver.requestSync(request);
                    engageScheduler();
                    finish();
                }
            }

        }

    }
}