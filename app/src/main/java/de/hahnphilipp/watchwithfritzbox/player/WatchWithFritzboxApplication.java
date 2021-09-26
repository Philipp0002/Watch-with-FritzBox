package de.hahnphilipp.watchwithfritzbox.player;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import de.hahnphilipp.watchwithfritzbox.R;
import de.hahnphilipp.watchwithfritzbox.async.FetchHbbTVSources;

public class WatchWithFritzboxApplication extends Application {

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
    }

}
