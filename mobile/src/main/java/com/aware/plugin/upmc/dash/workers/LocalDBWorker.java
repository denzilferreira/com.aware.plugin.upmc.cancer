package com.aware.plugin.upmc.dash.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.aware.plugin.upmc.dash.utils.Constants;
import com.aware.plugin.upmc.dash.utils.DBUtils;
import com.aware.plugin.upmc.dash.utils.LogFile;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static com.aware.plugin.upmc.dash.utils.Constants.DB_URL;
import static com.aware.plugin.upmc.dash.utils.Constants.PASS;
import static com.aware.plugin.upmc.dash.utils.Constants.TABLE_INTERVENTIONS;
import static com.aware.plugin.upmc.dash.utils.Constants.TABLE_SENSOR_DATA;
import static com.aware.plugin.upmc.dash.utils.Constants.USER;

public class LocalDBWorker extends Worker {

    public LocalDBWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(Constants.TAG, "LocalDBWorker: Starting work...");
        Connection conn = null;
        Statement stmt = null;
        try {
            LogFile.createFile();
            //Register JDBC driver
            Class.forName("com.mysql.jdbc.Driver");
            // 1. Sync Step Count
            //Open a connection
            int counter = 0;
            double ls_timestamp = -1;
            LogFile.writeToFile("LocalDBWorker: Connecting to database [SensorData]");
            Log.d(Constants.TAG, "LocalDBWorker: Connecting to database [SensorData]");
            conn = DriverManager.getConnection(DB_URL, USER, PASS);
            stmt = conn.createStatement();
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT * FROM ");
            sql.append(TABLE_SENSOR_DATA);
            ResultSet rs = stmt.executeQuery(sql.toString());
            while (rs.next()) {
                double timeStamp = rs.getDouble("timestamp");
                ls_timestamp = timeStamp;
                int type = rs.getInt("type");
                int data = rs.getInt("data");
                String sessionId = rs.getString("session_id");
                DBUtils.saveSensor(getApplicationContext(), timeStamp, type, data, sessionId);
                counter++;
            }
            LogFile.writeToFile("LocalDBWorker: Synced " + counter + " records [SensorData]: " + ls_timestamp);
            Log.d(Constants.TAG, "LocalDBWorker: Synced " + counter + " records [SensorData]: " + ls_timestamp);

            // 2. drop step count table
            //After syncing all the data records, clear the table
            String command = "DROP TABLE SensorData";
            stmt.executeUpdate(command);
            Log.d(Constants.TAG, "LocalDBWorker: Dropped table [SensorData]");
            LogFile.writeToFile("LocalDBWorker: Dropped table [SensorData]");
            command =
                    "CREATE TABLE SensorData " + "(timestamp double not NULL, " + " session_id " + "varchar(255) NULL, " + " type int(11) not NULL, " + " data int(11) " + "not NULL)";
            stmt.executeUpdate(command);
            Log.d(Constants.TAG, "LocalDBWorker:Reset SensorData table");
            LogFile.writeToFile("LocalDBWorker:Reset SensorData table");
            // 3. Sync Interventions Count
            Log.d(Constants.TAG, "LocalDBWorker: Connecting to database [interventions_watch]");
            LogFile.writeToFile("LocalDBWorker: Connecting to database [interventions_watch]");
            sql = new StringBuilder();
            sql.append("SELECT * FROM ");
            sql.append(TABLE_INTERVENTIONS);
            rs = stmt.executeQuery(sql.toString());
            counter = 0;
            ls_timestamp = -1;
            while (rs.next()) {
                double timeStamp = rs.getDouble("timestamp");
                int type = rs.getInt("notif_type");
                int snooze_shown = Constants.SNOOZE_NOT_SHOWN;
                String session_id = rs.getString("session_id");
                if (type == 0) {
                    type = Constants.NOTIF_TYPE_APPRAISAL;
                } else if (type == 1) {
                    type = Constants.NOTIF_TYPE_INACTIVITY;
                    snooze_shown = Constants.SNOOZE_SHOWN;
                } else if (type == 2) {
                    type = Constants.NOTIF_TYPE_INACTIVITY;
                } else if (type == 3) {
                    type = Constants.NOTIF_TYPE_BATT_LOW;
                } else {
                    Log.d(Constants.TAG, "LocalDBWorker: Corrupt  table [interventions_watch]");
                    LogFile.writeToFile("LocalDBWorker: Corrupt  table [interventions_watch]");
                    return Result.failure();
                }
                DBUtils.saveWatchIntervention(getApplicationContext(), timeStamp, session_id, type,
                        Constants.NOTIF_DEVICE_WATCH, snooze_shown);
                counter++;
                ls_timestamp = timeStamp;
            }
            Log.d(Constants.TAG,"LocalDBWorker: Synced " + counter + " records [interventions_watch]: " + ls_timestamp);
            LogFile.writeToFile(
                    "LocalDBWorker: Synced " + counter + " records [interventions_watch]: " + ls_timestamp);
            // 4. drop interventions table
            command = "DROP TABLE interventions_watch";
            Log.d(Constants.TAG, "LocalDBWorker: Dropping table [interventions_watch]");
            LogFile.writeToFile("LocalDBWorker: Dropping table [interventions_watch]");
            stmt.executeUpdate(command);
            command =
                    "CREATE TABLE interventions_watch " + "(id int(11) not NULL AUTO_INCREMENT, " + " timestamp double not NULL, " + " session_id varchar(255) NULL, " + " notif_type int NOT NULL, " + " PRIMARY KEY ( id ))";
            stmt.executeUpdate(command);

            // 5. Sync responses
            Log.d(Constants.TAG, "LocalDBWorker: Connecting to database [responses_watch]");
            LogFile.writeToFile("LocalDBWorker: Connecting to database [responses_watch]");
            command = "SELECT * FROM responses_watch";
            rs = stmt.executeQuery(command);
            counter = 0;
            ls_timestamp = -1;
            while (rs.next()) {
                int busy = rs.getInt("busy");
                int pain = rs.getInt("pain");
                int nausea = rs.getInt("nausea");
                int tired = rs.getInt("tired");
                int other = rs.getInt("other");
                int ok = rs.getInt("ok");
                int no = rs.getInt("no");
                int snooze = rs.getInt("snooze");
                double timeStamp = rs.getDouble("timestamp");
                String sessionId = rs.getString("session_id");
                DBUtils.saveWatchResponse(getApplicationContext(), timeStamp, sessionId, busy, pain,
                        nausea, tired, other, ok, no, snooze);
                counter++;
                ls_timestamp = timeStamp;
            }
            Log.d(Constants.TAG, "LocalDBWorker: Synced " + counter + " records [responses_watch]: " + ls_timestamp);
            LogFile.writeToFile("LocalDBWorker: Synced " + counter + " records [responses_watch]: " + ls_timestamp);
            // 6. Drop responses table
            Log.d(Constants.TAG, "LocalDBWorker:Dropping table [responses_watch]");
            LogFile.writeToFile("LocalDBWorker:Dropping table [responses_watch]");
            command = "DROP TABLE responses_watch";
            stmt.executeUpdate(command);
            command =
                    "CREATE TABLE responses_watch " + "(id int(11) not NULL AUTO_INCREMENT, " +
                            " timestamp double not NULL, " + " session_id varchar(255) NULL, " +
                            " ok int NULL, " + " no int NULL, " + " snooze int NULL, " + " busy " + "int NULL, " + " pain int NULL, " + " nausea int NULL, " + " tired " + "int NULL, " + " other int NULL, " + " PRIMARY KEY ( id ))";
            stmt.executeUpdate(command);
            //Clean-up environment
            rs.close();
            stmt.close();
            conn.close();
        } catch (ClassNotFoundException e) {
            Log.d(Constants.TAG, "LocalDBWorker:class not found.. retrying..");
            try {
                LogFile.writeToFile("LocalDBWorker:class not found.. retrying..");
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                LogFile.writeToFile(e.toString());
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            e.printStackTrace();
            return Result.retry();
        } catch (SQLException e) {
            Log.d(Constants.TAG, "LocalDBWorker:SQLException.. retrying..");
            try {
                LogFile.writeToFile("LocalDBWorker:SQLException.. retrying..");
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                LogFile.writeToFile(e.toString());
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            e.printStackTrace();
            return Result.retry();
        } catch (IOException e) {
            Log.d(Constants.TAG, "LocalDBWorker:IOException.. retrying..");
            try {
                LogFile.writeToFile("LocalDBWorker:IOException.. retrying..");
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                LogFile.writeToFile(e.toString());
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            e.printStackTrace();
        } finally {
            try {
                if (stmt != null)
                    stmt.close();
            } catch (SQLException se2) {
                Log.d(Constants.TAG, "LocalDBWorker:SQLException.. (stmt close) retrying..");
                try {
                    LogFile.writeToFile("LocalDBWorker:SQLException.. (stmt close) retrying..");
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    se2.printStackTrace(pw);
                    LogFile.writeToFile(se2.toString());
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                se2.printStackTrace();
            }
            try {
                if (conn != null)
                    conn.close();
            } catch (SQLException se) {
                Log.d(Constants.TAG, "LocalDBWorker:SQLException.. (conn close) retrying..");
                try {
                    LogFile.writeToFile("LocalDBWorker:SQLException.. (conn close) retrying..");
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    se.printStackTrace(pw);
                    LogFile.writeToFile(se.toString());
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                se.printStackTrace();
            }
        }
        return Result.success();
    }
}
