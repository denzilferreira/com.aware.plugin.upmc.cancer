package com.aware.plugin.upmc.dash.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
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

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.plugin.upmc.dash.R;
import com.aware.plugin.upmc.dash.services.FitbitMessageService;
import com.aware.plugin.upmc.dash.settings.Settings;
import com.aware.plugin.upmc.dash.utils.Constants;
import com.aware.ui.PermissionsHandler;
import com.crashlytics.android.Crashlytics;
import com.ramotion.fluidslider.FluidSlider;

import java.util.ArrayList;

import io.fabric.sdk.android.Fabric;
import kotlin.Unit;

public class MainActivity extends AppCompatActivity {

    private boolean STUDYLESS_DEBUG = false;
    private boolean FIRST_RUN = true;
    private boolean isRegistered = false;
    private JoinedStudy joinedObserver = new JoinedStudy();


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

        FIRST_RUN = Aware.getSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_MORNING_HOUR).length() == 0;
    }


    public void hideOtherSlidersExcept(int except_id, int[] FS_IDS, int[] TVE_IDS, int[] IBE_IDS) {
        for (int i = 0; i < FS_IDS.length; i++) {
            if (i != except_id) {
                View view = findViewById(FS_IDS[i]);
                if (view.getVisibility() == View.VISIBLE) {
                    findViewById(IBE_IDS[i]).setVisibility(View.VISIBLE);
                    findViewById(TVE_IDS[i]).setVisibility(View.VISIBLE);
                    findViewById(FS_IDS[i]).setVisibility(View.GONE);
                }
            }
        }
    }



    @Override
    protected void onResume() {
        super.onResume();
        // Checking for more permissions
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
            // continue only if permissions are ok
            if (!STUDYLESS_DEBUG) {
                if (!Aware.IS_CORE_RUNNING) {
                    //This initialises the core framework, assigns Device ID if it doesn't exist yet, etc.
                    Log.d(Constants.TAG, "MainActivity:startAware");
                    Intent aware = new Intent(getApplicationContext(), Aware.class);
                    startService(aware);
                }

                if (!Aware.isStudy(getApplicationContext())) {
                    // if app hasn't joined the study yet
                    Log.d(Constants.TAG, "MainActivity:onResume:FIRST_RUN - registering join study observer");
                    IntentFilter filter = new IntentFilter(Aware.ACTION_JOINED_STUDY);
                    registerReceiver(joinedObserver, filter);
                    showSettings(true);
                }
                else {
                    showSymptomSurvey();
                }
            }
        }

    }


    public void showSymptomSurvey() {

        final int[] FS_IDS = {R.id.fluidSlider1, R.id.fluidSlider2, R.id.fluidSlider3, R.id.fluidSlider4,
                R.id.fluidSlider5, R.id.fluidSlider6, R.id.fluidSlider7, R.id.fluidSlider8,
                R.id.fluidSlider9, R.id.fluidSlider10, R.id.fluidSlider11};
        final int[] RG_IDS = {R.id.rg1, R.id.rg2, R.id.rg3, R.id.rg4,
                R.id.rg5, R.id.rg6, R.id.rg7, R.id.rg8,
                R.id.rg9, R.id.rg10, R.id.rg11
        };
        final int[] IBE_IDS = {R.id.ibe1, R.id.ibe2, R.id.ibe3, R.id.ibe4,
                R.id.ibe5, R.id.ibe6, R.id.ibe7, R.id.ibe8,
                R.id.ibe9, R.id.ibe10, R.id.ibe11
        };
        final int[] TVE_IDS = {R.id.editView1, R.id.editView2, R.id.editView3, R.id.editView4,
                R.id.editView5, R.id.editView6, R.id.editView7, R.id.editView8,
                R.id.editView9, R.id.editView10, R.id.editView11
        };
        final int[] RBP_IDS = {R.id.rbp1, R.id.rbp2, R.id.rbp3, R.id.rbp4,
                R.id.rbp5, R.id.rbp6, R.id.rbp7, R.id.rbp8,
                R.id.rbp9, R.id.rbp10, R.id.rbp11};
        setContentView(R.layout.activity_main);

        // UI stuff
        ScrollView sv = findViewById(R.id.scroll_view);
        for (int i = 0; i < FS_IDS.length; i++) {
            FluidSlider fs = findViewById(FS_IDS[i]);
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
                    hideOtherSlidersExcept(index, FS_IDS, TVE_IDS, IBE_IDS);
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
                                    findViewById(TVE_IDS[index]).setVisibility(View.VISIBLE);
                                }
                                , 750);
                        return Unit.INSTANCE;
                    });

                } else if (rb.getText().equals("No")) {
                    findViewById(IBE_IDS[index]).setVisibility(View.GONE);
                    findViewById(TVE_IDS[index]).setVisibility(View.GONE);
                    FluidSlider fs = findViewById(FS_IDS[index]);
                    if (fs.getVisibility() == View.VISIBLE) {
                        fs.setVisibility(View.GONE);
                    }
                }


            });

            findViewById(IBE_IDS[index]).setOnClickListener(view -> {
                hideOtherSlidersExcept(index, FS_IDS, TVE_IDS, IBE_IDS);
                Log.d("tv", "clicked");
                FluidSlider fs = findViewById(FS_IDS[index]);
                fs.setVisibility(View.VISIBLE);
                if ((index > 5))
                    sv.post(() -> sv.scrollTo(0, fs.getTop()));
            });

        }


        findViewById(R.id.submit).setOnClickListener(view -> {
            for (int i = 0; i < RG_IDS.length; i++) {
                RadioGroup rg = findViewById(RG_IDS[i]);

            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_upmc, menu);
        if (!STUDYLESS_DEBUG) {
            for (int i = 0; i < menu.size(); i++) {
                MenuItem item = menu.getItem(i);
                if (item.getTitle().toString().equalsIgnoreCase("Sync") && !Aware.isStudy(getApplicationContext())) {
                    item.setVisible(false);
                }
                if (item.getTitle().toString().equalsIgnoreCase("settings") && !Aware.isStudy(getApplicationContext())) {
                    item.setVisible(false);
                }
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
            showSettings(false);
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
            mBuilder.setPositiveButton("OK", (dialog, which) -> {
                if (device_label.getText().length() > 0 && !device_label.getText().toString().equals(Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_LABEL))) {
                    Aware.setSetting(getApplicationContext(), Aware_Preferences.DEVICE_LABEL, device_label.getText().toString());
                }
                dialog.dismiss();
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
            saveSchedule.setText("Join Study");
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
            int morning_hour = Integer.parseInt(Aware.getSetting(this, Settings.PLUGIN_UPMC_CANCER_MORNING_HOUR));
            int morning_minute = Integer.parseInt(Aware.getSetting(this, Settings.PLUGIN_UPMC_CANCER_MORNING_MINUTE));
            int night_hour = Integer.parseInt(Aware.getSetting(this, Settings.PLUGIN_UPMC_CANCER_NIGHT_HOUR));
            int night_minute = Integer.parseInt(Aware.getSetting(this, Settings.PLUGIN_UPMC_CANCER_NIGHT_MINUTE));
            Log.d(Constants.TAG, "UPMC:loadSchedule:savedTimes:" + morning_hour + "" + morning_minute + "" + night_hour + "" + night_minute);
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
            saveSchedule.setText("Saving Schedule....");
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
                morning_hour = Integer.parseInt(Aware.getSetting(context, Settings.PLUGIN_UPMC_CANCER_MORNING_HOUR)) != morningHour;
                morning_minute = Integer.parseInt(Aware.getSetting(context, Settings.PLUGIN_UPMC_CANCER_MORNING_MINUTE)) != morningMinute;
                night_hour = Integer.parseInt(Aware.getSetting(context, Settings.PLUGIN_UPMC_CANCER_NIGHT_HOUR)) != nightHour;
                night_minute = Integer.parseInt(Aware.getSetting(context, Settings.PLUGIN_UPMC_CANCER_NIGHT_MINUTE)) != nightMinute;
            } catch (NumberFormatException ex) {
                Log.d(Constants.TAG, "Exception!");
                morning_hour = true;
                morning_minute = true;
                night_hour = true;
                night_minute = true;
            }
            if (morning_hour || morning_minute || night_hour || night_minute) {
                Aware.setSetting(context, Settings.PLUGIN_UPMC_CANCER_MORNING_HOUR, "" + morningHour);
                Aware.setSetting(context, Settings.PLUGIN_UPMC_CANCER_MORNING_MINUTE, "" + morningMinute);
                Aware.setSetting(context, Settings.PLUGIN_UPMC_CANCER_NIGHT_HOUR, "" + nightHour);
                Aware.setSetting(context, Settings.PLUGIN_UPMC_CANCER_NIGHT_MINUTE, "" + nightMinute);

                if (firstRun) {
                    // if first run & study not joined, join the study
                    if (!Aware.isStudy(getApplicationContext())) {
                        isRegistered = true;
                        Log.d(Constants.TAG, "MainActivity:showSettings: Joining Study");
                        progressBar.setVisibility(View.VISIBLE);
                        Aware.joinStudy(getApplicationContext(), "https://r2d2.hcii.cs.cmu.edu/aware/dashboard/index.php/webservice/index/118/TKKPrzN2s0km");
                    } else {
                        if (!isMyServiceRunning(FitbitMessageService.class))
                            sendFitbitMessageServiceAction(Constants.ACTION_FIRST_RUN);
                    }
                } else {
                    // if not first time, settings have changed
                    Log.d(Constants.TAG, "MainActivity:showSettings: settings changed" );
                    sendFitbitMessageServiceAction(Constants.ACTION_SETTINGS_CHANGED);
                    toastThanks(getApplicationContext());
                    finish();
                }
            }
            else {
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
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }


    private class JoinedStudy extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(Constants.TAG, "MainActivity:JoinedStudy:onReceive:");
            if (intent.getAction().equalsIgnoreCase(Aware.ACTION_JOINED_STUDY)) {
                Aware.setSetting(getApplicationContext(), Aware_Preferences.DEBUG_FLAG, true); //enable logcat debug messages
                Aware.setSetting(getApplicationContext(), Aware_Preferences.WEBSERVICE_WIFI_ONLY, true);
                Aware.setSetting(getApplicationContext(), Aware_Preferences.WEBSERVICE_FALLBACK_NETWORK, 6);
                Aware.setSetting(getApplicationContext(), Aware_Preferences.FREQUENCY_WEBSERVICE, 30);
                Aware.setSetting(getApplicationContext(), Aware_Preferences.FREQUENCY_CLEAN_OLD_DATA, 1);
                Aware.setSetting(getApplicationContext(), Aware_Preferences.WEBSERVICE_SILENT, true);
                Aware.setSetting(getApplicationContext(), Aware_Preferences.DEBUG_FLAG, false);
                Aware.startPlugin(getApplicationContext(), "com.aware.plugin.upmc.dash");
                Aware.isBatteryOptimizationIgnored(getApplicationContext(), "com.aware.plugin.upmc.dash");
                Toast.makeText(getApplicationContext(), "Joined Study!", Toast.LENGTH_SHORT).show();
                Toast.makeText(getApplicationContext(), "ID:" + Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID), Toast.LENGTH_SHORT).show();
                if (Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_LABEL).length() == 0) {
                    Aware.setSetting(getApplicationContext(), Aware_Preferences.DEVICE_LABEL, "upmc_dash_user");

                }
                unregisterReceiver(joinedObserver);
                isRegistered = false;
//                sendFitbitMessageServiceAction(Constants.ACTION_FIRST_RUN);


//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                    Aware.setSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_MORNING_HOUR, morning_timer.getHour());
//                    Aware.setSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_MORNING_MINUTE, morning_timer.getMinute());
//                    Aware.setSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_NIGHT_HOUR, night_timer.getHour());
//                    Aware.setSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_NIGHT_MINUTE, night_timer.getMinute());
//                } else {
//                    Aware.setSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_MORNING_HOUR, morning_timer.getCurrentHour());
//                    Aware.setSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_MORNING_MINUTE, morning_timer.getCurrentMinute());
//                    Aware.setSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_NIGHT_HOUR, night_timer.getCurrentHour());
//                    Aware.setSetting(getApplicationContext(), Settings.PLUGIN_UPMC_CANCER_NIGHT_MINUTE, night_timer.getCurrentMinute());
//                }
////                Aware.startPlugin(getApplicationContext(), "com.aware.plugin.google.activity_recognition");
//                Aware.startPlugin(getApplicationContext(), "com.aware.plugin.upmc.dash");
//                //Ask accessibility to be activated
//                Applications.isAccessibilityServiceActive(getApplicationContext());
//                Aware.isBatteryOptimizationIgnored(getApplicationContext(), "com.aware.plugin.upmc.dash");
//
//                unregisterReceiver(joinedObserver);
//                isRegistered = false;
//                Toast.makeText(getApplicationContext(), "Joined Study!", Toast.LENGTH_SHORT).show();
//                Toast.makeText(getApplicationContext(), "ID:" + Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID), Toast.LENGTH_SHORT).show();
//                Log.d(Constants.TAG, "ID: " + Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
//
//                if (Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_LABEL).length() == 0) {
//                    Aware.setSetting(getApplicationContext(), Aware_Preferences.DEVICE_LABEL, "upmc_dash_user");
//
//                }
//                String label = Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_LABEL);
//                String prefix = getDeviceTypePostfix();
//                Aware.setSetting(getApplicationContext(), Aware_Preferences.DEVICE_LABEL, label + prefix);
//                if (readDeviceType().equals(Constants.DEVICE_TYPE_FITBIT)) {
//                    sendFitbitMessageServiceAction(Constants.ACTION_FIRST_RUN);
////                    startFitbitCheckPromptAlarm();
//                } else
//                    sendMessageServiceAction(Constants.ACTION_FIRST_RUN);


                finish();
            }
        }


    }
}


