package de.hahnphilipp.watchwithfritzbox.setup;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import de.hahnphilipp.watchwithfritzbox.R;
import de.hahnphilipp.watchwithfritzbox.utils.ChannelUtils;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class NewSetupSearchActivity extends AppCompatActivity implements MediaPlayer.EventListener {

    private static final int baseFrequency = 308; // Rasterfrequenz Basis
    private static final int stepFrequency = 8; // Rasterfrequenz Schritte
    private static final int startFrequency = 330;
    private String ip;
    private List<ChannelUtils.Channel> channelList;

    private LibVLC mLibVLC = null;
    public MediaPlayer mMediaPlayer = null;
    public Media media;

    private boolean pmtFound = false;
    private MediaPlayer.Nit nit = null;
    private int currentNitTransportStreamIndex = -1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_search);

        ip = getIntent().getStringExtra("ip");

        loadLibVLC();
        tune(startFrequency, null);
    }

    /**
     * Tune VLC to frequency
     * @param pmtPid PID for reading PMT (null if not yet known)
     */
    public void tune(Integer frequency, Integer pmtPid) {
        Log.d("SETUP_SEARCH", "Tuning frequency " + frequency + " with PMT-PID " + pmtPid);
        List<Integer> pids = new ArrayList<>(List.of(0,16,17,18,20));
        if(pmtPid != null) pids.add(pmtPid);
        String uri = "rtsp://" + ip + ":554/?avm=1&freq=" + frequency + "&bw=8&msys=dvbc&mtype=256qam&sr=6900&specinv=1&pids=" + pids.stream().map(i -> i + "").collect(Collectors.joining(","));

        mMediaPlayer.stop();
        if (media != null && !media.isReleased()) {
            media.release();
        }
        media = new Media(mLibVLC, Uri.parse(uri));
        mMediaPlayer.setMedia(media);
        mMediaPlayer.play();
    }

    public void loadLibVLC() {
        final ArrayList<String> args = new ArrayList<>();
        args.add("-vvvvv");

        args.add("--http-reconnect");
        args.add("--sout-keep");
        args.add("--vout=none");
        args.add("--aout=none");

        mLibVLC = new LibVLC(this, args);
        mMediaPlayer = new MediaPlayer(mLibVLC);
        mMediaPlayer.setEventListener(this);
    }

    public void presortChannels() {
        findViewById(R.id.setup_order_no_button).setVisibility(View.INVISIBLE);
        findViewById(R.id.setup_order_yes_button).setVisibility(View.INVISIBLE);
        findViewById(R.id.setup_order_progressBar).setVisibility(View.VISIBLE);

        OkHttpClient client = new OkHttpClient.Builder()
                .followRedirects(true)
                .followSslRedirects(true)
                .build();
        Request request = new Request.Builder()
                .url("https://hahnphilipp.de/watchwithfritzbox/presetOrder.json")
                .build();

        // OkHttp Request
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(NewSetupSearchActivity.this, R.string.setup_order_error, Toast.LENGTH_LONG).show();

                    findViewById(R.id.setup_order_no_button).setVisibility(View.VISIBLE);
                    findViewById(R.id.setup_order_yes_button).setVisibility(View.VISIBLE);
                    findViewById(R.id.setup_order_progressBar).setVisibility(View.GONE);
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                ArrayList<ChannelUtils.Channel> channelsList = ChannelUtils.getAllChannels(NewSetupSearchActivity.this);
                int position = 0;

                ObjectMapper objectMapper = new ObjectMapper();
                TypeReference<List<List<String>>> serialType = new TypeReference<>() {};
                List<List<String>> responseBody = objectMapper.readValue(response.body().string(), serialType);
                for (List<String> channelNames : responseBody) {
                    for (String channelName : channelNames) {
                        Optional<ChannelUtils.Channel> channelToMove = channelsList
                                .stream()
                                .filter(channel -> channel.title.equalsIgnoreCase(channelName))
                                .findFirst();

                        if (channelToMove.isPresent()) {
                            position++;
                            channelsList = ChannelUtils.moveChannelToPosition(NewSetupSearchActivity.this, channelToMove.get().number, position);
                            break;
                        }
                    }
                }

                skipToNext();
            }
        });
    }

    public void skipToNext() {
        startActivity(new Intent(NewSetupSearchActivity.this, ShowcaseGesturesActivity.class));
        finish();
        overridePendingTransition(0, 0);
    }

    @Override
    public void onEvent(MediaPlayer.Event event) {
        switch (event.type) {
            case MediaPlayer.Event.PatReceived:
                AsyncTask.execute(() -> {
                    Log.d("SETUP_PAT", event.getRecordPath());
                    if (pmtFound) return;
                    MediaPlayer.Pat pat = event.getPat();
                    int lowestPatPid = Integer.MAX_VALUE;
                    for (int i = 0; i < pat.getPatPids().length; i++) {
                        MediaPlayer.PatPid patPid = pat.getPatPids()[i];
                        if (patPid.getNumber() != 0 && lowestPatPid > patPid.getPid()) {
                            lowestPatPid = patPid.getPid();
                        }
                    }
                    if (lowestPatPid != Integer.MAX_VALUE) {
                        pmtFound = true;
                        tune(currentNitTransportStreamIndex != -1 ? currentNitTransportStreamIndex : startFrequency, lowestPatPid);
                    }
                });

                break;
            case MediaPlayer.Event.NitReceived:
                AsyncTask.execute(() -> {
                    if(nit != null) return;
                    Log.d("SETUP_NIT", event.getRecordPath());
                    nit = event.getNit();
                    Log.d("SETUP_NIT", nit.getNitTransportStreams().length + " Transport streams");

                    currentNitTransportStreamIndex = 0;
                    MediaPlayer.NitTransportStream nts = nit.getNitTransportStreams()[currentNitTransportStreamIndex];
                    if(nts.getCable() == null) {
                        // TODO Show error -> NOT CABLE TV!! (maybe satellite, terrestrial)
                        return;
                    }
                    tune(getRasteredFrequency(nts.getCable().getFrequency()), null);
                    pmtFound = false;

                });
                break;
            case MediaPlayer.Event.EpgNewServiceInfo:
                AsyncTask.execute(() -> {
                    //MediaPlayer.ServiceInfo serviceInfo = event.getServiceInfo();
                    Log.d("SETUP_EPGNEWSERVICE", event.getRecordPath());
                    /*ChannelUtils.Channel originalChannel = ChannelUtils.getChannelByTitle(TVPlayerActivity.this, serviceInfo.getName());
                    if (originalChannel != null) {
                        ChannelUtils.Channel channel = originalChannel.copy();
                        channel.serviceId = Math.toIntExact(serviceInfo.getServiceId());
                        channel.provider = serviceInfo.getProvider();
                        channel.free = serviceInfo.isFreeCa();
                        try {
                            switch (Math.toIntExact(serviceInfo.getTypeId())) {
                                case 1:
                                    channel.type = ChannelUtils.ChannelType.SD;
                                    break;
                                case 25:
                                    channel.type = ChannelUtils.ChannelType.HD;
                                    break;
                                case 2:
                                    channel.type = ChannelUtils.ChannelType.RADIO;
                                    break;
                            }
                        } catch (Exception unused) {
                        }
                        ChannelUtils.updateChannel(TVPlayerActivity.this, originalChannel, channel);
                    }*/
                });

                break;
            default:
                break;
        }
    }

    private int getRasteredFrequency(long dvbFrequency) {
        float correctedDvbFrequency = (float) dvbFrequency / 100_000;
        return Math.round((correctedDvbFrequency - baseFrequency) / stepFrequency) * stepFrequency + baseFrequency;
    }
}
