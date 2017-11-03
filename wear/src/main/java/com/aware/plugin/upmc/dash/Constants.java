package com.aware.plugin.upmc.dash;

/**
 * Created by RaghuTeja on 6/23/17.
 */


public final class Constants {
    public static final long CONNECTION_TIME_OUT_MS = 4000;
    public static final String TAG = "CANCER";
    public static final int UPMC_DASH_PHONE_DISCONNECTED = 1;
    public static final String CAPABILITY_PHONE_APP = "upmcdash_phoneapp";
    public static final String NODE_ID = "upmcdash_wearapp";

    public static final String ALARM_COMM = "AlarmComm";
    public static final String START_SC = "start_step_count";  // START STEP COUNT COMMAND
    public static final String COMM_KEY_NOTIF = "message";  // key in putExtra



    // Messages from phone
    public static final String GET_STATUS_WEAR = "get_wear_status";
    public static final String IS_WEAR_RUNNING = "is wear running";
    public static final String STOP_SC = "stop_step_count";    // STOP STEP COUNT COMMAND
    public static final String KILL_DASH = "killeverything"; //KILL COMMANDS
    public static final String INIT_TS = "morningtimestamp&symptoms"; // Morning timestamp plus
    public static final String TIME_RESET = "resetconfig"; // Morning timestamp plus
    public static final String ACK = "device acknowledges";
    public static final String DEMO_NOTIF = "demonotif";
    public static final String OK_ACTION = "OK_ACTION";





    // messages to phone
    public static final String LOW_ACTIVITY = "step count below threshold";




    //Bluetooth Communication
    public static final String BLUETOOTH_COMM = "bluetooth_comm";
    public static final String BLUETOOTH_COMM_KEY = "bluetooth_comm_key";


    //Shared PRefs
    public static final String MORNING_HOUR = "morning hour key";
    public static final String MORNING_MINUTE = "morning minute key";
    public static final String NIGHT_HOUR = "night hour key";
    public static final String NIGHT_MINUTE = "night minute key";



    //states
    public static final String STATUS_INIT = "init";
    public static final String STATUS_LOGGING = "logging"; // currently logging



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
    public static final String NOTIFTEXT_SYNC_FAILED = "Sync Failed: Cannot display alerts on phone";
    public static final String NOTIFTEXT_SYNC_SUCCESS = "Synced: Phone will display alerts";
    public static final String NOTIFTEXT_SYNC_FAILED_BLUETOOTH = "Sync Failed : Switch on Bluetooth";
    public static final String NOTIFTEXT_SENDING_MESSAGE = "Sending message...";
    public static final String NOTIFTEXT_TRY_CONNECT = "Syncing with phone...";


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








}
