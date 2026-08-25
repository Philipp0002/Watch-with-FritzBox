package de.hahnphilipp.watchwithfritzbox.utils;

import android.content.Context;
import android.net.wifi.WifiManager;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UpnpDeviceDiscovery {

    private final Context context;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public interface DiscoveryCallback {
        void onDeviceFound(String xmlUrl, String server);
        void onTimeout();
        void onError(Exception e);
    }

    public UpnpDeviceDiscovery(Context context) {
        this.context = context.getApplicationContext();
    }

    public void discoverDeviceByUrn(final String targetUrn, final int timeoutMs, final DiscoveryCallback callback) {
        executorService.execute(() -> {
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            if (wifiManager == null) {
                callback.onError(new Exception("WifiManager not available"));
                return;
            }

            WifiManager.MulticastLock multicastLock = wifiManager.createMulticastLock("upnp_discovery_lock");
            DatagramSocket socket = null;

            try {
                multicastLock.acquire();

                String message = "M-SEARCH * HTTP/1.1\r\n" +
                        "HOST: 239.255.255.250:1900\r\n" +
                        "MAN: \"ssdp:discover\"\r\n" +
                        "MX: 3\r\n" +
                        "ST: " + targetUrn + "\r\n" +
                        "\r\n";

                byte[] sendData = message.getBytes();
                InetAddress multicastAddress = InetAddress.getByName("239.255.255.250");
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, multicastAddress, 1900);

                socket = new DatagramSocket();
                socket.setSoTimeout(timeoutMs);
                socket.send(sendPacket);

                byte[] receiveData = new byte[1024];
                long startTime = System.currentTimeMillis();

                while (System.currentTimeMillis() - startTime < timeoutMs) {
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    try {
                        socket.receive(receivePacket);
                        String response = new String(receivePacket.getData(), 0, receivePacket.getLength());

                        if (response.toUpperCase(Locale.US).contains("HTTP/1.1 200 OK")) {
                            String locationUrl = parseHeader(response, "LOCATION");
                            String server = parseHeader(response, "SERVER");
                            if (locationUrl != null) {
                                callback.onDeviceFound(locationUrl, server);
                            }
                        }
                    } catch (InterruptedIOException e) {
                        break;
                    }
                }
                callback.onTimeout();

            } catch (IOException e) {
                callback.onError(e);
            } finally {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
                if (multicastLock.isHeld()) {
                    multicastLock.release();
                }
            }
        });
    }

    private String parseHeader(String response, String headerName) {
        String[] lines = response.split("\r\n");
        String prefix = headerName.toUpperCase(Locale.US) + ":";
        for (String line : lines) {
            if (line.toUpperCase(Locale.US).startsWith(prefix)) {
                return line.substring(prefix.length()).trim();
            }
        }
        return null;
    }
}