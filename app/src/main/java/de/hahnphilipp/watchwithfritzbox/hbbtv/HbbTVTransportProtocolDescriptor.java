package de.hahnphilipp.watchwithfritzbox.hbbtv;

import java.nio.charset.StandardCharsets;

public class HbbTVTransportProtocolDescriptor {

    public int protocolId;
    public int label;
    public String url;

    /** 0x05 transport_protocol_descriptor (häufig HTTP base URL)
     *  Typische Struktur: for each entry:
     *   protocol_id (16) + transport_label (8) + url_base_length (8) + url_base (url_base_length)
     */
    public static HbbTVTransportProtocolDescriptor fromBytes(byte[] d) {
        HbbTVTransportProtocolDescriptor hbbTVTransportProtocolDescriptor = new HbbTVTransportProtocolDescriptor();
        if (d == null || d.length < 3) return null;
        int pos = 0;
        while (pos + 3 <= d.length) {
            if (pos + 2 > d.length) break;
            int protocolId = ((d[pos] & 0xFF) << 8) | (d[pos+1] & 0xFF);
            pos += 2;
            int label = d[pos++] & 0xFF;
            if (pos >= d.length) {
                //sb.append(String.format("proto=0x%04X label=0x%02X (no url)", protocolId, label));
                hbbTVTransportProtocolDescriptor.protocolId = protocolId;
                hbbTVTransportProtocolDescriptor.label = label;
                break;
            }
            int urlLen = d[pos++] & 0xFF;
            int usable = Math.min(urlLen, d.length - pos);
            String url = "";
            if (usable > 0) {
                url = new String(d, pos, usable, StandardCharsets.UTF_8);
                pos += usable;
            }
            //sb.append(String.format("[proto=0x%04X label=0x%02X url=\"%s\"] ", protocolId, label, url));
            hbbTVTransportProtocolDescriptor.protocolId = protocolId;
            hbbTVTransportProtocolDescriptor.label = label;
            hbbTVTransportProtocolDescriptor.url = url;
        }
        return hbbTVTransportProtocolDescriptor;
    }

}
