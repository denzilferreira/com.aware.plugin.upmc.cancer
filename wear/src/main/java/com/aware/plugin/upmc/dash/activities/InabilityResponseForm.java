package com.aware.plugin.upmc.dash.activities;

import android.os.Bundle;

import androidx.recyclerview.widget.RecyclerView;
import android.support.wear.widget.WearableLinearLayoutManager;
import android.support.wear.widget.WearableRecyclerView;
import android.support.wearable.activity.WearableActivity;
import android.widget.TextView;

import com.aware.plugin.upmc.dash.R;
import com.aware.plugin.upmc.dash.adapters.CustomScrollingLayoutCallback;
import com.aware.plugin.upmc.dash.adapters.InabilityAdapter;

public class InabilityResponseForm extends WearableActivity {


    private String[] responses = {"Busy", "Nausea", "Pain", "Other"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inability_response_form);
        WearableRecyclerView wearableRecyclerView = findViewById(R.id.recycler_launcher_view);

//        wearableRecyclerView.setEdgeItemsCenteringEnabled(true);
//        wearableRecyclerView.setCircularScrollingGestureEnabled(true);
//        wearableRecyclerView.setBezelFraction(0.5f);
//        wearableRecyclerView.setScrollDegreesPerScreen(90);
//        wearableRecyclerView.setHasFixedSize(true);

        RecyclerView.LayoutManager mLayoutManager = new WearableLinearLayoutManager(this);
        wearableRecyclerView.setLayoutManager(mLayoutManager);


        InabilityAdapter inabilityAdapter = new InabilityAdapter(responses);
        wearableRecyclerView.setAdapter(inabilityAdapter);


        // Enables Always-on
        setAmbientEnabled();



    }
}
