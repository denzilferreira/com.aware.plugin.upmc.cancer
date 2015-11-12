package com.aware.plugin.google.fused_location;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.aware.providers.Locations_Provider;
import com.aware.ui.Stream_UI;
import com.aware.utils.IContextCard;

import java.io.IOException;
import java.util.List;

public class ContextCard implements IContextCard {
    public ContextCard(){}

    private int refresh_interval = 60 * 1000; //every minute

    private Handler uiRefresher = new Handler(Looper.getMainLooper());
    private Runnable uiChanger = new Runnable() {
        @Override
        public void run() {
            //Modify card's content here once it's initialized
            if( card != null ) {

                Cursor last_location = sContext.getContentResolver().query(Locations_Provider.Locations_Data.CONTENT_URI, null, null, null, Locations_Provider.Locations_Data.TIMESTAMP + " DESC LIMIT 1");
                if( last_location != null && last_location.moveToFirst() ) {
                    double lat = last_location.getDouble(last_location.getColumnIndex(Locations_Provider.Locations_Data.LATITUDE));
                    double lon = last_location.getDouble(last_location.getColumnIndex(Locations_Provider.Locations_Data.LONGITUDE));

                    try {
                        Geocoder geo = new Geocoder(sContext);
                        String geo_text = "";
                        List<Address> addressList = geo.getFromLocation(lat, lon, 1);
                        for(int i = 0; i<addressList.size(); i++ ) {
                            Address address1 = addressList.get(i);
                            for( int j = 0; j< address1.getMaxAddressLineIndex(); j++ ) {
                                if( address1.getAddressLine(j).length() > 0 ) {
                                    geo_text += address1.getAddressLine(j) + "\n";
                                }
                            }
                            geo_text+=address1.getCountryName();
                        }
                        address.setText(geo_text);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if( last_location != null && ! last_location.isClosed() ) last_location.close();

            }

            //Reset timer and schedule the next card refresh
            uiRefresher.postDelayed(uiChanger, refresh_interval);
        }
    };

    private Context sContext;
    private View card;
    private TextView address;

    @Override
    public View getContextCard(Context context) {

        sContext = context;

        //Tell Android that you'll monitor the stream statuses
        IntentFilter filter = new IntentFilter();
        filter.addAction(Stream_UI.ACTION_AWARE_STREAM_OPEN);
        filter.addAction(Stream_UI.ACTION_AWARE_STREAM_CLOSED);
        context.registerReceiver(streamObs, filter);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        card = inflater.inflate(R.layout.card, null);
        address = (TextView) card.findViewById(R.id.address);

        uiRefresher.post(uiChanger);

        return card;
    }

    //This is a BroadcastReceiver that keeps track of stream status. Used to stop the refresh when user leaves the stream and restart again otherwise
    private StreamObs streamObs = new StreamObs();
    public class StreamObs extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if( intent.getAction().equals(Stream_UI.ACTION_AWARE_STREAM_OPEN) ) {
                //start refreshing when user enters the stream
                uiRefresher.post(uiChanger);
            }
            if( intent.getAction().equals(Stream_UI.ACTION_AWARE_STREAM_CLOSED) ) {
                //stop refreshing when user leaves the stream
                uiRefresher.removeCallbacks(uiChanger);
                uiRefresher.removeCallbacksAndMessages(null);
            }
        }
    }
}
