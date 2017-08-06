package com.aware.plugin.upmc.dash;

/**
 * Created by RaghuTeja on 6/22/17.
 */

public final class Constants {

    public static final String CONNECTION_PATH = "/upmc-dash";
    public static final String TAG = "CANCER";
    public static final String CAPABILITY_WEAR_APP = "upmcdash_wearapp";
    //local broadcast receivers/ MessageService stuff
    // intent filters for Broadcast Receivers
    public static final String LOCAL_MESSAGE_INTENT_FILTER = "upmcdash_phone_local_message";
    public static final String WEAR_STATUS_INTENT_FILTER = "upmcdash_phone_wear_status";
    public static final String NOTIFICATION_RECEIVER_INTENT_FILTER = "upmc_notification_receiver";
    public static final String NOTIFICATION_MESSAGE_INTENT_FILTER = "upmc_notif_intent_filter";


    // Messages to wear
    public static final String START_SC = "start_step_count";  // START STEP COUNT COMMAND
    public static final String STOP_SC = "stop_step_count";    // STOP STEP COUNT COMMAND
    public static final String KILL_DASH = "killeverything"; //KILL COMMANDS

    //replies from wear

    public static final String WEAR_SERICE_RUNNING = "wear service is running";



    public static final String COMM_KEY_UPMC = "message_upmc";  // key in putExtra
    public static final String COMM_KEY_NOTIF = "message_notif";  // key in putExtra

    public static final String COMM_KEY_MSGSERVICE = "message_msgservice";  // key in putExtra

    public static final String GET_STATUS_WEAR = "get_wear_status";
    public static final String IS_WEAR_RUNNING = "is wear running";


    // states
    public static final String STATUS_READY = "ready"; // ready to start logging
    public static final String STATUS_LOGGING = "logging"; // currently logging
    public static final String STATUS_DISCONNECTED = "disconnected"; // currently disconnected

    //Bluetooth Communication
    public static final String BLUETOOTH_COMM = "bluetooth_comm";
    public static final String BLUETOOTH_COMM_KEY = "bluetooth_comm_key";


}
