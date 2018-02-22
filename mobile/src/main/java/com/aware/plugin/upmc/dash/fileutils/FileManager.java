package com.aware.plugin.upmc.dash.fileutils;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.aware.plugin.upmc.dash.utils.Constants;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.Wearable;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.concurrent.TimeUnit;

/**
 * Created by RaghuTeja on 10/23/17.
 */

public class FileManager {

    public static final String STORAGE_FOLDER = Environment.getExternalStorageDirectory()+"/dash";
    public static final String STORAGE_FILE = "/motionlog";
    public static final String EXTENSION = ".txt";
    public static boolean loop = true;

    public static void createFile() throws IOException {
        File path = new File(STORAGE_FOLDER);
        if(!path.exists())
            path.mkdirs();
        File file = new File(STORAGE_FOLDER+STORAGE_FILE + EXTENSION);
        if(!file.exists())
            file.createNewFile();
        Log.d(Constants.TAG, "FileManager:createFile");
    }

    public static void writeToFile(int sc_count, int alarm) throws IOException {
        File file = new File(STORAGE_FOLDER+STORAGE_FILE+EXTENSION);
        PrintWriter writer = new PrintWriter(new FileOutputStream(file, true));
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(System.currentTimeMillis());
        stringBuilder.append("\t");
        stringBuilder.append(sc_count);
        stringBuilder.append("\t");
        stringBuilder.append(alarm);
        stringBuilder.append("\n");
        writer.append(stringBuilder.toString());
        writer.flush();
        writer.close();
    }

    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if(Environment.MEDIA_MOUNTED.equals(state))
            return true;
        return false;
    }

    public static boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if(Environment.MEDIA_MOUNTED.equals(state)
                || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state))
            return true;
        return false;
    }

    public static Asset createAssetFromLogFile() throws IOException {
        File file = new File(STORAGE_FOLDER+STORAGE_FILE+EXTENSION);
        int size = (int) file.length();
        byte[] bytes = new byte[size];
        BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
        final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        buf.read(bytes, 0,bytes.length);
        buf.close();
        return Asset.createFromBytes(bytes);
    }


    public static InputStream loadLogFileFromAsset(Asset asset, GoogleApiClient googleApiClient) throws IOException {
        Log.d(Constants.TAG,"FileManager: loadLogFileFromAsset entered ");
        if(asset == null)
            throw new IllegalArgumentException("Asset must be non-null");
        Log.d(Constants.TAG,"FileManager: loadLogFileFromAsset w1 ");
        if(!googleApiClient.isConnected()) {
            final ConnectionResult result =
                    googleApiClient.blockingConnect(5000, TimeUnit.MILLISECONDS);
            Log.d(Constants.TAG,"FileManager: loadLogFileFromAsset w2 ");
            if(!result.isSuccess())
                return null;
        }
        Log.d(Constants.TAG,"FileManager: loadLogFileFromAsset 23 ");
        InputStream assetInputStream = Wearable.DataApi.getFdForAsset(googleApiClient, asset).await().getInputStream();
        if(assetInputStream == null) {
            Log.d(Constants.TAG, "Requested an unknown Asset");
        }
        Log.d(Constants.TAG,"FileManager: " + assetInputStream.available());
        return assetInputStream;
    }



    public static void saveAssetAsTextFile(InputStream assetInputStream, Context context) throws IOException {
        createFile();
        File file = new File(STORAGE_FOLDER+STORAGE_FILE + EXTENSION);
        OutputStream outputStream = new FileOutputStream(file);
        int read;
        byte[] buffer = new byte[4*1024];
        while((read = assetInputStream.read(buffer))!=-1) {
            outputStream.write(buffer, 0, read);
        }
        outputStream.flush();
        outputStream.close();
    }




    public static void stressTest() throws IOException {
//        for(int i=0; i<1000;i++) {
//            writeToFile(i);
//        }

    }
}
