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
    public static final String MORNING_TIME = "morningtimestamp"; // Morning timestamp plus
    public static final String MORNING_TIME_RESET = "resetmorningtimestamp"; // Morning timestamp plus
    public static final String ACK = "device acknowledges";

    //replies from wear

    public static final String COMM_KEY_UPMC = "message_upmc";  // key in putExtra
    public static final String COMM_KEY_NOTIF = "message_notif";  // key in putExtra
    public static final String COMM_KEY_MSGSERVICE = "message_msgservice";  // key in putExtra

    //messages from wear
    public static final String NOTIFY_INACTIVITY = "user is inactive";


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
    public static final String NOTIFTEXT_SENDING_MESSAGE = "Sending message...";
    public static final String NOTIFTEXT_IN_PROGRESS = "Watch is logging your activity..";
    public static final String NOTIFTEXT_TRY_CONNECT = "Syncing with watch...";


    //Broadcasts from UPMC Settings Panel
    public static final String SETTINGS_COMM = "settings comm";
    public static final String SETTING_INTENT_FILTER = "settings intent filter";
    public static final String SETTINGS_CHANGED = "settings panel changed";



    public static final String MORNING_HOUR = "morning hour key";
    public static final String MORNING_MINUTE = "morning minute key";

}
