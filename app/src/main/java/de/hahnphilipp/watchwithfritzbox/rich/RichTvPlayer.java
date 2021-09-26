package de.hahnphilipp.watchwithfritzbox.rich;

import android.content.Context;
import android.content.res.Resources;
import android.media.PlaybackParams;
import android.net.Uri;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;

import com.google.android.media.tv.companionlibrary.TvPlayer;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.interfaces.IVLCVout;

import java.util.ArrayList;

/**
 * A wrapper around ExoPlayer which implements TvPlayer. This is the class that actually renders
 * the video, subtitles and all these sorts of things.
 */
public class RichTvPlayer implements TvPlayer {
    private LibVLC libVlc;
    public MediaPlayer player;

    /**
     * AppPlayer constructor
     * @param context Context
     */
    public RichTvPlayer(Context context) {
        ArrayList<String> options = new ArrayList<>();
        options.add("-vv");
        options.add("--aout=opensles");

        libVlc = new LibVLC(context, options);
        player = new MediaPlayer(libVlc);
    }

    /**
     * Load media
     * @param mediaUri Media URI
     */
    public void loadMedia(String mediaUri) {
        loadMedia(Uri.parse(mediaUri));
    }

    /**
     * Load media
     * @param mediaUri Media URI
     */
    public void loadMedia(Uri mediaUri) {
        final Media media = new Media(libVlc, mediaUri);
        media.setHWDecoderEnabled(true, false);
        media.addOption(":clock-jitter=0");
        media.addOption(":clock-synchro=0");
        media.addOption(":network-caching=1000"); // In milliseconds
        media.addOption(":sout-keep");
        media.addOption(":audio-time-stretch");

        player.setMedia(media);

        media.release();
    }

    /**
     * Release player
     */
    public void release() {
        player.release();
        libVlc.release();
    }

    /**
     * Set surface
     * @param surface Video surface
     */
    @Override
    public void setSurface(Surface surface) {
        Log.d("setSurface", "aaa");
        final IVLCVout vlcVout = player.getVLCVout();
        if (surface != null) {
            DisplayMetrics dm = Resources.getSystem().getDisplayMetrics();
            vlcVout.setVideoSurface(surface,null);
            vlcVout.setWindowSize(dm.widthPixels, dm.heightPixels);
            vlcVout.attachViews(new IVLCVout.OnNewVideoLayoutListener() {
                @Override
                public void onNewVideoLayout(IVLCVout vlcVout, int width, int height, int visibleWidth, int visibleHeight, int sarNum, int sarDen) {

                }
            });
        } else {
            vlcVout.detachViews();
        }
        Log.d("setSurface", "bbb");
    }


    /**
     * Get current position
     * @return Current position in milliseconds
     */
    @Override
    public long getCurrentPosition() {
        return (long) (player.getPosition() * 1000);
    }


    /**
     * Get duration
     * @return Duration in milliseconds
     */
    @Override
    public long getDuration() {
        return player.getLength();
    }


    /**
     * Start or resume player
     */
    @Override
    public void play() {
        player.play();
    }


    /**
     * Pause player
     */
    @Override
    public void pause() {
        player.pause();
    }


    /**
     * Stop player
     */
    public void stop() {
        player.stop();
    }


    /**
     * Seek to
     * @param position Position in milliseconds
     */
    @Override
    public void seekTo(long position) {
        float pos = (float) position;
        pos /= 1000;
        player.setPosition(pos);
    }

    public void setEventListener(MediaPlayer.EventListener eventListener){
        player.setEventListener(eventListener);
    }


    /**
     * Set volume
     * @param volume Volume between 0 and 1
     */
    @Override
    public void setVolume(float volume) {
        player.setVolume((int) (volume * 100));
    }


    @Override
    public void setPlaybackParams(PlaybackParams params) {}

    @Override
    public void registerCallback(Callback callback) {}

    @Override
    public void unregisterCallback(Callback callback) {}
}
