package com.aware.plugin.upmc.dash.utils;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

public class LogFile {
    public static final String STORAGE_FOLDER = Environment.getExternalStorageDirectory()+"/dash";
    public static final String STORAGE_FILE = "/log";
    public static final String EXTENSION = ".txt";


    public static void createFile() throws IOException {
        File path = new File(STORAGE_FOLDER);
        if(!path.exists())
            path.mkdirs();
        File file = new File(STORAGE_FOLDER+STORAGE_FILE + EXTENSION);
        if(!file.exists())
            file.createNewFile();
        Log.d(Constants.TAG, "LogFile:createFile");
    }

    public static void writeToFile(String log) throws IOException {
        Log.d(Constants.TAG, "LogFile:writeToFile");
        File file = new File(STORAGE_FOLDER+STORAGE_FILE+EXTENSION);
        PrintWriter writer = new PrintWriter(new FileOutputStream(file, true));
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(System.currentTimeMillis());
        stringBuilder.append("\t");
        stringBuilder.append(log);
        writer.append(stringBuilder.toString());
        stringBuilder.append("\n");
        writer.flush();
        writer.close();
    }

}
