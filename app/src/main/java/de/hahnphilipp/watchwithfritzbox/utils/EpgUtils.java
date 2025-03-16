package de.hahnphilipp.watchwithfritzbox.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import de.hahnphilipp.watchwithfritzbox.R;

public class EpgUtils {


    public static HashMap<Long, EpgEvent> getAllEvents(Context context, int channelNumber) {
        SharedPreferences sp = context.getSharedPreferences(
                context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);

        Type eventMapType = new TypeToken<HashMap<Long, EpgEvent>>() {
        }.getType();

        HashMap<Long, EpgEvent> events = new Gson().fromJson(sp.getString("events" + channelNumber, "[]"), eventMapType);
        return events;
    }

    public static void addEvent(Context context, int channelNumber, EpgEvent epgEvent) {
        SharedPreferences sp = context.getSharedPreferences(
                context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();

        HashMap<Long, EpgEvent> allEvents = getAllEvents(context, channelNumber);
        allEvents = new HashMap<>(allEvents.entrySet().stream().filter(entry -> entry.getValue().id != epgEvent.id).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        allEvents.put(epgEvent.startTime, epgEvent);

        Type eventMapType = new TypeToken<HashMap<Long, EpgEvent>>() {
        }.getType();
        String channelsJson = new Gson().toJson(allEvents, eventMapType);
        editor.putString("events" + channelNumber, channelsJson);
        editor.commit();
    }

    public static EpgEvent getEventAtTime(Context context, int channelNumber, long timeInSec) {
        HashMap<Long, EpgEvent> allEvents = getAllEvents(context, channelNumber);
        long nearestEventTime = -1;
        for (long eventTime : allEvents.keySet()) {
            if (eventTime <= timeInSec) {
                if (timeInSec - eventTime < timeInSec - nearestEventTime) {
                    nearestEventTime = eventTime;
                }
            }
        }

        EpgEvent event = allEvents.get(nearestEventTime);
        if (event != null && nearestEventTime + event.duration >= timeInSec) {
            return event;
        } else {
            return null;
        }
    }

    public static EpgEvent getEventNow(Context context, int channelNumber) {
        return getEventAtTime(context, channelNumber, System.currentTimeMillis() / 1000);
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
