package com.aware.plugin.upmc.cancer;

import android.net.Uri;

/**
 * Created by RaghuTeja on 6/22/17.
 */

public final class Constants {

    public static final String CONNECTION_PATH = "/upmc-dash";
    public static final String TAG = "DASH";
    public static final String CAPABILITY_WEAR_APP = "upmcdash_wearapp";
    //local broadcast receivers/ MessageService stuff
    // intent filters for Broadcast Receivers
    public static final String LOCAL_MESSAGE_INTENT_FILTER = "upmcdash_phone_local_message";
    public static final String WEAR_STATUS_INTENT_FILTER = "upmcdash_phone_wear_status";
    public static final String NOTIFICATION_RECEIVER_INTENT_FILTER = "upmc_notification_receiver";
    public static final String NOTIFICATION_MESSAGE_INTENT_FILTER = "upmc_notif_intent_filter";


    public static final String START_SC = "start_step_count";  // START STEP COUNT COMMAND
    public static final String STOP_SC = "stop_step_count";    // STOP STEP COUNT COMMAND
    public static final String KILL_DASH = "killeverything"; //KILL COMMANDS



    public static final String COMM_KEY_UPMC = "message_upmc";  // key in putExtra
    public static final String COMM_KEY_NOTIF = "message_notif";  // key in putExtra

    public static final String COMM_KEY_MSGSERVICE = "message_msgservice";  // key in putExtra

    public static final String GET_STATUS_WEAR = "get_wear_status";


    public static final String STATUS_READY = "ready"; // ready to start logging


}
