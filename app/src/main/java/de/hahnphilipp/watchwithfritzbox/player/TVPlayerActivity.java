package de.hahnphilipp.watchwithfritzbox.player;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;


import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.util.VLCVideoLayout;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import de.hahnphilipp.watchwithfritzbox.epg.LogcatEpgReader;
import de.hahnphilipp.watchwithfritzbox.utils.ChannelUtils;
import de.hahnphilipp.watchwithfritzbox.R;

public class TVPlayerActivity extends FragmentActivity {

    public VLCVideoLayout mVideoLayout = null;
    private LibVLC mLibVLC = null;
    public MediaPlayer mMediaPlayer = null;

    public ChannelListTVOverlay mChannelOverlayFragment;
    public SettingsTVOverlay mSettingsOverlayFragment;

    private LogcatEpgReader logcatEpgReader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tvplayer);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        final ArrayList<String> args = new ArrayList<>();
        args.add("-vvv");

        /*args.add("--no-ts-trust-pcr");
        args.add("--ts-seek-percent");*/

        //args.add("--ts-cc-check");
        //args.add("0");

        //args.add("--network-caching=" + 1000);
        //args.add("--no-audio-time-stretch");
        /*args.add("--avcodec-skiploopfilter");
        args.add("1");
        args.add("--avcodec-skip-frame");
        args.add("0");
        args.add("--avcodec-skip-idct");
        args.add("0");
        args.add("--avcodec-corrupted");
        args.add("1");
        args.add("--avcodec-hurry-up");
        args.add("1");
        args.add("--audio-resampler");
        args.add("soxr");*/
        //args.add("--stats");
        //args.add("--http-reconnect");

        //args.add("--telx-override-page");
        //args.add("101");

        args.add("--audio-resampler");
        args.add("soxr");
        args.add("--http-reconnect");
        args.add("--sout-keep");
        args.add("--no-audio-time-stretch");
        args.add("--avcodec-skiploopfilter");
        args.add("1");
        args.add("--network-caching=1500");
        args.add("--live-caching=1500");
        args.add("--sout-mux-caching=1500");
        args.add("-vvvvv");
        args.add("--freetype-rel-fontsize=16");
        args.add("--freetype-color=16777215");
        args.add("--freetype-background-opacity=128");
        args.add("--avcodec-hurry-up");
        args.add("1");
        args.add("--avcodec-corrupted");
        args.add("1");


        mLibVLC = new LibVLC(this, args);
        mMediaPlayer = new MediaPlayer(mLibVLC);

        mVideoLayout = findViewById(R.id.video_layout);

        initializeOverlay();
    }

    private void initializeOverlay() {
        if (mChannelOverlayFragment != null)
            return;

        mChannelOverlayFragment = (ChannelListTVOverlay) getSupportFragmentManager().findFragmentByTag("channels_video_overlay_fragment");
        if (mChannelOverlayFragment != null)
            return;

        mChannelOverlayFragment = new ChannelListTVOverlay();
        mChannelOverlayFragment.context = this;
        mChannelOverlayFragment.setArguments(getIntent().getExtras());
        FragmentTransaction ftChannel = getSupportFragmentManager().beginTransaction();
        ftChannel.replace(R.id.overlayChannels, mChannelOverlayFragment, "channels_video_overlay_fragment");
        ftChannel.commit();

        if (mSettingsOverlayFragment != null)
            return;

        mSettingsOverlayFragment = (SettingsTVOverlay) getSupportFragmentManager().findFragmentByTag("settings_video_overlay_fragment");
        if (mSettingsOverlayFragment != null)
            return;

        mSettingsOverlayFragment = new SettingsTVOverlay();
        mSettingsOverlayFragment.context = this;
        mSettingsOverlayFragment.setArguments(getIntent().getExtras());
        FragmentTransaction ftSettings = getSupportFragmentManager().beginTransaction();
        ftSettings.replace(R.id.overlaySettings, mSettingsOverlayFragment, "settings_video_overlay_fragment");
        ftSettings.commit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMediaPlayer.release();
        mLibVLC.release();
    }


    @Override
    protected void onResume() {
        Log.d("TVPlayerActivity", "onResume");
        super.onResume();
        //mOverlayFragment.showOverlays();
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return mChannelOverlayFragment.onKeyDown(keyCode, event) || mSettingsOverlayFragment.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onPause() {
        Log.d("TVPlayerActivity", "onPause");
        mChannelOverlayFragment.hideOverlays();
        mSettingsOverlayFragment.hideOverlays();
        super.onPause();
        Log.d("TVPlayerActivity", "onPauseEnd");
    }


    @Override
    protected void onStart() {
        super.onStart();
        mMediaPlayer.attachViews(mVideoLayout, null, false, false);
        launchPlayer(false);
    }

    Timer switchChannelTimer = null;

    public void launchPlayer(boolean withWaitInterval) {
        if(logcatEpgReader != null){
            logcatEpgReader.stopLogcatRead();
        }
        logcatEpgReader = new LogcatEpgReader(this);
        logcatEpgReader.readLogcat();
        mMediaPlayer.pause();
        findViewById(R.id.player_skip_overlay).setVisibility(View.VISIBLE);
        findViewById(R.id.player_skip_radio).setVisibility(View.GONE);
        int lastChannelNumber = ChannelUtils.getLastSelectedChannel(TVPlayerActivity.this);
        ChannelUtils.Channel channel = ChannelUtils.getChannelByNumber(TVPlayerActivity.this, lastChannelNumber);
        ((TextView) findViewById(R.id.player_number)).setText("CH " + channel.number);
        ((TextView) findViewById(R.id.player_channel)).setText(channel.title);

        if (!ChannelUtils.getHbbTvFromChannel(TVPlayerActivity.this, ChannelUtils.getLastSelectedChannel(TVPlayerActivity.this)).isEmpty()) {
            findViewById(R.id.hbbTvInfoCard).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.hbbTvInfoCard).setVisibility(View.GONE);
        }

        if (channel.type == ChannelUtils.ChannelType.HD) {
            ((ImageView) findViewById(R.id.player_type)).setImageResource(R.drawable.high_definition);
        } else if (channel.type == ChannelUtils.ChannelType.SD) {
            ((ImageView) findViewById(R.id.player_type)).setImageResource(R.drawable.standard_definition);
        } else if (channel.type == ChannelUtils.ChannelType.RADIO) {
            ((ImageView) findViewById(R.id.player_type)).setImageResource(R.drawable.radio_tower);
        }

        int timeWait = withWaitInterval ? 500 : 0;
        if (switchChannelTimer != null) {
            switchChannelTimer.cancel();
            switchChannelTimer.purge();
        }
        switchChannelTimer = new Timer();
        switchChannelTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> {
                    ((ProgressBar) findViewById(R.id.player_skip_timer)).setProgress(0);
                    findViewById(R.id.player_skip_timer).setVisibility(View.VISIBLE);
                });
                SharedPreferences sp = TVPlayerActivity.this.getSharedPreferences(
                        getString(R.string.preference_file_key), Context.MODE_PRIVATE);
                int hwAccel = sp.getInt("setting_hwaccel", 1);
                Log.d("URIIIL", channel.url);
                final Media media = new Media(mLibVLC, Uri.parse(channel.url));
                media.setHWDecoderEnabled(hwAccel != 0, hwAccel == 2);
                mMediaPlayer.setMedia(media);
                media.release();
                mMediaPlayer.play();

                mMediaPlayer.setEventListener(event -> {
                    switch (event.type) {
                        case MediaPlayer.Event.Buffering:
                            runOnUiThread(() -> {
                                ((ProgressBar) findViewById(R.id.player_skip_timer)).setProgress((int) event.getBuffering());

                                if (event.getBuffering() == 100F) {
                                    mSettingsOverlayFragment.updateTVSettings();
                                    findViewById(R.id.player_skip_timer).setVisibility(View.INVISIBLE);
                                    if (channel.type == ChannelUtils.ChannelType.RADIO) {
                                        findViewById(R.id.player_skip_radio).setVisibility(View.VISIBLE);
                                    } else {
                                        findViewById(R.id.player_skip_overlay).setVisibility(View.GONE);
                                    }
                                }
                            });
                            if (event.getBuffering() == 100F) {
                                mMediaPlayer.setEventListener(null);
                            }
                            break;
                        default:
                            break;
                    }
                });

            }
        }, timeWait);


    }

    String number = "0000";
    Timer numberEnterTimer = null;

    public void enterNumber(int entered) {
        Log.d("ChannelNumber", number + "");
        number = number + entered;
        number = number.substring(1, number.length());


        ((TextView) findViewById(R.id.player_enter_number_text)).setText(number);
        findViewById(R.id.player_enter_number_overlay).setVisibility(View.VISIBLE);

        final ChannelUtils.Channel selection = ChannelUtils.getChannelByNumber(TVPlayerActivity.this, Integer.parseInt(number));

        if (selection == null) {
            ((TextView) findViewById(R.id.player_enter_number_channel)).setText("");
            findViewById(R.id.player_enter_number_channel).setVisibility(View.GONE);
        } else {
            ((TextView) findViewById(R.id.player_enter_number_channel)).setText(selection.title);
            findViewById(R.id.player_enter_number_channel).setVisibility(View.VISIBLE);
        }

        if (numberEnterTimer != null) {
            numberEnterTimer.cancel();
            numberEnterTimer.purge();
        }
        numberEnterTimer = new Timer();

        numberEnterTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (selection != null)
                    ChannelUtils.updateLastSelectedChannel(TVPlayerActivity.this, selection.number);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (selection != null)
                            launchPlayer(true);
                        findViewById(R.id.player_enter_number_overlay).setVisibility(View.GONE);
                    }
                });


                number = "0000";

            }
        }, 1500);

    }

    @Override
    protected void onStop() {
        super.onStop();

        mMediaPlayer.stop();
        mMediaPlayer.detachViews();
    }

}
