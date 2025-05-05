package de.hahnphilipp.watchwithfritzbox.async;

import android.os.AsyncTask;

import com.w3ma.m3u8parser.data.Playlist;
import com.w3ma.m3u8parser.exception.PlaylistParseException;
import com.w3ma.m3u8parser.parser.M3U8Parser;
import com.w3ma.m3u8parser.scanner.M3U8ItemScanner;

import java.io.IOException;
import java.net.URL;
import java.text.ParseException;


public class GetPlaylists extends AsyncTask<Void, Void, Void> {

    public String ip;
    public Runnable futureRunSD;
    public Runnable futureRunHD;
    public Runnable futureRunRadio;
    public Runnable futureRunFinished;

    public Playlist playlistSD = null;
    public Playlist playlistHD = null;
    public Playlist playlistRadio = null;

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
            URL sdURL = new URL("http://"+ip+"/dvb/m3u/tvsd.m3u");
            M3U8Parser m3U8ParserSD = new M3U8Parser(sdURL.openStream(), M3U8ItemScanner.Encoding.UTF_8);
            playlistSD = m3U8ParserSD.parse();
            if(!playlistSD.getTrackSetMap().containsKey("")){
                error = true;
                return;
            }
            if(futureRunSD != null)
                futureRunSD.run();

            URL hdURL = new URL("http://"+ip+"/dvb/m3u/tvhd.m3u");
            M3U8Parser m3U8ParserHD = new M3U8Parser(hdURL.openStream(), M3U8ItemScanner.Encoding.UTF_8);
            playlistHD = m3U8ParserHD.parse();
            if(!playlistHD.getTrackSetMap().containsKey("")){
                error = true;
                return;
            }
            if(futureRunHD != null)
                futureRunHD.run();

            URL radioURL = new URL("http://"+ip+"/dvb/m3u/radio.m3u");
            M3U8Parser m3U8ParserRadio = new M3U8Parser(radioURL.openStream(), M3U8ItemScanner.Encoding.UTF_8);
            playlistRadio = m3U8ParserRadio.parse();
            if(!playlistRadio.getTrackSetMap().containsKey("")){
                error = true;
                return;
            }

            if(futureRunRadio != null)
                futureRunRadio.run();
        } catch (Exception e) {
            e.printStackTrace();
            error = true;
        }
    }

}
