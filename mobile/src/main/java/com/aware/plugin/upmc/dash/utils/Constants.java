package com.aware.plugin.upmc.dash.utils;

/**
 * Created by RaghuTeja on 6/22/17.
 */

public final class Constants {






    public static final String TAG = "CANCER";
    public static final String CAPABILITY_WEAR_APP = "upmcdash_wearapp";
    //local broadcast receivers/ MessageService stuff
    // intent filters for Broadcast Receivers

    public static final String NOTIFICATION_MESSAGE_INTENT_FILTER = "upmc_notif_intent_filter";


    // Messages to wear
    public static final String IS_WEAR_RUNNING = "is wear running";
    public static final String INIT_TS = "morningtimestamp&symptoms"; // Morning timestamp plus
    public static final String ACK = "device acknowledges";

    //replies from wear

    public static final String COMM_KEY_UPMC = "message_upmc";  // key in putExtra

    //messages from wear
    public static final String NOTIFY_INACTIVITY = "user is inactive";
    public static final String NOTIFY_GREAT_JOB = "user is active";


    // states from wear
    public static final String STATUS_INIT = "init"; // need to be time initiated
    public static final String STATUS_LOGGING = "logging"; // currently logging

    //Bluetooth Communication
    public static final String BLUETOOTH_COMM = "bluetooth_comm";
    public static final String BLUETOOTH_COMM_KEY = "bluetooth_comm_key";








    // Snooze Action

    public static final String ACTION_NOTIF_SNOOZE = "SNOOZE_ACTION";
    public static final String ALARM_COMM = "AlarmComm";
    public static final String SNOOZE_ALARM_INTENT_FILTER = "snooze alarm intent filter";
    public static final String SNOOZE_ALARM_EXTRA_KEY = "snooze alarm extra key";
    public static final String SNOOZE_ALARM_EXTRA = "snooze alarm extra";


    // Ok Action
    public static final String ACTION_NOTIF_OK = "OK_ACTION";



    //No Action
    public static final String ACTION_NOTIF_NO = "NO_ACTION";





    //MessageService Actions
    public static final String ACTION_SETUP_WEAR = "setup_wear";



    //

    public static final String ACTION_SNOOZE = "action_snooze";



    //vicinity check
    public static final String VICINITY_CHECK_INTENT_FILTER = "vcheckfilter";
    public static final String VICINITY_RESULT_KEY = "vicinity result key";
    public static final int WEAR_IN_RANGE = 1;
    public static final int WEAR_NOT_IN_RANGE = 0;
    public static final int WEAR_VICINITY_CHECK_FAILED = -1;

    //complete survey

    public static final String COMPLETE_SURVEY = "Tap to begin your survey";

    //MessageService ACTIONS
    public static final String ACTION_SURVEY = "action_survey";
    public static final String ACTION_FIRST_RUN = "first_run";
    public static final String ACTION_APPRAISAL = "action_appraisal_ok";
    public static final String ACTION_INACTIVITY = "action_inactivity_responses";
    public static final String ACTION_VICINITY = "action_vicinity";
    public static final String ACTION_SETTINGS_CHANGED = "action_settings_changed";
    public static final String ACTION_REBOOT = "action_reboot";



    // Wear setup notif
    public static final String SETUP_WEAR = "Tap to setup watch";
    public static final String CONNECTED_WEAR = "Connected: Watch is logging your activity.. ";
    public static final String FAILED_WEAR = "Disconnected: Tap to reconnect";
    public static final String FAILED_WEAR_BLUETOOTH = "Disconnected: Switch on Bluetooth";


    //Wear monitor notif

    public static final String NOTIF_APPRAISAL = "Great Job! You have been active";
    public static final String NOTIF_INACTIVITY =  "Ready for a quick walk?";

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







    // Loading Activity Stuff

    public static final String LOADING_ACTIVITY_INTENT_FILTER = "LOADING_ACTIVITY_INTENT_FILTER";
    public static final String MESSAGE_EXTRA_KEY = "MESSAGE_EXTRA_KEY";
    public static final String LOADING_ACTIVITY_CONNECTED_MESSAGE = "Your watch is  synced! You will receive alerts.";
    public static final String LOADING_ACTIVITY_FAILED_MESSAGE = "Setup failed! Please try again by tapping the setup notification later.";







}
