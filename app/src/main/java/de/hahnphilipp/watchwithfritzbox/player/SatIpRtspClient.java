package de.hahnphilipp.watchwithfritzbox.player;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Minimaler RTSP-Client für SAT>IP (AVM FritzBox).
 *
 * Führt den vollständigen RTSP-Handshake durch:
 *   DESCRIBE → SETUP → PLAY
 * und gibt den ausgehandelten lokalen UDP-Port zurück,
 * auf dem der RTP-Stream empfangen werden soll.
 */
public class SatIpRtspClient {

    private static final String TAG = "SatIpRtsp";

    private Socket       rtspSocket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private int          cseq    = 1;
    private String       session = null;
    private String       fullUrl = null;

    /**
     * Baut die RTSP-Verbindung auf und startet den Stream.
     *
     * @param host    IP/Hostname des SAT>IP-Servers
     * @param port    RTSP-Port (typisch 554)
     * @param query   Query-String ohne führendes '?'
     * @param rtpPort Lokaler UDP-Port für RTP-Empfang
     */
    public void connect(String host, int port, String query, int rtpPort) throws IOException {
        fullUrl = "rtsp://" + host + ":" + port + "/?" + query;

        rtspSocket = new Socket(host, port);
        rtspSocket.setSoTimeout(10_000);
        inputStream  = rtspSocket.getInputStream();
        outputStream = rtspSocket.getOutputStream();

        // ── DESCRIBE ──────────────────────────────────────────────────────────
        sendRequest(
                "DESCRIBE " + fullUrl + " RTSP/1.0\r\n" +
                        "CSeq: " + (cseq++) + "\r\n" +
                        "Accept: application/sdp\r\n" +
                        "\r\n"
        );
        // SDP-Body vollständig lesen damit der Socket-Puffer sauber ist
        readResponse(true);

        // ── SETUP ─────────────────────────────────────────────────────────────
        sendRequest(
                "SETUP " + fullUrl + " RTSP/1.0\r\n" +
                        "CSeq: " + (cseq++) + "\r\n" +
                        "Transport: RTP/AVP;unicast;" +
                        "client_port=" + rtpPort + "-" + (rtpPort + 1) + "\r\n" +
                        "\r\n"
        );
        String setupResp = readResponse(false);

        session = extractHeader(setupResp, "Session");
        if (session != null && session.contains(";")) {
            session = session.split(";")[0].trim();
        }
        if (session == null || session.isEmpty()) {
            throw new IOException("Kein Session-Header in SETUP-Antwort: " + setupResp);
        }
        Log.d(TAG, "RTSP Session: " + session);

        // ── PLAY ──────────────────────────────────────────────────────────────
        sendRequest(
                "PLAY " + fullUrl + " RTSP/1.0\r\n" +
                        "CSeq: " + (cseq++) + "\r\n" +
                        "Session: " + session + "\r\n" +
                        "Range: npt=0.000-\r\n" +
                        "\r\n"
        );
        readResponse(false);

        Log.d(TAG, "RTSP PLAY gesendet – RTP-Empfang auf Port " + rtpPort);
    }

    /** Beendet den Stream sauber mit TEARDOWN. */
    public void teardown() {
        if (outputStream == null || fullUrl == null) return;
        try {
            sendRequest(
                    "TEARDOWN " + fullUrl + " RTSP/1.0\r\n" +
                            "CSeq: " + (cseq++) + "\r\n" +
                            (session != null ? "Session: " + session + "\r\n" : "") +
                            "\r\n"
            );
            readResponse(false);
        } catch (IOException e) {
            Log.w(TAG, "TEARDOWN fehlgeschlagen (ignoriert): " + e.getMessage());
        } finally {
            close();
        }
    }

    public void close() {
        try { if (rtspSocket != null) rtspSocket.close(); } catch (IOException ignored) {}
    }

    // ── Interne Hilfsmethoden ────────────────────────────────────────────────

    private void sendRequest(String request) throws IOException {
        Log.d(TAG, ">> " + request.trim());
        outputStream.write(request.getBytes("UTF-8"));
        outputStream.flush();
    }

    /**
     * Liest eine vollständige RTSP-Antwort.
     *
     * @param consumeBody true = Content-Length Bytes nach dem Header komplett lesen
     *                    (notwendig nach DESCRIBE damit der Puffer sauber bleibt)
     */
    private String readResponse(boolean consumeBody) throws IOException {
        StringBuilder sb      = new StringBuilder();
        StringBuilder line    = new StringBuilder();
        int           status  = 200;
        int           contentLength = 0;
        boolean       firstLine     = true;

        // Header zeilenweise lesen (CRLF)
        int b;
        while ((b = inputStream.read()) != -1) {
            if (b == '\r') continue; // CR ignorieren
            if (b == '\n') {
                String lineStr = line.toString();
                sb.append(lineStr).append("\n");
                line.setLength(0);

                if (firstLine) {
                    firstLine = false;
                    String[] parts = lineStr.split(" ", 3);
                    if (parts.length >= 2) {
                        try { status = Integer.parseInt(parts[1]); }
                        catch (NumberFormatException ignored) {}
                    }
                }

                // Content-Length Header merken
                if (lineStr.toLowerCase().startsWith("content-length:")) {
                    try {
                        contentLength = Integer.parseInt(
                                lineStr.substring("content-length:".length()).trim());
                    } catch (NumberFormatException ignored) {}
                }

                // Leerzeile = Header-Ende
                if (lineStr.isEmpty()) break;
            } else {
                line.append((char) b);
            }
        }

        // Body vollständig lesen wenn gewünscht (z.B. SDP nach DESCRIBE)
        if (consumeBody && contentLength > 0) {
            byte[] body = new byte[contentLength];
            int read = 0;
            while (read < contentLength) {
                int n = inputStream.read(body, read, contentLength - read);
                if (n < 0) break;
                read += n;
            }
            // Body nicht loggen (zu groß), nur Länge
            Log.d(TAG, "<body " + read + " bytes consumed>");
        }

        String response = sb.toString();
        Log.d(TAG, "<< " + response.trim());

        if (status >= 400) {
            throw new IOException("RTSP-Fehler " + status + ": " + response);
        }
        return response;
    }

    /** Extrahiert einen Header-Wert (case-insensitive), null wenn nicht gefunden. */
    private String extractHeader(String response, String headerName) {
        for (String lineStr : response.split("\n")) {
            if (lineStr.toLowerCase().startsWith(headerName.toLowerCase() + ":")) {
                return lineStr.substring(headerName.length() + 1).trim();
            }
        }
        return null;
    }
}