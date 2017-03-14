package com.aware.plugin.upmc.cancer;

import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateFormat;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.aware.providers.Scheduler_Provider;
import com.aware.utils.Scheduler;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Created by denzil on 14/03/2017.
 */

public class DebugSchedules extends AppCompatActivity {

    private LinearLayout container;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_schedules);
        container = (LinearLayout) findViewById(R.id.schedules_list);
    }

    @Override
    protected void onResume() {
        super.onResume();

        container.removeAllViews();

        Cursor schedules = getContentResolver().query(Scheduler_Provider.Scheduler_Data.CONTENT_URI, null, Scheduler_Provider.Scheduler_Data.SCHEDULE_ID + " LIKE '%random%'", null, Scheduler_Provider.Scheduler_Data.SCHEDULE_ID + " ASC");
        if (schedules != null && schedules.moveToFirst()) {
            do {
                try {
                    String jsonSchedule = schedules.getString(schedules.getColumnIndex(Scheduler_Provider.Scheduler_Data.SCHEDULE));
                    Scheduler.Schedule schedule = new Scheduler.Schedule(new JSONObject(jsonSchedule));

                    Calendar last = Calendar.getInstance();
                    last.setTimeInMillis(schedule.getTimer());
                    last.setTimeZone(TimeZone.getDefault());

                    TextView scheduleDebug = new TextView(getApplicationContext());

                    String debug = "";
                    debug += schedules.getString(schedules.getColumnIndex(Scheduler_Provider.Scheduler_Data.SCHEDULE_ID)) + "\n";
                    debug += "Trigger at: " + DateFormat.format("dd-MM-yyyy HH:mm:ss", last).toString();

                    scheduleDebug.setText(debug);

                    container.addView(scheduleDebug);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } while (schedules.moveToNext());
        }
        if (schedules != null && !schedules.isClosed()) schedules.close();
    }
}
