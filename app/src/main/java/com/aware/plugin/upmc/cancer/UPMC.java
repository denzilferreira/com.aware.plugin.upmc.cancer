package com.aware.plugin.upmc.cancer;

import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.aware.Aware;
import com.aware.Aware_Preferences;

import java.util.Calendar;

public class UPMC extends ActionBarActivity {

    private static SharedPreferences prefs;
    private static String DEVICE_ID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(getPackageName(), MODE_PRIVATE);

        Intent aware = new Intent(this, Aware.class);
        startService(aware);

        DEVICE_ID = Aware.getSetting(this, Aware_Preferences.DEVICE_ID);

        Log.d(Plugin.TAG, "Device ID:" + DEVICE_ID);

        Aware.startPlugin(this, getPackageName());

        Intent joinStudy = new Intent(this, Aware_Preferences.StudyConfig.class);
        joinStudy.putExtra(Aware_Preferences.StudyConfig.EXTRA_JOIN_STUDY, "https://api.awareframework.com/index.php/webservice/index/205/tgj4NVrQK5Wl");
        startService(joinStudy);
    }

    private void loadSchedule() {
        setContentView(R.layout.settings_upmc_cancer);
        ImageButton saveSchedule = (ImageButton) findViewById(R.id.save_button);

        final TimePicker morning_timer = (TimePicker) findViewById(R.id.morning_start_time);
        morning_timer.setIs24HourView(true);
        if( prefs.contains("morning_hours") ) {
            morning_timer.setCurrentHour(prefs.getInt("morning_hours",0));
        }
        if( prefs.contains("morning_minutes")) {
            morning_timer.setCurrentMinute(prefs.getInt("morning_minutes",0));
        }
        
        final TimePicker evening_timer = (TimePicker) findViewById(R.id.evening_start_time);
        evening_timer.setIs24HourView(true);
        if( prefs.contains("evening_hours") ) {
            evening_timer.setCurrentHour(prefs.getInt("evening_hours",0));
        }
        if( prefs.contains("evening_minutes")) {
            evening_timer.setCurrentMinute(prefs.getInt("evening_minutes",0));
        }
        saveSchedule.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt("morning_hours", morning_timer.getCurrentHour());
                editor.putInt("morning_minutes", morning_timer.getCurrentMinute());
                editor.putInt("evening_hours", evening_timer.getCurrentHour());
                editor.putInt("evening_minutes", evening_timer.getCurrentMinute());

                editor.putBoolean("scheduled", true);
                editor.commit();

                Toast.makeText(getApplicationContext(), "Applying new schedule...", Toast.LENGTH_SHORT).show();

                Intent restart = new Intent( getApplicationContext(), Plugin.class);
                restart.setAction(Plugin.ACTION_PLUGIN_UPMC_CANCER_SCHEDULE);
                startService(restart);

                finish();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if( ! prefs.contains("scheduled") ) {
            loadSchedule();
            return;
        }

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(System.currentTimeMillis());

        setContentView(R.layout.activity_upmc_cancer);

        final LinearLayout morning_questions = (LinearLayout) findViewById(R.id.morning_questions);
        final LinearLayout evening_questions = (LinearLayout) findViewById(R.id.evening_questions);

        final TimePicker to_bed = (TimePicker) findViewById(R.id.bed_time);
        final TimePicker from_bed = (TimePicker) findViewById(R.id.woke_time);
        final RadioGroup qos_sleep = (RadioGroup) findViewById(R.id.qos_sleep);

        final RadioGroup qos_stress = (RadioGroup) findViewById(R.id.quality_of_stress);
        final EditText most_stress = (EditText) findViewById(R.id.most_stressed_moment);

        if( cal.get(Calendar.HOUR_OF_DAY) >= 8 && cal.get(Calendar.HOUR_OF_DAY) <= 11 ) {
            morning_questions.setVisibility(View.VISIBLE);
            evening_questions.setVisibility(View.GONE);
        }
        if( cal.get(Calendar.HOUR_OF_DAY) >= 20 && cal.get(Calendar.HOUR_OF_DAY) <= 23 ) {
            morning_questions.setVisibility(View.GONE);
            evening_questions.setVisibility(View.VISIBLE);
        }

        final TextView pain_rating = (TextView) findViewById(R.id.pain_rating);
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

        final TextView disconnected_rating = (TextView) findViewById(R.id.disconnected_rating);
        SeekBar disconnected = (SeekBar) findViewById(R.id.rate_disconnected);
        disconnected.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                disconnected_rating.setText(String.valueOf(i));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        final TextView concentrating_rating = (TextView) findViewById(R.id.concentrating_rating);
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

        final TextView not_enjoying_rating = (TextView) findViewById(R.id.not_enjoying_rating);
        SeekBar not_enjoying = (SeekBar) findViewById(R.id.rate_not_enjoying);
        not_enjoying.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                not_enjoying_rating.setText(String.valueOf(i));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        final TextView irritable_rating = (TextView) findViewById(R.id.irritable_rating);
        SeekBar irritable = (SeekBar) findViewById(R.id.rate_irritable);
        irritable.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                irritable_rating.setText(String.valueOf(i));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        final TextView breath_rating = (TextView) findViewById(R.id.breath_rating);
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

        final TextView appetite_rating = (TextView) findViewById(R.id.appetite_rating);
        SeekBar appetite = (SeekBar) findViewById(R.id.rate_appetite);
        appetite.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                appetite_rating.setText(String.valueOf(i));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

       final TextView other_rating = (TextView) findViewById(R.id.other_rating);
        final TextView other_label = (TextView) findViewById(R.id.lbl_other);
        other_label.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Dialog other_labeler = new Dialog(UPMC.this);
                other_labeler.setTitle("Can you be more specific, please?");
                other_labeler.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                other_labeler.getWindow().setGravity(Gravity.TOP);
                other_labeler.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);

                LinearLayout editor = new LinearLayout(UPMC.this);
                editor.setOrientation(LinearLayout.VERTICAL);
                other_labeler.setContentView(editor);
                other_labeler.show();

                final EditText label = new EditText(UPMC.this);
                label.setText(other_label.getText());
                editor.addView(label);

                Button confirm = new Button(UPMC.this);
                confirm.setText("OK");
                confirm.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if( label.getText().length() == 0 ) label.setText("Other");
                        other_label.setText(label.getText().toString());
                        InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        inputManager.hideSoftInputFromWindow(label.getWindowToken(), 0);
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
                if( other_label.getText().equals("Other") ) {
                    final Dialog other_labeler = new Dialog(UPMC.this);
                    other_labeler.setTitle("Can you be more specific, please?");
                    other_labeler.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                    other_labeler.getWindow().setGravity(Gravity.TOP);
                    other_labeler.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);

                    LinearLayout editor = new LinearLayout(UPMC.this);
                    editor.setOrientation(LinearLayout.VERTICAL);
                    other_labeler.setContentView(editor);
                    other_labeler.show();

                    final EditText label = new EditText(UPMC.this);
                    editor.addView(label);

                    Button confirm = new Button(UPMC.this);
                    confirm.setText("OK");
                    confirm.setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            if( label.getText().length() == 0 ) label.setText("Other");
                            other_label.setText(label.getText().toString());
                            InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                            inputManager.hideSoftInputFromWindow(label.getWindowToken(), 0);
                            other_labeler.dismiss();
                        }
                    });

                    editor.addView(confirm);
                }
            }
        });



        final ImageButton answer_questions = (ImageButton) findViewById(R.id.answer_questionnaire);
        answer_questions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                ContentValues answer = new ContentValues();
                answer.put(Provider.Cancer_Data.DEVICE_ID, DEVICE_ID);
                answer.put(Provider.Cancer_Data.TIMESTAMP, System.currentTimeMillis());

                if( morning_questions.getVisibility() == View.VISIBLE ) {
                    answer.put(Provider.Cancer_Data.TO_BED, to_bed.getCurrentHour() + "h" + to_bed.getCurrentMinute());
                    answer.put(Provider.Cancer_Data.FROM_BED, from_bed.getCurrentHour() + "h"+from_bed.getCurrentMinute());
                    answer.put(Provider.Cancer_Data.SCORE_SLEEP, (String) ((RadioButton) findViewById(qos_sleep.getCheckedRadioButtonId())).getText());
                }

                if( evening_questions.getVisibility() == View.VISIBLE ) {
                    answer.put(Provider.Cancer_Data.MOST_STRESS_LABEL, most_stress.getText().toString());
                    answer.put(Provider.Cancer_Data.SCORE_STRESS, (String) ((RadioButton) findViewById(qos_stress.getCheckedRadioButtonId())).getText());
                }

                answer.put(Provider.Cancer_Data.SCORE_PAIN, pain_rating.getText().toString());
                answer.put(Provider.Cancer_Data.SCORE_FATIGUE, fatigue_rating.getText().toString());
                answer.put(Provider.Cancer_Data.SCORE_DISCONNECTED, disconnected_rating.getText().toString());
                answer.put(Provider.Cancer_Data.SCORE_CONCENTRATING, concentrating_rating.getText().toString());
                answer.put(Provider.Cancer_Data.SCORE_SAD, sad_rating.getText().toString());
                answer.put(Provider.Cancer_Data.SCORE_ANXIOUS, anxious_rating.getText().toString());
                answer.put(Provider.Cancer_Data.SCORE_ENJOY, not_enjoying_rating.getText().toString());
                answer.put(Provider.Cancer_Data.SCORE_IRRITABLE, irritable_rating.getText().toString());
                answer.put(Provider.Cancer_Data.SCORE_SHORT_BREATH, breath_rating.getText().toString());
                answer.put(Provider.Cancer_Data.SCORE_NUMBNESS, numb_rating.getText().toString());
                answer.put(Provider.Cancer_Data.SCORE_NAUSEA, nausea_rating.getText().toString());
                answer.put(Provider.Cancer_Data.SCORE_APPETITE, appetite_rating.getText().toString());
                answer.put(Provider.Cancer_Data.SCORE_OTHER, other_rating.getText().toString());
                answer.put(Provider.Cancer_Data.OTHER_LABEL, other_label.getText().toString());

                getContentResolver().insert(Provider.Cancer_Data.CONTENT_URI, answer);

                Log.d("UPMC", "Answers:" + answer.toString());

                Toast.makeText(getApplicationContext(), "Saved successfully.", Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_upmc, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            loadSchedule();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
