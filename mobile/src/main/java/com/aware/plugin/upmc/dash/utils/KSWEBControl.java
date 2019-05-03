package com.aware.plugin.upmc.dash.utils;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import androidx.fragment.app.FragmentActivity;
import android.util.Log;

public class KSWEBControl extends FragmentActivity {
    public static final String FTP_START = "ru.kslabs.ksweb.CMD.FTP_START";
    public static final String FTP_STOP = "ru.kslabs.ksweb.CMD.FTP_STOP";

    public static final String LIGHTTPD_START = "ru.kslabs.ksweb.CMD.LIGHTTPD_START";
    public static final String LIGHTTPD_STOP = "ru.kslabs.ksweb.CMD.LIGHTTPD_STOP";

    public static final String NGINX_START = "ru.kslabs.ksweb.CMD.NGINX_START";
    public static final String NGINX_STOP = "ru.kslabs.ksweb.CMD.NGINX_STOP";

    public static final String MYSQL_START = "ru.kslabs.ksweb.CMD.MYSQL_START";
    public static final String MYSQL_STOP = "ru.kslabs.ksweb.CMD.MYSQL_STOP";

    public static final String KSWEB_CLOSE = "ru.kslabs.ksweb.CMD.KSWEB_CLOSE";
    public static final String KSWEB_START = "ru.kslabs.ksweb.CMD.KSWEB_START";
    public static final String KSWEB_FINISH_ACTIVITY = "ru.kslabs.ksweb.CMD.KSWEB_FINISH_ACTIVITY";

    public static final String MYSQL_SET_CONFIG = "ru.kslabs.ksweb.CMD.MYSQL_SET_CONFIG";
    public static final String PHP_SET_CONFIG = "ru.kslabs.ksweb.CMD.PHP_SET_CONFIG";
    public static final String LIGHTTPD_SET_CONFIG = "ru.kslabs.ksweb.CMD.LIGHTTPD_SET_CONFIG";
    public static final String NGINX_SET_CONFIG = "ru.kslabs.ksweb.CMD.NGINX_SET_CONFIG";

    public static final String NGINX_ADD_HOST = "ru.kslabs.ksweb.CMD.NGINX_ADD_HOST";
    public static final String NGINX_DELETE_HOST = "ru.kslabs.ksweb.CMD.NGINX_DELETE_HOST";

    public static final String LIGHTTPD_ADD_HOST = "ru.kslabs.ksweb.CMD.LIGHTTPD_ADD_HOST";
    public static final String LIGHTTPD_DELETE_HOST = "ru.kslabs.ksweb.CMD.LIGHTPD_DELETE_HOST";

    public static final String RESPOND_OK = "ru.kslabs.ksweb.CMD.RESPOND_OK";
    public static final String RESPOND_ERROR = "ru.kslabs.ksweb.CMD.RESPOND_ERROR";

    public static final String TAG_KEY = "TAG";
    public static final String DATA_KEY = "DATA";
    public static final String CMD_KEY = "CMD";

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v("KSWEB", intent.getAction() + "; " + intent.getExtras().getString("TAG"));
        }
    };

    public void setKSWEBCmdBroadcastReceiver(BroadcastReceiver receiver) {
        this.receiver = receiver;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        IntentFilter filter = new IntentFilter();
        filter.addAction(RESPOND_OK);
        filter.addAction(RESPOND_ERROR);
        registerReceiver(receiver, filter);
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(receiver);
        super.onDestroy();
    }

    public static void lighttpdDeleteHost(Activity activity, String tag, String hostname, String port, String rootDir) {
        Intent intent = new Intent();
        intent.setAction(LIGHTTPD_DELETE_HOST);
        intent.putExtra(TAG_KEY, tag);
        intent.putExtra(DATA_KEY, new String[]{hostname, port, rootDir});
        activity.sendBroadcast(intent);
    }

    public static void lighttpdAddHost(Activity activity, String tag, String hostname, String port, String rootDir) {
        Intent intent = new Intent();
        intent.setAction(LIGHTTPD_ADD_HOST);
        intent.putExtra(TAG_KEY, tag);
        intent.putExtra(DATA_KEY, new String[]{hostname, port, rootDir});
        activity.sendBroadcast(intent);
    }

    public static void nginxDeleteHost(Activity activity, String tag, String hostname, String port, String rootDir) {
        Intent intent = new Intent();
        intent.setAction(NGINX_DELETE_HOST);
        intent.putExtra(TAG_KEY, tag);
        intent.putExtra(DATA_KEY, new String[]{hostname, port, rootDir});
        activity.sendBroadcast(intent);
    }

    public static void nginxAddHost(Activity activity, String tag, String hostname, String port, String rootDir) {
        Intent intent = new Intent();
        intent.setAction(NGINX_ADD_HOST);
        intent.putExtra(TAG_KEY, tag);
        intent.putExtra(DATA_KEY, new String[]{hostname, port, rootDir});
        activity.sendBroadcast(intent);
    }

    public static void nginxSetConfig(Activity activity, String tag, String configTxt) {
        Intent intent = new Intent();
        intent.setAction(NGINX_SET_CONFIG);
        intent.putExtra(TAG_KEY, tag);
        intent.putExtra(DATA_KEY, new String[]{configTxt});
        activity.sendBroadcast(intent);
    }

    public static void lighttpdSetConfig(Activity activity, String tag, String configTxt) {
        Intent intent = new Intent();
        intent.setAction(LIGHTTPD_SET_CONFIG);
        intent.putExtra(TAG_KEY, tag);
        intent.putExtra(DATA_KEY, new String[]{configTxt});
        activity.sendBroadcast(intent);
    }

    public static void phpSetConfig(Activity activity, String tag, String configTxt) {
        Intent intent = new Intent();
        intent.setAction(PHP_SET_CONFIG);
        intent.putExtra(TAG_KEY, tag);
        intent.putExtra(DATA_KEY, new String[]{configTxt});
        activity.sendBroadcast(intent);
    }

    public static void mysqlSetConfig(Activity activity, String tag, String configTxt) {
        Intent intent = new Intent();
        intent.setAction(MYSQL_SET_CONFIG);
        intent.putExtra(TAG_KEY, tag);
        intent.putExtra(DATA_KEY, new String[]{configTxt});
        activity.sendBroadcast(intent);
    }

    public static void kswebClose(Activity activity, String tag) {
        Intent intent = new Intent();
        intent.setAction(KSWEB_CLOSE);
        intent.putExtra(TAG_KEY, tag);
        activity.sendBroadcast(intent);
    }

    public static void kswebStart(Activity activity, String tag) {
        Intent intent = new Intent();
        intent.setAction(KSWEB_START);
        intent.putExtra(TAG_KEY, tag);
        activity.sendBroadcast(intent);
    }

    public static void kswebFinishActivity(Activity activity, String tag) {
        Intent intent = new Intent();
        intent.setAction(KSWEB_FINISH_ACTIVITY);
        intent.putExtra(TAG_KEY, tag);
        activity.sendBroadcast(intent);
    }

    public static void mysqlStop(Activity activity, String tag) {
        Intent intent = new Intent();
        intent.setAction(MYSQL_STOP);
        intent.putExtra(TAG_KEY, tag);
        activity.sendBroadcast(intent);
    }

    public static void mysqlStart(Activity activity, String tag) {
        Intent intent = new Intent();
        intent.setAction(MYSQL_START);
        intent.putExtra(TAG_KEY, tag);
        activity.sendBroadcast(intent);
    }

    public static void nginxStop(Activity activity, String tag) {
        Intent intent = new Intent();
        intent.setAction(NGINX_STOP);
        intent.putExtra(TAG_KEY, tag);
        activity.sendBroadcast(intent);
    }

    public static void nginxStart(Activity activity, String tag) {
        Intent intent = new Intent();
        intent.setAction(NGINX_START);
        intent.putExtra(TAG_KEY, tag);
        activity.sendBroadcast(intent);
    }

    public static void lighttpdStop(Activity activity, String tag) {
        Intent intent = new Intent();
        intent.setAction(LIGHTTPD_STOP);
        intent.putExtra(TAG_KEY, tag);
        activity.sendBroadcast(intent);
    }

    public static void lighttpdStart(Activity activity, String tag) {
        Intent intent = new Intent();
        intent.setAction(LIGHTTPD_START);
        intent.putExtra(TAG_KEY, tag);
        activity.sendBroadcast(intent);
    }

    public static void ftpStop(Activity activity, String tag) {
        Intent intent = new Intent();
        intent.setAction(FTP_STOP);
        intent.putExtra(TAG_KEY, tag);
        activity.sendBroadcast(intent);
    }

    public static void ftpStart(Activity activity, String tag) {
        Intent intent = new Intent();
        intent.setAction(FTP_START);
        intent.putExtra(TAG_KEY, tag);
        activity.sendBroadcast(intent);
    }

}
