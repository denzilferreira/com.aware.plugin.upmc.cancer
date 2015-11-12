package com.aware.plugin.google.activity_recognition;

import java.util.Calendar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.aware.ui.Stream_UI;
import com.aware.utils.Converters;
import com.aware.utils.IContextCard;

/**
 * New Stream UI cards<br/>
 * Implement here what you see on your Plugin's UI.
 * @author denzilferreira
 */
public class ContextCard implements IContextCard {

    private int refresh_interval = 60 * 1000; //once a minute

    //You may use sContext on uiChanger to do queries to databases, etc.
    private Context sContext;

    //Declare here all the UI elements you'll be accessing
    private View card;
    private TextView still, walking, running, biking, driving;

    public ContextCard(){}

    private android.os.Handler uiRefresher = new Handler(Looper.getMainLooper());
    private Runnable uiChanger = new Runnable() {
        @Override
        public void run() {
            //Modify card's content here once it's initialized
            if( card != null ) {
                Calendar mCalendar = Calendar.getInstance();
                mCalendar.setTimeInMillis(System.currentTimeMillis());

                //Modify time to be at the begining of today
                mCalendar.set(Calendar.HOUR_OF_DAY, 0);
                mCalendar.set(Calendar.MINUTE, 0);
                mCalendar.set(Calendar.SECOND, 0);
                mCalendar.set(Calendar.MILLISECOND, 0);

                //Get stats for today
                still.setText(Converters.readable_elapsed(Stats.getTimeStill(sContext.getContentResolver(), mCalendar.getTimeInMillis(), System.currentTimeMillis())));
                walking.setText(Converters.readable_elapsed(Stats.getTimeWalking(sContext.getContentResolver(), mCalendar.getTimeInMillis(), System.currentTimeMillis())));
                running.setText(Converters.readable_elapsed(Stats.getTimeRunning(sContext.getContentResolver(), mCalendar.getTimeInMillis(), System.currentTimeMillis())));
                biking.setText(Converters.readable_elapsed(Stats.getTimeBiking(sContext.getContentResolver(), mCalendar.getTimeInMillis(), System.currentTimeMillis())));
                driving.setText(Converters.readable_elapsed(Stats.getTimeVehicle(sContext.getContentResolver(), mCalendar.getTimeInMillis(), System.currentTimeMillis())));
            }

            //Reset timer and schedule the next card refresh
            uiRefresher.postDelayed(uiChanger, refresh_interval);
        }
    };

	public View getContextCard(Context context) {

        sContext = context;

        //Tell Android that you'll monitor the stream statuses
        IntentFilter filter = new IntentFilter();
        filter.addAction(Stream_UI.ACTION_AWARE_STREAM_OPEN);
        filter.addAction(Stream_UI.ACTION_AWARE_STREAM_CLOSED);
        context.registerReceiver(streamObs, filter);

        //Load card information to memory
        LayoutInflater sInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        card = sInflater.inflate(R.layout.layout, null);

        //Initialize UI elements from the card
        still = (TextView) card.findViewById(R.id.time_still);
        walking = (TextView) card.findViewById(R.id.time_walking);
        biking = (TextView) card.findViewById(R.id.time_biking);
        running = (TextView) card.findViewById(R.id.time_running);
        driving = (TextView) card.findViewById(R.id.time_vehicle);

        //Begin refresh cycle
        uiRefresher.post(uiChanger);

        //Return the card to AWARE/apps
        return card;
	}

    //This is a BroadcastReceiver that keeps track of stream status. Used to stop the refresh when user leaves the stream and restart again otherwise
    private StreamObs streamObs = new StreamObs();
    public class StreamObs extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if( intent.getAction().equals(Stream_UI.ACTION_AWARE_STREAM_OPEN) ) {
                //start refreshing when user enters the stream
                uiRefresher.postDelayed(uiChanger, refresh_interval);
            }
            if( intent.getAction().equals(Stream_UI.ACTION_AWARE_STREAM_CLOSED) ) {
                //stop refreshing when user leaves the stream
                uiRefresher.removeCallbacks(uiChanger);
                uiRefresher.removeCallbacksAndMessages(null);
            }
        }
    }
}
