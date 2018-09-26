package com.aware.plugin.upmc.dash.fileutils;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;

import com.aware.plugin.upmc.dash.utils.Constants;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataClient;

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
        DataClient dataClient = params.getDataClient();
        Asset asset = params.getAsset();
        final Context context = params.getContext();
        try {
            FileManager.loadLogFileFromAsset(asset, dataClient).addOnCompleteListener(new OnCompleteListener<DataClient.GetFdForAssetResponse>() {
                @Override
                public void onComplete(@NonNull Task<DataClient.GetFdForAssetResponse> task) {
                    InputStream assetInputStream =  task.getResult().getInputStream();
                    if(assetInputStream == null) {
                        Log.d(Constants.TAG, "Requested an unknown Asset");
                    }
                    try {
                        FileManager.saveAssetAsTextFile(assetInputStream, context);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    delegate.onSyncSuccess();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        return (long) 0;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected void onPostExecute(Long aLong) {
        Log.d(Constants.TAG, "SyncFilesTast: syncDone");
        super.onPostExecute(aLong);
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
    }
}
