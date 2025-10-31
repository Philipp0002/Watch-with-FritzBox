package de.hahnphilipp.watchwithfritzbox.hbbtv;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class HbbTVHttpDescriptor {
    public int label;   // z.B. 02
    public int length;  // Länge der URL
    public String url;

    /**
     * Parst einen Descriptor-Byte-Array.
     * Erwartet, dass der erste Byte der Tag ist (0x02)
     * und der zweite Byte die Länge der URL-Daten.
     */
    public static HbbTVHttpDescriptor fromBytes(byte[] data) {
        if (data == null || data.length < 3) return null;

        HbbTVHttpDescriptor desc = new HbbTVHttpDescriptor();

        desc.label = data[2-1] & 0xFF; // Byte 2 ist Label (Index 2-1=1)
        desc.length = data[3] & 0xFF;

        if (data.length < 3 + desc.length -1) return null; // Sicherheitsprüfung

        byte[] urlBytes = Arrays.copyOfRange(data, 4, 4 + desc.length);
        desc.url = new String(urlBytes, StandardCharsets.UTF_8);

        return desc;
    }

}
