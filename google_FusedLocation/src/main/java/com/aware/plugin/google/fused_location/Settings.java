
package com.aware.plugin.google.fused_location;

import com.aware.Aware;
import com.aware.ui.Aware_Activity;
import com.aware.ui.Aware_Toolbar;
import com.google.android.gms.location.LocationRequest;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

public class Settings extends AppCompatActivity {

	/**
	 * Boolean to activate/deactivate Google Fused Location
	 */
    public static final String STATUS_GOOGLE_FUSED_LOCATION = "status_google_fused_location";
    /**
     * How frequently should we try to acquire location (in seconds)
     */
    public static final String FREQUENCY_GOOGLE_FUSED_LOCATION = "frequency_google_fused_location";
    /**
     * How fast you are willing to get the latest location (in seconds)
     */
    public static final String MAX_FREQUENCY_GOOGLE_FUSED_LOCATION = "max_frequency_google_fused_location";
    /**
     * How important is accuracy to you and battery impact. One of the following:<br/>
     * {@link LocationRequest#PRIORITY_HIGH_ACCURACY}<br/>
     * {@link LocationRequest#PRIORITY_BALANCED_POWER_ACCURACY}<br/>
     * {@link LocationRequest#PRIORITY_LOW_POWER}<br/>
     * {@link LocationRequest#PRIORITY_NO_POWER}
     */
    public static final String ACCURACY_GOOGLE_FUSED_LOCATION = "accuracy_google_fused_location";
    
    /**
     * Update interval for location, in seconds ( default = 180 )
     */
    public static int update_interval = 180;
    
    /**
     * Fastest update interval for location, in seconds ( default = 1 )
     */
    public static int max_update_interval = 1;
    
    /**
     * Desired location accuracy (default = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY <br/>
     * Other possible Values: 
     * {@link LocationRequest#PRIORITY_HIGH_ACCURACY} <br/>
     * {@link LocationRequest#PRIORITY_BALANCED_POWER_ACCURACY} <br/>
     * {@link LocationRequest#PRIORITY_LOW_POWER} <br/>
     * {@link LocationRequest#PRIORITY_NO_POWER}
     */
    public static int location_accuracy = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY;
    
    private static EditText update_frequency = null;
    private static EditText max_update_frequency = null;
    private static Spinner accuracy_level = null;
    private static ArrayAdapter<CharSequence> adapter = null;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.settings);

        update_frequency = (EditText) findViewById(R.id.update_frequency);
        update_frequency.setText(Aware.getSetting(this, Settings.FREQUENCY_GOOGLE_FUSED_LOCATION));
        
        max_update_frequency = (EditText) findViewById(R.id.fastest_update_frequency);
        max_update_frequency.setText(Aware.getSetting(this, Settings.MAX_FREQUENCY_GOOGLE_FUSED_LOCATION));
        
        accuracy_level = (Spinner) findViewById(R.id.accuracy_level);
        adapter = ArrayAdapter.createFromResource(this, R.array.accuracies, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        accuracy_level.setAdapter(adapter);
        
        switch(Integer.parseInt(Aware.getSetting(this, Settings.ACCURACY_GOOGLE_FUSED_LOCATION))) {
            case LocationRequest.PRIORITY_HIGH_ACCURACY:
                accuracy_level.setSelection(0);
                location_accuracy = LocationRequest.PRIORITY_HIGH_ACCURACY;
                break;
            case LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY:
                accuracy_level.setSelection(1);
                location_accuracy = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY;
                break;
            case LocationRequest.PRIORITY_LOW_POWER:
                accuracy_level.setSelection(2);
                location_accuracy = LocationRequest.PRIORITY_LOW_POWER;
                break;
            case LocationRequest.PRIORITY_NO_POWER:
                accuracy_level.setSelection(3);
                location_accuracy = LocationRequest.PRIORITY_NO_POWER;
                break;
            default:
                accuracy_level.setSelection(1);
                location_accuracy = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY;
                break;
        }
        
        Button submit = (Button) findViewById(R.id.submit);
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if( update_frequency.getText().length() > 0 ) {
                    Aware.setSetting(getApplicationContext(), Settings.FREQUENCY_GOOGLE_FUSED_LOCATION, update_frequency.getText().toString());
                }
                if( max_update_frequency.getText().length() > 0) {
                    Aware.setSetting(getApplicationContext(), Settings.MAX_FREQUENCY_GOOGLE_FUSED_LOCATION, max_update_frequency.getText().toString());
                }
                switch( accuracy_level.getSelectedItemPosition() ) {
                    case 0:
                        Aware.setSetting(getApplicationContext(), Settings.ACCURACY_GOOGLE_FUSED_LOCATION, LocationRequest.PRIORITY_HIGH_ACCURACY);
                        break;
                    case 1:
                        Aware.setSetting(getApplicationContext(), Settings.ACCURACY_GOOGLE_FUSED_LOCATION, LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
                        break;
                    case 2:
                        Aware.setSetting(getApplicationContext(), Settings.ACCURACY_GOOGLE_FUSED_LOCATION, LocationRequest.PRIORITY_LOW_POWER);
                        break;
                    case 3:
                    	Aware.setSetting(getApplicationContext(), Settings.ACCURACY_GOOGLE_FUSED_LOCATION, LocationRequest.PRIORITY_NO_POWER);
                        break;
                }
                
                Aware.startPlugin(getApplicationContext(), "com.aware.plugin.google.fused_location");

                finish();
            }
        });
    }

    @Override
    public void onPostCreate(Bundle savedInstanceState, PersistableBundle persistentState) {
        super.onPostCreate(savedInstanceState, persistentState);

        Toolbar aware_toolbar = (Toolbar) findViewById(R.id.aware_toolbar);
        setSupportActionBar(aware_toolbar);
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        update_frequency.setText(Aware.getSetting(getApplicationContext(), Settings.FREQUENCY_GOOGLE_FUSED_LOCATION));
        max_update_frequency.setText(Aware.getSetting(getApplicationContext(), Settings.MAX_FREQUENCY_GOOGLE_FUSED_LOCATION));
        
        switch(Integer.parseInt(Aware.getSetting(getApplicationContext(), Settings.ACCURACY_GOOGLE_FUSED_LOCATION))) {
            case LocationRequest.PRIORITY_HIGH_ACCURACY:
                accuracy_level.setSelection(0);
                location_accuracy = LocationRequest.PRIORITY_HIGH_ACCURACY;
                break;
            case LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY:
                accuracy_level.setSelection(1);
                location_accuracy = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY;
                break;
            case LocationRequest.PRIORITY_LOW_POWER:
                accuracy_level.setSelection(2);
                location_accuracy = LocationRequest.PRIORITY_LOW_POWER;
                break;
            case LocationRequest.PRIORITY_NO_POWER:
                accuracy_level.setSelection(3);
                location_accuracy = LocationRequest.PRIORITY_NO_POWER;
                break;
            default:
                accuracy_level.setSelection(1);
                location_accuracy = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY;
                break;
        }
    }
}
