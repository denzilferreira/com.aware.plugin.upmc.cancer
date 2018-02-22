package com.aware.plugin.upmc.dash.fileutils;

/**
 * Created by RaghuTeja on 10/26/17.
 */

public interface SyncFilesResponse {
    void onSyncSuccess();
    void onSyncFailed();
}
