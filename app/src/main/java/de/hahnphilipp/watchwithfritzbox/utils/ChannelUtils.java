package de.hahnphilipp.watchwithfritzbox.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;

import de.hahnphilipp.watchwithfritzbox.R;

public class ChannelUtils {

    static int selectedChannel = -1;

    public static void setChannels(Context context, ArrayList<ChannelUtils.Channel> channels) {
        SharedPreferences sp = context.getSharedPreferences(
                context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = sp.edit();
        Type channelListType = new TypeToken<ArrayList<ChannelUtils.Channel>>() {
        }.getType();
        String channelsJson = new Gson().toJson(channels, channelListType);
        editor.putString("channels", channelsJson);
        editor.commit();
    }

    public static void updateChannel(Context context, Channel oldChannel, Channel newChannel){
        ArrayList<Channel> channels = getAllChannels(context);
        channels.remove(oldChannel);
        channels.add(newChannel);
        setChannels(context, channels);
    }

    public static Channel getNextChannel(Context context, int number) {
        for (Channel ch : getAllChannels(context)) {
            if (ch.number == number + 1) {
                return ch;
            }
        }
        return getChannelByNumber(context, 1);
    }

    public static ArrayList<Channel> moveChannelToPosition(Context context, int fromChannelPos, int toChannelPos) {
        ArrayList<Channel> channels = getAllChannels(context);
        return moveChannelToPosition(context, fromChannelPos, toChannelPos, channels);
    }

    public static ArrayList<Channel> moveChannelToPosition(Context context, int fromChannelPos, int toChannelPos, ArrayList<Channel> channels) {
        if (fromChannelPos == toChannelPos) return channels;

        Channel toMove = null;

        for (Channel channel : channels) {
            if (channel.number == fromChannelPos) {
                toMove = channel;
                break;
            }
        }

        if (getLastSelectedChannel(context) == toMove.number) {
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
        }
        return channels;

    }

    public static Channel getPreviousChannel(Context context, int number) {
        Channel highest = null;
        for (Channel ch : getAllChannels(context)) {
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
        for (Channel ch : getAllChannels(context)) {
            if (ch.number == number) {
                return ch;
            }
        }
        return null;
    }

    public static Channel getChannelByTitle(Context context, String title) {
        for (Channel ch : getAllChannels(context)) {
            if (ch.title.equalsIgnoreCase(title)) {
                return ch;
            }
        }
        return null;
    }

    public static Channel getChannelByServiceId(Context context, int serviceId) {
        for (Channel ch : getAllChannels(context)) {
            if (ch.serviceId == serviceId) {
                return ch;
            }
        }
        return null;
    }

    public static ArrayList<Channel> getAllChannels(Context context) {
        SharedPreferences sp = context.getSharedPreferences(
                context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);

        Type channelListType = new TypeToken<ArrayList<Channel>>() {
        }.getType();

        ArrayList<Channel> channels = new Gson().fromJson(sp.getString("channels", "[]"), channelListType);
        Collections.sort(channels, Comparator.comparingInt(o -> o.number));
        return channels;
    }

    public static String getAllChannelsM3U(Context context) {
        ArrayList<Channel> channels = getAllChannels(context);

        StringBuilder m3u = new StringBuilder("#EXTM3U\n");
        for (Channel channel : channels) {
            m3u.append("#EXTINF:0,").append(channel.title).append("\n");
            m3u.append("#EXTVLCOPT:network-caching=1000\n");
            m3u.append(channel.url).append("\n");
        }

        return m3u.toString();
    }

    public static String getIconURL(Channel channel) {
        try {
            return "https://tv.avm.de/tvapp/logos/" +
                    (channel.type == ChannelType.HD ? "hd/" : (channel.type == ChannelType.RADIO ? "radio/" : "")) +
                    URLEncoder.encode(channel.title.toLowerCase().replace(" ", "_").replace("+", ""), "UTF-8") +
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
        Type channelListType = new TypeToken<Map<Long, Integer>>() {}.getType();
        String channelsMapping = new Gson().toJson(channelIdToNumber, channelListType);
        editor.putString("channelMappingRichTv", channelsMapping);
        editor.commit();
    }

    public static Map<Long, Integer> getChannelIDMappingForRichTv(Context context) {
        SharedPreferences sp = context.getSharedPreferences(
                context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);

        Type channelListType = new TypeToken<Map<Long, Integer>>() {
        }.getType();

        Map<Long, Integer> channels = new Gson().fromJson(sp.getString("channelMappingRichTv", "[]"), channelListType);
        return channels;
    }


    public static class Channel {
        public int number;
        public String title;
        public String url;
        public ChannelType type;

        public int serviceId;
        public String provider;

        public Channel(int number, String title, String url, ChannelType type) {
            this.number = number;
            this.title = title;
            this.url = url;
            this.type = type;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Channel channel = (Channel) o;
            return Objects.equals(title, channel.title)
                    && Objects.equals(url, channel.url)
                    && type == channel.type
                    && serviceId == serviceId
                    && Objects.equals(provider, channel.provider);
        }

        @Override
        public int hashCode() {
            return Objects.hash(title, url, type, serviceId, provider);
        }

        public Channel copy() {
            Channel copy = new Channel(number, title, url, type);
            copy.provider = provider;
            copy.serviceId = serviceId;
            return copy;
        }
    }

    public enum ChannelType {
        SD, HD, RADIO;
    }

    public static class HbbTV {
        public String title;
        public String url;

        public HbbTV(String title, String url) {
            this.title = title;
            this.url = url;
        }
    }
}
