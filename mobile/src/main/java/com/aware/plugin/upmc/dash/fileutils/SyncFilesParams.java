package com.aware.plugin.upmc.dash.fileutils;

import android.content.Context;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataClient;

/**
 * Created by RaghuTeja on 10/26/17.
 */

public class SyncFilesParams {
    private Asset asset;
    private Context context;
    private DataClient dataClient;

    public  SyncFilesParams(Context context, Asset asset, DataClient dataClient) {
        setAsset(asset);
        setContext(context);
        setDataClient(dataClient);
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public Asset getAsset() {
        return asset;
    }
    public void setAsset(Asset asset) {
        this.asset = asset;
    }


    public DataClient getDataClient() {
        return dataClient;
    }

    public void setDataClient(DataClient dataClient) {
        this.dataClient = dataClient;
    }
}
