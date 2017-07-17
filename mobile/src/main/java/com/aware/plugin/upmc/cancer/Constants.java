package com.aware.plugin.upmc.cancer;

/**
 * Created by RaghuTeja on 6/22/17.
 */

public final class Constants {
    public static final long CONNECTION_TIME_OUT_MS = 4000;
    public static final String CONNECTION_PATH = "/upmc-dash";
    public static final String TAG = "DASH";
    public static final String CAPABILITY_WEAR_APP = "upmcdash_wearapp";

    public static final String NODE_ID = "upmcdash_phoneapp";

    //local broadcast receivers/ MessageService stuff
    // intent filters for Broadcast Receivers
    public static final String LOCAL_MESSAGE_INTENT_FILTER = "upmcdash_phone_local_message";
    public static final String WEAR_STATUS_INTENT_FILTER = "upmcdash_phone_wear_status";
    public static final String NOTIFICATION_INTENT_FILTER = "upmc_notification_receiver";
    public static final String NOTIFICATION_MESSAGE_INTENT_FILTER = "upmc_notif_intent_filter";


    public static final String START_SC = "start_step_count";  // START STEP COUNT COMMAND
    public static final String COMM_KEY = "message";  // key in putExtra

    public static final String STATUS_WEAR = "get_wear_status";


}
