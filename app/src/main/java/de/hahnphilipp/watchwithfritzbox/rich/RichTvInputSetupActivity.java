package de.hahnphilipp.watchwithfritzbox.rich;

import android.app.Activity;
import android.content.ComponentName;
import android.media.tv.TvInputInfo;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.media.tv.companionlibrary.EpgSyncJobService;

import java.util.Timer;
import java.util.TimerTask;

import de.hahnphilipp.watchwithfritzbox.R;

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
