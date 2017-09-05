package com.aware.plugin.upmc.dash;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

public class InabilityResponseForm extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final CharSequence[] items = {"Busy", "Pain", "Nausea", "Other"};
        final boolean[] selectedItems = new boolean[items.length];

        LayoutInflater factory = LayoutInflater.from(this);
        View inabilityForm = factory.inflate(R.layout.content_inability_response_form,null);


        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);

        dialogBuilder.setTitle("Why are you unable to walk?")
                .setCancelable(false)
                .setView(inabilityForm)
//                .setMultiChoiceItems(items, selectedItems, new DialogInterface.OnMultiChoiceClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialogInterface, int i, boolean b) {
//
//                    }
//                }).setPositiveButton("Submit", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialogInterface, int i) {
//                        if(selectedItems[3]) {
//                            Toast.makeText(getApplicationContext(), "Please specify what your reason is", Toast.LENGTH_LONG).show();
//                        }
//                        else {
//                            Log.d(Constants.TAG, "IRF:: Here");
//                            dialogInterface.cancel();
//                        }
//                    }
//                })
                .create().show();




    }

}
