package com.aware.plugin.upmc.dash.syncadapters;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.aware.plugin.upmc.dash.activities.Provider;
import com.aware.syncadapters.AwareSyncAdapter;

/**
 * Created by RaghuTeja on 10/20/17.
 */

public class UPMC_Sync extends Service {
    private AwareSyncAdapter sSyncAdapter = null;
    private static final Object sSyncAdapterLock = new Object();
    @Override
    public void onCreate() {
        super.onCreate();
        synchronized (sSyncAdapterLock) {
            if (sSyncAdapter == null) {
                sSyncAdapter = new AwareSyncAdapter(getApplicationContext(), true, true);
                sSyncAdapter.init(
                        Provider.DATABASE_TABLES, Provider.TABLES_FIELDS,
                        new Uri[]{
                                Provider.Symptom_Data.CONTENT_URI,
                                Provider.Motivational_Data.CONTENT_URI,
                                Provider.Stepcount_Data.CONTENT_URI
                        }
                );
            }
        }

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
