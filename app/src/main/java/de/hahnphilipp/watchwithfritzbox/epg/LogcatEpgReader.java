package de.hahnphilipp.watchwithfritzbox.epg;

import android.content.Context;
import android.util.Log;

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
    ChannelUtils.Channel lastReferencedChannel;
    HashMap<Integer, Integer> serviceIdToChannelNr;
    boolean lastEpgDescNotFinished = false;

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

                if (lineUntrimmed.contains("D VLC     :")) {
                    if (lineUntrimmed.contains("ts demux:")) {
                        String line = lineUntrimmed
                                .substring(
                                        lineUntrimmed
                                                .indexOf("ts demux:") +
                                                "ts demux:".length()
                                ).trim();
                        processLine(line, true);
                    } else {
                        String line = lineUntrimmed
                                .substring(
                                        lineUntrimmed
                                                .indexOf("D VLC     :") +
                                                "D VLC     :".length()
                                ).trim();
                        processLine(line, false);
                    }
                }
            }
        });
        asyncLogcatReader.execute();
    }

    private void processLine(String line, boolean isTsDemux) {
        try {
            if (isTsDemux && line.startsWith("* service ")) {
                line = line.replace("eit ", "");
                Map<String, String> map = parseKeyValue(line.substring(10));
                String serviceId = map.get("id");
                boolean freeChannel = map.get("id").equals("0");
                lastReferencedServiceId = Integer.parseInt(serviceId);
                lastReferencedServiceFree = freeChannel;
                lastReferencedEvent = null;
                lastEpgDescNotFinished = false;
            } else if (isTsDemux && line.startsWith("- type=")) {
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
                lastReferencedEvent = null;
                lastEpgDescNotFinished = false;
            } else if (isTsDemux && line.startsWith("new EIT ")) {
                Map<String, String> map = parseKeyValue(line.substring(8));
                String serviceId = map.get("service_id");
                lastReferencedChannel = ChannelUtils.getChannelByServiceId(context, Integer.parseInt(serviceId));
                lastReferencedEvent = null;
                lastEpgDescNotFinished = false;
            } else if (isTsDemux && line.startsWith("* event ") && isTsDemux) {
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
                lastEpgDescNotFinished = false;
            } else if (isTsDemux && line.startsWith("- short event")) {
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
                    lastEpgDescNotFinished = false;
                }
            } else if (isTsDemux && line.startsWith("- text='")) {
                if (lastReferencedEvent != null) {
                    int descIndex = line.indexOf("text='") + "text='".length();
                    int descEndIndex = line.charAt(line.length() - 1) == '\'' ? line.length() - 1 : line.length();


                    String desc = line.substring(descIndex, descEndIndex);

                    if (lastReferencedEvent.description == null) {
                        lastReferencedEvent.description = "";
                    }
                    lastReferencedEvent.description += desc;
                    if (!line.endsWith("'")) {
                        lastEpgDescNotFinished = true;
                    }
                }
            } else if (!isTsDemux && !line.trim().isEmpty() && lastEpgDescNotFinished) {
                if (lastReferencedEvent != null && lastReferencedEvent.description != null) {
                    int descIndex = 0;
                    int descEndIndex = line.charAt(line.length() - 1) == '\'' ? line.length() - 1 : line.length();

                    String desc = line.substring(descIndex, descEndIndex);

                    lastReferencedEvent.description += "\n" + desc;
                    if (line.endsWith("'")) {
                        lastEpgDescNotFinished = false;
                    }
                }
            }
            saveLastReferencedEvent();
        } catch (Exception e) {
            Log.e("LogcatEpgReader", "Error parsing line: " + line);
            e.printStackTrace();
        }
    }

    public void stopLogcatRead() {
        if (asyncLogcatReader != null) {
            asyncLogcatReader.stop = true;
            asyncLogcatReader = null;
        }
    }

    public void saveLastReferencedEvent() {
        if (lastReferencedEvent != null && lastReferencedChannel != null) {
            lastReferencedEvent.eitReceivedTimeMillis = System.currentTimeMillis();
            EpgUtils.addEvent(context, lastReferencedChannel.number, lastReferencedEvent);
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
