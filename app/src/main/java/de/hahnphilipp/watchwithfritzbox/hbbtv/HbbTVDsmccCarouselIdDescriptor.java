package de.hahnphilipp.watchwithfritzbox.hbbtv;

public class HbbTVDsmccCarouselIdDescriptor {

    public int carouselId;

    /** 0x13 DSM-CC Carousel Identifier (vereinfachte Darstellung) */
    public static HbbTVDsmccCarouselIdDescriptor fromBytes(byte[] d) {
        HbbTVDsmccCarouselIdDescriptor hbbTVDsmccCarouselIdDescriptor = new HbbTVDsmccCarouselIdDescriptor();
        if (d == null || d.length < 2) return null;
        // structure can be: carouselId (16) + private data...
        int carouselId = ((d[0] & 0xFF) << 8) | (d[1] & 0xFF);
        hbbTVDsmccCarouselIdDescriptor.carouselId = carouselId;
        return hbbTVDsmccCarouselIdDescriptor;
        //return "DSM-CC Carousel Identifier: id=" + carouselId + " (extra bytes=" + (d.length-2) + ")";
    }

}
