package de.hahnphilipp.watchwithfritzbox.async;

import android.os.AsyncTask;

import com.w3ma.m3u8parser.data.Playlist;
import com.w3ma.m3u8parser.data.Track;
import com.w3ma.m3u8parser.parser.M3U8Parser;
import com.w3ma.m3u8parser.scanner.M3U8ItemScanner;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import de.hahnphilipp.watchwithfritzbox.utils.ChannelUtils;

public class GetPlaylists extends AsyncTask<Void, Void, Void> {

    public String ip;
    public GetPlaylistResult callback;

    public GetPlaylists(String ip) {
        this.ip = ip;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        runFetch();
        return null;
    }

    public void runFetch() {
        try {
            URL sdURL = new URL("http://"+ip+"/dvb/m3u/tvsd.m3u");
            M3U8Parser m3U8ParserSD = new M3U8Parser(sdURL.openStream(), M3U8ItemScanner.Encoding.UTF_8);
            Playlist playlistSD = m3U8ParserSD.parse();
            if(!playlistSD.getTrackSetMap().containsKey("")){
                throw new RuntimeException("Couldn't fetch sd channels");
            }
            if(callback != null)
                callback.onTypeLoaded(ChannelUtils.ChannelType.SD, playlistSD.getTrackSetMap().get("").size());

            URL hdURL = new URL("http://"+ip+"/dvb/m3u/tvhd.m3u");
            M3U8Parser m3U8ParserHD = new M3U8Parser(hdURL.openStream(), M3U8ItemScanner.Encoding.UTF_8);
            Playlist playlistHD = m3U8ParserHD.parse();
            if(!playlistHD.getTrackSetMap().containsKey("")){
                throw new RuntimeException("Couldn't fetch hd channels");
            }
            if(callback != null)
                callback.onTypeLoaded(ChannelUtils.ChannelType.HD, playlistHD.getTrackSetMap().get("").size());

            URL radioURL = new URL("http://"+ip+"/dvb/m3u/radio.m3u");
            M3U8Parser m3U8ParserRadio = new M3U8Parser(radioURL.openStream(), M3U8ItemScanner.Encoding.UTF_8);
            Playlist playlistRadio = m3U8ParserRadio.parse();
            if(!playlistRadio.getTrackSetMap().containsKey("")){
                throw new RuntimeException("Couldn't fetch radio channels");
            }
            if(callback != null) {
                callback.onTypeLoaded(ChannelUtils.ChannelType.RADIO, playlistRadio.getTrackSetMap().get("").size());

                int channelNumber = 1;

                ArrayList<ChannelUtils.Channel> channels = new ArrayList<>();
                for (Track t : playlistHD.getTrackSetMap().get("")) {
                    ChannelUtils.Channel channel = new ChannelUtils.Channel(channelNumber, t.getExtInfo().getTitle(), replaceHost(t.getUrl(), ip), ChannelUtils.ChannelType.HD);
                    channels.add(channel);
                    channelNumber++;
                }

                for (Track t : playlistSD.getTrackSetMap().get("")) {
                    ChannelUtils.Channel channel = new ChannelUtils.Channel(channelNumber, t.getExtInfo().getTitle(), replaceHost(t.getUrl(), ip), ChannelUtils.ChannelType.SD);
                    channels.add(channel);
                    channelNumber++;
                }

                for (Track t : playlistRadio.getTrackSetMap().get("")) {
                    ChannelUtils.Channel channel = new ChannelUtils.Channel(channelNumber, t.getExtInfo().getTitle(), replaceHost(t.getUrl(), ip), ChannelUtils.ChannelType.RADIO);
                    channels.add(channel);
                    channelNumber++;
                }


                callback.onAllLoaded(false, channels);
            }
        } catch (Exception e) {
            if(callback != null)
                callback.onAllLoaded(true, null);
            e.printStackTrace();
        }
    }

    public static String replaceHost(String url, String newHost) {
        try {
            java.net.URI uri = new java.net.URI(url);

            java.net.URI newUri = new java.net.URI(
                    uri.getScheme(),
                    uri.getUserInfo(),
                    newHost,
                    uri.getPort(),
                    uri.getPath(),
                    uri.getQuery(),
                    uri.getFragment()
            );

            return newUri.toString();
        } catch (java.net.URISyntaxException e) {
            e.printStackTrace();
            return url;
        }
    }

    public interface GetPlaylistResult {
        void onTypeLoaded(ChannelUtils.ChannelType type, int channelAmount);
        void onAllLoaded(boolean error, List<ChannelUtils.Channel> channelList);
    }


}
