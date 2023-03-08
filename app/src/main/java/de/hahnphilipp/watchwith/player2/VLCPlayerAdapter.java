package de.hahnphilipp.watchwith.player2;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.view.SurfaceHolder;

import androidx.leanback.media.PlaybackBaseControlGlue;
import androidx.leanback.media.PlaybackGlueHost;
import androidx.leanback.media.PlayerAdapter;
import androidx.leanback.media.SurfaceHolderGlueHost;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

import java.util.ArrayList;

public class VLCPlayerAdapter extends PlayerAdapter {

    public static final String TAG = "VLCPlayerAdapter";
    private LibVLC mLibVLC = null;
    public MediaPlayer mMediaPlayer = null;
    public Context context;

    String playbackUrl;

    boolean mHasDisplay = false;
    SurfaceHolderGlueHost mSurfaceHolderGlueHost;
    boolean mInitialized = false;
    long mBufferedProgress = 0;
    boolean mBufferingStart = false;
    long mDuration = -1L;
    long mTime = -1L;
    boolean mIsSeekable = false;

    public VLCPlayerAdapter(Context context) {
        this.context = context;

        final ArrayList<String> args = new ArrayList<>();
        args.add("-vvv");

        args.add("--network-caching=" + 1000);
        args.add("--audio-time-stretch");
        args.add("--avcodec-skiploopfilter");
        args.add("1");
        args.add("--avcodec-skip-frame");
        args.add("0");
        args.add("--avcodec-skip-idct");
        args.add("0");
        args.add("--android-display-chroma");
        args.add("RV32");
        args.add("--audio-resampler");
        args.add("soxr");
        args.add("--stats");
        args.add("--vout=android-opaque,android-display");
        args.add("--http-reconnect");


        mLibVLC = new LibVLC(context, args);
        mMediaPlayer = new MediaPlayer(mLibVLC);
    }

    @Override
    public void onAttachedToHost(PlaybackGlueHost host) {
        if (host instanceof SurfaceHolderGlueHost) {
            mSurfaceHolderGlueHost = ((SurfaceHolderGlueHost) host);
            mSurfaceHolderGlueHost.setSurfaceHolderCallback(new VideoPlayerSurfaceHolderCallback());
        }
    }


    public boolean setDataSource(String url) {
        if(playbackUrl == url && playbackUrl != null){
            return false;
        }
        if(url == null){
            return false;
        }
        playbackUrl = url;
        prepareMediaForPlaying();
        return true;
    }

    void prepareMediaForPlaying(){
        reset();

        final Media media = new Media(mLibVLC, Uri.parse(playbackUrl) );
        //media.setHWDecoderEnabled(hwAccel != 0, hwAccel == 2);
        mMediaPlayer.setMedia(media);

        mMediaPlayer.setEventListener(new MediaPlayer.EventListener() {
            @Override
            public void onEvent(MediaPlayer.Event event) {
                switch (event.type){
                    case MediaPlayer.Event.MediaChanged:
                        Log.d(TAG, "libvlc Event.MediaChanged");
                        break;
                    case MediaPlayer.Event.Opening:
                        Log.d(TAG, "libvlc Event.Opening");
                        if(mSurfaceHolderGlueHost == null || mHasDisplay){
                            getCallback().onPreparedStateChanged(VLCPlayerAdapter.this);
                        }
                        break;
                    case MediaPlayer.Event.Buffering:
                        Log.d(TAG, "libvlc Event.Buffering");
                        mBufferedProgress = (long)(getDuration() * event.getBuffering() / 100); //TODO ROUND TO LONG???
                        getCallback().onBufferedPositionChanged(VLCPlayerAdapter.this);
                        if(mTime < mBufferedProgress) {
                            mBufferingStart = false;
                            notifyBufferingStartEnd();
                        } else {
                            mBufferingStart = true;
                            notifyBufferingStartEnd();
                        }
                        break;
                    case MediaPlayer.Event.Playing:
                        Log.d(TAG, "libvlc Event.Playing");
                        getCallback().onPlayStateChanged(VLCPlayerAdapter.this);
                        break;
                    case MediaPlayer.Event.Paused:
                        Log.d(TAG, "libvlc Event.Paused");
                        getCallback().onPlayStateChanged(VLCPlayerAdapter.this);
                        break;
                    case MediaPlayer.Event.Stopped:
                        Log.d(TAG, "libvlc Event.Stopped");
                        break;
                    case MediaPlayer.Event.EndReached:
                        Log.d(TAG, "libvlc Event.EndReached");
                        getCallback().onPlayStateChanged(VLCPlayerAdapter.this);
                        getCallback().onPlayCompleted(VLCPlayerAdapter.this);
                        break;
                    case MediaPlayer.Event.EncounteredError:
                        Log.d(TAG, "libvlc Event.EncounteredError");
                        getCallback().onError(VLCPlayerAdapter.this, 0, "an error occured");
                        break;
                    case MediaPlayer.Event.TimeChanged:
                        Log.d(TAG, "libvlc Event.TimeChanged");
                        getCallback().onCurrentPositionChanged(VLCPlayerAdapter.this);
                        break;
                    case MediaPlayer.Event.SeekableChanged:
                        Log.d(TAG, "libvlc Event.SeekableChanged");
                        mIsSeekable = event.getSeekable();
                        getCallback().onDurationChanged(VLCPlayerAdapter.this);
                        break;
                    case MediaPlayer.Event.PausableChanged:
                        Log.d(TAG, "libvlc Event.PausableChanged");
                        break;
                    case MediaPlayer.Event.LengthChanged:
                        Log.d(TAG, "libvlc Event.LengthChanged:" + event.getLengthChanged());
                        mDuration = event.getLengthChanged();
                        getCallback().onDurationChanged(VLCPlayerAdapter.this);
                        break;
                    case MediaPlayer.Event.Vout:
                        Log.d(TAG, "libvlc Event.Vout");
                        break;
                    case MediaPlayer.Event.ESAdded:
                        Log.d(TAG, "libvlc Event.ESAdded");
                        break;
                    case MediaPlayer.Event.ESDeleted:
                        Log.d(TAG, "libvlc Event.ESDeleted");
                        break;
                    case MediaPlayer.Event.ESSelected:
                        Log.d(TAG, "libvlc Event.ESSelected");
                        break;
                    case MediaPlayer.Event.RecordChanged:
                        Log.d(TAG, "libvlc Event.RecordChanged");
                        break;
                    default:
                        Log.d(TAG, "libvlc Event.Unknown");
                        break;
                }
            }
        });
        notifyBufferingStartEnd();
    }

    @Override
    public long getSupportedActions() {
        return (PlaybackBaseControlGlue.ACTION_PLAY_PAUSE +
                PlaybackBaseControlGlue.ACTION_REWIND +
                PlaybackBaseControlGlue.ACTION_FAST_FORWARD);
    }

    @Override
    public long getBufferedPosition() {
        return mBufferedProgress;
    }

    @Override
    public void seekTo(long positionInMs) {
        if(!mInitialized){
            return;
        }
        mMediaPlayer.setTime(positionInMs);
    }

    @Override
    public boolean isPrepared() {
        return mInitialized && (mSurfaceHolderGlueHost == null || mHasDisplay);
    }

    @Override
    public void play() {
        if(mMediaPlayer.isPlaying()){
            return;
        }
        mMediaPlayer.play();
    }

    @Override
    public void pause() {
        if(isPlaying()) {
            mMediaPlayer.pause();
        }
    }

    @Override
    public boolean isPlaying() {
        return mMediaPlayer.isPlaying();
    }

    @Override
    public long getDuration() {
        return mDuration;
    }

    @Override
    public long getCurrentPosition() {
        return mTime;
    }

    void notifyBufferingStartEnd(){
        getCallback().onBufferingStateChanged(this, mBufferingStart || !mInitialized);
    }

    void setDisplay(SurfaceHolder surfaceHolder) {
        boolean hadDisplay = mHasDisplay;
        mHasDisplay = surfaceHolder != null;
        if (hadDisplay == mHasDisplay) {
            return;
        }

        if (surfaceHolder != null) {
            mMediaPlayer.getVLCVout().setVideoSurface(surfaceHolder.getSurface(), surfaceHolder);
            mMediaPlayer.getVLCVout().setWindowSize(surfaceHolder.getSurfaceFrame().width(), surfaceHolder.getSurfaceFrame().height());
            mMediaPlayer.getVLCVout().attachViews();
            mInitialized = true;
        } else {
            mInitialized = false;
            mMediaPlayer.getVLCVout().detachViews();
        }

        getCallback().onPreparedStateChanged(this);
    }

    void reset(){
        changeToUninitialized();
        mMediaPlayer.stop();
        if(mMediaPlayer.hasMedia()){
            mMediaPlayer.getMedia().release();
            mMediaPlayer.setMedia(null);
        }
    }

    void changeToUninitialized(){
        if(mMediaPlayer.hasMedia()){
            mMediaPlayer.getMedia().release();
            mMediaPlayer.setMedia(null);
        }
        if(mInitialized){
            mInitialized = false;
            notifyBufferingStartEnd();
            if(mHasDisplay){
                getCallback().onPreparedStateChanged(this);
            }
        }
    }

    void release(){
        changeToUninitialized();
        mHasDisplay = false;
        mMediaPlayer.release();
        mLibVLC.release();
    }

    @Override
    public void onDetachedFromHost() {
        if(mSurfaceHolderGlueHost != null){
            mSurfaceHolderGlueHost.setSurfaceHolderCallback(null);
            mSurfaceHolderGlueHost = null;
        }
        reset();
        release();
    }

    class VideoPlayerSurfaceHolderCallback implements SurfaceHolder.Callback {
        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {
            setDisplay(surfaceHolder);
        }

        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
            setDisplay(null);
        }
    }
}
