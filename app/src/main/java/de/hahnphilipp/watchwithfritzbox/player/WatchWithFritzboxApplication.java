package de.hahnphilipp.watchwithfritzbox.player;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import com.jakewharton.threetenabp.AndroidThreeTen;

import java.io.IOException;

import de.hahnphilipp.watchwithfritzbox.R;
import de.hahnphilipp.watchwithfritzbox.async.FetchHbbTVSources;
import de.hahnphilipp.watchwithfritzbox.webserver.TVWebServer;

public class WatchWithFritzboxApplication extends Application {

    public TVWebServer webServer = null;

    @Override
    public void onCreate() {
        super.onCreate();
        AndroidThreeTen.init(this);

        FetchHbbTVSources fetchHbbTVSources = new FetchHbbTVSources();
        fetchHbbTVSources.futurerun = () -> {
            SharedPreferences sp = getSharedPreferences(
                    getString(R.string.preference_file_key), Context.MODE_PRIVATE);

            SharedPreferences.Editor editor = sp.edit();

            if (fetchHbbTVSources.response != null) {
                editor.putString("hbbtvSources", fetchHbbTVSources.response.toString());
                editor.commit();
            }
        };
        fetchHbbTVSources.execute();

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
