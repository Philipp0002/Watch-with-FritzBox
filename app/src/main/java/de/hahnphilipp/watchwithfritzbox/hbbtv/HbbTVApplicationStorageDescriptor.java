package de.hahnphilipp.watchwithfritzbox.hbbtv;

public class HbbTVApplicationStorageDescriptor {

    public String storageProperty;

    /** 0x10 application_storage_descriptor (vereinfachter Parser) */
    public static HbbTVApplicationStorageDescriptor fromBytes(byte[] d) {
        HbbTVApplicationStorageDescriptor hbbTVApplicationStorageDescriptor = new HbbTVApplicationStorageDescriptor();
        if (d == null || d.length < 1) return null;
        // storage_property and maybe other flags depending on spec version
        int storageProp = d[0] & 0xFF;
        hbbTVApplicationStorageDescriptor.storageProperty = "0x" +Integer.toHexString(storageProp);
        return hbbTVApplicationStorageDescriptor;
        //return "application_storage_descriptor: storageProperty=0x" + Integer.toHexString(storageProp) + " (see TS102809)";
    }

}
