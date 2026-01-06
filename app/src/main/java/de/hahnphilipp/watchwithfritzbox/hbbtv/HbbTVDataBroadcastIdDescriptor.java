package de.hahnphilipp.watchwithfritzbox.hbbtv;

public class HbbTVDataBroadcastIdDescriptor {

    public String dataId;

    /** 0x66 data_broadcast_id_descriptor (vereinfachte Darstellung) */
    public static HbbTVDataBroadcastIdDescriptor fromBytes(byte[] d) {
        HbbTVDataBroadcastIdDescriptor hbbTVDataBroadcastIdDescriptor = new HbbTVDataBroadcastIdDescriptor();
        if (d == null || d.length < 2) return null;
        int dataId = ((d[0] & 0xFF) << 8) | (d[1] & 0xFF);
        hbbTVDataBroadcastIdDescriptor.dataId = "0x" + Integer.toHexString(dataId);
        return hbbTVDataBroadcastIdDescriptor;
        //return "data_broadcast_id_descriptor: id=0x" + Integer.toHexString(dataId) + " (" + dataId + ")";
    }
}
