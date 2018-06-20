package com.aware.plugin.upmc.dash.activities;

import android.content.ContentValues;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.aware.Aware;
import com.aware.Aware_Preferences;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static com.aware.plugin.upmc.dash.utils.Constants.DB_URL;
import static com.aware.plugin.upmc.dash.utils.Constants.PASS;
import static com.aware.plugin.upmc.dash.utils.Constants.TABLE_SENSOR_DATA;
import static com.aware.plugin.upmc.dash.utils.Constants.USER;

public class DataSyncingActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Connection conn = null;
        Statement stmt = null;
        try {
            //Register JDBC driver
            Class.forName("com.mysql.jdbc.Driver");
            //Open a connection
            Log.d("yiyi", "Connecting to database to sync sensor data...");
            conn = DriverManager.getConnection(DB_URL, USER, PASS);
            stmt = conn.createStatement();
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT unixTime, type, data FROM ");
            sql.append(TABLE_SENSOR_DATA);
            ResultSet rs = stmt.executeQuery(sql.toString());
            if (rs.next()) {
                long timeStamp = rs.getLong("unixTime");
                int type = rs.getInt("type");
                int data = rs.getInt("data");
                syncSCWithServer(timeStamp, type, data);
            }
            //After syncing all the data records, clear the table
            String command = "DROP TABLE SensorData";
            stmt.executeUpdate(command);
            Log.d("yiyi", "Table deleted!!!");
            command = "CREATE TABLE SensorData " +
                    "(unixTime bigint(20) not NULL, " +
                    " type int(11) not NULL, " +
                    " data int(11) not NULL)";
            stmt.executeUpdate(command);
            Log.d("yiyi", "Reset table SensorData");
            //Clean-up environment
            rs.close();
            stmt.close();
            conn.close();
        } catch (Exception e) {
        } finally {
            try {
                if (stmt != null)
                    stmt.close();
            } catch (SQLException se2) {
            }
            try {
                if (conn != null)
                    conn.close();
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
    }

    public void syncSCWithServer(long timeStamp, int type, int data) {
        ContentValues step_count = new ContentValues();
        step_count.put(Provider.Stepcount_Data.DEVICE_ID, Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
        step_count.put(Provider.Stepcount_Data.TIMESTAMP, timeStamp);
        step_count.put(Provider.Stepcount_Data.STEP_COUNT, data);
        step_count.put(Provider.Stepcount_Data.ALARM_TYPE, type);
        getContentResolver().insert(Provider.Stepcount_Data.CONTENT_URI, step_count);
    }
}
