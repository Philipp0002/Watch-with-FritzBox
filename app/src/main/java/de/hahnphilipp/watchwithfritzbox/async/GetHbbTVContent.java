package de.hahnphilipp.watchwithfritzbox.async;

import android.os.AsyncTask;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;


public class GetHbbTVContent extends AsyncTask<Void, Void, Void> {

    public String hbbTvUrl;
    public Runnable futureRunFinished;

    public Response response = null;
    public ResponseBody responseBody = null;
    public String responseBodyString = null;
    public String responseMimeType = null;

    @Override
    protected Void doInBackground(Void... voids) {

        runFetch();
        if(futureRunFinished != null)
            futureRunFinished.run();

        return null;
    }

    public void runFetch() {
        OkHttpClient okHttpClient = new OkHttpClient.Builder().build();
        Request request = new Request.Builder()
                .url(hbbTvUrl)
                .build();
        try {
            response = okHttpClient.newCall(request).execute();
            responseBody = response.body();
            responseBodyString = responseBody.string();
            responseMimeType = responseBody.contentType().toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
