package de.hahnphilipp.watchwith.rich;

import android.media.tv.TvContract;
import android.net.Uri;
import android.util.Log;

import com.google.android.media.tv.companionlibrary.EpgSyncJobService;
import com.google.android.media.tv.companionlibrary.model.Channel;
import com.google.android.media.tv.companionlibrary.model.InternalProviderData;
import com.google.android.media.tv.companionlibrary.model.Program;
import com.google.android.media.tv.companionlibrary.utils.TvContractUtils;

import java.util.ArrayList;
import java.util.List;

import de.hahnphilipp.watchwith.utils.ChannelUtils;

public class RichJobService extends EpgSyncJobService {
    @Override
    public List<Channel> getChannels() {
        ArrayList<Channel> channels = new ArrayList<Channel>();
        ArrayList<ChannelUtils.Channel> channelsApp = ChannelUtils.getAllChannels(getApplicationContext());
        Log.d("RichJobServiceaa", "START");
        for(ChannelUtils.Channel channelApp : channelsApp){

            InternalProviderData internalProviderData = new InternalProviderData();
            internalProviderData.setVideoUrl(channelApp.url);
            internalProviderData.setVideoType(TvContractUtils.SOURCE_TYPE_MPEG_DASH);
            channels.add(new Channel.Builder()
                    .setDisplayName(channelApp.title)
                    .setDisplayNumber(channelApp.number+"")
                    .setServiceType((channelApp.type == ChannelUtils.ChannelType.RADIO ? TvContract.Channels.SERVICE_TYPE_AUDIO : TvContract.Channels.SERVICE_TYPE_AUDIO_VIDEO))
                    .setType(TvContract.Channels.TYPE_DVB_C2)
                    .setChannelLogo("https://tv.avm.de/tvapp/logos/"+(channelApp.type == ChannelUtils.ChannelType.HD ? "hd/": (channelApp.type == ChannelUtils.ChannelType.RADIO ? "radio/" : ""))+channelApp.title.toLowerCase().replace(" ", "_").replace("+", "")+".png")
                    .setOriginalNetworkId(channelApp.number)
                    .setInternalProviderData(internalProviderData)
                    .build() );
            Log.d("RichJobService", channelApp.title + " LOAD");
        }
        return channels;
    }

    @Override
    public List<Program> getProgramsForChannel(Uri channelUri, Channel channel, long startMs, long endMs) {
        List<Program> programsTears = new ArrayList<>();
        /*InternalProviderData internalProviderData = new InternalProviderData();
        internalProviderData.setVideoUrl(channel.getInternalProviderData().getVideoUrl());
        programsTears.add(new Program.Builder()
                .setTitle("-")
                .setStartTimeUtcMillis(startMs)
                .setEndTimeUtcMillis(endMs)
                .setDescription("-")
                .setInternalProviderData(internalProviderData)
                .build());*/
        return new ArrayList<Program>();
    }
}
