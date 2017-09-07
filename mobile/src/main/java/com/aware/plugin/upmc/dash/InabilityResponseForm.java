package com.aware.plugin.upmc.dash;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

public class InabilityResponseForm extends AppCompatActivity {

    private int checkCount = 0;
    private EditText editText;
    private Button submitButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final CharSequence[] items = {"Busy", "Pain", "Nausea", "Other"};
        LayoutInflater factory = LayoutInflater.from(this);
        View inabilityForm = factory.inflate(R.layout.content_inability_response_form,null);


        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);

        dialogBuilder.setTitle("Why are you unable to walk?")
                .setCancelable(false)
                .setView(inabilityForm)
                .setPositiveButton("Submit", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                })
                .create();

        AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.show();
        editText = inabilityForm.findViewById(R.id.reason_field);
        submitButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
        submitButton.setEnabled(false);
        editText.setEnabled(false);
        editText.setVisibility(View.INVISIBLE);
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                submitButton.setEnabled(true);
            }
        });

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    public void onCheckBoxClocked(View view) {
        boolean checked = ((CheckBox) view).isChecked();
        switch (view.getId()) {
            case R.id.other_checkbox:
                if(checked) {
                    editText.setEnabled(true);
                    editText.setVisibility(View.VISIBLE);
                    submitButton.setEnabled(false);
                }
                else {
                    editText.setEnabled(false);
                }
                break;
            default:
                checkCount++;
                if(checkCount%2==1){
                    submitButton.setEnabled(true);
                }
                else {
                    submitButton.setEnabled(false);
                }
                break;
        }
    }

}
