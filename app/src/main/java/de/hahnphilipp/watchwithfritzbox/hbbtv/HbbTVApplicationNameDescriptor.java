package de.hahnphilipp.watchwithfritzbox.hbbtv;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class HbbTVApplicationNameDescriptor {

    // LANG -> NAME mapping
    HashMap<String, String> names;

    /** 0x01 application_name_descriptor
     *  Aufbau: eine oder mehrere Sprachblöcke:
     *   ISO_639_language_code (3 bytes) + name_length(1) + name_bytes(name_length)
     */
    public static HbbTVApplicationNameDescriptor fromBytes(byte[] d) {
        HbbTVApplicationNameDescriptor hbbTVApplicationNameDescriptor = new HbbTVApplicationNameDescriptor();
        if (d == null || d.length < 4) return null;
        hbbTVApplicationNameDescriptor.names = new HashMap<>();
        int pos = 0;
        while (pos + 4 <= d.length) {
            String lang = new String(d, pos, 3, StandardCharsets.US_ASCII);
            int nameLen = d[pos + 3] & 0xFF;
            pos += 4;
            if (pos + nameLen > d.length) nameLen = Math.max(0, d.length - pos);
            String name = new String(d, pos, nameLen, StandardCharsets.UTF_8);
            hbbTVApplicationNameDescriptor.names.put(lang, name);
            pos += nameLen;
        }
        return hbbTVApplicationNameDescriptor;
    }
}
