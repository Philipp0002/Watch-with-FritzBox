package de.hahnphilipp.watchwith.rich;

import android.app.Activity;
import android.content.ComponentName;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.media.tv.companionlibrary.EpgSyncJobService;
import com.google.android.media.tv.companionlibrary.model.Channel;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import de.hahnphilipp.watchwith.R;
import de.hahnphilipp.watchwith.utils.ChannelUtils;

public class RichTvInputSetupActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.richtvinputsetupactivity);

        try {
            String inputId = getIntent().getStringExtra(TvInputInfo.EXTRA_INPUT_ID);
            EpgSyncJobService.cancelAllSyncRequests(this);
            EpgSyncJobService.requestImmediateSync(this, inputId,
                    new ComponentName(this, RichJobService.class));
        }catch(Exception e){
            Toast.makeText(this, R.string.setup_richtv_error, Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }catch(Error e){
            Toast.makeText(this, R.string.setup_richtv_error, Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                finish();
            }
        }, 5000);
    }


}
