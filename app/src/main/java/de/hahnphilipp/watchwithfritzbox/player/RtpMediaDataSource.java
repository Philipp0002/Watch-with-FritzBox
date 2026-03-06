package de.hahnphilipp.watchwithfritzbox.player;

import android.media.MediaDataSource;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

/**
 * Empfängt RTP/UDP-Pakete (MPEG-TS Payload) und stellt sie dem
 * MediaExtractor über die MediaDataSource-Schnittstelle zur Verfügung.
 *
 * Mindest-API: 23 (MediaDataSource)
 *
 * Funktionsweise:
 *  - Ein Empfangs-Thread lauscht auf dem angegebenen UDP-Port.
 *  - Jedes RTP-Paket wird entpackt (Header wird abgeschnitten),
 *    der MPEG-TS-Payload in einen RingBuffer geschrieben.
 *  - MediaExtractor ruft readAt(position, ...) auf; der RingBuffer
 *    liefert die Bytes an der gewünschten absoluten Position.
 */
@RequiresApi(api = Build.VERSION_CODES.M)
public class RtpMediaDataSource extends MediaDataSource {

    private static final String TAG              = "RtpDataSource";
    private static final int    RING_BUFFER_SIZE = 8 * 1024 * 1024; // 8 MB
    private static final int    UDP_TIMEOUT_MS   = 5_000;
    private static final int    READ_TIMEOUT_MS  = 10_000;
    private static final int    MAX_UDP_PACKET   = 1500;

    // ── RingBuffer ───────────────────────────────────────────────────────────
    private final byte[] ring     = new byte[RING_BUFFER_SIZE];
    private       long   writePos = 0;
    private final Object lock     = new Object();

    // ── Netzwerk ─────────────────────────────────────────────────────────────
    private DatagramSocket   udpSocket;
    private Thread           receiveThread;
    private volatile boolean closed = false;

    /**
     * Öffnet den UDP-Socket sofort (damit keine Pakete verloren gehen)
     * und startet den Empfangs-Thread.
     *
     * WICHTIG: Vor rtsp.connect() UND vor extractor.setDataSource() aufrufen!
     */
    public void startReceiving(int port) throws SocketException {
        udpSocket = new DatagramSocket(port);
        udpSocket.setSoTimeout(UDP_TIMEOUT_MS);

        Log.d(TAG, "UDP-Socket geöffnet auf Port " + port
                + " | LocalAddress: " + udpSocket.getLocalAddress()
                + " | LocalPort: " + udpSocket.getLocalPort());

        receiveThread = new Thread(() -> {
            byte[]         buf          = new byte[MAX_UDP_PACKET];
            DatagramPacket packet       = new DatagramPacket(buf, buf.length);
            long           packetsTotal = 0;
            long           bytesTotal   = 0;
            long           timeoutCount = 0;

            while (!closed && !Thread.currentThread().isInterrupted()) {
                try {
                    udpSocket.receive(packet);
                    packetsTotal++;
                    bytesTotal += packet.getLength();

                    // Ersten empfangenen Paket immer loggen
                    if (packetsTotal == 1) {
                        Log.d(TAG, "Erstes RTP-Paket empfangen!"
                                + " Von: " + packet.getAddress() + ":" + packet.getPort()
                                + " Länge: " + packet.getLength() + " Bytes");
                    }
                    // Alle 100 Pakete Status loggen
                    if (packetsTotal % 100 == 0) {
                        Log.d(TAG, "RTP-Statistik: " + packetsTotal + " Pakete, "
                                + (bytesTotal / 1024) + " KB gesamt");
                    }

                    processRtpPacket(buf, packet.getLength());

                } catch (SocketTimeoutException e) {
                    timeoutCount++;
                    Log.w(TAG, "UDP-Timeout #" + timeoutCount
                            + " (keine Pakete in " + UDP_TIMEOUT_MS + "ms)"
                            + " | Bisher: " + packetsTotal + " Pakete empfangen");
                    // Nach 3 Timeouts ohne ein einziges Paket → wahrscheinlich
                    // kein UDP-Empfang möglich, abbrechen
                    if (packetsTotal == 0 && timeoutCount >= 3) {
                        Log.e(TAG, "FEHLER: 3x Timeout ohne ein einziges Paket!"
                                + " RTP kommt nicht an Port " + port + " an.");
                        break;
                    }
                } catch (IOException e) {
                    if (!closed) Log.e(TAG, "UDP-Empfangsfehler", e);
                }
            }
            Log.d(TAG, "RTP-Empfangs-Thread beendet | Gesamt: "
                    + packetsTotal + " Pakete, " + (bytesTotal / 1024) + " KB");

            // Wenn nie ein Paket ankam, DataSource schließen damit readAt() nicht
            // ewig wartet sondern mit -1 zurückkehrt
            if (packetsTotal == 0) {
                closed = true;
                synchronized (lock) { lock.notifyAll(); }
            }
        }, "RTP-Receiver");

        receiveThread.setDaemon(true);
        receiveThread.start();
        Log.d(TAG, "RTP-Empfangs-Thread gestartet");
    }

    /**
     * RTP-Header abschneiden (RFC 3550) und TS-Payload in RingBuffer schreiben.
     *
     * RTP Fixed Header (12 Byte Minimum):
     *  Byte 0: V(2) P(1) X(1) CC(4)
     *  Byte 1: M(1) PT(7)
     *  Byte 2-3: Sequence Number
     *  Byte 4-7: Timestamp
     *  Byte 8-11: SSRC
     *  [Byte 12+: CSRC-Liste (CC * 4 Byte)]
     *  [Optional: Extension Header]
     */
    private void processRtpPacket(byte[] data, int length) {
        if (length < 12) return;

        int cc         = data[0] & 0x0F;
        int headerSize = 12 + (cc * 4);

        boolean hasExtension = (data[0] & 0x10) != 0;
        if (hasExtension && length > headerSize + 4) {
            int extWords = ((data[headerSize + 2] & 0xFF) << 8)
                    |  (data[headerSize + 3] & 0xFF);
            headerSize += 4 + (extWords * 4);
        }

        if (headerSize >= length) return;

        int payloadSize = length - headerSize;

        synchronized (lock) {
            for (int i = 0; i < payloadSize; i++) {
                ring[(int) ((writePos + i) % RING_BUFFER_SIZE)] = data[headerSize + i];
            }
            writePos += payloadSize;
            lock.notifyAll();
        }
    }

    // ── MediaDataSource ──────────────────────────────────────────────────────

    @Override
    public int readAt(long position, byte[] buffer, int offset, int size) throws IOException {
        synchronized (lock) {
            long deadline = System.currentTimeMillis() + READ_TIMEOUT_MS;

            while (writePos <= position && !closed) {
                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0) {
                    Log.w(TAG, "readAt Timeout – position=" + position
                            + " writePos=" + writePos);
                    return -1;
                }
                try {
                    lock.wait(remaining);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return -1;
                }
            }

            if (closed) return -1;

            if (writePos - position > RING_BUFFER_SIZE) {
                position = writePos - RING_BUFFER_SIZE;
            }

            int available = (int) Math.min(writePos - position, size);
            for (int i = 0; i < available; i++) {
                buffer[offset + i] = ring[(int) ((position + i) % RING_BUFFER_SIZE)];
            }
            return available;
        }
    }

    @Override
    public long getSize() {
        return -1;
    }

    @Override
    public void close() {
        closed = true;
        if (udpSocket != null && !udpSocket.isClosed()) {
            udpSocket.close();
        }
        if (receiveThread != null) {
            receiveThread.interrupt();
        }
        synchronized (lock) {
            lock.notifyAll();
        }
        Log.d(TAG, "RtpMediaDataSource geschlossen");
    }
}