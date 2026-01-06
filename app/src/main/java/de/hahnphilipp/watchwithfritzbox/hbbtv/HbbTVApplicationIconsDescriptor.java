package de.hahnphilipp.watchwithfritzbox.hbbtv;

import java.nio.charset.StandardCharsets;

public class HbbTVApplicationIconsDescriptor {

    public String locator;
    public int iconFlags;

    /** 0x0B application_icons_descriptor (vereinfachte Darstellung)
     *  Enthält icon_locator_length + icon_locator_bytes + icon_flags (16) ...
     */
    public static HbbTVApplicationIconsDescriptor fromBytes(byte[] d) {
        HbbTVApplicationIconsDescriptor hbbTVApplicationIconsDescriptor = new HbbTVApplicationIconsDescriptor();
        if (d == null || d.length < 1) return null;
        int pos = 0;
        int locatorLen = d[pos++] & 0xFF;
        String locator = "";
        if (locatorLen > 0 && pos + locatorLen <= d.length) {
            locator = new String(d, pos, locatorLen, StandardCharsets.UTF_8);
            pos += locatorLen;
        }
        // next should be icon_flags (2 bytes)
        String flags = "";
        if (pos + 2 <= d.length) {
            int iconFlags = ((d[pos] & 0xFF) << 8) | (d[pos+1] & 0xFF);
            pos += 2;
            flags = String.format("iconFlags=0x%04X", iconFlags);
            hbbTVApplicationIconsDescriptor.iconFlags = iconFlags;
        }
        hbbTVApplicationIconsDescriptor.locator = locator;
        //return "application_icons_descriptor: locator=\"" + locator + "\" " + flags;
        return hbbTVApplicationIconsDescriptor;
    }

}
