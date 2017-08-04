package com.aware.plugin.upmc.dash;

/**
 * Created by RaghuTeja on 6/23/17.
 */


public final class Constants {
    public static final long CONNECTION_TIME_OUT_MS = 4000;
    public static final String CONNECTION_PATH = "/upmc-dash";
    public static final String TAG = "DASH";
    public static final int UPMC_DASH_PHONE_DISCONNECTED = 1;
    public static final String CAPABILITY_PHONE_APP = "upmcdash_phoneapp";
    public static final String NODE_ID = "upmcdash_wearapp";

    public static final String ALARM_COMM = "AlarmComm";
    public static final String START_SC = "start_step_count";  // START STEP COUNT COMMAND
    public static final String COMM_KEY_NOTIF = "message";  // key in putExtra

    public static final String GET_STATUS_WEAR = "get_wear_status";


    //states

    public static final String STATUS_READY = "ready"; // ready to start logging

    public static final String STOP_SC = "stop_step_count";    // STOP STEP COUNT COMMAND

    public static final String KILL_DASH = "killeverything"; //KILL COMMANDS



    public static final String NOTIFICATION_RECEIVER_INTENT_FILTER = "upmc_notification_receiver";

    //local broadcast
    public static final String ALARM_LOCAL_RECEIVER_INTENT_FILTER = "alarm_local_broadcast_receiver";
    public static final int ALARM_TYPE_1HR = 0;
    public static final int ALARM_TYPE_2HR = 1;


}
