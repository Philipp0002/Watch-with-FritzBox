package de.hahnphilipp.watchwithfritzbox.epg;

import android.content.Context;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.hahnphilipp.watchwithfritzbox.async.AsyncLogcatReader;
import de.hahnphilipp.watchwithfritzbox.utils.ChannelUtils;
import de.hahnphilipp.watchwithfritzbox.utils.EpgUtils;

public class LogcatEpgReader {

    Context context;
    AsyncLogcatReader asyncLogcatReader;

    EpgUtils.EpgEvent lastReferencedEvent;

    int lastReferencedServiceId = -1;
    boolean lastReferencedServiceFree = true;
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
        if (line.startsWith("* service ")) {
            line = line.replace("eit ", "");
            Map<String, String> map = parseKeyValue(line.substring(10));
            String serviceId = map.get("id");
            boolean freeChannel = map.get("id").equals("0");
            lastReferencedServiceId = Integer.parseInt(serviceId);
            lastReferencedServiceFree = freeChannel;
        } else if (line.startsWith("- type=")) {
            Map<String, String> map = parseKeyValue(line.substring(2));
            String provider = map.get("provider");
            String name = map.get("name");
            String type = map.get("type");

            ChannelUtils.Channel ch = ChannelUtils.getChannelByTitle(context, name);
            if (ch == null) return;
            ChannelUtils.Channel originalCh = ch.copy();
            if (lastReferencedServiceId != -1) {
                ch.serviceId = lastReferencedServiceId;
                ch.provider = provider;
                ch.free = lastReferencedServiceFree;
                switch (type) {
                    case "1":
                        ch.type = ChannelUtils.ChannelType.SD;
                        break;
                    case "25":
                        ch.type = ChannelUtils.ChannelType.HD;
                        break;
                    case "2":
                        ch.type = ChannelUtils.ChannelType.RADIO;
                        break;
                }
                ChannelUtils.updateChannel(context, originalCh, ch);
            }
            lastReferencedServiceId = -1;
        } else if (line.startsWith("new EIT ")) {
            Map<String, String> map = parseKeyValue(line.substring(8));
            String serviceId = map.get("service_id");
            lastReferencedServiceIdForEvents = Integer.parseInt(serviceId);
        } else if (line.startsWith("* event ")) {
            Map<String, String> map = parseKeyValue(line.substring(8));

            String _eventId = map.get("id");
            String _startTime = map.get("start_time");
            String _duration = map.get("duration");

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

    private Map<String, String> parseKeyValue(String input) {
        Map<String, String> map = new LinkedHashMap<>();
        Pattern pattern = Pattern.compile("(\\w+)[:=]([^:=]+?)(?=\\s+\\w+[:=]|$)");
        Matcher matcher = pattern.matcher(input);

        while (matcher.find()) {
            String key = matcher.group(1);
            String value = matcher.group(2).trim();
            map.put(key, value);
        }
        return map;
    }
}
