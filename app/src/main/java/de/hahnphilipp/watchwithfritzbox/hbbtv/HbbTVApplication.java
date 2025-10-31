package de.hahnphilipp.watchwithfritzbox.hbbtv;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class HbbTVApplication {
    public HbbTVApplicationDescriptor applicationDescriptor;
    public HbbTVApplicationNameDescriptor applicationNameDescriptor;
    public HbbTVTransportProtocolDescriptor transportProtocolDescriptor;
    public HbbTVApplicationIconsDescriptor applicationIconsDescriptor;
    public HbbTVApplicationStorageDescriptor applicationStorageDescriptor;
    public HbbTVDsmccCarouselIdDescriptor dsmccCarouselIdDescriptor;
    public HbbTVGraphicsConstraintsDescriptor graphicsConstraintsDescriptor;
    public HbbTVSimpleApplicationLocationDescriptor simpleApplicationLocationDescriptor;
    public HbbTVDataBroadcastIdDescriptor dataBroadcastIdDescriptor;
    public HbbTVApplicationTransportDescriptor applicationTransportDescriptor;
    public HbbTVHttpDescriptor httpDescriptor;

    public static HbbTVApplication fromAitApplication(AitApplication aitApplication) {
        HbbTVApplication hbbTVApplication = new HbbTVApplication();

        for (AitDescriptor descriptor : aitApplication.descriptors) {
            switch (descriptor.tag & 0xFF) {
                case 0x00:
                    hbbTVApplication.applicationDescriptor = HbbTVApplicationDescriptor.fromBytes(descriptor.data);
                    break;
                case 0x01:
                    hbbTVApplication.applicationNameDescriptor = HbbTVApplicationNameDescriptor.fromBytes(descriptor.data);
                    break;
                case 0x05:
                    hbbTVApplication.transportProtocolDescriptor = HbbTVTransportProtocolDescriptor.fromBytes(descriptor.data);
                    break;
                case 0x0B:
                    hbbTVApplication.applicationIconsDescriptor = HbbTVApplicationIconsDescriptor.fromBytes(descriptor.data);
                    break;
                case 0x10:
                    hbbTVApplication.applicationStorageDescriptor = HbbTVApplicationStorageDescriptor.fromBytes(descriptor.data);
                    break;
                case 0x13:
                    hbbTVApplication.dsmccCarouselIdDescriptor = HbbTVDsmccCarouselIdDescriptor.fromBytes(descriptor.data);
                    break;
                case 0x14:
                    hbbTVApplication.graphicsConstraintsDescriptor = HbbTVGraphicsConstraintsDescriptor.fromBytes(descriptor.data);
                    break;
                case 0x15:
                    hbbTVApplication.simpleApplicationLocationDescriptor = HbbTVSimpleApplicationLocationDescriptor.fromBytes(descriptor.data);
                    break;
                case 0x16:
                    hbbTVApplication.applicationTransportDescriptor = HbbTVApplicationTransportDescriptor.fromBytes(descriptor.data);
                    break;
                case 0x66:
                    hbbTVApplication.dataBroadcastIdDescriptor = HbbTVDataBroadcastIdDescriptor.fromBytes(descriptor.data);
                    break;
                case 0x2:
                    hbbTVApplication.httpDescriptor = HbbTVHttpDescriptor.fromBytes(descriptor.data);
                    break;
                default:
                    Log.e("HbbTV", "Unknown descriptor 0x" + Integer.toHexString(descriptor.tag & 0xFF) +
                            " (" + (descriptor.data != null ? descriptor.data.length : 0) + " bytes): " + hex(descriptor.data));
                    break;
            }
        }
        return hbbTVApplication;
    }

    private static String hex(byte[] b) {
        if (b == null) return "null";
        StringBuilder sb = new StringBuilder();
        for (byte x : b) sb.append(String.format("%02X ", x));
        return sb.toString().trim();
    }
}
