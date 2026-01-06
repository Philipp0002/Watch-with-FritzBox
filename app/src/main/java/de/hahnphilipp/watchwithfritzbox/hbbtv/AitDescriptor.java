package de.hahnphilipp.watchwithfritzbox.hbbtv;

import androidx.annotation.NonNull;

import com.google.android.gms.common.util.Hex;

import java.nio.charset.StandardCharsets;

public class AitDescriptor {
    public int tag;
    public byte[] data;

    @NonNull
    @Override
    public String toString() {
        String decoded = new String(data, StandardCharsets.UTF_8);
        return "AitDescriptor (tag="+Integer.toHexString(tag) + "; data=" + Hex.bytesToStringUppercase(data) + "; text="+decoded+")";
    }
}
