package com.aware.plugin.upmc.dash.utils;

import android.net.Uri;

/**
 * Created by RaghuTeja on 6/22/17.
 */

public final class Constants {

    public static final String DEVICE_TYPE_FITBIT = "DEVICE_TYPE_FITBIT";
    public static final String DEVICE_TYPE_ANDROID = "DEVICE_TYPE_ANDROID";

    public static final String PREFERENCES_KEY_DEVICE_TYPE = "PREFERENCES_KEY_DEVICE_TYPE";
    public static final String PREFERENCES_DEFAULT_DEVICE_TYPE = "PREFERENCES_DEFAULT_DEVICE_TYPE";



    public static final String TAG = "CANCER";
    public static final String CAPABILITY_WEAR_APP = "upmcdash_wearapp";
    public static final String CAPABILITY_DEMO_WEAR_APP = "upmcdash_demo_wearapp";
    public static final String CAPABILITY_PHONE_APP= "upmcdash_phoneapp";

    public static final String CAPABILITY_DEMO_PHONE_APP = "upmcdash_demo_phoneapp";

    public static final String PREFERENCES_KEY_WEAR_NODEID = "pref_key_wear_nodeid";
    public static final String PREFERENCES_DEFAULT_WEAR_NODEID = "pref_default_wear_nodeid";


    //local broadcast receivers/ MessageService stuff
    // intent filters for Broadcast Receivers

    public static final String NOTIFICATION_MESSAGE_INTENT_FILTER = "upmc_notif_intent_filter";
    public static final String ACTION_SYNC_SETTINGS = "ACTION_SYNC_SETTINGS";



    // Messages to wear
    public static final String IS_WEAR_RUNNING = "is_wear_running";
    public static final String ACK = "device_acknowledges";

    //replies from wear

    public static final String COMM_KEY_UPMC = "message_upmc";  // key in putExtra

    //messages from wear
    public static final String NOTIFY_INACTIVITY = "user is inactive";
    public static final String NOTIFY_INACTIVITY_SNOOZED = "user is inactive after snooze";

    public static final String NOTIFY_GREAT_JOB = "user is active";


    //states
    public static final String STATE_INIT = "STATE_INIT";
    public static final String STATE_LOGGING = "STATE_LOGGING"; // currently logging
    public static final String STATE_INACTIVE = "STATE_INACTIVE"; //  currently paused

    //Bluetooth Communication
    public static final String BLUETOOTH_COMM = "bluetooth_comm";
    public static final String BLUETOOTH_COMM_KEY = "bluetooth_comm_key";




    public static final String ACTION_SHOW_SNOOZE = "action_show_snooze";


    public static final String ACTION_SHOW_INABILITY = "action_show_inability";


    public static final String ACTION_STOP_SELF = "ACTION_STOP_SELF";





    // Snooze Action

    public static final String ALARM_COMM = "AlarmComm";
    public static final String SNOOZE_ALARM_INTENT_FILTER = "snooze alarm intent filter";
    public static final String SNOOZE_ALARM_EXTRA_KEY = "snooze alarm extra key";
    public static final String SNOOZE_ALARM_EXTRA = "snooze alarm extra";


    // Ok Action
    public static final String ACTION_NOTIF_SNOOZE = "SNOOZE_ACTION";

    public static final String ACTION_NOTIF_OK = "OK_ACTION";



    //No Action
    public static final String ACTION_NOTIF_NO = "NO_ACTION";





    //MessageService Actions
    public static final String ACTION_SETUP_WEAR = "setup_wear";

    //

    public static final String ACTION_INIT = "ACTION_INIT";

    public static final String ACTION_SNOOZE_ALARM = "ACTION_SNOOZE_ALARM";



    //vicinity check
    public static final String VICINITY_CHECK_INTENT_FILTER = "vcheckfilter";
    public static final String VICINITY_RESULT_KEY = "vicinity result key";
    public static final int WEAR_IN_RANGE = 1;
    public static final int WEAR_NOT_IN_RANGE = 0;
    public static final int WEAR_VICINITY_CHECK_FAILED = -1;

    // survey notification contents

    public static final String COMPLETE_SURVEY_CONTENT = "Tap to begin your survey";


    public static final String SELF_REPORT_CONTENT = "Tap to begin your self-report";


    // survey notification titles
    public static final String SELF_REPORT_TITLE = "UPMC Dash Self-Report";
    public static final String COMPLETE_SURVEY_TITLE = "UPMC Dash - Survey";

    //MessageService ACTIONS
    public static final String ACTION_FIRST_RUN = "first_run";
    public static final String ACTION_APPRAISAL = "action_appraisal_ok";
    public static final String ACTION_INACTIVITY = "action_inactivity_responses";
    public static final String ACTION_VICINITY = "action_vicinity";
    public static final String ACTION_SETTINGS_CHANGED = "action_settings_changed";
    public static final String ACTION_REBOOT = "action_reboot";



    // Demo mode

    public static final String DEMO_MODE = "Demo mode active";



    // Wear setup notif
    public static final String SETUP_WEAR = "Tap to setup watch";
    public static final String CONNECTED_WEAR = "Connected: Watch is monitoring your activity..";
    public static final String FAILED_WEAR = "Disconnected: Watch is not in range";
    public static final String FAILED_WEAR_DISCOVERY = "Setup Failed: Tap to retry";


    //Wear monitor notif

    public static final String NOTIF_APPRAISAL = "Great job being active!";
    public static final String NOTIF_INACTIVITY =  "Ready for a short walk?";

    /**
     *
     *   Notification Channels & Ids
     *
     */
    // channel IDs
    public static final String SURVEY_NOTIF_CHNL_ID = "survey_notification";

    public static final String INTERVENTION_NOTIF_CHNL_ID = "intervention_notification";
    public static final String INTERVENTION_NOTIF_CHNL_NAME = "UPMC Dash intervention notification";
    public static final String INTERVENTION_NOTIF_CHNL_DESC = "UPMC Dash intervention notification";


    // Notification IDs
    public static final int SURVEY_NOTIF_ID = 1;
    public static final int SETUP_NOTIF_ID = 2;
    public static final int INTERVENTION_NOTIF_ID = 3;


    // Demo mode Notification IDs
    public static final int DEMO_NOTIF_ID = 10;
    public static final String DEMO_NOTIF_CHNL_ID = "demo_notification";



    // show morning questions

    public static String ACTION_SHOW_MORNING = "ACTION_SHOW_MORNING";








    // Loading Activity Stuff

    public static final String LOADING_ACTIVITY_INTENT_FILTER = "LOADING_ACTIVITY_INTENT_FILTER";
    public static final String MESSAGE_EXTRA_KEY = "MESSAGE_EXTRA_KEY";
    public static final String LOADING_ACTIVITY_CONNECTED_MESSAGE = "Your watch is  synced! You will receive alerts.";
    public static final String LOADING_ACTIVITY_FAILED_MESSAGE = "Setup Complete! However, your watch is not reachable";



    public static int INTERVENTION_TIMEOUT = 5 * 60 * 1000;
    public static int DURATION_AWAKE = 15000;
    public static int DURATION_VIBRATE = 3000;

    public static Uri MESSAGE_URI = new Uri.Builder().scheme("wear").path("/upmc-dash").build();





    // fitbit stuff



    //  Database credentials
    public static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    public static final String HOST_URL = "jdbc:mysql://localhost";
    public static final String DB_NAME = "UPMC";
    public static final String DB_URL = "jdbc:mysql://localhost/UPMC";
    public static final String USER = "root";
    public static final String PASS = "";

    // Table names
    public static final String TABLE_PS = "PatientSurvey";
    public static final String TABLE_TS = "TimeSchedule";
    public static final String TABLE_PROMPT = "PromptFromWatch";
    public static final String TABLE_CONN = "Connection";
    public static final String TABLE_COMMAND = "CommandFromPhone";

    // Messages sent to the watch
    public static final String CASE1 = "<7";
    public static final String CASE2 = ">=7";
    public static final String SNOOZE_COMMAND = "Snooze";
    public static final String CLOSE_COMMAND = "Close";

    // Messages received from the watch
    public static final String NOTIFICATION = "Ready?";
    public static final String NOTIF_NO_SNOOZE = "Ready!";
    public static final String MINIMESSAGE = "Great job!";
    public static final String CLOSE_NOTIF = "Close";

    // Package names of apps required on the phone
    public static final String PACKAGE_KSWEB = "ru.kslabs.ksweb";
    public static final String PACKAGE_FITBIT = "com.fitbit.FitbitMobile";

    // Fitbit notif
    public static final String FAILED_WEAR_FITBIT = "Disconnected: Relaunching Fitbit app...";
    public static final String FAILED_WEAR_BLUETOOTH = "Disconnected: Reconnecting the bluetooth...";
    public static final String BLUETOOTH_ON = "Bluetooth reconnected.";
    public static final String CONN_STATUS = "Fitbit watch connection";
    public static final String CONTENT_TITLE_FITBIT = "UPMC Dash Fitbit";

    //MessageService ACTIONS
    public static final String ACTION_CHECK_PROMPT = "check_prompt";
    public static final String ACTION_CHECK_CONN = "check_watch_connection";
    public static final String ACTION_SURVEY_COMPLETED = "notify_survey_completed";

    // Survey notif
    public static final String SURVEY_COMPLETED = "Survey already completed. Tap to complete again.";

    // Database and local server setup commands
    public static final String LIGHTTPD_START = "ru.kslabs.ksweb.CMD.LIGHTTPD_START";
    public static final String LIGHTTPD_ADD_HOST = "ru.kslabs.ksweb.CMD.LIGHTTPD_ADD_HOST";
    public static final String DATA_KEY = "DATA";
    public static final String TAG_KEY = "TAG";





}
