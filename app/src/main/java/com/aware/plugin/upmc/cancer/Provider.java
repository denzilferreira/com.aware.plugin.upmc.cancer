package com.aware.plugin.upmc.cancer;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
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
 * Created by denzil on 25/11/14.
 * Edited by Grace on 19/08/15
 */
public class Provider extends ContentProvider {

    public static final int DATABASE_VERSION = 5;
    public static String AUTHORITY = "com.aware.plugin.upmc.cancer.provider.survey";

    private static final int ANSWERS = 1;
    private static final int ANSWERS_ID = 2;
    private static final int MOTIVATIONS = 3;
    private static final int MOTIVATIONS_ID = 4;

    public static final class Symptom_Data implements BaseColumns {
        private Symptom_Data() {
        }

        public static final Uri CONTENT_URI = Uri.parse("content://" + Provider.AUTHORITY + "/upmc_cancer");
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.upmc.cancer";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.upmc.cancer";

        public static final String _ID = "_id";
        public static final String TIMESTAMP = "timestamp";
        public static final String DEVICE_ID = "device_id";
        public static final String TO_BED = "to_bed";
        public static final String FROM_BED = "from_bed";
        public static final String SCORE_SLEEP = "score_sleep";
        public static final String SCORE_STRESS = "score_stress";
        public static final String SCORE_ANGRY = "score_angry";
        public static final String SCORE_HAPPY = "score_happy";
        public static final String SCORE_MOST_STRESS = "score_most_stress";
        public static final String MOST_STRESS_LABEL = "most_stress_label";
        public static final String SCORE_PAIN = "score_pain";
        public static final String SCORE_FATIGUE = "score_fatigue";
        public static final String SCORE_DISCONNECTED = "score_disconnected";
        public static final String SCORE_CONCENTRATING = "score_concentrating";
        public static final String SCORE_SAD = "score_sad";
        public static final String SCORE_ANXIOUS = "score_anxious";
        public static final String SCORE_ENJOY = "score_enjoy";
        public static final String SCORE_IRRITABLE = "score_irritable";
        public static final String SCORE_SHORT_BREATH = "score_short_breath";
        public static final String SCORE_NUMBNESS = "score_numbness";
        public static final String SCORE_NAUSEA = "score_nausea";
        public static final String SCORE_APPETITE = "score_appetite";
        public static final String SCORE_OTHER = "score_other";
        public static final String OTHER_LABEL = "other_label";
    }

    public static final class Motivational_Data implements BaseColumns {
        private Motivational_Data(){}

        static final Uri CONTENT_URI = Uri.parse("content://" + Provider.AUTHORITY + "/upmc_cancer_motivation");
        static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.upmc.cancer.motivation";
        static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.upmc.cancer.motivation";

        static final String _ID = "_id";
        static final String TIMESTAMP = "timestamp";
        static final String DEVICE_ID = "device_id";
        static final String ACTION = "motivation_action"; //triggered 0, answered = 1, snoozed = 2
        static final String RATIONALE = "motivation_rationale";
        static final String ANSWER_TIME = "answer_timestamp";
    }

    public static String DATABASE_NAME = "plugin_upmc_cancer.db";

    public static final String[] DATABASE_TABLES = {
            "upmc_cancer",
            "upmc_cancer_motivation"
    };

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
            Symptom_Data.SCORE_DISCONNECTED + " text default ''," +
            Symptom_Data.SCORE_CONCENTRATING + " text default ''," +
            Symptom_Data.SCORE_SAD + " text default ''," +
            Symptom_Data.SCORE_ANXIOUS + " text default ''," +
            Symptom_Data.SCORE_ENJOY + " text default ''," +
            Symptom_Data.SCORE_IRRITABLE + " text default ''," +
            Symptom_Data.SCORE_SHORT_BREATH + " text default ''," +
            Symptom_Data.SCORE_NUMBNESS + " text default ''," +
            Symptom_Data.SCORE_NAUSEA + " text default ''," +
            Symptom_Data.SCORE_APPETITE + " text default ''," +
            Symptom_Data.SCORE_OTHER + " text default ''," +
            Symptom_Data.OTHER_LABEL + " text default ''",

            Motivational_Data._ID + " integer primary key autoincrement," +
            Motivational_Data.TIMESTAMP + " real default 0,"+
            Motivational_Data.DEVICE_ID + " text default '',"+
            Motivational_Data.ACTION + " integer default 0,"+
            Motivational_Data.RATIONALE + " text default '',"+
            Motivational_Data.ANSWER_TIME + " real default 0"
    };

    private static UriMatcher sUriMatcher = null;
    private static HashMap<String, String> surveyMap = null;
    private static HashMap<String, String> motivationMap = null;
    private static DatabaseHelper databaseHelper = null;
    private static SQLiteDatabase database = null;

    private boolean initializeDB() {
        if (databaseHelper == null) {
            databaseHelper = new DatabaseHelper(getContext(), DATABASE_NAME, null, DATABASE_VERSION, DATABASE_TABLES, TABLES_FIELDS);
        }
        if (database == null || !database.isOpen()) {
            database = databaseHelper.getWritableDatabase();
        }
        return (database != null);
    }

    @Override
    public boolean onCreate() {
        AUTHORITY = getContext().getPackageName() + ".provider.survey";

        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(Provider.AUTHORITY, DATABASE_TABLES[0], ANSWERS);
        sUriMatcher.addURI(Provider.AUTHORITY, DATABASE_TABLES[0] + "/#", ANSWERS_ID);
        sUriMatcher.addURI(Provider.AUTHORITY, DATABASE_TABLES[1], MOTIVATIONS);
        sUriMatcher.addURI(Provider.AUTHORITY, DATABASE_TABLES[1] + "/#", MOTIVATIONS_ID);

        surveyMap = new HashMap<>();
        surveyMap.put(Symptom_Data._ID, Symptom_Data._ID);
        surveyMap.put(Symptom_Data.TIMESTAMP, Symptom_Data.TIMESTAMP);
        surveyMap.put(Symptom_Data.DEVICE_ID, Symptom_Data.DEVICE_ID);
        surveyMap.put(Symptom_Data.TO_BED, Symptom_Data.TO_BED);
        surveyMap.put(Symptom_Data.FROM_BED, Symptom_Data.FROM_BED);
        surveyMap.put(Symptom_Data.SCORE_SLEEP, Symptom_Data.SCORE_SLEEP);
        surveyMap.put(Symptom_Data.SCORE_MOST_STRESS, Symptom_Data.SCORE_MOST_STRESS);
        surveyMap.put(Symptom_Data.MOST_STRESS_LABEL, Symptom_Data.MOST_STRESS_LABEL);
        surveyMap.put(Symptom_Data.SCORE_STRESS, Symptom_Data.SCORE_STRESS);
        surveyMap.put(Symptom_Data.SCORE_ANGRY, Symptom_Data.SCORE_ANGRY);
        surveyMap.put(Symptom_Data.SCORE_HAPPY, Symptom_Data.SCORE_HAPPY);
        surveyMap.put(Symptom_Data.SCORE_PAIN, Symptom_Data.SCORE_PAIN);
        surveyMap.put(Symptom_Data.SCORE_FATIGUE, Symptom_Data.SCORE_FATIGUE);
        surveyMap.put(Symptom_Data.SCORE_DISCONNECTED, Symptom_Data.SCORE_DISCONNECTED);
        surveyMap.put(Symptom_Data.SCORE_CONCENTRATING, Symptom_Data.SCORE_CONCENTRATING);
        surveyMap.put(Symptom_Data.SCORE_SAD, Symptom_Data.SCORE_SAD);
        surveyMap.put(Symptom_Data.SCORE_ANXIOUS, Symptom_Data.SCORE_ANXIOUS);
        surveyMap.put(Symptom_Data.SCORE_ENJOY, Symptom_Data.SCORE_ENJOY);
        surveyMap.put(Symptom_Data.SCORE_IRRITABLE, Symptom_Data.SCORE_IRRITABLE);
        surveyMap.put(Symptom_Data.SCORE_SHORT_BREATH, Symptom_Data.SCORE_SHORT_BREATH);
        surveyMap.put(Symptom_Data.SCORE_NUMBNESS, Symptom_Data.SCORE_NUMBNESS);
        surveyMap.put(Symptom_Data.SCORE_NAUSEA, Symptom_Data.SCORE_NAUSEA);
        surveyMap.put(Symptom_Data.SCORE_APPETITE, Symptom_Data.SCORE_APPETITE);
        surveyMap.put(Symptom_Data.SCORE_OTHER, Symptom_Data.SCORE_OTHER);
        surveyMap.put(Symptom_Data.OTHER_LABEL, Symptom_Data.OTHER_LABEL);

        motivationMap = new HashMap<>();
        motivationMap.put(Motivational_Data._ID, Motivational_Data._ID);
        motivationMap.put(Motivational_Data.TIMESTAMP, Motivational_Data.TIMESTAMP);
        motivationMap.put(Motivational_Data.DEVICE_ID, Motivational_Data.DEVICE_ID);
        motivationMap.put(Motivational_Data.ACTION, Motivational_Data.ACTION);
        motivationMap.put(Motivational_Data.RATIONALE, Motivational_Data.RATIONALE);
        motivationMap.put(Motivational_Data.ANSWER_TIME, Motivational_Data.ANSWER_TIME);

        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        if (!initializeDB()) {
            Log.w(Plugin.TAG, "Database unavailable...");
            return null;
        }

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
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        if (!initializeDB()) {
            Log.w(Plugin.TAG, "Database unavailable...");
            return null;
        }

        ContentValues values = (initialValues != null) ? new ContentValues(
                initialValues) : new ContentValues();

        switch (sUriMatcher.match(uri)) {
            case ANSWERS:
                database.beginTransaction();
                long quest_id = database.insertWithOnConflict(DATABASE_TABLES[0],
                        Symptom_Data.DEVICE_ID, values, SQLiteDatabase.CONFLICT_IGNORE);
                database.setTransactionSuccessful();
                database.endTransaction();
                if (quest_id > 0) {
                    Uri questUri = ContentUris.withAppendedId(Symptom_Data.CONTENT_URI,
                            quest_id);
                    getContext().getContentResolver().notifyChange(questUri, null);
                    return questUri;
                }
                throw new SQLException("Failed to insert row into " + uri);
            case MOTIVATIONS:
                database.beginTransaction();
                long motiv_id = database.insertWithOnConflict(DATABASE_TABLES[1],
                        Motivational_Data.DEVICE_ID, values, SQLiteDatabase.CONFLICT_IGNORE);
                database.setTransactionSuccessful();
                database.endTransaction();
                if (motiv_id > 0) {
                    Uri questUri = ContentUris.withAppendedId(Motivational_Data.CONTENT_URI,
                            motiv_id);
                    getContext().getContentResolver().notifyChange(questUri, null);
                    return questUri;
                }
                throw new SQLException("Failed to insert row into " + uri);
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        if (!initializeDB()) {
            Log.w(Plugin.TAG, "Database unavailable...");
            return 0;
        }

        int count = 0;
        switch (sUriMatcher.match(uri)) {
            case ANSWERS:
                database.beginTransaction();
                count = database.delete(DATABASE_TABLES[0], selection,
                        selectionArgs);
                database.setTransactionSuccessful();
                database.endTransaction();
                break;
            case MOTIVATIONS:
                database.beginTransaction();
                count = database.delete(DATABASE_TABLES[1], selection,
                        selectionArgs);
                database.setTransactionSuccessful();
                database.endTransaction();
                break;
            default:

                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        if (!initializeDB()) {
            Log.w(Plugin.TAG, "Database unavailable...");
            return 0;
        }

        int count = 0;
        switch (sUriMatcher.match(uri)) {
            case ANSWERS:
                database.beginTransaction();
                count = database.update(DATABASE_TABLES[0], values, selection,
                        selectionArgs);
                database.setTransactionSuccessful();
                database.endTransaction();
                break;
            case MOTIVATIONS:
                database.beginTransaction();
                count = database.update(DATABASE_TABLES[1], values, selection,
                        selectionArgs);
                database.setTransactionSuccessful();
                database.endTransaction();
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }
}
