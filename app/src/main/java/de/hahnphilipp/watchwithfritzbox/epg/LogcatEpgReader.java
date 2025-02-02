package de.hahnphilipp.watchwithfritzbox.epg;

import android.content.Context;

import java.util.HashMap;

import de.hahnphilipp.watchwithfritzbox.async.AsyncLogcatReader;
import de.hahnphilipp.watchwithfritzbox.utils.ChannelUtils;
import de.hahnphilipp.watchwithfritzbox.utils.EpgUtils;

public class LogcatEpgReader {

    Context context;
    AsyncLogcatReader asyncLogcatReader;

    EpgUtils.EpgEvent lastReferencedEvent;

    int lastReferencedServiceId = -1;
    int lastReferencedServiceIdForEvents = -1;
    HashMap<Integer, Integer> serviceIdToChannelNr;

    public LogcatEpgReader(Context context) {
        this.context = context;
        serviceIdToChannelNr = new HashMap<>();
    }

    public void readLogcat() {
        stopLogcatRead();
        asyncLogcatReader = new AsyncLogcatReader(new AsyncLogcatReader.LogcatCallback() {
            @Override
            public void onLineRead(String lineUntrimmed) {
                if (lineUntrimmed.contains("EPGREAD")) return;

                if (lineUntrimmed.contains("D VLC") && lineUntrimmed.contains("ts demux:")) {
                    String line = lineUntrimmed
                            .substring(
                                    lineUntrimmed
                                            .indexOf("ts demux:") +
                                            "ts demux:".length()
                            ).trim();
                    processLine(line);
                }
                /*if(line.contains("* service")){
                    lastReferencedServiceId = line;
                }
                if(line.contains("- type=")){
                    lastReferencedChannel = line;
                    Log.d("EPGREAD", lastReferencedServiceId);
                    Log.d("EPGREAD", lastReferencedChannel);
                }*/
            }
        });
        asyncLogcatReader.execute();
    }

    private void processLine(String line) {
        if (line.startsWith("* service")) {
            int idIndex = line.indexOf("id=") + "id=".length();
            String serviceId = line.substring(idIndex, idIndex + 5);
            lastReferencedServiceId = Integer.parseInt(serviceId);
        } else if (line.startsWith("- type=")) {
            int providerIndex = line.indexOf("provider=") + "provider=".length();
            int providerEndIndex = line.indexOf("name=", providerIndex);
            int nameIndex = line.indexOf("name=") + "name=".length();

            String provider = line.substring(providerIndex, providerEndIndex);
            String name = line.substring(nameIndex);

            ChannelUtils.Channel ch = ChannelUtils.getChannelByTitle(context, name);
            if (ch == null) return;
            ChannelUtils.Channel originalCh = ch.copy();
            if (lastReferencedServiceId != -1) {
                ch.serviceId = lastReferencedServiceId;
                ch.provider = provider;
                ChannelUtils.updateChannel(context, originalCh, ch);
            }
            lastReferencedServiceId = -1;
        } else if (line.startsWith("new EIT")) {
            int serviceIdIndex = line.indexOf("service_id=") + "service_id=".length();
            int serviceIdEndIndex = line.indexOf(" ", serviceIdIndex);
            String serviceId = line.substring(serviceIdIndex, serviceIdEndIndex);
            lastReferencedServiceIdForEvents = Integer.parseInt(serviceId);
        } else if (line.startsWith("* event")) {
            int idIndex = line.indexOf("id=") + "id=".length();
            int idEndIndex = line.indexOf(" ", idIndex);
            int startTimeIndex = line.indexOf("start_time:") + "start_time:".length();
            int startTimeEndIndex = line.indexOf(" ", startTimeIndex);
            int durationIndex = line.indexOf("duration=") + "duration=".length();
            int durationEndIndex = line.indexOf(" ", durationIndex);

            String _eventId = line.substring(idIndex, idEndIndex);
            String _startTime = line.substring(startTimeIndex, startTimeEndIndex);
            String _duration = line.substring(durationIndex, durationEndIndex);

            int eventId = Integer.parseInt(_eventId);
            long startTime = Long.parseLong(_startTime);
            long duration = Long.parseLong(_duration);

            lastReferencedEvent = new EpgUtils.EpgEvent();
            lastReferencedEvent.id = eventId;
            lastReferencedEvent.startTime = startTime;
            lastReferencedEvent.duration = duration;
        } else if (line.startsWith("- short event")) {
            if (lastReferencedEvent != null) {
                int langIndex = line.indexOf("lang=") + "lang=".length();
                int langEndIndex = line.indexOf(" ", langIndex);

                String lang = line.substring(langIndex, langEndIndex);

                int titleIndex = line.indexOf('\'') + 1;
                int endOfTitleIndex = line.indexOf('\'', titleIndex + 1);
                String title = line.substring(titleIndex, endOfTitleIndex);

                int subtitleIndex = line.indexOf('\'', endOfTitleIndex + 1) + 1;
                String subtitle = line.substring(subtitleIndex, line.length() - 1);


                lastReferencedEvent.lang = lang;
                lastReferencedEvent.subtitle = subtitle.equalsIgnoreCase("(null)") ? null : subtitle;
                lastReferencedEvent.title = title;
            }
        } else {
            if (lastReferencedEvent != null) {
                ChannelUtils.Channel ch = ChannelUtils.getChannelByServiceId(context, lastReferencedServiceIdForEvents);
                if (ch != null) {
                    EpgUtils.addEvent(context, ch.number, lastReferencedEvent);
                    //Log.d("ADDEVENTWAT", ch.number + " " + ch.title + " / " + lastReferencedEvent.title + " - " + lastReferencedEvent.subtitle + " (" + lastReferencedEvent.startTime + " " + lastReferencedEvent.duration + ")");
                } else {
                    lastReferencedServiceIdForEvents = -1;
                }
                lastReferencedEvent = null;
            }
        }
    }

    public void stopLogcatRead() {
        if (asyncLogcatReader != null) {
            asyncLogcatReader.stop = true;
            asyncLogcatReader = null;
        }
    }
}
