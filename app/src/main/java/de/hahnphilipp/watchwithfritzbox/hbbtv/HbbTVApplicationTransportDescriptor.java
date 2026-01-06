package de.hahnphilipp.watchwithfritzbox.hbbtv;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class HbbTVApplicationTransportDescriptor {
    public int label;
    public int protocolId;
    public String baseUrl;

    public static HbbTVApplicationTransportDescriptor fromBytes(byte[] d) {
        if (d == null || d.length < 3) return null; // zu kurz für Tag + Length + Label

        ByteBuffer buf = ByteBuffer.wrap(d);
        buf.order(ByteOrder.BIG_ENDIAN);

        int tag = buf.get() & 0xFF;
        if (tag != 0x16) {
            // Nicht der erwartete Descriptor
            return null;
        }

        int descriptorLength = buf.get() & 0xFF;
        if (descriptorLength == 0 || descriptorLength > buf.remaining()) {
            return null; // ungültige Länge
        }

        int startPos = buf.position();
        HbbTVApplicationTransportDescriptor desc = new HbbTVApplicationTransportDescriptor();

        // Label
        if (buf.remaining() < 1) return null;
        desc.label = buf.get() & 0xFF;

        // Protocol ID
        if (buf.remaining() < 2) return null;
        desc.protocolId = buf.getShort() & 0xFFFF;

        // Selector Length
        if (buf.remaining() < 1) return null;
        int selectorLength = buf.get() & 0xFF;

        // Selector Bytes / URL
        if (selectorLength > 0 && selectorLength <= buf.remaining()) {
            byte[] selectorBytes = new byte[selectorLength];
            buf.get(selectorBytes);
            if (desc.protocolId == 0x0002) {
                // HTTP URL
                desc.baseUrl = new String(selectorBytes, StandardCharsets.UTF_8);
            } else {
                // Sonstiges Protokoll
                desc.baseUrl = Arrays.toString(selectorBytes);
            }
        }

        // Sicherstellen, dass wir exakt am Ende des Descriptors sind
        buf.position(startPos + descriptorLength);

        return desc;
    }

}
