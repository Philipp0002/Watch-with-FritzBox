package de.hahnphilipp.watchwithfritzbox.rich;

import android.app.Activity;
import android.content.ComponentName;
import android.content.IntentFilter;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

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
        setContentView(R.layout.richtvinputsetupactivity);

        //String inputId = getIntent().getStringExtra(TvInputInfo.EXTRA_INPUT_ID);
        /*Log.d("INPUUUTID", inputId + "");
        mSyncStatusChangedReceiver = new SyncStatusBroadcastReceiver(inputId, this);
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(
                        mSyncStatusChangedReceiver,
                        new IntentFilter(EpgSyncJobService.ACTION_SYNC_STATUS_CHANGED));

        try {
            EpgSyncJobService.cancelAllSyncRequests(this);
            EpgSyncJobService.requestImmediateSync(this, inputId,
                    new ComponentName(this, RichJobService.class));
        }catch(Exception e){
            Toast.makeText(this, R.string.setup_richtv_error, Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }catch(Error e){
            Toast.makeText(this, R.string.setup_richtv_error, Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }*/

        getApplicationContext().getContentResolver().delete(TvContract.Channels.CONTENT_URI, null, null);

        ArrayList<ChannelUtils.Channel> channelsApp = ChannelUtils.getAllChannels(getApplicationContext());
        Log.d("RichJobServiceaa", "START");
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


    /*@Override
    public void onScanStepCompleted(int completedStep, int totalSteps) {
        Log.d("SCANNED STEP", completedStep + " / " + totalSteps);
    }

    @Override
    public void onScannedChannel(CharSequence displayName, CharSequence displayNumber) {
        Log.d("SCANNED CHANNEL", displayName + " " + displayNumber);
    }

    @Override
    public void onScanFinished() {
        RichTvInputSetupActivity.this.setResult(Activity.RESULT_OK);
        finish();
    }

    @Override
    public void onScanError(int errorCode) {
        Log.d("SCANNED ERROR", errorCode + "");
        RichTvInputSetupActivity.this.setResult(Activity.RESULT_CANCELED);
        finish();
    }*/
}
