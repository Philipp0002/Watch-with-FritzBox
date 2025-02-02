package de.hahnphilipp.watchwithfritzbox.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;

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

    public static class EpgEvent {

        public long id;
        public long startTime;
        public long duration;
        public String lang;
        public String title;
        public String subtitle;
        public String description;

    }

}
