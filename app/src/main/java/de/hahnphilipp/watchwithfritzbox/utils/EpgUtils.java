package de.hahnphilipp.watchwithfritzbox.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.Room;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import de.hahnphilipp.watchwithfritzbox.R;

public class EpgUtils {

    private static EpgDatabase db;
    private static final ArrayList<EpgUpdateListener> epgUpdateListeners = new ArrayList<>();
    private static final HashMap<Integer, EpgEvent> epgNowCache = new HashMap<>();
    private static Timer epgNowUpdateTimer;

    private static final long REMOVE_EVENT_TIME = 60 * 60; // 1 hour

    private static EpgDatabase getDatabase(Context context) {
        if(db == null) {
            db = Room.databaseBuilder(context, EpgDatabase.class, "epg.db").allowMainThreadQueries().build();
        }
        return db;
    }

    public static List<EpgEvent> getAllEvents(Context context, int channelNumber) {
        return getDatabase(context).epgDao().getEventsForChannel(channelNumber);
    }

    public static List<EpgEvent> getAllEventsEndingAfter(Context context, int channelNumber, long time) {
        return getDatabase(context).epgDao().getEventsForChannelEndingAfter(channelNumber, time);
    }

    public static void swapChannelPositions(Context context, int fromChannelPos, int toChannelPos) {
        SharedPreferences sp = context.getSharedPreferences(
                context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();

        String fromEvents = sp.getString("events" + fromChannelPos, "[]");
        String toEvents = sp.getString("events" + toChannelPos, "[]");
        editor.putString("events" + fromChannelPos, toEvents);
        editor.putString("events" + toChannelPos, fromEvents);
        editor.commit();
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
        @Ignore
        public boolean isEmpty = false;

        public static EpgEvent createEmptyEvent(Context context, long startTime, long duration) {
            EpgEvent event = new EpgEvent();
            event.id = -new Random().nextLong();
            event.startTime = startTime;
            event.duration = duration;
            event.title = context.getString(R.string.epg_no_program);
            event.isEmpty = true;
            return event;
        }

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

    }

}
