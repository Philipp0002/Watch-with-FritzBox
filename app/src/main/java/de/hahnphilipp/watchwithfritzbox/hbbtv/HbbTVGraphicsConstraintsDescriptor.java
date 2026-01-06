package de.hahnphilipp.watchwithfritzbox.hbbtv;

public class HbbTVGraphicsConstraintsDescriptor {

    public boolean canRunWithoutVisibleUI;

    /** 0x14 graphics_constraints_descriptor (vereinfacht) */
    public static HbbTVGraphicsConstraintsDescriptor fromBytes(byte[] d) {
        HbbTVGraphicsConstraintsDescriptor hbbTVGraphicsConstraintsDescriptor = new HbbTVGraphicsConstraintsDescriptor();
        if (d == null || d.length < 1) return null;
        int b0 = d[0] & 0xFF;
        boolean canRunWithoutVisibleUI = (b0 & 0x01) != 0; // example bit mapping — check spec version
        hbbTVGraphicsConstraintsDescriptor.canRunWithoutVisibleUI = canRunWithoutVisibleUI;
        return hbbTVGraphicsConstraintsDescriptor;
    }

}
