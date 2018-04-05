package com.aware.plugin.upmc.dash.utils;

import android.net.Uri;

/**
 * Created by RaghuTeja on 6/23/17.
 */


public final class Constants {
    public static final long CONNECTION_TIME_OUT_MS = 4000;
    public static final String TAG = "CANCER";
    public static final int UPMC_DASH_PHONE_DISCONNECTED = 1;
    public static final String CAPABILITY_PHONE_APP = "upmcdash_phoneapp";
    public static final String CAPABILITY_WEAR_APP = "upmcdash_wearapp";

    public static final String ALARM_COMM = "AlarmComm";
    public static final String START_SC = "start_step_count";  // START STEP COUNT COMMAND
    public static final String COMM_KEY_NOTIF = "message";  // key in putExtra


    public static final String PREFERENCES_DEFAULT_PHONE_NODEID = "pref_default_phone_nodeid";
    public static final String PREFERENCES_KEY_PHONE_NODEID = "pref_key_phone_nodeid";




    // Messages from phone
    public static final String IS_WEAR_RUNNING = "is_wear_running";
    public static final String INIT_TS = "init_time_and_symptoms"; // Morning timestamp plus
    public static final String ACK = "device_acknowledges";






    // messages to phone
    public static final String LOW_ACTIVITY = "step count below threshold";


    public static final String NOTIFY_INACTIVITY_SNOOZED = "user is inactive after snooze";





    //Bluetooth Communication
    public static final String BLUETOOTH_COMM = "bluetooth_comm";
    public static final String BLUETOOTH_COMM_KEY = "bluetooth_comm_key";


    //Shared PRefs
    public static final String MORNING_HOUR = "morning hour key";
    public static final String MORNING_MINUTE = "morning minute key";
    public static final String NIGHT_HOUR = "night hour key";
    public static final String NIGHT_MINUTE = "night minute key";



    //states
    public static final String STATE_INIT = "STATE_INIT";
    public static final String STATE_LOGGING = "STATE_LOGGING"; // currently logging
    public static final String STATE_INACTIVE = "STATE_INACTIVE"; //  currently paused



    public static final String NOTIFICATION_RECEIVER_INTENT_FILTER = "upmc_notification_receiver";

    //local broadcast
    public static final String ALARM_LOCAL_RECEIVER_INTENT_FILTER = "alarm_local_broadcast_receiver";
    public static final String FEEDBACK_BROADCAST_INTENT_FILTER = "feedback_broadcast_intent_filter";
    public static final int ALARM_TYPE_1HR = 0;
    public static final int ALARM_TYPE_2HR = 1;
    public static final String SENSOR_INTENT_FILTER = "sensor comm key";
    public static final String NOTIFY_INACTIVITY = "user is inactive";
    public static final String NOTIFY_GREAT_JOB = "user is active";
    public static final String SENSOR_EXTRA_KEY = "sensor intent comm";
    public static final String SENSOR_ALARM = "1hr/2hr sensor alarm";


    // notification messages
    public static final String FAILED_PHONE = "(Disconnected)\nPhone cannot display alerts";
    public static final String CONNECTED_PHONE = "(Connected)\nPhone will display alerts";
    public static final String FAILED_PHONE_BLUETOOTH = "Disconnected : Switch on Bluetooth";
    public static final String NOTIFTEXT_TRY_CONNECT = "Scanning for phone...";


    // Sensor Service Notification
    public static final String SS_MONITORING = "(Running)\n until night time";
    public static final String SS_NOT_MONITORING = "(Paused)\n until morning time";


    //sensors
    public static final int SENSOR_INTERVAL_1HR = 60 * 1000;
    public static final int SENSOR_INTERVAL_2HR = 120 * 1000;
    public static final String SENSOR_START_INTENT_KEY = "onstartcommmand intent extra";


    //Symptoms
    public static final int SYMPTOMS_0 = 0;
    public static final int SYMPTOMS_1 = 1;
    public static final int MINUTE_MONITORING = 2;
    public static final String SYMPTOMS_INTENT_FILTER = "symptoms filter";
    public static final String SYMPTOMS_KEY = "symptoms keys";
    public static final String SYMPTOMS_PREFS = "symptoms prefs";
    public static final String SYMP_RESET = "symptomschanged";


    // symptoms/time reset broadcast
    public static final String RESET_BROADCAST_INTENT_FILTER = "broadcast reset";
    public static final String TIME_RESET_KEY = "time reset key";
    public static final String SYMP_RESET_KEY = "symp reset key";


    //ConnectivityChecker
    public static final String CHECK_CONNECTIVITY_EXTRA_KEY = "ccik";
    public static final String ALARM_CHECK_CONN = "acc";



    public static final String MESSAGE_SERVICE_NOTIFICATION_CHANNEL_ID = "upmc_dash_message_service";

    //Notification IDs
    public static int MESSAGE_SERVICE_NOTIFICATION_ID = 20;


    //MessageService onStartCommand Actions
    public static final String ACTION_SETUP_WEAR = "setup_wear";
    public static final String ACTION_SCAN_PHONE = "ACTION_SCAN_PHONE";
    public static final String ACTION_FIRST_RUN = "first_run";
    public static final String ACTION_REBOOT = "action_reboot_run";
    public static final String ACTION_NOTIFY_INACTIVITY = "action_notify_user";
    public static final String ACTION_SYNC_DATA = "action_sync_data";
    public static final String ACTION_NOTIFY_GREAT_JOB = "action_notify_great_job";



    //SensorService actions

    public static final String ACTION_MINUTE_ALARM = "action_minute_alarm";
    public static final String ACTION_FEEDBACK_ALARM = "action_feedback_alarm";
    public static final String ACTION_NOTIF_SNOOZE = "SNOOZE_ACTION";
    public static final String ACTION_NOTIF_OK = "OK_ACTION";
    public static final String ACTION_NOTIF_NO = "NO_ACTION";
    public static final String ACTION_SNOOZE_ALARM = "ACTION_SNOOZE_ALARM";


    public static final String ACTION_NOTIF_SNOOZE_PHONE = "SNOOZE_ACTION_PHONE";
    public static final String ACTION_NOTIF_OK_PHONE = "OK_ACTION_PHONE";
    public static final String ACTION_NOTIF_NO_PHONE = "NO_ACTION_PHONE";


    //NotificationResponse actions
    public static final String ACTION_SHOW_SNOOZE = "actopm_show_all";


    //notifstuff
    public static final String SETUP_WEAR = "(HELLO!)\nUse your phone to begin setup";


    /**
     * Notification Channel IDs, IDs, Names, Descriptions
     *
     */


    //Channel IDs
    public static final String SESSION_STATUS_CHNL_ID = "session_status";
    public static final String INTERVENTION_NOTIF_CHNL_ID = "intervention_notification_wear";

    public static final String CAPABILITY_DEMO_WEAR_APP = "upmcdash_demo_wearapp";
    public static final String CAPABILITY_DEMO_PHONE_APP = "upmcdash_demo_phoneapp";




    // Channel Names
    public static final String SESSION_STATUS_CHNL_NAME = "UPMC Dash Session Status";
    public static final String INTERVENTION_NOTIF_CHNL_NAME = "UPMC Dash Wear intervention notification";

    //Channel Desc
    public static final String INTERVENTION_NOTIF_CHNL_DESC = "UPMC Dash Wear intervention notification";



    // Notif IDs
    public static int SESSION_STATUS_NOTIF_ID = 40;
    public static int INTERVENTION_NOTIF_ID = 50;



    // Demo mode Notification IDs
    public static final int DEMO_NOTIF_ID = 100;
    public static final String DEMO_NOTIF_CHNL_ID = "wear_demo_notification";
    public static final String DEMO_MODE = "Demo Mode";


    // intervention timeout

    public static int INTERVENTION_TIMEOUT = 15000;
    public static int DURATION_AWAKE = 15000;
    public static int DURATION_VIBRATE = 3000;

    public static Uri MESSAGE_URI = new Uri.Builder().scheme("wear").path("/upmc-dash").build();


















}
