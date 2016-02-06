package com.aware.plugin.google.fused_location;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * Created by denzilferreira on 06/02/16.
 */
public class DialogLocation extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        finish();
    }
}
