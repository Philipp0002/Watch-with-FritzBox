package de.hahnphilipp.watchwithfritzbox.webserver;

import android.content.Context;

import java.io.IOException;
import java.util.Map;

import de.hahnphilipp.watchwithfritzbox.utils.AssetUtils;
import de.hahnphilipp.watchwithfritzbox.utils.ChannelUtils;
import fi.iki.elonen.NanoHTTPD;

public class TVWebServer extends NanoHTTPD {

    private Context context;

    public TVWebServer(Context context) throws IOException {
        super(8080);
        this.context = context;
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        System.out.println("\nRunning! Point your browsers to http://localhost:8080/ \n");
    }

    @Override
    public Response serve(IHTTPSession session) {
        try {
            String reorderChannelsView = AssetUtils.getStringFromAsset(context, "reorderChannelsView.html");
            String channels = "";
            for(ChannelUtils.Channel channel : ChannelUtils.getAllChannels(context)){
                channels += AssetUtils.getStringFromAsset(context, "channelItem.html")
                        .replace("%CHANNELNAME%", channel.title)
                        .replace("%CHANNELNUMBER%", Integer.toString(channel.number))
                        .replace("%CHANNELBADGE%", channel.type.toString())
                        .replace("%LOGOURL%", "https://tv.avm.de/tvapp/logos/"+(channel.type == ChannelUtils.ChannelType.HD ? "hd/": (channel.type == ChannelUtils.ChannelType.RADIO ? "radio/" : ""))+channel.title.toLowerCase().replace(" ", "_").replace("+", "")+".png");
            }
            //reorderChannelsView.replace("%CHANNELS%", channels);
            return newFixedLengthResponse(reorderChannelsView);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String msg = "<html><body><h1>Hello server</h1>\n";
        Map<String, String> parms = session.getParms();
        if (parms.get("username") == null) {
            msg += "<form action='?' method='get'>\n  <p>Your name: <input type='text' name='username'></p>\n" + "</form>\n";
        } else {
            msg += "<p>Hello, " + parms.get("username") + "!</p>";
        }
        return newFixedLengthResponse(msg + "</body></html>\n");
    }
}