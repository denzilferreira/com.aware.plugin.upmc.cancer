package com.aware.plugin.upmc.dash.receivers;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

import com.aware.Aware;
import com.aware.plugin.upmc.dash.utils.Constants;

import static com.aware.plugin.upmc.dash.activities.Provider.AUTHORITY;

/**
 * Created by RaghuTeja on 3/6/18.
 */

public class ContextBroadcaster extends WakefulBroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Aware.ACTION_AWARE_SYNC_DATA)) {
            Log.d(Constants.TAG, "ContextBroadcaster: onReceive");
            Bundle sync = new Bundle();
            sync.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
            sync.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
            ContentResolver.requestSync(Aware.getAWAREAccount(context), AUTHORITY, sync);
        }
    }
}
