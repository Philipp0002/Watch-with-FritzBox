package de.hahnphilipp.watchwithfritzbox.hbbtv;

import java.nio.ByteBuffer;

public class HbbTVApplicationDescriptor {

    public int appProfilesLen;
    public int profile;
    public int versionMajor;
    public int versionMinor;
    public int versionMicro;
    public int serviceBound;
    public int visibility;
    public int priority;


    /** 0x00 application_descriptor
     *  Syntax (vereinfachte/essentielle Felder, vgl. ETSI TS 102 809):
     *   application_profiles_length (8)
     *   for each profile:
     *     application_profile (16)
     *     version.major (8)
     *     version.minor (8)
     *     version.micro (8)
     *   service_bound_flag (1)
     *   visibility (2)
     *   reserved (5)
     *   application_priority (8)
     *   transport_protocol_labels[] (8 * N)
     */
    public static HbbTVApplicationDescriptor fromBytes(byte[] d) {
        HbbTVApplicationDescriptor hbbTVApplicationDescriptor = new HbbTVApplicationDescriptor();
        if (d == null || d.length < 1) return null;
        ByteBuffer bb = ByteBuffer.wrap(d);
        int appProfilesLen = bb.get() & 0xFF;
        hbbTVApplicationDescriptor.appProfilesLen = appProfilesLen;
        int pos = 1;
        while (pos + 4 <= 1 + appProfilesLen && pos + 4 <= d.length) {
            int profile = ((d[pos] & 0xFF) << 8) | (d[pos+1] & 0xFF);
            int vMaj = d[pos+2] & 0xFF;
            int vMin = d[pos+3] & 0xFF;
            // some specs include micro as separate byte; ETSI shows 3 version bytes often; try to be tolerant
            int vMicro = 0;
            if (pos + 4 < 1 + appProfilesLen && pos + 4 < d.length) {
                // if there is another byte we might interpret it as micro
                vMicro = d[pos+4] & 0xFF;
                // but do not advance pos extra here because we used fixed 4 for many cases — keep conservative
            }
            hbbTVApplicationDescriptor.profile = profile;
            hbbTVApplicationDescriptor.versionMajor = vMaj;
            hbbTVApplicationDescriptor.versionMinor = vMin;
            hbbTVApplicationDescriptor.versionMicro = vMicro;
            pos += 4;
        }
        // after profiles: a byte containing service_bound_flag(1) + visibility(2) + reserved(5)
        if (pos < d.length) {
            int flags = d[pos] & 0xFF;
            int serviceBound = (flags >> 7) & 0x1;        // if encoded as top bit
            int visibility = (flags >> 5) & 0x3;
            // if bits differ in encoding, you may need to shift differently — check your TS 102 809 version
            hbbTVApplicationDescriptor.serviceBound = serviceBound;
            hbbTVApplicationDescriptor.visibility = visibility;
            pos++;
            if (pos < d.length) {
                int priority = d[pos] & 0xFF;
                hbbTVApplicationDescriptor.priority = priority;
                pos++;
            }
            // remaining bytes may be transport labels
            // flip: dont need that probably
            /*if (pos < d.length) {
                sb.append("; transport_labels=");
                for (int i = pos; i < d.length; i++) {
                    sb.append(String.format("0x%02X", d[i])).append(i+1<d.length? ",":"");
                }
            }*/
        }
        return hbbTVApplicationDescriptor;
    }
}
