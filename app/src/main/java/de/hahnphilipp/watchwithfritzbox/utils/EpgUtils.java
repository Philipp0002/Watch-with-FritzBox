package de.hahnphilipp.watchwithfritzbox.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Random;

import de.hahnphilipp.watchwithfritzbox.R;

public class EpgUtils {

    private static final long REMOVE_EVENT_TIME = 60 * 60; // 1 hour


    public static HashMap<Long, EpgEvent> getAllEvents(Context context, int channelNumber) {
        SharedPreferences sp = context.getSharedPreferences(
                context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);

        Type eventMapType = new TypeToken<HashMap<Long, EpgEvent>>() {
        }.getType();

        HashMap<Long, EpgEvent> events = new Gson().fromJson(sp.getString("events" + channelNumber, "[]"), eventMapType);

        return events;
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



    public static void addEvent(Context context, int channelNumber, EpgEvent epgEvent) {
        SharedPreferences sp = context.getSharedPreferences(
                context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();

        HashMap<Long, EpgEvent> allEvents = getAllEvents(context, channelNumber);

        // CLEANUP OLD EVENTS
        allEvents = allEvents.entrySet().stream()
                .filter(entry -> entry.getValue().eitReceivedTimeMillis + (REMOVE_EVENT_TIME*1000) > System.currentTimeMillis())
                .filter(entry -> entry.getValue().startTime >= epgEvent.startTime + epgEvent.duration ||
                        epgEvent.startTime >= entry.getValue().startTime + entry.getValue().duration)
                .collect(HashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), HashMap::putAll);


        allEvents.put(epgEvent.id, epgEvent);

        Type eventMapType = new TypeToken<HashMap<Long, EpgEvent>>() {
        }.getType();
        String channelsJson = new Gson().toJson(allEvents, eventMapType);
        editor.putString("events" + channelNumber, channelsJson);
        editor.apply();
    }

    public static EpgEvent getEventAtTime(Context context, int channelNumber, LocalDateTime localDateTime) {
        long timeInSec = localDateTime.atZone(ZoneId.systemDefault()).toEpochSecond();
        HashMap<Long, EpgEvent> allEvents = getAllEvents(context, channelNumber);
        for (EpgEvent event : allEvents.values()) {
            if (event.startTime <= timeInSec && event.startTime + event.duration >= timeInSec) {
                return event;
            }
        }
        return null;
    }

    public static EpgEvent getEventNow(Context context, int channelNumber) {
        return getEventAtTime(context, channelNumber, LocalDateTime.now());
    }

    public static EpgEvent getEventNext(Context context, int channelNumber) {
        HashMap<Long, EpgEvent> allEvents = getAllEvents(context, channelNumber);
        long nearestEventTime = -1;
        for (long eventTime : allEvents.keySet()) {
            if (eventTime > System.currentTimeMillis() / 1000) {
                if (nearestEventTime == -1 || eventTime < nearestEventTime) {
                    nearestEventTime = eventTime;
                }
            }
        }

        return allEvents.get(nearestEventTime);
    }

    public static int secondsToPx(float seconds) {
        return (int) (seconds / 8 * Resources.getSystem().getDisplayMetrics().density);
    }

    public static class EpgEvent {

        public long id;
        public long eitReceivedTimeMillis;
        public long startTime;
        public long duration;
        public String lang;
        public String title;
        public String subtitle;
        public String description;
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

        public LocalDateTime getStartLocalDateTime() {
            return Instant.ofEpochSecond(this.startTime).atZone(ZoneId.systemDefault()).toLocalDateTime();
        }

        public LocalDateTime getEndLocalDateTime() {
            try {
                return Instant.ofEpochSecond(this.startTime + this.duration).atZone(ZoneId.systemDefault()).toLocalDateTime();
            } catch (Exception e) {
                return null;
            }
        }

    }

}
