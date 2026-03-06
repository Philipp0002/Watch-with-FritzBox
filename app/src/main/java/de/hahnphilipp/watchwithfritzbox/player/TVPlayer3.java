package de.hahnphilipp.watchwithfritzbox.player;

import android.os.Build;
import android.os.Bundle;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import de.hahnphilipp.watchwithfritzbox.R;

@RequiresApi(api = Build.VERSION_CODES.M)
public class TVPlayer3 extends AppCompatActivity implements SurfaceHolder.Callback {

    private static final String TAG          = "TVPlayer3";
    private static final int    RTP_PORT     = 50004;

    // ── UI ───────────────────────────────────────────────────────────────────
    private SurfaceView surfaceView;
    private EditText    etServerUrl;
    private Button      btnPlay, btnStop;
    private TextView    tvStatus;

    // ── State ────────────────────────────────────────────────────────────────
    private Surface             surface;
    private final AtomicBoolean isPlaying = new AtomicBoolean(false);
    private ExecutorService     executor;
    private Future<?>           decodeFuture;
    private final Handler       mainHandler = new Handler(Looper.getMainLooper());

    // ── Lifecycle ────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tvplayer3);

        surfaceView = findViewById(R.id.surfaceView);
        etServerUrl = findViewById(R.id.etServerUrl);
        btnPlay     = findViewById(R.id.btnPlay);
        btnStop     = findViewById(R.id.btnStop);
        tvStatus    = findViewById(R.id.tvStatus);

        surfaceView.getHolder().addCallback(this);
        executor = Executors.newSingleThreadExecutor();

        btnPlay.setOnClickListener(v -> startPlayback());
        btnStop.setOnClickListener(v -> stopPlayback());
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopPlayback();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }

    // ── SurfaceHolder.Callback ───────────────────────────────────────────────

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        surface = holder.getSurface();
        setStatus("Bereit");
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stopPlayback();
        surface = null;
    }

    // ── Playback Control ─────────────────────────────────────────────────────

    private void startPlayback() {
        if (isPlaying.get()) return;
        if (surface == null) {
            Toast.makeText(this, "Surface nicht bereit", Toast.LENGTH_SHORT).show();
            return;
        }
        String url = etServerUrl.getText().toString().trim();
        if (url.isEmpty()) {
            Toast.makeText(this, "Bitte URL eingeben", Toast.LENGTH_SHORT).show();
            return;
        }
        isPlaying.set(true);
        setUiPlaying(true);
        setStatus("Verbinde …");
        decodeFuture = executor.submit(() -> decodeLoop(url));
    }

    private void stopPlayback() {
        if (!isPlaying.getAndSet(false)) return;
        if (decodeFuture != null) decodeFuture.cancel(true);
        mainHandler.post(() -> {
            setUiPlaying(false);
            setStatus("Gestoppt");
        });
    }

    // ── Decode Loop ──────────────────────────────────────────────────────────

    private void decodeLoop(String rtspUrl) {
        Uri    uri   = Uri.parse(rtspUrl);
        String host  = uri.getHost();
        int    port  = uri.getPort() > 0 ? uri.getPort() : 554;
        String query = uri.getEncodedQuery() != null ? uri.getEncodedQuery() : "";

        RtpToHttpBridge bridge    = new RtpToHttpBridge(RTP_PORT);
        SatIpRtspClient rtsp      = new SatIpRtspClient();
        MediaExtractor  extractor = new MediaExtractor();
        MediaCodec      decoder   = null;

        try {
            // ── Schritt 1: Bridge starten (UDP + HTTP) ────────────────────────
            // UDP-Socket öffnet sich sofort, HTTP-Server wartet auf Verbindung
            setStatus("UDP + HTTP-Bridge starten …");
            int httpPort = bridge.start();
            Log.d(TAG, "Bridge läuft: UDP:" + RTP_PORT + " → HTTP:" + httpPort);

            // ── Schritt 2: RTSP-Handshake ─────────────────────────────────────
            setStatus("RTSP SETUP …");
            rtsp.connect(host, port, query, RTP_PORT);

            // ── Schritt 3: MediaExtractor verbindet sich mit lokalem HTTP-Server
            // Ab hier fließt der Stream: FritzBox→UDP→Bridge→HTTP→MediaExtractor
            setStatus("Stream wird geladen …");
            String localUrl = "http://127.0.0.1:" + httpPort;
            Log.d(TAG, "MediaExtractor.setDataSource: " + localUrl);
            extractor.setDataSource(localUrl);
            Log.d(TAG, "setDataSource() erfolgreich");

            // ── Schritt 4: Video-Track finden ─────────────────────────────────
            int videoTrack = findVideoTrack(extractor);
            if (videoTrack < 0) {
                postError("Kein H.264/H.265-Videotrack gefunden");
                return;
            }

            extractor.selectTrack(videoTrack);
            MediaFormat format = extractor.getTrackFormat(videoTrack);
            String mime = format.getString(MediaFormat.KEY_MIME);
            Log.d(TAG, "Video-Track " + videoTrack + ": " + format);

            // ── Schritt 5: MediaCodec konfigurieren ───────────────────────────
            decoder = MediaCodec.createDecoderByType(mime);
            decoder.configure(format, surface, null, 0);
            decoder.start();

            setStatus("Wiedergabe läuft (" + mime + ")");

            // ── Schritt 6: Decode-Schleife ────────────────────────────────────
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            boolean inputEos  = false;
            boolean outputEos = false;

            while (!outputEos && isPlaying.get() && !Thread.currentThread().isInterrupted()) {

                // Input: Sample → Decoder
                if (!inputEos) {
                    int inputIndex = decoder.dequeueInputBuffer(10_000);
                    if (inputIndex >= 0) {
                        ByteBuffer inputBuffer = decoder.getInputBuffer(inputIndex);
                        if (inputBuffer != null) {
                            int sampleSize = extractor.readSampleData(inputBuffer, 0);
                            if (sampleSize < 0) {
                                decoder.queueInputBuffer(inputIndex, 0, 0, 0,
                                        MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                                inputEos = true;
                            } else {
                                decoder.queueInputBuffer(inputIndex, 0, sampleSize,
                                        extractor.getSampleTime(), 0);
                                extractor.advance();
                            }
                        }
                    }
                }

                // Output: Frame auf Surface rendern
                int outputIndex = decoder.dequeueOutputBuffer(bufferInfo, 10_000);
                if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    Log.d(TAG, "Output format: " + decoder.getOutputFormat());
                } else if (outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    Thread.sleep(5);
                } else if (outputIndex >= 0) {
                    decoder.releaseOutputBuffer(outputIndex, bufferInfo.size > 0);
                    if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        outputEos = true;
                    }
                }
            }

            setStatus("Wiedergabe beendet");

        } catch (InterruptedException e) {
            Log.d(TAG, "Decode-Thread unterbrochen");
            Thread.currentThread().interrupt();

        } catch (IOException e) {
            Log.e(TAG, "Fehler: " + e.getMessage(), e);
            postError("Fehler: " + e.getMessage());

        } catch (Exception e) {
            Log.e(TAG, "Unbekannter Fehler", e);
            postError("Fehler: " + e.getMessage());

        } finally {
            if (decoder != null) {
                try { decoder.stop();    } catch (Exception ignored) {}
                try { decoder.release(); } catch (Exception ignored) {}
            }
            extractor.release();
            bridge.stop();
            rtsp.teardown();

            isPlaying.set(false);
            mainHandler.post(() -> setUiPlaying(false));
        }
    }

    // ── Hilfsmethoden ────────────────────────────────────────────────────────

    private int findVideoTrack(MediaExtractor extractor) {
        for (int i = 0; i < extractor.getTrackCount(); i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime != null && mime.startsWith("video/")) {
                Log.d(TAG, "Video-Track: index=" + i + " mime=" + mime);
                return i;
            }
        }
        return -1;
    }

    private void postError(String msg) {
        mainHandler.post(() -> {
            setStatus("Fehler: " + msg);
            setUiPlaying(false);
            Toast.makeText(TVPlayer3.this, msg, Toast.LENGTH_LONG).show();
        });
    }

    private void setStatus(String msg) {
        mainHandler.post(() -> tvStatus.setText(msg));
    }

    private void setUiPlaying(boolean playing) {
        btnPlay.setEnabled(!playing);
        btnStop.setEnabled(playing);
    }
}