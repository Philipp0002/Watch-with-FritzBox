package de.hahnphilipp.watchwithfritzbox.rich;

import static android.media.tv.TvTrackInfo.TYPE_AUDIO;
import static android.media.tv.TvTrackInfo.TYPE_SUBTITLE;
import static android.media.tv.TvTrackInfo.TYPE_VIDEO;
import static android.media.tv.interactive.TvInteractiveAppServiceInfo.INTERACTIVE_APP_TYPE_HBBTV;

import android.content.Context;
import android.content.res.Resources;
import android.media.tv.AitInfo;
import android.media.tv.TvInputManager;
import android.media.tv.TvInputService;
import android.media.tv.TvTrackInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.Nullable;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.interfaces.IVLCVout;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

import de.hahnphilipp.watchwithfritzbox.utils.ChannelUtils;
import de.hahnphilipp.watchwithfritzbox.utils.EpgUtils;

public class RichTvInputService extends TvInputService {

    @Nullable
    @Override
    public TvInputService.Session onCreateSession(String inputId) {
        RichTvInputSessionImpl session = new RichTvInputSessionImpl(this);
        //session.setOverlayViewEnabled(true);
        return session;
    }

    class RichTvInputSessionImpl extends TvInputService.Session implements MediaPlayer.EventListener {

        Surface surface;
        int deselectAudioTrackId = -1;
        int deselectSubtitleTrackId = -1;

        private LibVLC libVlc;
        public MediaPlayer player;

        public RichTvInputSessionImpl(Context context) {
            super(context);
            ArrayList<String> args = new ArrayList<>();
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

            libVlc = new LibVLC(context, args);
            player = new MediaPlayer(libVlc);
            player.setEventListener(this);
        }

        @Override
        public void onSetCaptionEnabled(boolean enabled) {

        }

        @Override
        public boolean onTune(Uri channelUri) {
            player.stop();
            notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_TUNING);


            long richChannelId = Long.parseLong(channelUri.getLastPathSegment());
            Map<Long, Integer> mapping = ChannelUtils.getChannelIDMappingForRichTv(getApplicationContext());
            if(mapping == null) {
                notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_UNKNOWN);
                return false;
            }
            int channelNr = mapping.get(richChannelId);
            ChannelUtils.Channel channel = ChannelUtils.getChannelByNumber(getApplicationContext(), channelNr);
            if(channel == null) {
                notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_UNKNOWN);
                return false;
            }

            try {
                final Media media = new Media(libVlc, Uri.parse(channel.url));

                player.setMedia(media);

                media.release();
                player.play();
            } catch (Exception e) {
                notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_UNKNOWN);
                e.printStackTrace();
                return false;
            }
            notifyVideoAvailable();
            return true;
        }

        @Override
        public boolean onSelectTrack(int type, @Nullable String trackId) {
            if(trackId == null) {
                if(type == TYPE_AUDIO) {
                    player.setAudioTrack(deselectAudioTrackId);
                } else if(type == TYPE_SUBTITLE) {
                    player.setSpuTrack(deselectSubtitleTrackId);
                }
            } else {
                if(type == TYPE_AUDIO) {
                    player.setAudioTrack(Integer.parseInt(trackId));
                } else if(type == TYPE_SUBTITLE) {
                    player.setSpuTrack(Integer.parseInt(trackId));
                }
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


        @Override
        public void onEvent(MediaPlayer.Event event) {
            switch (event.type) {
                case MediaPlayer.Event.CaInfoReceived:
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_CAS_NO_CARD);
                    } else {
                        notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_WEAK_SIGNAL);
                    }
                    break;
                case MediaPlayer.Event.EpgNewEvent:
                    AsyncTask.execute(() -> {
                        MediaPlayer.EpgEvent vlcEvent = event.getEvent();
                        EpgUtils.processVlcEpgEvent(getApplicationContext(), vlcEvent);
                    });
                    break;
                case MediaPlayer.Event.EpgNewServiceInfo:
                    AsyncTask.execute(() -> {
                        MediaPlayer.ServiceInfo serviceInfo = event.getServiceInfo();
                        ChannelUtils.processVlcServiceInfo(getApplicationContext(), serviceInfo);
                    });
                    break;
                case MediaPlayer.Event.CommonDescriptorsFound:
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        // HbbTV = 0x0010
                        AsyncTask.execute(() -> {
                            MediaPlayer.CommonDescriptors commonDescriptors = event.getCommonDescriptors();
                            if ("0x0010".equals(commonDescriptors.getApplicationId())) {
                                notifyAitInfoUpdated(new AitInfo(INTERACTIVE_APP_TYPE_HBBTV, commonDescriptors.getVersion().intValue()));
                            }
                        });
                    }
                    break;
                case MediaPlayer.Event.Buffering:
                    if (event.getBuffering() == 100F) {
                        notifyVideoAvailable();
                        ArrayList<TvTrackInfo> tvTrackInfoList = new ArrayList<>();

                        String audioId = null;
                        String subtitleId = null;

                        MediaPlayer.TrackDescription[] descriptionsAudio = player.getAudioTracks();
                        for (MediaPlayer.TrackDescription desc : descriptionsAudio) {
                            if (desc.name.equalsIgnoreCase("Disable")) {
                                deselectAudioTrackId = desc.id;
                                continue;
                            }

                            TvTrackInfo.Builder builder = new TvTrackInfo.Builder(TYPE_AUDIO, desc.id + "");
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                builder.setDescription(desc.name);
                            }
                            a:
                            for (Locale locale : Locale.getAvailableLocales()) {
                                if (desc.name.toLowerCase().contains(locale.getDisplayLanguage(Locale.ENGLISH).toLowerCase())) {
                                    builder.setLanguage(locale.getISO3Language());
                                    break a;
                                }
                            }
                            tvTrackInfoList.add(builder.build());

                            if (player.getAudioTrack() == desc.id) {
                                audioId = desc.name;
                            }
                        }
                        MediaPlayer.TrackDescription[] descriptionsSubtitle = player.getSpuTracks();
                        for (MediaPlayer.TrackDescription desc : descriptionsSubtitle) {
                            if (desc.name.equalsIgnoreCase("Disable")) {
                                deselectSubtitleTrackId = desc.id;
                                continue;
                            }

                            TvTrackInfo.Builder builder = new TvTrackInfo.Builder(TYPE_SUBTITLE, desc.id + "");
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                builder.setDescription(desc.name);
                            }
                            for (Locale locale : Locale.getAvailableLocales()) {
                                if (desc.name.toLowerCase().contains(locale.getDisplayLanguage(Locale.ENGLISH).toLowerCase())) {
                                    builder.setLanguage(locale.getISO3Language());
                                    break;
                                }
                            }
                            tvTrackInfoList.add(builder.build());

                            if (player.getSpuTrack() == desc.id) {
                                subtitleId = desc.name;
                            }
                        }
                        notifyTracksChanged(tvTrackInfoList);


                        Media.VideoTrack videoTrack = player.getCurrentVideoTrack();
                        if (videoTrack != null) {
                            TvTrackInfo.Builder builder = new TvTrackInfo.Builder(TvTrackInfo.TYPE_VIDEO, "video");
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                builder.setDescription("Video");
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
                    }
                    break;
                default:
                    break;
            }
        }
    }
}