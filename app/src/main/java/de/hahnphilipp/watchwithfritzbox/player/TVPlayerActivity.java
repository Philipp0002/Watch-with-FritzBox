package de.hahnphilipp.watchwithfritzbox.player;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.interfaces.IVLCVout;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import de.hahnphilipp.watchwithfritzbox.R;
import de.hahnphilipp.watchwithfritzbox.epg.EPGFragment;
import de.hahnphilipp.watchwithfritzbox.utils.ChannelUtils;
import de.hahnphilipp.watchwithfritzbox.utils.EpgUtils;
import de.hahnphilipp.watchwithfritzbox.utils.KeyDownReceiver;

public class TVPlayerActivity extends FragmentActivity implements MediaPlayer.EventListener {

    public IVLCVout ivlcVout;
    public SurfaceView surfaceView;
    public SurfaceView subtitlesView;
    private LibVLC mLibVLC = null;
    public MediaPlayer mMediaPlayer = null;
    public Media media;

    public HbbTVOverlay mHbbTvOverlay;
    public ChannelListTVOverlay mChannelOverlayFragment;
    public SettingsTVOverlay mSettingsOverlayFragment;
    public EPGFragment mEPGOverlayFragment;
    public TeletextTVOverlay mTeletextOverlayFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tvplayer);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        surfaceView = findViewById(R.id.video_layout);
        subtitlesView = findViewById(R.id.subtitles_layout);

        loadLibVLC();

        initializeOverlay();

        initGlide();
    }

    public void unloadLibVLC() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        if (mLibVLC != null) {
            mLibVLC.release();
            mLibVLC = null;
        }
        if (ivlcVout != null) {
            ivlcVout.detachViews();
            ivlcVout = null;
        }
    }

    public void initGlide() {
        AsyncTask.execute(() -> {
            ChannelUtils.Channel channel = ChannelUtils.getChannelByNumber(this, 1);
            if(channel != null) {
                Glide.with(this)
                        .load(ChannelUtils.getIconURL(channel))
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .preload();
            } else {
                Glide.get(this);
            }
        });
    }

    public void loadLibVLC() {
        SharedPreferences sp = getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        final ArrayList<String> args = new ArrayList<>();
        args.add("-vvvvv");

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

        if (sp.contains("setting_deinterlace")) {
            args.add("--video-filter=deinterlace");
            args.add("--deinterlace-mode=" + sp.getString("setting_deinterlace", "x"));
            args.add("--vout-filter=" + sp.getString("setting_deinterlace", "x"));
            args.add("--deinterlace-mode");
            args.add(sp.getString("setting_deinterlace", "x"));
        }

        mLibVLC = new LibVLC(this, args);
        mMediaPlayer = new MediaPlayer(mLibVLC);
        mMediaPlayer.setEventListener(this);
        mMediaPlayer.setAudioDelay(sp.getLong("setting_audio_delay", 0) * 1000);
        ivlcVout = mMediaPlayer.getVLCVout();
        ivlcVout.setVideoView(surfaceView);
        ivlcVout.setSubtitlesView(subtitlesView);
        ivlcVout.attachViews();

        final ViewTreeObserver observer = surfaceView.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(() -> {
            // Set rendering size
            ivlcVout.setWindowSize(surfaceView.getWidth(), surfaceView.getHeight());
        });
    }

    private void initializeOverlay() {
        mHbbTvOverlay = new HbbTVOverlay();
        mHbbTvOverlay.context = this;
        mHbbTvOverlay.setArguments(getIntent().getExtras());
        getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .add(R.id.hbbtv_container, mHbbTvOverlay)
                .commit();

        mChannelOverlayFragment = new ChannelListTVOverlay();
        mChannelOverlayFragment.context = this;
        mChannelOverlayFragment.setArguments(getIntent().getExtras());

        mSettingsOverlayFragment = new SettingsTVOverlay();
        mSettingsOverlayFragment.context = this;
        mSettingsOverlayFragment.setArguments(getIntent().getExtras());

        mEPGOverlayFragment = new EPGFragment();
        mEPGOverlayFragment.setArguments(getIntent().getExtras());

        mTeletextOverlayFragment = new TeletextTVOverlay();
        mTeletextOverlayFragment.context = this;
        mTeletextOverlayFragment.setArguments(getIntent().getExtras());
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

    public void popAllOverlayFragments() {
        while (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStackImmediate();
        }
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
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        return mHbbTvOverlay.onKeyDownLong(keyCode, event);
        //return super.onKeyLongPress(keyCode, event);
    }

    long keyPressDownTime;
    Integer keyLastPressed;
    boolean keyLongpressInvoked = false;


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // CUSTOM LONG PRESS LOGIC
        if (keyLastPressed != null && keyLastPressed == keyCode) {
            if (System.currentTimeMillis() - keyPressDownTime > 1000) {
                if (!keyLongpressInvoked) {
                    keyLongpressInvoked = onKeyDownLong(keyCode, event);
                    return keyLongpressInvoked || super.onKeyDown(keyCode, event);
                }
                return true;
            }
        } else {
            keyPressDownTime = System.currentTimeMillis();
            keyLastPressed = keyCode;
        }
        return super.onKeyDown(keyCode, event);
    }

    public boolean onKeyDownLong(int keyCode, KeyEvent event) {
        Fragment overlayFragment = getSupportFragmentManager().findFragmentById(R.id.overlayMenu);
        if (overlayFragment instanceof KeyDownReceiver) {
            return ((KeyDownReceiver) overlayFragment).onKeyDownLong(keyCode, event);
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            return mHbbTvOverlay.onKeyDownLong(keyCode, event);
        }
        return false;
    }


    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        keyLastPressed = null;
        keyLongpressInvoked = false;
        if (event.isCanceled()) {
            return true;
        }
        if (mHbbTvOverlay.onKeyUp(keyCode, event)) {
            return true/* || super.onKeyUp(keyCode, event)*/;
        }
        Fragment overlayFragment = getSupportFragmentManager().findFragmentById(R.id.overlayMenu);
        if (overlayFragment instanceof KeyDownReceiver) {
            return ((KeyDownReceiver) overlayFragment).onKeyUp(keyCode, event) || super.onKeyUp(keyCode, event);
        }

        if (overlayFragment == null && event.getAction() == KeyEvent.ACTION_UP) {
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
        return super.onKeyUp(keyCode, event);
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

    public void pausePlayer() {
        mMediaPlayer.pause();
    }

    Timer switchChannelTimer = null;

    public void launchPlayer(boolean withWaitInterval) {
        launchPlayer(withWaitInterval, true, true);
    }

    public void launchPlayer(boolean withWaitInterval, boolean withLoadingScreen, boolean clearHbbTv) {
        mMediaPlayer.pause();
        if(withLoadingScreen) {
            findViewById(R.id.player_skip_overlay).setVisibility(View.VISIBLE);
        }
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
                    ((LinearProgressIndicator) findViewById(R.id.player_skip_timer)).setProgress(0);
                    findViewById(R.id.player_skip_timer).setVisibility(View.VISIBLE);
                    if(clearHbbTv) mHbbTvOverlay.clearHbbTv();
                });
                SharedPreferences sp = TVPlayerActivity.this.getSharedPreferences(
                        getString(R.string.preference_file_key), Context.MODE_PRIVATE);
                int hwAccel = sp.getInt("setting_hwaccel", 1);
                Log.d("PlaybackActivity", "Starting playback of " + channel.title + " -" + channel.url);
                if (media != null && !media.isReleased()) {
                    media.release();
                }
                media = new Media(mLibVLC, Uri.parse(channel.url));
                mMediaPlayer.setMedia(media);
                media.setHWDecoderEnabled(hwAccel != 0, hwAccel == 2);

                media.addOption("--deinterlace=1");
                media.addOption("--video-filter=deinterlace");
                media.addOption("--deinterlace-mode=blend");


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

                media.addOption(":vbi-page=" + entered);
                mMediaPlayer.setMedia(media);
                /*if (selection != null)
                    ChannelUtils.updateLastSelectedChannel(TVPlayerActivity.this, selection.number);*/
                runOnUiThread(() -> {
                    /*if (selection != null)
                        launchPlayer(true);*/
                    Toast.makeText(TVPlayerActivity.this, media.isReleased() + "", Toast.LENGTH_SHORT).show();
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
        if (media != null) {
            media.release();
            media = null;
        }
        mMediaPlayer = null;
        mLibVLC = null;
    }

    public void stopHbbTV() {
        mHbbTvOverlay.clearHbbTv();
    }


    @Override
    public void onEvent(MediaPlayer.Event event) {
        switch (event.type) {
            case MediaPlayer.Event.NitReceived:
                Log.d("NIT", event.getRecordPath());
                break;
            case MediaPlayer.Event.CommonDescriptorsFound:
                // HbbTV = 0x0010
                MediaPlayer.CommonDescriptors commonDescriptors = event.getCommonDescriptors();
                if (commonDescriptors.getApplicationId().equals("0x0010")) {
                    mHbbTvOverlay.processHbbTvInfo(commonDescriptors);
                    return;
                }
                break;
            case MediaPlayer.Event.TeletextPageLoaded:
                if(true)return;
                AsyncTask.execute(() -> {
                            Log.d("TELETEXT", event.getRecordPath());
                            MediaPlayer.Teletext txt = event.getTeletextText();
                            if (txt != null && txt.getPageNumber() == 100) {
                                mTeletextOverlayFragment.experimentalSetTeletext(txt);
                            }
                        });
                //Log.d("TEST", event.getTeletextText().getPageNumber() + "");
                /*for(MediaPlayer.TeletextCell[] rows : event.getTeletextText().getCells()) {
                    StringBuilder builder = new StringBuilder();
                    for(MediaPlayer.TeletextCell cell : rows) {
                        builder.append(cell.getCharacter());
                    }
                    Log.d("TELETEXT", builder.toString());
                }*/
                break;
            case MediaPlayer.Event.EpgNewEvent:
                AsyncTask.execute(() -> {
                    MediaPlayer.EpgEvent vlcEvent = event.getEvent();
                    ChannelUtils.Channel ch = ChannelUtils.getChannelByServiceId(TVPlayerActivity.this, Integer.parseInt(vlcEvent.getServiceId()));

                    if (ch != null) {
                        EpgUtils.EpgEvent epgEvent = new EpgUtils.EpgEvent();
                        epgEvent.channelNumber = ch.number;
                        epgEvent.id = vlcEvent.getEventId();
                        epgEvent.description = vlcEvent.getDescription();
                        epgEvent.subtitle = vlcEvent.getShortDescription();
                        epgEvent.title = vlcEvent.getName();
                        epgEvent.duration = vlcEvent.getDuration();
                        epgEvent.startTime = vlcEvent.getStart();
                        epgEvent.eitReceivedTimeMillis = System.currentTimeMillis();
                        //TODO epgEvent.lang

                        EpgUtils.addEvent(TVPlayerActivity.this, epgEvent);
                    }
                });


                break;
            case MediaPlayer.Event.EpgNewServiceInfo:
                AsyncTask.execute(() -> {
                    MediaPlayer.ServiceInfo serviceInfo = event.getServiceInfo();
                    ChannelUtils.Channel originalChannel = ChannelUtils.getChannelByTitle(TVPlayerActivity.this, serviceInfo.getName());
                    if (originalChannel != null) {
                        ChannelUtils.Channel channel = originalChannel.copy();
                        channel.serviceId = Math.toIntExact(serviceInfo.getServiceId());
                        channel.provider = serviceInfo.getProvider();
                        channel.free = serviceInfo.isFreeCa();
                        try {
                            switch (Math.toIntExact(serviceInfo.getTypeId())) {
                                case 1:
                                    channel.type = ChannelUtils.ChannelType.SD;
                                    break;
                                case 25:
                                    channel.type = ChannelUtils.ChannelType.HD;
                                    break;
                                case 2:
                                    channel.type = ChannelUtils.ChannelType.RADIO;
                                    break;
                            }
                        } catch (Exception unused) {
                        }
                        ChannelUtils.updateChannel(TVPlayerActivity.this, originalChannel, channel);
                    }
                });

                break;
            case MediaPlayer.Event.Buffering:
                runOnUiThread(() -> {
                    ((LinearProgressIndicator) findViewById(R.id.player_skip_timer)).setProgress((int) event.getBuffering());

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
