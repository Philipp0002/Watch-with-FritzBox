package de.hahnphilipp.watchwith.webserver;

import android.content.Context;

import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import de.hahnphilipp.watchwith.player.ChannelListTVOverlay;
import de.hahnphilipp.watchwith.player.EditChannelListTVOverlay;
import de.hahnphilipp.watchwith.utils.AssetUtils;
import de.hahnphilipp.watchwith.utils.ChannelUtils;

public class TVWebServer {

    private Context context;

    public TVWebServer(Context context) throws IOException {
        this.context = context;
        AsyncHttpServer server = new AsyncHttpServer();

        server.get("/", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                try {
                    String reorderChannelsView = AssetUtils.getStringFromAsset(context, "index.html");
                    response.send(reorderChannelsView);
                    return;
                } catch (IOException e) {
                    e.printStackTrace();
                }
                response.send("There was an error");
            }
        });

        server.get("/reorder", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
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
            }
        });


        server.get("/asset", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                String assetString = request.getQuery().getString("name");
                String contentType = request.getQuery().getString("contentType");
                try {
                    InputStream fileStream = AssetUtils.getFileFromAsset(context, "web-assets/"+assetString);
                    response.setContentType(contentType);
                    response.sendStream(fileStream, fileStream.available());
                    return;
                } catch (IOException e) {
                    e.printStackTrace();
                }
                response.send("There was an error");
            }
        });

        server.get("/moveChannel", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                String fromString = request.getQuery().getString("from");
                String toString = request.getQuery().getString("to");

                if(fromString == null || toString == null){
                    response.send("{\"msg\": \"'from' or 'to' parameters not defined\"}");
                    return;
                }

                int from = Integer.parseInt(fromString);
                int to = Integer.parseInt(toString);

                ChannelUtils.moveChannelToPosition(context, from, to);

                //response.send("a: "+params.get(0)+" b: "+params.get(1));
                response.send("{\"msg\": \"success\"}");
                EditChannelListTVOverlay.notifyChannelListChanged();
                ChannelListTVOverlay.notifyChannelListChanged();
            }
        });

// listen on port 5000
        server.listen(8080);
// browsing http://localhost:5000 will return Hello!!!
    }

    private static List<String> getPathParamtersFromUrl(String url) {
        String[] path = url.split("\\?");
        String[] pathparams = path[0].split("\\//");
        String[] param = pathparams[1].split("\\/");
        List<String> p = Arrays.asList(param);
        return p.stream().filter(x -> !x.contains(".")).collect(Collectors.toList());
    }



}