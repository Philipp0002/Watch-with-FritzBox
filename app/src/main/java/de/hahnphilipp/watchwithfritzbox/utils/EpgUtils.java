package de.hahnphilipp.watchwithfritzbox.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.util.Log;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.Room;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import de.hahnphilipp.watchwithfritzbox.R;

public class EpgUtils {

    private static EpgDatabase db;

    private static final long REMOVE_EVENT_TIME = 60 * 60; // 1 hour
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static EpgDatabase getDatabase(Context context) {
        if(db == null) {
            db = Room.databaseBuilder(context, EpgDatabase.class, "epg.db").allowMainThreadQueries().build();
        }
        return db;
    }

    public static List<EpgEvent> getAllEvents(Context context, int channelNumber) {
        return getDatabase(context).epgDao().getEventsForChannel(channelNumber);
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

        // CLEANUP OLD EVENTS
        /*allEvents = allEvents.entrySet().stream()
                .filter(entry -> entry.getValue().eitReceivedTimeMillis + (REMOVE_EVENT_TIME*1000) > System.currentTimeMillis())
                .filter(entry -> entry.getValue().startTime >= epgEvent.startTime + epgEvent.duration ||
                        epgEvent.startTime >= entry.getValue().startTime + entry.getValue().duration)
                .collect(HashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), HashMap::putAll);*/
    }

    public static EpgEvent getEventAtTime(Context context, int channelNumber, LocalDateTime localDateTime) {
        long timeInSec = localDateTime.atZone(ZoneId.systemDefault()).toEpochSecond();

        return getDatabase(context).epgDao().getEventAtTime(channelNumber, timeInSec);
    }

    public static EpgEvent getEventNow(Context context, int channelNumber) {
        return getEventAtTime(context, channelNumber, LocalDateTime.now());
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

    @Entity(primaryKeys = {"channelNumber", "id"})
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
