package de.hahnphilipp.watchwithfritzbox.async;

import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;


public class AsyncLogcatReader extends AsyncTask<Void, Void, Void> {

    public LogcatCallback logcatCallback;
    public boolean stop = false;

    public AsyncLogcatReader(LogcatCallback logcatCallback){
        this.logcatCallback = logcatCallback;
    }


    @Override
    protected Void doInBackground(Void... voids) {

        runRead();

        return null;
    }

    public void runRead() {
        try {
            Process process = Runtime.getRuntime().exec("logcat");
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));

            String line = "";
            while ((line = bufferedReader.readLine()) != null && !stop) {
                logcatCallback.onLineRead(line);
            }
        }
        catch (IOException e) {}
    }

    public static abstract class LogcatCallback {
        public abstract void onLineRead(String line);
    }

}
