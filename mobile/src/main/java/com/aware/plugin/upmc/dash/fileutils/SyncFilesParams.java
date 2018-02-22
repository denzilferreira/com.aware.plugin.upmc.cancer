package com.aware.plugin.upmc.dash.fileutils;

import android.content.Context;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Asset;

/**
 * Created by RaghuTeja on 10/26/17.
 */

public class SyncFilesParams {
    GoogleApiClient client;
    Asset asset;

    public  SyncFilesParams(GoogleApiClient client, Asset asset, Context context) {
        setAsset(asset);
        setClient(client);
        setContext(context);
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    Context context;


    public GoogleApiClient getClient() {
        return client;
    }

    public void setClient(GoogleApiClient client) {
        this.client = client;
    }

    public Asset getAsset() {
        return asset;
    }

    public void setAsset(Asset asset) {
        this.asset = asset;
    }



}
