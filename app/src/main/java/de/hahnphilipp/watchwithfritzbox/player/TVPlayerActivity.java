package de.hahnphilipp.watchwithfritzbox.player;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

import androidx.annotation.OptIn;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;
import androidx.media3.extractor.DefaultExtractorsFactory;
import androidx.media3.extractor.ts.TsExtractor;
import androidx.media3.ui.PlayerView;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import de.hahnphilipp.watchwithfritzbox.R;
import de.hahnphilipp.watchwithfritzbox.epg.LogcatEpgReader;
import de.hahnphilipp.watchwithfritzbox.utils.ChannelUtils;
import de.hahnphilipp.watchwithfritzbox.utils.KeyDownReceiver;

import com.arthenica.mobileffmpeg.FFmpeg;


public class TVPlayerActivity extends FragmentActivity {

    public PlayerView surfaceView;

    public ChannelListTVOverlay mChannelOverlayFragment;
    public SettingsTVOverlay mSettingsOverlayFragment;

    private LogcatEpgReader logcatEpgReader;


    @OptIn(markerClass = UnstableApi.class) @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tvplayer);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        surfaceView = findViewById(R.id.video_layout);


        FFmpeg.execute("-encoders");
        Log.d("AAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAA");
        int port = 5050;

        File file = new File(getFilesDir(), "stream.m3u8");
        if(file.exists()) file.delete();

        // SD rtsp://192.168.178.1:554/?avm=1&freq=466&bw=8&msys=dvbc&mtype=256qam&sr=6900&specinv=1&pids=0,16,17,18,20,7830,7832,7833,7834,7838,7839
        // HD rtsp://192.168.178.1:554/?avm=1&freq=450&bw=8&msys=dvbc&mtype=256qam&sr=6900&specinv=1&pids=0,16,17,18,20,6100,6110,6120,6121,6123,6130,6131,6170,6171,6172
        String cmd = "-y -i rtsp://192.168.178.1:554/?avm=1&freq=450&bw=8&msys=dvbc&mtype=256qam&sr=6900&specinv=1&pids=0,16,17,18,20,6100,6110,6120,6121,6123,6130,6131,6170,6171,6172 " +
                "-map 0:v:0 -map 0:a:0 " +
                "-c copy " +
                "-f hls " +
                "-hls_time 2 " +
                "-hls_list_size 5 " +
                "-hls_flags delete_segments " +
                "" + file.getAbsolutePath();


        //String cmd = "-y -i rtsp://192.168.178.1:554/?avm=1&freq=450&bw=8&msys=dvbc&mtype=256qam&sr=6900&specinv=1&pids=0,16,17,18,20,6100,6110,6120,6121,6123,6130,6131,6170,6171,6172 -c copy -f mpegts tcp://127.0.0.1:"+port;
        FFmpeg.executeAsync(cmd, (l, i) -> {

        });

        new Thread(new Runnable() {
            @Override
            public void run() {

                /*try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                InputStream inputStream;
                try {
                    ServerSocket serverSocket = new ServerSocket(port);
                    Socket client = serverSocket.accept();

                    Log.i("FFMPEG", "FFmpeg connected");

                    inputStream = client.getInputStream();
                }catch (Exception e) {
                    e.printStackTrace();
                    return;
                }*/


                new Handler(Looper.getMainLooper()).post(() -> {

                    try {
                        ExoPlayer player = new ExoPlayer.Builder(TVPlayerActivity.this).build();

                        surfaceView.setPlayer(player);

                        /*DataSource.Factory factory = () -> new InputStreamDataSource(inputStream);
                        MediaItem item = MediaItem.fromUri(Uri.parse("tcp://127.0.0.1:" + port));
                        ProgressiveMediaSource src = new ProgressiveMediaSource.Factory(factory).createMediaSource(item);*/

                        Uri uri = Uri.parse("file:/" + file.getAbsolutePath());
                        Log.d("URIII", uri.toString());

                        MediaItem item = MediaItem.fromUri(Uri.fromFile(file));
                        /*DataSource.Factory dataSourceFactory = new DefaultHttpDataSource.Factory();
                        MediaSource src = new ProgressiveMediaSource.Factory(dataSourceFactory)
                                .createMediaSource(item);*/


                        player.setMediaItem(item);
                        player.prepare();
                        player.play();
                    } catch (Exception e) {
                        Log.e("FFMPEG", "Fehler im Player: " + e.getMessage(), e);
                    }
                });
            }
        }).start();
        /*ExoPlayer player = new ExoPlayer.Builder(this).build();
        player.setMediaSource(mediaSource);
        player.prepare();
        player.play();*/


        //initializeOverlay();
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
        //launchPlayer(false);
    }

    Timer switchChannelTimer = null;

    public void launchPlayer(boolean withWaitInterval) {
        if (logcatEpgReader != null) {
            logcatEpgReader.stopLogcatRead();
        }
        logcatEpgReader = new LogcatEpgReader(this);
        logcatEpgReader.readLogcat();
        //mMediaPlayer.pause();
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
                //final Media media = new Media(mLibVLC, Uri.parse(channel.url));
                //mMediaPlayer.setMedia(media);
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
                //media.addOption(":vbi-page=150");
                //media.addOption(":vbi-opaque");
                //media.setHWDecoderEnabled(hwAccel != 0, hwAccel == 2);


                //media.release();
                //mMediaPlayer.play();

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

        //mMediaPlayer.stop();
        //ivlcVout.detachViews();
        //mLibVLC.release();
        //mMediaPlayer.release();
        //mMediaPlayer = null;
        //mLibVLC = null;
    }

    /*@Override
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
    }*/
}
