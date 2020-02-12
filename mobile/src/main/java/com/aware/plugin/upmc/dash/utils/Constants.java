package com.aware.plugin.upmc.dash.utils;

/**
 * Created by RaghuTeja on 6/22/17.
 */

public final class Constants {


    public static final String SNOOZE_ON_ALERT =
            "Are you sure you want to disable notifications to walk for the rest of the day?";
    public static final String SNOOZE_OFF_ALERT =
            "Are you sure you want to resume notifications to walk?";


    public static final String SNOOZE_ALERT_POS = "Confirm";
    public static final String SNOOZE_ALERT_NEG = "Cancel";


    public static final String DEVICE_TYPE_REGULAR = "DEVICE_TYPE_REGULAR";
    public static final String DEVICE_TYPE_CONTROL = "DEVICE_TYPE_CONTROL";


    public static final String PREFERENCES_KEY_DEVICE_TYPE = "PREFERENCES_KEY_DEVICE_TYPE";
    public static final String PREFERENCES_DEFAULT_DEVICE_TYPE = "PREFERENCES_DEFAULT_DEVICE_TYPE";


    //scheduler ids
    public static final String MORNING_SURVEY_SCHED_ID = "cancer_survey_morning";
    public static final String EVENING_SYNC_ID = "cancer_sync_data";
    // survey notification contents


    public static final String COMPLETE_SURVEY_CONTENT = "Tap to report";


    public static final String SELF_REPORT_CONTENT = "Tap to edit";
    public static final String NOTIF_RESPONSE_EXTRA_KEY = "NOTIF_RESPONSE_EXTRA_KEY";
    public static final int NOTIF_TYPE_INACTIVITY = 1;
    public static final int NOTIF_DEVICE_PHONE = 0;
    public static final int NOTIF_DEVICE_WATCH = 1;
    public static final int NOTIF_TYPE_BATT_LOW = 2;
    public static final int SNOOZE_SHOWN = 1;
    public static final int SNOOZE_NOT_SHOWN = 0;
    public static final int NOTIF_TYPE_APPRAISAL = 0;
    public static final int DND_TOGGLE_MANUAL = 1;
    public static final int DND_TOGGLE_AUTO = 0;
    public static final String SHOW_MORNING_SURVEY = "SHOW_MORNING_SURVEY";
    public static final String SHOW_NORMAL_SURVEY = "SHOW_NORMAL_SURVEY";
    public static final String FITBIT_STATUS_CHNL_ID = "FITBIT_STATUS_CHNL_ID";
    public static final CharSequence FITBIT_STATUS_CHNL_NAME = "UPMC Dash Fitbit Status Channel";
    public static final String FITBIT_STATUS_CHNL_DESC = "Fitbit notifications";
    public static final String SELF_REPORT_TITLE = "UPMC Dash Self-Report";
    // survey notification titles
    public static final String COMPLETE_SURVEY_TITLE = "UPMC Dash - Survey";
    public static final String TAG = "CANCER";
    public static final String ACTION_SHOW_SNOOZE = "action_show_snooze";
    public static final String ACTION_SHOW_INABILITY = "action_show_inability";
    public static final String ACTION_STOP_SELF = "ACTION_STOP_SELF";
    public static final String ACTION_SYNC_DATA = "sync_sensor_data";
    public static final String ACTION_TEST1 = "ACTION_TEST1";
    public static final String ACTION_TEST2 = "ACTION_TEST2";
    // Ok Action
    public static final String ACTION_NOTIF_SNOOZE = "SNOOZE_ACTION";
    public static final String ACTION_NOTIF_OK = "OK_ACTION";
    //No Action
    public static final String ACTION_NOTIF_NO = "NO_ACTION";
    public static final String ACTION_FIRST_RUN = "first_run";
    // Tired response
    public static final String ACTION_APPRAISAL = "action_appraisal_ok";
    public static final String ACTION_INACTIVITY = "action_inactivity_responses";
    public static final String ACTION_SETTINGS_CHANGED = "action_settings_changed";
    public static final String ACTION_REBOOT = "action_reboot";
    // Wear setup notif
    public static final String SETUP_WEAR = "Tap to setup watch";
    public static final String CONNECTED_WEAR = "Connected: Watch is monitoring your activity..";
    public static final String FAILED_WEAR = "Disconnected: Watch is not in range";
    public static final String NOTIF_APPRAISAL = "Great job being active!";
    //Wear monitor notif
    public static final String NOTIF_INACTIVITY = "Ready for a short walk?";
    /**
     * Notification Channels & Ids
     */
    // channel IDs
    public static final String INTERVENTION_NOTIF_CHNL_ID = "intervention_notification";
    public static final String INTERVENTION_NOTIF_CHNL_NAME = "UPMC Dash Intervention Channel";
    public static final String INTERVENTION_NOTIF_CHNL_DESC = "UPMC Dash intervention notification";
    // Notification IDs
    public static final int SURVEY_NOTIF_ID = 1;
    public static final int SETUP_NOTIF_ID = 2;
    public static final int INTERVENTION_NOTIF_ID = 3;
    // Demo mode Notification IDs
    public static final int DEMO_NOTIF_ID = 10;
    public static final String DEMO_NOTIF_CHNL_ID = "demo_notification";
    public static final String SURVEY_NOTIF_CHNL_NAME = "UPMC Dash Survey Channel";
    public static final String SURVEY_NOTIF_CHNL_ID = "survey_notification";
    public static final String SURVEY_NOTIF_CHNL_DESC = "Notifications for daily surveys.";
    public static final String SELF_REPORT_NOTIF_CHNL_NAME = "UPMC Dash Self-Report Channel";
    public static final String SELF_REPORT_CHNL_ID = "selfreport_notification";
    public static final String SELF_REPORT_CHNL_DESC = "Notification for self-reports.";
    //  Database credentials
    public static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    public static final String HOST_URL = "jdbc:mysql://localhost";
    public static final String DB_NAME = "UPMC";
    public static final String DB_URL = "jdbc:mysql://localhost/UPMC";
    // fitbit stuff
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
    public static final String DO_NOT_DISTURB_COMMAND = "Do Not Disturb On";
    public static final String REMOVE_DO_NOT_DISTURB = "Do Not Disturb Off";
    // Messages received from the watch
    public static final String NOTIFICATION = "Ready?";
    public static final String NOTIF_NO_SNOOZE = "Ready!";
    public static final String MINIMESSAGE = "Great job!";
    public static final String CLOSE_NOTIF = "Close";
    public static final String OTHER = "Other";
    public static final String TABLE_SENSOR_DATA = "SensorData";
    public static final String TABLE_RESPONSES = "responses_watch";
    public static final String TABLE_INTERVENTIONS = "interventions_watch";
    // Package names of apps required on the phone
    public static final String PACKAGE_KSWEB = "ru.kslabs.ksweb";
    public static final String PACKAGE_FITBIT = "com.fitbit.FitbitMobile";
    // Fitbit notif
    public static final String FAILED_WEAR_FITBIT = "Disconnected: Relaunching Fitbit app...";
    public static final String FAILED_WEAR_BLUETOOTH =
            "Disconnected: Reconnecting the bluetooth...";
    public static final String BLUETOOTH_ON = "Bluetooth reconnected.";
    public static final String CONN_STATUS = "Fitbit watch connection";
    public static final String CONTENT_TITLE_FITBIT = "UPMC Dash Fitbit";
    //MessageService ACTIONS
    public static final String ACTION_CHECK_PROMPT = "check_prompt";
    public static final String ACTION_CHECK_CONN = "check_watch_connection";
    public static final String ACTION_SURVEY_COMPLETED = "notify_survey_completed";
    public static final String ACTION_DO_NOT_DISTURB = "ACTION_DO_NOT_DISTURB";
    // Survey notif
    public static final String SURVEY_COMPLETED =
            "Survey already completed. Tap to complete again.";
    // Database and local server setup commands
    public static final String LIGHTTPD_START = "ru.kslabs.ksweb.CMD.LIGHTTPD_START";
    public static final String LIGHTTPD_ADD_HOST = "ru.kslabs.ksweb.CMD.LIGHTTPD_ADD_HOST";
    public static final String DATA_KEY = "DATA";
    public static final String TAG_KEY = "TAG";
    //wakelock
    public static final String WAKELOCK_TAG = "upmcdash:notifwakelock";
    public static final String DND_MODE_ON = "DND_MODE_ON";
    public static final String DND_MODE_OFF = "DND_MODE_OFF";
    public static final String INCOMPLETE_SYMPS_TEXT = "Symptom ratings not complete";
    public static final String SURVEY_CONTENT_TEXT = "UPMC DASH SURVEY";
    public static final String ACTION_TEST3 = "ACTION_TEST3";
    public static final String ACTION_TEST4 = "ACTION_TEST4";
    public static final String ACTION_TEST5 = "ACTION_TEST5";
    public static String ACTION_SHOW_MORNING = "ACTION_SHOW_MORNING";
    public static int INTERVENTION_TIMEOUT = 5 * 60 * 1000;
    public static int DURATION_AWAKE = 15000;
    public static int DURATION_VIBRATE = 3000;


}
