package de.hahnphilipp.watchwithfritzbox.player;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Brücke zwischen RTP/UDP und MediaExtractor via lokalem HTTP-Server.
 *
 * NuCachedSource2-Problem und Lösung:
 *
 * NuCachedSource2 liest beim setDataSource()-Aufruf einen definierten Sniffing-
 * Buffer (intern SNIFF_SIZE = 128KB + weitere reads bis ~1.5MB) und ruft dann
 * intern seekTo(0) auf – er öffnet eine ZWEITE HTTP-Verbindung mit dem Header
 * "Range: bytes=0-" um von vorne zu lesen.
 *
 * Wenn wir keinen Range-Header unterstützen und dieselben Daten nicht nochmal
 * liefern können (Live-Stream!), meldet er EOS.
 *
 * Lösung: Range-Requests erkennen und mit "206 Partial Content" beantworten,
 * aber trotzdem den Live-Stream ab der aktuellen Position liefern.
 * NuCachedSource2 akzeptiert das und streamt weiter.
 */
public class RtpToHttpBridge {

    private static final String TAG            = "RtpHttpBridge";
    private static final int    MAX_UDP_SIZE   = 1500;
    private static final int    QUEUE_CAPACITY = 4000;

    private final int rtpPort;
    private int       httpPort;

    private ServerSocket           serverSocket;
    private DatagramSocket         udpSocket;
    private Thread                 rtpThread;
    private Thread                 httpThread;
    private volatile boolean       running = false;

    private final BlockingQueue<byte[]> queue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);

    public RtpToHttpBridge(int rtpPort) {
        this.rtpPort = rtpPort;
    }

    public int start() throws IOException {
        running = true;

        udpSocket = new DatagramSocket(rtpPort);
        udpSocket.setSoTimeout(5_000);
        Log.d(TAG, "UDP-Socket auf Port " + rtpPort + " geöffnet");

        serverSocket = new ServerSocket(0);
        serverSocket.setSoTimeout(30_000);
        httpPort = serverSocket.getLocalPort();
        Log.d(TAG, "HTTP-Server auf Port " + httpPort + " gestartet");

        rtpThread  = new Thread(this::rtpReceiveLoop, "RTP-Receiver");
        httpThread = new Thread(this::httpServeLoop,  "HTTP-Server");
        rtpThread.setDaemon(true);
        httpThread.setDaemon(true);
        rtpThread.start();
        httpThread.start();

        return httpPort;
    }

    public int getHttpPort() { return httpPort; }

    public void stop() {
        running = false;
        queue.clear();
        try { if (udpSocket    != null) udpSocket.close();    } catch (Exception ignored) {}
        try { if (serverSocket != null) serverSocket.close(); } catch (Exception ignored) {}
        if (rtpThread  != null) rtpThread.interrupt();
        if (httpThread != null) httpThread.interrupt();
        Log.d(TAG, "RtpToHttpBridge gestoppt");
    }

    // ── RTP-Empfangs-Thread ──────────────────────────────────────────────────

    private void rtpReceiveLoop() {
        byte[]         buf   = new byte[MAX_UDP_SIZE];
        DatagramPacket pkt   = new DatagramPacket(buf, buf.length);
        long           total = 0;

        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                udpSocket.receive(pkt);
                total++;
                if (total == 1)
                    Log.d(TAG, "Erstes RTP-Paket von "
                            + pkt.getAddress() + ":" + pkt.getPort()
                            + " | " + pkt.getLength() + " B");
                if (total % 500 == 0)
                    Log.d(TAG, "RTP: " + total + " Pakete | Queue: " + queue.size());

                byte[] payload = stripRtpHeader(buf, pkt.getLength());
                if (payload != null) {
                    if (!queue.offer(payload)) {
                        queue.poll();
                        queue.offer(payload);
                    }
                }
            } catch (SocketTimeoutException e) {
                Log.w(TAG, "RTP UDP-Timeout");
            } catch (IOException e) {
                if (running) Log.e(TAG, "RTP Fehler", e);
            }
        }
        Log.d(TAG, "RTP-Thread beendet | Gesamt: " + total + " Pakete");
    }

    private byte[] stripRtpHeader(byte[] data, int length) {
        if (length < 12) return null;
        int cc         = data[0] & 0x0F;
        int headerSize = 12 + (cc * 4);
        if ((data[0] & 0x10) != 0 && length > headerSize + 4) {
            int extWords = ((data[headerSize + 2] & 0xFF) << 8)
                    |  (data[headerSize + 3] & 0xFF);
            headerSize += 4 + (extWords * 4);
        }
        if (headerSize >= length) return null;
        byte[] out = new byte[length - headerSize];
        System.arraycopy(data, headerSize, out, 0, out.length);
        return out;
    }

    // ── Lokaler HTTP-Server ──────────────────────────────────────────────────

    private void httpServeLoop() {
        int connCount = 0;
        Log.d(TAG, "HTTP-Server bereit …");

        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                Socket client = serverSocket.accept();
                connCount++;
                final int id = connCount;
                Log.d(TAG, "Verbindung #" + id + " von " + client.getRemoteSocketAddress());
                Thread t = new Thread(() -> handleHttpClient(client, id), "HTTP-Conn-" + id);
                t.setDaemon(true);
                t.start();
            } catch (SocketTimeoutException e) {
                if (running) Log.d(TAG, "accept() Timeout, warte weiter …");
            } catch (IOException e) {
                if (running) Log.e(TAG, "accept() Fehler", e);
            }
        }
        Log.d(TAG, "HTTP-Server-Thread beendet");
    }

    private void handleHttpClient(Socket client, int id) {
        try {
            // HTTP-Request lesen und Range-Header merken
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(client.getInputStream()));
            String requestLine = reader.readLine();
            Log.d(TAG, "#" + id + " " + requestLine);

            boolean isRangeRequest = false;
            String headerLine;
            while ((headerLine = reader.readLine()) != null && !headerLine.isEmpty()) {
                if (headerLine.toLowerCase().startsWith("range:")) {
                    isRangeRequest = true;
                    Log.d(TAG, "#" + id + " Range-Request erkannt: " + headerLine);
                }
            }

            OutputStream out = client.getOutputStream();

            // Bei Range-Request mit 206 antworten – NuCachedSource2 akzeptiert das
            // und streamt weiter ohne EOS zu signalisieren.
            // Content-Range mit sehr großem End-Wert vortäuschen (Live-Stream).
            String statusLine = isRangeRequest
                    ? "HTTP/1.1 206 Partial Content\r\n"
                    : "HTTP/1.1 200 OK\r\n";

            String responseHeader = statusLine +
                    "Content-Type: video/mp2t\r\n" +
                    "Transfer-Encoding: chunked\r\n" +
                    "Cache-Control: no-cache, no-store\r\n" +
                    "Pragma: no-cache\r\n" +
                    "Connection: keep-alive\r\n" +
                    // Für Range-Requests: extrem große "Datei" vortäuschen
                    // NuCachedSource2 liest content-range um zu wissen ob seek möglich
                    (isRangeRequest
                            ? "Content-Range: bytes 0-999999999999/1000000000000\r\n"
                            : "") +
                    "\r\n";

            out.write(responseHeader.getBytes("UTF-8"));
            out.flush();
            Log.d(TAG, "#" + id + " Header gesendet (" +
                    (isRangeRequest ? "206" : "200") + "), starte Stream …");

            long bytesWritten = 0;

            while (running && !Thread.currentThread().isInterrupted()) {
                try {
                    byte[] chunk = queue.poll(5_000, TimeUnit.MILLISECONDS);
                    if (chunk == null) {
                        chunk = createTsNullPacket();
                        Log.w(TAG, "#" + id + " Keepalive TS-Null");
                    }

                    String chunkLen = Integer.toHexString(chunk.length) + "\r\n";
                    out.write(chunkLen.getBytes("UTF-8"));
                    out.write(chunk);
                    out.write("\r\n".getBytes("UTF-8"));
                    bytesWritten += chunk.length;

                    if (bytesWritten % (512 * 1024) < chunk.length) {
                        out.flush();
                        Log.d(TAG, "#" + id + " " + (bytesWritten / 1024) + " KB gesendet");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (IOException e) {
                    Log.w(TAG, "#" + id + " Disconnect (" + e.getMessage()
                            + ") nach " + (bytesWritten / 1024) + " KB");
                    break;
                }
            }
        } catch (IOException e) {
            if (running) Log.e(TAG, "#" + id + " Fehler", e);
        } finally {
            try { client.close(); } catch (IOException ignored) {}
            Log.d(TAG, "#" + id + " Verbindung geschlossen");
        }
    }

    /** MPEG-TS Null-Paket (PID 0x1FFF, 188 Bytes) als Keepalive. */
    private byte[] createTsNullPacket() {
        byte[] pkt = new byte[188];
        pkt[0] = 0x47;
        pkt[1] = 0x1F;
        pkt[2] = (byte) 0xFF;
        pkt[3] = 0x10;
        return pkt;
    }
}