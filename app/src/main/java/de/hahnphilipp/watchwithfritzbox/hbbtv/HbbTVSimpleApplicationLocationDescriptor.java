package de.hahnphilipp.watchwithfritzbox.hbbtv;

import java.nio.charset.StandardCharsets;

public class HbbTVSimpleApplicationLocationDescriptor {

    public String initialPath;

    /** 0x15 simple_application_location_descriptor
     *  Enthält einfach initial_path_bytes (z. B. "index.html" oder "player/index.php?arg...").
     */
    public static HbbTVSimpleApplicationLocationDescriptor fromBytes(byte[] d) {
        HbbTVSimpleApplicationLocationDescriptor hbbTVSimpleApplicationLocationDescriptor = new HbbTVSimpleApplicationLocationDescriptor();
        if (d == null || d.length == 0) return null;
        String path = new String(d, StandardCharsets.UTF_8);
        hbbTVSimpleApplicationLocationDescriptor.initialPath = path;
        return hbbTVSimpleApplicationLocationDescriptor;
    }
}
