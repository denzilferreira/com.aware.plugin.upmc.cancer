package com.aware.plugin.upmc.dash.fileutils;

import android.content.Context;
import android.os.AsyncTask;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Asset;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by RaghuTeja on 10/26/17.
 */

public class SyncFilesTask extends AsyncTask<SyncFilesParams, Integer, Long> {
    public SyncFilesResponse delegate = null;

    public SyncFilesTask(SyncFilesResponse syncFilesResponse) {
        this.delegate = syncFilesResponse;
    }
    @Override
    protected Long doInBackground(SyncFilesParams... paramList) {
        SyncFilesParams params = paramList[0];
        GoogleApiClient googleApiClient = params.getClient();
        Asset asset = params.getAsset();
        Context context = params.getContext();
        InputStream inputStream = null;
        try {
            inputStream = FileManager.loadLogFileFromAsset(asset, googleApiClient);
            FileManager.saveAssetAsTextFile(inputStream, context);

        } catch (IOException e) {
            e.printStackTrace();
        }
        delegate.onSyncSuccess();
        return (long) 0;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected void onPostExecute(Long aLong) {
        super.onPostExecute(aLong);
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
    }
}
