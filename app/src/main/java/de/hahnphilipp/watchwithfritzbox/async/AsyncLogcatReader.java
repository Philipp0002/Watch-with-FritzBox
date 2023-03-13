package de.hahnphilipp.watchwithfritzbox.async;

import android.os.AsyncTask;

import com.w3ma.m3u8parser.data.Playlist;
import com.w3ma.m3u8parser.exception.PlaylistParseException;
import com.w3ma.m3u8parser.parser.M3U8Parser;
import com.w3ma.m3u8parser.scanner.M3U8ItemScanner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.ParseException;


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
