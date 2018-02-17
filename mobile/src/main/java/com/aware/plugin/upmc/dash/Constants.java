package com.aware.plugin.upmc.dash;

/**
 * Created by RaghuTeja on 6/22/17.
 */

public final class Constants {






    public static final String TAG = "CANCER";
    public static final String CAPABILITY_WEAR_APP = "upmcdash_wearapp";
    //local broadcast receivers/ MessageService stuff
    // intent filters for Broadcast Receivers

    public static final String NOTIFICATION_RECEIVER_INTENT_FILTER = "upmc_notification_receiver";
    public static final String NOTIFICATION_MESSAGE_INTENT_FILTER = "upmc_notif_intent_filter";


    // Messages to wear
    public static final String GET_STATUS_WEAR = "get_wear_status";
    public static final String IS_WEAR_RUNNING = "is wear running";
    public static final String STOP_SC = "stop_step_count";    // STOP STEP COUNT COMMAND
    public static final String KILL_DASH = "killeverything"; //KILL COMMANDS
    public static final String INIT_TS = "morningtimestamp&symptoms"; // Morning timestamp plus
    public static final String TIME_RESET = "resetconfig"; // Morning timestamp plus
    public static final String ACK = "device acknowledges";
    public static final String SYMP_RESET = "symptomschanged";
    public static final String DEMO_NOTIF = "demonotif";

    //replies from wear

    public static final String COMM_KEY_UPMC = "message_upmc";  // key in putExtra
    public static final String COMM_KEY_NOTIF = "message_notif";  // key in putExtra
    public static final String COMM_KEY_MSGSERVICE = "message_msgservice";  // key in putExtra

    //messages from wear
    public static final String NOTIFY_INACTIVITY = "user is inactive";
    public static final String NOTIFY_GREAT_JOB = "user is active";


    // states
    public static final String STATUS_INIT = "init"; // need to be time initiated
    public static final String STATUS_LOGGING = "logging"; // currently logging
    public static final String STATUS_DISCONNECTED = "disconnected"; // currently disconnected

    //Bluetooth Communication
    public static final String BLUETOOTH_COMM = "bluetooth_comm";
    public static final String BLUETOOTH_COMM_KEY = "bluetooth_comm_key";


    // notification messages
    public static final String NOTIFTEXT_SYNC_FAILED = "Sync Failed: Cannot display alerts from watch";
    public static final String NOTIFTEXT_SYNC_SUCCESS = "Synced with watch";
    public static final String NOTIFTEXT_SYNC_FAILED_BLUETOOTH = "Sync Failed : Switch on Bluetooth";
    public static final String NOTIFTEXT_IN_PROGRESS = "Watch is logging your activity..";
    public static final String NOTIFTEXT_TRY_CONNECT = "Scanning for watch...";


    //Broadcasts from UPMC Settings Panel
    public static final String SETTINGS_EXTRA_KEY = "settings comm";
    public static final String SETTING_INTENT_FILTER = "settings intent filter";
    public static final String VICINITY_CHECK = "vcheck";
    public static final String SETTINGS_CHANGED = "settings panel changed";



    public static final String MORNING_HOUR = "morning hour key";
    public static final String MORNING_MINUTE = "morning minute key";
    public static final String NIGHT_HOUR = "night hour key";
    public static final String NIGHT_MINUTE = "night minute key";

    //Symptoms
    public static final int SYMPTOMS_0 = 0;
    public static final int SYMPTOMS_1 = 1;
    public static final String SYMPTOMS_INTENT_FILTER = "symptoms filter";
    public static final String SYMPTOMS_KEY = "symptoms keys";
    public static final String SYMPTOMS_PREFS = "symptoms prefs";


    //NotifReceiver
    public static final String NOTIF_COMM = "notif_comm";
    public static final String NOTIF_KEY = "notif_key";



    // Snooze Action

    public static final String SNOOZE_ACTION = "SNOOZE_ACTION";
    public static final String ALARM_COMM = "AlarmComm";
    public static final String SNOOZE_ALARM_INTENT_FILTER = "snooze alarm intent filter";
    public static final String SNOOZE_ALARM_EXTRA_KEY = "snooze alarm extra key";
    public static final String SNOOZE_ALARM_EXTRA = "snooze alarm extra";


    // Ok Action
    public static final String OK_ACTION = "OK_ACTION";

    public static final String OK_ACTION_GJ = "OK_ACTION_GJ";


    //No Action
    public static final String NO_ACTION = "NO_ACTION";

    //Retry Action
    public static final String RETRY_ACTION = "RETRY_ACTION";




    //MessageService Actions
    public static final String ACTION_SETUP_WEAR = "setup_wear";



    //vicinity check
    public static final String VICINITY_CHECK_INTENT_FILTER = "vcheckfilter";
    public static final String VICINITY_INTENT_FILTER = "vicinity intent filter";
    public static final String VICINITY_RESULT_KEY = "vicinity result key";
    public static final int WEAR_IN_RANGE = 1;
    public static final int WEAR_NOT_IN_RANGE = 0;
    public static final int WEAR_VICINITY_CHECK_FAILED = -1;

    //complete survey

    public static final String COMPLETE_SURVEY = "Tap to begin your survey";
    public static final String ACTION_SURVEY = "action_survey";
    public static final String ACTION_FIRST_RUN = "first_run";


    // Wear setup
    public static final String SETUP_WEAR = "Tap to setup wear";

    //msg_comm
    public static final String DEMO_MODE = "demomodemsgservice";


    // Kill demo
    public static final String KILL_DEMO = "killdemo";





    //Notification Channel ID
    public static final String NOTIFICATION_CHANNEL_ID = "upmc_dash";






}
