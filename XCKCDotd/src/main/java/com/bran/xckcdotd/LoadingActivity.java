package com.bran.xckcdotd;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.os.Bundle;
import android.app.Activity;
import android.os.Handler;
import android.view.Menu;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Random;

public class LoadingActivity extends Activity {
    // SharedPreferences key, etc.
    private static final String INTERRUPTED_KEY = "interrupted";
    private static final String PROGRESS_KEY = "progress-status";
    private static final String BACKGROUND_RESOURCE_KEY = "background-resource";

    // Background images and random selector
    private static final int[] backgrounds = new int[]{R.drawable.loading_bg1, R.drawable.loading_bg2,
            R.drawable.loading_bg3, R.drawable.loading_bg4, R.drawable.loading_bg5};
    private static Random generator = new Random();

    // UI Thread handler for UI update requests
    private Handler mHandler = new Handler();

    // Load duration settings and progess stats
    private static final int SECONDS_TO_LOAD = 8;
    private static final int PROG_UPDATE_THREAD_DELAY = SECONDS_TO_LOAD * 1000 / 100;
    private int mProgressStatus;
    private Boolean mLoaded;

    // UI
    private ProgressBar mProgress;
    private ImageView background;
    private int backgroundResource;
    private TextView statusBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);

        mLoaded = false;
        // Retrieve any interrupted progress
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        Boolean interrupted = preferences.getBoolean(INTERRUPTED_KEY, false);
        if(interrupted) {
            mProgressStatus = preferences.getInt(PROGRESS_KEY, 0);
            backgroundResource = preferences.getInt(BACKGROUND_RESOURCE_KEY, 0);
        }

        // Set up UI
        mProgress = (ProgressBar) findViewById(R.id.loading_progress);
        mProgress.getProgressDrawable().setColorFilter(getResources().getColor(R.color.blue), PorterDuff.Mode.SRC_IN);

        background = (ImageView) findViewById(R.id.loading_background);
        if(backgroundResource==0) backgroundResource = backgrounds[generator.nextInt(backgrounds.length)];
        background.setImageResource(backgroundResource);

        statusBar = (TextView) findViewById(R.id.loading_status);
        Typeface tf = Typeface.createFromAsset(getAssets(), "myriad.ttf");
        statusBar.setTypeface(tf);

        // Progress update thread
        new Thread(new Runnable() {
            public void run() {
                while(!mLoaded) {
                    // Update the progress bar
                    mProgressStatus++;
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mProgress.setProgress(mProgressStatus);
                        }
                    });
                    mLoaded = mProgressStatus==100;

                    try { Thread.sleep(PROG_UPDATE_THREAD_DELAY); } // ~50fps
                    catch (Exception e) { e.printStackTrace(); }
                }
                mLoaded = true;
                enterApp();
            }
        }).start();

        // Background zooming thread
        new Thread(new Runnable() {
            public void run() {
                float scaleX = background.getScaleX();
                float scaleY = background.getScaleY();
                final float endScaleX = scaleX * 1.1f;
                final float endScaleY = scaleY * 1.1f;
                ScaleAnimation scale = new ScaleAnimation(scaleX, endScaleX, scaleY, endScaleY, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                scale.setDuration(SECONDS_TO_LOAD * 1000);
                scale.setFillAfter(true);
                background.startAnimation(scale);
            }
        }).start();
    }

    private void enterApp() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onStop(){
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        super.onStop();
        if(!mLoaded) { // interrupted
            editor.putBoolean(INTERRUPTED_KEY, true);
            editor.putInt(PROGRESS_KEY, mProgressStatus);
            editor.putInt(BACKGROUND_RESOURCE_KEY, backgroundResource);
            editor.commit();
        }
        else { // not interrupted
            editor.putBoolean(INTERRUPTED_KEY, false);
            editor.commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.loading, menu);
        return true;
    }

}
