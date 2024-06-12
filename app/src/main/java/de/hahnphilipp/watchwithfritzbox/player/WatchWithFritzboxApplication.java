package de.hahnphilipp.watchwithfritzbox.player;

import android.app.Application;

import com.jakewharton.threetenabp.AndroidThreeTen;

import java.io.IOException;

import de.hahnphilipp.watchwithfritzbox.webserver.TVWebServer;

public class WatchWithFritzboxApplication extends Application {

    public TVWebServer webServer = null;

    @Override
    public void onCreate() {
        super.onCreate();
        AndroidThreeTen.init(this);

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
    }
}
