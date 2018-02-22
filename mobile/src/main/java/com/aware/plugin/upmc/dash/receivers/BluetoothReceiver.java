package com.aware.plugin.upmc.dash.receivers;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.aware.plugin.upmc.dash.utils.Constants;

/**
 * Created by RaghuTeja on 8/6/17.
 */

public class BluetoothReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if(action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(Constants.BLUETOOTH_COMM).putExtra(Constants.BLUETOOTH_COMM_KEY, state));
        }

    }
}