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
import java.util.Arrays;
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
     * @param pmtPids PIDs for reading PMT (null if not yet known)
     */
    public void tune(Integer frequency, List<Integer> pmtPids) {
        Log.d("SETUP_SEARCH", "Tuning frequency " + frequency + " with PMT-PIDs " + pmtPids);
        List<Integer> pids = new ArrayList<>(List.of(0,16,17,18,20));
        if(pmtPids != null) pids.addAll(pmtPids);
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

        //args.add("--http-reconnect");
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
                    List<Integer> pmtPids = new ArrayList<>();
                    for (int i = 0; i < pat.getPatPids().length; i++) {
                        MediaPlayer.PatPid patPid = pat.getPatPids()[i];
                        if(patPid.getNumber() != 0) {
                            pmtPids.add(patPid.getPid());
                        }
                    }
                    pmtFound = true;

                    int freqToTune = startFrequency;
                    if(currentNitTransportStreamIndex != -1 && nit != null) {
                        MediaPlayer.NitTransportStream nts = nit.getNitTransportStreams()[currentNitTransportStreamIndex];
                        if(nts.getCable() != null) {
                            double freqMhz = decodeFrequency((int) nts.getCable().getFrequency().longValue());
                            freqToTune = roundToGrid(freqMhz);
                        }
                    }

                    tune(freqToTune, pmtPids);
                });

                break;
            case MediaPlayer.Event.NitReceived:
                AsyncTask.execute(() -> {
                    if(nit != null) return;
                    Log.d("SETUP_NIT", event.getRecordPath());
                    nit = event.getNit();
                    Log.d("SETUP_NIT", nit.getNitTransportStreams().length + " Transport streams");

                    for(MediaPlayer.NitTransportStream nts : nit.getNitTransportStreams()) {
                        Log.d("SETUP_NIT", "======================");
                        Log.d("SETUP_NIT", "TS freq " + nts.getCable().getFrequency() + " mod " + nts.getCable().getModulation());
                        Log.d("SETUP_NIT", Arrays.toString(nts.getServices()));
                    }

                    currentNitTransportStreamIndex = 0;
                    MediaPlayer.NitTransportStream nts = nit.getNitTransportStreams()[currentNitTransportStreamIndex];
                    if(nts.getCable() == null) {
                        // TODO Show error -> NOT CABLE TV!! (maybe satellite, terrestrial)
                        return;
                    }
                    double freqMhz = decodeFrequency((int) nts.getCable().getFrequency().longValue());
                    int freqGrid = roundToGrid(freqMhz);
                    String mod = decodeModulation(nts.getCable().getModulation());
                    tune(freqGrid, null);
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

    /**
     * Dekodiert BCD-kodierte Frequenz aus DVB-C NIT Cable Delivery Descriptor
     *
     * @param freqBcd BCD-kodierte Frequenz als Integer (in 10 Hz Einheiten)
     * @return Frequenz in MHz
     */
    public static double decodeFrequency(int freqBcd) {
        // Bytes extrahieren und umkehren (falls Little Endian)
        int byte0 = (freqBcd >> 24) & 0xFF;
        int byte1 = (freqBcd >> 16) & 0xFF;
        int byte2 = (freqBcd >> 8) & 0xFF;
        int byte3 = freqBcd & 0xFF;

        // BCD dekodieren - jedes Byte hat 2 Dezimalziffern
        long bcdValue = 0;
        bcdValue += bcdFromByte(byte0) * 1_000_000L;
        bcdValue += bcdFromByte(byte1) * 10_000L;
        bcdValue += bcdFromByte(byte2) * 100L;
        bcdValue += bcdFromByte(byte3);

        // In 10 kHz Einheiten → MHz
        return bcdValue / 10000.0;
    }

    private static int bcdFromByte(int b) {
        int high = (b >> 4) & 0x0F;
        int low = b & 0x0F;
        return high * 10 + low;
    }

    /**
     * Rundet Frequenz auf typisches Vodafone DVB-C Raster
     *
     * @param freqMhz Frequenz in MHz
     * @return Gerundete Frequenz auf Raster
     */
    public static int roundToGrid(double freqMhz) {
        // S21-S41: 346-610 MHz, 8 MHz Raster
        if (freqMhz >= 346 && freqMhz <= 610) {
            int base = 346;
            int offset = (int) Math.round((freqMhz - base) / 8.0) * 8;
            return base + offset;
        }

        // S10-S20: 210-330 MHz, 10 MHz Raster
        else if (freqMhz >= 210 && freqMhz <= 330) {
            return (int) Math.round(freqMhz / 10.0) * 10;
        }

        // S02-S09: 121-177 MHz, 8 MHz Raster
        else if (freqMhz >= 121 && freqMhz <= 177) {
            int base = 121;
            int offset = (int) Math.round((freqMhz - base) / 8.0) * 8;
            return base + offset;
        }

        // Sonderkanal
        else if (freqMhz >= 110 && freqMhz <= 120) {
            return 113;
        }

        // Außerhalb bekannter Bereiche
        return (int) Math.round(freqMhz);
    }

    /**
     * Dekodiert Modulation aus Cable Delivery Descriptor
     *
     * @param modValue Modulations-Wert (0-5)
     * @return Modulations-String (z.B. "256qam")
     */
    public static String decodeModulation(long modValue) {
        switch ((int) modValue) {
            case 0: return "undefined";
            case 1: return "16qam";
            case 2: return "32qam";
            case 3: return "64qam";
            case 4: return "128qam";
            case 5: return "256qam";
            default: return "unknown";
        }
    }
}
