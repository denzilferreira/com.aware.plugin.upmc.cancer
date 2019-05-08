package com.aware.plugin.upmc.dash.activities;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

import com.aware.Aware;
import com.aware.plugin.upmc.dash.utils.Constants;
import com.aware.utils.DatabaseHelper;

import java.util.HashMap;

public class Provider extends ContentProvider {
    public static String AUTHORITY = "com.aware.plugin.upmc.dash.provider.survey"; //change to package.provider.your_plugin_name
    public static final int DATABASE_VERSION = 28; //increase this if you make changes to the database structure, i.e., rename columns, etc.
    public static String DATABASE_NAME = "plugin_upmc_dash.db"; //the database filename, use plugin_xxx for plugins.

    //Add here your database table names, as many as you need
    public static final String[] DATABASE_TABLES = {
            "upmc_dash_surveys",
            "upmc_dash_stepcount",
            "upmc_dash_interventions",
            "upmc_dash_responses",
            "upmc_dash_dndtoggle"
    };


    private static final int ANSWERS = 1;
    private static final int ANSWERS_ID = 2;
    private static final int STEPCOUNT = 3;
    private static final int STEPCOUNT_ID = 4;
    private static final int INTERVENTION = 5;
    private static final int INTERVENTION_ID = 6;
    private static final int RESPONSE = 7;
    private static final int RESPONSE_ID = 8;
    private static final int DND = 9;
    private static final int DND_ID = 10;



    // These are the columns that we need to sync data, don't change this!
    public interface AWAREColumns extends BaseColumns {
        String _ID = "_id";
        String TIMESTAMP = "timestamp";
        String DEVICE_ID = "device_id";
    }

    public static final class Symptom_Data implements AWAREColumns {
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/upmc_dash_surveys");
        static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.upmc.dash";
        static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.upmc.dash";
        static final String TO_BED = "to_bed";
        static final String FROM_BED = "from_bed";
        static final String SCORE_PAIN = "score_pain";
        static final String SCORE_FATIGUE = "score_fatigue";
        static final String SCORE_SLEEP_DISTURBANCE = "score_sleep_dist";
        static final String SCORE_CONCENTRATING = "score_concentrating";
        static final String SCORE_SAD = "score_sad";
        static final String SCORE_ANXIOUS = "score_anxious";
        static final String SCORE_SHORT_BREATH = "score_short_breath";
        static final String SCORE_NUMBNESS = "score_numbness";
        static final String SCORE_NAUSEA = "score_nausea";
        static final String SCORE_DIARRHEA = "score_diarrhea";
        static final String SCORE_DIZZY = "score_dizzy";
        static final String SCORE_OTHER = "score_other";
        static final String OTHER_LABEL = "other_label";
    }


    public static final class Stepcount_Data implements AWAREColumns {
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/upmc_dash_stepcount");
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.upmc.dash.stepcount";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.upmc.dash.stepcount";
        public static final String STEP_COUNT = "stepcount";
        public static final String ALARM_TYPE = "alarmtype";
        public static final String SESSION_ID = "session_id";
    }

    public static final class Notification_Interventions implements AWAREColumns {
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/upmc_dash_interventions");
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.upmc.dash.interventions";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.upmc.dash.interventions";
        public static final String NOTIF_ID = "session_id";
        public static final String NOTIF_TYPE = "notif_type";
        public static final String NOTIF_DEVICE = "notif_device";
        public static final String SNOOZE_SHOWN = "snooze_shown";



    }

    public static final class Notification_Responses implements AWAREColumns {
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/upmc_dash_responses");
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.upmc.dash.responses";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.upmc.dash.responses";
        public static final String NOTIF_ID = "session_id";
        public static final String NOTIF_TYPE = "notif_type";
        public static final String NOTIF_DEVICE = "notif_device";
        public static final String RESP_OK = "resp_ok";
        public static final String RESP_NO = "resp_no";
        public static final String RESP_SNOOZE = "resp_snooze";
        public static final String RESP_BUSY = "resp_busy";
        public static final String RESP_PAIN = "resp_pain";
        public static final String RESP_NAUSEA = "resp_nausea";
        public static final String RESP_TIRED = "resp_tired";
        public static final String RESP_OTHER = "resp_other";
        public static final String RESP_OTHER_SYMP = "resp_other_symp";

    }


    public static final class Dnd_Toggle implements AWAREColumns {
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/upmc_dash_dndtoggle");
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.upmc.dash.dndtoggle";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.upmc.dash.dndtoggle";
        public static final String TOGGLE_POS = "toggle_pos";
        public static final String TOGGLED_BY = "toggled_by";

    }




    public static final String[] TABLES_FIELDS = {
            Symptom_Data._ID + " integer primary key autoincrement," +
                    Symptom_Data.TIMESTAMP + " real default 0," +
                    Symptom_Data.DEVICE_ID + " text default ''," +
                    Symptom_Data.TO_BED + " text default ''," +
                    Symptom_Data.FROM_BED + " text default ''," +
                    Symptom_Data.SCORE_PAIN + " text default ''," +
                    Symptom_Data.SCORE_FATIGUE + " text default ''," +
                    Symptom_Data.SCORE_SLEEP_DISTURBANCE + " text default ''," +
                    Symptom_Data.SCORE_CONCENTRATING + " text default ''," +
                    Symptom_Data.SCORE_SAD + " text default ''," +
                    Symptom_Data.SCORE_ANXIOUS + " text default ''," +
                    Symptom_Data.SCORE_SHORT_BREATH + " text default ''," +
                    Symptom_Data.SCORE_NUMBNESS + " text default ''," +
                    Symptom_Data.SCORE_NAUSEA + " text default ''," +
                    Symptom_Data.SCORE_DIARRHEA + " text default ''," +
                    Symptom_Data.SCORE_DIZZY + " text default ''," +
                    Symptom_Data.SCORE_OTHER + " text default ''," +
                    Symptom_Data.OTHER_LABEL + " text default ''",


            Stepcount_Data._ID + " integer primary key autoincrement," +
                    Stepcount_Data.TIMESTAMP + " real default 0," +
                    Stepcount_Data.DEVICE_ID + " text default ''," +
                    Stepcount_Data.STEP_COUNT + " integer default -1," +
                    Stepcount_Data.ALARM_TYPE + " integer default -1," +
                    Stepcount_Data.SESSION_ID + " text default ''",

            Notification_Interventions._ID + " integer primary key autoincrement," +
                    Notification_Interventions.TIMESTAMP + " real default 0," +
                    Notification_Interventions.DEVICE_ID + " text default ''," +
                    Notification_Interventions.NOTIF_ID + " text default ''," +
                    Notification_Interventions.NOTIF_TYPE + " integer default -1," +
                    Notification_Interventions.NOTIF_DEVICE + " integer default -1," +
                    Notification_Interventions.SNOOZE_SHOWN + " integer default -1",




            Notification_Responses._ID + " integer primary key autoincrement," +
                    Notification_Responses.TIMESTAMP + " real default 0," +
                    Notification_Responses.DEVICE_ID + " text default ''," +
                    Notification_Responses.NOTIF_ID + " text default ''," +
                    Notification_Responses.NOTIF_TYPE + " integer default -1," +
                    Notification_Responses.NOTIF_DEVICE + " integer default -1," +
                    Notification_Responses.RESP_OK + " integer default -1," +
                    Notification_Responses.RESP_NO + " integer default -1," +
                    Notification_Responses.RESP_SNOOZE + " integer default -1," +
                    Notification_Responses.RESP_BUSY + " integer default -1," +
                    Notification_Responses.RESP_PAIN + " integer default -1," +
                    Notification_Responses.RESP_NAUSEA + " integer default -1," +
                    Notification_Responses.RESP_TIRED + " integer default -1," +
                    Notification_Responses.RESP_OTHER + " integer default -1," +
                    Notification_Responses.RESP_OTHER_SYMP + " text default ''",

            Dnd_Toggle._ID + " integer primary key autoincrement," +
                    Dnd_Toggle.TIMESTAMP + " real default 0," +
                    Dnd_Toggle.DEVICE_ID + " text default ''," +
                    Dnd_Toggle.TOGGLE_POS + " integer default -1," +
                    Dnd_Toggle.TOGGLED_BY + " integer default -1"



    };

    private static UriMatcher sUriMatcher = null;
    private static HashMap<String, String> surveyMap = null;
    private static HashMap<String, String> stepcountMap = null;
    private static HashMap<String, String> interventionsMap = null;
    private static HashMap<String, String> respMap = null;
    private static HashMap<String, String> dndMap = null;
    private DatabaseHelper dbHelper = null;
    private static SQLiteDatabase database = null;

    private void initialiseDatabase() {
        if (dbHelper == null)
            dbHelper = new DatabaseHelper(getContext(), DATABASE_NAME, null, DATABASE_VERSION, DATABASE_TABLES, TABLES_FIELDS);
        if (database == null)
            database = dbHelper.getWritableDatabase();
    }

    @Override
    public boolean onCreate() {
        AUTHORITY = getContext().getPackageName() + ".provider.survey";

        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[0], ANSWERS);
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[0] + "/#", ANSWERS_ID);
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[1], STEPCOUNT);
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[1] + "/#", STEPCOUNT_ID);
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[2], INTERVENTION);
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[2] + "/#", INTERVENTION_ID);
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[3], RESPONSE);
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[3] + "/#", RESPONSE_ID);
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[4], DND);
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[4] + "/#", DND_ID);

        surveyMap = new HashMap<>();
        surveyMap.put(Symptom_Data._ID, Symptom_Data._ID);
        surveyMap.put(Symptom_Data.TIMESTAMP, Symptom_Data.TIMESTAMP);
        surveyMap.put(Symptom_Data.DEVICE_ID, Symptom_Data.DEVICE_ID);
        surveyMap.put(Symptom_Data.TO_BED, Symptom_Data.TO_BED);
        surveyMap.put(Symptom_Data.FROM_BED, Symptom_Data.FROM_BED);
        surveyMap.put(Symptom_Data.SCORE_PAIN, Symptom_Data.SCORE_PAIN);
        surveyMap.put(Symptom_Data.SCORE_FATIGUE, Symptom_Data.SCORE_FATIGUE);
        surveyMap.put(Symptom_Data.SCORE_SLEEP_DISTURBANCE, Symptom_Data.SCORE_SLEEP_DISTURBANCE);
        surveyMap.put(Symptom_Data.SCORE_CONCENTRATING, Symptom_Data.SCORE_CONCENTRATING);
        surveyMap.put(Symptom_Data.SCORE_SAD, Symptom_Data.SCORE_SAD);
        surveyMap.put(Symptom_Data.SCORE_ANXIOUS, Symptom_Data.SCORE_ANXIOUS);
        surveyMap.put(Symptom_Data.SCORE_SHORT_BREATH, Symptom_Data.SCORE_SHORT_BREATH);
        surveyMap.put(Symptom_Data.SCORE_NUMBNESS, Symptom_Data.SCORE_NUMBNESS);
        surveyMap.put(Symptom_Data.SCORE_NAUSEA, Symptom_Data.SCORE_NAUSEA);
        surveyMap.put(Symptom_Data.SCORE_DIARRHEA, Symptom_Data.SCORE_DIARRHEA);
        surveyMap.put(Symptom_Data.SCORE_DIZZY, Symptom_Data.SCORE_DIZZY);
        surveyMap.put(Symptom_Data.SCORE_OTHER, Symptom_Data.SCORE_OTHER);
        surveyMap.put(Symptom_Data.OTHER_LABEL, Symptom_Data.OTHER_LABEL);


        stepcountMap = new HashMap<>();
        stepcountMap.put(Stepcount_Data._ID, Stepcount_Data._ID);
        stepcountMap.put(Stepcount_Data.TIMESTAMP, Stepcount_Data.TIMESTAMP);
        stepcountMap.put(Stepcount_Data.DEVICE_ID, Stepcount_Data.DEVICE_ID);
        stepcountMap.put(Stepcount_Data.STEP_COUNT, Stepcount_Data.STEP_COUNT);
        stepcountMap.put(Stepcount_Data.ALARM_TYPE, Stepcount_Data.ALARM_TYPE);
        stepcountMap.put(Stepcount_Data.SESSION_ID, Stepcount_Data.SESSION_ID);



        interventionsMap = new HashMap<>();
        interventionsMap.put(Notification_Interventions._ID, Notification_Interventions._ID);
        interventionsMap.put(Notification_Interventions.TIMESTAMP, Notification_Interventions.TIMESTAMP);
        interventionsMap.put(Notification_Interventions.DEVICE_ID, Notification_Interventions.DEVICE_ID);
        interventionsMap.put(Notification_Interventions.NOTIF_ID, Notification_Interventions.NOTIF_ID);
        interventionsMap.put(Notification_Interventions.NOTIF_TYPE, Notification_Interventions.NOTIF_TYPE);
        interventionsMap.put(Notification_Interventions.NOTIF_DEVICE, Notification_Interventions.NOTIF_DEVICE);
        interventionsMap.put(Notification_Interventions.SNOOZE_SHOWN, Notification_Interventions.SNOOZE_SHOWN);




        respMap = new HashMap<>();
        respMap.put(Notification_Responses._ID, Notification_Responses._ID);
        respMap.put(Notification_Responses.TIMESTAMP, Notification_Responses.TIMESTAMP);
        respMap.put(Notification_Responses.DEVICE_ID, Notification_Responses.DEVICE_ID);
        respMap.put(Notification_Responses.NOTIF_ID, Notification_Responses.NOTIF_ID);
        respMap.put(Notification_Responses.NOTIF_TYPE, Notification_Responses.NOTIF_TYPE);
        respMap.put(Notification_Responses.NOTIF_DEVICE, Notification_Responses.NOTIF_DEVICE);
        respMap.put(Notification_Responses.RESP_OK, Notification_Responses.RESP_OK);
        respMap.put(Notification_Responses.RESP_NO, Notification_Responses.RESP_NO);
        respMap.put(Notification_Responses.RESP_SNOOZE, Notification_Responses.RESP_SNOOZE);
        respMap.put(Notification_Responses.RESP_BUSY, Notification_Responses.RESP_BUSY);
        respMap.put(Notification_Responses.RESP_PAIN, Notification_Responses.RESP_PAIN);
        respMap.put(Notification_Responses.RESP_NAUSEA, Notification_Responses.RESP_NAUSEA);
        respMap.put(Notification_Responses.RESP_TIRED, Notification_Responses.RESP_TIRED);
        respMap.put(Notification_Responses.RESP_OTHER, Notification_Responses.RESP_OTHER);
        respMap.put(Notification_Responses.RESP_OTHER_SYMP, Notification_Responses.RESP_OTHER_SYMP);


        dndMap = new HashMap<>();
        dndMap.put(Dnd_Toggle._ID, Dnd_Toggle._ID);
        dndMap.put(Dnd_Toggle.TIMESTAMP, Dnd_Toggle.TIMESTAMP);
        dndMap.put(Dnd_Toggle.DEVICE_ID, Dnd_Toggle.DEVICE_ID);
        dndMap.put(Dnd_Toggle.TOGGLE_POS, Dnd_Toggle.TOGGLE_POS);
        dndMap.put(Dnd_Toggle.TOGGLED_BY, Dnd_Toggle.TOGGLED_BY);

        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        initialiseDatabase();

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        Log.d(Constants.TAG, "RATM: " +sUriMatcher.match(uri));
        switch (sUriMatcher.match(uri)) {
            case ANSWERS:
                qb.setTables(DATABASE_TABLES[0]);
                qb.setProjectionMap(surveyMap);
                break;
            case STEPCOUNT:
                qb.setTables(DATABASE_TABLES[1]);
                qb.setProjectionMap(stepcountMap);
                break;
            case INTERVENTION:
                qb.setTables(DATABASE_TABLES[2]);
                qb.setProjectionMap(interventionsMap);
                break;
            case RESPONSE:
                qb.setTables(DATABASE_TABLES[3]);
                qb.setProjectionMap(respMap);
                break;
            case DND:
                qb.setTables(DATABASE_TABLES[4]);
                qb.setProjectionMap(dndMap);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        try {

            Cursor c = qb.query(database, projection, selection, selectionArgs,
                    null, null, sortOrder);
            c.setNotificationUri(getContext().getContentResolver(), uri);
            return c;
        } catch (IllegalStateException e) {
            if (Aware.DEBUG)
                Log.e(Aware.TAG, e.getMessage());

            return null;
        }
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case ANSWERS:
                return Symptom_Data.CONTENT_TYPE;
            case ANSWERS_ID:
                return Symptom_Data.CONTENT_ITEM_TYPE;
            case STEPCOUNT:
                return Stepcount_Data.CONTENT_TYPE;
            case STEPCOUNT_ID:
                return Stepcount_Data.CONTENT_ITEM_TYPE;
            case INTERVENTION:
                return Notification_Interventions.CONTENT_TYPE;
            case INTERVENTION_ID:
                return Notification_Interventions.CONTENT_ITEM_TYPE;
            case RESPONSE:
                return Notification_Responses.CONTENT_TYPE;
            case RESPONSE_ID:
                return Notification_Responses.CONTENT_ITEM_TYPE;
            case DND:
                return Dnd_Toggle.CONTENT_TYPE;
            case DND_ID:
                return Dnd_Toggle.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        initialiseDatabase();
        if (database == null) return null;

        ContentValues values = (initialValues != null) ? new ContentValues(initialValues) : new ContentValues();

        database.beginTransaction();

        switch (sUriMatcher.match(uri)) {
            case ANSWERS:
                long quest_id = database.insertWithOnConflict(DATABASE_TABLES[0],
                        Symptom_Data.DEVICE_ID, values, SQLiteDatabase.CONFLICT_IGNORE);
                database.setTransactionSuccessful();
                database.endTransaction();
                if (quest_id > 0) {
                    Uri questUri = ContentUris.withAppendedId(Symptom_Data.CONTENT_URI,
                            quest_id);
                    getContext().getContentResolver().notifyChange(questUri, null, false);
                    return questUri;
                }
                database.endTransaction();
                throw new SQLException("Failed to insert row into " + uri);
            case STEPCOUNT:
                long step_id = database.insertWithOnConflict(DATABASE_TABLES[1],
                        Stepcount_Data.DEVICE_ID, values, SQLiteDatabase.CONFLICT_IGNORE);
                database.setTransactionSuccessful();
                database.endTransaction();
                if (step_id > 0) {
                    Uri stepUri = ContentUris.withAppendedId(Stepcount_Data.CONTENT_URI,
                            step_id);
                    getContext().getContentResolver().notifyChange(stepUri, null, false);
                    return stepUri;
                }
                database.endTransaction();
                throw new SQLException("Failed to insert row into " + uri);

            case INTERVENTION:
                long interv_id = database.insertWithOnConflict(DATABASE_TABLES[2],
                        Notification_Interventions.DEVICE_ID, values, SQLiteDatabase.CONFLICT_IGNORE);
                database.setTransactionSuccessful();
                database.endTransaction();
                if (interv_id > 0) {
                    Uri intervUri = ContentUris.withAppendedId(Notification_Interventions.CONTENT_URI,
                            interv_id);
                    getContext().getContentResolver().notifyChange(intervUri, null, false);
                    return intervUri;
                }
                database.endTransaction();
                throw new SQLException("Failed to insert row into " + uri);

            case RESPONSE:
                long resp_id = database.insertWithOnConflict(DATABASE_TABLES[3],
                        Notification_Responses.DEVICE_ID, values, SQLiteDatabase.CONFLICT_IGNORE);
                database.setTransactionSuccessful();
                database.endTransaction();
                if (resp_id > 0) {
                    Uri respUri = ContentUris.withAppendedId(Notification_Responses.CONTENT_URI,
                            resp_id);
                    getContext().getContentResolver().notifyChange(respUri, null, false);
                    return respUri;
                }
                database.endTransaction();
                throw new SQLException("Failed to insert row into " + uri);

            case DND:
                long dnd_id = database.insertWithOnConflict(DATABASE_TABLES[4],
                        Dnd_Toggle.DEVICE_ID, values, SQLiteDatabase.CONFLICT_IGNORE);
                database.setTransactionSuccessful();
                database.endTransaction();
                if (dnd_id > 0) {
                    Uri dndUri = ContentUris.withAppendedId(Dnd_Toggle.CONTENT_URI,
                            dnd_id);
                    getContext().getContentResolver().notifyChange(dndUri, null, false);
                    return dndUri;
                }
                database.endTransaction();
                throw new SQLException("Failed to insert row into " + uri);
            default:
                database.endTransaction();
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        initialiseDatabase();
        if (database == null) return 0;

        database.beginTransaction();

        int count;
        switch (sUriMatcher.match(uri)) {
            case ANSWERS:
                count = database.delete(DATABASE_TABLES[0], selection, selectionArgs);
                break;
            case STEPCOUNT:
                count = database.delete(DATABASE_TABLES[1], selection, selectionArgs);
                break;
            case INTERVENTION:
                count = database.delete(DATABASE_TABLES[2], selection, selectionArgs);
                break;
            case RESPONSE:
                count = database.delete(DATABASE_TABLES[3], selection, selectionArgs);
            break;
            case DND:
                count = database.delete(DATABASE_TABLES[4], selection, selectionArgs);
                break;
            default:
                database.endTransaction();
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        database.setTransactionSuccessful();
        database.endTransaction();

        getContext().getContentResolver().notifyChange(uri, null, false);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        initialiseDatabase();
        if (database == null) return 0;

        database.beginTransaction();

        int count;
        switch (sUriMatcher.match(uri)) {
            case ANSWERS:
                count = database.update(DATABASE_TABLES[0], values, selection, selectionArgs);
                break;
            case STEPCOUNT:
                count = database.update(DATABASE_TABLES[1], values, selection, selectionArgs);
                break;
            case INTERVENTION:
                count = database.update(DATABASE_TABLES[2], values, selection, selectionArgs);
                break;
            case RESPONSE:
                count = database.update(DATABASE_TABLES[3], values, selection, selectionArgs);
                break;
            case DND:
                count = database.update(DATABASE_TABLES[4], values, selection, selectionArgs);
                break;
            default:
                database.endTransaction();
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        database.setTransactionSuccessful();
        database.endTransaction();
        getContext().getContentResolver().notifyChange(uri, null, false);
        return count;
    }

    public static String getAuthority(Context context) {
        AUTHORITY = context.getPackageName() + ".provider.survey";
        return AUTHORITY;
    }
}
