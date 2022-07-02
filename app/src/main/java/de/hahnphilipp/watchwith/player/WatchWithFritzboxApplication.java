package de.hahnphilipp.watchwith.player;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import java.io.IOException;

import de.hahnphilipp.watchwith.R;
import de.hahnphilipp.watchwith.async.FetchHbbTVSources;
import de.hahnphilipp.watchwith.webserver.TVWebServer;

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

                if(fetchHbbTVSources.response != null) {
                    editor.putString("hbbtvSources", fetchHbbTVSources.response.toString());
                    editor.commit();
                }
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
