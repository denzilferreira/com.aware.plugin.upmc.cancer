package com.aware.plugin.upmc.dash;

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
import com.aware.utils.DatabaseHelper;

import java.util.HashMap;

/**
 * Created by denzil on 25/11/
 * <p>
 * <p>
 * CREATE TABLE `upmc_dash`.`upmc_dash_stepcount` (
 * `_id` INT(11) NOT NULL AUTO_INCREMENT,
 * `timestamp` DOUBLE NULL DEFAULT '0',
 * `device_id` VARCHAR(45) NULL DEFAULT '',
 * `stepcount` INT NULL DEFAULT NULL,
 * `alarmtype` INT NULL DEFAULT NULL,
 * PRIMARY KEY (`_id`));
 */
public class Provider extends ContentProvider {

    public static String AUTHORITY = "com.aware.plugin.upmc.dash.provider.survey"; //change to package.provider.your_plugin_name

    public static final int DATABASE_VERSION = 10; //increase this if you make changes to the database structure, i.e., rename columns, etc.

    public static String DATABASE_NAME = "plugin_upmc_dash.db"; //the database filename, use plugin_xxx for plugins.

    //Add here your database table names, as many as you need
    public static final String[] DATABASE_TABLES = {
            "upmc_dash",
            "upmc_dash_motivation",
            "upmc_dash_stepcount"
    };


    private static final int ANSWERS = 1;
    private static final int ANSWERS_ID = 2;
    private static final int MOTIVATIONS = 3;
    private static final int MOTIVATIONS_ID = 4;
    private static final int STEPCOUNT = 5;
    private static final int STEPCOUNT_ID = 6;


    // These are the columns that we need to sync data, don't change this!
    public interface AWAREColumns extends BaseColumns {
        String _ID = "_id";
        String TIMESTAMP = "timestamp";
        String DEVICE_ID = "device_id";
    }

    public static final class Symptom_Data implements AWAREColumns {
        private Symptom_Data() {
        }

        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/upmc_dash");
        static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.upmc.dash";
        static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.upmc.dash";
        static final String TO_BED = "to_bed";
        static final String FROM_BED = "from_bed";
        static final String SCORE_SLEEP = "score_sleep";
        static final String SCORE_STRESS = "score_stress";
        static final String SCORE_ANGRY = "score_angry";
        static final String SCORE_HAPPY = "score_happy";
        static final String SCORE_MOST_STRESS = "score_most_stress";
        static final String MOST_STRESS_LABEL = "most_stress_label";
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
        static final String SCORE_OTHER = "score_other";
        static final String OTHER_LABEL = "other_label";
    }

    public static final class Motivational_Data implements AWAREColumns {
        private Motivational_Data() {
        }

        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/upmc_dash_motivation");
        static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.upmc.dash.motivation";
        static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.upmc.dash.motivation";
        static final String RATIONALE = "motivation_rationale";
    }


    public static final class Stepcount_Data implements AWAREColumns {
        private Stepcount_Data() {
        }

        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/upmc_dash_stepcount");
        static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.upmc.dash.stepcount";
        static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.upmc.dash.stepcount";
        static final String STEP_COUNT = "stepcount";
        static final String ALARM_TYPE = "alarmtype";
    }

    public static final String[] TABLES_FIELDS = {
            Symptom_Data._ID + " integer primary key autoincrement," +
                    Symptom_Data.TIMESTAMP + " real default 0," +
                    Symptom_Data.DEVICE_ID + " text default ''," +
                    Symptom_Data.TO_BED + " text default ''," +
                    Symptom_Data.FROM_BED + " text default ''," +
                    Symptom_Data.SCORE_SLEEP + " text default ''," +
                    Symptom_Data.SCORE_STRESS + " text default ''," +
                    Symptom_Data.SCORE_ANGRY + " text default ''," +
                    Symptom_Data.SCORE_HAPPY + " text default ''," +
                    Symptom_Data.SCORE_MOST_STRESS + " text default ''," +
                    Symptom_Data.MOST_STRESS_LABEL + " text default ''," +
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
                    Symptom_Data.SCORE_OTHER + " text default ''," +
                    Symptom_Data.OTHER_LABEL + " text default ''",

            Motivational_Data._ID + " integer primary key autoincrement," +
                    Motivational_Data.TIMESTAMP + " real default 0," +
                    Motivational_Data.DEVICE_ID + " text default ''," +
                    Motivational_Data.RATIONALE + " text default ''",

            Stepcount_Data._ID + " integer primary key autoincrement," +
                    Stepcount_Data.TIMESTAMP + " real default 0," +
                    Stepcount_Data.DEVICE_ID + " text default ''," +
                    Stepcount_Data.STEP_COUNT + " integer default -1," +
                    Stepcount_Data.ALARM_TYPE + " integer default -1"
    };

    private static UriMatcher sUriMatcher = null;
    private static HashMap<String, String> surveyMap = null;
    private static HashMap<String, String> motivationMap = null;
    private static HashMap<String, String> stepcountMap = null;
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
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[1], MOTIVATIONS);
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[1] + "/#", MOTIVATIONS_ID);
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[2], STEPCOUNT);
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[2] + "/#", STEPCOUNT_ID);

        surveyMap = new HashMap<>();
        surveyMap.put(Symptom_Data._ID, Symptom_Data._ID);
        surveyMap.put(Symptom_Data.TIMESTAMP, Symptom_Data.TIMESTAMP);
        surveyMap.put(Symptom_Data.DEVICE_ID, Symptom_Data.DEVICE_ID);
        surveyMap.put(Symptom_Data.TO_BED, Symptom_Data.TO_BED);
        surveyMap.put(Symptom_Data.FROM_BED, Symptom_Data.FROM_BED);
        surveyMap.put(Symptom_Data.SCORE_SLEEP, Symptom_Data.SCORE_SLEEP);
        surveyMap.put(Symptom_Data.SCORE_STRESS, Symptom_Data.SCORE_STRESS);
        surveyMap.put(Symptom_Data.SCORE_ANGRY, Symptom_Data.SCORE_ANGRY);
        surveyMap.put(Symptom_Data.SCORE_HAPPY, Symptom_Data.SCORE_HAPPY);
        surveyMap.put(Symptom_Data.SCORE_MOST_STRESS, Symptom_Data.SCORE_MOST_STRESS);
        surveyMap.put(Symptom_Data.MOST_STRESS_LABEL, Symptom_Data.MOST_STRESS_LABEL);
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
        surveyMap.put(Symptom_Data.SCORE_OTHER, Symptom_Data.SCORE_OTHER);
        surveyMap.put(Symptom_Data.OTHER_LABEL, Symptom_Data.OTHER_LABEL);

        motivationMap = new HashMap<>();
        motivationMap.put(Motivational_Data._ID, Motivational_Data._ID);
        motivationMap.put(Motivational_Data.TIMESTAMP, Motivational_Data.TIMESTAMP);
        motivationMap.put(Motivational_Data.DEVICE_ID, Motivational_Data.DEVICE_ID);
        motivationMap.put(Motivational_Data.RATIONALE, Motivational_Data.RATIONALE);

        stepcountMap = new HashMap<>();
        stepcountMap.put(Stepcount_Data._ID, Stepcount_Data._ID);
        stepcountMap.put(Stepcount_Data.TIMESTAMP, Stepcount_Data.TIMESTAMP);
        stepcountMap.put(Stepcount_Data.DEVICE_ID, Stepcount_Data.DEVICE_ID);
        stepcountMap.put(Stepcount_Data.STEP_COUNT, Stepcount_Data.STEP_COUNT);
        stepcountMap.put(Stepcount_Data.ALARM_TYPE, Stepcount_Data.ALARM_TYPE);

        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        initialiseDatabase();

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        switch (sUriMatcher.match(uri)) {
            case ANSWERS:
                qb.setTables(DATABASE_TABLES[0]);
                qb.setProjectionMap(surveyMap);
                break;
            case MOTIVATIONS:
                qb.setTables(DATABASE_TABLES[1]);
                qb.setProjectionMap(motivationMap);
                break;
            case STEPCOUNT:
                qb.setTables(DATABASE_TABLES[2]);
                qb.setProjectionMap(stepcountMap);
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
            case MOTIVATIONS:
                return Motivational_Data.CONTENT_TYPE;
            case MOTIVATIONS_ID:
                return Motivational_Data.CONTENT_ITEM_TYPE;
            case STEPCOUNT:
                return Stepcount_Data.CONTENT_TYPE;
            case STEPCOUNT_ID:
                return Stepcount_Data.CONTENT_ITEM_TYPE;
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
            case MOTIVATIONS:
                long motiv_id = database.insertWithOnConflict(DATABASE_TABLES[1],
                        Motivational_Data.DEVICE_ID, values, SQLiteDatabase.CONFLICT_IGNORE);
                database.setTransactionSuccessful();
                database.endTransaction();
                if (motiv_id > 0) {
                    Uri questUri = ContentUris.withAppendedId(Motivational_Data.CONTENT_URI,
                            motiv_id);
                    getContext().getContentResolver().notifyChange(questUri, null, false);
                    return questUri;
                }
                database.endTransaction();
                throw new SQLException("Failed to insert row into " + uri);
            case STEPCOUNT:
                long step_id = database.insertWithOnConflict(DATABASE_TABLES[2],
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
            case MOTIVATIONS:
                count = database.delete(DATABASE_TABLES[1], selection, selectionArgs);
                break;
            case STEPCOUNT:
                count = database.delete(DATABASE_TABLES[2], selection, selectionArgs);
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
            case MOTIVATIONS:
                count = database.update(DATABASE_TABLES[1], values, selection, selectionArgs);
                break;
            case STEPCOUNT:
                count = database.update(DATABASE_TABLES[2], values, selection, selectionArgs);
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
