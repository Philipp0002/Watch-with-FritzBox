package de.hahnphilipp.watchwithfritzbox.player;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import de.hahnphilipp.watchwithfritzbox.R;
import de.hahnphilipp.watchwithfritzbox.epg.LogcatEpgReader;
import de.hahnphilipp.watchwithfritzbox.utils.ChannelUtils;
import de.hahnphilipp.watchwithfritzbox.utils.KeyDownReceiver;

public class TVPlayerActivity extends FragmentActivity implements MediaPlayer.EventListener {

    public IVLCVout ivlcVout;
    public SurfaceView surfaceView;
    public SurfaceView subtitlesView;
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
        args.add("-vvvvv");

        args.add("--audio-resampler");
        args.add("soxr");
        args.add("--http-reconnect");
        args.add("--sout-keep");
        args.add("--no-audio-time-stretch");
        args.add("--avcodec-skiploopfilter");
        args.add("1");
        args.add("--freetype-color=16777215");
        args.add("--freetype-background-opacity=128");
        args.add("--network-caching=1500");
        args.add("--live-caching=1500");
        args.add("--sout-mux-caching=1500");
        args.add("--avcodec-hurry-up");
        args.add("1");
        //args.add("--telx-hide");
        //args.add("--demux");
        //args.add("live555");
        //args.add("--vbi-text");

        surfaceView = findViewById(R.id.video_layout);
        subtitlesView = findViewById(R.id.subtitles_layout);

        mLibVLC = new LibVLC(this, args);
        mMediaPlayer = new MediaPlayer(mLibVLC);
        mMediaPlayer.setEventListener(this);
        ivlcVout = mMediaPlayer.getVLCVout();
        ivlcVout.setVideoView(surfaceView);
        ivlcVout.setSubtitlesView(subtitlesView);
        ivlcVout.attachViews();

        final ViewTreeObserver observer = surfaceView.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(() -> {
            // Set rendering size
            ivlcVout.setWindowSize(surfaceView.getWidth(), surfaceView.getHeight());
        });

        initializeOverlay();
    }

    private void initializeOverlay() {
        mChannelOverlayFragment = new ChannelListTVOverlay();
        mChannelOverlayFragment.context = this;
        mChannelOverlayFragment.setArguments(getIntent().getExtras());


        mSettingsOverlayFragment = new SettingsTVOverlay();
        mSettingsOverlayFragment.context = this;
        mSettingsOverlayFragment.setArguments(getIntent().getExtras());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    @Override
    protected void onResume() {
        super.onResume();
    }

    public void addOverlayFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.overlayMenu, fragment)
                .addToBackStack(null)
                .commit();
    }

    public void popOverlayFragment() {
        getSupportFragmentManager().popBackStack();
    }

    public void zapChannel(boolean toNext) {
        Animation a = AnimationUtils.loadAnimation(TVPlayerActivity.this, toNext ? R.anim.slide_up : R.anim.slide_down);
        a.setInterpolator(new AccelerateDecelerateInterpolator());
        a.setFillEnabled(false);
        a.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                ChannelUtils.Channel toChannel =
                        toNext ?
                                ChannelUtils.getNextChannel(TVPlayerActivity.this, ChannelUtils.getLastSelectedChannel(TVPlayerActivity.this)) :
                                ChannelUtils.getPreviousChannel(TVPlayerActivity.this, ChannelUtils.getLastSelectedChannel(TVPlayerActivity.this));
                ChannelUtils.updateLastSelectedChannel(TVPlayerActivity.this, toChannel.number);
                launchPlayer(true);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        findViewById(R.id.video_layout).startAnimation(a);
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Fragment overlayFragment = getSupportFragmentManager().findFragmentById(R.id.overlayMenu);
        if (overlayFragment instanceof KeyDownReceiver) {
            return ((KeyDownReceiver) overlayFragment).onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
        }

        if (overlayFragment == null && event.getAction() == KeyEvent.ACTION_DOWN) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_CHANNEL_UP) {
                zapChannel(true);
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN || keyCode == KeyEvent.KEYCODE_CHANNEL_DOWN) {
                zapChannel(false);
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                addOverlayFragment(mChannelOverlayFragment);
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                addOverlayFragment(mSettingsOverlayFragment);
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_0) {
                enterNumber(0);
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_1) {
                enterNumber(1);
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_2) {
                enterNumber(2);
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_3) {
                enterNumber(3);
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_4) {
                enterNumber(4);
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_5) {
                enterNumber(5);
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_6) {
                enterNumber(6);
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_7) {
                enterNumber(7);
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_8) {
                enterNumber(8);
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_9) {
                enterNumber(9);
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }


    @Override
    protected void onStart() {
        super.onStart();
        //mMediaPlayer.attachViews(mVideoLayout, null, false, false);
        launchPlayer(false);
    }

    Timer switchChannelTimer = null;

    public void launchPlayer(boolean withWaitInterval) {
        if (logcatEpgReader != null) {
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
                Log.d("PlaybackActivity", "Starting playback of " + channel.title + " -" + channel.url);
                final Media media = new Media(mLibVLC, Uri.parse(channel.url));
                mMediaPlayer.setMedia(media);
                /**
                 * --vbi-page=<integer [0 .. 7995392]>
                 *                                  Teletext page
                 *           Open the indicated Teletext page. Default page is index 100.
                 *       --vbi-opaque, --no-vbi-opaque
                 *                                  Opacity
                 *                                  (default disabled)
                 *           Setting to true makes the text to be boxed and maybe easier to read.
                 *       --vbi-position={0 (Center), 1 (Left), 2 (Right), 4 (Top), 8 (Bottom), 5 (Top-Left), 6 (Top-Right), 9 (Bottom-Left), 10 (Bottom-Right)}
                 *                                  Teletext alignment
                 *           You can enforce the teletext position on the video (0=center, 1=left,
                 *           2=right, 4=top, 8=bottom, you can also use combinations of these
                 *           values, eg. 6 = top-right).
                 *       --vbi-text, --no-vbi-text  Teletext text subtitles
                 *                                  (default disabled)
                 *           Output teletext subtitles as text instead of as RGBA.
                 *       --vbi-level={0 (1), 1 (1.5), 2 (2.5), 3 (3.5)}
                 *                                  Presentation Level
                 */
                media.addOption(":vbi-page=150");
                media.addOption(":vbi-opaque");
                media.setHWDecoderEnabled(hwAccel != 0, hwAccel == 2);


                media.release();
                mMediaPlayer.play();

            }
        }, timeWait);


    }

    String number = "0000";
    Timer numberEnterTimer = null;

    public void enterNumber(int entered) {
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
                runOnUiThread(() -> {
                    if (selection != null)
                        launchPlayer(true);
                    findViewById(R.id.player_enter_number_overlay).setVisibility(View.GONE);
                });


                number = "0000";

            }
        }, 1500);

    }

    @Override
    protected void onStop() {
        super.onStop();

        mMediaPlayer.stop();
        ivlcVout.detachViews();
        mLibVLC.release();
        mMediaPlayer.release();
        mMediaPlayer = null;
        mLibVLC = null;
    }

    @Override
    public void onEvent(MediaPlayer.Event event) {
        switch (event.type) {
            case MediaPlayer.Event.Buffering:
                runOnUiThread(() -> {
                    ((ProgressBar) findViewById(R.id.player_skip_timer)).setProgress((int) event.getBuffering());

                    if (event.getBuffering() == 100F) {
                        int lastChannelNumber = ChannelUtils.getLastSelectedChannel(TVPlayerActivity.this);
                        ChannelUtils.Channel channel = ChannelUtils.getChannelByNumber(TVPlayerActivity.this, lastChannelNumber);

                        mSettingsOverlayFragment.updateTVSettings();
                        findViewById(R.id.player_skip_timer).setVisibility(View.INVISIBLE);
                        if (channel.type == ChannelUtils.ChannelType.RADIO) {
                            findViewById(R.id.player_skip_radio).setVisibility(View.VISIBLE);
                        } else {
                            findViewById(R.id.player_skip_overlay).setVisibility(View.GONE);
                        }
                    }
                });
                break;
            default:
                break;
        }
    }
}
