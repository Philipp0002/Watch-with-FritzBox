package de.hahnphilipp.watchwithfritzbox.rich;

import android.content.Context;
import android.content.res.Resources;
import android.media.tv.TvInputManager;
import android.media.tv.TvInputService;
import android.media.tv.TvTrackInfo;
import android.net.Uri;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.Nullable;


import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.interfaces.IMedia;
import org.videolan.libvlc.interfaces.IVLCVout;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

import static android.media.tv.TvTrackInfo.TYPE_AUDIO;
import static android.media.tv.TvTrackInfo.TYPE_SUBTITLE;
import static android.media.tv.TvTrackInfo.TYPE_VIDEO;

import de.hahnphilipp.watchwithfritzbox.utils.ChannelUtils;
import kotlin.Unit;
import kotlinx.coroutines.CompletableDeferred;

public class RichTvInputService extends TvInputService {

    @Nullable
    @Override
    public TvInputService.Session onCreateSession(String inputId) {
        RichTvInputSessionImpl session = new RichTvInputSessionImpl(this);
        //session.setOverlayViewEnabled(true);
        Log.d("RichTvInputService", inputId + "");
        return session;
    }

    class RichTvInputSessionImpl extends TvInputService.Session {

        Surface surface;
        String deselectAudioTrackId = "-1";
        String deselectSubtitleTrackId = "-1";

        private LibVLC libVlc;
        public MediaPlayer player;

        public RichTvInputSessionImpl(Context context) {
            super(context);
            ArrayList<String> options = new ArrayList<>();
            options.add("-vv");

            libVlc = new LibVLC(context, options);
            player = new MediaPlayer(libVlc);
        }

        @Override
        public void onSetCaptionEnabled(boolean enabled) {

        }

        @Override
        public boolean onTune(Uri channelUri) {

            Log.d("RICHTVINPUUT", "onTune");
            player.stop();
            notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_TUNING);


            long richChannelId = Long.parseLong(channelUri.getLastPathSegment());
            Map<Long, Integer> mapping = ChannelUtils.getChannelIDMappingForRichTv(getApplicationContext());
            int channelNr = mapping.get(richChannelId);
            ChannelUtils.Channel channel = ChannelUtils.getChannelByNumber(getApplicationContext(), channelNr);

            try {
                final Media media = new Media(libVlc, Uri.parse(channel.url));
                /*media.setHWDecoderEnabled(true, false);
                media.addOption(":clock-jitter=0");
                media.addOption(":clock-synchro=0");
                media.addOption(":network-caching=1000"); // In milliseconds
                media.addOption(":sout-keep");
                media.addOption(":audio-time-stretch");*/

                player.setMedia(media);

                media.release();
                player.play();
            } catch (Exception e) {
                notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_UNKNOWN);
                e.printStackTrace();
                return false;
            }
            player.setEventListener(new MediaPlayer.EventListener() {
                @Override
                public void onEvent(MediaPlayer.Event event) {
                    switch (event.type) {
                        case MediaPlayer.Event.Buffering:
                            if (event.getBuffering() == 100F) {
                                notifyVideoAvailable();
                                ArrayList<TvTrackInfo> tvTrackInfoList = new ArrayList<TvTrackInfo>();

                                String audioId = null;
                                String subtitleId = null;

                                IMedia.Track[] descriptionsAudio = player.getTracks(Media.Track.Type.Audio);
                                for (IMedia.Track desc : descriptionsAudio) {
                                    if (desc.name.equalsIgnoreCase("Disable")) {
                                        deselectAudioTrackId = desc.id;
                                        continue;
                                    }

                                    TvTrackInfo.Builder builder = new TvTrackInfo.Builder(TYPE_AUDIO, desc.id + "");
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        builder.setDescription((CharSequence) desc.name);
                                    }
                                    a:
                                    for (Locale locale : Locale.getAvailableLocales()) {
                                        if (desc.name.toLowerCase().contains(locale.getDisplayLanguage(Locale.ENGLISH).toLowerCase())) {
                                            builder.setLanguage(locale.getISO3Language());
                                            break a;
                                        }
                                    }
                                    tvTrackInfoList.add(builder.build());

                                    if (player.getSelectedTrack(IMedia.Track.Type.Audio).id.equals(desc.id)) {
                                        audioId = desc.name;
                                    }
                                }
                                IMedia.Track[] descriptionsSubtitle = player.getTracks(Media.Track.Type.Text);
                                for (IMedia.Track desc : descriptionsAudio) {
                                    if (desc.name.equalsIgnoreCase("Disable")) {
                                        deselectSubtitleTrackId = desc.id;
                                        continue;
                                    }

                                    TvTrackInfo.Builder builder = new TvTrackInfo.Builder(TYPE_SUBTITLE, desc.id + "");
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        builder.setDescription((CharSequence) desc.name);
                                    }
                                    a:
                                    for (Locale locale : Locale.getAvailableLocales()) {
                                        if (desc.name.toLowerCase().contains(locale.getDisplayLanguage(Locale.ENGLISH).toLowerCase())) {
                                            builder.setLanguage(locale.getISO3Language());
                                            break a;
                                        }
                                    }
                                    tvTrackInfoList.add(builder.build());

                                    if (player.getSelectedTrack(IMedia.Track.Type.Text).id.equals(desc.id)) {
                                        subtitleId = desc.name;
                                    }
                                }
                                notifyTracksChanged(tvTrackInfoList);


                                if (player.getSelectedTrack(IMedia.Track.Type.Video) != null) {
                                    Media.VideoTrack videoTrack = (Media.VideoTrack) player.getSelectedTrack(IMedia.Track.Type.Video);
                                    TvTrackInfo.Builder builder = new TvTrackInfo.Builder(TvTrackInfo.TYPE_VIDEO, "video");
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        builder.setDescription((CharSequence) "Video");
                                    }
                                    builder.setVideoFrameRate(player.getRate());
                                    float height = videoTrack.height;
                                    if (height == 1088F) height = 1080F;
                                    float width = videoTrack.width;
                                    builder.setVideoHeight((int) height);
                                    builder.setVideoWidth((int) width);
                                    tvTrackInfoList.add(builder.build());
                                }


                                if (audioId != null)
                                    notifyTrackSelected(TYPE_AUDIO, audioId);
                                if (subtitleId != null)
                                    notifyTrackSelected(TYPE_SUBTITLE, subtitleId);
                                notifyTrackSelected(TYPE_VIDEO, "video");
                                player.setEventListener(null);
                            }
                            break;
                        default:
                            break;
                    }
                }
            });
            notifyVideoAvailable();
            Log.d("RICHTVINPUUT", "notifyVideoAvailable");
            return true;
        }

        @Override
        public boolean onSelectTrack(int type, @Nullable String trackId) {
            if(trackId == null) {
                player.unselectTrackType(type == TYPE_AUDIO ? IMedia.Track.Type.Audio : IMedia.Track.Type.Text);
            } else {
                player.selectTrack(trackId);
            }
            return true;
        }

        private void releasePlayer() {
            if (player != null) {
                player.stop();
                player.release();
                player = null;
            }
        }

        @Override
        public void onRelease() {
            releasePlayer();
        }

        @Override
        public boolean onSetSurface(@Nullable Surface surface) {
            Log.d("RICHTVINPUUT", "onsetsurface " + surface);
            if(surface == null) return false;
            this.surface = surface;
            final IVLCVout vlcVout = player.getVLCVout();
            DisplayMetrics dm = Resources.getSystem().getDisplayMetrics();
            vlcVout.setVideoSurface(surface, null);
            vlcVout.setWindowSize(dm.widthPixels, dm.heightPixels);
            vlcVout.attachViews();
            return true;
        }

        @Override
        public void onSetStreamVolume(float volume) {
            player.setVolume((int) (volume * 100));
        }


    }
}
