package de.hahnphilipp.watchwithfritzbox.rich;

import android.app.Activity;
import android.media.tv.TvContract;
import android.net.Uri;
import android.os.Bundle;

import androidx.tvprovider.media.tv.Channel;

import java.util.ArrayList;
import java.util.HashMap;

import de.hahnphilipp.watchwithfritzbox.R;
import de.hahnphilipp.watchwithfritzbox.utils.ChannelUtils;

public class RichTvInputSetupActivity extends Activity /*implements SyncStatusBroadcastReceiver.SyncListener */{

    //private SyncStatusBroadcastReceiver mSyncStatusChangedReceiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rich_tv_input_setup);

        getApplicationContext().getContentResolver().delete(TvContract.Channels.CONTENT_URI, null, null);

        ArrayList<ChannelUtils.Channel> channelsApp = ChannelUtils.getAllChannels(getApplicationContext());
        HashMap<Long, Integer> channelRichMap = new HashMap<>();
        for(ChannelUtils.Channel channelApp : channelsApp) {

            Channel channel = new Channel.Builder()
                    .setDisplayName(channelApp.title)
                    .setDisplayNumber(channelApp.number + "")
                    .setInputId("de.hahnphilipp.watchwithfritzbox/.rich.RichTvInputService")
                    .build();

            Uri uri = getApplicationContext().getContentResolver().insert(TvContract.Channels.CONTENT_URI, channel.toContentValues());

            long channelId = Long.parseLong(uri.getLastPathSegment());
            channelRichMap.put(channelId, channelApp.number);
            ChannelUtils.saveChannelIDMappingForRichTv(getApplicationContext(), channelRichMap);

        }
    }


}
