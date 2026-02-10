package de.hahnphilipp.watchwithfritzbox.setup;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import de.hahnphilipp.watchwithfritzbox.R;
import de.hahnphilipp.watchwithfritzbox.utils.ChannelUtils;

public class NewSetupSearchActivity extends AppCompatActivity implements MediaPlayer.EventListener {

    private static final int startFrequency = 53477376; // 330 Mhz in BCD
    private int currentFrequencyBcd = startFrequency;
    private int currentFrequencyMhz;
    private Map<Integer, Integer> currentPids;
    private String currentModulation = "256qam";
    private String ip;
    private List<ChannelUtils.Channel> channelList;

    private LibVLC mLibVLC = null;
    public MediaPlayer mMediaPlayer = null;
    public Media media;

    private boolean pmtFound = false;
    private MediaPlayer.Nit nit = null;
    private int currentNitTransportStreamIndex = -1;
    private int channelNumber = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_setup_search);

        ip = getIntent().getStringExtra("ip");

        loadLibVLC();
        tune(startFrequency, null, currentModulation);
    }

    /**
     * Tune VLC to frequency
     *
     * @param pmtPids PIDs for reading PMT (null if not yet known)
     */
    public void tune(Integer frequencyBcd, Map<Integer, Integer> pmtPids, String modulation) {
        this.currentFrequencyBcd = frequencyBcd;
        this.currentModulation = modulation;
        double freqMhz = decodeFrequency(frequencyBcd);
        int frequency = roundToGrid(freqMhz);
        this.currentFrequencyMhz = frequency;

        Log.d("SETUP_SEARCH", "Tuning frequency " + frequency + " with PMT-PIDs " + pmtPids);
        List<Integer> pids = new ArrayList<>(List.of(0, 16, 17, 18, 20));
        if (pmtPids != null) pids.addAll(pmtPids.values());
        String uri = "rtsp://" + ip + ":554/?avm=1&freq=" + frequency + "&bw=8&msys=dvbc&mtype=" + modulation + "&sr=6900&specinv=1&pids=" + pids.stream().map(i -> i + "").collect(Collectors.joining(","));
        this.currentPids = pmtPids;

        mMediaPlayer.stop();
        if (media != null && !media.isReleased()) {
            media.release();
        }
        media = new Media(mLibVLC, Uri.parse(uri));
        mMediaPlayer.setMedia(media);
        mMediaPlayer.play();
    }

    public void tuneNext() {
        currentNitTransportStreamIndex++;
        MediaPlayer.NitTransportStream nts = nit.getNitTransportStreams()[currentNitTransportStreamIndex];
        if (nts.getCable() == null) {
            // TODO Show error -> NOT CABLE TV!! (maybe satellite, terrestrial)
            return;
        }
        String mod = decodeModulation(nts.getCable().getModulation());
        tune((int) nts.getCable().getFrequency().longValue(), null, mod);
        pmtFound = false;
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

    @Override
    public void onEvent(MediaPlayer.Event event) {
        switch (event.type) {
            case MediaPlayer.Event.PatReceived:
                AsyncTask.execute(() -> {
                    Log.d("SETUP_PAT", event.getRecordPath());
                    if (pmtFound) return;
                    MediaPlayer.Pat pat = event.getPat();
                    HashMap<Integer, Integer> pmtPids = new HashMap<>();
                    for (int i = 0; i < pat.getPatPids().length; i++) {
                        MediaPlayer.PatPid patPid = pat.getPatPids()[i];
                        if (patPid.getNumber() != 0) {
                            pmtPids.put(patPid.getNumber(), patPid.getPid());
                        }
                    }
                    pmtFound = true;

                    tune(currentFrequencyBcd, pmtPids, currentModulation);
                });

                break;
            case MediaPlayer.Event.NitReceived:
                AsyncTask.execute(() -> {
                    if (nit != null) return;
                    Log.d("SETUP_NIT", event.getRecordPath());
                    nit = event.getNit();

                    currentNitTransportStreamIndex = -1;
                    tuneNext();
                });
                break;
            case MediaPlayer.Event.EpgNewServiceInfoFinished:
                AsyncTask.execute(() -> {
                    Log.d("SETUP_SEARCH", (nit == null) + " nit is null?");
                    if (nit == null) return;
                    if (currentNitTransportStreamIndex + 1 < nit.getNitTransportStreams().length) {
                        tuneNext();
                    } else {
                        // Finished all frequencies
                        runOnUiThread(() -> {
                            skipToNext();
                        });
                    }
                });
                break;
            case MediaPlayer.Event.EpgNewServiceInfo:
                AsyncTask.execute(() -> {
                    MediaPlayer.ServiceInfo serviceInfo = event.getServiceInfo();
                    Log.d("SETUP_EPGNEWSERVICE", event.getRecordPath());
                    HashSet<Integer> pids = new HashSet<>(List.of(0, 16, 17, 18, 20));
                    if(currentPids.containsKey(serviceInfo.getServiceId())) pids.add(currentPids.get(serviceInfo.getServiceId()));
                    for (int i : serviceInfo.getPids()) pids.add(i);

                    ChannelUtils.Channel channel = new ChannelUtils.Channel();
                    channel.number = ++channelNumber;
                    channel.title = serviceInfo.getName();
                    channel.serviceId = Math.toIntExact(serviceInfo.getServiceId());
                    channel.provider = serviceInfo.getProvider();
                    channel.free = serviceInfo.isFreeCA() != null && serviceInfo.isFreeCA();
                    channel.url = "rtsp://" + ip + ":554/?avm=1&freq=" + currentFrequencyMhz + "&bw=8&msys=dvbc&mtype=" + currentModulation + "&sr=6900&specinv=1&pids=" + pids.stream().map(i -> i + "").collect(Collectors.joining(","));

                    try {
                        switch (Math.toIntExact(serviceInfo.getTypeId())) {
                            case 1: // DVB SD
                            case 22: // SKY SD
                                channel.type = ChannelUtils.ChannelType.SD;
                                break;
                            case 25: // DVB HD
                                channel.type = ChannelUtils.ChannelType.HD;
                                break;
                            case 2: // DVB Digital Radio
                            case 10: // DVB FM Radio
                                channel.type = ChannelUtils.ChannelType.RADIO;
                                break;
                            default:
                                channel.type = ChannelUtils.ChannelType.OTHER;
                        }
                    } catch (Exception unused) {
                    }
                    runOnUiThread(() -> {
                        ((TextView) findViewById(R.id.setup_search_channels)).append("\n" + channel.title + " (" + (channel.free ? "free" : "paytv") + ")");
                        ((ScrollView) findViewById(R.id.setup_search_channels_scroll)).fullScroll(View.FOCUS_DOWN);
                    });
                    ChannelUtils.updateChannel(NewSetupSearchActivity.this, null, channel);
                });

                break;
            default:
                break;
        }
    }

    public void skipToNext() {
        startActivity(new Intent(NewSetupSearchActivity.this, ShowcaseGesturesActivity.class));
        finish();
        overridePendingTransition(0, 0);
    }


    public static int encodeFrequency(double mhz) {
        // MHz → 10 kHz Einheiten
        long bcdValue = Math.round(mhz * 10_000);

        // Einzelne 2-stellige Dezimalblöcke extrahieren
        int part0 = (int) ((bcdValue / 1_000_000) % 100);
        int part1 = (int) ((bcdValue / 10_000) % 100);
        int part2 = (int) ((bcdValue / 100) % 100);
        int part3 = (int) (bcdValue % 100);

        int byte0 = bcdToByte(part0);
        int byte1 = bcdToByte(part1);
        int byte2 = bcdToByte(part2);
        int byte3 = bcdToByte(part3);

        // Zusammensetzen zu int
        return (byte0 << 24)
                | (byte1 << 16)
                | (byte2 << 8)
                | byte3;
    }

    private static int bcdToByte(int value) {
        int high = value / 10;
        int low = value % 10;
        return (high << 4) | low;
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
            case 0:
                return "undefined";
            case 1:
                return "16qam";
            case 2:
                return "32qam";
            case 3:
                return "64qam";
            case 4:
                return "128qam";
            case 5:
                return "256qam";
            default:
                return "unknown";
        }
    }
}
