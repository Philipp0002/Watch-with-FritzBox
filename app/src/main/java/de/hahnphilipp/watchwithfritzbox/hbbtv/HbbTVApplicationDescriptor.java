package de.hahnphilipp.watchwithfritzbox.hbbtv;

import java.nio.ByteBuffer;

public class HbbTVApplicationDescriptor {

    public int appProfilesLen;
    public int profile;
    public int versionMajor;
    public int versionMinor;
    public int versionMicro;
    public int serviceBound;
    /**
     * 0 = not visible for user and other apps
     * 1 = visible only for user selection
     * 2 = visible only for other apps
     * 3 = visible for user and other apps
     */
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

        // Das Ende der Profildaten, relativ zum Start des Arrays 'd'
        int profileDataEnd = 1 + appProfilesLen;

        // Schleife, solange 5 Bytes (pos bis pos+4) innerhalb der
        // deklarierten Profillänge UND der gesamten Datenlänge verfügbar sind.
        while (pos + 4 < profileDataEnd && pos + 4 < d.length) {

            int profile = ((d[pos] & 0xFF) << 8) | (d[pos+1] & 0xFF);
            int vMaj = d[pos+2] & 0xFF;
            int vMin = d[pos+3] & 0xFF;
            int vMicro = d[pos+4] & 0xFF; // vMicro immer als 5. Byte lesen

            // Hinweis: Dies speichert nur das LETZTE gefundene Profil.
            // Wenn Sie alle Profile benötigen, brauchen Sie eine Liste.
            hbbTVApplicationDescriptor.profile = profile;
            hbbTVApplicationDescriptor.versionMajor = vMaj;
            hbbTVApplicationDescriptor.versionMinor = vMin;
            hbbTVApplicationDescriptor.versionMicro = vMicro;

            pos += 5; // <-- DIE ENTSCHEIDENDE KORREKTUR
        }

        // Der Rest des Codes (flags, priority) sollte nun korrekt
        // an der Position 'pos' (nach den Profildaten) ansetzen.
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
