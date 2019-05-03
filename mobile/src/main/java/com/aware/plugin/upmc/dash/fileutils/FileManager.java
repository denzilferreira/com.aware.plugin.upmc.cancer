package com.aware.plugin.upmc.dash.fileutils;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.aware.plugin.upmc.dash.R;
import com.aware.plugin.upmc.dash.utils.Constants;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataClient;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Field;

/**
 * Created by RaghuTeja on 10/23/17.
 */

public class FileManager {

    public static final String STORAGE_FOLDER = Environment.getExternalStorageDirectory()+"/dash";
    public static final String STORAGE_FILE = "/motionlog";
    public static final String EXTENSION = ".txt";
    public static boolean loop = true;


    public static final String HTDOCS_STORAGE_FOLDER = Environment.getExternalStorageDirectory()+"/htdocs";

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


    public static Task<DataClient.GetFdForAssetResponse> loadLogFileFromAsset(Asset asset, DataClient dataClient) throws IOException {
        Log.d(Constants.TAG,"FileManager: loadLogFileFromAsset entered ");
        if(asset == null)
            throw new IllegalArgumentException("Asset must be non-null");
        return dataClient.getFdForAsset(asset);
    }



    public static void saveAssetAsTextFile(InputStream assetInputStream, Context context) throws IOException {
        Log.d(Constants.TAG, "saveAssetAsTextFile: Writing file with size: " + String.valueOf(assetInputStream.available()));
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


    public static void createHtdocsFolder() throws IOException {
        File path = new File(HTDOCS_STORAGE_FOLDER);
        if(path.exists()) {
            Log.d(Constants.TAG, "createHtdocsFolder: folder exists, deleting stuff ");
            if(path.isDirectory()) {
                File[] children = path.listFiles();
                for (File child : children)
                    if(child.isFile())
                        child.delete();
                path.delete();
                path.mkdirs();
            }
        }
        else{
            Log.d(Constants.TAG, "createHtdocsFolder: folder doesn't exist, creating anew " + path.mkdirs());

        }

    }


    public static void writeResourcesToHtdocs(Context context) throws IllegalAccessException, IOException {
        createHtdocsFolder();
        Log.d(Constants.TAG, "writeResourcesToSdCard: Writing raw resources to sd card");
        Field[] fields = R.raw.class.getFields();
        for(Field field: fields) {
            Log.d(Constants.TAG, "writeResourceToSdCard: field name" + field.getName());
            int resourceId = field.getInt(field);
            InputStream in = context.getResources().openRawResource(resourceId);
            File resourceFile = new File(HTDOCS_STORAGE_FOLDER + "/" + field.getName() + ".php");
            resourceFile.createNewFile();
            FileOutputStream out = new FileOutputStream(resourceFile);
            byte[] buff = new byte[1024];
            int read = 0;
            try {
                while((read = in.read(buff))> 0) {
                    out.write(buff, 0, read);
                }
            }
            finally {
                in.close();
                out.close();
            }
        }
    }



    public static void stressTest() throws IOException {
//        for(int i=0; i<1000;i++) {
//            writeToFile(i);
//        }

    }


    public static class DirectoryCleaner {
        private final File mFile;

        public DirectoryCleaner(File file) {
            mFile = file;
        }

        public void clean() {
            if (null == mFile || !mFile.exists() || !mFile.isDirectory()) return;
            for (File file : mFile.listFiles()) {
                delete(file);
            }
        }

        private void delete(File file) {
            if (file.isDirectory()) {
                for (File child : file.listFiles()) {
                    delete(child);
                }
            }
            file.delete();

        }
    }

}
