package de.hahnphilipp.watchwithfritzbox.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.Room;
import androidx.tvprovider.media.tv.TvContractCompat;

import org.videolan.libvlc.MediaPlayer;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import de.hahnphilipp.watchwithfritzbox.R;
import de.hahnphilipp.watchwithfritzbox.rich.RichTvUtils;

public class EpgUtils {

    private static EpgDatabase db;
    private static final ArrayList<EpgUpdateListener> epgUpdateListeners = new ArrayList<>();
    private static final HashMap<Integer, EpgEvent> epgNowCache = new HashMap<>();
    private static Timer epgNowUpdateTimer;

    private static final long REMOVE_EVENT_TIME = 60 * 60; // 1 hour

    public static void processVlcEpgEvent(Context context, MediaPlayer.EpgEvent vlcEvent) {
        ChannelUtils.Channel ch = ChannelUtils.getChannelByDvb(context, vlcEvent.getNetworkId(), vlcEvent.getTransportStreamId(), vlcEvent.getServiceId());

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
            epgEvent.rating = vlcEvent.getRating();
            epgEvent.genre = vlcEvent.getGenre();
            epgEvent.subGenre = vlcEvent.getSubGenre();
            epgEvent.lang = vlcEvent.getLanguage();

            EpgUtils.addEvent(context, epgEvent);
        }
    }

    private static EpgDatabase getDatabase(Context context) {
        if(db == null) {
            db = Room.databaseBuilder(context, EpgDatabase.class, "epg.db").allowMainThreadQueries().build();
        }
        return db;
    }

    public static List<EpgEvent> getAllEvents(Context context, int channelNumber) {
        return getDatabase(context).epgDao().getEventsForChannel(channelNumber);
    }

    public static List<EpgEvent> getAllEvents(Context context) {
        return getDatabase(context).epgDao().getAllEvents();
    }

    public static List<EpgEvent> getAllEventsEndingAfter(Context context, int channelNumber, long time) {
        return getDatabase(context).epgDao().getEventsForChannelEndingAfter(channelNumber, time);
    }

    public static void moveChannelPosition(Context context, int fromChannelPos, int toChannelPos) {
        if (fromChannelPos == toChannelPos) {
            return;
        }
        getDatabase(context).epgDao().moveChannelEvents(fromChannelPos, toChannelPos);

        epgNowCache.clear();
    }

    public static void addEvent(Context context, EpgEvent epgEvent) {
        getDatabase(context).epgDao().deleteExpiredAndOverlappingEvents(
                epgEvent.channelNumber,
                epgEvent.startTime,
                epgEvent.duration,
                REMOVE_EVENT_TIME,
                System.currentTimeMillis()
        );

        getDatabase(context).epgDao().insert(epgEvent);

        if(ChannelUtils.richTvEnabled(context)) {
            try {
                RichTvUtils.insertEpgEvent(context, epgEvent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static EpgEvent getEventAtTime(Context context, int channelNumber, LocalDateTime localDateTime) {
        long timeInSec = localDateTime.atZone(ZoneId.systemDefault()).toEpochSecond();

        return getDatabase(context).epgDao().getEventAtTime(channelNumber, timeInSec);
    }

    public static List<EpgEvent> getEventsAtTime(Context context, LocalDateTime localDateTime) {
        long timeInSec = localDateTime.atZone(ZoneId.systemDefault()).toEpochSecond();

        return getDatabase(context).epgDao().getEventsAtTime(timeInSec);
    }

    public static EpgEvent getEventNowFromCache(int channelNumber) {
        return epgNowCache.get(channelNumber);
    }

    public static List<EpgEvent> getEventsNow(Context context) {
        return getEventsAtTime(context, LocalDateTime.now());
    }

    public static EpgEvent getEventNext(Context context, int channelNumber) {
        List<EpgEvent> allEvents = getAllEvents(context, channelNumber);
        long nearestEventTime = -1;
        EpgEvent nearestEvent = null;
        for (EpgEvent epgEvent : allEvents) {
            long eventTime = epgEvent.startTime;
            if (eventTime > System.currentTimeMillis() / 1000) {
                if (nearestEventTime == -1 || eventTime < nearestEventTime) {
                    nearestEventTime = eventTime;
                    nearestEvent = epgEvent;
                }
            }
        }

        return nearestEvent;
    }

    public static void resetEpgDatabase(Context context) {
        epgNowCache.clear();
        getDatabase(context).epgDao().deleteAll();
    }

    public static int secondsToPx(float seconds) {
        return (int) (seconds / 8 * Resources.getSystem().getDisplayMetrics().density);
    }

    public static void addEpgUpdateListener(EpgUpdateListener listener) {
        epgUpdateListeners.add(listener);
    }
    public static void removeEpgUpdateListener(EpgUpdateListener listener) {
        epgUpdateListeners.remove(listener);
    }
    public static void notifyEpgNowChanged(ChannelUtils.Channel channel, EpgEvent newEpgEvent) {
        for(EpgUpdateListener listener : epgUpdateListeners) {
            listener.onEpgNowChanged(channel, newEpgEvent);
        }
    }

    public static void runEpgNowUpdateScheduler(Context context) {
        if(epgNowUpdateTimer != null) {
            epgNowUpdateTimer.cancel();
        }
        epgNowUpdateTimer = new Timer();
        epgNowUpdateTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                List<EpgEvent> eventsNow = getEventsNow(context);
                List<EpgEvent> epgNowCacheOld = new ArrayList<>(epgNowCache.values());
                epgNowCache.clear();
                for(EpgEvent eventNow : eventsNow) {
                    ChannelUtils.Channel channel = ChannelUtils.getChannelByNumber(context, eventNow.channelNumber);
                    if(channel != null) {
                        epgNowCache.put(eventNow.channelNumber, eventNow);
                        if(!epgNowCacheOld.contains(eventNow)) {
                            notifyEpgNowChanged(channel, eventNow);
                        }
                    }
                }
            }
        }, 0, 30 * 1000);
    }

    public interface EpgUpdateListener {
        void onEpgNowChanged(ChannelUtils.Channel channel, EpgEvent newEpgEvent);
    }

    @Entity(
            primaryKeys = {"channelNumber", "id"},
            indices = {
                    @Index(
                            name = "idx_epg_channel_time",
                            value = {"channelNumber", "startTime"}
                    )
            }
    )
    public static class EpgEvent {
        public int channelNumber;
        public long id;
        public long eitReceivedTimeMillis;
        public long startTime;
        public long duration;
        public String lang;
        public String title;
        public String subtitle;
        public String description;
        public Integer rating;
        // https://www.apt-browse.com/browse/debian/jessie/main/amd64/libdvbpsi-dev/1.2.0-1/file/usr/include/dvbpsi/dr_54.h
        public Integer genre;
        public Integer subGenre;

        @Ignore
        public LocalDateTime getStartLocalDateTime() {
            return Instant.ofEpochSecond(this.startTime).atZone(ZoneId.systemDefault()).toLocalDateTime();
        }

        @Ignore
        public LocalDateTime getEndLocalDateTime() {
            try {
                return Instant.ofEpochSecond(this.startTime + this.duration).atZone(ZoneId.systemDefault()).toLocalDateTime();
            } catch (Exception e) {
                return null;
            }
        }

        @StringRes
        @Nullable
        @Ignore
        public Integer getGenreStringResId() {
            if(this.genre == null) {
                return null;
            }
            Integer subGenre = this.subGenre != null ? this.subGenre : 0x00;
            return genreMap.get(Pair.create(this.genre, subGenre));
        }

        @Ignore
        public String getRichTvGenre() {
            if(this.genre == null) {
                return null;
            }
            return richGenreMap.get(this.genre);
        }

    }

    private static Map<Pair<Integer, Integer>, Integer> genreMap;
    private static Map<Integer, String> richGenreMap;

    static {
        richGenreMap = new HashMap<>();
        richGenreMap.put(0x01,  TvContractCompat.Programs.Genres.MOVIES);
        richGenreMap.put(0x02,  TvContractCompat.Programs.Genres.NEWS);
        richGenreMap.put(0x03,  TvContractCompat.Programs.Genres.ENTERTAINMENT);
        richGenreMap.put(0x04,  TvContractCompat.Programs.Genres.SPORTS);
        richGenreMap.put(0x05,  TvContractCompat.Programs.Genres.FAMILY_KIDS);
        richGenreMap.put(0x06,  TvContractCompat.Programs.Genres.MUSIC);
        richGenreMap.put(0x07,  TvContractCompat.Programs.Genres.ARTS);
        richGenreMap.put(0x08,  TvContractCompat.Programs.Genres.LIFE_STYLE);
        richGenreMap.put(0x09,  TvContractCompat.Programs.Genres.EDUCATION);
        richGenreMap.put(0x0A,  TvContractCompat.Programs.Genres.TRAVEL);

        genreMap = new HashMap<>();
        genreMap.put(Pair.create(0x01, 0x00), R.string.epg_event_category_movie);
        genreMap.put(Pair.create(0x01, 0x01), R.string.epg_event_category_movie_detective);
        genreMap.put(Pair.create(0x01, 0x02), R.string.epg_event_category_movie_adventure);
        genreMap.put(Pair.create(0x01, 0x03), R.string.epg_event_category_movie_sf);
        genreMap.put(Pair.create(0x01, 0x04), R.string.epg_event_category_movie_comedy);
        genreMap.put(Pair.create(0x01, 0x05), R.string.epg_event_category_movie_soap);
        genreMap.put(Pair.create(0x01, 0x06), R.string.epg_event_category_movie_romance);
        genreMap.put(Pair.create(0x01, 0x07), R.string.epg_event_category_movie_classical);
        genreMap.put(Pair.create(0x01, 0x08), R.string.epg_event_category_movie_adult);

        genreMap.put(Pair.create(0x02, 0x00), R.string.epg_event_category_news);
        genreMap.put(Pair.create(0x02, 0x01), R.string.epg_event_category_news_weather);
        genreMap.put(Pair.create(0x02, 0x02), R.string.epg_event_category_news_magazine);
        genreMap.put(Pair.create(0x02, 0x03), R.string.epg_event_category_news_documentary);
        genreMap.put(Pair.create(0x02, 0x04), R.string.epg_event_category_news_discussion);

        genreMap.put(Pair.create(0x03, 0x00), R.string.epg_event_category_show);
        genreMap.put(Pair.create(0x03, 0x01), R.string.epg_event_category_show_quiz);
        genreMap.put(Pair.create(0x03, 0x02), R.string.epg_event_category_show_variety);
        genreMap.put(Pair.create(0x03, 0x03), R.string.epg_event_category_show_talk);

        genreMap.put(Pair.create(0x04, 0x00), R.string.epg_event_category_sports);
        genreMap.put(Pair.create(0x04, 0x01), R.string.epg_event_category_sports_events);
        genreMap.put(Pair.create(0x04, 0x02), R.string.epg_event_category_sports_magazine);
        genreMap.put(Pair.create(0x04, 0x03), R.string.epg_event_category_sports_football);
        genreMap.put(Pair.create(0x04, 0x04), R.string.epg_event_category_sports_tennis);
        genreMap.put(Pair.create(0x04, 0x05), R.string.epg_event_category_sports_team);
        genreMap.put(Pair.create(0x04, 0x06), R.string.epg_event_category_sports_athletics);
        genreMap.put(Pair.create(0x04, 0x07), R.string.epg_event_category_sports_motor);
        genreMap.put(Pair.create(0x04, 0x08), R.string.epg_event_category_sports_water);        genreMap.put(Pair.create(0x04, 0x00), R.string.epg_event_category_sports);
        genreMap.put(Pair.create(0x04, 0x09), R.string.epg_event_category_sports_winter);
        genreMap.put(Pair.create(0x04, 0x0A), R.string.epg_event_category_sports_equestrian);
        genreMap.put(Pair.create(0x04, 0x0B), R.string.epg_event_category_sports_martial);

        genreMap.put(Pair.create(0x05, 0x00), R.string.epg_event_category_children);
        genreMap.put(Pair.create(0x05, 0x01), R.string.epg_event_category_children_prescool);
        genreMap.put(Pair.create(0x05, 0x02), R.string.epg_event_category_children_0614);
        genreMap.put(Pair.create(0x05, 0x03), R.string.epg_event_category_children_1016);
        genreMap.put(Pair.create(0x05, 0x04), R.string.epg_event_category_children_educational);
        genreMap.put(Pair.create(0x05, 0x05), R.string.epg_event_category_children_cartoons);

        genreMap.put(Pair.create(0x06, 0x00), R.string.epg_event_category_music);
        genreMap.put(Pair.create(0x06, 0x01), R.string.epg_event_category_music_poprock);
        genreMap.put(Pair.create(0x06, 0x02), R.string.epg_event_category_music_classical);
        genreMap.put(Pair.create(0x06, 0x03), R.string.epg_event_category_music_folk);
        genreMap.put(Pair.create(0x06, 0x04), R.string.epg_event_category_music_jazz);
        genreMap.put(Pair.create(0x06, 0x05), R.string.epg_event_category_music_opera);
        genreMap.put(Pair.create(0x06, 0x06), R.string.epg_event_category_music_ballet);

        genreMap.put(Pair.create(0x07, 0x00), R.string.epg_event_category_culture);
        genreMap.put(Pair.create(0x07, 0x01), R.string.epg_event_category_culture_performance);
        genreMap.put(Pair.create(0x07, 0x02), R.string.epg_event_category_culture_finearts);
        genreMap.put(Pair.create(0x07, 0x03), R.string.epg_event_category_culture_religion);
        genreMap.put(Pair.create(0x07, 0x04), R.string.epg_event_category_culture_traditional);
        genreMap.put(Pair.create(0x07, 0x05), R.string.epg_event_category_culture_literature);
        genreMap.put(Pair.create(0x07, 0x06), R.string.epg_event_category_culture_cinema);
        genreMap.put(Pair.create(0x07, 0x07), R.string.epg_event_category_culture_experimental);
        genreMap.put(Pair.create(0x07, 0x08), R.string.epg_event_category_culture_press);
        genreMap.put(Pair.create(0x07, 0x09), R.string.epg_event_category_culture_newmedia);
        genreMap.put(Pair.create(0x07, 0x0A), R.string.epg_event_category_culture_magazine);
        genreMap.put(Pair.create(0x07, 0x0B), R.string.epg_event_category_culture_fashion);

        genreMap.put(Pair.create(0x08, 0x00), R.string.epg_event_category_social);
        genreMap.put(Pair.create(0x08, 0x01), R.string.epg_event_category_social_magazine);
        genreMap.put(Pair.create(0x08, 0x02), R.string.epg_event_category_social_advisory);
        genreMap.put(Pair.create(0x08, 0x03), R.string.epg_event_category_social_people);

        genreMap.put(Pair.create(0x09, 0x00), R.string.epg_event_category_education);
        genreMap.put(Pair.create(0x09, 0x01), R.string.epg_event_category_education_nature);
        genreMap.put(Pair.create(0x09, 0x02), R.string.epg_event_category_education_technology);
        genreMap.put(Pair.create(0x09, 0x03), R.string.epg_event_category_education_medicine);
        genreMap.put(Pair.create(0x09, 0x04), R.string.epg_event_category_education_foreign);
        genreMap.put(Pair.create(0x09, 0x05), R.string.epg_event_category_education_social);
        genreMap.put(Pair.create(0x09, 0x06), R.string.epg_event_category_education_further);
        genreMap.put(Pair.create(0x09, 0x07), R.string.epg_event_category_education_language);

        genreMap.put(Pair.create(0x0A, 0x00), R.string.epg_event_category_leisure);
        genreMap.put(Pair.create(0x0A, 0x01), R.string.epg_event_category_leisure_travel);
        genreMap.put(Pair.create(0x0A, 0x02), R.string.epg_event_category_leisure_handicraft);
        genreMap.put(Pair.create(0x0A, 0x03), R.string.epg_event_category_leisure_motoring);
        genreMap.put(Pair.create(0x0A, 0x04), R.string.epg_event_category_leisure_fitness);
        genreMap.put(Pair.create(0x0A, 0x05), R.string.epg_event_category_leisure_cooking);
        genreMap.put(Pair.create(0x0A, 0x06), R.string.epg_event_category_leisure_shopping);
        genreMap.put(Pair.create(0x0A, 0x07), R.string.epg_event_category_leisure_gardening);

        genreMap.put(Pair.create(0x0B, 0x00), R.string.epg_event_category_special_originallanguage);
        genreMap.put(Pair.create(0x0B, 0x01), R.string.epg_event_category_special_blackwhite);
        genreMap.put(Pair.create(0x0B, 0x02), R.string.epg_event_category_special_unpublished);
        genreMap.put(Pair.create(0x0B, 0x03), R.string.epg_event_category_special_live);
    }

}
