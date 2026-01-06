package de.hahnphilipp.watchwithfritzbox.player;

import android.app.Application;

import com.bumptech.glide.Glide;
import com.jakewharton.threetenabp.AndroidThreeTen;

import java.io.IOException;

import de.hahnphilipp.watchwithfritzbox.utils.EpgUtils;
import de.hahnphilipp.watchwithfritzbox.webserver.NSDService;
import de.hahnphilipp.watchwithfritzbox.webserver.TVWebServer;

public class WatchWithFritzboxApplication extends Application {

    public TVWebServer webServer = null;

    @Override
    public void onCreate() {
        super.onCreate();
        AndroidThreeTen.init(this);

        NSDService.createInstance(this);
        NSDService.getInstance().registerService(8081);
        NSDService.getInstance().discoverServices();

        EpgUtils.runEpgNowUpdateScheduler(this);

        try {
            webServer = new TVWebServer(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        if (webServer != null) {
            webServer.stop();
        }
        NSDService.getInstance().unregisterService();
    }
}
