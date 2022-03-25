package de.hahnphilipp.watchwithfritzbox.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Objects;

import de.hahnphilipp.watchwithfritzbox.R;

public class ChannelUtils {

    static int selectedChannel = -1;

    public static Channel getNextChannel(Context context, int number){
        for(Channel ch : getAllChannels(context)){
            if(ch.number == number+1){
                return ch;
            }
        }
        return getChannelByNumber(context, 1);
    }

    public static ArrayList<Channel> moveChannelToPosition(Context context, int fromChannelPos, int toChannelPos){
        ArrayList<Channel> channels = getAllChannels(context);
        return moveChannelToPosition(context, fromChannelPos, toChannelPos, channels);
    }

    public static ArrayList<Channel> moveChannelToPosition(Context context, int fromChannelPos, int toChannelPos, ArrayList<Channel> channels){
        if(fromChannelPos == toChannelPos) return channels;

        Channel toMove = null;

        for(Channel channel : channels){
            if(channel.number == fromChannelPos){
                toMove = channel;
                break;
            }
        }

        if(getLastSelectedChannel(context) == toMove.number){
            updateLastSelectedChannel(context, toChannelPos);
        }

        if(toMove != null) {
            channels.remove(toMove);
            channels.add(toChannelPos-1, toMove);

            int i = 1;
            for(Channel channel : channels){
                channel.number = i;
                i++;
            }


            SharedPreferences sp = context.getSharedPreferences(
                    context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);

            SharedPreferences.Editor editor = sp.edit();

            Type channelListType = new TypeToken<ArrayList<ChannelUtils.Channel>>() {
            }.getType();
            String channelsJson = new Gson().toJson(channels, channelListType);
            editor.putString("channels", channelsJson);
            editor.commit();
        }
        return channels;

    }

    public static Channel getPreviousChannel(Context context, int number){
        Channel highest = null;
        for(Channel ch : getAllChannels(context)){
            if(highest == null || highest.number < ch.number){
                highest = ch;
            }
            if(ch.number == number-1){
                return ch;
            }
        }
        return highest;
    }

    public static int getLastSelectedChannel(Context context){
        SharedPreferences sp = context.getSharedPreferences(
                context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);

        if(selectedChannel == -1){
            return sp.getInt("lastChannel", 1);
        }else{
            return selectedChannel;
        }
    }
    public static void updateLastSelectedChannel(Context context, int number){
        SharedPreferences sp = context.getSharedPreferences(
                context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = sp.edit();
        editor.putInt("lastChannel", number);
        editor.commit();
    }

    public static Channel getChannelByNumber(Context context, int number){
        for(Channel ch : getAllChannels(context)){
            if(ch.number == number){
                return ch;
            }
        }
        return null;
    }

    public static ArrayList<Channel> getAllChannels(Context context){
        SharedPreferences sp = context.getSharedPreferences(
                context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);

        Type channelListType = new TypeToken<ArrayList<Channel>>(){}.getType();

        ArrayList<Channel> channels = new Gson().fromJson(sp.getString("channels", "[]"), channelListType);
        Collections.sort(channels, new Comparator<Channel>() {
            @Override
            public int compare(Channel o1, Channel o2) {
                return o1.number - o2.number;
            }
        });
        return channels;
    }

    public static ArrayList<HbbTV> getHbbTvFromChannel(Context context, int number){
        SharedPreferences sp = context.getSharedPreferences(
                context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);

        Channel channel = getChannelByNumber(context, number);

        ArrayList<HbbTV> hbbTVList = new ArrayList<HbbTV>();
        JsonArray array = new JsonParser().parse(sp.getString("hbbtvSources", "[]")).getAsJsonArray();
        for(JsonElement element : array){
            JsonObject groupObj = element.getAsJsonObject();
            z: for(JsonElement a: groupObj.get("channels").getAsJsonArray()){
                if(channel.title.toLowerCase().equalsIgnoreCase(a.getAsString().toLowerCase())){

                    for(JsonElement b : groupObj.get("urls").getAsJsonArray()){
                        JsonObject jsonUrlObj = b.getAsJsonObject();
                        hbbTVList.add(new HbbTV(jsonUrlObj.get("title").getAsString(), jsonUrlObj.get("url").getAsString()));
                    }

                    break z;
                }
            }
        }
        return hbbTVList;
    }


    public static class Channel {
        public int number;
        public String title;
        public String url;
        public ChannelType type;

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
            return Objects.equals(title, channel.title) && Objects.equals(url, channel.url) && type == channel.type;
        }

        @Override
        public int hashCode() {
            return Objects.hash(title, url, type);
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
