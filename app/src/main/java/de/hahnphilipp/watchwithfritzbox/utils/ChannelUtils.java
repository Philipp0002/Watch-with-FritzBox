package de.hahnphilipp.watchwithfritzbox.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import de.hahnphilipp.watchwithfritzbox.R;

public class ChannelUtils {

    static int selectedChannel = -1;
    static ArrayList<Channel> channelsCache = null;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void setChannels(Context context, List<Channel> channels) {
        Collections.sort(channels, Comparator.comparingInt(o -> o.number));
        channelsCache = new ArrayList<>(channels);
        SharedPreferences sp = context.getSharedPreferences(
                context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = sp.edit();
        String channelsJson;
        try {
            channelsJson = objectMapper.writeValueAsString(channels);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return;
        }
        editor.putString("channels", channelsJson);
        editor.apply();
    }

    public static void updateChannel(Context context, Channel oldChannel, Channel newChannel){
        ArrayList<Channel> channels = getAllChannels(context);
        channels.remove(oldChannel);
        channels.add(newChannel);
        setChannels(context, channels);
    }

    public static Channel getNextChannel(Context context, int number) {
        for (Channel ch : new ArrayList<>(getAllChannels(context))) {
            if (ch.number == number + 1) {
                return ch;
            }
        }
        return getChannelByNumber(context, 1);
    }

    public static ArrayList<Channel> moveChannelToPosition(Context context, int fromChannelPos, int toChannelPos) {
        ArrayList<Channel> channels = new ArrayList<>(getAllChannels(context));
        return moveChannelToPosition(context, fromChannelPos, toChannelPos, channels);
    }

    public static ArrayList<Channel> moveChannelToPosition(Context context, int fromChannelPos, int toChannelPos, ArrayList<Channel> channels) {
        if (fromChannelPos == toChannelPos) return channels;

        Channel toMove = channels.stream().filter(channel -> channel.number == fromChannelPos).findFirst().orElse(null);

        if (getLastSelectedChannel(context) == fromChannelPos) {
            updateLastSelectedChannel(context, toChannelPos);
        }

        if (toMove != null) {
            channels.remove(toMove);
            channels.add(toChannelPos - 1, toMove);

            int i = 1;
            for (Channel channel : channels) {
                channel.number = i;
                i++;
            }

            setChannels(context, channels);
            EpgUtils.swapChannelPositions(context, fromChannelPos, toChannelPos);
        }
        return channels;

    }

    public static Channel getPreviousChannel(Context context, int number) {
        Channel highest = null;
        for (Channel ch : new ArrayList<>(getAllChannels(context))) {
            if (highest == null || highest.number < ch.number) {
                highest = ch;
            }
            if (ch.number == number - 1) {
                return ch;
            }
        }
        return highest;
    }

    public static int getLastSelectedChannel(Context context) {
        SharedPreferences sp = context.getSharedPreferences(
                context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);

        if (selectedChannel == -1) {
            return sp.getInt("lastChannel", 1);
        } else {
            return selectedChannel;
        }
    }

    public static void updateLastSelectedChannel(Context context, int number) {
        SharedPreferences sp = context.getSharedPreferences(
                context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = sp.edit();
        editor.putInt("lastChannel", number);
        editor.commit();
    }

    public static Channel getChannelByNumber(Context context, int number) {
        for (Channel ch : new ArrayList<>(getAllChannels(context))) {
            if (ch.number == number) {
                return ch;
            }
        }
        return null;
    }

    public static Channel getChannelByTitle(Context context, String title) {
        return new ArrayList<>(getAllChannels(context))
                .stream()
                .filter(ch -> ch.title.equalsIgnoreCase(title))
                .findFirst()
                .orElse(null);
    }

    public static Channel getChannelByServiceId(Context context, int serviceId) {
        for (Channel ch : new ArrayList<>(getAllChannels(context))) {
            if (ch.serviceId == serviceId) {
                return ch;
            }
        }
        return null;
    }

    public static ArrayList<Channel> getAllChannels(Context context) {
        if(channelsCache != null) {
            return channelsCache;
        }
        SharedPreferences sp = context.getSharedPreferences(
                context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);

        TypeReference<ArrayList<Channel>> channelListType = new TypeReference<>() {};

        ArrayList<Channel> channels;
        try {
            channels = objectMapper.readValue(sp.getString("channels", "[]"), channelListType);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        Collections.sort(channels, Comparator.comparingInt(o -> o.number));
        return channels;
    }

    public static String getAllChannelsM3U(Context context) {
        ArrayList<Channel> channels = getAllChannels(context);

        StringBuilder m3u = new StringBuilder("#EXTM3U\n");
        for (Channel channel : channels) {
            m3u.append("#EXTINF:0 wwfb-type=\"").append(channel.type).append("\",").append(channel.title).append("\n");
            m3u.append("#EXTVLCOPT:network-caching=1000\n");
            //m3u.append("#EXTWWFB:"+ channel.type.toString() +"\n");
            m3u.append(channel.url).append("\n");
        }

        return m3u.toString();
    }

    public static String getIconURL(Channel channel) {
        try {
            return "https://tv.avm.de/tvapp/logos/" +
                    (channel.type == ChannelType.HD ? "hd/" : (channel.type == ChannelType.RADIO ? "radio/" : "")) +
                    URLEncoder.encode(channel.title.toLowerCase().replace("ü", "ue").replace("ä", "ae").replace("ö", "oe").replace(" ", "_").replace("+", ""), "UTF-8") +
                    ".png";
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static void saveChannelIDMappingForRichTv(Context context, Map<Long, Integer> channelIdToNumber){
        SharedPreferences sp = context.getSharedPreferences(
                context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = sp.edit();
        String channelsMapping = null;
        try {
            channelsMapping = objectMapper.writeValueAsString(channelIdToNumber);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return;
        }
        editor.putString("channelMappingRichTv", channelsMapping);
        editor.commit();
    }

    public static Map<Long, Integer> getChannelIDMappingForRichTv(Context context) {
        SharedPreferences sp = context.getSharedPreferences(
                context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);

        TypeReference<Map<Long, Integer>> channelListType = new TypeReference<>() {};

        Map<Long, Integer> channels = null;
        try {
            channels = objectMapper.readValue(sp.getString("channelMappingRichTv", "[]"), channelListType);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return channels;
    }

    public static Channel getChannelByPids(Context context, int[] pids) {
        main: for (Channel ch : new ArrayList<>(getAllChannels(context))) {
            List<Integer> channelPids = ch.getPids();
            for(int pid : pids) {
                if (!channelPids.contains(pid)) {
                    continue main;
                }
            }
            return ch;
        }
        return null;
    }

    public static Channel getChannelByDvb(Context context, int onid, int tsid, int sid) {
        for (Channel ch : new ArrayList<>(getAllChannels(context))) {
            if(ch.onId == onid && ch.tsId == tsid && ch.serviceId == sid) {
                return ch;
            }
        }
        return null;
    }


    public static class Channel {
        public int number;
        public String title;
        public String url;
        public ChannelType type;

        public long onId;
        public long tsId;
        public int serviceId;

        public String provider;
        public boolean free;

        public Channel() {

        }

        public Channel(int number, String title, String url, ChannelType type) {
            this.number = number;
            this.title = title;
            this.url = url;
            this.type = type;
            this.free = true;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Channel channel = (Channel) o;
            return Objects.equals(title, channel.title)
                    && Objects.equals(url, channel.url)
                    && type == channel.type
                    && serviceId == channel.serviceId
                    && Objects.equals(provider, channel.provider);
        }

        @Override
        public int hashCode() {
            return Objects.hash(title, url, type, serviceId, provider);
        }

        @JsonIgnore
        public Channel copy() {
            Channel copy = new Channel(number, title, url, type);
            copy.provider = provider;
            copy.serviceId = serviceId;
            return copy;
        }

        @JsonIgnore
        public List<Integer> getPids() {
            try {
                URI urlObj = new URI(url);
                return Arrays.stream(urlObj.getQuery().split("&"))
                        .map(s -> s.split("=", 2))
                        .filter(o -> o.length == 2 && o[0].equalsIgnoreCase("pids"))
                        .flatMap(o -> Arrays.stream(o[1].split(",")))
                        .filter(Objects::nonNull)
                        .map(Integer::valueOf)
                        .collect(Collectors.toList());
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
            return List.of();
        }

    }

    public enum ChannelType {
        SD, HD, RADIO, OTHER;
    }

}
