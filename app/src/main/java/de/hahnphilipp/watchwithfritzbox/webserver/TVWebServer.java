package de.hahnphilipp.watchwithfritzbox.webserver;

import android.content.Context;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.w3ma.m3u8parser.data.Playlist;
import com.w3ma.m3u8parser.data.Track;
import com.w3ma.m3u8parser.exception.PlaylistParseException;
import com.w3ma.m3u8parser.parser.M3U8Parser;
import com.w3ma.m3u8parser.scanner.M3U8ItemScanner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import de.hahnphilipp.watchwithfritzbox.player.ChannelListTVOverlay;
import de.hahnphilipp.watchwithfritzbox.player.EditChannelListTVOverlay;
import de.hahnphilipp.watchwithfritzbox.utils.AssetUtils;
import de.hahnphilipp.watchwithfritzbox.utils.ChannelUtils;
import de.hahnphilipp.watchwithfritzbox.utils.EpgUtils;
import de.hahnphilipp.watchwithfritzbox.utils.WLog;

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

        server.get("/channels", (request, response) -> {
            try {
                List<ChannelItem> channels = new ArrayList<>(ChannelUtils.getAllChannels(context)).stream()
                        .map(ChannelItem::fromChannel)
                        .toList();

                String channelsJson = new ObjectMapper().writeValueAsString(channels);
                response.setContentType("application/json");
                response.send(channelsJson);
                return;
            } catch (IOException e) {
                e.printStackTrace();
            }
            response.send("There was an error");
        });

        server.get("/reorder", (request, response) -> {
            try {
                String reorderChannelsView = AssetUtils.getStringFromAsset(context, "reorderChannelsView.html");
                response.send(reorderChannelsView);
                return;
            } catch (IOException e) {
                e.printStackTrace();
            }
            response.send("There was an error");
        });

        server.get("/logs", (request, response) -> {
            try {
                if(request.getQuery().containsKey("raw")) {
                    response.setContentType("text/plain");
                    response.send(WLog.getLog());
                    return;
                }
                String logView = AssetUtils.getStringFromAsset(context, "logsView.html");
                logView = logView.replace("%LOGS%", WLog.getLog());
                response.send(logView);
                return;
            } catch (IOException e) {
                e.printStackTrace();
            }
            response.send("There was an error");
        });

        server.get("/downloadChannelList", (request, response) -> {
            String channels = ChannelUtils.getAllChannelsM3U(context);

            response.setContentType("audio/x-mpegurl");
            response.getHeaders().add("Content-Disposition", "attachment; filename=channelList.m3u");
            response.send(channels);
        });

        server.post("/uploadChannelList", (request, response) -> {
            String channelListString = (String) request.getBody().get();
            M3U8Parser m3U8ParserSD = new M3U8Parser(new ByteArrayInputStream(channelListString.getBytes(StandardCharsets.UTF_8)), M3U8ItemScanner.Encoding.UTF_8);
            try {
                Playlist playlist = m3U8ParserSD.parse();
                int channelNumber = 1;
                ArrayList<ChannelUtils.Channel> channels = new ArrayList<>();
                for (Track t : playlist.getTrackSetMap().get("")) {
                    ChannelUtils.ChannelType type = ChannelUtils.ChannelType.SD;
                    boolean free = true;
                    String wwfbType = t.getExtInfo().getWwfbType();
                    if (wwfbType != null) {
                        try{
                            type = ChannelUtils.ChannelType.valueOf(wwfbType);
                        } catch (IllegalArgumentException ignored) {
                        }
                    }

                    String wwfbFree = t.getExtInfo().getWwfbFree();
                    if (wwfbFree != null && !wwfbFree.isEmpty()) {
                        free = Boolean.parseBoolean(wwfbFree);
                    }
                    ChannelUtils.Channel channel = new ChannelUtils.Channel(channelNumber, t.getExtInfo().getTitle(), t.getUrl(), type);
                    channel.free = free;
                    channels.add(channel);
                    channelNumber++;
                }
                ChannelUtils.setChannels(context, channels, false);
                EpgUtils.resetEpgDatabase(context);
                response.send("{\"msg\": \"success\"}");
                EditChannelListTVOverlay.notifyChannelListChanged();
                ChannelListTVOverlay.notifyChannelListChanged();
                return;
            } catch (IOException | ParseException | PlaylistParseException e) {
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
            try {
                String fromString = request.getQuery().getString("from");
                String toString = request.getQuery().getString("to");

                if (fromString == null || toString == null) {
                    response.send("{\"msg\": \"'from' or 'to' parameters not defined\"}");
                    return;
                }

                int from = Integer.parseInt(fromString);
                int to = Integer.parseInt(toString);

                ChannelUtils.moveChannelToPosition(context, from, to);

                List<ChannelItem> channels = new ArrayList<>(ChannelUtils.getAllChannels(context)).stream()
                        .map(ChannelItem::fromChannel)
                        .toList();

                String channelsJson = new ObjectMapper().writeValueAsString(channels);
                response.setContentType("application/json");
                response.send(channelsJson);
                EditChannelListTVOverlay.notifyChannelListChanged();
                ChannelListTVOverlay.notifyChannelListChanged();
            } catch (IOException e) {
                e.printStackTrace();
            }
            response.send("There was an error");
        });

        // listen on port 8080
        server.listen(8080);
    }

    public void stop() {
        server.stop();
    }

    static class ChannelItem {
        public String name;
        public int number;
        public ChannelUtils.ChannelType type;
        public boolean free;
        public String logoUrl;

        public ChannelItem(String name, int number, ChannelUtils.ChannelType type, boolean free, String logoUrl) {
            this.name = name;
            this.number = number;
            this.type = type;
            this.free = free;
            this.logoUrl = logoUrl;
        }

        public static ChannelItem fromChannel(ChannelUtils.Channel channel) {
            return new ChannelItem(channel.title, channel.number, channel.type, channel.free, ChannelUtils.getIconURL(channel));
        }
    }

}