package de.hahnphilipp.watchwithfritzbox.async;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.w3ma.m3u8parser.data.Playlist;
import com.w3ma.m3u8parser.exception.PlaylistParseException;
import com.w3ma.m3u8parser.parser.M3U8Parser;
import com.w3ma.m3u8parser.scanner.M3U8ItemScanner;

import java.io.IOException;
import java.net.URL;
import java.text.ParseException;

import de.hahnphilipp.watchwithfritzbox.webserver.TVWebServer;


public class StartWebServer extends AsyncTask<Void, Void, Void> {

    public Runnable futureRunFinished;
    public Context context;
    public TVWebServer webServer;

    public boolean error = false;

    @Override
    protected Void doInBackground(Void... voids) {

        runFetch();
        if(futureRunFinished != null)
            futureRunFinished.run();

        return null;
    }

    public void runFetch() {
        try {
            webServer = new TVWebServer(context);
        } catch (IOException e) {
            error = true;
            e.printStackTrace();
        }
    }

}
