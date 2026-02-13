package de.hahnphilipp.watchwithfritzbox.rich;

import android.content.ContentResolver;
import android.content.Context;
import android.media.tv.TvContentRating;
import android.media.tv.TvContract;
import android.net.Uri;

import androidx.tvprovider.media.tv.Channel;
import androidx.tvprovider.media.tv.Program;
import androidx.tvprovider.media.tv.TvContractCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.hahnphilipp.watchwithfritzbox.utils.ChannelUtils;
import de.hahnphilipp.watchwithfritzbox.utils.EpgUtils;

public class RichTvUtils {

    public static void reinsertChannels(Context context) {
        context.getContentResolver().delete(TvContract.Channels.CONTENT_URI, null, null);

        ArrayList<ChannelUtils.Channel> channelsApp = ChannelUtils.getAllChannels(context);
        HashMap<Long, Integer> channelRichMap = new HashMap<>();
        for(ChannelUtils.Channel channelApp : channelsApp) {

            Channel channel = new Channel.Builder()
                    .setDisplayName(channelApp.title)
                    .setDisplayNumber(channelApp.number + "")
                    .setInputId("de.hahnphilipp.watchwithfritzbox/.rich.RichTvInputService")
                    .build();

            Uri uri = context.getContentResolver().insert(TvContract.Channels.CONTENT_URI, channel.toContentValues());

            long channelId = Long.parseLong(uri.getLastPathSegment());
            channelRichMap.put(channelId, channelApp.number);
            ChannelUtils.saveChannelIDMappingForRichTv(context, channelRichMap);
        }
    }

    public static void reinsertAllEpgEvents(Context context, List<EpgUtils.EpgEvent> epgEvent) {
        ContentResolver resolver = context.getContentResolver();

        // Alte Programme für diesen Kanal löschen (optional)
        resolver.delete(TvContract.Programs.CONTENT_URI, null, null);

        epgEvent.forEach(e -> insertEpgEvent(context, e));
    }

    public static void insertEpgEvent(Context context, EpgUtils.EpgEvent epgEvent) {
        Long richChannelID = ChannelUtils.getRichChannelID(context, epgEvent.channelNumber);

        if(richChannelID == null) {
            return;
        }
        insertEpgEventForRichChannel(context, richChannelID, epgEvent);
    }

    public static void insertEpgEventForRichChannel(Context context, long richChannelID, EpgUtils.EpgEvent epgEvent) {
        ContentResolver resolver = context.getContentResolver();

        try {
            String eventGenre = epgEvent.getRichTvGenre();
            @TvContractCompat.Programs.Genres.Genre
            String[] richGenre = eventGenre == null ? new String[0] : new String[]{eventGenre};
            Program program = new Program.Builder()
                    .setEventId((int) epgEvent.id)
                    .setId(epgEvent.id)
                    .setChannelId(richChannelID)
                    .setTitle(epgEvent.title)
                    .setDescription(epgEvent.description)
                    .setBroadcastGenres(richGenre)
                    .setStartTimeUtcMillis(epgEvent.startTime * 1000)
                    .setEndTimeUtcMillis((epgEvent.startTime + epgEvent.duration) * 1000)
                    .build();

            resolver.insert(TvContract.Programs.CONTENT_URI, program.toContentValues());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
