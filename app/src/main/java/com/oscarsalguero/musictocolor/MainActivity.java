/***
 * Copyright (c) 2015 Oscar Salguero www.oscarsalguero.com
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.oscarsalguero.musictocolor;

import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.audiofx.Equalizer;
import android.media.audiofx.Visualizer;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.IOException;
import java.util.Random;

/**
 * Demonstrates how to get colors from music using the {@link Visualizer} class.
 * Created by RacZo on 9/2/15.
 */
public class MainActivity extends AppCompatActivity {

    private MediaPlayer mMediaPlayer;
    private Equalizer mEqualizer;
    private Visualizer mVisualizer;
    private LinearLayout mLinearLayout;
    private VisualizerView mVisualizerView;
    private TextView mColorTextView;

    private static final String INITIAL_COLOR = "#ff000000";
    private static final String SPACE = " ";

    private static final float VISUALIZER_HEIGHT_DIP = 50f;

    private static final String LOG_TAG = MainActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        TextView mStatusTextView = new TextView(this);
        mLinearLayout = new LinearLayout(this);
        mLinearLayout.setOrientation(LinearLayout.VERTICAL);
        mLinearLayout.addView(mStatusTextView);

        setContentView(mLinearLayout);

        // Create the MediaPlayer
        AssetFileDescriptor assetFileDescriptor = null;
        try {
            assetFileDescriptor = getAssets().openFd("dbz.mp3");

            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setDataSource(
                    assetFileDescriptor.getFileDescriptor(),
                    assetFileDescriptor.getStartOffset(),
                    assetFileDescriptor.getLength());
            mMediaPlayer.prepare();
            mMediaPlayer.start();

        } catch (IllegalArgumentException e) {
            Log.e(LOG_TAG, e.getMessage());
        } catch (IllegalStateException e) {
            Log.e(LOG_TAG, e.getMessage());
        } catch (IOException e) {
            Log.e(LOG_TAG, e.getMessage());
        }

        Log.d(LOG_TAG, "MediaPlayer audio session ID: " + mMediaPlayer.getAudioSessionId());

        setupVisualizerFxAndUI();
        setupEqualizerFxAndUI();

        // Make sure the visualizer is enabled only when you actually want to receive data, and
        // when it makes sense to receive data.
        mVisualizer.setEnabled(true);

        // When the stream ends, we don't need to collect any more data. We don't do this in
        // setupVisualizerFxAndUI because we likely want to have more, non-Visualizer related code
        // in this callback.
        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            public void onCompletion(MediaPlayer mediaPlayer) {
                mVisualizer.setEnabled(false);
            }
        });

        mMediaPlayer.start();
        mStatusTextView.setText(getString(R.string.label_waveform));

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            case R.id.action_about:
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.git_hub_repo_url)));
                startActivity(intent);
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupEqualizerFxAndUI() {

        // TextView to show colors
        mColorTextView = new TextView(this);
        mColorTextView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (int) (VISUALIZER_HEIGHT_DIP * getResources().getDisplayMetrics().density)));
        mColorTextView.setBackgroundColor(Color.BLACK);
        mColorTextView.setTextColor(Color.WHITE);
        mColorTextView.setTextSize(24);
        mColorTextView.setGravity(Gravity.CENTER);

        mLinearLayout.addView(mColorTextView);

        // Create the Equalizer object (an AudioEffect subclass) and attach it to our media player,
        // with a default priority (0).
        mEqualizer = new Equalizer(0, mMediaPlayer.getAudioSessionId());
        mEqualizer.setEnabled(true);

        TextView eqTextView = new TextView(this);
        eqTextView.setText(getString(R.string.label_equalizer));
        mLinearLayout.addView(eqTextView);

        short bands = mEqualizer.getNumberOfBands();

        final short minEQLevel = mEqualizer.getBandLevelRange()[0];
        final short maxEQLevel = mEqualizer.getBandLevelRange()[1];

        for (short i = 0; i < bands; i++) {

            final short band = i;

            TextView freqTextView = new TextView(this);
            freqTextView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            freqTextView.setGravity(Gravity.CENTER_HORIZONTAL);

            freqTextView.setText((mEqualizer.getCenterFreq(band) / 1000) + SPACE + getString(R.string.label_hz));
            mLinearLayout.addView(freqTextView);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);

            TextView minDbTextView = new TextView(this);
            minDbTextView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            minDbTextView.setText((minEQLevel / 100) + SPACE + getString(R.string.label_db));

            TextView maxDbTextView = new TextView(this);
            maxDbTextView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            maxDbTextView.setText((maxEQLevel / 100) + SPACE + getString(R.string.label_db));

            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            layoutParams.weight = 1;

            SeekBar bar = new SeekBar(this);
            bar.setLayoutParams(layoutParams);
            bar.setMax(maxEQLevel - minEQLevel);
            bar.setProgress(mEqualizer.getBandLevel(band));

            bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                public void onProgressChanged(SeekBar seekBar, int progress,
                                              boolean fromUser) {
                    mEqualizer.setBandLevel(band, (short) (progress + minEQLevel));
                }

                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                public void onStopTrackingTouch(SeekBar seekBar) {
                }

            });

            row.addView(minDbTextView);
            row.addView(bar);
            row.addView(maxDbTextView);

            mLinearLayout.addView(row);
        }

    }

    private void setupVisualizerFxAndUI() {
        // Create a VisualizerView (defined below), which will render the simplified audio wave form to a Canvas.
        mVisualizerView = new VisualizerView(this);
        mVisualizerView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (int) (VISUALIZER_HEIGHT_DIP * getResources().getDisplayMetrics().density)));
        mLinearLayout.addView(mVisualizerView);

        // Create the Visualizer object and attach it to our media player.
        mVisualizer = new Visualizer(mMediaPlayer.getAudioSessionId());

        mVisualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);
        mVisualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {

            public void onWaveFormDataCapture(Visualizer visualizer, byte[] bytes, int samplingRate) {
                int color = updateColor(mVisualizerView.getWaveFormHeight());
                mVisualizerView.updateVisualizer(bytes, color);
            }

            public void onFftDataCapture(Visualizer visualizer, byte[] bytes, int samplingRate) {
            }

        }, Visualizer.getMaxCaptureRate() / 2, true, false);
    }

    private int updateColor(float waveFormHeight){

        int min = 1;
        int max = 3;
        Random r = new Random();
        int multiplier = r.nextInt(max - min + 1) + min;

        Random rnd = new Random();
        int alpha = 255;
        int n = Math.round(waveFormHeight) * multiplier;
        if(n > 256){
            n = 256;
        }
        //Log.d(LOG_TAG, "n: " + n);
        int color = Color.argb(alpha, rnd.nextInt(n), rnd.nextInt(n), rnd.nextInt(n));

        String hexColorString = INITIAL_COLOR;
        mColorTextView.setText(getString(R.string.label_hex) + SPACE + hexColorString);
        try {
            // Using the sampling rate as the to generate a HEX string and then an integer value for color
            if(color != Color.WHITE) { // Avoiding white color so the hex value on the TextView is always visible
                mColorTextView.setBackgroundColor(color);
                mColorTextView.setText(getString(R.string.label_hex) + SPACE + Integer.toHexString(color));
            }
        } catch (IllegalArgumentException e) {
            Log.e(LOG_TAG, e.getMessage());
        }

        return color;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isFinishing() && mMediaPlayer != null) {
            mVisualizer.release();
            mEqualizer.release();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }
}
