package de.hahnphilipp.watchwithfritzbox.rich;

import android.app.Activity;
import android.content.ComponentName;
import android.media.tv.TvInputInfo;
import android.os.Bundle;
import android.util.Log;

import com.google.android.media.tv.companionlibrary.EpgSyncJobService;

import java.util.Timer;
import java.util.TimerTask;

import de.hahnphilipp.watchwithfritzbox.R;

public class RichTvInputSetupActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.richtvinputsetupactivity);
        Log.d("RichTvInputSetupAct", "START");
        /*if (null == savedInstanceState) {
            GuidedStepFragment.addAsRoot(this, new FirstStepFragment(), android.R.id.content);
        }*/
        String inputId = getIntent().getStringExtra(TvInputInfo.EXTRA_INPUT_ID);
        EpgSyncJobService.cancelAllSyncRequests(this);
        EpgSyncJobService.requestImmediateSync(this, inputId,
                new ComponentName(this, RichJobService.class));


        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                finish();
            }
        }, 5000);

    }


}
