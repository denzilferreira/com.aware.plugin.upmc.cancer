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
import android.os.Environment;
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

    public static final int DATABASE_VERSION = 3;
    public static String AUTHORITY = "com.aware.plugin.upmc.cancer.provider";

    private static final int ANSWERS = 1;
    private static final int ANSWERS_ID = 2;

    public static final class Cancer_Data implements BaseColumns {
        private Cancer_Data(){}

        public static final Uri CONTENT_URI = Uri.parse("content://"+ Provider.AUTHORITY + "/upmc_cancer");
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
        public static final String SCORE_HOW_STRESS = "score_how_stress";
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

    public static String DATABASE_NAME = Environment.getExternalStorageDirectory() + "/AWARE/plugin_upmc_cancer.db";
    public static final String[] DATABASE_TABLES = {"upmc_cancer"};
    public static final String[] TABLES_FIELDS = {
            Cancer_Data._ID + " integer primary key autoincrement," +
            Cancer_Data.TIMESTAMP + " real default 0," +
            Cancer_Data.DEVICE_ID + " text default ''," +
            Cancer_Data.TO_BED + " text default ''," +
            Cancer_Data.FROM_BED + " text default ''," +
            Cancer_Data.SCORE_SLEEP + " text default ''," +
            Cancer_Data.SCORE_STRESS + " text default ''," +
            Cancer_Data.SCORE_ANGRY + " text default ''," +
            Cancer_Data.SCORE_HAPPY + " text default ''," +
            Cancer_Data.SCORE_MOST_STRESS + " text default ''," +
            Cancer_Data.MOST_STRESS_LABEL + " text default ''," +
            Cancer_Data.SCORE_HOW_STRESS + " text default ''," +
            Cancer_Data.SCORE_PAIN + " text default ''," +
            Cancer_Data.SCORE_FATIGUE + " text default ''," +
            Cancer_Data.SCORE_DISCONNECTED + " text default ''," +
            Cancer_Data.SCORE_CONCENTRATING + " text default ''," +
            Cancer_Data.SCORE_SAD + " text default ''," +
            Cancer_Data.SCORE_ANXIOUS + " text default ''," +
            Cancer_Data.SCORE_ENJOY + " text default ''," +
            Cancer_Data.SCORE_IRRITABLE + " text default ''," +
            Cancer_Data.SCORE_SHORT_BREATH + " text default ''," +
            Cancer_Data.SCORE_NUMBNESS + " text default ''," +
            Cancer_Data.SCORE_NAUSEA + " text default ''," +
            Cancer_Data.SCORE_APPETITE + " text default ''," +
            Cancer_Data.SCORE_OTHER + " text default ''," +
            Cancer_Data.OTHER_LABEL + " text default ''," +
            "UNIQUE (" + Cancer_Data.TIMESTAMP + "," + Cancer_Data.DEVICE_ID + ")"
    };

    private static UriMatcher sUriMatcher = null;
    private static HashMap<String, String> questionsMap = null;
    private static DatabaseHelper databaseHelper = null;
    private static SQLiteDatabase database = null;

    private boolean initializeDB() {
        if (databaseHelper == null) {
            databaseHelper = new DatabaseHelper( getContext(), DATABASE_NAME, null, DATABASE_VERSION, DATABASE_TABLES, TABLES_FIELDS );
        }
        if( database == null || ! database.isOpen() ) {
            database = databaseHelper.getWritableDatabase();
        }
        return( database != null );
    }

    @Override
    public boolean onCreate() {
        AUTHORITY = getContext().getPackageName() + ".provider";

        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(Provider.AUTHORITY, DATABASE_TABLES[0], ANSWERS);
        sUriMatcher.addURI(Provider.AUTHORITY, DATABASE_TABLES[0] + "/#", ANSWERS_ID);

        questionsMap = new HashMap<>();
        questionsMap.put(Cancer_Data._ID, Cancer_Data._ID);
        questionsMap.put(Cancer_Data.TIMESTAMP, Cancer_Data.TIMESTAMP);
        questionsMap.put(Cancer_Data.TO_BED, Cancer_Data.TO_BED);
        questionsMap.put(Cancer_Data.FROM_BED, Cancer_Data.FROM_BED);
        questionsMap.put(Cancer_Data.SCORE_SLEEP, Cancer_Data.SCORE_SLEEP);
        questionsMap.put(Cancer_Data.SCORE_MOST_STRESS, Cancer_Data.SCORE_MOST_STRESS);
        questionsMap.put(Cancer_Data.MOST_STRESS_LABEL, Cancer_Data.MOST_STRESS_LABEL);
        questionsMap.put(Cancer_Data.SCORE_STRESS, Cancer_Data.SCORE_STRESS);
        questionsMap.put(Cancer_Data.SCORE_ANGRY, Cancer_Data.SCORE_ANGRY);
        questionsMap.put(Cancer_Data.SCORE_HAPPY, Cancer_Data.SCORE_HAPPY);
        questionsMap.put(Cancer_Data.SCORE_PAIN, Cancer_Data.SCORE_PAIN);
        questionsMap.put(Cancer_Data.SCORE_FATIGUE, Cancer_Data.SCORE_FATIGUE);
        questionsMap.put(Cancer_Data.SCORE_DISCONNECTED, Cancer_Data.SCORE_DISCONNECTED);
        questionsMap.put(Cancer_Data.SCORE_CONCENTRATING, Cancer_Data.SCORE_CONCENTRATING);
        questionsMap.put(Cancer_Data.SCORE_SAD, Cancer_Data.SCORE_SAD);
        questionsMap.put(Cancer_Data.SCORE_ANXIOUS, Cancer_Data.SCORE_ANXIOUS);
        questionsMap.put(Cancer_Data.SCORE_ENJOY, Cancer_Data.SCORE_ENJOY);
        questionsMap.put(Cancer_Data.SCORE_IRRITABLE, Cancer_Data.SCORE_IRRITABLE);
        questionsMap.put(Cancer_Data.SCORE_SHORT_BREATH, Cancer_Data.SCORE_SHORT_BREATH);
        questionsMap.put(Cancer_Data.SCORE_NUMBNESS, Cancer_Data.SCORE_NUMBNESS);
        questionsMap.put(Cancer_Data.SCORE_NAUSEA, Cancer_Data.SCORE_NAUSEA);
        questionsMap.put(Cancer_Data.SCORE_APPETITE, Cancer_Data.SCORE_APPETITE);
        questionsMap.put(Cancer_Data.SCORE_OTHER, Cancer_Data.SCORE_OTHER);
        questionsMap.put(Cancer_Data.OTHER_LABEL, Cancer_Data.OTHER_LABEL);

        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        if( ! initializeDB() ) {
            Log.w(AUTHORITY,"Database unavailable...");
            return null;
        }

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        switch (sUriMatcher.match(uri)) {
            case ANSWERS:
                qb.setTables(DATABASE_TABLES[0]);
                qb.setProjectionMap(questionsMap);
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
                return Cancer_Data.CONTENT_TYPE;
            case ANSWERS_ID:
                return Cancer_Data.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        if( ! initializeDB() ) {
            Log.w(AUTHORITY,"Database unavailable...");
            return null;
        }

        ContentValues values = (initialValues != null) ? new ContentValues(
                initialValues) : new ContentValues();

        switch (sUriMatcher.match(uri)) {
            case ANSWERS:
                database.beginTransaction();
                long quest_id = database.insertWithOnConflict(DATABASE_TABLES[0],
                        Cancer_Data.DEVICE_ID, values, SQLiteDatabase.CONFLICT_IGNORE);
                database.setTransactionSuccessful();
                database.endTransaction();
                if (quest_id > 0) {
                    Uri questUri = ContentUris.withAppendedId(Cancer_Data.CONTENT_URI,
                            quest_id);
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
        if( ! initializeDB() ) {
            Log.w(AUTHORITY, "Database unavailable...");
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
            default:

                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        if( ! initializeDB() ) {
            Log.w(AUTHORITY,"Database unavailable...");
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
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }
}
