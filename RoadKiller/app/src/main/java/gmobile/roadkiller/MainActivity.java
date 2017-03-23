package gmobile.roadkiller;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.PersistableBundle;
import android.preference.PreferenceManager;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import gmobile.basegameutils.BaseGameActivity;

/**
 * Created by mark on 3/20/17.
 */

public class MainActivity extends BaseGameActivity {
    SharedPreferences sp;
    SharedPreferences.Editor ed;
    MediaPlayer mp;
    SoundPool sndpool;

    int snd_result;
    int snd_go;
    int snd_time_up;
    int snd_game_over;
    int snd_hit;
    int snd_explode;
    int snd_fire;
    int score;
    int t;
    int screen_width;
    int screen_height;
    int current_section = R.id.main;
    boolean show_leaderboard;
    float mouse_x;
    float mouse_y;
    View hero;
    View wheel1;
    View wheel2;
    View ground0;
    View ground1;
    View ground_over0;
    View ground_over1;
    List<View> cars = new ArrayList<View>();
    List<Float> cars_speeds = new ArrayList<Float>();
    List<ImageView> cars_wheels = new ArrayList<ImageView>();
    List<ImageView> rockets = new ArrayList<ImageView>();
    List<ImageView> times = new ArrayList<ImageView>();
    boolean game_paused;
    AnimationDrawable anim_explode;
    float speed;
    float y_min;
    float y_max;
    int num_rockets;
    ObjectAnimator anim;
    boolean isForeground = true;

    final float max_speed = 6f; // hero max speed
    final float turn_speed = 3f; // hero turn speed
    final int num_cars = 7; // number of cars on the road
    final float car_bumper = 22; // center vertical point of car hit
    final float hit_area = 5; // hit area
    final int added_time = 10; // time to add when get gas
    Handler h = new Handler();

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.main);
        // don't automatically sign in
        mHelper.setMaxAutoSignInAttempts(0);

        // fullscreen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // preferences
        sp = PreferenceManager.getDefaultSharedPreferences(this);
        ed = sp.edit();

        // bg sound
        mp = new MediaPlayer();
        try {
            AssetFileDescriptor descriptor = getAssets().openFd("snd_bg.mp3");
            mp.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
            descriptor.close();
            mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mp.setLooping(true);
            mp.setVolume(0, 0);
            mp.prepare();
            mp.start();
        } catch (Exception e) {
        }

        // if mute
        if (sp.getBoolean("mute", false)) {
            ((Button) findViewById(R.id.btn_sound)).setText(getString(R.string.btn_sound));
        } else {
            mp.setVolume(0.2f, 0.2f);
        }

        // SoundPool
        sndpool = new SoundPool(3, AudioManager.STREAM_MUSIC, 0);
        try {
            snd_result = sndpool.load(getAssets().openFd("snd_result.mp3"), 1);
            snd_go = sndpool.load(getAssets().openFd("snd_go.mp3"), 1);
            snd_time_up = sndpool.load(getAssets().openFd("snd_time_up.mp3"), 1);
            snd_explode = sndpool.load(getAssets().openFd("snd_explode.mp3"), 1);
            snd_hit = sndpool.load(getAssets().openFd("snd_hit.mp3"), 1);
            snd_fire = sndpool.load(getAssets().openFd("snd_fire.mp3"), 1);
            snd_game_over = sndpool.load(getAssets().openFd("snd_game_over.mp3"), 1);
        } catch (IOException e) {
        }

        // hide navigation bar listener
        findViewById(R.id.all).setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                hide_navigation_bar();
            }
        });

        // hero
        hero = findViewById(R.id.hero);
        wheel1 = findViewById(R.id.wheel1);
        wheel2 = findViewById(R.id.wheel2);

        // add cars, rockets, times
        for (int i = 0; i < num_cars; i++) {
            // rocket
            ImageView rocket = new ImageView(this);
            rocket.setBackgroundResource(R.mipmap.rocket);
            rocket.setLayoutParams(new ActionBar.LayoutParams((int) DpToPx(24), (int) DpToPx(12)));
            ((ViewGroup) findViewById(R.id.game)).addView(rocket);
            rockets.add(rocket);

            // times
            ImageView time = new ImageView(this);
            time.setBackgroundResource(R.mipmap.time);
            time.setLayoutParams(new ActionBar.LayoutParams((int) DpToPx(13), (int) DpToPx(16)));
            ((ViewGroup) findViewById(R.id.game)).addView(time);
            times.add(time);

            // car
            ImageView car = new ImageView(this);
            car.setBackgroundResource(R.mipmap.car);
            car.setLayoutParams(new ActionBar.LayoutParams((int) DpToPx(60), (int) DpToPx(34)));
            ((ViewGroup) findViewById(R.id.game)).addView(car);
            cars.add(car);
            cars_speeds.add(0f);

            // wheel 1
            ImageView wheel = new ImageView(this);
            wheel.setBackgroundResource(R.mipmap.wheel_car);
            wheel.setLayoutParams(new ActionBar.LayoutParams((int) DpToPx(16), (int) DpToPx(16)));
            ((ViewGroup) findViewById(R.id.game)).addView(wheel);
            cars_wheels.add(wheel);

            // wheel 2
            wheel = new ImageView(this);
            wheel.setBackgroundResource(R.mipmap.wheel_car);
            wheel.setLayoutParams(new ActionBar.LayoutParams((int) DpToPx(16), (int) DpToPx(16)));
            ((ViewGroup) findViewById(R.id.game)).addView(wheel);
            cars_wheels.add(wheel);
        }

        // ground
        ground0 = (ImageView) findViewById(R.id.ground0);
        ground1 = (ImageView) findViewById(R.id.ground1);
        ground_over0 = (ImageView) findViewById(R.id.ground_over0);
        ground_over1 = (ImageView) findViewById(R.id.ground_over1);

        // animation explode
        anim_explode = (AnimationDrawable) ((ImageView) findViewById(R.id.explode)).getDrawable();
        anim_explode.start();

        // custom font
        Typeface font = Typeface.createFromAsset(getAssets(), "CooperBlack.otf");
        ((TextView) findViewById(R.id.txt_result)).setTypeface(font);
        ((TextView) findViewById(R.id.txt_high_result)).setTypeface(font);
        ((TextView) findViewById(R.id.txt_score)).setTypeface(font);
        ((TextView) findViewById(R.id.txt_time)).setTypeface(font);
        ((TextView) findViewById(R.id.txt_rockets)).setTypeface(font);
        ((TextView) findViewById(R.id.mess)).setTypeface(font);

        // control touch listener
        findViewById(R.id.btn_control).setOnTouchListener(new View.OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (t != 0 && hero.getVisibility() == View.VISIBLE) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                        case MotionEvent.ACTION_MOVE:
                            mouse_x = event.getX();
                            mouse_y = event.getY();
                            break;
                        case MotionEvent.ACTION_CANCEL:
                        case MotionEvent.ACTION_UP:
                            mouse_x = findViewById(R.id.img_control).getX() + findViewById(R.id.img_control).getWidth() * 0.2f;
                            mouse_y = findViewById(R.id.img_control).getY() + findViewById(R.id.img_control).getHeight() * 0.5f;
                            break;
                    }
                }
                return false;
            }
        });

        // fire touch listener
        findViewById(R.id.btn_fire).setOnTouchListener(new View.OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (t != 0 && hero.getVisibility() == View.VISIBLE && num_rockets != 0
                        && findViewById(R.id.rocket).getVisibility() == View.GONE && event.getAction() == MotionEvent.ACTION_DOWN) {
                    // fire
                    num_rockets--;
                    ((TextView) findViewById(R.id.txt_rockets)).setText(getString(R.string.rockets) + " " + num_rockets);
                    findViewById(R.id.rocket).setVisibility(View.VISIBLE);

                    // rocket start position
                    findViewById(R.id.rocket).setX(hero.getX() + (hero.getWidth() - findViewById(R.id.rocket).getWidth()) * 0.5f);
                    findViewById(R.id.rocket).setY(
                            hero.getY() + DpToPx(car_bumper) - findViewById(R.id.rocket).getHeight() * 0.5f);

                    // sound
                    if (!sp.getBoolean("mute", false) && isForeground)
                        sndpool.play(snd_fire, 0.1f, 0.1f, 0, 0, 1);
                }
                return false;
            }
        });

    }


    // DpToPx
    float DpToPx(float dp) {
        return (dp * Math.max(getResources().getDisplayMetrics().widthPixels, getResources().getDisplayMetrics().heightPixels) / 540f);
    }



    private void hide_navigation_bar() {
    }


    @Override
    public void onSignInSucceeded() {

    }

    @Override
    public void onSignInFailed() {

    }





}
