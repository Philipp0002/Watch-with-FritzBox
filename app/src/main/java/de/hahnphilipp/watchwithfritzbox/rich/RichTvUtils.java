package de.hahnphilipp.watchwithfritzbox.rich;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentValues;
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
        try {
            context.getContentResolver().delete(TvContract.Channels.CONTENT_URI, null, null);

            ArrayList<ChannelUtils.Channel> channelsApp = ChannelUtils.getAllChannels(context);
            HashMap<Long, Integer> channelRichMap = new HashMap<>();
            ArrayList<ContentProviderOperation> operations = new ArrayList<>();

            for (ChannelUtils.Channel channelApp : channelsApp) {

                Channel channel = new Channel.Builder()
                        .setDisplayName(channelApp.title)
                        .setDisplayNumber(channelApp.number + "")
                        .setInputId("de.hahnphilipp.watchwithfritzbox/.rich.RichTvInputService")
                        .build();

                operations.add(ContentProviderOperation.newInsert(TvContract.Channels.CONTENT_URI)
                        .withValues(channel.toContentValues())
                        .build());
            }
            ContentProviderResult[] results = context.getContentResolver().applyBatch(TvContract.AUTHORITY, operations);

            for (int i = 0; i < results.length; i++) {
                Uri uri = results[i].uri;
                long channelId = Long.parseLong(uri.getLastPathSegment());
                channelRichMap.put(channelId, channelsApp.get(i).number);
            }

            ChannelUtils.saveChannelIDMappingForRichTv(context, channelRichMap);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void reinsertAllEpgEvents(Context context, List<EpgUtils.EpgEvent> epgEvents) {
        ContentResolver resolver = context.getContentResolver();

        // Alte Programme für diesen Kanal löschen (optional)
        resolver.delete(TvContract.Programs.CONTENT_URI, null, null);

        ContentValues[] programs = new ContentValues[epgEvents.size()];
        for(int i = 0; i < epgEvents.size(); i++) {
            EpgUtils.EpgEvent e = epgEvents.get(i);
            Long richChannelID = ChannelUtils.getRichChannelID(context, e.channelNumber);
            if(richChannelID == null) {
                return;
            }

            Program program = programFromEpgEvent(e, richChannelID);

            programs[i] = program.toContentValues();
        }
        resolver.bulkInsert(TvContract.Programs.CONTENT_URI, programs);
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
            Program program = programFromEpgEvent(epgEvent, richChannelID);
            ContentValues values = program.toContentValues();

            // Prüfe auf existierendes Programm anhand von Channel-ID und Event-ID
            String selection = TvContract.Programs.COLUMN_CHANNEL_ID + " = ? AND " +
                    TvContract.Programs.COLUMN_EVENT_ID + " = ?";
            String[] selectionArgs = new String[] {
                    String.valueOf(richChannelID),
                    String.valueOf(epgEvent.id)
            };

            int rowsUpdated = resolver.update(
                    TvContract.Programs.CONTENT_URI,
                    values,
                    selection,
                    selectionArgs
            );

            // Falls kein Update stattgefunden hat, neuen Eintrag einfügen
            if (rowsUpdated == 0) {
                resolver.insert(TvContract.Programs.CONTENT_URI, values);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Program programFromEpgEvent(EpgUtils.EpgEvent epgEvent, long richChannelID) {
        String eventGenre = epgEvent.getRichTvGenre();
        @TvContractCompat.Programs.Genres.Genre
        String[] richGenre = eventGenre == null ? new String[0] : new String[]{eventGenre};
        return new Program.Builder()
                .setEventId((int) epgEvent.id)
                //.setId(epgEvent.id)
                .setChannelId(richChannelID)
                .setTitle(epgEvent.title)
                .setDescription(epgEvent.description)
                .setBroadcastGenres(richGenre)
                .setStartTimeUtcMillis(epgEvent.startTime * 1000)
                .setEndTimeUtcMillis((epgEvent.startTime + epgEvent.duration) * 1000)
                .build();
    }
}
