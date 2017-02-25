/***
 * Copyright (c) 2017 Oscar Salguero www.oscarsalguero.com
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

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import android.widget.Toast;

import com.oscarsalguero.musictocolor.view.VisualizerView;

import java.util.Random;

/**
 * Demonstrates how to get colors from music using the {@link Visualizer} class.
 * Created by RacZo on 9/2/15.
 */
public class MainActivity extends AppCompatActivity {

    private static final String LOG_TAG = MainActivity.class.getName();

    private MediaPlayer mMediaPlayer;
    private Equalizer mEqualizer;
    private Visualizer mVisualizer;
    private LinearLayout mWaveformLayout;
    private LinearLayout mEqualizerLayout;
    private VisualizerView mVisualizerView;
    private TextView mColorTextView;

    private static final String SPACE = " ";
    private static final float VISUALIZER_HEIGHT_DIP = 56;

    /**
     * Id to identify a mic permission request.
     */
    private static final int REQUEST_MIC = 0;

    /**
     * Id to identify a storage permission request.
     */
    private static final int REQUEST_STORAGE = 1;

    /**
     * Permissions required to read/write external storage.
     */
    private static String[] PERMISSIONS_STORAGE = {Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};

    /**
     * Permissions required to play audio.
     */
    private static String[] PERMISSIONS_AUDIO = {Manifest.permission.RECORD_AUDIO,
            Manifest.permission.MODIFY_AUDIO_SETTINGS};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mWaveformLayout = (LinearLayout) findViewById(R.id.layout_waveform);

        mEqualizerLayout = (LinearLayout) findViewById(R.id.layout_equalizer);

        mColorTextView = (TextView) findViewById(R.id.text_view_color_hex);
        mColorTextView.setBackgroundColor(Color.BLACK);
        mColorTextView.setText(getString(R.string.label_hex) + SPACE + "n/a");

        // Creating the MediaPlayer object
        mMediaPlayer = new MediaPlayer();

        setupUI();

        initializeMediaPlayer();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mMediaPlayer != null) {
            if (!mMediaPlayer.isPlaying()) {
                mMediaPlayer.start();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mMediaPlayer != null) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isFinishing() && mMediaPlayer != null) {
            mVisualizer.release();
            mEqualizer.release();
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.stop();
            }
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
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

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
            case REQUEST_MIC: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
        }
    }

    /**
     * Sets up the visualizer and creates the equalizer controls
     */
    private void setupUI() {

        // Create a VisualizerView (defined below), which will render the simplified audio wave form to a Canvas.
        mVisualizerView = new VisualizerView(this);
        mVisualizerView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (int) (VISUALIZER_HEIGHT_DIP * getResources().getDisplayMetrics().density)));
        mWaveformLayout.addView(mVisualizerView);

        // Create the Visualizer object and attach it to our media player.
        mVisualizer = new Visualizer(mMediaPlayer.getAudioSessionId());
        mVisualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);
        mVisualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {

            public void onWaveFormDataCapture(Visualizer visualizer, byte[] bytes, int samplingRate) {
                int color = updateColor(mVisualizerView.getWaveFormHeight());
                // Avoiding white color so we don't change the visualizer and text view to white
                if (color != Color.WHITE) {
                    mVisualizerView.updateVisualizer(bytes, color);
                    updateTextViewColor(color);
                }
            }

            public void onFftDataCapture(Visualizer visualizer, byte[] bytes, int samplingRate) {
            }

        }, Visualizer.getMaxCaptureRate() / 2, true, false);

        // Create the Equalizer object (an AudioEffect subclass) and attach it to our media player,
        // with a default priority (0).
        mEqualizer = new Equalizer(0, mMediaPlayer.getAudioSessionId());
        mEqualizer.setEnabled(true);
        // Getting equalizer bands
        short bands = mEqualizer.getNumberOfBands();
        // Getting ranges
        final short minEQLevel = mEqualizer.getBandLevelRange()[0];
        final short maxEQLevel = mEqualizer.getBandLevelRange()[1];
        // Creating layouts with text views and seek bars for each band
        for (short i = 0; i < bands; i++) {

            final short band = i;

            TextView freqTextView = new TextView(this);
            freqTextView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            freqTextView.setGravity(Gravity.CENTER_HORIZONTAL);

            freqTextView.setText((mEqualizer.getCenterFreq(band) / 1000) + SPACE + getString(R.string.label_hz));
            mEqualizerLayout.addView(freqTextView);

            LinearLayout bandLayout = new LinearLayout(this);
            bandLayout.setOrientation(LinearLayout.HORIZONTAL);

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

            SeekBar seekBar = new SeekBar(this);
            seekBar.setLayoutParams(layoutParams);
            seekBar.setMax(maxEQLevel - minEQLevel);
            seekBar.setProgress(mEqualizer.getBandLevel(band));
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                public void onProgressChanged(SeekBar seekBar, int progress,
                                              boolean fromUser) {
                    mEqualizer.setBandLevel(band, (short) (progress + minEQLevel));
                }

                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                public void onStopTrackingTouch(SeekBar seekBar) {
                }

            });

            bandLayout.addView(minDbTextView);
            bandLayout.addView(seekBar);
            bandLayout.addView(maxDbTextView);

            mEqualizerLayout.addView(bandLayout);
        }

    }

    /**
     * Updates the color on the text view based on the waveform's height
     *
     * @param waveFormHeight the waveform's height given by the Visualizer class
     * @return an integer value with the color that was generated
     */
    private int updateColor(float waveFormHeight) {

        int min = 1;
        int max = 3;
        Random r = new Random();
        int multiplier = r.nextInt(max - min + 1) + min;

        Random rnd = new Random();
        int alpha = 255;
        int n = Math.round(waveFormHeight) * multiplier;
        if (n > 256) {
            n = 256;
        }
        // Using the sampling rate as the to generate a HEX string and then an integer value for color
        int color = Color.argb(alpha, rnd.nextInt(n), rnd.nextInt(n), rnd.nextInt(n));
        return color;
    }

    /**
     * Initializes the media player
     */
    private void initializeMediaPlayer() {
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        AssetFileDescriptor assetFileDescriptor = null;
        try {
            assetFileDescriptor = getAssets().openFd("dbz.mp3");

            mMediaPlayer.setDataSource(
                    assetFileDescriptor.getFileDescriptor(),
                    assetFileDescriptor.getStartOffset(),
                    assetFileDescriptor.getLength());
            mMediaPlayer.prepare();
            mMediaPlayer.start();

        } catch (Exception e) {
            Log.e(LOG_TAG, "An error has occurred while setting up MediaPlayer", e);
        }

        Log.d(LOG_TAG, "MediaPlayer audio session ID: " + mMediaPlayer.getAudioSessionId());

        // Make sure the visualizer is enabled only when you actually want to receive data, and
        // when it makes sense to receive data.
        mVisualizer.setEnabled(true);

        // When the stream ends, we don't need to collect any more data. We don't do this in
        // setupVisualizer because we likely want to have more, non-Visualizer related code
        // in this callback.
        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            public void onCompletion(MediaPlayer mediaPlayer) {
                mVisualizer.setEnabled(false);
            }
        });
        mMediaPlayer.start();
        Toast.makeText(this, getString(R.string.label_music_attribution), Toast.LENGTH_LONG).show();
    }

    /**
     * Updates the background color of the text view and its text with the HEX value for the color
     *
     * @param color an integer with the color we want to set as the text view's background
     */
    private void updateTextViewColor(int color) {
        try {
            mColorTextView.setBackgroundColor(color);
            mColorTextView.setText(getString(R.string.label_hex) + SPACE + Integer.toHexString(color));
        } catch (IllegalArgumentException e) {
            Log.e(LOG_TAG, "An error has occurred while updating the color", e);
        }
    }
}
