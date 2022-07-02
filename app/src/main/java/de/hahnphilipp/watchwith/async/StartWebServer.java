package de.hahnphilipp.watchwith.async;

import android.content.Context;
import android.os.AsyncTask;

import java.io.IOException;

import de.hahnphilipp.watchwith.webserver.TVWebServer;


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
