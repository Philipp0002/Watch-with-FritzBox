package de.hahnphilipp.watchwithfritzbox.player;

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
import org.videolan.libvlc.interfaces.IMedia;
import org.videolan.libvlc.util.VLCVideoLayout;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import de.hahnphilipp.watchwithfritzbox.R;
import de.hahnphilipp.watchwithfritzbox.utils.ChannelUtils;

public class TVPlayerActivity extends FragmentActivity {

    public VLCVideoLayout mVideoLayout = null;
    private LibVLC mLibVLC = null;
    public MediaPlayer mMediaPlayer = null;

    public ChannelListTVOverlay mChannelOverlayFragment;
    public SettingsTVOverlay mSettingsOverlayFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tvplayer);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        final ArrayList<String> args = new ArrayList<>();
        args.add("-vvv");//--rtsp-user=user --rtsp-pwd=26112002
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
        launchPlayer();
    }

    Timer switchChannelTimer = null;

    public void launchPlayer(){
        mMediaPlayer.pause();
        findViewById(R.id.player_skip_overlay).setVisibility(View.VISIBLE);
        findViewById(R.id.player_skip_radio).setVisibility(View.GONE);
        int lastChannelNumber = ChannelUtils.getLastSelectedChannel(TVPlayerActivity.this);
        ChannelUtils.Channel channel = ChannelUtils.getChannelByNumber(TVPlayerActivity.this, lastChannelNumber);
        ((TextView) findViewById(R.id.player_number)).setText("CH "+channel.number);
        ((TextView) findViewById(R.id.player_channel)).setText(channel.title);

        if(!ChannelUtils.getHbbTvFromChannel(TVPlayerActivity.this,ChannelUtils.getLastSelectedChannel(TVPlayerActivity.this)).isEmpty()) {
            findViewById(R.id.hbbTvInfoCard).setVisibility(View.VISIBLE);
        }else{
            findViewById(R.id.hbbTvInfoCard).setVisibility(View.GONE);
        }

        if(channel.type == ChannelUtils.ChannelType.HD){
            ((ImageView) findViewById(R.id.player_type)).setImageResource(R.drawable.ic_high_definition);
        }else if(channel.type == ChannelUtils.ChannelType.SD){
            ((ImageView) findViewById(R.id.player_type)).setImageResource(R.drawable.ic_standard_definition);
        }else if(channel.type == ChannelUtils.ChannelType.RADIO){
            ((ImageView) findViewById(R.id.player_type)).setImageResource(R.drawable.ic_radio_tower);
        }

        if(switchChannelTimer != null) {
                switchChannelTimer.cancel();
                switchChannelTimer.purge();
        }
        switchChannelTimer = new Timer();
            switchChannelTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ((ProgressBar)findViewById(R.id.player_skip_timer)).setProgress(0);
                            findViewById(R.id.player_skip_timer).setVisibility(View.VISIBLE);
                        }
                    });

                    final Media media = new Media(mLibVLC, Uri.parse(channel.url));
                    media.setHWDecoderEnabled(true, false);
                    mMediaPlayer.setMedia(media);
                    media.release();
                    mMediaPlayer.play();
                    mMediaPlayer.setEventListener(new MediaPlayer.EventListener() {
                        @Override
                        public void onEvent(MediaPlayer.Event event) {
                            switch (event.type) {
                                case MediaPlayer.Event.Buffering:
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            ((ProgressBar)findViewById(R.id.player_skip_timer)).setProgress((int)event.getBuffering());

                                            if(event.getBuffering() == 100F) {
                                                mSettingsOverlayFragment.updateTVSettings();
                                                findViewById(R.id.player_skip_timer).setVisibility(View.INVISIBLE);
                                                if(channel.type == ChannelUtils.ChannelType.RADIO){
                                                    findViewById(R.id.player_skip_radio).setVisibility(View.VISIBLE);
                                                }else{
                                                    findViewById(R.id.player_skip_overlay).setVisibility(View.GONE);
                                                }
                                            }
                                        }
                                    });
                                    if(event.getBuffering() == 100F) {
                                        mMediaPlayer.setEventListener(null);
                                    }
                                    break;
                                default:
                                    break;
                            }
                        }
                    });

                }
            }, 500);


    }

    String number = "0000";
    Timer numberEnterTimer = null;
    public void enterNumber(int entered){
        Log.d("ChannelNumber", number+"");
        number = number+entered;
        number = number.substring(1, number.length());


        ((TextView)findViewById(R.id.player_enter_number_text)).setText(number);
        findViewById(R.id.player_enter_number_overlay).setVisibility(View.VISIBLE);

        final ChannelUtils.Channel selection = ChannelUtils.getChannelByNumber(TVPlayerActivity.this, Integer.parseInt(number));

        if(selection == null){
            ((TextView)findViewById(R.id.player_enter_number_channel)).setText("");
            findViewById(R.id.player_enter_number_channel).setVisibility(View.GONE);
        }else{
            ((TextView)findViewById(R.id.player_enter_number_channel)).setText(selection.title);
            findViewById(R.id.player_enter_number_channel).setVisibility(View.VISIBLE);
        }

        if(numberEnterTimer != null){
            numberEnterTimer.cancel();
            numberEnterTimer.purge();
        }
        numberEnterTimer = new Timer();

        numberEnterTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if(selection != null)
                ChannelUtils.updateLastSelectedChannel(TVPlayerActivity.this, selection.number);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(selection != null)
                            launchPlayer();
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
