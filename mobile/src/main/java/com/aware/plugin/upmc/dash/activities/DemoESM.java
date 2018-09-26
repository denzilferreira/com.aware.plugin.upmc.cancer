package com.aware.plugin.upmc.dash.activities;

import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.aware.plugin.upmc.dash.R;

public class DemoESM extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Handler handler = new Handler();
        Toast.makeText(getApplication(), "Demo is starting...", Toast.LENGTH_LONG).show();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                setContentView(R.layout.activity_upmc_dash);
                setUpDials();
                Toast.makeText(getApplication(), "This is a demo. Click SAVE ANSWERS to exit", Toast.LENGTH_SHORT).show();
                LinearLayout linearLayout = (LinearLayout) findViewById(R.id.morning_questions);
                linearLayout.setVisibility(View.VISIBLE);
                Button saveAnswers = (Button) findViewById(R.id.answer_questionnaire);
                saveAnswers.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Toast.makeText(getApplication(), "Demo has ended..", Toast.LENGTH_LONG).show();
                        finish();
                    }
                });
            }
        }, 4000);
    }

    public void setUpDials() {
        final TextView pain_rating = (TextView) findViewById(R.id.pain_rating);
        pain_rating.setText("-1");
        SeekBar pain = (SeekBar) findViewById(R.id.rate_pain);
        pain.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                pain_rating.setText(String.valueOf(i));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        final TextView fatigue_rating = (TextView) findViewById(R.id.fatigue_rating);
        fatigue_rating.setText("-1");
        SeekBar fatigue = (SeekBar) findViewById(R.id.rate_fatigue);
        fatigue.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                fatigue_rating.setText(String.valueOf(i));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        final TextView concentrating_rating = (TextView) findViewById(R.id.concentrating_rating);
        concentrating_rating.setText("-1");
        SeekBar concentrating = (SeekBar) findViewById(R.id.rate_concentrating);
        concentrating.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                concentrating_rating.setText(String.valueOf(i));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        final TextView sad_rating = (TextView) findViewById(R.id.sad_rating);
        sad_rating.setText("-1");
        SeekBar sad = (SeekBar) findViewById(R.id.rate_sad);
        sad.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                sad_rating.setText(String.valueOf(i));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        final TextView anxious_rating = (TextView) findViewById(R.id.anxious_rating);
        anxious_rating.setText("-1");
        SeekBar anxious = (SeekBar) findViewById(R.id.rate_anxious);
        anxious.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                anxious_rating.setText(String.valueOf(i));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        final TextView breath_rating = (TextView) findViewById(R.id.breath_rating);
        breath_rating.setText("-1");
        SeekBar breath = (SeekBar) findViewById(R.id.rate_breath);
        breath.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                breath_rating.setText(String.valueOf(i));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        final TextView numb_rating = (TextView) findViewById(R.id.numb_rating);
        numb_rating.setText("-1");
        SeekBar numb = (SeekBar) findViewById(R.id.rate_numb);
        numb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                numb_rating.setText(String.valueOf(i));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        final TextView nausea_rating = (TextView) findViewById(R.id.nausea_rating);
        nausea_rating.setText("-1");
        SeekBar nausea = (SeekBar) findViewById(R.id.rate_nausea);
        nausea.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                nausea_rating.setText(String.valueOf(i));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        final TextView sleep_disturb_rating = (TextView) findViewById(R.id.sleep_disturbance_rating);
        sleep_disturb_rating.setText("-1");
        SeekBar sleep_disturb = (SeekBar) findViewById(R.id.rate_sleep_disturbance);
        sleep_disturb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                sleep_disturb_rating.setText(String.valueOf(i));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        final TextView diarrhea_rating = (TextView) findViewById(R.id.diarrhea_rating);
        diarrhea_rating.setText("-1");
        SeekBar diarrhea = (SeekBar) findViewById(R.id.rate_diarrhea);
        diarrhea.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                diarrhea_rating.setText(String.valueOf(i));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        final TextView other_rating = (TextView) findViewById(R.id.other_rating);
        other_rating.setText("-1");
        final TextView other_label = (TextView) findViewById(R.id.lbl_other);
        other_label.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Dialog other_labeler = new Dialog(DemoESM.this);
                other_labeler.setTitle("Can you be more specific, please?");
                other_labeler.getWindow().setGravity(Gravity.TOP);
                other_labeler.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);

                LinearLayout editor = new LinearLayout(DemoESM.this);
                editor.setOrientation(LinearLayout.VERTICAL);
                other_labeler.setContentView(editor);
                other_labeler.show();

                final EditText label = new EditText(DemoESM.this);
                label.setHint("Can you be more specific, please?");
                editor.addView(label);
                label.requestFocus();
                other_labeler.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

                Button confirm = new Button(DemoESM.this);
                confirm.setText("OK");
                confirm.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (label.getText().length() == 0) label.setText("Other");
                        other_label.setText(label.getText().toString());
                        other_labeler.dismiss();
                    }
                });

                editor.addView(confirm);
            }
        });

        SeekBar other = (SeekBar) findViewById(R.id.rate_other);
        other.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                other_rating.setText(String.valueOf(i));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (other_label.getText().equals("Other")) {
                    final Dialog other_labeler = new Dialog(DemoESM.this);
                    other_labeler.getWindow().setGravity(Gravity.TOP);
                    other_labeler.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);

                    LinearLayout editor = new LinearLayout(DemoESM.this);
                    editor.setOrientation(LinearLayout.VERTICAL);
                    other_labeler.setContentView(editor);
                    other_labeler.show();

                    final EditText label = new EditText(DemoESM.this);
                    label.setHint("Can you be more specific, please?");
                    editor.addView(label);
                    label.requestFocus();
                    other_labeler.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

                    Button confirm = new Button(DemoESM.this);
                    confirm.setText("OK");
                    confirm.setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            if (label.getText().length() == 0) label.setText("Other");
                            other_label.setText(label.getText().toString());
                            other_labeler.dismiss();
                        }
                    });
                    editor.addView(confirm);
                }
            }
        });

    }

}