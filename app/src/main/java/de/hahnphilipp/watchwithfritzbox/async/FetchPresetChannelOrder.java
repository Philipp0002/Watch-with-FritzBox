package de.hahnphilipp.watchwithfritzbox.async;

import android.os.AsyncTask;


import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.charset.Charset;


public class FetchPresetChannelOrder extends AsyncTask<Void, Void, Void> {

    public JsonArray response;
    public Runnable futurerun;

    @Override
    protected Void doInBackground(Void... voids) {

        runFetch();
        if(futurerun != null)
            futurerun.run();

        return null;
    }

    public void runFetch() {
        JsonArray obj1 = null;
        try {
            obj1 = readJsonFromUrl("https://hahnphilipp.de/watchwithfritzbox/presetOrder.json");
        } catch (java.io.FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        if (obj1 != null){
            response = obj1;
        }else{
            response = new JsonArray();
        }
    }

    public static JsonArray readJsonFromUrl(String url) throws IOException {
        InputStream is = new URL(url).openStream();
        try {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            String jsonText = readAll(rd);
            JsonArray json = new JsonParser().parse(jsonText).getAsJsonArray();
            return json;
        } finally {
            is.close();
        }
    }

    private static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }
}

