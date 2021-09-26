package de.hahnphilipp.watchwithfritzbox.rich;

import android.content.Context;
import android.media.tv.TvInputManager;
import android.media.tv.TvInputService;
import android.media.tv.TvTrackInfo;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;

import com.google.android.media.tv.companionlibrary.BaseTvInputService;
import com.google.android.media.tv.companionlibrary.TvPlayer;
import com.google.android.media.tv.companionlibrary.model.Channel;
import com.google.android.media.tv.companionlibrary.model.Program;
import com.google.android.media.tv.companionlibrary.model.RecordedProgram;
import com.google.android.media.tv.companionlibrary.utils.TvContractUtils;

import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.interfaces.IMedia;

import java.util.ArrayList;
import java.util.Locale;

import de.hahnphilipp.watchwithfritzbox.R;
import de.hahnphilipp.watchwithfritzbox.utils.ChannelUtils;

import static android.media.tv.TvTrackInfo.TYPE_AUDIO;
import static android.media.tv.TvTrackInfo.TYPE_SUBTITLE;
import static android.media.tv.TvTrackInfo.TYPE_VIDEO;

public class RichTvInputService extends BaseTvInputService {

    @Nullable
    @Override
    public TvInputService.Session onCreateSession(String inputId) {
        RichTvInputSessionImpl session = new RichTvInputSessionImpl(this, inputId);
        //session.setOverlayViewEnabled(true);
        Log.d("RichTvInputService", inputId+"");
        return super.sessionCreated(session);
    }

    class RichTvInputSessionImpl extends BaseTvInputService.Session {
        RichTvPlayer tvPlayer = null;
        Surface mSurface = null;

        int deselectAudioTrackId = -1;
        int deselectSubtitleTrackId = -1;

        public RichTvInputSessionImpl(Context context, String inputId) {
            super(context, inputId);
        }

        @Override
        public TvPlayer getTvPlayer() {
            if(tvPlayer == null){
                tvPlayer = new RichTvPlayer(getApplicationContext());
            }
            return tvPlayer;
        }

        @Override
        public boolean onPlayProgram(Program program, long startPosMs) {
            return false;
        }

        @Override
        public boolean onPlayRecordedProgram(RecordedProgram recordedProgram) {
            return false;
        }

        @Override
        public void onSetCaptionEnabled(boolean enabled) {

        }

        @Override
        public boolean onTune(Uri channelUri) {
            getTvPlayer();
            notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_TUNING);

            Channel ch = TvContractUtils.getChannel(getApplicationContext().getContentResolver(), channelUri);
            tvPlayer.loadMedia(Uri.parse(ch.getInternalProviderData().getVideoUrl()));
            tvPlayer.play();
            tvPlayer.setEventListener(new MediaPlayer.EventListener() {
                @Override
                public void onEvent(MediaPlayer.Event event) {
                    switch (event.type) {
                        case MediaPlayer.Event.Buffering:
                            if(event.getBuffering() == 100F) {
                                notifyVideoAvailable();
                                ArrayList<TvTrackInfo> tvTrackInfoList = new ArrayList<TvTrackInfo>();

                                String audioId = null;
                                String subtitleId = null;

                                MediaPlayer.TrackDescription[] descriptionsAudio = tvPlayer.player.getAudioTracks();
                                for(MediaPlayer.TrackDescription desc : descriptionsAudio){
                                    if(desc.name.equalsIgnoreCase("Disable")) {
                                        deselectAudioTrackId = desc.id;
                                        continue;
                                    }

                                    TvTrackInfo.Builder builder = new TvTrackInfo.Builder(TYPE_AUDIO, desc.id+"");
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        builder.setDescription((CharSequence) desc.name);
                                    }
                                    a:for(Locale locale : Locale.getAvailableLocales()){
                                        if(desc.name.toLowerCase().contains(locale.getDisplayLanguage(Locale.ENGLISH).toLowerCase())){
                                            builder.setLanguage(locale.getISO3Language());
                                            break a;
                                        }
                                    }
                                    tvTrackInfoList.add(builder.build());

                                    if(tvPlayer.player.getAudioTrack() == desc.id){
                                        audioId = desc.name;
                                    }
                                }
                                MediaPlayer.TrackDescription[] descriptionsSubtitle = tvPlayer.player.getSpuTracks();
                                for(MediaPlayer.TrackDescription desc : descriptionsAudio){
                                    if(desc.name.equalsIgnoreCase("Disable")) {
                                        deselectSubtitleTrackId = desc.id;
                                        continue;
                                    }

                                    TvTrackInfo.Builder builder = new TvTrackInfo.Builder(TYPE_SUBTITLE, desc.id+"");
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        builder.setDescription((CharSequence) desc.name);
                                    }
                                    a:for(Locale locale : Locale.getAvailableLocales()){
                                        if(desc.name.toLowerCase().contains(locale.getDisplayLanguage(Locale.ENGLISH).toLowerCase())){
                                            builder.setLanguage(locale.getISO3Language());
                                            break a;
                                        }
                                    }
                                    tvTrackInfoList.add(builder.build());

                                    if(tvPlayer.player.getSpuTrack() == desc.id){
                                        subtitleId = desc.name;
                                    }
                                }
                                notifyTracksChanged(tvTrackInfoList);


                                if(tvPlayer.player.getCurrentVideoTrack() != null) {
                                    TvTrackInfo.Builder builder = new TvTrackInfo.Builder(TvTrackInfo.TYPE_VIDEO, "video");
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        builder.setDescription((CharSequence) "Video");
                                    }
                                    builder.setVideoFrameRate(tvPlayer.player.getRate());
                                    float height = tvPlayer.player.getCurrentVideoTrack().height;
                                    if(height == 1088F) height = 1080F;
                                    float width = tvPlayer.player.getCurrentVideoTrack().width;
                                    builder.setVideoHeight((int)height);
                                    builder.setVideoWidth((int)width);
                                    tvTrackInfoList.add(builder.build());
                                }


                                if(audioId != null)
                                    notifyTrackSelected(TYPE_AUDIO, audioId);
                                if(subtitleId != null)
                                    notifyTrackSelected(TYPE_SUBTITLE, subtitleId);
                                notifyTrackSelected(TYPE_VIDEO, "video");
                                tvPlayer.setEventListener(null);
                            }
                            break;
                        default:
                            break;
                    }
                }
            });
            notifyVideoAvailable();
            return true;
        }

        @Override
        public boolean onSelectTrack(int type, @Nullable String trackId) {
            if(type == TYPE_AUDIO){
                tvPlayer.player.setAudioTrack(trackId == null ? deselectAudioTrackId : Integer.parseInt(trackId));
            }else if(type == TYPE_SUBTITLE){
                tvPlayer.player.setSpuTrack(trackId == null ? deselectSubtitleTrackId : Integer.parseInt(trackId));
            }
            return true;
        }

        private void releasePlayer() {
            if (tvPlayer != null) {
                tvPlayer.setSurface(null);
                tvPlayer.stop();
                tvPlayer.release();
                tvPlayer = null;
            }
        }

        @Override
        public void onRelease() {
            super.onRelease();
            releasePlayer();
        }



    }
}
