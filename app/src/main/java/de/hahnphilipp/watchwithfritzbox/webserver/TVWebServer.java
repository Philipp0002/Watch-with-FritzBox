package de.hahnphilipp.watchwithfritzbox.webserver;

import android.content.Context;

import com.koushikdutta.async.http.server.AsyncHttpServer;

import java.io.IOException;
import java.io.InputStream;

import de.hahnphilipp.watchwithfritzbox.player.ChannelListTVOverlay;
import de.hahnphilipp.watchwithfritzbox.player.EditChannelListTVOverlay;
import de.hahnphilipp.watchwithfritzbox.utils.AssetUtils;
import de.hahnphilipp.watchwithfritzbox.utils.ChannelUtils;

public class TVWebServer {

    AsyncHttpServer server;
    Context context;

    public TVWebServer(Context context) throws IOException {
        this.context = context;
        server = new AsyncHttpServer();

        start();
    }

    public void start() {
        server.get("/", (request, response) -> {
            try {
                String reorderChannelsView = AssetUtils.getStringFromAsset(context, "index.html");
                response.send(reorderChannelsView);
                return;
            } catch (IOException e) {
                e.printStackTrace();
            }
            response.send("There was an error");
        });

        server.get("/reorder", (request, response) -> {
            try {
                String reorderChannelsView = AssetUtils.getStringFromAsset(context, "reorderChannelsView.html");
                String channelItemView = AssetUtils.getStringFromAsset(context, "channelItem.html");
                String channels = "";
                for (ChannelUtils.Channel channel : ChannelUtils.getAllChannels(context)) {
                    channels += channelItemView
                            .replace("%CHANNELNAME%", channel.title)
                            .replace("%CHANNELNUMBER%", Integer.toString(channel.number))
                            .replace("%CHANNELBADGE%", channel.type.toString())
                            .replace("%LOGOURL%", "https://tv.avm.de/tvapp/logos/" + (channel.type == ChannelUtils.ChannelType.HD ? "hd/" : (channel.type == ChannelUtils.ChannelType.RADIO ? "radio/" : "")) + channel.title.toLowerCase().replace(" ", "_").replace("+", "") + ".png");
                }
                reorderChannelsView = reorderChannelsView.replace("%CHANNELS%", channels);
                response.send(reorderChannelsView);
                return;
            } catch (IOException e) {
                e.printStackTrace();
            }
            response.send("There was an error");
        });


        server.get("/asset", (request, response) -> {
            String assetString = request.getQuery().getString("name");
            String contentType = request.getQuery().getString("contentType");
            try {
                InputStream fileStream = AssetUtils.getFileFromAsset(context, "web-assets/" + assetString);
                response.setContentType(contentType);
                response.sendStream(fileStream, fileStream.available());
                return;
            } catch (IOException e) {
                e.printStackTrace();
            }
            response.send("There was an error");
        });

        server.get("/moveChannel", (request, response) -> {
            String fromString = request.getQuery().getString("from");
            String toString = request.getQuery().getString("to");

            if (fromString == null || toString == null) {
                response.send("{\"msg\": \"'from' or 'to' parameters not defined\"}");
                return;
            }

            int from = Integer.parseInt(fromString);
            int to = Integer.parseInt(toString);

            ChannelUtils.moveChannelToPosition(context, from, to);

            response.send("{\"msg\": \"success\"}");
            EditChannelListTVOverlay.notifyChannelListChanged();
            ChannelListTVOverlay.notifyChannelListChanged();
        });

        // listen on port 8080
        server.listen(8080);
    }

    public void stop() {
        server.stop();
    }

}