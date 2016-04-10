package com.thecompanyisalie.timebeeps;

        import android.app.Activity;
        import android.content.Context;
        import android.graphics.Rect;
        import android.media.MediaPlayer;
        import android.media.Ringtone;
        import android.media.RingtoneManager;
        import android.net.Uri;
        import android.os.CountDownTimer;
        import android.os.Bundle;
        import android.view.Menu;
        import android.view.MenuInflater;
        import android.view.MenuItem;
        import android.view.MotionEvent;
        import android.view.View;
        import android.view.inputmethod.InputMethodManager;
        import android.widget.EditText;
        import android.widget.TextView;
        import android.util.Log;


public class Interval extends Activity {

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if ( v instanceof EditText) {
                Rect outRect = new Rect();
                // er línan fyrir neðan nauðsynleg?? Finna hvað þetta í raun gerir.. virðist ekki hafa áhrif í testi.
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int)event.getRawX(), (int)event.getRawY())) {
                    // clearFocus() færir fókusinn á fyrsta focusable element-ið (t.d. EditText) á síðunni.
                    // personal preference er að hafa fócusinn á sama elementi.
                    // v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent( event );
    }

    CountDownTimer activeTimer;
    CountDownTimer restTimer;

    // set initial values:
    boolean timerOn = false;
    boolean isPaused = false;
    boolean isUnpaused = false;
    boolean inActiveTimer = false;
    boolean inRestTimer = false;
    // setting default values, in case input is not usable
    Integer active = 30;
    Integer rest = 60;
    Integer sets = 5;

    public Integer getInput(EditText field, Integer defValue ) {
        if((field.getText().toString()).equals("")) return defValue;
        try {
            return Integer.parseInt(field.getText().toString());
        } catch (Exception e) {
            // reaching here means the input is non-empty-string non-integers..
            return null;
        }
    }

    // called when used click the Start button
    public void startTimer (final View view) {
        // only if program is not running already do we start it.
        //  a.k.a.: ignoring start button pressed if program is running.
        if (!timerOn) {
            timerOn = true;

            // get references to Active, Rest and Sets fields. of both Edit and Remaining fields.
            final EditText active_duration = (EditText) findViewById(R.id.assignSetActive);
            final TextView remaining_active = (TextView) findViewById(R.id.activeRemaining);

            final EditText rest_duration = (EditText) findViewById(R.id.assignSetRest);
            final TextView remaining_rest = (TextView) findViewById(R.id.restRemaining);

            final EditText totalSets = (EditText) findViewById(R.id.assignSets);
            final TextView remaining_sets = (TextView) findViewById(R.id.setsRemaining);

            active = getInput(active_duration, active);
            rest = getInput(rest_duration, rest);
            sets = getInput(totalSets, sets);

            remaining_active.setText("" + active);
            remaining_rest.setText("" + rest);
            remaining_sets.setText("" + sets);

            // start with the Active timer (the Rest one handles new Sets, so should be after):
            doTimerActive();
        }
    }

    public void stopTimer (final View view) {
        if (timerOn) {
            if (inActiveTimer) activeTimer.cancel();
            if (inRestTimer) restTimer.cancel();
            // re-set initial values:
            timerOn = false;
            isPaused = false;
            isUnpaused = false;
            inActiveTimer = false;
            inRestTimer = false;
            active = 30;
            rest = 60;
            sets = 5;
        }
    }

    public void pauseTimer (final View view){
        if (timerOn) {
            isPaused = !isPaused;

            if (isPaused) {
                if (inActiveTimer) activeTimer.cancel();
                if (inRestTimer) restTimer.cancel();
            } else {
                isUnpaused = true;
                if (inActiveTimer) doTimerActive();
                else if (inRestTimer) doTimerRest();
            }
        }

    }

    public void doTimerActive() {
        inActiveTimer = true;

        if (isUnpaused) {
            final TextView remaining_active = (TextView) findViewById(R.id.activeRemaining);
            active = Integer.parseInt(remaining_active.getText().toString());
        }
        isUnpaused = false;

        activeTimer = new CountDownTimer(active * 1000, 100) {
            public void onTick(long millisUntilFinished) {
                performTickActive(millisUntilFinished);
            }

            public void onFinish() {
                playSound();
                inActiveTimer = false;
                doTimerRest();
            }
        }.start();
    }

    public void doTimerRest() {
        inRestTimer = true;

        if (isUnpaused) {
            final TextView remaining_rest = (TextView) findViewById(R.id.restRemaining);
            rest = Integer.parseInt(remaining_rest.getText().toString());
        }
        isUnpaused = false;

        restTimer = new CountDownTimer(rest*1000, 100) {
            public void onTick(long millisUntilFinished) {performTickRest(millisUntilFinished);}

            public void onFinish() {
                TextView remaining_sets = (TextView) findViewById(R.id.setsRemaining);

                Integer sets_left = Integer.parseInt(remaining_sets.getText().toString());

                if (sets_left > 1) {
                    try {
                        sets_left = sets_left-1;
                        performTickSet(sets_left);
                        remaining_sets.setText("" + sets_left);
                        playSound();
                        inRestTimer = false;
                        doTimerActive();
                    } catch (Exception e) {
                        System.out.println("Error: " + e.toString());
                    }
                } else {
                    playSoundFinal();
                    // re-set initial values:
                    timerOn = false;
                    isPaused = false;
                    isUnpaused = false;
                    inActiveTimer = false;
                    inRestTimer = false;
                    active = 30;
                    rest = 60;
                    sets = 5;
                }
            }
        }.start();
    }

    public void performTickActive(long millisUntilFinished) {
        TextView until_next_beep_elem = (TextView) findViewById(R.id.activeRemaining);
        until_next_beep_elem.setText("" + String.valueOf(Math.round(millisUntilFinished * 0.001f)));
    }

    public void performTickRest(long millisUntilFinished) {
        TextView until_next_beep_elem = (TextView) findViewById(R.id.restRemaining);
        until_next_beep_elem.setText("" + String.valueOf(Math.round(millisUntilFinished * 0.001f)));
    }

    public void performTickSet(Integer setNumber) {
        TextView until_next_beep_elem = (TextView) findViewById(R.id.setsRemaining);
        until_next_beep_elem.setText("" + setNumber);
    }

    public void playSound() {
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            MediaPlayer mp = MediaPlayer.create(getApplicationContext(), notification);
            mp.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void playSoundFinal() {
        TextView until_next_beep_INT = (TextView) findViewById(R.id.activeRemaining);
        TextView until_next_interval_INT = (TextView) findViewById(R.id.restRemaining);
        TextView until_next_set_INT = (TextView) findViewById(R.id.setsRemaining);
        until_next_beep_INT.setText("done!");
        until_next_interval_INT.setText("done!");
        until_next_set_INT.setText("done!");

        try {
            new CountDownTimer(3000, 500) {
                public void onTick(long millisUntilFinished) {
                    Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                    Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                    r.play();
                }

                public void onFinish() {}
            }.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_interval);
        // If your minSdkVersion is lower than 11, instead use:
        // getActionBar().setDisplayHomeAsUpEnabled(true);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_interval, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
