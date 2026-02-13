package de.hahnphilipp.watchwithfritzbox.rich;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;

import de.hahnphilipp.watchwithfritzbox.R;
import de.hahnphilipp.watchwithfritzbox.utils.EpgUtils;

public class RichTvInputSetupActivity extends Activity /*implements SyncStatusBroadcastReceiver.SyncListener */{

    //private SyncStatusBroadcastReceiver mSyncStatusChangedReceiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rich_tv_input_setup);

        AsyncTask.execute(() -> {
            RichTvUtils.reinsertChannels(getApplicationContext());
            RichTvUtils.reinsertAllEpgEvents(getApplicationContext(), EpgUtils.getAllEvents(getApplicationContext()));

            runOnUiThread(this::finish);
        });

    }


}
