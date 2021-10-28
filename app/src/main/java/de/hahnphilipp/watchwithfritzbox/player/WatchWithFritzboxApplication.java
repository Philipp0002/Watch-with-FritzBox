package de.hahnphilipp.watchwithfritzbox.player;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import java.io.IOException;

import de.hahnphilipp.watchwithfritzbox.R;
import de.hahnphilipp.watchwithfritzbox.async.FetchHbbTVSources;
import de.hahnphilipp.watchwithfritzbox.async.StartWebServer;
import de.hahnphilipp.watchwithfritzbox.webserver.TVWebServer;

public class WatchWithFritzboxApplication extends Application {

    TVWebServer webServer = null;

    @Override
    public void onCreate() {
        super.onCreate();

        FetchHbbTVSources fetchHbbTVSources = new FetchHbbTVSources();
        fetchHbbTVSources.futurerun = new Runnable() {
            @Override
            public void run() {
                SharedPreferences sp = getSharedPreferences(
                        getString(R.string.preference_file_key), Context.MODE_PRIVATE);

                SharedPreferences.Editor editor = sp.edit();

                editor.putString("hbbtvSources", fetchHbbTVSources.response.toString());
                editor.commit();
            }
        };
        fetchHbbTVSources.execute();

        /*StartWebServer sws = new StartWebServer();
        sws.webServer = webServer;
        sws.context = getApplicationContext();
        sws.futureRunFinished = new Runnable() {
            @Override
            public void run() {
                if(!sws.error){
                    webServer = sws.webServer;
                }
            }
        };
        sws.execute();*/

        try {
            webServer = new TVWebServer(this);
            webServer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
